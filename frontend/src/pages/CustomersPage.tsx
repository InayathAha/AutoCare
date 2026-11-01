import React, { useEffect, useState } from 'react';
import { customersApi, ApiErrorResponse } from '../api';
import type {
  ICustomer,
  ICustomerRequest,
  IVehicle,
  IVehicleRequest,
  IInvoiceSummary,
  IApiError,
  VehicleType,
} from '../types';
import { Modal } from '../components/Modal';
import { ErrorMessage } from '../components/ErrorMessage';

export const CustomersPage: React.FC = () => {
  const [customers, setCustomers] = useState<ICustomer[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCustomerId, setSelectedCustomerId] = useState<string | null>(null);
  const [selectedCustomer, setSelectedCustomer] = useState<ICustomer | null>(null);
  const [customerVehicles, setCustomerVehicles] = useState<IVehicle[]>([]);
  const [serviceHistory, setServiceHistory] = useState<IInvoiceSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<IApiError | null>(null);

  // Modals state
  const [isCreateCustomerOpen, setIsCreateCustomerOpen] = useState(false);
  const [isRegisterVehicleOpen, setIsRegisterVehicleOpen] = useState(false);
  const [isRedeemOpen, setIsRedeemOpen] = useState(false);

  // Form states
  const [customerForm, setCustomerForm] = useState<ICustomerRequest>({
    name: '',
    phone: '',
    email: '',
  });

  const [vehicleForm, setVehicleForm] = useState<IVehicleRequest>({
    type: 'CAR',
    licensePlate: '',
    brand: '',
    model: '',
    seatingCapacity: 5,
    cargoVolume: null,
    engineCC: null,
  });

  const [redeemPointsAmount, setRedeemPointsAmount] = useState<number>(10);

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

  const loadCustomers = async (q?: string) => {
    try {
      setLoading(true);
      setError(null);
      const data = await customersApi.list(q);
      setCustomers(data);
      if (selectedCustomerId) {
        const updated = data.find((c) => c.id === selectedCustomerId);
        if (updated) setSelectedCustomer(updated);
      }
    } catch (err) {
      handleApiError(err);
    } finally {
      setLoading(false);
    }
  };

  const loadCustomerDetails = async (id: string) => {
    try {
      setError(null);
      const [cust, vehicles, history] = await Promise.all([
        customersApi.get(id),
        customersApi.getVehicles(id),
        customersApi.getServiceHistory(id),
      ]);
      setSelectedCustomer(cust);
      setCustomerVehicles(vehicles);
      setServiceHistory(history);
    } catch (err) {
      handleApiError(err);
    }
  };

  useEffect(() => {
    loadCustomers();
  }, []);

  useEffect(() => {
    if (selectedCustomerId) {
      loadCustomerDetails(selectedCustomerId);
    } else {
      setSelectedCustomer(null);
      setCustomerVehicles([]);
      setServiceHistory([]);
    }
  }, [selectedCustomerId]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    loadCustomers(searchQuery);
  };

  const handleCreateCustomer = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setError(null);
      const created = await customersApi.create(customerForm);
      setIsCreateCustomerOpen(false);
      setCustomerForm({ name: '', phone: '', email: '' });
      await loadCustomers();
      setSelectedCustomerId(created.id);
    } catch (err) {
      handleApiError(err);
    }
  };

  const handleRegisterVehicle = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedCustomerId) return;
    try {
      setError(null);
      await customersApi.registerVehicle(selectedCustomerId, vehicleForm);
      setIsRegisterVehicleOpen(false);
      setVehicleForm({
        type: 'CAR',
        licensePlate: '',
        brand: '',
        model: '',
        seatingCapacity: 5,
        cargoVolume: null,
        engineCC: null,
      });
      await loadCustomerDetails(selectedCustomerId);
      await loadCustomers();
    } catch (err) {
      handleApiError(err);
    }
  };

  const handleRedeemPoints = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedCustomerId) return;
    try {
      setError(null);
      await customersApi.redeemLoyalty(selectedCustomerId, redeemPointsAmount);
      setIsRedeemOpen(false);
      await loadCustomerDetails(selectedCustomerId);
      await loadCustomers();
    } catch (err) {
      handleApiError(err);
    }
  };

  const handleDeleteCustomer = async (id: string) => {
    if (!window.confirm('Are you sure you want to delete this customer?')) return;
    try {
      setError(null);
      await customersApi.delete(id);
      setSelectedCustomerId(null);
      await loadCustomers();
    } catch (err) {
      handleApiError(err);
    }
  };

  return (
    <div className="page-container">
      <div className="page-header">
        <div>
          <h2>Customers &amp; Vehicles</h2>
          <p className="page-sub">Manage customer directory, vehicle registration, and loyalty rewards</p>
        </div>
        <button
          type="button"
          className="btn btn-primary"
          onClick={() => setIsCreateCustomerOpen(true)}
        >
          + Add Customer
        </button>
      </div>

      <ErrorMessage error={error} onDismiss={() => setError(null)} />

      <div className="search-bar-row">
        <form onSubmit={handleSearch} className="search-form">
          <input
            type="text"
            className="input"
            placeholder="Search by name or email..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
          <button type="submit" className="btn btn-secondary">
            Search
          </button>
          {searchQuery && (
            <button
              type="button"
              className="btn btn-ghost"
              onClick={() => {
                setSearchQuery('');
                loadCustomers('');
              }}
            >
              Clear
            </button>
          )}
        </form>
      </div>

      <div className="split-view">
        <div className="panel list-panel">
          <h3>Customer Directory</h3>
          {loading && <p className="loading-state">Loading customers...</p>}
          {!loading && customers.length === 0 && (
            <p className="empty-state">No customers found.</p>
          )}
          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Phone</th>
                  <th>Email</th>
                  <th>Points</th>
                  <th>Vehicles</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {customers.map((c) => (
                  <tr
                    key={c.id}
                    className={selectedCustomerId === c.id ? 'selected-row' : ''}
                    onClick={() => setSelectedCustomerId(c.id)}
                    style={{ cursor: 'pointer' }}
                  >
                    <td><strong>{c.name}</strong></td>
                    <td>{c.phone}</td>
                    <td>{c.email}</td>
                    <td><span className="badge badge-points">{c.loyaltyPoints} pts</span></td>
                    <td>{c.vehicleCount}</td>
                    <td>
                      <button
                        type="button"
                        className="btn btn-sm btn-ghost"
                        onClick={(e) => {
                          e.stopPropagation();
                          setSelectedCustomerId(c.id);
                        }}
                      >
                        View
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {selectedCustomer && (
          <div className="panel detail-panel">
            <div className="detail-header">
              <div>
                <h3>{selectedCustomer.name}</h3>
                <span className="text-muted">{selectedCustomer.email} • {selectedCustomer.phone}</span>
              </div>
              <div className="detail-actions">
                <button
                  type="button"
                  className="btn btn-sm btn-outline"
                  onClick={() => setIsRedeemOpen(true)}
                  disabled={selectedCustomer.loyaltyPoints <= 0}
                >
                  🎁 Redeem Points
                </button>
                <button
                  type="button"
                  className="btn btn-sm btn-danger"
                  onClick={() => handleDeleteCustomer(selectedCustomer.id)}
                >
                  Delete
                </button>
              </div>
            </div>

            <div className="customer-stats">
              <div className="stat-card">
                <span className="stat-label">Loyalty Balance</span>
                <span className="stat-value">{selectedCustomer.loyaltyPoints} points</span>
              </div>
              <div className="stat-card">
                <span className="stat-label">Registered Vehicles</span>
                <span className="stat-value">{customerVehicles.length}</span>
              </div>
            </div>

            <div className="sub-section">
              <div className="sub-header">
                <h4>Registered Vehicles</h4>
                <button
                  type="button"
                  className="btn btn-sm btn-primary"
                  onClick={() => setIsRegisterVehicleOpen(true)}
                >
                  + Register Vehicle
                </button>
              </div>

              {customerVehicles.length === 0 ? (
                <p className="empty-sub">No vehicles registered yet.</p>
              ) : (
                <div className="vehicle-cards-grid">
                  {customerVehicles.map((v) => (
                    <div key={v.licensePlate} className="vehicle-card">
                      <div className="vehicle-card-top">
                        <span className="plate-badge">{v.licensePlate}</span>
                        <span className={`type-badge type-${v.type.toLowerCase()}`}>
                          {v.type}
                        </span>
                      </div>
                      <div className="vehicle-card-body">
                        <h5>{v.brand} {v.model}</h5>
                        <p className="text-muted">Base Service Fee: ${v.baseServiceFee.toFixed(2)}</p>
                        {v.seatingCapacity !== null && (
                          <p className="spec">💺 Seating: {v.seatingCapacity} seats</p>
                        )}
                        {v.cargoVolume !== null && (
                          <p className="spec">📦 Cargo Volume: {v.cargoVolume} m³</p>
                        )}
                        {v.engineCC !== null && (
                          <p className="spec">⚡ Engine: {v.engineCC} cc</p>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="sub-section">
              <h4>Service History</h4>
              {serviceHistory.length === 0 ? (
                <p className="empty-sub">No previous service invoices found.</p>
              ) : (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Invoice ID</th>
                      <th>Vehicle Plate</th>
                      <th>Status</th>
                      <th>Total</th>
                      <th>Date</th>
                    </tr>
                  </thead>
                  <tbody>
                    {serviceHistory.map((inv) => (
                      <tr key={inv.invoiceId}>
                        <td><code>{inv.invoiceId.substring(0, 8)}...</code></td>
                        <td>{inv.licensePlate}</td>
                        <td><span className={`status-badge status-${inv.status.toLowerCase()}`}>{inv.status}</span></td>
                        <td><strong>${inv.totalAmount.toFixed(2)}</strong></td>
                        <td>{new Date(inv.issuedAt).toLocaleDateString()}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Add Customer Modal */}
      <Modal
        title="Add New Customer"
        isOpen={isCreateCustomerOpen}
        onClose={() => setIsCreateCustomerOpen(false)}
      >
        <form onSubmit={handleCreateCustomer} className="form-grid">
          <div className="form-group">
            <label>Full Name *</label>
            <input
              type="text"
              className="input"
              required
              value={customerForm.name}
              onChange={(e) => setCustomerForm({ ...customerForm, name: e.target.value })}
            />
          </div>
          <div className="form-group">
            <label>Phone Number *</label>
            <input
              type="text"
              className="input"
              required
              value={customerForm.phone}
              onChange={(e) => setCustomerForm({ ...customerForm, phone: e.target.value })}
            />
          </div>
          <div className="form-group">
            <label>Email Address *</label>
            <input
              type="email"
              className="input"
              required
              value={customerForm.email}
              onChange={(e) => setCustomerForm({ ...customerForm, email: e.target.value })}
            />
          </div>
          <div className="modal-footer">
            <button
              type="button"
              className="btn btn-ghost"
              onClick={() => setIsCreateCustomerOpen(false)}
            >
              Cancel
            </button>
            <button type="submit" className="btn btn-primary">
              Save Customer
            </button>
          </div>
        </form>
      </Modal>

      {/* Register Vehicle Modal */}
      <Modal
        title={`Register Vehicle for ${selectedCustomer?.name || ''}`}
        isOpen={isRegisterVehicleOpen}
        onClose={() => setIsRegisterVehicleOpen(false)}
      >
        <form onSubmit={handleRegisterVehicle} className="form-grid">
          <div className="form-group">
            <label>Vehicle Category *</label>
            <select
              className="input"
              value={vehicleForm.type}
              onChange={(e) =>
                setVehicleForm({
                  ...vehicleForm,
                  type: e.target.value as VehicleType,
                  seatingCapacity: e.target.value === 'CAR' ? 5 : null,
                  cargoVolume: e.target.value === 'VAN' ? 10.0 : null,
                  engineCC: e.target.value === 'MOTORCYCLE' ? 250 : null,
                })
              }
            >
              <option value="CAR">Car (Base Fee: $50.00)</option>
              <option value="VAN">Van (Base Fee: $85.00)</option>
              <option value="MOTORCYCLE">Motorcycle (Base Fee: $30.00)</option>
            </select>
          </div>
          <div className="form-group">
            <label>License Plate *</label>
            <input
              type="text"
              className="input"
              placeholder="e.g. CAB-1234"
              required
              value={vehicleForm.licensePlate}
              onChange={(e) => setVehicleForm({ ...vehicleForm, licensePlate: e.target.value.toUpperCase() })}
            />
          </div>
          <div className="form-group">
            <label>Brand *</label>
            <input
              type="text"
              className="input"
              placeholder="e.g. Toyota"
              required
              value={vehicleForm.brand}
              onChange={(e) => setVehicleForm({ ...vehicleForm, brand: e.target.value })}
            />
          </div>
          <div className="form-group">
            <label>Model &amp; Year *</label>
            <input
              type="text"
              className="input"
              placeholder="e.g. Corolla 2022"
              required
              value={vehicleForm.model}
              onChange={(e) => setVehicleForm({ ...vehicleForm, model: e.target.value })}
            />
          </div>

          {/* Conditional Fields based on Vehicle Category */}
          {vehicleForm.type === 'CAR' && (
            <div className="form-group">
              <label>Seating Capacity *</label>
              <input
                type="number"
                min="1"
                className="input"
                required
                value={vehicleForm.seatingCapacity || ''}
                onChange={(e) => setVehicleForm({ ...vehicleForm, seatingCapacity: parseInt(e.target.value, 10) || null })}
              />
            </div>
          )}

          {vehicleForm.type === 'VAN' && (
            <div className="form-group">
              <label>Cargo Volume (m³) *</label>
              <input
                type="number"
                step="0.1"
                min="0.1"
                className="input"
                required
                value={vehicleForm.cargoVolume || ''}
                onChange={(e) => setVehicleForm({ ...vehicleForm, cargoVolume: parseFloat(e.target.value) || null })}
              />
            </div>
          )}

          {vehicleForm.type === 'MOTORCYCLE' && (
            <div className="form-group">
              <label>Engine Displacement (cc) *</label>
              <input
                type="number"
                min="50"
                className="input"
                required
                value={vehicleForm.engineCC || ''}
                onChange={(e) => setVehicleForm({ ...vehicleForm, engineCC: parseInt(e.target.value, 10) || null })}
              />
            </div>
          )}

          <div className="modal-footer">
            <button
              type="button"
              className="btn btn-ghost"
              onClick={() => setIsRegisterVehicleOpen(false)}
            >
              Cancel
            </button>
            <button type="submit" className="btn btn-primary">
              Register Vehicle
            </button>
          </div>
        </form>
      </Modal>

      {/* Redeem Loyalty Points Modal */}
      <Modal
        title={`Redeem Loyalty Points (${selectedCustomer?.loyaltyPoints || 0} pts available)`}
        isOpen={isRedeemOpen}
        onClose={() => setIsRedeemOpen(false)}
      >
        <form onSubmit={handleRedeemPoints} className="form-grid">
          <p className="text-muted">
            1 loyalty point = $0.10 discount. Available: {selectedCustomer?.loyaltyPoints || 0} points
          </p>
          <div className="form-group">
            <label>Points to Redeem *</label>
            <input
              type="number"
              min="1"
              max={selectedCustomer?.loyaltyPoints || 1}
              className="input"
              required
              value={redeemPointsAmount}
              onChange={(e) => setRedeemPointsAmount(parseInt(e.target.value, 10) || 0)}
            />
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-ghost" onClick={() => setIsRedeemOpen(false)}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary">
              Confirm Redemption
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
