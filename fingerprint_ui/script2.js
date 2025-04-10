// Global variables
let employees = [];
let selectedEmployeeId = null;
let segmentationModels = [];
let recognitionModels = [];

// Fetch data on page load
document.addEventListener('DOMContentLoaded', function() {
    fetchEmployees();
    fetchSegmentationModels();
    fetchRecognitionModels();
    
    // Show the initial "no employee selected" message
    document.getElementById('noEmployeeSelected').classList.remove('d-none');
});

// Function to fetch employees from API
function fetchEmployees() {
    // Show loading indicator
    document.getElementById('loadingIndicator').style.display = 'block';
    
    fetch('http://localhost:8080/api/employee')
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            // Hide loading indicator
            document.getElementById('loadingIndicator').style.display = 'none';
            
            // Store employees data
            employees = data;
            
            // Populate the table
            populateEmployeeTable(employees);
        })
        .catch(error => {
            // Hide loading indicator
            document.getElementById('loadingIndicator').style.display = 'none';
            
            console.error('Error fetching employees:', error);
            // Show error message in the employee table
            document.getElementById('employeeTableBody').innerHTML = `
                <tr>
                    <td colspan="4" class="text-center text-danger">
                        Failed to load employees. ${error.message}
                    </td>
                </tr>
            `;
        });
}

// Function to fetch segmentation models
function fetchSegmentationModels() {
    fetch('http://localhost:8080/api/fingerprint-segmentation')
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            // Store segmentation models data
            segmentationModels = data;
            
            // Populate the models table
            populateSegmentationModelsTable(segmentationModels);
            
            // Populate the segmentation model dropdowns
            populateSegmentationModelDropdowns(segmentationModels);
        })
        .catch(error => {
            console.error('Error fetching segmentation models:', error);
            // Show error message in the segmentation model table
            document.getElementById('segmentationModelTableBody').innerHTML = `
                <tr>
                    <td colspan="3" class="text-center text-danger">
                        Failed to load segmentation models. ${error.message}
                    </td>
                </tr>
            `;
        });
}

// Function to fetch recognition models
function fetchRecognitionModels() {
    fetch('http://localhost:8080/api/fingerprint-recognition')
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            // Store recognition models data
            recognitionModels = data;
            
            // Populate the models table
            populateRecognitionModelsTable(recognitionModels);
            
            // Populate the recognition model dropdown
            populateRecognitionModelDropdown(recognitionModels, 'recognitionModel');
            populateRecognitionModelDropdown(recognitionModels, 'recognitionModelRegister');
        })
        .catch(error => {
            console.error('Error fetching recognition models:', error);
            // Show error message in the recognition model table
            document.getElementById('recognitionModelTableBody').innerHTML = `
                <tr>
                    <td colspan="3" class="text-center text-danger">
                        Failed to load recognition models. ${error.message}
                    </td>
                </tr>
            `;
        });
}

