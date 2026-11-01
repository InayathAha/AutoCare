import React, { useEffect, useState } from 'react';
import { invoicesApi, customersApi, employeesApi, ApiErrorResponse } from '../api';
import type {
  IInvoice,
  IInvoiceSummary,
  IOpenInvoiceRequest,
  IDiscountPercentageRequest,
  IDiscountCouponRequest,
  ITaxRateRequest,
  ICustomer,
  IVehicle,
  IEmployee,
  IApiError,
  InvoiceStatus,
} from '../types';
import { Modal } from '../components/Modal';
import { ErrorMessage } from '../components/ErrorMessage';

interface InvoicesPageProps {
  initialInvoiceId?: string | null;
  onNavigateToTasks?: (invoiceId: string) => void;
}

export const InvoicesPage: React.FC<InvoicesPageProps> = ({ initialInvoiceId, onNavigateToTasks }) => {
  const [invoices, setInvoices] = useState<IInvoiceSummary[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [selectedInvoiceId, setSelectedInvoiceId] = useState<string | null>(initialInvoiceId || null);
  const [selectedInvoice, setSelectedInvoice] = useState<IInvoice | null>(null);
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState<IApiError | null>(null);

  // Modals
  const [isOpenInvoiceModal, setIsOpenInvoiceModal] = useState(false);
  const [isPercentDiscountModal, setIsPercentDiscountModal] = useState(false);
  const [isCouponDiscountModal, setIsCouponDiscountModal] = useState(false);
  const [isTaxRateModal, setIsTaxRateModal] = useState(false);
  const [isReceiptModal, setIsReceiptModal] = useState(false);
  const [receiptContent, setReceiptContent] = useState<string>('');

  // Dropdown data for modals
  const [customers, setCustomers] = useState<ICustomer[]>([]);
  const [selectedCustomerVehicles, setSelectedCustomerVehicles] = useState<IVehicle[]>([]);
  const [managers, setManagers] = useState<IEmployee[]>([]);

  // Open Invoice Form
  const [openForm, setOpenForm] = useState<IOpenInvoiceRequest>({
    customerId: '',
    licensePlate: '',
  });

  // Discount forms
  const [percentageForm, setPercentageForm] = useState<IDiscountPercentageRequest>({
    percentage: 10,
    managerId: '',
  });

  const [couponForm, setCouponForm] = useState<IDiscountCouponRequest>({
    couponCode: '',
    fixedReduction: 15.0,
  });

  const [taxRateForm, setTaxRateForm] = useState<ITaxRateRequest>({
    taxRate: 0.10,
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

  const loadInvoices = async (status?: string) => {
    try {
      setLoading(true);
      setError(null);
      const data = await invoicesApi.list(status || undefined);
      setInvoices(data);
    } catch (err) {
      handleApiError(err);
    } finally {
      setLoading(false);
    }
  };

  const loadInvoiceDetail = async (id: string) => {
    try {
      setDetailLoading(true);
      setError(null);
      const data = await invoicesApi.get(id);
      setSelectedInvoice(data);
    } catch (err) {
      handleApiError(err);
    } finally {
      setDetailLoading(false);
    }
  };

  useEffect(() => {
    loadInvoices(statusFilter);
  }, [statusFilter]);

  useEffect(() => {
    if (selectedInvoiceId) {
      loadInvoiceDetail(selectedInvoiceId);
    } else {
      setSelectedInvoice(null);
    }
  }, [selectedInvoiceId]);

  // When customer changes in Open Invoice modal, load their vehicles
  useEffect(() => {
    if (openForm.customerId) {
      customersApi
        .getVehicles(openForm.customerId)
        .then((vehicles) => {
          setSelectedCustomerVehicles(vehicles);
          if (vehicles.length > 0) {
            setOpenForm((prev) => ({ ...prev, licensePlate: vehicles[0].licensePlate }));
          } else {
            setOpenForm((prev) => ({ ...prev, licensePlate: '' }));
          }
        })
        .catch(handleApiError);
    } else {
      setSelectedCustomerVehicles([]);
      setOpenForm((prev) => ({ ...prev, licensePlate: '' }));
    }
  }, [openForm.customerId]);

  const handleOpenInvoiceModalLaunch = async () => {
    try {
      setError(null);
      const [custList, managerList] = await Promise.all([
        customersApi.list(),
        employeesApi.list('MANAGER'),
      ]);
      setCustomers(custList);
      setManagers(managerList);
      if (custList.length > 0) {
        setOpenForm({
          customerId: custList[0].id,
          licensePlate: '',
        });
      }
      setIsOpenInvoiceModal(true);
    } catch (err) {
      handleApiError(err);
    }
  };

  const handleOpenInvoiceSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!openForm.customerId || !openForm.licensePlate) return;
    try {
      setError(null);
      const created = await invoicesApi.open(openForm);
      setIsOpenInvoiceModal(false);
      await loadInvoices(statusFilter);
      setSelectedInvoiceId(created.invoiceId);
    } catch (err) {
      handleApiError(err);
    }
  };

  const handleApplyPercentageDiscount = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedInvoice) return;
    try {
      setError(null);
      const payload: IDiscountPercentageRequest = {
        percentage: Number(percentageForm.percentage),
        managerId: percentageForm.managerId?.trim() ? percentageForm.managerId : null,
      };
      const updated = await invoicesApi.applyPercentageDiscount(selectedInvoice.invoiceId, payload);
      setSelectedInvoice(updated);
      setIsPercentDiscountModal(false);
      await loadInvoices(statusFilter);
    } catch (err) {
      handleApiError(err);
    }
  };

  const handleApplyCouponDiscount = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedInvoice) return;
    try {
      setError(null);
      const updated = await invoicesApi.applyCouponDiscount(selectedInvoice.invoiceId, {
        couponCode: couponForm.couponCode.trim().toUpperCase(),
        fixedReduction: Number(couponForm.fixedReduction),
      });
      setSelectedInvoice(updated);
      setIsCouponDiscountModal(false);
      await loadInvoices(statusFilter);
    } catch (err) {
      handleApiError(err);
    }
  };

  const handleSetTaxRate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedInvoice) return;
    try {
      setError(null);
      const updated = await invoicesApi.setTaxRate(selectedInvoice.invoiceId, Number(taxRateForm.taxRate));
      setSelectedInvoice(updated);
      setIsTaxRateModal(false);
      await loadInvoices(statusFilter);
    } catch (err) {
      handleApiError(err);
    }
  };

  const handleIssueInvoice = async () => {
    if (!selectedInvoice) return;
    const confirmIssue = window.confirm(
      'Issue this invoice? All tasks must be COMPLETED. Once issued, task additions and discounts are locked.'
    );
    if (!confirmIssue) return;

    try {
      setError(null);
      const updated = await invoicesApi.issue(selectedInvoice.invoiceId);
      setSelectedInvoice(updated);
      await loadInvoices(statusFilter);
    } catch (err) {
      handleApiError(err);
    }
  };

  const handleSettleInvoice = async () => {
    if (!selectedInvoice) return;
    const confirmSettle = window.confirm(
      'Settle payment for this invoice? This marks the invoice PAID and awards loyalty points to the customer.'
    );
    if (!confirmSettle) return;

    try {
      setError(null);
      const updated = await invoicesApi.settle(selectedInvoice.invoiceId);
      setSelectedInvoice(updated);
      await loadInvoices(statusFilter);
    } catch (err) {
      handleApiError(err);
    }
  };

  const handleViewReceipt = async () => {
    if (!selectedInvoice) return;
    try {
      setError(null);
      const receipt = await invoicesApi.getReceipt(selectedInvoice.invoiceId);
      setReceiptContent(receipt);
      setIsReceiptModal(true);
    } catch (err) {
      handleApiError(err);
    }
  };

  const handlePrintReceipt = () => {
    window.print();
  };

  const getStatusBadgeClass = (status: InvoiceStatus) => {
    switch (status) {
      case 'OPEN':
        return 'badge badge-warning';
      case 'ISSUED':
        return 'badge badge-primary';
      case 'PAID':
        return 'badge badge-success';
      default:
        return 'badge';
    }
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h2>Invoices & Billing</h2>
          <p className="page-subtitle">Itemised billing, polymorphic fees, discounts, and payments</p>
        </div>
        <button
          type="button"
          className="btn btn-primary"
          onClick={handleOpenInvoiceModalLaunch}
        >
          + Open New Invoice
        </button>
      </div>

      <ErrorMessage error={error} onDismiss={() => setError(null)} />

      {/* Status Filter Bar */}
      <div className="filters-bar">
        <div className="filter-group">
          <label htmlFor="status-filter">Filter by Status:</label>
          <select
            id="status-filter"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="form-control"
          >
            <option value="">All Statuses</option>
            <option value="OPEN">Open (In Progress)</option>
            <option value="ISSUED">Issued (Pending Payment)</option>
            <option value="PAID">Paid (Settled)</option>
          </select>
        </div>
        <span className="results-count">
          Showing {invoices.length} {invoices.length === 1 ? 'invoice' : 'invoices'}
        </span>
      </div>

      <div className="split-view">
        {/* Left Column: Invoices List */}
        <div className="split-list-panel">
          {loading ? (
            <div className="loading-state">Loading invoices...</div>
          ) : invoices.length === 0 ? (
            <div className="empty-state">No invoices found for the selected filter.</div>
          ) : (
            <div className="cards-list">
              {invoices.map((inv) => (
                <div
                  key={inv.invoiceId}
                  className={`card item-card ${selectedInvoiceId === inv.invoiceId ? 'selected' : ''}`}
                  onClick={() => setSelectedInvoiceId(inv.invoiceId)}
                  role="button"
                  tabIndex={0}
                >
                  <div className="card-header-row">
                    <span className="card-id font-mono">
                      #{inv.invoiceId.length > 8 ? inv.invoiceId.substring(0, 8) : inv.invoiceId}
                    </span>
                    <span className={getStatusBadgeClass(inv.status)}>{inv.status}</span>
                  </div>
                  <div className="card-body">
                    <div className="item-detail">
                      <strong>Customer:</strong> {inv.customerName}
                    </div>
                    <div className="item-detail">
                      <strong>Vehicle:</strong> <span className="font-mono">{inv.licensePlate}</span>
                    </div>
                    <div className="item-detail card-total-row">
                      <strong>Total:</strong>
                      <span className="price-tag">${inv.totalAmount.toFixed(2)}</span>
                    </div>
                  </div>
                  <div className="card-footer-date">
                    {inv.issuedAt ? new Date(inv.issuedAt).toLocaleDateString() : 'Draft'}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Right Column: Invoice Detail */}
        <div className="split-detail-panel">
          {detailLoading ? (
            <div className="loading-state">Loading invoice details...</div>
          ) : !selectedInvoice ? (
            <div className="empty-state">Select an invoice from the list to view details and billing actions.</div>
          ) : (
            <div className="detail-content">
              {/* Invoice Detail Header */}
              <div className="detail-header-card">
                <div className="detail-title-row">
                  <div>
                    <h3>Invoice #{selectedInvoice.invoiceId}</h3>
                    <div className="date-meta">
                      Created/Issued: {new Date(selectedInvoice.issuedAt).toLocaleString()}
                      {selectedInvoice.settledAt && (
                        <span> | Settled: {new Date(selectedInvoice.settledAt).toLocaleString()}</span>
                      )}
                    </div>
                  </div>
                  <span className={getStatusBadgeClass(selectedInvoice.status)} style={{ fontSize: '1rem', padding: '6px 14px' }}>
                    {selectedInvoice.status}
                  </span>
                </div>

                <div className="detail-meta-grid">
                  <div className="meta-box">
                    <span className="meta-label">Customer</span>
                    <span className="meta-value">{selectedInvoice.customerName}</span>
                  </div>
                  <div className="meta-box">
                    <span className="meta-label">License Plate</span>
                    <span className="meta-value font-mono">{selectedInvoice.licensePlate}</span>
                  </div>
                  <div className="meta-box">
                    <span className="meta-label">Vehicle Type</span>
                    <span className="meta-value badge badge-info">{selectedInvoice.vehicleType}</span>
                  </div>
                  <div className="meta-box">
                    <span className="meta-label">Base Fee (Polymorphic)</span>
                    <span className="meta-value">${selectedInvoice.baseServiceFee.toFixed(2)}</span>
                  </div>
                </div>
              </div>

              {/* Action Toolbar */}
              <div className="invoice-actions-bar">
                {selectedInvoice.status === 'OPEN' && (
                  <>
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      onClick={() => setIsPercentDiscountModal(true)}
                    >
                      % Apply Discount
                    </button>
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      onClick={() => setIsCouponDiscountModal(true)}
                    >
                      🏷️ Apply Coupon
                    </button>
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      onClick={() => {
                        setTaxRateForm({ taxRate: selectedInvoice.taxRate });
                        setIsTaxRateModal(true);
                      }}
                    >
                      🏛️ Tax Rate ({Math.round(selectedInvoice.taxRate * 100)}%)
                    </button>
                    {onNavigateToTasks && (
                      <button
                        type="button"
                        className="btn btn-outline btn-sm"
                        onClick={() => onNavigateToTasks(selectedInvoice.invoiceId)}
                      >
                        + Manage Tasks
                      </button>
                    )}
                    <button
                      type="button"
                      className="btn btn-primary btn-sm ml-auto"
                      onClick={handleIssueInvoice}
                    >
                      ✓ Issue Invoice
                    </button>
                  </>
                )}

                {selectedInvoice.status === 'ISSUED' && (
                  <>
                    <button
                      type="button"
                      className="btn btn-success btn-sm"
                      onClick={handleSettleInvoice}
                    >
                      💳 Settle Payment
                    </button>
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm ml-auto"
                      onClick={handleViewReceipt}
                    >
                      🧾 View / Print Receipt
                    </button>
                  </>
                )}

                {selectedInvoice.status === 'PAID' && (
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm ml-auto"
                    onClick={handleViewReceipt}
                  >
                    🧾 View / Print Receipt
                  </button>
                )}
              </div>

              {/* Tasks and Parts Breakdown */}
              <div className="section-block">
                <h4>Service Tasks ({selectedInvoice.tasks.length})</h4>
                {selectedInvoice.tasks.length === 0 ? (
                  <p className="empty-subtext">
                    No tasks assigned to this invoice yet. Add tasks before issuing.
                  </p>
                ) : (
                  <div className="table-wrapper">
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>Task</th>
                          <th>Mechanic</th>
                          <th>Status</th>
                          <th>Labor Hrs</th>
                          <th>Labor Fee</th>
                          <th>Parts Cost</th>
                          <th>Total</th>
                        </tr>
                      </thead>
                      <tbody>
                        {selectedInvoice.tasks.map((task) => (
                          <React.Fragment key={task.taskId}>
                            <tr>
                              <td>
                                <strong>{task.description}</strong>
                                {task.usedParts.length > 0 && (
                                  <div className="small-text text-muted">
                                    {task.usedParts.length} parts used
                                  </div>
                                )}
                              </td>
                              <td>{task.assignedMechanicName || 'Unassigned'}</td>
                              <td>
                                <span
                                  className={`badge ${
                                    task.status === 'COMPLETED'
                                      ? 'badge-success'
                                      : task.status === 'IN_PROGRESS'
                                      ? 'badge-primary'
                                      : 'badge-warning'
                                  }`}
                                >
                                  {task.status}
                                </span>
                              </td>
                              <td>{task.billableHours.toFixed(1)} hrs</td>
                              <td>${task.laborCharge.toFixed(2)}</td>
                              <td>${task.partsCost.toFixed(2)}</td>
                              <td>
                                <strong>${task.totalTaskCost.toFixed(2)}</strong>
                              </td>
                            </tr>
                            {task.usedParts.length > 0 && (
                              <tr className="sub-row">
                                <td colSpan={7}>
                                  <div className="used-parts-inline">
                                    <span className="parts-label">Used Parts:</span>
                                    {task.usedParts.map((p) => (
                                      <span key={p.partCode} className="part-tag">
                                        {p.partName} ({p.quantity}x @ ${p.unitPrice.toFixed(2)} = ${p.subtotal.toFixed(2)})
                                      </span>
                                    ))}
                                  </div>
                                </td>
                              </tr>
                            )}
                          </React.Fragment>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>

              {/* Financial Calculation Summary */}
              <div className="billing-summary-card">
                <h4>Itemised Calculation</h4>
                <div className="billing-row">
                  <span>Base Service Fee ({selectedInvoice.vehicleType}):</span>
                  <span>${selectedInvoice.baseServiceFee.toFixed(2)}</span>
                </div>
                <div className="billing-row">
                  <span>Service Tasks (Labor + Parts):</span>
                  <span>
                    $
                    {selectedInvoice.tasks
                      .reduce((sum, t) => sum + t.totalTaskCost, 0)
                      .toFixed(2)}
                  </span>
                </div>
                <div className="billing-row divider-row">
                  <span>
                    <strong>Subtotal:</strong>
                  </span>
                  <span>
                    <strong>${selectedInvoice.subtotal.toFixed(2)}</strong>
                  </span>
                </div>
                {selectedInvoice.discountAmount > 0 && (
                  <div className="billing-row discount-row">
                    <span>Discount Applied:</span>
                    <span>-${selectedInvoice.discountAmount.toFixed(2)}</span>
                  </div>
                )}
                <div className="billing-row">
                  <span>Tax ({Math.round(selectedInvoice.taxRate * 100)}%):</span>
                  <span>+${selectedInvoice.taxAmount.toFixed(2)}</span>
                </div>
                <div className="billing-row grand-total-row">
                  <span>Total Amount Due:</span>
                  <span className="grand-total-price">
                    ${selectedInvoice.totalAmount.toFixed(2)}
                  </span>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Modal: Open New Invoice */}
      <Modal
        isOpen={isOpenInvoiceModal}
        onClose={() => setIsOpenInvoiceModal(false)}
        title="Open New Invoice"
      >
        <form onSubmit={handleOpenInvoiceSubmit}>
          <div className="form-group">
            <label htmlFor="select-customer">Select Customer:</label>
            <select
              id="select-customer"
              className="form-control"
              value={openForm.customerId}
              onChange={(e) => setOpenForm({ ...openForm, customerId: e.target.value })}
              required
            >
              <option value="">-- Choose Customer --</option>
              {customers.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.phone}) - {c.loyaltyPoints} points
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label htmlFor="select-vehicle">Select Registered Vehicle:</label>
            {selectedCustomerVehicles.length === 0 ? (
              <p className="form-help-text text-danger">
                This customer has no registered vehicles. Register a vehicle first under Customers.
              </p>
            ) : (
              <select
                id="select-vehicle"
                className="form-control"
                value={openForm.licensePlate}
                onChange={(e) => setOpenForm({ ...openForm, licensePlate: e.target.value })}
                required
              >
                <option value="">-- Choose Vehicle --</option>
                {selectedCustomerVehicles.map((v) => (
                  <option key={v.licensePlate} value={v.licensePlate}>
                    [{v.type}] {v.licensePlate} — {v.brand} {v.model} (Base Fee: ${v.baseServiceFee})
                  </option>
                ))}
              </select>
            )}
          </div>

          <div className="modal-actions">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setIsOpenInvoiceModal(false)}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={!openForm.customerId || !openForm.licensePlate}
            >
              Open Invoice
            </button>
          </div>
        </form>
      </Modal>

      {/* Modal: Percentage Discount */}
      <Modal
        isOpen={isPercentDiscountModal}
        onClose={() => setIsPercentDiscountModal(false)}
        title="Apply Percentage Discount"
      >
        <form onSubmit={handleApplyPercentageDiscount}>
          <div className="form-group">
            <label htmlFor="discount-pct">Discount Percentage (0 - 100%):</label>
            <input
              id="discount-pct"
              type="number"
              min="0"
              max="100"
              step="1"
              className="form-control"
              value={percentageForm.percentage}
              onChange={(e) =>
                setPercentageForm({ ...percentageForm, percentage: Number(e.target.value) })
              }
              required
            />
            {percentageForm.percentage > 15 && (
              <p className="form-help-text text-warning">
                Discounts greater than 15% require Manager Authorization ID.
              </p>
            )}
          </div>

          <div className="form-group">
            <label htmlFor="manager-select">
              Authorizing Manager {percentageForm.percentage > 15 ? '(Required)' : '(Optional)'}:
            </label>
            <select
              id="manager-select"
              className="form-control"
              value={percentageForm.managerId || ''}
              onChange={(e) =>
                setPercentageForm({ ...percentageForm, managerId: e.target.value })
              }
              required={percentageForm.percentage > 15}
            >
              <option value="">-- No Manager Required --</option>
              {managers.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.name} (ID: {m.id.substring(0, 8)}...)
                </option>
              ))}
            </select>
          </div>

          <div className="modal-actions">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setIsPercentDiscountModal(false)}
            >
              Cancel
            </button>
            <button type="submit" className="btn btn-primary">
              Apply Percentage Discount
            </button>
          </div>
        </form>
      </Modal>

      {/* Modal: Coupon Discount */}
      <Modal
        isOpen={isCouponDiscountModal}
        onClose={() => setIsCouponDiscountModal(false)}
        title="Apply Coupon Code"
      >
        <form onSubmit={handleApplyCouponDiscount}>
          <div className="form-group">
            <label htmlFor="coupon-code">Coupon Code:</label>
            <input
              id="coupon-code"
              type="text"
              placeholder="e.g. WELCOME10, SPRING20, VIP50"
              className="form-control font-mono"
              value={couponForm.couponCode}
              onChange={(e) =>
                setCouponForm({ ...couponForm, couponCode: e.target.value })
              }
              required
            />
            <small className="form-help-text">Valid codes: WELCOME10, SPRING20, VIP50</small>
          </div>

          <div className="form-group">
            <label htmlFor="coupon-reduction">Fixed Reduction Amount ($):</label>
            <input
              id="coupon-reduction"
              type="number"
              min="0.01"
              step="0.01"
              className="form-control"
              value={couponForm.fixedReduction}
              onChange={(e) =>
                setCouponForm({ ...couponForm, fixedReduction: Number(e.target.value) })
              }
              required
            />
          </div>

          <div className="modal-actions">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setIsCouponDiscountModal(false)}
            >
              Cancel
            </button>
            <button type="submit" className="btn btn-primary">
              Apply Coupon
            </button>
          </div>
        </form>
      </Modal>

      {/* Modal: Tax Rate */}
      <Modal
        isOpen={isTaxRateModal}
        onClose={() => setIsTaxRateModal(false)}
        title="Update Tax Rate"
      >
        <form onSubmit={handleSetTaxRate}>
          <div className="form-group">
            <label htmlFor="tax-rate-input">Tax Rate (decimal, e.g. 0.10 for 10%):</label>
            <input
              id="tax-rate-input"
              type="number"
              min="0"
              max="1.0"
              step="0.01"
              className="form-control"
              value={taxRateForm.taxRate}
              onChange={(e) =>
                setTaxRateForm({ taxRate: Number(e.target.value) })
              }
              required
            />
            <small className="form-help-text">
              Current rate: {(taxRateForm.taxRate * 100).toFixed(1)}%
            </small>
          </div>

          <div className="modal-actions">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setIsTaxRateModal(false)}
            >
              Cancel
            </button>
            <button type="submit" className="btn btn-primary">
              Save Tax Rate
            </button>
          </div>
        </form>
      </Modal>

      {/* Modal: Printable Receipt */}
      <Modal
        isOpen={isReceiptModal}
        onClose={() => setIsReceiptModal(false)}
        title="Official Invoice Receipt"
      >
        <div className="receipt-view">
          <pre className="receipt-monospace">{receiptContent}</pre>
          <div className="modal-actions">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setIsReceiptModal(false)}
            >
              Close
            </button>
            <button
              type="button"
              className="btn btn-primary"
              onClick={handlePrintReceipt}
            >
              🖨️ Print Receipt
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
