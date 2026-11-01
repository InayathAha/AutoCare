import React, { useEffect, useState } from 'react';
import { tasksApi, inventoryApi, invoicesApi, customersApi, ApiErrorResponse } from '../api';
import type {
  ITask,
  IInventoryItem,
  IInvoiceSummary,
  ICustomer,
  IApiError,
} from '../types';
import type { TabKey } from '../components/Navbar';
import { ErrorMessage } from '../components/ErrorMessage';

interface DashboardPageProps {
  onNavigate: (tab: TabKey, id?: string) => void;
}

export const DashboardPage: React.FC<DashboardPageProps> = ({ onNavigate }) => {
  const [tasks, setTasks] = useState<ITask[]>([]);
  const [lowStockItems, setLowStockItems] = useState<IInventoryItem[]>([]);
  const [invoices, setInvoices] = useState<IInvoiceSummary[]>([]);
  const [customers, setCustomers] = useState<ICustomer[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<IApiError | null>(null);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [allTasks, lowItems, allInvoices, allCustomers] = await Promise.all([
        tasksApi.list(),
        inventoryApi.listLowStock(),
        invoicesApi.list(),
        customersApi.list(),
      ]);

      setTasks(allTasks);
      setLowStockItems(lowItems);
      setInvoices(allInvoices);
      setCustomers(allCustomers);
    } catch (err) {
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
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDashboardData();
  }, []);

  // Computed metrics
  const openTasks = tasks.filter((t) => t.status === 'PENDING' || t.status === 'IN_PROGRESS');
  const pendingTasksCount = tasks.filter((t) => t.status === 'PENDING').length;
  const inProgressTasksCount = tasks.filter((t) => t.status === 'IN_PROGRESS').length;
  const completedTasksCount = tasks.filter((t) => t.status === 'COMPLETED').length;

  const unpaidInvoices = invoices.filter((i) => i.status === 'OPEN' || i.status === 'ISSUED');
  const unpaidTotal = unpaidInvoices.reduce((sum, inv) => sum + inv.totalAmount, 0);

  const totalVehiclesCount = customers.reduce((sum, c) => sum + (c.vehicleCount || 0), 0);

  return (
    <div className="page-container dashboard-page">
      <div className="page-header">
        <div>
          <h2>AutoCare Garage Dashboard</h2>
          <p className="page-subtitle">Real-time workshop activity, inventory health, and billing status</p>
        </div>
        <button
          type="button"
          className="btn btn-secondary"
          onClick={loadDashboardData}
          disabled={loading}
        >
          {loading ? 'Refreshing...' : '🔄 Refresh Overview'}
        </button>
      </div>

      <ErrorMessage error={error} onDismiss={() => setError(null)} />

      {/* KPI Metric Cards Grid */}
      <div className="stats-grid">
        {/* Metric 1: Open Tasks */}
        <div
          className="stat-card cursor-pointer"
          onClick={() => onNavigate('tasks')}
          role="button"
          tabIndex={0}
        >
          <div className="stat-card-icon bg-blue-subtle">📋</div>
          <div className="stat-card-body">
            <span className="stat-label">Active Service Tasks</span>
            <span className="stat-value">{openTasks.length}</span>
            <div className="stat-meta">
              <span className="badge badge-warning">{pendingTasksCount} Pending</span>
              <span className="badge badge-primary">{inProgressTasksCount} In Progress</span>
              <span className="badge badge-success">{completedTasksCount} Done</span>
            </div>
          </div>
        </div>

        {/* Metric 2: Low-Stock Inventory */}
        <div
          className={`stat-card cursor-pointer ${lowStockItems.length > 0 ? 'border-danger' : ''}`}
          onClick={() => onNavigate('inventory')}
          role="button"
          tabIndex={0}
        >
          <div className="stat-card-icon bg-red-subtle">📦</div>
          <div className="stat-card-body">
            <span className="stat-label">Low Stock Alerts</span>
            <span className={`stat-value ${lowStockItems.length > 0 ? 'text-danger' : ''}`}>
              {lowStockItems.length}
            </span>
            <div className="stat-meta">
              {lowStockItems.length === 0 ? (
                <span className="text-success small-text">✓ Stock levels optimal</span>
              ) : (
                <span className="text-danger small-text font-bold">⚠️ Reorder needed immediately</span>
              )}
            </div>
          </div>
        </div>

        {/* Metric 3: Unpaid Invoices */}
        <div
          className="stat-card cursor-pointer"
          onClick={() => onNavigate('invoices')}
          role="button"
          tabIndex={0}
        >
          <div className="stat-card-icon bg-amber-subtle">💳</div>
          <div className="stat-card-body">
            <span className="stat-label">Unpaid Invoices</span>
            <span className="stat-value">{unpaidInvoices.length}</span>
            <div className="stat-meta">
              <span className="font-bold text-dark">
                ${unpaidTotal.toFixed(2)}
              </span>{' '}
              <span className="text-muted small-text">outstanding</span>
            </div>
          </div>
        </div>

        {/* Metric 4: Customers & Vehicles */}
        <div
          className="stat-card cursor-pointer"
          onClick={() => onNavigate('customers')}
          role="button"
          tabIndex={0}
        >
          <div className="stat-card-icon bg-green-subtle">👤</div>
          <div className="stat-card-body">
            <span className="stat-label">Garage Customers</span>
            <span className="stat-value">{customers.length}</span>
            <div className="stat-meta">
              <span className="text-muted small-text">
                {totalVehiclesCount} registered vehicles
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Main Dashboard Layout: Two Columns */}
      <div className="dashboard-grid-sections">
        {/* Left Column: Active Tasks & Urgent Inventory */}
        <div className="dashboard-column">
          {/* Section: Low Stock Warning Box (if any) */}
          {lowStockItems.length > 0 && (
            <div className="dashboard-panel alert-box-panel">
              <div className="panel-header alert-panel-header">
                <h3>⚠️ Low Stock Warning ({lowStockItems.length})</h3>
                <button
                  type="button"
                  className="btn btn-sm btn-danger"
                  onClick={() => onNavigate('inventory')}
                >
                  Manage Stock &rarr;
                </button>
              </div>
              <div className="table-wrapper">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Part Code</th>
                      <th>Part Name</th>
                      <th>In Stock</th>
                      <th>Threshold</th>
                      <th>Unit Price</th>
                    </tr>
                  </thead>
                  <tbody>
                    {lowStockItems.slice(0, 5).map((item) => (
                      <tr key={item.partCode} className="highlight-danger">
                        <td className="font-mono">{item.partCode}</td>
                        <td><strong>{item.partName}</strong></td>
                        <td className="text-danger font-bold">{item.stockQuantity}</td>
                        <td>{item.reorderThreshold}</td>
                        <td>${item.unitPrice.toFixed(2)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Section: Urgent / In-Progress Tasks */}
          <div className="dashboard-panel">
            <div className="panel-header">
              <h3>Active Service Tasks</h3>
              <button
                type="button"
                className="btn btn-sm btn-secondary"
                onClick={() => onNavigate('tasks')}
              >
                Task Board &rarr;
              </button>
            </div>
            {openTasks.length === 0 ? (
              <p className="empty-subtext">No open tasks right now. All tasks are completed!</p>
            ) : (
              <div className="table-wrapper">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Description</th>
                      <th>Status</th>
                      <th>Mechanic</th>
                      <th>Cost</th>
                    </tr>
                  </thead>
                  <tbody>
                    {openTasks.slice(0, 6).map((task) => (
                      <tr key={task.taskId}>
                        <td>
                          <strong>{task.description}</strong>
                          <div className="small-text text-muted font-mono">
                            Inv: #{task.invoiceId.substring(0, 8)}...
                          </div>
                        </td>
                        <td>
                          <span
                            className={`badge ${
                              task.status === 'IN_PROGRESS' ? 'badge-primary' : 'badge-warning'
                            }`}
                          >
                            {task.status}
                          </span>
                        </td>
                        <td>{task.assignedMechanicName || 'Unassigned'}</td>
                        <td className="font-bold">${task.totalTaskCost.toFixed(2)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Unpaid Invoices & Quick Actions */}
        <div className="dashboard-column">
          {/* Section: Unsettled Invoices */}
          <div className="dashboard-panel">
            <div className="panel-header">
              <h3>Unsettled Invoices</h3>
              <button
                type="button"
                className="btn btn-sm btn-secondary"
                onClick={() => onNavigate('invoices')}
              >
                All Invoices &rarr;
              </button>
            </div>
            {unpaidInvoices.length === 0 ? (
              <p className="empty-subtext">No outstanding invoices. All bills are settled!</p>
            ) : (
              <div className="table-wrapper">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Invoice</th>
                      <th>Customer</th>
                      <th>Plate</th>
                      <th>Status</th>
                      <th>Amount</th>
                    </tr>
                  </thead>
                  <tbody>
                    {unpaidInvoices.slice(0, 6).map((inv) => (
                      <tr
                        key={inv.invoiceId}
                        className="cursor-pointer hover-row"
                        onClick={() => onNavigate('invoices', inv.invoiceId)}
                      >
                        <td className="font-mono">#{inv.invoiceId.substring(0, 8)}</td>
                        <td>{inv.customerName}</td>
                        <td className="font-mono">{inv.licensePlate}</td>
                        <td>
                          <span
                            className={`badge ${
                              inv.status === 'OPEN' ? 'badge-warning' : 'badge-primary'
                            }`}
                          >
                            {inv.status}
                          </span>
                        </td>
                        <td className="price-tag font-bold">${inv.totalAmount.toFixed(2)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Section: OOP Principles Highlight (Great for Viva Demo!) */}
          <div className="dashboard-panel oop-highlights-panel">
            <div className="panel-header">
              <h3>🎓 AutoCare OOP Engine Highlights</h3>
            </div>
            <ul className="oop-list">
              <li>
                <strong>Dynamic Polymorphism:</strong> Base service fees calculated dynamically via{' '}
                <code>Vehicle.getBaseServiceFee()</code> (Car $50, Van $85, Motorcycle $30) and salary calculated via{' '}
                <code>Employee.calculateSalary()</code>.
              </li>
              <li>
                <strong>Static Polymorphism (Overloading):</strong> Dual discount strategies implemented via{' '}
                <code>Invoice.applyDiscount(double)</code> and <code>Invoice.applyDiscount(String, double)</code>.
              </li>
              <li>
                <strong>Encapsulation & Invariants:</strong> Part stock cannot drop below zero; service task status progresses strictly PENDING &rarr; IN_PROGRESS &rarr; COMPLETED; Invoice totals are non-settable derived state.
              </li>
              <li>
                <strong>Abstraction & Interfaces:</strong> Pure interface <code>Billable</code> implemented by <code>Invoice</code>; abstract classes <code>Person</code>, <code>Employee</code>, and <code>Vehicle</code>.
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};
