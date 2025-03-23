import numpy as np
import matplotlib.pyplot as plt
import keras
from keras import layers
from keras.models import Model
from sklearn.utils import shuffle
from sklearn.model_selection import train_test_split
import random
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
def visualize_samples(data_dict):
    plt.figure(figsize=(15, 10))
    for idx, (key, (x, y)) in enumerate(data_dict.items(), start=1):
        plt.subplot(1, 4, idx)
        plt.title(y[0])
        plt.imshow(x[0].squeeze(), cmap='gray')
    plt.show()
visualize_samples(data_dict)
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
            blur_size = int(blur_strength * 10) * 2 + 1  # Must be odd number
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
    plt.figure(figsize=(16, 6))
    plt.subplot(2, 5, 1)
    plt.title('Original')
    plt.imshow(images[0].squeeze(), cmap='gray')
    for i, aug in enumerate(augmented_images):
        plt.subplot(2, 5, i + 2)
        plt.title(f'Aug {i + 1}')
        plt.imshow(aug.squeeze(), cmap='gray')
    plt.show()
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
                # Augmentation code as provided earlier
                img = x1_batch[i].copy()
                blur_strength = random.uniform(0, 0.5)
                if blur_strength > 0:
                    blur_size = int(blur_strength * 10) * 2 + 1  # Must be odd number
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
                x1_batch[i] = img
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
train_gen = DataGenerator(x_train, label_train, data_dict['real'][0], label_real_dict, shuffle=True)
val_gen = DataGenerator(x_val, label_val, data_dict['real'][0], label_real_dict, shuffle=False)
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
    # Second Convolutional Block
    x = Conv2D(64, (3, 3), activation='relu', padding='same')(x)
    x = MaxPooling2D(pool_size=(2, 2))(x)
    x = Dropout(0.25)(x)
    # Flatten the feature map
    x = Flatten()(x)
    return Model(input, x)
# Define the input shape for the images
input_shape = (90, 90, 1)
# Create the twin networks (base networks with shared weights)
base_network = create_base_network(input_shape)
# Define two input tensors for the two images
input_a = Input(shape=input_shape)
input_b = Input(shape=input_shape)
# Process each image through the shared twin network
processed_a = base_network(input_a)
processed_b = base_network(input_b)
# Compute the absolute difference between the feature vectors
subtracted = Subtract()([processed_a, processed_b])
# Further processing of the subtracted feature using another convolutional layer
x = Dense(128, activation='relu')(subtracted)
x = Dropout(0.5)(x)
# Output layer with sigmoid activation for binary classification
output = Dense(1, activation='sigmoid')(x)
# Create the full Siamese network model
model = Model(inputs=[input_a, input_b], outputs=output)
# Compile the model
model.compile(loss='binary_crossentropy', optimizer='adam', metrics=['accuracy'])
model.summary()
history = model.fit(
    train_gen,
    epochs=15,
    validation_data=val_gen
)
def evaluate_model(model, x_val, label_val):
    random_idx = random.randint(0, len(x_val))
    random_img = x_val[random_idx].reshape((1, 90, 90, 1)).astype(np.float32) / 255.
    match_key = ''.join(label_val[random_idx].astype(str)).zfill(6)
    matched_img = data_dict['real'][0][label_real_dict[match_key]].reshape((1, 90, 90, 1)).astype(np.float32) / 255.
    pred_matched = model.predict([random_img, matched_img])[0][0]
    unmatch_key, unmatch_idx = random.choice(list(label_real_dict.items()))
    unmatched_img = data_dict['real'][0][unmatch_idx].reshape((1, 90, 90, 1)).astype(np.float32) / 255.
    pred_unmatched = model.predict([random_img, unmatched_img])[0][0]
    plt.figure(figsize=(8, 4))
    plt.subplot(1, 3, 1)
    plt.title(f'Input: {label_val[random_idx]}')
    plt.imshow(random_img.squeeze(), cmap='gray')
    plt.subplot(1, 3, 2)
    plt.title(f'Match: {pred_matched:.2f}')
    plt.imshow(matched_img.squeeze(), cmap='gray')
    plt.subplot(1, 3, 3)
    plt.title(f'Unmatch: {pred_unmatched:.2f}')
    plt.imshow(unmatched_img.squeeze(), cmap='gray')
    plt.show()
