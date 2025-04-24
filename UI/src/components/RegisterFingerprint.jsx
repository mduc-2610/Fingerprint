import React, { useState, useEffect } from 'react';
import { apiService } from '../config/api';

// 👉 Component: Danh sách nhân viên
function EmployeeList({ employees, selectedEmployee, onSelect }) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 max-h-[300px] overflow-y-auto">
      {employees.map((employee) => (
        <div
          key={employee.id}
          onClick={() => onSelect(employee)}
          className={`p-4 border rounded-lg cursor-pointer transition-colors ${selectedEmployee?.id === employee.id
              ? 'bg-blue-50 border-blue-500'
              : 'hover:bg-gray-50'
            }`}
        >
          <div className="flex items-center">
            <img
              src={employee.photoUrl || '/avt.png'}
              alt={employee.fullName}
              className="w-16 h-16 rounded-full object-cover mr-4"
            />
            <div>
              <h3 className="font-semibold">{employee.fullName}</h3>
              <p className="text-gray-600">Mã NV: {employee.id}</p>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

// 👉 Component: Hiển thị nhân viên được chọn
function SelectedEmployeeCard({ employee }) {
  if (!employee) return null;

  return (
    <div className="bg-white border rounded-lg p-4 mb-4">
      <div className="flex items-center">
        <img
          src={employee.photoUrl || '/avt.png'}
          alt={employee.fullName}
          className="w-20 h-20 rounded-full object-cover mr-6"
        />
        <div>
          <h3 className="text-xl font-semibold">{employee.fullName}</h3>
          <p>Mã NV: {employee.id}</p>
          <p>SĐT: {employee.phoneNumber || 'Chưa cập nhật'}</p>
        </div>
      </div>
    </div>
  );
}

// 👉 Component: Xem trước ảnh
function FingerprintPreview({ previewImage }) {
  if (!previewImage) return null;

  return (
    <div className="text-center">
      <img
        src={previewImage}
        alt="Ảnh dấu vân tay"
        className="max-h-[300px] mx-auto rounded-lg"
      />
    </div>
  );
}

// 👉 Component: Kết quả đăng ký
function RegistrationResult({ result, employee, position }) {
  if (!result) return null;

  if (result.error) {
    return (
      <div className="mt-6 bg-red-50 border border-red-200 p-4 rounded-lg">
        <h3 className="text-lg font-semibold text-red-700 mb-2">
          Đăng Ký Thất Bại
        </h3>
        <p>{result.error}</p>
      </div>
    );
  }

  return (
    <div className="mt-6 bg-green-50 border border-green-200 p-4 rounded-lg">
      <h3 className="text-lg font-semibold text-green-700 mb-2">
        Đăng Ký Thành Công
      </h3>
      <div className="space-y-2">
        <p>Nhân Viên: {employee.fullName}</p>
        <p>Vị Trí: {position.replace('_', ' ')}</p>
        <p>Chất Lượng: {(result.quality * 100).toFixed(2)}%</p>
        <p>Ngày Đăng Ký: {new Date(result.capturedAt).toLocaleString()}</p>
      </div>
    </div>
  );
}

// 👉 Component chính
export function RegisterFingerprint({
  segmentationModelId,
  recognitionModelId
}) {
  const [employees, setEmployees] = useState([]);
  const [selectedEmployee, setSelectedEmployee] = useState(null);
  const [fingerprintFile, setFingerprintFile] = useState(null);
  const [fingerprintPosition, setFingerprintPosition] = useState('');
  const [previewImage, setPreviewImage] = useState(null);
  const [registrationResult, setRegistrationResult] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    const loadEmployees = async () => {
      try {
        const response = await apiService.getEmployees();
        setEmployees(response);
      } catch (err) {
        const apiError =
          err?.response?.data?.message || err?.message || 'Lỗi tải danh sách nhân viên';
        setError(apiError);
      }
    };

    loadEmployees();
  }, []);


  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    setFingerprintFile(file);

    const reader = new FileReader();

    reader.onloadend = () => {
      setPreviewImage(reader.result);
    };

    reader.onerror = () => {
      setError('Không thể đọc file ảnh. Vui lòng thử lại.');
      setPreviewImage(null);
    };

    try {
      reader.readAsDataURL(file);
    } catch {
      setError('Định dạng file không hợp lệ.');
      setPreviewImage(null);
    }
  };


  const handleRegisterFingerprint = async (e) => {
    e.preventDefault();
    setError(null);
    setRegistrationResult(null);

    if (!selectedEmployee) return setError('Vui lòng chọn nhân viên');
    if (!fingerprintFile) return setError('Vui lòng chọn ảnh dấu vân tay');
    if (!fingerprintPosition) return setError('Vui lòng chọn vị trí dấu vân tay');
    if (!segmentationModelId) return setError('Vui lòng chọn mô hình phân đoạn');
    if (!recognitionModelId) return setError('Vui lòng chọn mô hình nhận dạng');

    try {
      const formData = new FormData();
      formData.append('file', fingerprintFile);
      formData.append('position', fingerprintPosition);
      formData.append('employeeId', selectedEmployee.id);

      const response = await apiService.registerFingerprint(
        selectedEmployee.id,
        formData,
        segmentationModelId,
        recognitionModelId
      );

      setRegistrationResult(response);
    } catch (err) {
      const apiError =
        err?.response?.data?.message || err?.message || 'Lỗi đăng ký dấu vân tay';
      setError(apiError);
      setRegistrationResult(null);
    }
  };


  return (
    <div>
      <h2 className="text-2xl font-semibold mb-4">Đăng Ký Dấu Vân Tay Mới</h2>

      <div className="mb-4">
        <label className="block mb-2">Chọn Nhân Viên</label>
        <EmployeeList
          employees={employees}
          selectedEmployee={selectedEmployee}
          onSelect={setSelectedEmployee}
        />
      </div>

      <SelectedEmployeeCard employee={selectedEmployee} />

      <form onSubmit={handleRegisterFingerprint} className="space-y-4">
        <div>
          <label className="block mb-2">Vị Trí Dấu Vân Tay</label>
          <select
            value={fingerprintPosition}
            onChange={(e) => setFingerprintPosition(e.target.value)}
            className="w-full p-2 border rounded-md"
          >
            <option value="">Chọn vị trí</option>
            {[
              'LEFT_THUMB', 'LEFT_INDEX', 'LEFT_MIDDLE', 'LEFT_RING', 'LEFT_LITTLE',
              'RIGHT_THUMB', 'RIGHT_INDEX', 'RIGHT_MIDDLE', 'RIGHT_RING', 'RIGHT_LITTLE'
            ].map((pos) => (
              <option key={pos} value={pos}>
                {pos.replace('_', ' ').replace(/\b\w/g, (c) => c.toUpperCase())}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="block mb-2">Ảnh Dấu Vân Tay</label>
          <input
            type="file"
            accept=".bmp,.tif,.tiff"
            onChange={handleFileChange}
            className="w-full p-2 border rounded-md"
          />
        </div>

        <FingerprintPreview previewImage={previewImage} />

        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
            {error}
          </div>
        )}

        <button
          type="submit"
          className="w-full bg-blue-600 text-white py-2 rounded-md hover:bg-blue-700 transition"
        >
          Đăng Ký Dấu Vân Tay
        </button>
      </form>

      <RegistrationResult
        result={registrationResult}
        employee={selectedEmployee}
        position={fingerprintPosition}
      />
    </div>
  );
}
