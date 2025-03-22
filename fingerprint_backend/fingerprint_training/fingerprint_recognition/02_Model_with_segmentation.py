



import numpy as np
import keras
import os
from keras import layers
from keras.models import Model
from sklearn.utils import shuffle
from sklearn.model_selection import train_test_split
import random
from tensorflow.keras.layers import Input, Conv2D, MaxPooling2D, Flatten, Dense, Dropout, BatchNormalization, Subtract, Conv2DTranspose, concatenate, Cropping2D
import tensorflow as tf
import cv2

def load_dataset():
    datasets = ['real', 'easy', 'medium', 'hard']
    data_dict = {}
    for dataset in datasets:
        x_data = np.load(f'dataset/x_{dataset}.npy')
        y_data = np.load(f'dataset/y_{dataset}.npy')
        data_dict[dataset] = (x_data, y_data)
        print(f"{dataset}: X shape {x_data.shape}, Y shape {y_data.shape}")

    return data_dict

data_dict = load_dataset()
def load_segmentation_dataset():
    print("Loading fingerprint region segmentation dataset...")
    
    try:
        x_images = np.load('dataset/x_segmentation.npy')
        y_masks = np.load('dataset/y_segmentation.npy')
        print(f"Loaded existing segmentation dataset: {x_images.shape}, {y_masks.shape}")
        return x_images, y_masks
    except:
        print("Creating synthetic segmentation dataset...")
    
    data_dict = load_dataset()
    
    all_images = np.concatenate([
        data_dict['real'][0],
        data_dict['easy'][0],
        data_dict['medium'][0],
        data_dict['hard'][0]
    ], axis=0)
    
    masks = []
    for img in all_images:
        img_gray = img.squeeze()
        
        _, binary = cv2.threshold(img_gray, 200, 255, cv2.THRESH_BINARY_INV)
        
        kernel = np.ones((5, 5), np.uint8)
        binary = cv2.morphologyEx(binary, cv2.MORPH_CLOSE, kernel)
        binary = cv2.morphologyEx(binary, cv2.MORPH_OPEN, kernel)
        
        mask = binary / 255.0
        masks.append(mask)
    
    x_images = all_images
    y_masks = np.array(masks)[..., np.newaxis]
    
    os.makedirs('dataset', exist_ok=True)
    np.save('dataset/x_segmentation.npy', x_images)
    np.save('dataset/y_segmentation.npy', y_masks)
    
    print(f"Created segmentation dataset: X shape {x_images.shape}, Y shape {y_masks.shape}")
    return x_images, y_masks

import tensorflow as tf
from tensorflow.keras.metrics import Metric

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

from tensorflow.keras.layers import Input, Conv2D, MaxPooling2D, Conv2DTranspose, concatenate
from tensorflow.keras.models import Model

def build_unet_model(input_shape=(90, 90, 1)):
    inputs = Input(input_shape)
    
    c1 = Conv2D(16, (3, 3), activation='relu', padding='same')(inputs)
    c1 = Conv2D(16, (3, 3), activation='relu', padding='same')(c1)
    p1 = MaxPooling2D((2, 2))(c1)
    
    c2 = Conv2D(32, (3, 3), activation='relu', padding='same')(p1)
    c2 = Conv2D(32, (3, 3), activation='relu', padding='same')(c2)
    p2 = MaxPooling2D((2, 2))(c2)
    
    c3 = Conv2D(64, (3, 3), activation='relu', padding='same')(p2)
    c3 = Conv2D(64, (3, 3), activation='relu', padding='same')(c3)
    p3 = MaxPooling2D((2, 2))(c3)
    
    c4 = Conv2D(128, (3, 3), activation='relu', padding='same')(p3)
    c4 = Conv2D(128, (3, 3), activation='relu', padding='same')(c4)
    
    u5 = Conv2DTranspose(64, (2, 2), strides=(2, 2), padding='same')(c4)
    u5 = concatenate([u5, c3])
    c5 = Conv2D(64, (3, 3), activation='relu', padding='same')(u5)
    c5 = Conv2D(64, (3, 3), activation='relu', padding='same')(c5)
    
    u6 = Conv2DTranspose(32, (2, 2), strides=(2, 2), padding='same')(c5)
    diff_y = c2.shape[1] - u6.shape[1]
    diff_x = c2.shape[2] - u6.shape[2]
    if diff_y > 0 or diff_x > 0:
        from tensorflow.keras.layers import Cropping2D
        c2 = Cropping2D(cropping=((0, diff_y), (0, diff_x)))(c2)
    u6 = concatenate([u6, c2])
    c6 = Conv2D(32, (3, 3), activation='relu', padding='same')(u6)
    c6 = Conv2D(32, (3, 3), activation='relu', padding='same')(c6)
    
    u7 = Conv2DTranspose(16, (2, 2), strides=(2, 2), padding='same')(c6)
    diff_y = c1.shape[1] - u7.shape[1]
    diff_x = c1.shape[2] - u7.shape[2]
    if diff_y > 0 or diff_x > 0:
        from tensorflow.keras.layers import Cropping2D
        c1 = Cropping2D(cropping=((0, diff_y), (0, diff_x)))(c1)
    u7 = concatenate([u7, c1])
    c7 = Conv2D(16, (3, 3), activation='relu', padding='same')(u7)
    c7 = Conv2D(16, (3, 3), activation='relu', padding='same')(c7)
    
    outputs = Conv2D(1, (1, 1), activation='sigmoid')(c7)
    if outputs.shape[1] != input_shape[0] or outputs.shape[2] != input_shape[1]:
        from tensorflow.keras.layers import ZeroPadding2D
        outputs = ZeroPadding2D(padding=((1, 1), (1, 1)))(outputs)
    
    model = Model(inputs=[inputs], outputs=[outputs])
    model.compile(
        optimizer='adam',
        loss='binary_crossentropy',
        metrics=[IoU(name='IoU')]
    )
    
    return model