evaluate_model(model, x_val, label_val)

# Modify and supplement the code according to the following requirements:

# Add training for a fingerprint region detection model based on the existing training process (if not already included in the code).
# Proposed model: Use CNN (Convolutional Neural Network) or a segmentation model such as U-Net or Mask R-CNN to detect the fingerprint region.
# Data: You need a dataset of original fingerprint images along with labels for the fingerprint region (bounding box or mask).
# Training process:
# Label the fingerprint regions in the images (if not already labeled).
# Use a detection/segmentation model to identify the fingerprint region.
# Evaluate accuracy using IoU (Intersection over Union).
# After training, use this model to preprocess input images before passing them to the recognition step.
# Strategy:
# Use the first model to detect the fingerprint region in an image.
# Use the detected fingerprint region as input for the recognition model.
# The models should be saved using TensorFlow or an appropriate format and their accuracy should be measured (ensure proper handling of different sizes to avoid conflicts).



# write a section to store models to fingerprint_models folder

# this is the folder structure
# fingerprint_models
# fingerrint_recognition
#    01_DataPreprocessing.ipynb
#    02_Model_with_segmentation.ipynb

# then calculate accuracy then give me a data that will be fingerprint_recognition_models.json a list of recognition_model you 've used
# fingerprint_segmentation_models.json a list of segmentation model
# each model data will be like this
# id
# name
# version (option)
# accuracy
# createdAt
# updatedAt

# sửa và bổ sung code trên theo các yêu cầu sau 
#  Bổ sung Huấn luyện mô hình nhận dạng vùng chứa vân tay trong ảnh dựa vào phần huấn luyện trên (nếu trong code chưa có)
# * Mô hình đề xuất: Dùng CNN (Convolutional Neural Network) hoặc mô hình segmentation như U-Net, Mask R-CNN để phát hiện vùng vân tay.
# * Dữ liệu: Bạn cần tập ảnh vân tay gốc và nhãn cho vùng vân tay (bounding box hoặc mask).
# * Quy trình huấn luyện:
#    * Gán nhãn vùng vân tay trong ảnh (nếu chưa có).
#    * Dùng mô hình phát hiện/segment để xác định vùng vân tay.
#    * Kiểm tra độ chính xác bằng IoU (Intersection over Union).
#    * Sau khi huấn luyện, sử dụng mô hình này để tiền xử lý ảnh đầu vào trước khi đưa vào bước nhận diện.
# Đây là chiến lược
# * Dùng mô hình đầu tiên để tìm vùng chứa vân tay trong ảnh.
# * Dùng vùng vân tay này để đưa vào mô hình nhận diện.

# Các model sẽ được lưu lại dùng tf hoặc cái phù hợp để lưu 
# và tính toán độ chính xác của các model (Hãy chú ý về độ thích hợp của các kích cỡ tránh xung đột)


# Follow this instruction
# * technology will be used python for training and store model, 
# * java for backend and simple ui i got these models (the image i provide)
#    - integrate the python code i provide you to your generated code
#    - When adding a new fingerprint for employee (for recognition modul), update the fingerprint models by storing the new fingerprint in the fingerprint_adapting_dataset along with the employee_id.
#    - This ensures the models can adapt to new fingerprints for accurate matching.
# fingerprint_training/
# │   ├── fingerprint_adapting_dataset/ (employee fingerprint registered store here)
# │   │   ├── 101/ (id then fingerprint image file inside tif or bmp,  )
# │   │   ├── 102/
# │   │   └── ...
# │   │    fingerprint_models
# │   │   ├── recognition
# │   │   ├──------siamese_network.keras
# │   │   ├──segmentation
# │   │   ├──── unet_segmentation.keras
# │   │    fingerprint_recognition
# │   │   ├── model_with_segmentation.ipynb
# │   │   ├── dataset
# │   │   ├──── x_easy.npy
# │   │   ├──── x_hard.npy
# │   │    





