import React, { useState, useEffect } from 'react';
import { User } from 'lucide-react';
import { fetchData, Employee } from '../config/api';


export function EmployeeSelection({
    onEmployeeSelect,
    selectedEmployeeId,
    showFingerprintCount = false
}) {
    const [employees, setEmployees] = useState([]);
    const [searchTerm, setSearchTerm] = useState('');
    const [fingerprintCounts, setFingerprintCounts] = useState({});

    useEffect(() => {
        async function loadEmployees() {
            try {
                const fetchedEmployees = await fetchData('/employee');
                setEmployees(fetchedEmployees);

                // Fetch fingerprint counts if needed
                if (showFingerprintCount) {
                    const counts = await Promise.all(
                        fetchedEmployees.map(async (employee) => {
                            const samples = await fetchData(`/fingerprint-sample/employee/${employee.id}`);
                            const activeSamples = samples.filter((sample) => sample.active).length;
                            return {
                                employeeId: employee.id,
                                count: `${activeSamples} / ${employee.maxNumberSamples}`
                            };
                        })
                    );

                    const countsMap = counts.reduce((acc, curr) => {
                        acc[curr.employeeId] = curr.count;
                        return acc;
                    }, {});

                    setFingerprintCounts(countsMap);
                }
            } catch (error) {
                console.error('Failed to load employees', error);
            }
        }

        loadEmployees();
    }, [showFingerprintCount]);

    const filteredEmployees = employees.filter(employee =>
        employee.fullName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        employee.id.toLowerCase().includes(searchTerm.toLowerCase())
    );

    return (
        <div className="bg-white rounded-lg shadow-md">
            <div className="p-4 border-b">
                <h5 className="text-lg font-semibold">Employee Selection</h5>
            </div>
            <div className="p-4">
                <input
                    type="text"
                    placeholder="Search by name or ID"
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="w-full px-3 py-2 border rounded-md mb-4"
                />
                <div className="max-h-[300px] overflow-y-auto">
                    <table className="w-full">
                        <thead className="sticky top-0 bg-white border-b">
                            <tr>
                                <th className="p-2 text-left">Photo</th>
                                <th className="p-2 text-left">ID</th>
                                <th className="p-2 text-left">Name</th>
                                {showFingerprintCount && <th className="p-2 text-left">Fingerprints</th>}
                            </tr>
                        </thead>
                        <tbody>
                            {filteredEmployees.map((employee) => (
                                <tr
                                    key={employee.id}
                                    onClick={() => onEmployeeSelect(employee)}
                                    className={`cursor-pointer hover:bg-gray-100 ${selectedEmployeeId === employee.id ? 'bg-blue-50' : ''
                                        }`}
                                >
                                    <td className="p-2">
                                        {employee.photoUrl ? (
                                            <img
                                                src="../../public/avt.png"
                                                className="w-10 h-10 rounded-full object-cover"
                                            />
                                        ) : (
                                            <div className="w-10 h-10 bg-gray-200 rounded-full flex items-center justify-center">
                                                <User className="w-6 h-6 text-gray-500" />
                                            </div>
                                        )}
                                    </td>
                                    <td className="p-2">{employee.id}</td>
                                    <td className="p-2">{employee.fullName}</td>
                                    {showFingerprintCount && (
                                        <td className="p-2">{fingerprintCounts[employee.id] || 'Loading...'}</td>
                                    )}
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}