def evaluate_segmentation(model, x_val, y_val, num_samples=5):
    """
    Evaluate the segmentation model and visualize results.
    """
    y_pred = model.predict(x_val)
    
    iou_scores = []
    for i in range(len(y_val)):
        true = y_val[i].squeeze()
        pred = (y_pred[i].squeeze() > 0.5).astype(np.float32)
        
        intersection = np.sum(true * pred)
        union = np.sum(true) + np.sum(pred) - intersection
        iou = intersection / (union + 1e-7)
        iou_scores.append(iou)
    
    mean_iou = np.mean(iou_scores)
    print(f"Mean IoU on validation set: {mean_iou:.4f}")
    
    indices = np.random.choice(range(len(x_val)), num_samples, replace=False)
    return mean_iou
from sklearn.model_selection import train_test_split
from tensorflow.keras.callbacks import ModelCheckpoint, EarlyStopping
def train_segmentation_model():
    """
    Train a U-Net model for fingerprint region segmentation.
    """
    x_images, y_masks = load_segmentation_dataset()
    
    x_images = x_images.astype(np.float32) / 255.0
    
    x_train, x_val, y_train, y_val = train_test_split(
        x_images, y_masks, test_size=0.2, random_state=42
    )
    
    model = build_unet_model(input_shape=(90, 90, 1))
    model.summary()
    
    checkpoint = ModelCheckpoint(
        'fingerprint_segmentation_model.h5',
        monitor='val_custom_iou',
        mode='max',
        save_best_only=True,
        verbose=1
    )
    
    early_stopping = EarlyStopping(
        monitor='val_custom_iou',
        patience=10,
        restore_best_weights=True,
        mode='max',
        verbose=1
    )
    
    history = model.fit(
        x_train, y_train,
        batch_size=32,
        epochs=5,
        validation_data=(x_val, y_val),
        callbacks=[checkpoint, early_stopping]
    )
    
    model.save('fingerprint_segmentation_model.h5')
    
    
    
    
    evaluate_segmentation(model, x_val, y_val)
    
    return model





x_data = np.concatenate([data_dict['easy'][0], data_dict['medium'][0], data_dict['hard'][0]], axis=0)
label_data = np.concatenate([data_dict['easy'][1], data_dict['medium'][1], data_dict['hard'][1]], axis=0)

x_train, x_val, label_train, label_val = train_test_split(x_data, label_data, test_size=0.1)
print(f"x_train: {x_train.shape}, label_train: {label_train.shape}")
print(f"x_val: {x_val.shape}, label_val: {label_val.shape}")





