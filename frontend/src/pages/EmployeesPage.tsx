import React, { useEffect, useState } from 'react';
import { employeesApi, ApiErrorResponse } from '../api';
import type { IEmployee, IEmployeeRequest, IApiError, EmployeeType } from '../types';
import { Modal } from '../components/Modal';
import { ErrorMessage } from '../components/ErrorMessage';

export const EmployeesPage: React.FC = () => {
  const [employees, setEmployees] = useState<IEmployee[]>([]);
  const [roleFilter, setRoleFilter] = useState<string>('');
  const [specFilter, setSpecFilter] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<IApiError | null>(null);

  // Modals state
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [isOvertimeOpen, setIsOvertimeOpen] = useState(false);
  const [selectedMechanic, setSelectedMechanic] = useState<IEmployee | null>(null);
  const [overtimeHours, setOvertimeHours] = useState<number>(5);

  // Form state
  const [employeeForm, setEmployeeForm] = useState<IEmployeeRequest>({
    type: 'MECHANIC',
    name: '',
    phone: '',
    email: '',
    baseSalary: 1000.0,
    specialization: 'Engine',
    hourlyRate: 15.0,
    monthlyBonus: null,
  });

  const handleApiError = (err: unknown) => {
    if (err instanceof ApiErrorResponse) {
      setError(err.error);
    } else if (err instanceof Error) {
      setError({
        timestamp: new Date().toISOString(),
        status: 500,
        error: 'Client Error',
        code: 'INTERNAL_ERROR',
        message: err.message,
        path: '',
      });
    }
  };

  const loadEmployees = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await employeesApi.list(roleFilter || undefined, specFilter || undefined);
      setEmployees(data);
    } catch (err) {
      handleApiError(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadEmployees();
  }, [roleFilter, specFilter]);

  const handleAddEmployee = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setError(null);
      await employeesApi.create(employeeForm);
      setIsAddOpen(false);
      setEmployeeForm({
        type: 'MECHANIC',
        name: '',
        phone: '',
        email: '',
        baseSalary: 1000.0,
        specialization: 'Engine',
        hourlyRate: 15.0,
        monthlyBonus: null,
      });
      await loadEmployees();
    } catch (err) {
      handleApiError(err);
    }
  };

  const handleLogOvertime = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedMechanic) return;
    try {
      setError(null);
      await employeesApi.logOvertime(selectedMechanic.id, overtimeHours);
      setIsOvertimeOpen(false);
      setSelectedMechanic(null);
      await loadEmployees();
    } catch (err) {
      handleApiError(err);
    }
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h2>Employees &amp; Staff Dispatching</h2>
          <p className="page-sub">
            Demonstrates dynamic polymorphism: monthly salaries compute according to employee role
          </p>
        </div>
        <button
          type="button"
          className="btn btn-primary"
          onClick={() => setIsAddOpen(true)}
        >
          + Add Employee
        </button>
      </div>

      <ErrorMessage error={error} onDismiss={() => setError(null)} />

      <div className="filter-bar">
        <div className="filter-group">
          <label>Filter by Role:</label>
          <select
            className="input input-sm"
            value={roleFilter}
            onChange={(e) => {
              setRoleFilter(e.target.value);
              if (e.target.value !== 'MECHANIC') setSpecFilter('');
            }}
          >
            <option value="">All Roles</option>
            <option value="MECHANIC">Mechanic</option>
            <option value="MANAGER">Manager</option>
          </select>
        </div>

        {roleFilter === 'MECHANIC' && (
          <div className="filter-group">
            <label>Specialization:</label>
            <input
              type="text"
              className="input input-sm"
              placeholder="e.g. Engine"
              value={specFilter}
              onChange={(e) => setSpecFilter(e.target.value)}
            />
          </div>
        )}
      </div>

      <div className="panel">
        {loading && <p className="loading-state">Loading employees...</p>}
        {!loading && employees.length === 0 && (
          <p className="empty-state">No employees found.</p>
        )}
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Role</th>
                <th>Contact</th>
                <th>Base Salary</th>
                <th>Role Specifics</th>
                <th>Monthly Salary</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {employees.map((emp) => (
                <tr key={emp.id}>
                  <td>
                    <strong>{emp.name}</strong>
                    <br />
                    <span className="text-muted text-xs">ID: {emp.id.substring(0, 8)}...</span>
                  </td>
                  <td>
                    <span className={`badge badge-${emp.type.toLowerCase()}`}>
                      {emp.type}
                    </span>
                  </td>
                  <td>
                    <div>{emp.email}</div>
                    <div className="text-muted text-xs">{emp.phone}</div>
                  </td>
                  <td>${emp.baseSalary.toFixed(2)}</td>
                  <td>
                    {emp.type === 'MECHANIC' ? (
                      <div className="role-specifics">
                        <div>🔧 <strong>{emp.specialization || 'General'}</strong></div>
                        <div className="text-muted text-xs">
                          Overtime: {emp.overtimeHours || 0} hrs @ ${emp.hourlyRate?.toFixed(2) || '0.00'}/hr
                        </div>
                      </div>
                    ) : (
                      <div className="role-specifics">
                        <div>💼 Managerial Bonus:</div>
                        <div className="text-muted text-xs">+${emp.monthlyBonus?.toFixed(2) || '0.00'}/mo</div>
                      </div>
                    )}
                  </td>
                  <td>
                    <span className="badge badge-salary">
                      ${emp.monthlySalary.toFixed(2)}
                    </span>
                  </td>
                  <td>
                    {emp.type === 'MECHANIC' && (
                      <button
                        type="button"
                        className="btn btn-sm btn-outline"
                        onClick={() => {
                          setSelectedMechanic(emp);
                          setOvertimeHours(5);
                          setIsOvertimeOpen(true);
                        }}
                      >
                        ⏱ Log Overtime
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Add Employee Modal */}
      <Modal
        title="Add New Employee"
        isOpen={isAddOpen}
        onClose={() => setIsAddOpen(false)}
      >
        <form onSubmit={handleAddEmployee} className="form-grid">
          <div className="form-group">
            <label>Employee Role *</label>
            <select
              className="input"
              value={employeeForm.type}
              onChange={(e) =>
                setEmployeeForm({
                  ...employeeForm,
                  type: e.target.value as EmployeeType,
                  specialization: e.target.value === 'MECHANIC' ? 'Engine' : null,
                  hourlyRate: e.target.value === 'MECHANIC' ? 15.0 : null,
                  monthlyBonus: e.target.value === 'MANAGER' ? 300.0 : null,
                })
              }
            >
              <option value="MECHANIC">Mechanic (Overtime rate)</option>
              <option value="MANAGER">Manager (Monthly bonus)</option>
            </select>
          </div>
          <div className="form-group">
            <label>Full Name *</label>
            <input
              type="text"
              className="input"
              required
              value={employeeForm.name}
              onChange={(e) => setEmployeeForm({ ...employeeForm, name: e.target.value })}
            />
          </div>
          <div className="form-group">
            <label>Phone Number *</label>
            <input
              type="text"
              className="input"
              required
              value={employeeForm.phone}
              onChange={(e) => setEmployeeForm({ ...employeeForm, phone: e.target.value })}
            />
          </div>
          <div className="form-group">
            <label>Email Address *</label>
            <input
              type="email"
              className="input"
              required
              value={employeeForm.email}
              onChange={(e) => setEmployeeForm({ ...employeeForm, email: e.target.value })}
            />
          </div>
          <div className="form-group">
            <label>Base Salary ($) *</label>
            <input
              type="number"
              step="0.01"
              min="0"
              className="input"
              required
              value={employeeForm.baseSalary}
              onChange={(e) => setEmployeeForm({ ...employeeForm, baseSalary: parseFloat(e.target.value) || 0 })}
            />
          </div>

          {employeeForm.type === 'MECHANIC' && (
            <>
              <div className="form-group">
                <label>Specialization *</label>
                <input
                  type="text"
                  className="input"
                  placeholder="e.g. Engine, Brakes, Transmission"
                  required
                  value={employeeForm.specialization || ''}
                  onChange={(e) => setEmployeeForm({ ...employeeForm, specialization: e.target.value })}
                />
              </div>
              <div className="form-group">
                <label>Overtime Hourly Rate ($) *</label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  className="input"
                  required
                  value={employeeForm.hourlyRate || ''}
                  onChange={(e) => setEmployeeForm({ ...employeeForm, hourlyRate: parseFloat(e.target.value) || 0 })}
                />
              </div>
            </>
          )}

          {employeeForm.type === 'MANAGER' && (
            <div className="form-group">
              <label>Monthly Manager Bonus ($) *</label>
              <input
                type="number"
                step="0.01"
                min="0"
                className="input"
                required
                value={employeeForm.monthlyBonus || ''}
                onChange={(e) => setEmployeeForm({ ...employeeForm, monthlyBonus: parseFloat(e.target.value) || 0 })}
              />
            </div>
          )}

          <div className="modal-footer">
            <button type="button" className="btn btn-ghost" onClick={() => setIsAddOpen(false)}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary">
              Save Employee
            </button>
          </div>
        </form>
      </Modal>

      {/* Log Overtime Modal */}
      <Modal
        title={`Log Overtime: ${selectedMechanic?.name || ''}`}
        isOpen={isOvertimeOpen}
        onClose={() => setIsOvertimeOpen(false)}
      >
        <form onSubmit={handleLogOvertime} className="form-grid">
          <p className="text-muted">
            Current Overtime: {selectedMechanic?.overtimeHours || 0} hrs. Hourly Rate: ${selectedMechanic?.hourlyRate?.toFixed(2) || '0.00'}
          </p>
          <div className="form-group">
            <label>Additional Overtime Hours *</label>
            <input
              type="number"
              min="1"
              className="input"
              required
              value={overtimeHours}
              onChange={(e) => setOvertimeHours(parseInt(e.target.value, 10) || 1)}
            />
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-ghost" onClick={() => setIsOvertimeOpen(false)}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary">
              Log Overtime
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
