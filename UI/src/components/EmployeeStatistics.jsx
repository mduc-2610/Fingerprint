// EmployeeStatistics.jsx
import React, { useState, useEffect } from 'react';
import { apiService } from '../config/api';

export function EmployeeStatistics() {
    // State management
    const [dateRange, setDateRange] = useState({
        startDate: '',
        endDate: ''
    });
    const [employees, setEmployees] = useState([]);
    const [selectedEmployee, setSelectedEmployee] = useState(null);
    const [accessLogs, setAccessLogs] = useState([]);
    const [error, setError] = useState(null);
    const [areas, setAreas] = useState([]);

    // Initialize default date range (last 30 days)
    useEffect(() => {
        const endDate = new Date();
        const startDate = new Date();
        startDate.setDate(startDate.getDate() - 30);

        setDateRange({
            startDate: formatDateForInput(startDate),
            endDate: formatDateForInput(endDate)
        });

        // Load areas list
        loadAreas();
    }, []);

    // Format date for input datetime-local
    const formatDateForInput = (date) => {
        const pad = (num) => num.toString().padStart(2, '0');
        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
    };

    // Load areas list
    const loadAreas = async () => {
        try {
            const response = await apiService.getAreas();
            setAreas(response);
        } catch (error) {
            setError('Lỗi tải danh sách khu vực');
        }
    };

    // Load employee statistics
    const loadEmployeeStatistics = async () => {
        try {
            const response = await apiService.getEmployeeStatistics(
                dateRange.startDate,
                dateRange.endDate
            );
            setEmployees(response);
            setSelectedEmployee(null);
            setAccessLogs([]);
            setError(null);
        } catch (error) {
            setError('Lỗi tải thống kê nhân viên');
        }
    };

    // Load employee access logs
    const handleEmployeeSelect = async (employeeId) => {
        try {
            const response = await apiService.getEmployeeAccessLogs(
                employeeId,
                dateRange.startDate,
                dateRange.endDate
            );

            setSelectedEmployee(
                employees.find(emp => emp.employeeId === employeeId)
            );
            setAccessLogs(response);
            setError(null);
        } catch (error) {
            setError('Lỗi tải nhật ký truy cập');
        }
    };

    // Handle date range change
    const handleDateChange = (field, value) => {
        setDateRange(prev => ({
            ...prev,
            [field]: value
        }));
    };

    return (
        <div className="container mx-auto p-4">
            <h2 className="text-2xl font-semibold mb-4">Thống Kê Nhân Viên</h2>

            {/* Date filter component */}
            <DateFilter
                startDate={dateRange.startDate}
                endDate={dateRange.endDate}
                onDateChange={handleDateChange}
                onSubmit={loadEmployeeStatistics}
            />

            {/* Error message component */}
            {error && <ErrorMessage message={error} />}

            {/* Employee list component */}
            <EmployeeList
                employees={employees}
                selectedEmployeeId={selectedEmployee?.employeeId}
                onEmployeeSelect={handleEmployeeSelect}
            />

            {/* Access log details component */}
            {selectedEmployee && (
                <AccessLogDetails
                    employeeName={selectedEmployee.fullName}
                    accessLogs={accessLogs}
                    areas={areas}
                />
            )}
        </div>
    );
}

function DateFilter({ startDate, endDate, onDateChange, onSubmit }) {
    return (
        <div className="grid md:grid-cols-3 gap-4 mb-6">
            <div>
                <label className="block mb-2">Ngày Bắt Đầu</label>
                <input
                    type="datetime-local"
                    value={startDate}
                    onChange={(e) => onDateChange('startDate', e.target.value)}
                    className="w-full p-2 border rounded-md"
                />
            </div>
            <div>
                <label className="block mb-2">Ngày Kết Thúc</label>
                <input
                    type="datetime-local"
                    value={endDate}
                    onChange={(e) => onDateChange('endDate', e.target.value)}
                    className="w-full p-2 border rounded-md"
                />
            </div>
            <div className="flex items-end">
                <button
                    onClick={onSubmit}
                    className="w-full bg-blue-600 text-white py-2 rounded-md hover:bg-blue-700 transition"
                >
                    Tải Thống Kê
                </button>
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

function EmployeeList({ employees, selectedEmployeeId, onEmployeeSelect }) {
    return (
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
                        {employees.length > 0 ? (
                            employees.map((employee) => (
                                <tr
                                    key={employee.employeeId}
                                    onClick={() => onEmployeeSelect(employee.employeeId)}
                                    className={`cursor-pointer hover:bg-gray-50 ${selectedEmployeeId === employee.employeeId ? 'bg-blue-50' : ''
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
                            ))
                        ) : (
                            <tr>
                                <td colSpan="4" className="p-4 text-center text-gray-500">
                                    Không có dữ liệu
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}

// components/AccessLogDetails.jsx
function AccessLogDetails({ employeeName, accessLogs, areas }) {
    const [filters, setFilters] = useState({
        accessType: '',
        area: ''
    });
    const [filteredLogs, setFilteredLogs] = useState([]);

    // Filter logs when accessLogs or filters change
    useEffect(() => {
        setFilteredLogs(accessLogs.filter(log => {
            const accessTypeMatch = !filters.accessType ||
                log.accessType.toLowerCase() === filters.accessType.toLowerCase();

            const areaMatch = !filters.area ||
                (log.area?.name || '').toLowerCase() === filters.area.toLowerCase();

            return accessTypeMatch && areaMatch;
        }));
    }, [accessLogs, filters]);

    // Handle filter changes
    const handleFilterChange = (field, value) => {
        setFilters(prev => ({
            ...prev,
            [field]: value
        }));
    };

    return (
        <div className="mt-6 bg-white rounded-lg shadow-md">
            <div className="p-4 border-b flex justify-between items-center">
                <h3 className="text-lg font-semibold">
                    Nhật Ký Truy Cập - {employeeName}
                </h3>
                <div className="flex space-x-2">
                    <select
                        value={filters.accessType}
                        onChange={(e) => handleFilterChange('accessType', e.target.value)}
                        className="p-2 border rounded-md"
                    >
                        <option value="">Loại Truy Cập</option>
                        <option value="ENTRY">Vào</option>
                        <option value="EXIT">Ra</option>
                    </select>
                    <select
                        value={filters.area}
                        onChange={(e) => handleFilterChange('area', e.target.value)}
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
                        {filteredLogs.length > 0 ? (
                            filteredLogs.map((log, index) => (
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
                                <td colSpan="3" className="p-4 text-center text-gray-500">
                                    Không có nhật ký truy cập
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
}