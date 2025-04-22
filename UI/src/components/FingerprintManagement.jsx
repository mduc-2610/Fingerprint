import React, { useState, useEffect } from 'react';
import { apiService } from '../config/api';
import { formatFingerprintPosition, formatDate } from '../utils/formatters';

export function FingerprintManagement() {
  const [employees, setEmployees] = useState([]);
  const [selectedEmployee, setSelectedEmployee] = useState(null);
  const [fingerprintSamples, setFingerprintSamples] = useState([]);
  const [maxSamples, setMaxSamples] = useState(0);
  const [error, setError] = useState(null);

  // Tải danh sách nhân viên
  useEffect(() => {
    const loadEmployees = async () => {
      try {
        const response = await apiService.getEmployees();
        setEmployees(response);
      } catch (error) {
        setError('Lỗi tải danh sách nhân viên');
      }
    };

    loadEmployees();
  }, []);

  // Tải dấu vân tay khi chọn nhân viên
  useEffect(() => {
    const loadFingerprints = async () => {
      if (selectedEmployee) {
        try {
          const response = await apiService.getEmployeeFingerprints(selectedEmployee.id, false);
          setFingerprintSamples(response);
          setMaxSamples(selectedEmployee.maxNumberSamples);
          setError(null);
        } catch (error) {
          setError('Lỗi tải dấu vân tay');
        }
      }
    };

    loadFingerprints();
  }, [selectedEmployee]);

  // Đặt số lượng dấu vân tay tối đa
  const handleSetMaxSamples = async () => {
    if (!selectedEmployee) return;

    try {
      await apiService.setMaxFingerprintSamples(selectedEmployee.id, maxSamples);
      setError(null);
    } catch (error) {
      setError('Lỗi đặt số lượng dấu vân tay tối đa');
    }
  };

  // Kích hoạt/vô hiệu hóa dấu vân tay
  const handleToggleFingerprintStatus = async (fingerprintId, enable) => {
    try {
      await apiService.toggleFingerprintStatus(fingerprintId, enable);
      
      // Tải lại danh sách dấu vân tay
      const response = await apiService.getEmployeeFingerprints(selectedEmployee.id, false);
      setFingerprintSamples(response);
      setError(null);
    } catch (error) {
      setError(`Lỗi ${enable ? 'kích hoạt' : 'vô hiệu hóa'} dấu vân tay`);
    }
  };

  // Xóa dấu vân tay
  const handleDeleteFingerprint = async (fingerprintId) => {
    try {
      await apiService.deleteFingerprint(fingerprintId);
      
      // Tải lại danh sách dấu vân tay
      const response = await apiService.getEmployeeFingerprints(selectedEmployee.id, false);
      setFingerprintSamples(response);
      setError(null);
    } catch (error) {
      setError('Lỗi xóa dấu vân tay');
    }
  };

  // Vô hiệu hóa tất cả dấu vân tay
  const handleDisableAllFingerprints = async () => {
    if (!selectedEmployee) return;

    try {
      await apiService.disableAllFingerprints(selectedEmployee.id);
      
      // Tải lại danh sách dấu vân tay
      const response = await apiService.getEmployeeFingerprints(selectedEmployee.id, false);
      setFingerprintSamples(response);
      setError(null);
    } catch (error) {
      setError('Lỗi vô hiệu hóa tất cả dấu vân tay');
    }
  };

  // Tính số lượng dấu vân tay đang hoạt động
  const activeFingerprintCount = fingerprintSamples.filter(sample => sample.active).length;

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
      {/* Danh sách nhân viên */}
      <div className="bg-white rounded-lg shadow-md">
        <div className="p-4 border-b">
          <h2 className="text-lg font-semibold">Danh Sách Nhân Viên</h2>
        </div>
        <div className="max-h-[500px] overflow-y-auto">
          <table className="w-full">
            <thead className="sticky top-0 bg-gray-100">
              <tr>
                <th className="p-2 text-left">Ảnh</th>
                <th className="p-2 text-left">Mã NV</th>
                <th className="p-2 text-left">Tên</th>
                <th className="p-2 text-left">Dấu Vân Tay</th>
              </tr>
            </thead>
            <tbody>
              {employees.map((employee) => (
                <tr 
                  key={employee.id} 
                  onClick={() => setSelectedEmployee(employee)}
                  className={`cursor-pointer hover:bg-gray-100 ${
                    selectedEmployee?.id === employee.id ? 'bg-blue-50' : ''
                  }`}
                >
                  <td className="p-2">
                    <img 
                      src={employee.photoUrl || '/avt.png'} 
                      alt={employee.fullName}
                      className="w-10 h-10 rounded-full object-cover"
                    />
                  </td>
                  <td className="p-2">{employee.id}</td>
                  <td className="p-2">{employee.fullName}</td>
                  <td className="p-2">
                    {fingerprintSamples.filter(s => s.active).length} / {employee.maxNumberSamples}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Quản lý dấu vân tay */}
      <div className="bg-white rounded-lg shadow-md">
        {selectedEmployee ? (
          <>
            <div className="p-4 border-b flex items-center">
              <img 
                src={selectedEmployee.photoUrl || '/avt.png'} 
                alt={selectedEmployee.fullName}
                className="w-16 h-16 rounded-full object-cover mr-4"
              />
              <div>
                <h3 className="text-lg font-semibold">{selectedEmployee.fullName}</h3>
                <p className="text-gray-600">Mã NV: {selectedEmployee.id}</p>
                <p className="text-sm text-gray-500">
                  Dấu vân tay: {activeFingerprintCount} / {selectedEmployee.maxNumberSamples}
                </p>
              </div>
            </div>

            <div className="p-4">
              <div className="flex mb-4 space-x-2">
                <input 
                  type="number" 
                  value={maxSamples} 
                  onChange={(e) => setMaxSamples(Number(e.target.value))}
                  min="1"
                  className="flex-grow p-2 border rounded"
                  placeholder="Số lượng dấu vân tay tối đa"
                />
                <button 
                  onClick={handleSetMaxSamples}
                  className="bg-blue-500 text-white px-4 py-2 rounded"
                >
                  Đặt Giới Hạn
                </button>
                <button 
                  onClick={handleDisableAllFingerprints}
                  className="bg-yellow-500 text-white px-4 py-2 rounded"
                >
                  Vô Hiệu Hóa Tất Cả
                </button>
              </div>

              {error && (
                <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
                  {error}
                </div>
              )}

              <h4 className="text-lg font-semibold mb-2">Danh Sách Dấu Vân Tay</h4>
              <div className="grid grid-cols-2 gap-4">
                {fingerprintSamples.map((sample) => (
                  <div 
                    key={sample.id} 
                    className={`border rounded-lg p-4 ${
                      sample.active ? 'border-green-500 bg-green-50' : 'border-gray-300 bg-gray-50 opacity-60'
                    }`}
                  >
                    <div className="flex justify-between items-center mb-2">
                      <h5 className="font-semibold">
                        {formatFingerprintPosition(sample.position)}
                      </h5>
                      <span className={`px-2 py-1 rounded text-xs ${
                        sample.active ? 'bg-green-500 text-white' : 'bg-gray-300 text-gray-700'
                      }`}>
                        {sample.active ? 'Hoạt Động' : 'Ngừng Hoạt Động'}
                      </span>
                    </div>
                    <div className="text-sm text-gray-600 mb-2">
                      <p>Mã: {sample.id.substring(0, 8)}...</p>
                      <p>Ngày Đăng Ký: {formatDate(sample.capturedAt)}</p>
                      {sample.quality && (
                        <p>Chất Lượng: {(sample.quality * 100).toFixed(2)}%</p>
                      )}
                    </div>
                    <div className="flex space-x-2">
                      {sample.active ? (
                        <button 
                          onClick={() => handleToggleFingerprintStatus(sample.id, false)}
                          className="flex-grow bg-yellow-500 text-white px-2 py-1 rounded text-sm"
                        >
                          Vô Hiệu Hóa
                        </button>
                      ) : (
                        <button 
                          onClick={() => handleToggleFingerprintStatus(sample.id, true)}
                          className="flex-grow bg-green-500 text-white px-2 py-1 rounded text-sm"
                        >
                          Kích Hoạt
                        </button>
                      )}
                      <button 
                        onClick={() => handleDeleteFingerprint(sample.id)}
                        className="bg-red-500 text-white px-2 py-1 rounded text-sm"
                      >
                        Xóa
                      </button>
                    </div>
                  </div>
                ))}
              </div>

              {fingerprintSamples.length === 0 && (
                <div className="text-center text-gray-500 py-4">
                  Không có dấu vân tay nào được đăng ký
                </div>
              )}
            </div>
          </>
        ) : (
          <div className="p-4 text-center text-gray-500">
            Chọn nhân viên để quản lý dấu vân tay
          </div>
        )}
      </div>
    </div>
  );
}