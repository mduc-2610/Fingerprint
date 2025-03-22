#!/usr/bin/env python
# coding: utf-8

# In[1]:


import numpy as np
import tensorflow as tf
import keras
import random
from tensorflow.keras.models import load_model
import os
import cv2
import sys
from tensorflow.keras.metrics import Metric

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
        return self.intersection / (self.union + 1e-6)
        
    def reset_state(self):
        self.intersection.assign(0.0)
        self.union.assign(0.0)

# Load pre-trained models
try:
    fingerprint_recognition_model = load_model('../fingerprint_models/recognition/siamese_network.keras', custom_objects={'IoU': IoU})
    finger_segmentation_model = load_model('../fingerprint_models/segmentation/unet_segmentation.keras', custom_objects={'IoU': IoU})
    
    # Get input shapes
    if isinstance(fingerprint_recognition_model.input, list):
        input_shape = fingerprint_recognition_model.input[0].shape[1:3]
    else:
        input_shape = fingerprint_recognition_model.input_shape[1:3]
    
    if isinstance(finger_segmentation_model.input, list):
        segmentation_shape = finger_segmentation_model.input[0].shape[1:3]
    else:
        segmentation_shape = finger_segmentation_model.input_shape[1:3]
    
except Exception as e:
    print(f"Error loading models: {e}")
    input_shape = (90, 90)
    segmentation_shape = (90, 90)

# Function to preprocess fingerprint images
def preprocess_fingerprint(image_path):
    """Preprocess fingerprint images to match model requirements"""
    try:
        # Read image
        img = cv2.imread(image_path, cv2.IMREAD_GRAYSCALE)
        if img is None:
            raise ValueError(f"Could not load image from {image_path}")
        
        # Resize for segmentation
        img_for_segmentation = cv2.resize(img, (segmentation_shape[1], segmentation_shape[0]))
        
        # Enhance contrast
        clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
        img_for_segmentation = clahe.apply(img_for_segmentation)
        
        # Prepare for segmentation model
        img_for_segmentation = np.expand_dims(np.expand_dims(img_for_segmentation, axis=-1), axis=0) / 255.0
        
        # Get segmentation mask
        segmentation_output = finger_segmentation_model.predict(img_for_segmentation, verbose=0)
        
        # Process mask
        if isinstance(segmentation_output, list) and len(segmentation_output) > 0:
            mask = segmentation_output[0]
        else:
            mask = np.ones(img.shape, dtype=np.uint8)
        
        mask = (mask > 0.5).astype(np.uint8)
        
        # Convert 3D mask to 2D if needed
        if len(mask.shape) == 3:
            mask_2d = mask[:, :, 0]
        else:
            mask_2d = mask
        
        # Resize mask and apply to image
        mask_resized = cv2.resize(mask_2d, (img.shape[1], img.shape[0]))
        img = img * mask_resized
        
        # Resize for recognition model
        img = cv2.resize(img, (input_shape[1], input_shape[0]))
        
        # Normalize and add channel dimension
        img = img / 255.0
        img = np.expand_dims(img, axis=-1)
        
        return img
        
    except Exception as e:
        print(f"Error in preprocess_fingerprint: {e}")
        raise

# Function to create embedding extractor model
def create_embedding_model(recognition_model):
    feature_extractor = recognition_model.get_layer('functional')
    input_shape = feature_extractor.input_shape[1:]
    new_input = keras.layers.Input(shape=input_shape)
    embedding = feature_extractor(new_input)
    embedding_model = keras.Model(inputs=new_input, outputs=embedding)
    return embedding_model

# Get embedding extractor
embedding_model = create_embedding_model(fingerprint_recognition_model)

# Function to process new employee fingerprints
def process_new_employee(employee_id, fingerprint_images_paths):
    print(f"Processing fingerprints for employee {employee_id}")
    
    processed_dir = f"../processed_fingerprints/employee_{employee_id}"
    os.makedirs(processed_dir, exist_ok=True)
    
    fingerprints = []
    
    for i, img_path in enumerate(fingerprint_images_paths):
        try:
            processed_img = preprocess_fingerprint(img_path)
            processed_path = os.path.join(processed_dir, f"fp_{i}.npy")
            np.save(processed_path, processed_img)
            fingerprints.append(processed_img)
        except Exception as e:
            print(f"  Error processing {img_path}: {e}")
    
    if len(fingerprints) == 0:
        raise ValueError(f"No fingerprints were successfully processed for employee {employee_id}")
        
    return np.array(fingerprints)

