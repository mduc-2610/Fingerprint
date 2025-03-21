import numpy as np
import tensorflow as tf
import keras
import random
from tensorflow.keras.models import load_model
from tensorflow.keras.preprocessing.image import ImageDataGenerator
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.callbacks import ModelCheckpoint, EarlyStopping
import os
import cv2
import matplotlib.pyplot as plt
import sys
from tensorflow.keras.metrics import Metric
import re

@keras.saving.register_keras_serializable()
class IoU(Metric):
    def __init__(self, name='iou', **kwargs):
        super(IoU, self).__init__(name=name, **kwargs)
        self.intersection = self.add_weight(name='intersection', initializer='zeros')
        self.union = self.add_weight(name='union', initializer='zeros')
        
    def update_state(self, y_true, y_pred, sample_weight=None):
        y_pred = tf.cast(y_pred > 0.5, tf.float32)
        intersection = tf.reduce_sum(y_true * y_pred)
        union = tf.reduce_sum(y_true) + tf.reduce_sum(y_pred) - intersection
        
        self.intersection.assign_add(intersection)
        self.union.assign_add(union)
        
    def result(self):
        return self.intersection / (self.union + 1e-6)  # Adding small epsilon to avoid division by zero
        
    def reset_state(self):
        self.intersection.assign(0.0)
        self.union.assign(0.0)

# Load your pre-trained models
# Modify the part where you get the input shape
try:
    fingerprint_recognition_model = load_model('fingerprint_models/recognition/siamese_network.keras', custom_objects={'IoU': IoU})
    finger_segmentation_model = load_model('fingerprint_models/segmentation/unet_segmentation.keras', custom_objects={'IoU': IoU})
    
    # Fix for input shape retrieval
    if isinstance(fingerprint_recognition_model.input, list):
        # If the model has multiple inputs
        input_shape = fingerprint_recognition_model.input[0].shape[1:3]
    else:
        # If the model has a single input
        input_shape = fingerprint_recognition_model.input_shape[1:3]
    
    print(f"Recognition model expects input shape: {input_shape}")
    
    # Do the same for segmentation model
    if isinstance(finger_segmentation_model.input, list):
        segmentation_shape = finger_segmentation_model.input[0].shape[1:3]
    else:
        segmentation_shape = finger_segmentation_model.input_shape[1:3]
    
    print(f"Segmentation model expects input shape: {segmentation_shape}")
except Exception as e:
    print(f"Error loading models: {e}")
    # Fallback to default shape if model loading fails
    input_shape = (90, 90)
    segmentation_shape = (90, 90)
    print(f"Using fallback input shape: {input_shape}")


# Function to preprocess fingerprint images
def preprocess_fingerprint(image_path):
    """Preprocess fingerprint images to match model requirements"""
    try:
        # Read image
        print(f"Loading image from {image_path}")
        img = cv2.imread(image_path, cv2.IMREAD_GRAYSCALE)
        if img is None:
            raise ValueError(f"Could not load image from {image_path}")
        
        print(f"Original image size: {img.shape}")
        # In the preprocess_fingerprint function, before resizing:
        print("Resizing for recognition model")
        if not isinstance(input_shape, tuple) or len(input_shape) < 2:
            print(f"Warning: input_shape is not in expected format: {input_shape}")
            # Use a fallback shape
            resize_shape = (90, 90)
        else:
            resize_shape = (input_shape[1], input_shape[0])

        img = cv2.resize(img, resize_shape)
        # First resize to segmentation model's expected size
        print("Resizing image for segmentation")
        img_for_segmentation = cv2.resize(img, (segmentation_shape[1], segmentation_shape[0]))
        
        # Enhance contrast using CLAHE
        print("Applying CLAHE")
        clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
        img_for_segmentation = clahe.apply(img_for_segmentation)
        
        # Prepare for segmentation model
        print("Preparing image for segmentation model")
        img_for_segmentation = np.expand_dims(np.expand_dims(img_for_segmentation, axis=-1), axis=0) / 255.0
        print(f"Input to segmentation model shape: {img_for_segmentation.shape}")
        
        # Use segmentation model to extract fingerprint region
        print("Running segmentation model prediction")
        segmentation_output = finger_segmentation_model.predict(img_for_segmentation)
        print(f"Segmentation model output type: {type(segmentation_output)}")
        print(f"Segmentation model output length: {len(segmentation_output)}")
        
        # Add safety checks
        if isinstance(segmentation_output, list) and len(segmentation_output) > 0:
            mask = segmentation_output[0]
            print(f"Mask shape: {mask.shape}")
        else:
            print("Warning: Unexpected segmentation output format")
            print(f"Segmentation output: {segmentation_output}")
            # Create a placeholder mask of ones (no masking)
            mask = np.ones(img.shape, dtype=np.uint8)
        
        mask = (mask > 0.5).astype(np.uint8)
        print(f"Mask shape after thresholding: {mask.shape}")
        
        # Resize mask to original image size
        print("Resizing mask to match original image")
        if len(mask.shape) == 3:
            # Extract 2D mask from 3D array
            mask_2d = mask[:, :, 0]
        else:
            mask_2d = mask
        
        print(f"2D mask shape before resize: {mask_2d.shape}")
        mask_resized = cv2.resize(mask_2d, (img.shape[1], img.shape[0]))
        print(f"Resized mask shape: {mask_resized.shape}")
        
        # Apply mask
        print("Applying mask to image")
        img = img * mask_resized
        print(f"Image shape after masking: {img.shape}")
        
        # Resize to recognition model's expected size
        print("Resizing for recognition model")
        img = cv2.resize(img, (input_shape[1], input_shape[0]))
        
        # Normalize to [0, 1]
        print("Normalizing image")
        img = img / 255.0
        
        # Add channel dimension
        print("Adding channel dimension")
        img = np.expand_dims(img, axis=-1)
        
        print(f"Final processed image shape: {img.shape}")
        return img
        
    except Exception as e:
        import traceback
        print(f"Error in preprocess_fingerprint: {e}")
        print(f"Traceback: {traceback.format_exc()}")
        raise