# technology willbe used python for training and store model, java for backend and simple ui i got these models (the image i provide) When an adds a new fingerprint for employee (for recognition modul), update the fingerprint models by storing the new fingerprint in the fingerprint_adapting_dataset along with the employee_id. This ensures the models can adapt to new fingerprints for accurate matching. Employee Fingerprint Registration Module 
# fingerprint_training/
# │   ├── fingerprint_adapting_dataset/ (employee fingerprint registered store here)
# │   │   ├── 101/ (id then fingerprint image file inside tif or bmp,  )
# │   │   ├── 102/
# │   │   └── ...
# │   │    fingerprint_models
# │   │   ├── recognition
# │   │   ├──------siamese_network.keras
# │   │   ├──segmentation
# │   │   ├──── unet_segmentation.keras
# │   │    fingerprint_recognition
# │   │   ├── model_with_segmentation.ipynb
# │   │   ├── dataset
# │   │   ├──── x_easy.npy
# │   │   ├──── x_hard.npy
# │   │    \
#  1. U-Net Segmentation Model Architecture Your U-Net model is designed for fingerprint segmentation - separating the fingerprint from the background in images. Architecture Details: Input: 90×90×1 grayscale fingerprint images Encoder (Contracting) Path: First block: Two Conv2D layers (16 filters) → MaxPooling (reduces to 45×45) Second block: Two Conv2D layers (32 filters) → MaxPooling (reduces to 22×22) Third block: Two Conv2D layers (64 filters) → MaxPooling (reduces to 11×11) Bottleneck: Two Conv2D layers (128 filters) Decoder (Expanding) Path: First upsampling: Conv2DTranspose (64 filters) → Concatenate with encoder features Two Conv2D layers (64 filters) Second upsampling: Conv2DTranspose (32 filters) → Concatenate with encoder features Two Conv2D layers (32 filters) Third upsampling: Conv2DTranspose (16 filters) → Concatenate with encoder features Two Conv2D layers (16 filters) Final Conv2D layer (1 filter) for output mask ZeroPadding to restore original 90×90 dimensions Skip Connections: Connect corresponding layers between encoder and decoder Total Parameters: ~480K parameters Purpose: The segmentation model isolates the fingerprint from background noise, which dramatically improves recognition accuracy. The clean, segmented fingerprints are then fed to the recognition model. 2. Siamese Network Recognition Model Your Siamese network determines if two fingerprints belong to the same person. Architecture Details: Dual Inputs: Two 90×90×1 grayscale fingerprint images Shared Feature Extraction: Both inputs go through identical convolutional layers with shared weights Feature Processing: Output of shared network is a 30,976-dimensional feature vector for each input Subtract feature vectors to compute a difference vector Decision Layers: Dense layer (128 neurons) processes the difference vector Dropout layer (reduces overfitting) Final Dense layer (1 neuron) with sigmoid activation outputs a similarity score Total Parameters: ~4M parameters


# localhost:8080/api/fingerprint-segmentation
#
# [{"id":"e9f4df67-7eb0-45d0-8e6f-87cd84abad11","name":"UNet Segmentation v1_0","pathName":"unet_segmentation_v1_0","accuracy":0.994081,"valAccuracy":0.995765,"version":"1.0","createdAt":"2025-03-23T14:46:13.276231","updatedAt":"2025-03-23T14:46:13.276231"}]
# http://localhost:8080/api/fingerprint-recognition
# [{"id":"76fe3632-60b3-491d-a82e-75a4e892969a","name":"Siamese Network v1_0","pathName":"siamese_network_v1_0","accuracy":0.950703,"valAccuracy":0.953902,"version":"1.0","createdAt":"2025-03-23T14:46:13.442135","updatedAt":"2025-03-23T14:46:13.442135"},]
#


# # implement choose model to train 
# about fingerprint_recognition.py you just need to 
# specify --seg-path-name and --reg-path-name to make it train by the model you want