import { useState } from 'react';
import { Navbar, type TabKey } from './components/Navbar';
import { DashboardPage } from './pages/DashboardPage';
import { CustomersPage } from './pages/CustomersPage';
import { InventoryPage } from './pages/InventoryPage';
import { EmployeesPage } from './pages/EmployeesPage';
import { TaskBoardPage } from './pages/TaskBoardPage';
import { InvoicesPage } from './pages/InvoicesPage';
import './App.css';

export function App() {
  const [activeTab, setActiveTab] = useState<TabKey>('dashboard');
  const [selectedInvoiceId, setSelectedInvoiceId] = useState<string | null>(null);

  const handleNavigate = (tab: TabKey, invoiceId?: string) => {
    if (invoiceId) {
      setSelectedInvoiceId(invoiceId);
    }
    setActiveTab(tab);
  };

  const handleNavigateToTasks = (invoiceId: string) => {
    setSelectedInvoiceId(invoiceId);
    setActiveTab('tasks');
  };

  return (
    <div className="app-layout">
      <Navbar
        activeTab={activeTab}
        onSelectTab={(tab) => {
          setSelectedInvoiceId(null);
          setActiveTab(tab);
        }}
      />

      <main className="app-main-content">
        {activeTab === 'dashboard' && (
          <DashboardPage onNavigate={handleNavigate} />
        )}
        {activeTab === 'customers' && <CustomersPage />}
        {activeTab === 'inventory' && <InventoryPage />}
        {activeTab === 'employees' && <EmployeesPage />}
        {activeTab === 'tasks' && (
          <TaskBoardPage initialInvoiceId={selectedInvoiceId} />
        )}
        {activeTab === 'invoices' && (
          <InvoicesPage
            initialInvoiceId={selectedInvoiceId}
            onNavigateToTasks={handleNavigateToTasks}
          />
        )}
      </main>

      <footer className="app-footer">
        <p>
          AutoCare Garage &amp; Service Management System &bull; Coursework Project for Object-Oriented Programming (OOP)
        </p>
      </footer>
    </div>
  );
}

export default App;
