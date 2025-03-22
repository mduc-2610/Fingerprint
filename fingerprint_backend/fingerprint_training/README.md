# Fingerprint Recognition with SOCOFing Dataset

This repository contains various solutions for fingerprint recognition using the SOCOFing dataset from Kaggle. We employ Convolutional Neural Networks (CNNs) with Python and Keras to build models capable of identifying and verifying fingerprints. The project is organized as a series of Jupyter notebooks, each addressing different aspects and techniques in fingerprint recognition.

## Dataset

The dataset used in this project is the [SOCOFing](https://www.kaggle.com/datasets/ruizgara/socofing) dataset, which consists of fingerprint images. It is designed for developing and testing fingerprint recognition systems and algorithms.

## Project Structure

- **notebooks/**: Contains Jupyter notebooks for different stages and solutions of the project.
- **models/**: Directory where trained models are saved.
- **data/**: Placeholder directory for the dataset. The dataset can be downloaded from Kaggle page [SOCOFing](https://www.kaggle.com/datasets/ruizgara/socofing) or extracted here.
- **README.md**: This file.
- **requirements.txt**: List of dependencies required for this project.

## Installation

1. **Clone the repository:**

   ```bash
   git clone https://github.com/realivanivani/fingerprint-recognition.git
   cd fingerprint-recognition
   ```

2. **Set up a virtual environment (optional but recommended):**

   ```bash
   python3 -m venv venv
   source venv/bin/activate  # On Windows, use `venv\Scripts\activate`
   ```

3. **Install the required packages:**

   ```bash
   pip install -r requirements.txt
   ```

4. **Download the dataset:**

   - Download the SOCOFing dataset from [Kaggle](https://www.kaggle.com/datasets/ruizgara/socofing).
   - Extract the dataset into the `data/` directory.

## Notebooks

The project is organized into a series of Jupyter notebooks. Below is a brief description of each:

1. **01_Data_Preprocessing.ipynb:** 
   - Loads and preprocesses the fingerprint images.
   - Performs data augmentation to increase dataset variability.

2. **02_Model.ipynb:** 
   - Builds and trains CNN models using Keras.
   - Evaluates model performance and optimizes hyperparameters.

3. **03_Model_Validation_and_Summary.ipynb:**
   - Tests the model on validation data.
   - Analyzes results using metrics such as accuracy, precision, recall, and F1-score.
   - Explores additional approaches and solutions.
   - Compares performance with the main solution.

## Usage

To run the notebooks, you need to have Jupyter Notebook installed (Anaconda perhaps) or use a code interpreter like Visual Studio Code.
Open the desired notebook in your web browser and run the cells to see the code execution and outputs.

## Contributing

This project is a collaborative effort. If you have suggestions or improvements, feel free to fork the repository and submit a pull request. We welcome all contributions!

## License

This project is not licensed

## Contact

For any questions or issues, please contact [Ivan Ivani] at [jasamivanivani@gmail.com].

---

