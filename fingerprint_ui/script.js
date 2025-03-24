// Global variables
let selectedEmployee = null;
let selectedArea = null;
const BASE_URL = 'http://localhost:8080';

document.addEventListener('DOMContentLoaded', function() {
    // Initialize Bootstrap tabs
    const tabElements = document.querySelectorAll('button[data-bs-toggle="tab"]');
    tabElements.forEach(tab => {
        tab.addEventListener('click', function() {
            const targetId = tab.getAttribute('data-bs-target').substring(1);
            document.querySelectorAll('.tab-pane').forEach(pane => {
                pane.classList.remove('show', 'active');
            });
            document.getElementById(targetId).classList.add('show', 'active');
        });
    });

    // Load models on page load
    loadModels();
    
    // Load employee data
    loadEmployees();
    
    // Load area data
    loadAreas();
    
    // Setup search functionality
    setupEmployeeSearch();
    
    // Setup form submissions
    setupRegisterForm();
    setupRecognizeForm();
    
    // Setup file preview
    setupFilePreview();
});

// Models functions
function loadModels() {
    showLoading();
    
    fetch(`${BASE_URL}/api/fingerprint-segmentation`)
        .then(response => response.json())
        .then(data => {
            populateModelDropdown('segmentationModel', data);
            populateModelDropdown('segmentationModelRecognize', data);
            populateModelTable('segmentationModelTableBody', data);
        })
        .catch(error => {
            console.error('Error loading segmentation models:', error);
        });
    
    fetch(`${BASE_URL}/api/fingerprint-recognition`)
        .then(response => response.json())
        .then(data => {
            populateModelDropdown('recognitionModel', data);
            populateModelDropdown('recognitionModelRegister', data);
            populateModelTable('recognitionModelTableBody', data);
            hideLoading();
        })
        .catch(error => {
            console.error('Error loading recognition models:', error);
            hideLoading();
        });
}

// Area functions
function loadAreas() {
    fetch(`${BASE_URL}/api/area`)
        .then(response => response.json())
        .then(data => {
            populateAreaDropdown('areaSelect', data);
        })
        .catch(error => {
            console.error('Error loading areas:', error);
        });
}

function populateAreaDropdown(elementId, areas) {
    const dropdown = document.getElementById(elementId);
    dropdown.innerHTML = '';
    
    // Add default option
    const defaultOption = document.createElement('option');
    defaultOption.value = '';
    defaultOption.textContent = 'Select area (optional)';
    dropdown.appendChild(defaultOption);
    
    areas.forEach(area => {
        const option = document.createElement('option');
        option.value = area.id;
        option.textContent = area.name;
        dropdown.appendChild(option);
    });
}

function populateModelDropdown(elementId, models) {
    const dropdown = document.getElementById(elementId);
    dropdown.innerHTML = '';
    
    models.forEach(model => {
        const option = document.createElement('option');
        option.value = model.id;
        option.textContent = `${model.name} (Accuracy: ${(model.accuracy * 100).toFixed(2)}%)`;
        dropdown.appendChild(option);
    });
}

function populateModelTable(tableBodyId, models) {
    const tableBody = document.getElementById(tableBodyId);
    tableBody.innerHTML = '';
    
    models.forEach(model => {
        const row = document.createElement('tr');
        
        const nameCell = document.createElement('td');
        nameCell.textContent = model.name;
        
        const accuracyCell = document.createElement('td');
        accuracyCell.textContent = `${(model.accuracy * 100).toFixed(2)}%`;
        
        const createdCell = document.createElement('td');
        createdCell.textContent = new Date(model.createdAt).toLocaleString();
        
        row.appendChild(nameCell);
        row.appendChild(accuracyCell);
        row.appendChild(createdCell);
        
        tableBody.appendChild(row);
    });
}