# Function to create embedding extractor model
# Modify the create_embedding_model function to properly handle Siamese networks
def create_embedding_model(recognition_model):
    # Extract the feature extraction part
    feature_extractor = recognition_model.get_layer('functional')
    
    # Create a new standalone model with a single input
    input_shape = feature_extractor.input_shape[1:]  # Get input shape without batch dimension
    new_input = keras.layers.Input(shape=input_shape)
    
    # Apply the feature extractor
    embedding = feature_extractor(new_input)
    
    # Create and return the model
    embedding_model = keras.Model(inputs=new_input, outputs=embedding)
    return embedding_model

# Get embedding extractor
embedding_model = create_embedding_model(fingerprint_recognition_model)

# Function to collect and process new employee fingerprints
def process_new_employee(employee_id, fingerprint_images_paths):
    print(f"Processing fingerprints for employee {employee_id}")
    
    # Create directory for processed images
    processed_dir = f"data/processed/employee_{employee_id}"
    os.makedirs(processed_dir, exist_ok=True)
    
    fingerprints = []
    
    for i, img_path in enumerate(fingerprint_images_paths):
        try:
            # Preprocess the fingerprint
            processed_img = preprocess_fingerprint(img_path)
            
            # Save processed image
            processed_path = os.path.join(processed_dir, f"fp_{i}.npy")
            np.save(processed_path, processed_img)
            
            fingerprints.append(processed_img)
            print(f"  Processed fingerprint {i+1}/{len(fingerprint_images_paths)}")
        except Exception as e:
            print(f"  Error processing {img_path}: {e}")
    
    # Check if any fingerprints were successfully processed
    if len(fingerprints) == 0:
        raise ValueError(f"No fingerprints were successfully processed for employee {employee_id}")
        
    return np.array(fingerprints)

