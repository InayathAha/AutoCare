import React, { useEffect, useState } from 'react';
import { tasksApi, employeesApi, inventoryApi, ApiErrorResponse } from '../api';
import type {
  ITask,
  ITaskRequest,
  ITaskPartRequest,
  IEmployee,
  IInventoryItem,
  IApiError,
  TaskStatus,
} from '../types';
import { Modal } from '../components/Modal';
import { ErrorMessage } from '../components/ErrorMessage';

interface TaskBoardPageProps {
  initialInvoiceId?: string | null;
}

export const TaskBoardPage: React.FC<TaskBoardPageProps> = ({ initialInvoiceId }) => {
  const [tasks, setTasks] = useState<ITask[]>([]);
  const [mechanics, setMechanics] = useState<IEmployee[]>([]);
  const [inventoryItems, setInventoryItems] = useState<IInventoryItem[]>([]);
  const [mechanicFilter, setMechanicFilter] = useState<string>('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<IApiError | null>(null);

  // Modals state
  const [isCreateTaskOpen, setIsCreateTaskOpen] = useState(false);
  const [isAddPartOpen, setIsAddPartOpen] = useState(false);
  const [isRecordLaborOpen, setIsRecordLaborOpen] = useState(false);
  const [isDiagnosticsOpen, setIsDiagnosticsOpen] = useState(false);
  const [activeTask, setActiveTask] = useState<ITask | null>(null);

  // Forms
  const [taskForm, setTaskForm] = useState<ITaskRequest & { invoiceId: string }>({
    invoiceId: initialInvoiceId || '',
    description: '',
    assignedMechanicId: '',
  });

  useEffect(() => {
    if (initialInvoiceId) {
      setTaskForm((prev) => ({ ...prev, invoiceId: initialInvoiceId }));
      setIsCreateTaskOpen(true);
    }
  }, [initialInvoiceId]);

  const [partForm, setPartForm] = useState<ITaskPartRequest>({
    partCode: '',
    quantity: 1,
  });

  const [laborHours, setLaborHours] = useState<number>(1.5);
  const [diagDescription, setDiagDescription] = useState<string>('');

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

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [allTasks, allMechanics, allItems] = await Promise.all([
        tasksApi.list(undefined, mechanicFilter || undefined),
        employeesApi.list('MECHANIC'),
        inventoryApi.list(),
      ]);
      setTasks(allTasks);
      setMechanics(allMechanics);
      setInventoryItems(allItems);

      if (allMechanics.length > 0 && !taskForm.assignedMechanicId) {
        setTaskForm((prev) => ({ ...prev, assignedMechanicId: allMechanics[0].id }));
      }
      if (allItems.length > 0 && !partForm.partCode) {
        setPartForm((prev) => ({ ...prev, partCode: allItems[0].partCode }));
      }
    } catch (err) {
      handleApiError(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [mechanicFilter]);

  const handleCreateTask = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setError(null);
      await tasksApi.create(taskForm.invoiceId.trim(), {
        description: taskForm.description,
        assignedMechanicId: taskForm.assignedMechanicId,
      });
      setIsCreateTaskOpen(false);
      setTaskForm({
        invoiceId: '',
        description: '',
        assignedMechanicId: mechanics[0]?.id || '',
      });
      await loadData();
    } catch (err) {
      handleApiError(err);
    }
  };

  const handleStartTask = async (taskId: string) => {
    try {
      setError(null);
      await tasksApi.start(taskId);
      await loadData();
    } catch (err) {
      handleApiError(err);
    }
  };

  const handleCompleteTask = async (taskId: string) => {
    try {
      setError(null);
      await tasksApi.complete(taskId);
      await loadData();
    } catch (err) {
      handleApiError(err);
    }
  };

  const handleAddPart = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeTask) return;
    try {
      setError(null);
      await tasksApi.addPart(activeTask.taskId, partForm);
      setIsAddPartOpen(false);
      setActiveTask(null);
      await loadData();
    } catch (err) {
      handleApiError(err);
    }
  };

  const handleRecordLabor = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeTask) return;
    try {
      setError(null);
      await tasksApi.recordLabor(activeTask.taskId, laborHours);
      setIsRecordLaborOpen(false);
      setActiveTask(null);
      await loadData();
    } catch (err) {
      handleApiError(err);
    }
  };

  const handleUpdateDiagnostics = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeTask) return;
    try {
      setError(null);
      await tasksApi.updateDiagnostics(activeTask.taskId, diagDescription);
      setIsDiagnosticsOpen(false);
      setActiveTask(null);
      await loadData();
    } catch (err) {
      handleApiError(err);
    }
  };

  const tasksByStatus = (status: TaskStatus) => tasks.filter((t) => t.status === status);

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h2>Task Board &amp; Job Cards</h2>
          <p className="page-sub">
            Three-column workflow: PENDING → IN_PROGRESS → COMPLETED. State pattern and mechanics validation.
          </p>
        </div>
        <button
          type="button"
          className="btn btn-primary"
          onClick={() => setIsCreateTaskOpen(true)}
        >
          + Create Task
        </button>
      </div>

      <ErrorMessage error={error} onDismiss={() => setError(null)} />

      <div className="filter-bar">
        <div className="filter-group">
          <label>Filter by Mechanic:</label>
          <select
            className="input input-sm"
            value={mechanicFilter}
            onChange={(e) => setMechanicFilter(e.target.value)}
          >
            <option value="">All Mechanics</option>
            {mechanics.map((m) => (
              <option key={m.id} value={m.id}>
                {m.name} ({m.specialization || 'General'})
              </option>
            ))}
          </select>
        </div>
        <span className="text-muted">Total tasks: {tasks.length}</span>
      </div>

      {loading && <p className="loading-state">Loading tasks...</p>}

      <div className="kanban-grid">
        {/* Column 1: PENDING */}
        <div className="kanban-column column-pending">
          <div className="column-header">
            <h4>⏳ PENDING</h4>
            <span className="column-count">{tasksByStatus('PENDING').length}</span>
          </div>
          <div className="column-cards">
            {tasksByStatus('PENDING').map((task) => (
              <div key={task.taskId} className="task-card">
                <div className="task-card-header">
                  <span className="badge badge-pending">PENDING</span>
                  <span className="text-muted text-xs">Inv: {task.invoiceId?.substring(0, 8)}...</span>
                </div>
                <h5>{task.description}</h5>
                <p className="task-assignee">👤 {task.assignedMechanicName || 'Unassigned'}</p>
                <div className="task-costs">
                  <div>Labor: ${task.laborCharge.toFixed(2)}</div>
                  <div>Parts: ${task.partsCost.toFixed(2)}</div>
                  <div className="total-highlight">Total: ${task.totalTaskCost.toFixed(2)}</div>
                </div>
                <div className="task-card-footer">
                  <button
                    type="button"
                    className="btn btn-sm btn-primary w-full"
                    onClick={() => handleStartTask(task.taskId)}
                  >
                    ▶ Start Task
                  </button>
                </div>
              </div>
            ))}
            {tasksByStatus('PENDING').length === 0 && (
              <p className="empty-column">No pending tasks</p>
            )}
          </div>
        </div>

        {/* Column 2: IN_PROGRESS */}
        <div className="kanban-column column-in-progress">
          <div className="column-header">
            <h4>🔧 IN PROGRESS</h4>
            <span className="column-count">{tasksByStatus('IN_PROGRESS').length}</span>
          </div>
          <div className="column-cards">
            {tasksByStatus('IN_PROGRESS').map((task) => (
              <div key={task.taskId} className="task-card">
                <div className="task-card-header">
                  <span className="badge badge-progress">IN PROGRESS</span>
                  <span className="text-muted text-xs">Inv: {task.invoiceId?.substring(0, 8)}...</span>
                </div>
                <h5>{task.description}</h5>
                <p className="task-assignee">👤 {task.assignedMechanicName}</p>

                {task.usedParts && task.usedParts.length > 0 && (
                  <div className="used-parts-box">
                    <strong>Parts Used ({task.usedParts.length}):</strong>
                    <ul>
                      {task.usedParts.map((p, idx) => (
                        <li key={idx}>
                          {p.partName} x {p.quantity} (${p.subtotal.toFixed(2)})
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                <div className="task-costs">
                  <div>Labor ({task.billableHours}h): ${task.laborCharge.toFixed(2)}</div>
                  <div>Parts: ${task.partsCost.toFixed(2)}</div>
                  <div className="total-highlight">Total: ${task.totalTaskCost.toFixed(2)}</div>
                </div>

                <div className="task-action-grid">
                  <button
                    type="button"
                    className="btn btn-xs btn-outline"
                    onClick={() => {
                      setActiveTask(task);
                      setIsAddPartOpen(true);
                    }}
                  >
                    + Add Part
                  </button>
                  <button
                    type="button"
                    className="btn btn-xs btn-outline"
                    onClick={() => {
                      setActiveTask(task);
                      setLaborHours(task.billableHours || 1.5);
                      setIsRecordLaborOpen(true);
                    }}
                  >
                    ⏱ Labor
                  </button>
                  <button
                    type="button"
                    className="btn btn-xs btn-outline"
                    onClick={() => {
                      setActiveTask(task);
                      setDiagDescription(task.description);
                      setIsDiagnosticsOpen(true);
                    }}
                  >
                    ✏️ Diag
                  </button>
                </div>

                <div className="task-card-footer">
                  <button
                    type="button"
                    className="btn btn-sm btn-success w-full"
                    onClick={() => handleCompleteTask(task.taskId)}
                  >
                    ✓ Complete Task
                  </button>
                </div>
              </div>
            ))}
            {tasksByStatus('IN_PROGRESS').length === 0 && (
              <p className="empty-column">No active tasks in progress</p>
            )}
          </div>
        </div>

        {/* Column 3: COMPLETED */}
        <div className="kanban-column column-completed">
          <div className="column-header">
            <h4>✅ COMPLETED</h4>
            <span className="column-count">{tasksByStatus('COMPLETED').length}</span>
          </div>
          <div className="column-cards">
            {tasksByStatus('COMPLETED').map((task) => (
              <div key={task.taskId} className="task-card card-locked">
                <div className="task-card-header">
                  <span className="badge badge-completed">COMPLETED</span>
                  <span className="text-muted text-xs">🔒 Locked</span>
                </div>
                <h5>{task.description}</h5>
                <p className="task-assignee">👤 {task.assignedMechanicName}</p>

                {task.usedParts && task.usedParts.length > 0 && (
                  <div className="used-parts-box">
                    <strong>Parts Used ({task.usedParts.length}):</strong>
                    <ul>
                      {task.usedParts.map((p, idx) => (
                        <li key={idx}>
                          {p.partName} x {p.quantity} (${p.subtotal.toFixed(2)})
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                <div className="task-costs">
                  <div>Labor ({task.billableHours}h): ${task.laborCharge.toFixed(2)}</div>
                  <div>Parts: ${task.partsCost.toFixed(2)}</div>
                  <div className="total-highlight">Final: ${task.totalTaskCost.toFixed(2)}</div>
                </div>
              </div>
            ))}
            {tasksByStatus('COMPLETED').length === 0 && (
              <p className="empty-column">No completed tasks yet</p>
            )}
          </div>
        </div>
      </div>

      {/* Create Task Modal */}
      <Modal
        title="Create Job Card Task"
        isOpen={isCreateTaskOpen}
        onClose={() => setIsCreateTaskOpen(false)}
      >
        <form onSubmit={handleCreateTask} className="form-grid">
          <div className="form-group">
            <label>Invoice ID *</label>
            <input
              type="text"
              className="input"
              placeholder="Paste Invoice UUID (e.g. from Invoices tab)"
              required
              value={taskForm.invoiceId}
              onChange={(e) => setTaskForm({ ...taskForm, invoiceId: e.target.value })}
            />
          </div>
          <div className="form-group">
            <label>Task Description &amp; Diagnostic Details *</label>
            <textarea
              className="input textarea"
              rows={3}
              placeholder="e.g. Brake pad inspection, disc resurfacing, and fluid flush"
              required
              value={taskForm.description}
              onChange={(e) => setTaskForm({ ...taskForm, description: e.target.value })}
            />
          </div>
          <div className="form-group">
            <label>Assigned Mechanic *</label>
            <select
              className="input"
              required
              value={taskForm.assignedMechanicId}
              onChange={(e) => setTaskForm({ ...taskForm, assignedMechanicId: e.target.value })}
            >
              {mechanics.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.name} ({m.specialization || 'General'})
                </option>
              ))}
            </select>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-ghost" onClick={() => setIsCreateTaskOpen(false)}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary">
              Create Task
            </button>
          </div>
        </form>
      </Modal>

      {/* Add Part Modal */}
      <Modal
        title={`Add Part to Task: ${activeTask?.description || ''}`}
        isOpen={isAddPartOpen}
        onClose={() => setIsAddPartOpen(false)}
      >
        <form onSubmit={handleAddPart} className="form-grid">
          <div className="form-group">
            <label>Select Inventory Part *</label>
            <select
              className="input"
              value={partForm.partCode}
              onChange={(e) => setPartForm({ ...partForm, partCode: e.target.value })}
            >
              {inventoryItems.map((item) => (
                <option key={item.partCode} value={item.partCode}>
                  {item.partName} (${item.unitPrice.toFixed(2)}) - Stock: {item.stockQuantity}
                </option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label>Quantity *</label>
            <input
              type="number"
              min="1"
              className="input"
              required
              value={partForm.quantity}
              onChange={(e) => setPartForm({ ...partForm, quantity: parseInt(e.target.value, 10) || 1 })}
            />
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-ghost" onClick={() => setIsAddPartOpen(false)}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary">
              Attach Part
            </button>
          </div>
        </form>
      </Modal>

      {/* Record Labor Modal */}
      <Modal
        title={`Record Billable Labor: ${activeTask?.description || ''}`}
        isOpen={isRecordLaborOpen}
        onClose={() => setIsRecordLaborOpen(false)}
      >
        <form onSubmit={handleRecordLabor} className="form-grid">
          <p className="text-muted">Standard Labor Rate: $20.00/hour</p>
          <div className="form-group">
            <label>Billable Hours *</label>
            <input
              type="number"
              step="0.25"
              min="0"
              className="input"
              required
              value={laborHours}
              onChange={(e) => setLaborHours(parseFloat(e.target.value) || 0)}
            />
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-ghost" onClick={() => setIsRecordLaborOpen(false)}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary">
              Save Labor Hours
            </button>
          </div>
        </form>
      </Modal>

      {/* Update Diagnostics Modal */}
      <Modal
        title="Update Diagnostic Findings"
        isOpen={isDiagnosticsOpen}
        onClose={() => setIsDiagnosticsOpen(false)}
      >
        <form onSubmit={handleUpdateDiagnostics} className="form-grid">
          <div className="form-group">
            <label>Diagnostic Findings *</label>
            <textarea
              className="input textarea"
              rows={4}
              required
              value={diagDescription}
              onChange={(e) => setDiagDescription(e.target.value)}
            />
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-ghost" onClick={() => setIsDiagnosticsOpen(false)}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary">
              Update Diagnostics
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