// Employee search and selection functions
function loadEmployees() {
    showLoading();
    
    fetch(`${BASE_URL}/api/employee`)
        .then(response => response.json())
        .then(data => {
            populateEmployeeTable(data);
            hideLoading();
        })
        .catch(error => {
            console.error('Error loading employees:', error);
            hideLoading();
        });
}

function populateEmployeeTable(employees) {
    const tableBody = document.getElementById('employeeTableBody');
    tableBody.innerHTML = '';
    
    employees.forEach(employee => {
        const row = document.createElement('tr');
        row.setAttribute('data-employee-id', employee.id);
        row.addEventListener('click', () => selectEmployee(employee));
        
        const photoCell = document.createElement('td');
        const photoImg = document.createElement('img');
        photoImg.src = employee.photoUrl || '/img/default-profile.png';
        photoImg.alt = 'Employee Photo';
        photoImg.style.width = '50px';
        photoImg.style.height = '50px';
        photoImg.className = 'rounded-circle';
        photoCell.appendChild(photoImg);
        
        const idCell = document.createElement('td');
        idCell.textContent = employee.id;
        
        const nameCell = document.createElement('td');
        nameCell.textContent = `${employee.fullName}`;
        
        const phoneCell = document.createElement('td');
        phoneCell.textContent = employee.phoneNumber || '-';
        
        row.appendChild(photoCell);
        row.appendChild(idCell);
        row.appendChild(nameCell);
        row.appendChild(phoneCell);
        
        tableBody.appendChild(row);
    });
}

function setupEmployeeSearch() {
    const searchInput = document.getElementById('employeeSearch');
    searchInput.addEventListener('input', function() {
        const searchTerm = this.value.toLowerCase();
        const rows = document.querySelectorAll('#employeeTableBody tr');
        
        rows.forEach(row => {
            const text = row.textContent.toLowerCase();
            if (text.includes(searchTerm)) {
                row.style.display = '';
            } else {
                row.style.display = 'none';
            }
        });
    });
}

function selectEmployee(employee) {
    selectedEmployee = employee;
    
    document.getElementById('noEmployeeSelected').classList.add('d-none');
    
    const infoCard = document.getElementById('employeeInfoCard');
    infoCard.classList.remove('d-none');
    
    document.getElementById('selectedEmployeePhoto').src = employee.photoUrl || '/img/default-profile.png';
    document.getElementById('selectedEmployeeFullName').textContent = `${employee.fullName}`;
    document.getElementById('selectedEmployeeId').textContent = employee.id;
    document.getElementById('selectedEmployeePhone').textContent = employee.phoneNumber || '-';
    document.getElementById('selectedEmployeeAddress').textContent = employee.address || '-';
    
    // Highlight the selected row
    const rows = document.querySelectorAll('#employeeTableBody tr');
    rows.forEach(row => row.classList.remove('table-primary'));
    
    const selectedRow = document.querySelector(`#employeeTableBody tr[data-employee-id="${employee.id}"]`);
    if (selectedRow) {
        selectedRow.classList.add('table-primary');
    }
}

