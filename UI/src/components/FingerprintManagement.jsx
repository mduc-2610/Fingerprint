import React, { useState, useEffect } from 'react';
import { apiService } from '../config/api';
import { formatFingerprintPosition, formatDate } from '../utils/formatters';

export function FingerprintManagement() {
  const [employees, setEmployees] = useState([]);
  const [employeeFingerprints, setEmployeeFingerprints] = useState({});
  const [selectedEmployee, setSelectedEmployee] = useState(null);
  const [fingerprintSamples, setFingerprintSamples] = useState([]);
  const [maxSamples, setMaxSamples] = useState(0);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  // Tải danh sách nhân viên
  useEffect(() => {
    const loadEmployees = async () => {
      setLoading(true);
      try {
        const response = await apiService.getEmployees();
        setEmployees(response);

        // Tải sơ bộ thông tin vân tay cho mỗi nhân viên
        const fingerprintsData = {};
        for (const employee of response) {
          try {
            const fingerprints = await apiService.getEmployeeFingerprints(employee.id, true);
            fingerprintsData[employee.id] = fingerprints;
          } catch (error) {
            console.error(`Lỗi tải vân tay cho nhân viên ${employee.id}:`, error);
            fingerprintsData[employee.id] = [];
          }
        }
        setEmployeeFingerprints(fingerprintsData);
      } catch (error) {
        console.error('Lỗi tải danh sách nhân viên:', error);
        setError('Lỗi tải danh sách nhân viên');
      } finally {
        setLoading(false);
      }
    };

    loadEmployees();
  }, []);

  // Tải dấu vân tay chi tiết khi chọn nhân viên
  useEffect(() => {
    const loadFingerprints = async () => {
      if (selectedEmployee) {
        setLoading(true);
        try {
          const response = await apiService.getEmployeeFingerprints(selectedEmployee.id, false);
          setFingerprintSamples(response);
          setMaxSamples(selectedEmployee.maxNumberSamples);
          setError(null);
        } catch (error) {
          console.error('Lỗi tải dấu vân tay:', error);
          setError('Lỗi tải dấu vân tay');
          setFingerprintSamples([]);
        } finally {
          setLoading(false);
        }
      }
    };

    loadFingerprints();
  }, [selectedEmployee]);

  // Đặt số lượng dấu vân tay tối đa
  const handleSetMaxSamples = async () => {
    if (!selectedEmployee) return;

    setLoading(true);
    try {
      await apiService.setMaxFingerprintSamples(selectedEmployee.id, maxSamples);

      // Cập nhật thông tin nhân viên trong state
      setEmployees(prevEmployees =>
        prevEmployees.map(emp =>
          emp.id === selectedEmployee.id
            ? { ...emp, maxNumberSamples: maxSamples }
            : emp
        )
      );

      // Cập nhật selectedEmployee
      setSelectedEmployee(prev => ({ ...prev, maxNumberSamples: maxSamples }));

      setError(null);
    } catch (error) {
      console.error('Lỗi đặt số lượng dấu vân tay tối đa:', error);
      setError('Lỗi đặt số lượng dấu vân tay tối đa');
    } finally {
      setLoading(false);
    }
  };

  // Kích hoạt/vô hiệu hóa dấu vân tay
  const handleToggleFingerprintStatus = async (fingerprintId, enable) => {
    if (!selectedEmployee) return;

    setLoading(true);
    try {
      if (enable) {
        await apiService.enableFingerprint(fingerprintId);
      } else {
        await apiService.disableFingerprint(fingerprintId);
      }

      // Tải lại danh sách dấu vân tay
      const response = await apiService.getEmployeeFingerprints(selectedEmployee.id, false);
      setFingerprintSamples(response);

      // Cập nhật thông tin vân tay tổng quan
      const summaryResponse = await apiService.getEmployeeFingerprints(selectedEmployee.id, true);
      setEmployeeFingerprints(prev => ({
        ...prev,
        [selectedEmployee.id]: summaryResponse
      }));

      setError(null);
    } catch (error) {
      console.error(`Lỗi ${enable ? 'kích hoạt' : 'vô hiệu hóa'} dấu vân tay:`, error);
      setError(`Lỗi ${enable ? 'kích hoạt' : 'vô hiệu hóa'} dấu vân tay`);
    } finally {
      setLoading(false);
    }
  };

  // Xóa dấu vân tay
  const handleDeleteFingerprint = async (fingerprintId) => {
    if (!selectedEmployee) return;

    setLoading(true);
    try {
      await apiService.deleteFingerprint(fingerprintId);

      // Tải lại danh sách dấu vân tay
      const response = await apiService.getEmployeeFingerprints(selectedEmployee.id, false);
      setFingerprintSamples(response);

      // Cập nhật thông tin vân tay tổng quan
      const summaryResponse = await apiService.getEmployeeFingerprints(selectedEmployee.id, true);
      setEmployeeFingerprints(prev => ({
        ...prev,
        [selectedEmployee.id]: summaryResponse
      }));

      setError(null);
    } catch (error) {
      console.error('Lỗi xóa dấu vân tay:', error);
      setError('Lỗi xóa dấu vân tay');
    } finally {
      setLoading(false);
    }
  };

  // Vô hiệu hóa tất cả dấu vân tay
  const handleDisableAllFingerprints = async () => {
    if (!selectedEmployee) return;

    setLoading(true);
    try {
      await apiService.disableAllFingerprints(selectedEmployee.id);

      // Tải lại danh sách dấu vân tay
      const response = await apiService.getEmployeeFingerprints(selectedEmployee.id, false);
      setFingerprintSamples(response);

      // Cập nhật thông tin vân tay tổng quan
      const summaryResponse = await apiService.getEmployeeFingerprints(selectedEmployee.id, true);
      setEmployeeFingerprints(prev => ({
        ...prev,
        [selectedEmployee.id]: summaryResponse
      }));

      setError(null);
    } catch (error) {
      console.error('Lỗi vô hiệu hóa tất cả dấu vân tay:', error);
      setError('Lỗi vô hiệu hóa tất cả dấu vân tay');
    } finally {
      setLoading(false);
    }
  };

  // Kích hoạt tất cả dấu vân tay
  const handleEnableAllFingerprints = async () => {
    if (!selectedEmployee) return;

    setLoading(true);
    try {
      await apiService.enableAllFingerprints(selectedEmployee.id);

      // Tải lại danh sách dấu vân tay
      const response = await apiService.getEmployeeFingerprints(selectedEmployee.id, false);
      setFingerprintSamples(response);

      // Cập nhật thông tin vân tay tổng quan
      const summaryResponse = await apiService.getEmployeeFingerprints(selectedEmployee.id, true);
      setEmployeeFingerprints(prev => ({
        ...prev,
        [selectedEmployee.id]: summaryResponse
      }));

      setError(null);
    } catch (error) {
      console.error('Lỗi kích hoạt tất cả dấu vân tay:', error);
      setError('Lỗi kích hoạt tất cả dấu vân tay');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteAllFingerprints = async () => {
    if (!selectedEmployee) return;

    setLoading(true);
    try {
      await apiService.deleteAllFingerprints(selectedEmployee.id);

      // Tải lại danh sách dấu vân tay
      const response = await apiService.getEmployeeFingerprints(selectedEmployee.id, false);
      setFingerprintSamples(response);

      // Cập nhật thông tin vân tay tổng quan
      const summaryResponse = await apiService.getEmployeeFingerprints(selectedEmployee.id, true);
      setEmployeeFingerprints(prev => ({
        ...prev,
        [selectedEmployee.id]: summaryResponse
      }));

      setError(null);
    } catch (error) {
      console.error('Lỗi kích hoạt tất cả dấu vân tay:', error);
      setError('Lỗi kích hoạt tất cả dấu vân tay');
    } finally {
      setLoading(false);
    }
  }

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
          {loading && employees.length === 0 ? (
            <div className="p-4 text-center text-gray-500">Đang tải dữ liệu...</div>
          ) : (
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
                {employees.map((employee) => {
                  const fingerprints = employeeFingerprints[employee.id] || [];
                  const activeFingerprints = fingerprints.filter(fp => fp.active).length;

                  return (
                    <tr
                      key={employee.id}
                      onClick={() => setSelectedEmployee(employee)}
                      className={`cursor-pointer hover:bg-gray-100 ${selectedEmployee?.id === employee.id ? 'bg-blue-50' : ''
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
                        <div>{activeFingerprints} / {fingerprints.length} / {employee.maxNumberSamples}</div>
                        <div className="text-xs text-gray-400 italic">
                          (Hoạt động / Đã đăng ký / Giới hạn tối đa)
                        </div>
                      </td>
                    </tr>
                  );
                })}

                {employees.length === 0 && !loading && (
                  <tr>
                    <td colSpan="4" className="p-4 text-center text-gray-500">
                      Không có nhân viên nào
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          )}
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
                  Dấu vân tay:  {fingerprintSamples.length} / {selectedEmployee.maxNumberSamples}
                </p>
                <p className="text-xs text-gray-400 italic mt-1">
                  ( Đã đăng ký / Giới hạn tối đa)
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
                  disabled={loading}
                />
                <button
                  onClick={handleSetMaxSamples}
                  className="bg-blue-500 text-white px-4 py-2 rounded disabled:bg-blue-300"
                  disabled={loading}
                >
                  Đặt Giới Hạn
                </button>
                {fingerprintSamples.some((sample) => sample.active) ? (
                  <button
                    onClick={handleDisableAllFingerprints}
                    className="bg-yellow-500 text-white px-4 py-2 rounded disabled:bg-yellow-300"
                    disabled={loading}
                  >
                    Vô Hiệu Hóa Tất Cả
                  </button>
                ) : (
                  <button
                    onClick={handleEnableAllFingerprints}
                    className="bg-green-500 text-white px-4 py-2 rounded disabled:bg-green-300"
                    disabled={loading || fingerprintSamples.length === 0}
                  >
                    Kích Hoạt Tất Cả
                  </button>
                )}
              </div>

              {error && (
                <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
                  {error}
                </div>
              )}

              <h4 className="text-lg font-semibold mb-2">Danh Sách Dấu Vân Tay</h4>

              {loading && fingerprintSamples.length === 0 ? (
                <div className="text-center text-gray-500 py-4">
                  Đang tải dữ liệu...
                </div>
              ) : (
                <>
                  {fingerprintSamples.length > 0 ? (
                    <>
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        {fingerprintSamples.map((sample) => (
                          <div
                            key={sample.id}
                            className={`border rounded-lg p-4 ${sample.active
                              ? 'border-green-500 bg-green-50'
                              : 'border-gray-300 bg-gray-50 opacity-70'
                              }`}
                          >
                            <div className="flex justify-between items-center mb-2">
                              <h5 className="font-semibold">
                                {formatFingerprintPosition(sample.position)}
                              </h5>
                              <span
                                className={`px-2 py-1 rounded text-xs ${sample.active
                                  ? 'bg-green-500 text-white'
                                  : 'bg-gray-300 text-gray-700'
                                  }`}
                              >
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
                                  className="flex-grow bg-yellow-500 text-white px-2 py-1 rounded text-sm disabled:bg-yellow-300"
                                  disabled={loading}
                                >
                                  Vô Hiệu Hóa
                                </button>
                              ) : (
                                <button
                                  onClick={() => handleToggleFingerprintStatus(sample.id, true)}
                                  className="flex-grow bg-green-500 text-white px-2 py-1 rounded text-sm disabled:bg-green-300"
                                  disabled={loading}
                                >
                                  Kích Hoạt
                                </button>
                              )}
                              <button
                                onClick={() => handleDeleteFingerprint(sample.id)}
                                className="bg-red-500 text-white px-2 py-1 rounded text-sm disabled:bg-red-300"
                                disabled={loading}
                              >
                                Xóa
                              </button>
                            </div>
                          </div>
                        ))}
                      </div>
                      <div style={{
                        display: 'flex',
                        "justifyContent": 'flex-end',
                      }}>

                        <button
                          onClick={handleDeleteAllFingerprints}
                          className="mt-4 bg-red-500 text-white px-4 py-2 rounded disabled:bg-red-300"
                          disabled={loading}
                        >
                          xóa tất cả
                        </button>
                      </div>
                    </>

                  ) : (
                    <div className="text-center text-gray-500 py-4">
                      Không có dấu vân tay nào được đăng ký
                    </div>
                  )}
                </>
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