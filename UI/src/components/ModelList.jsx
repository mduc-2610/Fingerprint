import React, { useState, useEffect } from 'react';
import { apiService } from '../config/api';

export function ModelList() {
    const [items, setItems] = useState([]);
    const [segmentationModels, setSegmentationModels] = useState([]);
    const [recognitionModels, setRecognitionModels] = useState([]);
    const [selectedModel, setSelectedModel] = useState(null);
    const [modelType, setModelType] = useState(null);
    const [modelDetails, setModelDetails] = useState(null);
    const [error, setError] = useState(null);

    // Tải danh sách mô hình
    useEffect(() => {
        const loadModels = async () => {
            try {
                const [segmentationResponse, recognitionResponse] = await Promise.all([
                    apiService.getSegmentationModels(),
                    apiService.getRecognitionModels()
                ]);

                setSegmentationModels(segmentationResponse);
                setRecognitionModels(recognitionResponse);
            } catch (error) {
                setError('Lỗi tải danh sách mô hình');
            }
        };

        loadModels();
    }, []);

    // Xem chi tiết mô hình
    const handleViewModelDetails = async (model, type) => {
        try {
            // Gọi API để lấy chi tiết mô hình
            const response = await apiService.getModelDetails(type, model.id);

            setSelectedModel(model);
            setModelDetails(response);
            setModelType(type);

            if (type === 'recognition') {
                const itemsResponse = await apiService.getRecognitionsForModel(model.id);
                setItems(itemsResponse);
            } else if (type === 'segmentation') {
                const itemsResponse = await apiService.getSegmentationForModel(model.id);
                setItems(itemsResponse);
            }
        } catch (error) {
            setError('Lỗi lấy chi tiết mô hình');
        }
    };

    // Đóng modal chi tiết mô hình
    const handleCloseModelDetails = () => {
        setSelectedModel(null);
        setModelDetails(null);
        setModelType(null);
    };

    // useEffect(() => {
    //     if (selectedModel === ) {

    // Render modal chi tiết mô hình
    const renderModelDetailsModal = () => {
        if (!selectedModel || !modelDetails) return null;

        return (
            <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                <div className="bg-white rounded-lg max-w-4xl w-full max-h-[90vh] overflow-y-auto">
                    <div className="p-6">
                        {/* Tiêu đề modal */}
                        <div className="flex justify-between items-center border-b pb-4 mb-4">
                            <h3 className="text-2xl font-semibold">
                                Chi Tiết Mô Hình: {selectedModel.name}
                            </h3>
                            <button
                                onClick={handleCloseModelDetails}
                                className="text-gray-600 hover:text-gray-900"
                            >
                                Đóng
                            </button>
                        </div>

                        {/* Thông tin cơ bản */}
                        <div className="grid md:grid-cols-2 gap-6 mb-6">
                            <div>
                                <h4 className="text-lg font-semibold mb-3">Thông Tin Mô Hình</h4>
                                <div className="space-y-2">
                                    <p><strong>ID:</strong> {selectedModel.id}</p>
                                    <p><strong>Tên:</strong> {selectedModel.name}</p>
                                    <p><strong>Độ Chính Xác:</strong> {(selectedModel.accuracy * 100).toFixed(2)}%</p>
                                    <p><strong>Ngày Tạo:</strong> {new Date(selectedModel.createdAt).toLocaleString()}</p>
                                </div>
                            </div>

                            <div>
                                <h4 className="text-lg font-semibold mb-3">Thống Kê Sử Dụng</h4>
                                <div className="space-y-2">
                                    <p><strong>Tổng Lượt Sử Dụng:</strong> {modelDetails.totalUsage || 'Chưa có'}</p>
                                    <p>
                                        <strong>Độ Chính Xác Trung Bình:</strong>
                                        {modelDetails.averageConfidence !== null
                                            && `${(modelDetails.averageConfidence * 100).toFixed(2)}%`
                                        }
                                    </p>
                                </div>
                            </div>
                        </div>

                        {/* Chi tiết sử dụng */}
                        <div>
                            <h4 className="text-lg font-semibold mb-3">
                                {modelType === 'segmentation'
                                    ? 'Chi Tiết Mẫu Dấu Vân Tay'
                                    : 'Lịch Sử Nhận Dạng'}
                            </h4>

                            <div className="overflow-x-auto">
                                <table className="w-full border-collapse">
                                    <thead className="bg-gray-100">
                                        <tr>
                                            {modelType === 'segmentation' ? (
                                                <>
                                                    <th className="p-2 border">Tên nhân viên</th>
                                                    <th className="p-2 border">Vị Trí</th>
                                                    <th className="p-2 border">Chất Lượng</th>
                                                    <th className="p-2 border">Ngày Đăng Ký</th>
                                                </>
                                            ) : (
                                                <>
                                                    <th className="p-2 border">Tên nhân viên</th>
                                                    <th className="p-2 border">Thời Gian</th>
                                                    <th className="p-2 border">Độ Chính Xác</th>
                                                    <th className="p-2 border">Khu Vực</th>
                                                </>
                                            )}
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {items && items.length > 0 ? (
                                            items.map((item, index) => (
                                                <tr key={index} className="border-b hover:bg-gray-50">
                                                    {modelType === 'segmentation' ? (
                                                        <>
                                                            <td className="p-2 border">{item?.employee?.fullName || 'N/A'}</td>
                                                            <td className="p-2 border">{item.position || 'N/A'}</td>
                                                            <td className="p-2 border">
                                                                {item.quality ? `${(item.quality * 100).toFixed(2)}%` : 'N/A'}
                                                            </td>
                                                            <td className="p-2 border">
                                                                {item.capturedAt
                                                                    ? new Date(item.capturedAt).toLocaleString()
                                                                    : 'N/A'}
                                                            </td>
                                                        </>
                                                    ) : (
                                                        <>
                                                            <td className="p-2 border">{item?.employee?.fullName || 'N/A'}</td>
                                                            <td className="p-2 border">
                                                                {item.timestamp
                                                                    ? new Date(item.timestamp).toLocaleString()
                                                                    : 'N/A'}
                                                            </td>
                                                            <td className="p-2 border">
                                                                {item.confidence
                                                                    ? `${(item.confidence * 100).toFixed(2)}%`
                                                                    : 'N/A'}
                                                            </td>
                                                            <td className="p-2 border">{item.area?.name || 'N/A'}</td>
                                                        </>
                                                    )}
                                                </tr>
                                            ))
                                        ) : (
                                            <tr>
                                                <td
                                                    colSpan={modelType === 'segmentation' ? 4 : 4}
                                                    className="p-4 text-center text-gray-500"
                                                >
                                                    Không có dữ liệu
                                                </td>
                                            </tr>
                                        )}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    };

    return (
        <div>
            <h2 className="text-2xl font-semibold mb-4">Danh Sách Mô Hình</h2>

            {/* Hiển thị lỗi */}
            {error && (
                <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
                    {error}
                </div>
            )}

            <div className="grid md:grid-cols-2 gap-6">
                {/* Mô hình phân đoạn */}
                <div>
                    <h3 className="text-xl font-semibold mb-3">Mô Hình Phân Đoạn</h3>
                    <div className="space-y-2">
                        {segmentationModels.map((model) => (
                            <div
                                key={model.id}
                                className="bg-white border rounded-lg p-3 hover:bg-gray-50 transition cursor-pointer"
                                onClick={() => handleViewModelDetails(model, 'segmentation')}
                            >
                                <div className="flex justify-between items-center">
                                    <div>
                                        <h4 className="font-semibold">{model.name}</h4>
                                        <p className="text-sm text-gray-600">
                                            Độ Chính Xác: {(model.accuracy * 100).toFixed(2)}%
                                        </p>
                                    </div>
                                    <span className="text-sm text-gray-500">
                                        Chi Tiết
                                    </span>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Mô hình nhận dạng */}
                <div>
                    <h3 className="text-xl font-semibold mb-3">Mô Hình Nhận Dạng</h3>
                    <div className="space-y-2">
                        {recognitionModels.map((model) => (
                            <div
                                key={model.id}
                                className="bg-white border rounded-lg p-3 hover:bg-gray-50 transition cursor-pointer"
                                onClick={() => handleViewModelDetails(model, 'recognition')}
                            >
                                <div className="flex justify-between items-center">
                                    <div>
                                        <h4 className="font-semibold">{model.name}</h4>
                                        <p className="text-sm text-gray-600">
                                            Độ Chính Xác: {(model.accuracy * 100).toFixed(2)}%
                                        </p>
                                    </div>
                                    <span className="text-sm text-gray-500">
                                        Chi Tiết
                                    </span>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            {/* Modal chi tiết mô hình */}
            {renderModelDetailsModal()}
        </div>
    );
}