// Function to populate employee table
function populateEmployeeTable(employeeList) {
    const tableBody = document.getElementById('employeeTableBody');
    tableBody.innerHTML = '';
    
    if (employeeList.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="4" class="text-center">No employees found</td>
            </tr>
        `;
        return;
    }
    
    employeeList.forEach(employee => {
        const row = document.createElement('tr');
        row.className = 'selectable-row';
        row.dataset.employeeId = employee.id;
        
        // Add selected class if this employee is the selected one
        if (employee.id === selectedEmployeeId) {
            row.classList.add('selected');
        }
        
        row.innerHTML = `
            <td>
                <img src="${employee.photo || 'https://th.bing.com/th/id/OIP.AtJhqN3-No3fmZqKdpw17QHaHa?rs=1&pid=ImgDetMain'}" 
                     alt="${employee.fullName}" 
                     class="employee-photo"
                     onerror="this.src='https://th.bing.com/th/id/OIP.AtJhqN3-No3fmZqKdpw17QHaHa?rs=1&pid=ImgDetMain'">
            </td>
            <td>${employee.id}</td>
            <td>${employee.fullName}</td>
            <td>${employee.phoneNumber || '-'}</td>
        `;
        
        // Add click event to select employee
        row.addEventListener('click', function() {
            selectEmployee(employee);
        });
        
        tableBody.appendChild(row);
    });
}

// Function to populate segmentation models table
function populateSegmentationModelsTable(modelList) {
    const tableBody = document.getElementById('segmentationModelTableBody');
    tableBody.innerHTML = '';
    
    if (modelList.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="3" class="text-center">No segmentation models found</td>
            </tr>
        `;
        return;
    }
    
    modelList.forEach(model => {
        const row = document.createElement('tr');
        
        // Calculate accuracy for display as percentage
        const accuracy = (model.accuracy * 100).toFixed(2);
        const valAccuracy = (model.valAccuracy * 100).toFixed(2);
        
        // Set badge color based on accuracy
        let badgeClass = 'bg-danger';
        if (accuracy > 95) {
            badgeClass = 'bg-success';
        } else if (accuracy > 90) {
            badgeClass = 'bg-warning text-dark';
        } else if (accuracy > 80) {
            badgeClass = 'bg-info text-dark';
        }
        
        const formattedDate = new Date(model.createdAt).toLocaleDateString();
        
        row.innerHTML = `
            <td>
                ${model.name}
                <div class="model-version">v${model.version}</div>
            </td>
            <td>
                <span class="badge ${badgeClass} badge-accuracy">${accuracy}%</span>
                <div class="model-version">Val: ${valAccuracy}%</div>
            </td>
            <td>${formattedDate}</td>
        `;
        
        tableBody.appendChild(row);
    });
}

// Function to populate recognition models table
function populateRecognitionModelsTable(modelList) {
    const tableBody = document.getElementById('recognitionModelTableBody');
    tableBody.innerHTML = '';
    
    if (modelList.length === 0) {
        tableBody.innerHTML = `
            <tr>
                <td colspan="3" class="text-center">No recognition models found</td>
            </tr>
        `;
        return;
    }
    
    modelList.forEach(model => {
        const row = document.createElement('tr');
        
        // Calculate accuracy for display as percentage
        const accuracy = (model.accuracy * 100).toFixed(2);
        const valAccuracy = (model.valAccuracy * 100).toFixed(2);
        
        // Set badge color based on accuracy
        let badgeClass = 'bg-danger';
        if (accuracy > 95) {
            badgeClass = 'bg-success';
        } else if (accuracy > 90) {
            badgeClass = 'bg-warning text-dark';
        } else if (accuracy > 80) {
            badgeClass = 'bg-info text-dark';
        }
        
        const formattedDate = new Date(model.createdAt).toLocaleDateString();
        
        row.innerHTML = `
            <td>
                ${model.name}
                <div class="model-version">v${model.version}</div>
            </td>
            <td>
                <span class="badge ${badgeClass} badge-accuracy">${accuracy}%</span>
                <div class="model-version">Val: ${valAccuracy}%</div>
            </td>
            <td>${formattedDate}</td>
        `;
        
        tableBody.appendChild(row);
    });
}

// Function to populate segmentation model dropdowns
function populateSegmentationModelDropdowns(modelList) {
    const registerSelect = document.getElementById('segmentationModel');
    const recognizeSelect = document.getElementById('segmentationModelRecognize');
    
    // Clear existing options
    registerSelect.innerHTML = '';
    recognizeSelect.innerHTML = '';
    
    if (modelList.length === 0) {
        const option = document.createElement('option');
        option.value = '';
        option.textContent = 'No segmentation models available';
        option.disabled = true;
        option.selected = true;
        
        registerSelect.appendChild(option.cloneNode(true));
        recognizeSelect.appendChild(option);
        return;
    }
    
    modelList.forEach(model => {
        const accuracy = (model.accuracy * 100).toFixed(2);
        
        const option = document.createElement('option');
        option.value = model.id;
        option.textContent = `${model.name} (${accuracy}% accuracy)`;
        
        registerSelect.appendChild(option.cloneNode(true));
        recognizeSelect.appendChild(option);
    });
    
    // Select the first model by default
    registerSelect.value = modelList[0].id;
    recognizeSelect.value = modelList[0].id;
}

