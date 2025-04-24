import React, { useState, useEffect } from 'react';
import { apiService } from '../config/api';

export function RecognizeFingerprint({
    segmentationModelId,
    recognitionModelId,
}) {
    const [areas, setAreas] = useState([]);
    const [fingerprintFile, setFingerprintFile] = useState(null);
    const [previewImage, setPreviewImage] = useState(null);
    const [selectedArea, setSelectedArea] = useState('');
    const [recognitionResult, setRecognitionResult] = useState(null);
    const [error, setError] = useState(null);
    const [notification, setNotification] = useState("");

    // Tải danh sách khu vực
    useEffect(() => {
        const loadAreas = async () => {
            try {
                const response = await apiService.getAreas();
                setAreas(response);
            } catch (error) {
                setError('Lỗi tải danh sách khu vực');
            }
        };

        loadAreas();
    }, []);

    // Xử lý tải ảnh xem trước
    const handleFileChange = (e) => {
        const file = e.target.files[0];
        if (!file) return;

        setFingerprintFile(file);
        setError(null);
        setRecognitionResult(null);
        setNotification("");

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


    // Xử lý nhận dạng dấu vân tay
    const handleRecognizeFingerprint = async (e) => {
        e.preventDefault();
        setError(null);
        setRecognitionResult(null);
        setNotification("");

        if (!fingerprintFile) {
            return setError('Vui lòng chọn ảnh dấu vân tay');
        }

        if (!selectedArea) {
            return setError('Vui lòng chọn khu vực');
        }

        if (!segmentationModelId || !recognitionModelId) {
            return setError('Vui lòng chọn mô hình nhận dạng');
        }

        try {
            const formData = new FormData();
            formData.append('file', fingerprintFile);
            formData.append('areaId', selectedArea);

            const response = await apiService.recognizeFingerprint(
                formData,
                segmentationModelId,
                recognitionModelId
            );

            setRecognitionResult(response);
        } catch (err) {
            const apiError = err?.response?.data?.message || err.message || 'Lỗi nhận dạng dấu vân tay';
            setError(apiError);
        }
    };


    useEffect(() => {
        if (recognitionResult) {
            if (!recognitionResult.active) {
                setNotification("Dấu vân tay không hoạt động");
            } else if (!recognitionResult.accessable) {
                setNotification("Dấu vân tay không có quyền truy cập vào khu vực này");
            } else if (recognitionResult.authorized) {
                setNotification("Dấu vân tay đã được xác nhận thành công");
            } else {
                setNotification("Dấu vân tay không được xác nhận");
            }

        }
    }
        , [recognitionResult]);

    return (
        <div>
            <h2 className="text-2xl font-semibold mb-4">Nhận Dạng Dấu Vân Tay</h2>

            <form onSubmit={handleRecognizeFingerprint} className="space-y-4">
                <div>
                    <label className="block mb-2">Khu Vực</label>
                    <select
                        value={selectedArea}
                        onChange={(e) => setSelectedArea(e.target.value)}
                        className="w-full p-2 border rounded-md"
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
                        onChange={handleFileChange}
                        className="w-full p-2 border rounded-md"
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

                {/* Hiển thị lỗi */}
                {error && (
                    <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
                        {error}
                    </div>
                )}

                <button
                    type="submit"
                    className="w-full bg-blue-600 text-white py-2 rounded-md hover:bg-blue-700 transition"
                >
                    Nhận Dạng Dấu Vân Tay
                </button>
            </form>

            {/* Kết quả nhận dạng */}
            {recognitionResult && (
                <div className="mt-6">
                    {recognitionResult.matched ? (
                        <div className={`
              p-4 rounded-lg 
              ${recognitionResult.authorized && recognitionResult.accessable && recognitionResult.active ? 'bg-green-50 border-green-200' : 'bg-yellow-50 border-yellow-200'}
            `}>
                            <h3 className={`
                text-lg font-semibold mb-2 
                ${recognitionResult.authorized && recognitionResult.accessable && recognitionResult.active ? 'text-green-700' : 'text-yellow-700'}
              `}>
                                {notification}
                            </h3>

                            <div className="space-y-2">
                                <div className="flex items-center">
                                    <img
                                        src={recognitionResult.employee?.photoUrl || '/avt.png'}
                                        alt="Ảnh nhân viên"
                                        className="w-20 h-20 rounded-full object-cover mr-4"
                                    />
                                    <div>
                                        <p className="font-semibold">
                                            {recognitionResult.employee?.fullName || 'Không xác định'}
                                        </p>
                                        <p>Mã NV: {recognitionResult.employee?.id || 'N/A'}</p>
                                    </div>
                                </div>

                                <p>Độ Chính Xác: {(recognitionResult.confidence * 100).toFixed(2)}%</p>
                                <p>Thời Gian: {new Date(recognitionResult.accessLog.timestamp).toLocaleString()}</p>
                                <p>Khu Vực: {recognitionResult.accessLog.area?.name || 'Không xác định'}</p>
                            </div>
                        </div>
                    ) : (
                        <div className="bg-red-50 border border-red-200 p-4 rounded-lg text-red-700">
                            <h3 className="text-lg font-semibold mb-2">Nhận Dạng Thất Bại</h3>
                            <p>Không tìm thấy dấu vân tay phù hợp</p>
                            <p>Độ Chính Xác: {(recognitionResult.confidence * 100).toFixed(2)}%</p>
                            <p>Thời Gian: {new Date(recognitionResult.accessLog.timestamp).toLocaleString()}</p>
                            <p>Khu Vực: {recognitionResult.accessLog.area?.name || 'Không xác định'}</p>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}