def preview_augmentation(images):
    augmented_images = []
    
    for _ in range(9):
        img = images[0].copy()
        
        blur_strength = random.uniform(0, 0.5)
        if blur_strength > 0:
            blur_size = int(blur_strength * 10) * 2 + 1
            img = cv2.GaussianBlur(img, (blur_size, blur_size), blur_strength)
        
        h, w = img.shape[:2]
        
        scale_x = random.uniform(0.9, 1.1)
        scale_y = random.uniform(0.9, 1.1)
        
        tx = random.uniform(-0.1, 0.1) * w
        ty = random.uniform(-0.1, 0.1) * h
        
        angle = random.uniform(-30, 30)
        
        center = (w // 2, h // 2)
        M = cv2.getRotationMatrix2D(center, angle, 1.0)
        M[0, 0] *= scale_x
        M[1, 1] *= scale_y
        M[0, 2] += tx
        M[1, 2] += ty
        
        img = cv2.warpAffine(img, M, (w, h), borderMode=cv2.BORDER_CONSTANT, borderValue=255)
        
        augmented_images.append(img)
    
label_real_dict = {''.join(y.astype(str)).zfill(6): i for i, y in enumerate(data_dict['real'][1])}

class DataGenerator(keras.utils.Sequence):
    def __init__(self, x, label, x_real, label_real_dict, batch_size=32, shuffle=True):
        self.x = x
        self.label = label
        self.x_real = x_real
        self.label_real_dict = label_real_dict
        self.batch_size = batch_size
        self.shuffle = shuffle
        self.on_epoch_end()
    
    def __len__(self):
        return len(self.x) // self.batch_size
    
    def __getitem__(self, index):
        x1_batch = self.x[index * self.batch_size:(index + 1) * self.batch_size].copy()
        label_batch = self.label[index * self.batch_size:(index + 1) * self.batch_size]
        
        x2_batch = np.empty((self.batch_size, 90, 90, 1), dtype=np.float32)
        y_batch = np.zeros((self.batch_size, 1), dtype=np.float32)
        
        if self.shuffle:
            for i in range(len(x1_batch)):
                pass
        
        for i, l in enumerate(label_batch):
            match_key = ''.join(l.astype(str)).zfill(6)
            if random.random() > 0.5:
                x2_batch[i] = self.x_real[self.label_real_dict[match_key]][..., np.newaxis]
                y_batch[i] = 1.
            else:
                while True:
                    unmatch_key, unmatch_idx = random.choice(list(self.label_real_dict.items()))
                    if unmatch_key != match_key:
                        x2_batch[i] = self.x_real[unmatch_idx][..., np.newaxis]
                        break
                y_batch[i] = 0.
        
        x_input = (
            x1_batch.astype(np.float32) / 255., 
            x2_batch.astype(np.float32) / 255.
        )
        
        return x_input, y_batch
    
    def on_epoch_end(self):
        if self.shuffle:
            self.x, self.label = shuffle(self.x, self.label)





from tensorflow.keras.layers import Input, Conv2D, MaxPooling2D, Flatten, Dense, Dropout, BatchNormalization, Subtract
from tensorflow.keras.models import Model

def create_base_network(input_shape):
    """
    Function to create the base network (twin network) for the Siamese architecture.
    """
    input = Input(shape=input_shape)
    
    x = Conv2D(32, (3, 3), activation='relu', padding='same')(input)
    x = MaxPooling2D(pool_size=(2, 2))(x)
    x = Dropout(0.25)(x)
    
    x = Conv2D(64, (3, 3), activation='relu', padding='same')(x)
    x = MaxPooling2D(pool_size=(2, 2))(x)
    x = Dropout(0.25)(x)

    x = Flatten()(x)

    return Model(input, x)

input_shape = (90, 90, 1)

base_network = create_base_network(input_shape)

input_a = Input(shape=input_shape)
input_b = Input(shape=input_shape)

processed_a = base_network(input_a)
processed_b = base_network(input_b)

subtracted = Subtract()([processed_a, processed_b])

x = Dense(128, activation='relu')(subtracted)
x = Dropout(0.5)(x)

output = Dense(1, activation='sigmoid')(x)

model = Model(inputs=[input_a, input_b], outputs=output)

model.compile(loss='binary_crossentropy', optimizer='adam', metrics=['accuracy'])

model.summary()





seg_model = train_segmentation_model()




train_gen = DataGenerator(x_train, label_train, data_dict['real'][0], label_real_dict, shuffle=True)
val_gen = DataGenerator(x_val, label_val, data_dict['real'][0], label_real_dict, shuffle=False)

history = model.fit(
    train_gen,
    epochs=15,
    validation_data=val_gen
)
def extract_fingerprint_region(image, seg_model, padding=10):
    original_image = image.copy()
    
    try:
        if len(image.shape) == 2:
            input_img = np.expand_dims(image, axis=-1)
        else:
            input_img = image
            
        
        try:
            model_input_shape = seg_model.inputs[0].shape.as_list()[1:3]
        except AttributeError:
            model_input_shape = seg_model.inputs[0].shape[1:3]
        if model_input_shape[0] is not None and model_input_shape[1] is not None:
            input_img_resized = cv2.resize(input_img, (model_input_shape[1], model_input_shape[0]))
            input_img_resized = np.expand_dims(input_img_resized, axis=0)
        else:
            input_img_resized = np.expand_dims(input_img, axis=0)
        
        prediction = seg_model.predict(input_img_resized)
        
        if len(prediction.shape) == 4:
            mask = prediction[0]
            
            if mask.shape[-1] > 1:
                mask = np.argmax(mask, axis=-1)
                binary_mask = (mask == 1).astype(np.uint8) * 255
            else:
                binary_mask = (mask[:, :, 0] > 0.5).astype(np.uint8) * 255
        else:
            raise ValueError("Unexpected model output format")
            
        if model_input_shape[0] is not None and model_input_shape[1] is not None:
            if binary_mask.shape[:2] != image.shape[:2]:
                binary_mask = cv2.resize(binary_mask, (image.shape[1], image.shape[0]))
                
    except Exception as e:
        print(f"Segmentation model error: {e}. Using fallback thresholding method.")
        if len(image.shape) == 2:
            gray = image
        else:
            gray = image[:,:,0]
        
        if gray.max() <= 1.0:
            gray_uint8 = (gray * 255).astype(np.uint8)
        else:
            gray_uint8 = gray.astype(np.uint8)
            
        binary_mask = cv2.adaptiveThreshold(
            gray_uint8, 
            255, 
            cv2.ADAPTIVE_THRESH_GAUSSIAN_C, 
            cv2.THRESH_BINARY_INV, 
            11, 
            2
        )
    
    kernel = np.ones((3, 3), np.uint8)
    binary_mask = cv2.morphologyEx(binary_mask, cv2.MORPH_CLOSE, kernel, iterations=2)
    binary_mask = cv2.morphologyEx(binary_mask, cv2.MORPH_OPEN, kernel, iterations=1)
    
    contours, _ = cv2.findContours(binary_mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    
    if not contours:
        h, w = image.shape[:2]
        return image, (0, 0, w, h)
    
    largest_contour = max(contours, key=cv2.contourArea)
    
    x, y, w, h = cv2.boundingRect(largest_contour)
    
    x_min = max(0, x - padding)
    y_min = max(0, y - padding)
    x_max = min(original_image.shape[1], x + w + padding)
    y_max = min(original_image.shape[0], y + h + padding)
    
    if len(original_image.shape) == 2:
        cropped_image = original_image[y_min:y_max, x_min:x_max]
    else:
        cropped_image = original_image[y_min:y_max, x_min:x_max, :]
    
    return cropped_image, (x_min, y_min, x_max, y_max)


import pickle
def evaluate_combined_pipeline(fingerprint_model, seg_model, x_val, label_val, data_dict, label_real_dict, num_samples=5):
    """
    Evaluate the complete fingerprint matching pipeline with region detection.
    """
    results = []
    
    indices = np.random.choice(range(len(x_val)), num_samples, replace=False)
    
    for i, idx in enumerate(indices):
        input_img = x_val[idx].copy()
        match_key = ''.join(label_val[idx].astype(str)).zfill(6)
        
        matched_img = data_dict['real'][0][label_real_dict[match_key]].copy()
        
        while True:
            unmatch_key, unmatch_idx = random.choice(list(label_real_dict.items()))
            if unmatch_key != match_key:
                unmatched_img = data_dict['real'][0][unmatch_idx].copy()
                break
        
        input_normalized = input_img.astype(np.float32) / 255.0
        input_segmented, input_bbox = extract_fingerprint_region(input_normalized, seg_model)
        input_segmented = input_segmented.reshape(1, 90, 90, 1)
        
        matched_normalized = matched_img.astype(np.float32) / 255.0
        matched_segmented, matched_bbox = extract_fingerprint_region(matched_normalized, seg_model)
        matched_segmented = matched_segmented.reshape(1, 90, 90, 1)
        
        unmatched_normalized = unmatched_img.astype(np.float32) / 255.0
        unmatched_segmented, unmatched_bbox = extract_fingerprint_region(unmatched_normalized, seg_model)
        unmatched_segmented = unmatched_segmented.reshape(1, 90, 90, 1)
        
        pred_matched = fingerprint_model.predict([input_segmented, matched_segmented])[0][0]
        pred_unmatched = fingerprint_model.predict([input_segmented, unmatched_segmented])[0][0]
        
        results.append({
            'input_id': idx,
            'match_score': pred_matched,
            'unmatch_score': pred_unmatched,
            'correct': pred_matched > 0.5 and pred_unmatched < 0.5
        })
    
    
    correct_predictions = sum(1 for r in results if r['correct'])
    accuracy = correct_predictions / len(results)
    print(f"Pipeline accuracy on sample: {accuracy:.2f}")
    
    return results

with open('../fingerprint_models/model_performance.pkl', 'wb') as f:
    pickle.dump({
        'history_enhanced': history.history
    }, f)

evaluate_combined_pipeline(model, seg_model, x_val, label_val, data_dict, label_real_dict)




