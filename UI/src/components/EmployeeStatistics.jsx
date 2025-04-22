import React, { useState, useEffect } from 'react';
import { apiService } from '../config/api';

export function EmployeeStatistics() {
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');
    const [employees, setEmployees] = useState([]);
    const [selectedEmployee, setSelectedEmployee] = useState(null);
    const [accessLogs, setAccessLogs] = useState([]);
    const [error, setError] = useState(null);
    const [filteredAccessLogs, setFilteredAccessLogs] = useState([]);
    const [filters, setFilters] = useState({
        accessType: '',
        area: ''
    });
    const [areas, setAreas] = useState([]);

    // Thiết lập ngày mặc định (30 ngày qua)
    useEffect(() => {
        const endDate = new Date();
        const startDate = new Date();
        startDate.setDate(startDate.getDate() - 30);

        setStartDate(formatDateForInput(startDate));
        setEndDate(formatDateForInput(endDate));

        // Tải danh sách khu vực
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

    // Định dạng ngày cho input datetime-local
    const formatDateForInput = (date) => {
        const pad = (num) => num.toString().padStart(2, '0');

        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
    };

    // Tải thống kê nhân viên
    const loadEmployeeStatistics = async () => {
        try {
            const response = await apiService.getEmployeeStatistics(startDate, endDate);
            setEmployees(response);
            setSelectedEmployee(null);
            setAccessLogs([]);
        } catch (error) {
            setError('Lỗi tải thống kê nhân viên');
        }
    };

    // Tải nhật ký truy cập của nhân viên
    const loadEmployeeAccessLogs = async (employeeId) => {
        try {
            const response = await apiService.getEmployeeAccessLogs(
                employeeId,
                startDate,
                endDate
            );
            setSelectedEmployee(
                employees.find(emp => emp.employeeId === employeeId)
            );
            setAccessLogs(response);
        } catch (error) {
            setError('Lỗi tải nhật ký truy cập');
        }
    };

    useEffect(() => {
        setFilteredAccessLogs(accessLogs.filter(log => {
            const accessTypeMatch = !filters.accessType ||
                log.accessType.toLowerCase() === filters.accessType.toLowerCase();

            const areaMatch = !filters.area ||
                (log.area?.name || '').toLowerCase() === filters.area.toLowerCase();

            return accessTypeMatch && areaMatch;
        }))
    }, [accessLogs, filters]);


    return (
        <div>
            <h2 className="text-2xl font-semibold mb-4">Thống Kê Nhân Viên</h2>

            {/* Bộ lọc ngày */}
            <div className="grid md:grid-cols-3 gap-4 mb-6">
                <div>
                    <label className="block mb-2">Ngày Bắt Đầu</label>
                    <input
                        type="datetime-local"
                        value={startDate}
                        onChange={(e) => setStartDate(e.target.value)}
                        className="w-full p-2 border rounded-md"
                    />
                </div>
                <div>
                    <label className="block mb-2">Ngày Kết Thúc</label>
                    <input
                        type="datetime-local"
                        value={endDate}
                        onChange={(e) => setEndDate(e.target.value)}
                        className="w-full p-2 border rounded-md"
                    />
                </div>
                <div className="flex items-end">
                    <button
                        onClick={loadEmployeeStatistics}
                        className="w-full bg-blue-600 text-white py-2 rounded-md hover:bg-blue-700 transition"
                    >
                        Tải Thống Kê
                    </button>
                </div>
            </div>

            {/* Hiển thị lỗi */}
            {error && (
                <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
                    {error}
                </div>
            )}

            {/* Danh sách nhân viên */}
            <div className="bg-white rounded-lg shadow-md">
                <div className="p-4 border-b">
                    <h3 className="text-lg font-semibold">Danh Sách Nhân Viên</h3>
                </div>
                <div className="overflow-x-auto">
                    <table className="w-full">
                        <thead className="bg-gray-100">
                            <tr>
                                <th className="p-2 text-left">Ảnh</th>
                                <th className="p-2 text-left">Mã NV</th>
                                <th className="p-2 text-left">Tên</th>
                                <th className="p-2 text-left">Tổng Truy Cập</th>
                            </tr>
                        </thead>
                        <tbody>
                            {employees.map((employee) => (
                                <tr
                                    key={employee.employeeId}
                                    onClick={() => loadEmployeeAccessLogs(employee.employeeId)}
                                    className={`cursor-pointer hover:bg-gray-50 ${selectedEmployee?.id === employee.employeeId ? 'bg-blue-50' : ''
                                        }`}
                                >
                                    <td className="p-2">
                                        <img
                                            src={employee.photoUrl || '/avt.png'}
                                            alt={employee.fullName}
                                            className="w-10 h-10 rounded-full object-cover"
                                        />
                                    </td>
                                    <td className="p-2">{employee.employeeId}</td>
                                    <td className="p-2">{employee.fullName}</td>
                                    <td className="p-2">{employee.totalAccesses}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>

            {/* Chi tiết nhật ký truy cập */}
            {selectedEmployee && (
                <div className="mt-6 bg-white rounded-lg shadow-md">
                    <div className="p-4 border-b flex justify-between items-center">
                        <h3 className="text-lg font-semibold">
                            Nhật Ký Truy Cập - {selectedEmployee.fullName}
                        </h3>
                        <div className="flex space-x-2">
                            <select
                                value={filters.accessType}
                                onChange={(e) => setFilters(prev => ({
                                    ...prev,
                                    accessType: e.target.value
                                }))}
                                className="p-2 border rounded-md"
                            >
                                <option value="">Loại Truy Cập</option>
                                <option value="ENTRY">Vào</option>
                                <option value="EXIT">Ra</option>
                            </select>
                            <select
                                value={filters.area}
                                onChange={(e) => setFilters(prev => ({
                                    ...prev,
                                    area: e.target.value
                                }))}
                                className="p-2 border rounded-md"
                            >
                                <option value="">Khu Vực</option>
                                {areas.map((area) => (
                                    <option key={area.id} value={area.name}>
                                        {area.name}
                                    </option>
                                ))}
                            </select>
                        </div>
                    </div>
                    <div className="overflow-x-auto">
                        <table className="w-full">
                            <thead className="bg-gray-100">
                                <tr>
                                    <th className="p-2 text-left">Thời Gian</th>
                                    <th className="p-2 text-left">Loại Truy Cập</th>
                                    <th className="p-2 text-left">Khu Vực</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filteredAccessLogs.length > 0 ? (
                                    filteredAccessLogs.map((log, index) => (
                                        <tr
                                            key={index}
                                            className={`
                        ${log.authorized ? 'bg-green-50' : 'bg-red-50'}
                        hover:bg-opacity-75
                      `}
                                        >
                                            <td className="p-2">
                                                {new Date(log.timestamp).toLocaleString()}
                                            </td>
                                            <td className="p-2">{log.accessType}</td>
                                            <td className="p-2">{log.area?.name || 'Chung'}</td>
                                        </tr>
                                    ))
                                ) : (
                                    <tr>
                                        <td
                                            colSpan="3"
                                            className="p-4 text-center text-gray-500"
                                        >
                                            Không có nhật ký truy cập
                                        </td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}
        </div>
    );
}