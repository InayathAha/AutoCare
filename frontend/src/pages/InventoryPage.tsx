import React, { useEffect, useState } from 'react';
import { inventoryApi, ApiErrorResponse } from '../api';
import type { IInventoryItem, IInventoryItemRequest, IApiError } from '../types';
import { Modal } from '../components/Modal';
import { ErrorMessage } from '../components/ErrorMessage';

export const InventoryPage: React.FC = () => {
  const [items, setItems] = useState<IInventoryItem[]>([]);
  const [showLowStockOnly, setShowLowStockOnly] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<IApiError | null>(null);

  // Modals state
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [isRestockOpen, setIsRestockOpen] = useState(false);
  const [isReportOpen, setIsReportOpen] = useState(false);
  const [selectedItem, setSelectedItem] = useState<IInventoryItem | null>(null);

  // Forms
  const [itemForm, setItemForm] = useState<IInventoryItemRequest>({
    partCode: '',
    partName: '',
    unitPrice: 10.0,
    stockQuantity: 20,
    reorderThreshold: 5,
  });

  const [restockQty, setRestockQty] = useState<number>(10);
  const [managerIdInput, setManagerIdInput] = useState<string>('');
  const [generatedReport, setGeneratedReport] = useState<string | null>(null);

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

  const loadItems = async (lowStockOnly = showLowStockOnly) => {
    try {
      setLoading(true);
      setError(null);
      const data = lowStockOnly
        ? await inventoryApi.listLowStock()
        : await inventoryApi.list();
      setItems(data);
    } catch (err) {
      handleApiError(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadItems(showLowStockOnly);
  }, [showLowStockOnly]);

  const handleAddItem = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setError(null);
      await inventoryApi.create(itemForm);
      setIsAddOpen(false);
      setItemForm({
        partCode: '',
        partName: '',
        unitPrice: 10.0,
        stockQuantity: 20,
        reorderThreshold: 5,
      });
      await loadItems();
    } catch (err) {
      handleApiError(err);
    }
  };

  const handleRestock = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedItem) return;
    try {
      setError(null);
      await inventoryApi.restock(selectedItem.partCode, restockQty);
      setIsRestockOpen(false);
      setSelectedItem(null);
      await loadItems();
    } catch (err) {
      handleApiError(err);
    }
  };

  const handleGenerateReport = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setError(null);
      const res = await inventoryApi.getReport(managerIdInput.trim());
      setGeneratedReport(res.report);
    } catch (err) {
      handleApiError(err);
    }
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h2>Spare Parts Inventory</h2>
          <p className="page-sub">Track stock quantities, reorder thresholds, low stock alerts, and restocking</p>
        </div>
        <div className="header-actions">
          <button
            type="button"
            className="btn btn-outline"
            onClick={() => {
              setGeneratedReport(null);
              setIsReportOpen(true);
            }}
          >
            📋 Manager Report
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => setIsAddOpen(true)}
          >
            + Add Spare Part
          </button>
        </div>
      </div>

      <ErrorMessage error={error} onDismiss={() => setError(null)} />

      <div className="inventory-filter-bar">
        <label className="toggle-label">
          <input
            type="checkbox"
            checked={showLowStockOnly}
            onChange={(e) => setShowLowStockOnly(e.target.checked)}
          />
          <span>Show Low Stock Items Only</span>
        </label>
        <span className="text-muted">
          Total items: {items.length} {showLowStockOnly ? '(filtered)' : ''}
        </span>
      </div>

      <div className="panel">
        {loading && <p className="loading-state">Loading inventory items...</p>}
        {!loading && items.length === 0 && (
          <p className="empty-state">No inventory items found.</p>
        )}
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>Part Code</th>
                <th>Part Name</th>
                <th>Unit Price</th>
                <th>Current Stock</th>
                <th>Reorder Threshold</th>
                <th>Status</th>
                <th>Last Restocked</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.partCode} className={item.lowStock ? 'row-warning' : ''}>
                  <td><code>{item.partCode}</code></td>
                  <td><strong>{item.partName}</strong></td>
                  <td>${item.unitPrice.toFixed(2)}</td>
                  <td>
                    <span className={`stock-indicator ${item.lowStock ? 'stock-low' : 'stock-ok'}`}>
                      {item.stockQuantity} units
                    </span>
                  </td>
                  <td>{item.reorderThreshold} units</td>
                  <td>
                    {item.lowStock ? (
                      <span className="badge badge-danger">⚠️ LOW STOCK</span>
                    ) : (
                      <span className="badge badge-success">IN STOCK</span>
                    )}
                  </td>
                  <td>
                    {item.lastRestockedAt
                      ? new Date(item.lastRestockedAt).toLocaleDateString()
                      : <span className="text-muted">Never</span>}
                  </td>
                  <td>
                    <button
                      type="button"
                      className="btn btn-sm btn-outline"
                      onClick={() => {
                        setSelectedItem(item);
                        setRestockQty(10);
                        setIsRestockOpen(true);
                      }}
                    >
                      Restock
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Add Item Modal */}
      <Modal
        title="Add Spare Part"
        isOpen={isAddOpen}
        onClose={() => setIsAddOpen(false)}
      >
        <form onSubmit={handleAddItem} className="form-grid">
          <div className="form-group">
            <label>Part Code *</label>
            <input
              type="text"
              className="input"
              placeholder="e.g. OIL-5W30"
              required
              value={itemForm.partCode}
              onChange={(e) => setItemForm({ ...itemForm, partCode: e.target.value.toUpperCase() })}
            />
          </div>
          <div className="form-group">
            <label>Part Name *</label>
            <input
              type="text"
              className="input"
              placeholder="e.g. Synthetic Motor Oil 5W30"
              required
              value={itemForm.partName}
              onChange={(e) => setItemForm({ ...itemForm, partName: e.target.value })}
            />
          </div>
          <div className="form-group">
            <label>Unit Price ($) *</label>
            <input
              type="number"
              step="0.01"
              min="0"
              className="input"
              required
              value={itemForm.unitPrice}
              onChange={(e) => setItemForm({ ...itemForm, unitPrice: parseFloat(e.target.value) || 0 })}
            />
          </div>
          <div className="form-group">
            <label>Initial Stock Quantity *</label>
            <input
              type="number"
              min="0"
              className="input"
              required
              value={itemForm.stockQuantity}
              onChange={(e) => setItemForm({ ...itemForm, stockQuantity: parseInt(e.target.value, 10) || 0 })}
            />
          </div>
          <div className="form-group">
            <label>Reorder Threshold *</label>
            <input
              type="number"
              min="0"
              className="input"
              required
              value={itemForm.reorderThreshold}
              onChange={(e) => setItemForm({ ...itemForm, reorderThreshold: parseInt(e.target.value, 10) || 0 })}
            />
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-ghost" onClick={() => setIsAddOpen(false)}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary">
              Save Part
            </button>
          </div>
        </form>
      </Modal>

      {/* Restock Modal */}
      <Modal
        title={`Restock Part: ${selectedItem?.partName || ''} (${selectedItem?.partCode || ''})`}
        isOpen={isRestockOpen}
        onClose={() => setIsRestockOpen(false)}
      >
        <form onSubmit={handleRestock} className="form-grid">
          <p className="text-muted">Current Stock: {selectedItem?.stockQuantity || 0} units</p>
          <div className="form-group">
            <label>Units to Add *</label>
            <input
              type="number"
              min="1"
              className="input"
              required
              value={restockQty}
              onChange={(e) => setRestockQty(parseInt(e.target.value, 10) || 1)}
            />
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-ghost" onClick={() => setIsRestockOpen(false)}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary">
              Confirm Restock
            </button>
          </div>
        </form>
      </Modal>

      {/* Manager Report Modal */}
      <Modal
        title="Manager Low Stock Inventory Report"
        isOpen={isReportOpen}
        onClose={() => setIsReportOpen(false)}
      >
        <form onSubmit={handleGenerateReport} className="form-grid">
          <p className="text-muted">Enter a Manager Employee ID to authorize report generation.</p>
          <div className="form-group">
            <label>Manager Employee ID *</label>
            <input
              type="text"
              className="input"
              placeholder="e.g. 242004 or Manager UUID"
              required
              value={managerIdInput}
              onChange={(e) => setManagerIdInput(e.target.value)}
            />
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-ghost" onClick={() => setIsReportOpen(false)}>
              Close
            </button>
            <button type="submit" className="btn btn-primary">
              Generate Report
            </button>
          </div>
        </form>

        {generatedReport && (
          <div className="report-output">
            <h4>Generated Report:</h4>
            <pre className="report-pre">{generatedReport}</pre>
          </div>
        )}
      </Modal>
    </div>
  );
};