// Function to populate recognition model dropdown
function populateRecognitionModelDropdown(modelList, id) {
    const select = document.getElementById(id);
    
    // Clear existing options
    select.innerHTML = '';
    
    if (modelList.length === 0) {
        const option = document.createElement('option');
        option.value = '';
        option.textContent = 'No recognition models available';
        option.disabled = true;
        option.selected = true;
        
        select.appendChild(option);
        return;
    }
    
    modelList.forEach(model => {
        const accuracy = (model.accuracy * 100).toFixed(2);
        
        const option = document.createElement('option');
        option.value = model.id;
        option.textContent = `${model.name} (${accuracy}% accuracy)`;
        
        select.appendChild(option);
    });
    
    // Select the first model by default
    select.value = modelList[0].id;
}

// Function to select an employee
function selectEmployee(employee) {
    // Update selected employee ID
    selectedEmployeeId = employee.id;
    
    // Update selected class in the table
    const rows = document.querySelectorAll('.selectable-row');
    rows.forEach(row => {
        if (row.dataset.employeeId === employee.id) {
            row.classList.add('selected');
        } else {
            row.classList.remove('selected');
        }
    });
    
    // Update the selected employee info card
    document.getElementById('selectedEmployeeId').textContent = employee.id;
    document.getElementById('selectedEmployeeFullName').textContent = employee.fullName;
    document.getElementById('selectedEmployeePhone').textContent = employee.phoneNumber || '-';
    document.getElementById('selectedEmployeeAddress').textContent = employee.address || '-';
    
    // Update photo with error handling
    const photoElement = document.getElementById('selectedEmployeePhoto');
    photoElement.src = employee.photo || 'https://th.bing.com/th/id/OIP.AtJhqN3-No3fmZqKdpw17QHaHa?rs=1&pid=ImgDetMain';
    photoElement.onerror = function() {
        this.src = 'https://th.bing.com/th/id/OIP.AtJhqN3-No3fmZqKdpw17QHaHa?rs=1&pid=ImgDetMain';
    };
    
    // Show the employee info card and hide the "no employee selected" message
    document.getElementById('noEmployeeSelected').classList.add('d-none');
    document.getElementById('employeeInfoCard').classList.remove('d-none');
}

// Handle employee search
document.getElementById('employeeSearch').addEventListener('input', function(event) {
    const searchTerm = event.target.value.toLowerCase();
    
    if (searchTerm.trim() === '') {
        populateEmployeeTable(employees);
        return;
    }
    
    const filteredEmployees = employees.filter(employee => 
        employee.id.toLowerCase().includes(searchTerm) || 
        employee.fullName.toLowerCase().includes(searchTerm) ||
        (employee.phoneNumber && employee.phoneNumber.toLowerCase().includes(searchTerm))
    );
    
    populateEmployeeTable(filteredEmployees);
});

// Handle file preview for registration
document.getElementById('fingerprintFiles').addEventListener('change', function(event) {
    const previewContainer = document.getElementById('previewContainer');
    previewContainer.innerHTML = '';
    
    const files = event.target.files;
    for (let i = 0; i < files.length; i++) {
        const file = files[i];
        const reader = new FileReader();
        
        reader.onload = function(e) {
            const col = document.createElement('div');
            col.className = 'col-md-4';
            
            const img = document.createElement('img');
            img.className = 'preview-image';
            img.src = e.target.result;
            img.alt = 'Fingerprint Preview';
            
            col.appendChild(img);
            previewContainer.appendChild(col);
        };
        
        reader.readAsDataURL(file);
    }
});

// Handle file preview for recognition
document.getElementById('fingerprintFile').addEventListener('change', function(event) {
    const previewContainer = document.getElementById('recognizePreview');
    previewContainer.innerHTML = '';
    
    if (event.target.files.length > 0) {
        const file = event.target.files[0];
        const reader = new FileReader();
        
        reader.onload = function(e) {
            const img = document.createElement('img');
            img.className = 'preview-image';
            img.src = e.target.result;
            img.alt = 'Fingerprint Preview';
            
            previewContainer.appendChild(img);
        };
        
        reader.readAsDataURL(file);
    }
});

