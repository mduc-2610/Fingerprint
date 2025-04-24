// RecognizeFingerprint.jsx
import React, { useState, useEffect } from 'react';
import { apiService } from '../config/api';

export function RecognizeFingerprint() {
  const [areas, setAreas] = useState([]);
  const [fingerprintFile, setFingerprintFile] = useState(null);
  const [previewImage, setPreviewImage] = useState(null);
  const [selectedArea, setSelectedArea] = useState('');
  const [recognitionResult, setRecognitionResult] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const [notification, setNotification] = useState('');

  // Tải danh sách khu vực
  useEffect(() => {
    loadAreas();
  }, []);

  // Cập nhật thông báo khi có kết quả nhận dạng
  useEffect(() => {
    if (recognitionResult) {
      updateNotification(recognitionResult);
    }
  }, [recognitionResult]);

  const loadAreas = async () => {
    setLoading(true);
    try {
      const response = await apiService.getAreas();
      setAreas(response);
      setError(null);
    } catch (error) {
      console.error('Lỗi tải danh sách khu vực:', error);
      setError('Lỗi tải danh sách khu vực');
    } finally {
      setLoading(false);
    }
  };

  // Xử lý tải ảnh xem trước
  const handleFileChange = (file) => {
    setFingerprintFile(file);

    // Tạo ảnh xem trước
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setPreviewImage(reader.result);
      };
      reader.readAsDataURL(file);
    } else {
      setPreviewImage(null);
    }
  };

  // Xử lý thay đổi khu vực
  const handleAreaChange = (areaId) => {
    setSelectedArea(areaId);
  };

  // Xử lý nhận dạng dấu vân tay
  const handleRecognizeFingerprint = async () => {
    // Kiểm tra đầu vào
    if (!fingerprintFile) {
      setError('Vui lòng chọn ảnh dấu vân tay');
      return;
    }
    if (!selectedArea) {
      setError('Vui lòng chọn khu vực');
      return;
    }

    setLoading(true);
    try {
      // Tạo form data
      const formData = new FormData();
      formData.append('file', fingerprintFile);
      formData.append('areaId', selectedArea);

      // Gọi API nhận dạng
      const response = await apiService.recognizeFingerprint(formData);

      setRecognitionResult(response);
      setError(null);
    } catch (error) {
      console.error('Lỗi nhận dạng dấu vân tay:', error);
      setError('Lỗi nhận dạng dấu vân tay');
      setRecognitionResult(null);
    } finally {
      setLoading(false);
    }
  };

  // Cập nhật thông báo dựa trên kết quả nhận dạng
  const updateNotification = (result) => {
    if (!result.matched) {
      setNotification('Không tìm thấy dấu vân tay phù hợp');
    } else if (!result.active) {
      setNotification('Dấu vân tay không hoạt động');
    } else if (!result.accessable) {
      setNotification('Dấu vân tay không có quyền truy cập vào khu vực này');
    } else if (result.authorized) {
      setNotification('Dấu vân tay đã được xác nhận thành công');
    } else {
      setNotification('Dấu vân tay không được xác nhận');
    }
  };

  return (
    <div className="container mx-auto p-4">
      <h2 className="text-2xl font-semibold mb-4">Nhận Dạng Dấu Vân Tay</h2>

      {/* Hiển thị lỗi */}
      {error && <ErrorMessage message={error} />}

      {/* Form nhận dạng */}
      <FingerprintForm
        areas={areas}
        selectedArea={selectedArea}
        previewImage={previewImage}
        onAreaChange={handleAreaChange}
        onFileChange={handleFileChange}
        onSubmit={handleRecognizeFingerprint}
        loading={loading}
      />

      {/* Kết quả nhận dạng */}
      {recognitionResult && (
        <RecognitionResult
          result={recognitionResult}
          notification={notification}
        />
      )}
    </div>
  );
}

function FingerprintForm({
  areas,
  selectedArea,
  previewImage,
  onAreaChange,
  onFileChange,
  onSubmit,
  loading
}) {
  const handleFileInputChange = (e) => {
    const file = e.target.files[0];
    onFileChange(file);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit();
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block mb-2">Khu Vực</label>
        <select
          value={selectedArea}
          onChange={(e) => onAreaChange(e.target.value)}
          className="w-full p-2 border rounded-md"
          disabled={loading}
        >
          <option value="">Chọn khu vực</option>
          {areas.map((area) => (
            <option key={area.id} value={area.id}>
              {area.name}
            </option>
          ))}
        </select>
      </div>

      <div>
        <label className="block mb-2">Ảnh Dấu Vân Tay</label>
        <input
          type="file"
          accept=".bmp,.tif,.tiff"
          onChange={handleFileInputChange}
          className="w-full p-2 border rounded-md"
          disabled={loading}
        />
      </div>

      {/* Xem trước ảnh */}
      {previewImage && (
        <div className="text-center">
          <img
            src={previewImage}
            alt="Ảnh dấu vân tay"
            className="max-h-[300px] mx-auto rounded-lg"
          />
        </div>
      )}

      <button
        type="submit"
        className="w-full bg-blue-600 text-white py-2 rounded-md hover:bg-blue-700 transition disabled:bg-blue-400"
        disabled={loading}
      >
        {loading ? 'Đang xử lý...' : 'Nhận Dạng Dấu Vân Tay'}
      </button>
    </form>
  );
}

function RecognitionResult({ result, notification }) {
  // Xác định trạng thái để hiển thị màu sắc
  const isSuccess = result.matched && result.authorized && result.accessable && result.active;

  // Hiển thị kết quả thất bại
  if (!result.matched) {
    return (
      <div className="mt-6 bg-red-50 border border-red-200 p-4 rounded-lg text-red-700">
        <h3 className="text-lg font-semibold mb-2">Nhận Dạng Thất Bại</h3>
        <p>{notification}</p>
        <p>Độ Chính Xác: {(result.confidence * 100).toFixed(2)}%</p>
        <p>Thời Gian: {new Date(result.accessLog.timestamp).toLocaleString()}</p>
        <p>Khu Vực: {result.accessLog.area?.name || 'Không xác định'}</p>
      </div>
    );
  }

  // Hiển thị kết quả thành công
  return (
    <div className={`mt-6 p-4 rounded-lg border ${isSuccess ? 'bg-green-50 border-green-200' : 'bg-yellow-50 border-yellow-200'
      }`}>
      <h3 className={`text-lg font-semibold mb-2 ${isSuccess ? 'text-green-700' : 'text-yellow-700'
        }`}>
        {notification}
      </h3>

      <div className="space-y-2">
        <div className="flex items-center">
          <img
            src={result.employee?.photoUrl || '/avt.png'}
            alt="Ảnh nhân viên"
            className="w-20 h-20 rounded-full object-cover mr-4"
          />
          <div>
            <p className="font-semibold">
              {result.employee?.fullName || 'Không xác định'}
            </p>
            <p>Mã NV: {result.employee?.id || 'N/A'}</p>
          </div>
        </div>

        <p>Độ Chính Xác: {(result.confidence * 100).toFixed(2)}%</p>
        <p>Thời Gian: {new Date(result.accessLog.timestamp).toLocaleString()}</p>
        <p>Khu Vực: {result.accessLog.area?.name || 'Không xác định'}</p>
      </div>
    </div>
  );
}
function ErrorMessage({ message }) {
  return (
    <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
      {message}
    </div>
  );
}