// File preview functions
function setupFilePreview() {
    // Single file preview for registration
    document.getElementById('fingerprintFile').addEventListener('change', function(event) {
        const previewContainer = document.getElementById('registerPreview');
        previewContainer.innerHTML = '';
        
        if (this.files && this.files[0]) {
            const reader = new FileReader();
            reader.onload = function(e) {
                const img = document.createElement('img');
                img.src = e.target.result;
                img.className = 'img-fluid mt-3 mb-3';
                img.style.maxHeight = '200px';
                img.alt = 'Fingerprint Preview';
                previewContainer.appendChild(img);
                
                // Add position select for the single image
                const positionSelect = document.createElement('select');
                positionSelect.className = 'form-select mt-2';
                positionSelect.id = 'fingerPosition';
                
                const positions = ['LEFT_THUMB', 'LEFT_INDEX', 'LEFT_MIDDLE', 'LEFT_RING', 'LEFT_LITTLE', 
                                  'RIGHT_THUMB', 'RIGHT_INDEX', 'RIGHT_MIDDLE', 'RIGHT_RING', 'RIGHT_LITTLE'];
                
                positions.forEach(pos => {
                    const option = document.createElement('option');
                    option.value = pos;
                    option.textContent = pos.replace('_', ' ');
                    positionSelect.appendChild(option);
                });
                
                previewContainer.appendChild(positionSelect);
            };
            reader.readAsDataURL(this.files[0]);
        }
    });
    
    // Single file preview for recognition
    document.getElementById('fingerprintFileRecognize').addEventListener('change', function(event) {
        const previewContainer = document.getElementById('recognizePreview');
        previewContainer.innerHTML = '';
        
        if (this.files && this.files[0]) {
            const reader = new FileReader();
            reader.onload = function(e) {
                const img = document.createElement('img');
                img.src = e.target.result;
                img.className = 'img-fluid mt-3 mb-3';
                img.style.maxHeight = '200px';
                img.alt = 'Fingerprint Preview';
                previewContainer.appendChild(img);
            };
            reader.readAsDataURL(this.files[0]);
        }
    });
}

// Form submission functions
function setupRegisterForm() {
    document.getElementById('registerForm').addEventListener('submit', function(event) {
        event.preventDefault();
        
        if (!selectedEmployee) {
            alert('Please select an employee first.');
            return;
        }
        
        const fingerprintFile = document.getElementById('fingerprintFile').files[0];
        
        if (!fingerprintFile) {
            alert('Please upload a fingerprint image.');
            return;
        }
        
        const fingerprintPosition = document.getElementById('fingerPosition') ? 
            document.getElementById('fingerPosition').value : 'RIGHT_INDEX';
            
        const segmentationModelId = document.getElementById('segmentationModel').value;
        const recognitionModelId = document.getElementById('recognitionModelRegister').value;
        
        showLoading();
        
        // Create form data
        const formData = new FormData();
        formData.append('file', fingerprintFile);
        formData.append('position', fingerprintPosition);
        formData.append('segmentationModelId', segmentationModelId);
        formData.append('recognitionModelId', recognitionModelId);
        
        // Submit single fingerprint registration
        fetch(`${BASE_URL}/api/fingerprint/register/${selectedEmployee.id}/single`, {
            method: 'POST',
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            hideLoading();
            displayRegistrationResult(data);
        })
        .catch(error => {
            console.error('Error registering fingerprint:', error);
            hideLoading();
            displayRegistrationResult({
                error: 'Failed to register fingerprint. Please try again.'
            });
        });
    });
}

function setupRecognizeForm() {
    document.getElementById('recognizeForm').addEventListener('submit', function(event) {
        event.preventDefault();
        
        const fingerprintFile = document.getElementById('fingerprintFileRecognize').files[0];
        
        if (!fingerprintFile) {
            alert('Please upload a fingerprint image.');
            return;
        }
        
        const segmentationModelId = document.getElementById('segmentationModelRecognize').value;
        const recognitionModelId = document.getElementById('recognitionModel').value;
        const areaId = document.getElementById('areaSelect').value;
        
        showLoading();
        
        // Create form data
        const formData = new FormData();
        formData.append('file', fingerprintFile);
        formData.append('segmentationModelId', segmentationModelId);
        formData.append('recognitionModelId', recognitionModelId);
        
        // Add area ID if selected
        if (areaId) {
            formData.append('areaId', areaId);
        }
        
        // Submit recognition request
        fetch(`${BASE_URL}/api/fingerprint/recognize`, {
            method: 'POST',
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            hideLoading();
            displayRecognitionResult(data);
        })
        .catch(error => {
            console.error('Error recognizing fingerprint:', error);
            hideLoading();
            displayRecognitionResult({
                error: 'Failed to recognize fingerprint. Please try again.'
            });
        });
    });
}

