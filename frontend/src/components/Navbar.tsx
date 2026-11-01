import React from 'react';

export type TabKey = 'dashboard' | 'customers' | 'inventory' | 'employees' | 'tasks' | 'invoices';

interface NavbarProps {
  activeTab: TabKey;
  onSelectTab: (tab: TabKey) => void;
}

export const Navbar: React.FC<NavbarProps> = ({ activeTab, onSelectTab }) => {
  const tabs: { key: TabKey; label: string; icon: string }[] = [
    { key: 'dashboard', label: 'Dashboard', icon: '📊' },
    { key: 'customers', label: 'Customers & Vehicles', icon: '👤' },
    { key: 'inventory', label: 'Inventory', icon: '📦' },
    { key: 'employees', label: 'Employees', icon: '🔧' },
    { key: 'tasks', label: 'Task Board', icon: '📋' },
    { key: 'invoices', label: 'Invoices & Billing', icon: '💳' },
  ];

  return (
    <header className="app-header">
      <div className="header-brand">
        <span className="brand-logo">🚗</span>
        <div className="brand-titles">
          <h1>AutoCare</h1>
          <span className="brand-sub">Garage & Service Management</span>
        </div>
      </div>
      <nav className="header-nav">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            type="button"
            className={`nav-tab ${activeTab === tab.key ? 'active' : ''}`}
            onClick={() => onSelectTab(tab.key)}
          >
            <span className="tab-icon">{tab.icon}</span>
            <span className="tab-label">{tab.label}</span>
          </button>
        ))}
      </nav>
    </header>
  );
};
