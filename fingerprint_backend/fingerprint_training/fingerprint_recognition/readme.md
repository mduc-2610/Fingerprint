# Fingerprint Recognition Project - Model Directory
This is the directory where trained models are saved.

This directory contains the trained models for the **Fingerprint Recognition Project** using the **Sokoto Coventry Fingerprint Dataset (SOCOFing)**. The models stored here are used to recognize and classify fingerprints based on the dataset provided by Kaggle.

## Table of Contents
- [Model Architecture](#model-architecture)
- [Dependencies](#dependencies)
- [Dataset](#dataset)
- [Training Details](#training-details)
- [Model Usage](#model-usage)

## Model Architecture
The following diagram illustrates the architecture of the fingerprint recognition model that was used in this project:
![image](https://github.com/user-attachments/assets/88834915-4b61-4e11-b767-08054ed02abc)

The model leverages a **Convolutional Neural Network (CNN)** designed to classify fingerprints based on various categories, such as gender, hand (left/right), and fingerprint class.

Key layers include:
- Convolutional layers for feature extraction.
- Max-pooling layers to reduce dimensionality.
- Fully connected layers for classification.
- Softmax output layer for final predictions.

## Dependencies

The following libraries and frameworks were used to train and evaluate the model:

- **Python**: General programming language used for the project.
- **NumPy**: For numerical computations and handling of arrays.
- **Keras**: The deep learning library used to build and train the CNN model.
- **Matplotlib**: For visualizing training history and model performance metrics.
- **scikit-learn (sklearn)**: For evaluation metrics and splitting data.
- **imgaug**: For augmenting fingerprint images during training to improve model robustness.

Make sure to install these dependencies using the following command:
```bash
pip install numpy keras matplotlib scikit-learn imgaug