// Result display functions
function displayRegistrationResult(result) {
    const resultContainer = document.getElementById('registerResultContent');
    
    if (result.error) {
        resultContainer.innerHTML = `
            <div class="alert alert-danger">
                <h5>Registration Failed</h5>
                <p>${result.error}</p>
            </div>
        `;
        return;
    }
    
    resultContainer.innerHTML = `
        <div class="alert alert-success">
            <h5>Registration Successful</h5>
            <p>Successfully registered fingerprint for employee ${result.employeeId}.</p>
        </div>
        <div class="mt-3">
            <strong>Registered Fingerprint:</strong>
            <ul class="list-group mt-2">
                <li class="list-group-item">
                    Position: ${result.position}, Quality: ${(result.quality * 100).toFixed(2)}%, 
                    Captured at: ${new Date(result.capturedAt).toLocaleString()}
                </li>
            </ul>
        </div>
    `;
}

function displayRecognitionResult(result) {
    const resultContainer = document.getElementById('recognizeResultContent');
    
    if (result.error) {
        resultContainer.innerHTML = `
            <div class="alert alert-danger">
                <h5>Recognition Failed</h5>
                <p>${result.error}</p>
            </div>
        `;
        return;
    }
    
    if (result.matched) {
        resultContainer.innerHTML = `
            <div class="alert ${result.isAuthorized ? 'alert-success' : 'alert-warning'}">
                <h5>${result.isAuthorized ? 'Access Granted' : 'Access Denied'}</h5>
                <p>Fingerprint matched with confidence: ${(result.confidence * 100).toFixed(2)}%</p>
            </div>
            
            <div class="card mt-3">
                <div class="card-header">
                    <h5>Matched Employee</h5>
                </div>
                <div class="card-body">
                    <div class="row">
                        <div class="col-md-3 text-center">
                            <img src="${result.employee?.photoUrl || '/img/default-profile.png'}" alt="Employee Photo" 
                                class="img-fluid mb-2" style="max-height: 100px;">
                        </div>
                        <div class="col-md-9">
                            <h5>${result.employee ? `${result.employee.fullName}` : 'Unknown Employee'}</h5>
                            <p class="mb-1"><strong>ID:</strong> ${result.employeeId}</p>
                            <p class="mb-1"><strong>Phone:</strong> ${result.employee?.phoneNumber || '-'}</p>
                            <p class="mb-0"><strong>Address:</strong> ${result.employee?.address || '-'}</p>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="card mt-3">
                <div class="card-header">
                    <h5>Access Log</h5>
                </div>
                <div class="card-body">
                    <p><strong>Timestamp:</strong> ${new Date(result.accessLog.timestamp).toLocaleString()}</p>
                    <p><strong>Area:</strong> ${result.accessLog.area?.name || 'General Access'}</p>
                    <p><strong>Access Type:</strong> ${result.accessLog.accessType}</p>
                    <p><strong>Authorization:</strong> ${result.isAuthorized ? 'Authorized' : 'Not Authorized'}</p>
                </div>
            </div>
        `;
    } else {
        resultContainer.innerHTML = `
            <div class="alert alert-danger">
                <h5>Access Denied</h5>
                <p>No matching fingerprint found. Confidence: ${(result.confidence * 100).toFixed(2)}%</p>
            </div>
            
            <div class="card mt-3">
                <div class="card-header">
                    <h5>Access Log</h5>
                </div>
                <div class="card-body">
                    <p><strong>Timestamp:</strong> ${new Date(result.accessLog.timestamp).toLocaleString()}</p>
                    <p><strong>Area:</strong> ${result.accessLog.area?.name || 'General Access'}</p>
                    <p><strong>Access Type:</strong> ${result.accessLog.accessType}</p>
                    <p><strong>Authorization:</strong> Not Authorized</p>
                </div>
            </div>
        `;
    }
}

// Utility functions
function showLoading() {
    document.getElementById('loadingIndicator').style.display = 'flex';
}

function hideLoading() {
    document.getElementById('loadingIndicator').style.display = 'none';
}