// Handle registration form submission
document.getElementById('registerForm').addEventListener('submit', function(event) {
    event.preventDefault();
    
    if (!selectedEmployeeId) {
        alert('Please select an employee before registering fingerprints');
        return;
    }
    
    const segmentationModelId = document.getElementById('segmentationModel').value;
    const recognitionModelId = document.getElementById('recognitionModelRegister').value;
    if (!segmentationModelId) {
        alert('Please select a segmentation model');
        return;
    }
    
    const files = document.getElementById('fingerprintFiles').files;
    
    if (files.length < 1) {
        alert('Please select at least one fingerprint image');
        return;
    }
    
    const formData = new FormData();
    for (let i = 0; i < files.length; i++) {
        formData.append('files', files[i]);
    }
    formData.append('segmentationModelId', segmentationModelId);
    formData.append('recognitionModelId', recognitionModelId);
    
    // Show loading indicator
    document.getElementById('loadingIndicator').style.display = 'block';
    
    // Send API request using the selected employee ID
    fetch(`http://localhost:8080/api/fingerprint/register/${selectedEmployeeId}`, {
        method: 'POST',
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        // Hide loading indicator
        document.getElementById('loadingIndicator').style.display = 'none';
        
        // Show result
        const resultCard = document.getElementById('registerResult');
        const resultContent = document.getElementById('registerResultContent');
        
        resultCard.style.display = 'block';
        
        if (data.error) {
            resultContent.innerHTML = `
                <div class="alert alert-danger">
                    <h5>Registration Failed</h5>
                    <p>${data.error}</p>
                </div>
            `;
        } else {
            resultContent.innerHTML = `
                <div class="alert alert-success">
                    <h5>Registration Successful</h5>
                    <p>Employee ID: ${data.employeeId}</p>
                    <p>Fingerprints Registered: ${data.count}</p>
                    <p>Segmentation Model: ${getModelNameById(segmentationModels, segmentationModelId)}</p>
                </div>
            `;
        }
    })
    .catch(error => {
        // Hide loading indicator
        document.getElementById('loadingIndicator').style.display = 'none';
        
        // Show error
        const resultCard = document.getElementById('registerResult');
        const resultContent = document.getElementById('registerResultContent');
        
        resultCard.style.display = 'block';
        resultContent.innerHTML = `
            <div class="alert alert-danger">
                <h5>An Error Occurred</h5>
                <p>${error.message}</p>
            </div>
        `;
    });
});