# Function to enroll employees
def enroll_employees(employee_data_paths):
    new_employees_data = {}
    
    for employee_id, image_paths in employee_data_paths.items():
        try:
            fingerprints = process_new_employee(employee_id, image_paths)
            new_employees_data[employee_id] = fingerprints
            print(f"Successfully enrolled employee {employee_id} with {len(fingerprints)} fingerprints")
        except Exception as e:
            print(f"Failed to enroll employee {employee_id}: {e}")
    
    if not new_employees_data:
        raise ValueError("No employees were successfully enrolled")
    
    # Create embeddings database
    embeddings_db = {}
    for employee_id, fingerprints in new_employees_data.items():
        employee_embeddings = embedding_model.predict(fingerprints, verbose=0)
        embeddings_db[employee_id] = np.mean(employee_embeddings, axis=0)  # Store average embedding
    
    # Save embeddings database
    os.makedirs("../fingerprint_adapting_models", exist_ok=True)
    np.save("../fingerprint_adapting_models/employee_embeddings.npy", embeddings_db)
    
    return embeddings_db

# Function to recognize employees from new fingerprint
def recognize_employee(image_path, embeddings_db=None, threshold=0.7):
    try:
        # Preprocess image
        processed_img = preprocess_fingerprint(image_path)
        processed_img = np.expand_dims(processed_img, axis=0)
        
        results = {}
        
        # Similarity matching using embeddings
        if embeddings_db is not None:
            # Get embedding for the input fingerprint
            embedding = embedding_model.predict(processed_img, verbose=0)[0]
            
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

def generate_employee_data(dir):
    employee_data = {}
    for _employee_id in os.listdir(dir):
        employee_data[_employee_id] = []
        for _employee_fingerprint in os.listdir(f"{dir}/{_employee_id}"):
            employee_data[_employee_id].append(f"{dir}/{_employee_id}/{_employee_fingerprint}")
    return employee_data

# Check if models are loaded successfully
if 'fingerprint_recognition_model' not in globals() or 'finger_segmentation_model' not in globals():
    print("Models failed to load. Please check model paths and formats.")
    sys.exit(1)

# Ensure data paths are correct
EMPLOYEE_DIR = "../fingerprint_adapting_dataset"
employee_data_paths = generate_employee_data(dir=EMPLOYEE_DIR)

try:
    # Enroll employees
    embeddings_db = enroll_employees(employee_data_paths)
    
    
except Exception as e:
    print(f"An error occurred: {e}")


# In[2]:


# Test recognition with random employee
random_employee_id = random.randint(102, 109)
random_employee_fingerprint_position = random.randint(1, 5)
test_image = f"{EMPLOYEE_DIR}/{random_employee_id}/{random_employee_id}_{random_employee_fingerprint_position}.bmp"
if os.path.exists(test_image):
    print(f"Testing recognition with image: {test_image}")
    result = recognize_employee(
        test_image, 
        embeddings_db=embeddings_db
    )
    
    # Compare expected vs actual results
    expected_id = str(random_employee_id)
    actual_id = result.get('similarity', {}).get('employee_id')
    confidence = result.get('similarity', {}).get('confidence', 0)
    
    print(f"Expected ID: {expected_id}")
    print(f"Recognized ID: {actual_id}")
    print(f"Confidence: {confidence:.2f}")
    print(f"{result}")
    if expected_id == actual_id:
        print("✅ MATCH SUCCESSFUL!")
    else:
        print("❌ MATCH FAILED!")
        
else:
    print(f"Test image {test_image} not found!")
    


# In[3]:


def test_recognition_accuracy(employee_data_paths, embeddings_db, threshold=0.7):
    total_tests = 0
    successful_matches = 0

    for employee_id, image_paths in employee_data_paths.items():
        # if employee_id == 101 or employee_id == '101': continue
        # print(employee_id)
        for img_path in image_paths:
            if os.path.exists(img_path):
                result = recognize_employee(img_path, embeddings_db=embeddings_db, threshold=threshold)
                
                expected_id = str(employee_id)
                actual_id = result.get('similarity', {}).get('employee_id')
                confidence = result.get('similarity', {}).get('confidence', 0)

                if expected_id == actual_id:
                    # print("✅ MATCH SUCCESSFUL!")
                    successful_matches += 1
                # else:
                #     print("❌ MATCH FAILED!")

                total_tests += 1

    # Calculate accuracy
    if total_tests > 0:
        accuracy = (successful_matches / total_tests) * 100
        print(f"\nRecognition Accuracy: {accuracy:.2f}% ({successful_matches}/{total_tests} successful matches)")
    else:
        print("No test cases found!")

# Run accuracy test
test_recognition_accuracy(employee_data_paths, embeddings_db)