# Function to fine-tune model with new employees
def fine_tune_model(model, new_employees_data, learning_rate=0.0001, epochs=20, batch_size=16):
    print("Starting fine-tuning process...")
    
    # Check if data is empty
    if len(new_employees_data) == 0:
        raise ValueError("No employee data available for fine-tuning")
    
    # Prepare data
    X = []
    employee_ids = []
    
    for employee_id, fingerprints in new_employees_data.items():
        if len(fingerprints) > 0:  # Only add if there are fingerprints
            for fp in fingerprints:
                X.append(fp)
                employee_ids.append(employee_id)  # Store original employee ID
    
    # Check if we have data to train on
    if len(X) == 0:
        raise ValueError("No fingerprint data available for fine-tuning")
    
    X = np.array(X)
    
    # Map original employee IDs to sequential class indices
    unique_ids = sorted(set(employee_ids))
    id_to_index = {id: idx for idx, id in enumerate(unique_ids)}
    
    # Convert employee IDs to sequential indices
    y = np.array([id_to_index[emp_id] for emp_id in employee_ids])
    
    print(f"Training data shape: X={X.shape}, y={y.shape}")
    print(f"Employee ID mapping: {id_to_index}")
    
    # Convert labels to one-hot encoding
    num_classes = len(unique_ids)  # Number of unique employee IDs
    y_encoded = keras.utils.to_categorical(y, num_classes=num_classes)
    
    # Create an embedding model first (using one branch of Siamese network)
    embedding_model = create_embedding_model(model)
    
    # Create a new classification model on top of embeddings
    inputs = keras.layers.Input(shape=X.shape[1:])
    x = embedding_model(inputs)
    predictions = keras.layers.Dense(num_classes, activation='softmax', name='output_dense')(x)
    
    # Create the complete classification model
    classification_model = keras.Model(inputs=inputs, outputs=predictions)
    
    # Compile
    classification_model.compile(
        optimizer=Adam(learning_rate=learning_rate),
        loss='categorical_crossentropy',
        metrics=['accuracy']
    )
    
    # Define callbacks
    checkpoint = ModelCheckpoint(
        "fine_tuned_model.keras",
        monitor='val_accuracy',
        save_best_only=True,
        mode='max',
        verbose=1
    )
    
    early_stopping = EarlyStopping(
        monitor='val_accuracy',
        patience=5,
        restore_best_weights=True,
        verbose=1
    )
    
    # Data augmentation
    datagen = ImageDataGenerator(
        rotation_range=10,
        width_shift_range=0.1,
        height_shift_range=0.1,
        zoom_range=0.1,
        horizontal_flip=False,
        vertical_flip=False,
        validation_split=0.2
    )
    
    # Split data - ensure we have enough samples
    if len(X) < 2:  # Need at least one sample for training and one for validation
        raise ValueError(f"Not enough samples for training. Got {len(X)} samples, need at least 2.")
    
    train_idx = max(1, int(0.8 * len(X)))  # Ensure at least 1 sample for training
    X_train, X_val = X[:train_idx], X[train_idx:]
    y_train, y_val = y_encoded[:train_idx], y_encoded[train_idx:]
    
    print(f"Training split: {X_train.shape}, {y_train.shape}")
    print(f"Validation split: {X_val.shape}, {y_val.shape}")
    
    # If validation set is empty, use a small portion of training data
    if len(X_val) == 0:
        print("Warning: Validation set is empty. Using a portion of training data for validation.")
        val_idx = max(1, int(0.2 * len(X_train)))
        X_val = X_train[:val_idx]
        y_val = y_train[:val_idx]
        X_train = X_train[val_idx:]
        y_train = y_train[val_idx:]
    
    # Fit the model
    history = classification_model.fit(
        datagen.flow(X_train, y_train, batch_size=min(batch_size, len(X_train))),
        validation_data=(X_val, y_val),
        epochs=epochs,
        callbacks=[checkpoint, early_stopping]
    )
    
    # Save the mapping for later use in recognition
    np.save("employee_id_mapping.npy", id_to_index)
    
    return classification_model, history, id_to_index

# Function to enroll new employees in the system
# def enroll_employees(employee_data_paths):
#     new_employees_data = {}
    
#     for employee_id, image_paths in employee_data_paths.items():
#         try:
#             # Process fingerprints
#             fingerprints = process_new_employee(employee_id, image_paths)
#             new_employees_data[employee_id] = fingerprints
#             print(f"Successfully enrolled employee {employee_id} with {len(fingerprints)} fingerprints")
#         except Exception as e:
#             print(f"Failed to enroll employee {employee_id}: {e}")
    
#     # Check if we have any data to work with
#     if not new_employees_data:
#         raise ValueError("No employees were successfully enrolled")
    
#     # Fine-tune the model
#     fine_tuned_model, history = fine_tune_model(fingerprint_recognition_model, new_employees_data)
    
#     # Save embeddings for faster matching
#     embeddings_db = {}
    
#     for employee_id, fingerprints in new_employees_data.items():
#         employee_embeddings = embedding_model.predict(fingerprints)
#         embeddings_db[employee_id] = np.mean(employee_embeddings, axis=0)  # Store average embedding
    
#     # Save embeddings database
#     np.save("employee_embeddings.npy", embeddings_db)
    
#     return fine_tuned_model, embeddings_db

def enroll_employees(employee_data_paths):
    new_employees_data = {}
    
    for employee_id, image_paths in employee_data_paths.items():
        try:
            # Process fingerprints
            fingerprints = process_new_employee(employee_id, image_paths)
            new_employees_data[employee_id] = fingerprints
            print(f"Successfully enrolled employee {employee_id} with {len(fingerprints)} fingerprints")
        except Exception as e:
            print(f"Failed to enroll employee {employee_id}: {e}")
    
    # Check if we have any data to work with
    if not new_employees_data:
        raise ValueError("No employees were successfully enrolled")
    
    # Fine-tune the model
    fine_tuned_model, history, id_mapping = fine_tune_model(fingerprint_recognition_model, new_employees_data)
    
    # Save embeddings for faster matching
    embeddings_db = {}
    
    for employee_id, fingerprints in new_employees_data.items():
        employee_embeddings = embedding_model.predict(fingerprints)
        embeddings_db[employee_id] = np.mean(employee_embeddings, axis=0)  # Store average embedding
    
    # Save embeddings database
    np.save("employee_embeddings.npy", embeddings_db)
    
    return fine_tuned_model, embeddings_db