// Handle recognition form submission
document.getElementById('recognizeForm').addEventListener('submit', function(event) {
    event.preventDefault();
    
    const recognitionModelId = document.getElementById('recognitionModel').value;
    if (!recognitionModelId) {
        alert('Please select a recognition model');
        return;
    }
    
    const segmentationModelId = document.getElementById('segmentationModelRecognize').value;
    if (!segmentationModelId) {
        alert('Please select a segmentation model');
        return;
    }
    
    const file = document.getElementById('fingerprintFile').files[0];
    
    if (!file) {
        alert('Please select a fingerprint image');
        alert('Please select a fingerprint image');
        return;
    }
    
    const formData = new FormData();
    formData.append('file', file);
    formData.append('recognitionModelId', recognitionModelId);
    formData.append('segmentationModelId', segmentationModelId);
    
    // Show loading indicator
    document.getElementById('loadingIndicator').style.display = 'block';
    
    // Send API request
    fetch('http://localhost:8080/api/fingerprint/recognize', {
        method: 'POST',
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        // Hide loading indicator
        document.getElementById('loadingIndicator').style.display = 'none';
        
        // Show result
        const resultCard = document.getElementById('recognizeResult');
        const resultContent = document.getElementById('recognizeResultContent');
        
        resultCard.style.display = 'block';
        
        if (data.error) {
            resultContent.innerHTML = `
                <div class="alert alert-danger">
                    <h5>Recognition Failed</h5>
                    <p>${data.error}</p>
                </div>
            `;
        } else if (data.employeeId) {
            // Find the recognized employee data
            const recognizedEmployee = employees.find(emp => emp.id === data.employeeId);
            
            if (recognizedEmployee) {
                resultContent.innerHTML = `
                    <div class="alert alert-success">
                        <h5>Employee Recognized!</h5>
                        <div class="d-flex align-items-center mb-2">
                            <img src="${recognizedEmployee.photo || 'https://th.bing.com/th/id/OIP.AtJhqN3-No3fmZqKdpw17QHaHa?rs=1&pid=ImgDetMain'}" 
                                 alt="${recognizedEmployee.fullName}" 
                                 class="employee-photo me-3"
                                 onerror="this.src='https://th.bing.com/th/id/OIP.AtJhqN3-No3fmZqKdpw17QHaHa?rs=1&pid=ImgDetMain'">
                            <div>
                                <p class="mb-0"><strong>ID:</strong> ${recognizedEmployee.id}</p>
                                <p class="mb-0"><strong>Name:</strong> ${recognizedEmployee.fullName}</p>
                            </div>
                        </div>
                        <p><strong>Confidence:</strong> ${(data.confidence * 100).toFixed(2)}%</p>
                        <p><strong>Segmentation Model:</strong> ${getModelNameById(segmentationModels, segmentationModelId)}</p>
                        <p><strong>Recognition Model:</strong> ${getModelNameById(recognitionModels, recognitionModelId)}</p>
                    </div>
                `;
                
                // Also select this employee in the table
                selectEmployee(recognizedEmployee);
            } else {
                resultContent.innerHTML = `
                    <div class="alert alert-warning">
                        <h5>Employee Recognized but Not Found in Database</h5>
                        <p>Employee ID: ${data.employeeId}</p>
                        <p>Confidence: ${(data.confidence * 100).toFixed(2)}%</p>
                        <p>The employee may have been removed from the system.</p>
                    </div>
                `;
            }
        } else {
            resultContent.innerHTML = `
                <div class="alert alert-warning">
                    <h5>No Match Found</h5>
                    <p>No matching fingerprint was found in the database.</p>
                </div>
            `;
        }
    })
    .catch(error => {
        // Hide loading indicator
        document.getElementById('loadingIndicator').style.display = 'none';
        
        // Show error
        const resultCard = document.getElementById('recognizeResult');
        const resultContent = document.getElementById('recognizeResultContent');
        
        resultCard.style.display = 'block';
        resultContent.innerHTML = `
            <div class="alert alert-danger">
                <h5>An Error Occurred</h5>
                <p>${error.message}</p>
            </div>
        `;
    });
});

// Helper function to get model name by ID
function getModelNameById(modelList, modelId) {
    const model = modelList.find(m => m.id === modelId);
    return model ? model.name : 'Unknown model';
}

// Clear registration form and result
document.getElementById('clearRegisterForm').addEventListener('click', function() {
    document.getElementById('registerForm').reset();
    document.getElementById('previewContainer').innerHTML = '';
    document.getElementById('registerResult').style.display = 'none';
});

// Clear recognition form and result
document.getElementById('clearRecognizeForm').addEventListener('click', function() {
    document.getElementById('recognizeForm').reset();
    document.getElementById('recognizePreview').innerHTML = '';
    document.getElementById('recognizeResult').style.display = 'none';
});

// Toggle models table visibility
document.getElementById('toggleModelsTable').addEventListener('click', function() {
    const modelsTable = document.getElementById('modelsTableContainer');
    const toggleButton = document.getElementById('toggleModelsTable');
    
    if (modelsTable.classList.contains('d-none')) {
        modelsTable.classList.remove('d-none');
        toggleButton.textContent = 'Hide Models';
    } else {
        modelsTable.classList.add('d-none');
        toggleButton.textContent = 'Show Models';
    }
});

// Refresh data
document.getElementById('refreshData').addEventListener('click', function() {
    fetchEmployees();
    fetchSegmentationModels();
    fetchRecognitionModels();
});