# Function to recognize employees from new fingerprint
def recognize_employee(image_path, fine_tuned_model=None, embeddings_db=None, threshold=0.7):
    try:
        # Preprocess image
        processed_img = preprocess_fingerprint(image_path)
        processed_img = np.expand_dims(processed_img, axis=0)
        
        results = {}
        
        # Method 1: Direct classification (if fine-tuned model is available)
        if fine_tuned_model is not None:
            # Load the ID mapping
            try:
                id_mapping = np.load("employee_id_mapping.npy", allow_pickle=True).item()
                # Create reverse mapping (index -> employee_id)
                index_to_id = {idx: emp_id for emp_id, idx in id_mapping.items()}
            except Exception as e:
                print(f"Error loading ID mapping: {e}")
                index_to_id = {}  # Empty mapping as fallback
            
            # Make predictions
            preds = fine_tuned_model.predict(processed_img)[0]
            pred_index = np.argmax(preds)
            confidence = preds[pred_index]
            
            # Map the predicted index back to employee ID
            if pred_index in index_to_id:
                employee_id = index_to_id[pred_index]
            else:
                employee_id = str(pred_index)  # Fallback if mapping fails
            
            if confidence >= threshold:
                results["classification"] = {"employee_id": employee_id, "confidence": float(confidence)}
            else:
                results["classification"] = {"employee_id": None, "confidence": float(confidence)}
        
        # Method 2: Similarity matching (using embeddings)
        if embeddings_db is not None:
            # Get embedding for the input fingerprint using the embedding model
            embedding = embedding_model.predict(processed_img)[0]
            
            # Find most similar embedding
            best_match = None
            best_similarity = -1
            
            for employee_id, stored_embedding in embeddings_db.items():
                # Compute cosine similarity
                similarity = np.dot(embedding, stored_embedding) / (np.linalg.norm(embedding) * np.linalg.norm(stored_embedding))
                
                if similarity > best_similarity:
                    best_similarity = similarity
                    best_match = employee_id
            
            if best_similarity >= threshold:
                results["similarity"] = {"employee_id": best_match, "confidence": float(best_similarity)}
            else:
                results["similarity"] = {"employee_id": None, "confidence": float(best_similarity)}
        
        return results
    
    except Exception as e:
        print(f"Error during recognition: {e}")
        return {"error": str(e)}
    

def generate_employee_data(dir) :
    employee_data = {}

    for _employee_id in os.listdir(dir):
        employee_data[_employee_id] = []
        for _employee_fingerprint in os.listdir(f"{dir}/{_employee_id}"):
            employee_data[_employee_id].append(f"{dir}/{_employee_id}/{_employee_fingerprint}")
    return employee_data

# Example usage
if __name__ == "__main__":
    # Check if models are loaded successfully before proceeding
    if 'fingerprint_recognition_model' not in globals() or 'finger_segmentation_model' not in globals():
        print("Models failed to load. Please check model paths and formats.")
        sys.exit(1)
    
    
    # Ensure data paths are correct and files exist
    EMPLOYEE_DIR = "Organized_Fingerprints"
    employee_data_paths = generate_employee_data(dir=EMPLOYEE_DIR)
    print(employee_data_paths)
    # Verify files exist
    for emp_id, paths in employee_data_paths.items():
        for path in paths:
            if not os.path.exists(path):
                print(f"Warning: File {path} does not exist!")
    
    try:
        # Enroll employees
        fine_tuned_model, embeddings_db = enroll_employees(employee_data_paths)
        
        # Example recognition
        random_employee_id = random.randint(102, 109)
        random_employee_fingperint_position = random.randint(0, 4)
        test_image = f"{EMPLOYEE_DIR}/{random_employee_id}/{random_employee_id}_{random_employee_fingperint_position}.tif"  # Fixed path
        if os.path.exists(test_image):
            print(f"Testing recognition with image: {test_image}")
            result = recognize_employee(
                test_image, 
                fine_tuned_model=fine_tuned_model, 
                embeddings_db=embeddings_db
            )

            print("Expect results: ", random_employee_id)            
            print("Recognition results:", result)

            print("")
        else:
            print(f"Test image {test_image} not found!")
    except Exception as e:
        print(f"An error occurred: {e}")