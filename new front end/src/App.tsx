import { useEffect, useMemo, useState, type ReactNode } from "react"
import {
  customersApi,
  vehiclesApi,
  employeesApi,
  inventoryApi,
  tasksApi,
  invoicesApi,
  ApiErrorResponse,
} from "./api"
import type {
  ICustomer,
  IVehicle,
  IEmployee,
  ITask,
  IInventoryItem,
  IInvoice,
  IInvoiceSummary,
  VehicleType,
  EmployeeType,
  TaskStatus,
} from "./types"

type Page =
  | "design"
  | "dashboard"
  | "customers"
  | "customer"
  | "employees"
  | "tasks"
  | "inventory"
  | "invoices"
  | "invoice"
  | "receipt"
  | "states"
  | "flow"

type IconName =
  | "grid"
  | "dashboard"
  | "people"
  | "person"
  | "wrench"
  | "tasks"
  | "box"
  | "invoice"
  | "receipt"
  | "warning"
  | "flow"
  | "search"
  | "plus"
  | "chevron"
  | "car"
  | "star"
  | "clock"
  | "lock"
  | "print"
  | "download"
  | "check"
  | "more"
  | "filter"
  | "bell"
  | "gear"
  | "arrow"
  | "x"
  | "refresh"

const iconPaths: Record<IconName, ReactNode> = {
  grid: (
    <>
      <rect x="3" y="3" width="7" height="7" rx="2" />
      <rect x="14" y="3" width="7" height="7" rx="2" />
      <rect x="3" y="14" width="7" height="7" rx="2" />
      <rect x="14" y="14" width="7" height="7" rx="2" />
    </>
  ),
  dashboard: (
    <>
      <path d="M4 13a8 8 0 1 1 16 0" />
      <path d="m12 13 4-4" />
      <path d="M5 19h14" />
    </>
  ),
  people: (
    <>
      <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
      <circle cx="9" cy="7" r="4" />
      <path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" />
    </>
  ),
  person: (
    <>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21a8 8 0 0 1 16 0" />
    </>
  ),
  wrench: (
    <path d="M14.7 6.3a4 4 0 0 0-5-5L12 3.6 9.6 6 7.3 3.7a4 4 0 0 0 5 5l-8.6 8.6a2.1 2.1 0 1 0 3 3z" />
  ),
  tasks: (
    <>
      <rect x="3" y="3" width="18" height="18" rx="3" />
      <path d="m8 12 2 2 5-5M8 18h8" />
    </>
  ),
  box: (
    <>
      <path d="m21 8-9-5-9 5 9 5z" />
      <path d="m3 8 9 5 9-5v8l-9 5-9-5z" />
      <path d="M12 13v8" />
    </>
  ),
  invoice: (
    <>
      <path d="M6 2h9l4 4v16l-3-2-4 2-4-2-3 2V3a1 1 0 0 1 1-1z" />
      <path d="M9 9h6M9 13h6M9 17h3" />
    </>
  ),
  receipt: (
    <>
      <path d="M5 3h14v18l-3-2-4 2-4-2-3 2z" />
      <path d="M8 8h8M8 12h8M8 16h5" />
    </>
  ),
  warning: (
    <>
      <path d="M10.3 3.7 2.6 17a2 2 0 0 0 1.7 3h15.4a2 2 0 0 0 1.7-3L13.7 3.7a2 2 0 0 0-3.4 0z" />
      <path d="M12 9v4M12 17h.01" />
    </>
  ),
  flow: (
    <>
      <circle cx="5" cy="5" r="3" />
      <circle cx="19" cy="19" r="3" />
      <path d="M8 5h4a4 4 0 0 1 4 4v6M16 19h-4a4 4 0 0 1-4-4V9" />
    </>
  ),
  search: (
    <>
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-4-4" />
    </>
  ),
  plus: <path d="M12 5v14M5 12h14" />,
  chevron: <path d="m9 18 6-6-6-6" />,
  car: (
    <>
      <path d="m5 11 2-5h10l2 5" />
      <rect x="3" y="10" width="18" height="8" rx="2" />
      <circle cx="7" cy="18" r="2" />
      <circle cx="17" cy="18" r="2" />
    </>
  ),
  star: (
    <path d="m12 2 3 6 6.5 1-4.7 4.6 1.1 6.4-5.9-3.1L6.1 20l1.1-6.4L2.5 9 9 8z" />
  ),
  clock: (
    <>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 2" />
    </>
  ),
  lock: (
    <>
      <rect x="5" y="10" width="14" height="11" rx="2" />
      <path d="M8 10V7a4 4 0 0 1 8 0v3" />
    </>
  ),
  print: (
    <>
      <path d="M6 9V3h12v6M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2" />
      <rect x="6" y="14" width="12" height="8" />
    </>
  ),
  download: (
    <>
      <path d="M12 3v12M7 10l5 5 5-5" />
      <path d="M4 20h16" />
    </>
  ),
  check: <path d="m5 12 4 4L19 6" />,
  more: (
    <>
      <circle cx="5" cy="12" r="1" />
      <circle cx="12" cy="12" r="1" />
      <circle cx="19" cy="12" r="1" />
    </>
  ),
  filter: <path d="M3 5h18l-7 8v6l-4 2v-8z" />,
  bell: (
    <>
      <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" />
      <path d="M10 21h4" />
    </>
  ),
  gear: (
    <>
      <circle cx="12" cy="12" r="3" />
      <path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1-2.8 2.8-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.6v.2h-4V21a1.7 1.7 0 0 0-1-1.6 1.7 1.7 0 0 0-1.9.3l-.1.1L4.2 17l.1-.1a1.7 1.7 0 0 0 .3-1.9A1.7 1.7 0 0 0 3 14H2.8v-4H3a1.7 1.7 0 0 0 1.6-1 1.7 1.7 0 0 0-.3-1.9L4.2 7 7 4.2l.1.1a1.7 1.7 0 0 0 1.9.3A1.7 1.7 0 0 0 10 3V2.8h4V3a1.7 1.7 0 0 0 1 1.6 1.7 1.7 0 0 0 1.9-.3l.1-.1L19.8 7l-.1.1a1.7 1.7 0 0 0-.3 1.9 1.7 1.7 0 0 0 1.6 1h.2v4H21a1.7 1.7 0 0 0-1.6 1z" />
    </>
  ),
  arrow: (
    <>
      <path d="M5 12h14M14 7l5 5-5 5" />
    </>
  ),
  x: <path d="m6 6 12 12M18 6 6 18" />,
  refresh: (
    <>
      <path d="M21.5 2v6h-6M2.5 22v-6h6" />
      <path d="M19.9 15.5A9 9 0 0 1 4.1 8.5L2.5 10m17.4 4-.9 1.5A9 9 0 0 1 4.1 15.5" />
    </>
  ),
}

function Icon({
  name,
  size = 20,
  filled = false,
}: {
  name: IconName
  size?: number
  filled?: boolean
}) {
  return (
    <svg
      className="icon"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill={filled ? "currentColor" : "none"}
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {iconPaths[name]}
    </svg>
  )
}

const nav: { id: Page; label: string; icon: IconName; number?: string }[] = [
  { id: "design", label: "Design System", icon: "grid", number: "00" },
  { id: "dashboard", label: "Dashboard", icon: "dashboard", number: "01" },
  { id: "customers", label: "Customers", icon: "people", number: "02" },
  { id: "customer", label: "Customer Detail", icon: "person", number: "03" },
  { id: "employees", label: "Employees", icon: "wrench", number: "04" },
  { id: "tasks", label: "Task Board", icon: "tasks", number: "05" },
  { id: "inventory", label: "Inventory", icon: "box", number: "06" },
  { id: "invoices", label: "Invoices", icon: "invoice", number: "07" },
  { id: "invoice", label: "Invoice Detail", icon: "receipt", number: "08" },
  { id: "receipt", label: "Receipt", icon: "receipt", number: "09" },
  { id: "states", label: "States & Errors", icon: "warning", number: "10" },
  { id: "flow", label: "Prototype Flow", icon: "flow", number: "11" },
]

function Badge({ status }: { status: string }) {
  const s = status.toUpperCase()
  const tone = [
    "PAID",
    "COMPLETED",
    "IN STOCK",
    "MANAGER",
    "CONNECTED",
  ].includes(s)
    ? "green"
    : ["ISSUED", "IN_PROGRESS", "LOW STOCK", "LOW_STOCK", "MECHANIC"].includes(
          s,
        )
      ? "orange"
      : ["OPEN", "CAR", "VAN", "MOTORCYCLE"].includes(s)
        ? "blue"
        : "gray"
  return (
    <span className={`badge ${tone}`}>
      <span className="badge-dot" />
      {status.replace("_", " ")}
    </span>
  )
}

function Button({
  children,
  icon,
  tone = "primary",
  onClick,
  disabled,
  title,
  type = "button",
}: {
  children: ReactNode
  icon?: IconName
  tone?: "primary" | "secondary" | "danger" | "ghost"
  onClick?: () => void
  disabled?: boolean
  title?: string
  type?: "button" | "submit" | "reset"
}) {
  return (
    <button
      type={type}
      className={`button ${tone}`}
      onClick={onClick}
      disabled={disabled}
      title={title}
    >
      {icon && <Icon name={icon} size={17} />}
      <span>{children}</span>
    </button>
  )
}

function Card({
  children,
  className = "",
}: {
  children: ReactNode
  className?: string
}) {
  return <section className={`card ${className}`}>{children}</section>
}

function Segmented({
  items,
  active,
  onChange,
}: {
  items: string[]
  active: string
  onChange: (item: string) => void
}) {
  return (
    <div className="segmented">
      {items.map((item) => (
        <button
          key={item}
          type="button"
          className={active === item ? "active" : ""}
          onClick={() => onChange(item)}
        >
          {item}
        </button>
      ))}
    </div>
  )
}

function Search({
  placeholder = "Search",
  value = "",
  onChange,
}: {
  placeholder?: string
  value?: string
  onChange?: (val: string) => void
}) {
  return (
    <label className="search">
      <Icon name="search" size={17} />
      <input
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange?.(e.target.value)}
      />
      <kbd>⌘ K</kbd>
    </label>
  )
}

function PageHeader({
  title,
  eyebrow,
  children,
}: {
  title: string
  eyebrow?: string
  children?: ReactNode
}) {
  return (
    <header className="page-header">
      <div>
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        <h1>{title}</h1>
      </div>
      {children && <div className="header-actions">{children}</div>}
    </header>
  )
}

function Field({
  label,
  placeholder,
  error,
  disabled,
  value,
  defaultValue,
  type = "text",
  step,
  onChange,
  name,
}: {
  label: string
  placeholder?: string
  error?: string
  disabled?: boolean
  value?: string | number
  defaultValue?: string | number
  type?: string
  step?: string
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void
  name?: string
}) {
  return (
    <label className={`field ${error ? "has-error" : ""}`}>
      <span>{label}</span>
      <input
        name={name}
        type={type}
        step={step}
        placeholder={placeholder}
        disabled={disabled}
        value={value}
        defaultValue={defaultValue}
        onChange={onChange}
      />
      {error && <small>{error}</small>}
    </label>
  )
}

function Sheet({
  title,
  children,
  onClose,
  wide = false,
}: {
  title: string
  children: ReactNode
  onClose: () => void
  wide?: boolean
}) {
  return (
    <div className="modal-backdrop" onMouseDown={onClose}>
      <section
        className={`sheet ${wide ? "wide" : ""}`}
        onMouseDown={(e) => e.stopPropagation()}
      >
        <div className="grabber" />
        <div className="sheet-header">
          <button className="icon-button" onClick={onClose} type="button">
            <Icon name="x" />
          </button>
          <h2>{title}</h2>
          <Button onClick={onClose} tone="secondary">
            Done
          </Button>
        </div>
        {children}
      </section>
    </div>
  )
}

function Empty({
  icon = "box",
  title,
  text,
  actionText,
  onAction,
}: {
  icon?: IconName
  title: string
  text: string
  actionText?: string
  onAction?: () => void
}) {
  return (
    <div className="empty">
      <div className="empty-symbol">
        <Icon name={icon} size={28} />
      </div>
      <h3>{title}</h3>
      <p>{text}</p>
      {actionText && (
        <Button icon="plus" onClick={onAction}>
          {actionText}
        </Button>
      )}
    </div>
  )
}

function Skeleton() {
  return (
    <div className="skeleton-list">
      {[1, 2, 3, 4].map((x) => (
        <div className="skeleton-row" key={x}>
          <span className="sk-circle" />
          <div>
            <span />
            <span />
          </div>
        </div>
      ))}
    </div>
  )
}

function ErrorBanner({
  title = "Unable to complete request",
  text = "The server returned an error.",
}: {
  title?: string
  text?: string
}) {
  return (
    <div className="error-banner">
      <Icon name="warning" />
      <div>
        <strong>{title}</strong>
        <p>{text}</p>
      </div>
    </div>
  )
}

function Sidebar({
  page,
  setPage,
  taskCount = 0,
}: {
  page: Page
  setPage: (p: Page) => void
  taskCount?: number
}) {
  return (
    <aside className="sidebar">
      <button className="brand" onClick={() => setPage("dashboard")}>
        <span className="brandmark">
          <Icon name="wrench" size={18} />
        </span>
        <span>
          <strong>AutoCare</strong>
          <small>Service Studio</small>
        </span>
      </button>
      <nav>
        {nav.slice(1, 10).map((item) => (
          <button
            key={item.id}
            className={page === item.id ? "active" : ""}
            onClick={() => setPage(item.id)}
            title={item.label}
          >
            <Icon name={item.icon} />
            <span>{item.label}</span>
            {item.id === "tasks" && taskCount > 0 && <i>{taskCount}</i>}
          </button>
        ))}
      </nav>
      <div className="sidebar-bottom">
        {nav
          .slice(0, 1)
          .concat(nav.slice(10))
          .map((item) => (
            <button
              key={item.id}
              className={page === item.id ? "active" : ""}
              onClick={() => setPage(item.id)}
              title={item.label}
            >
              <Icon name={item.icon} />
              <span>{item.label}</span>
            </button>
          ))}
        <div className="profile">
          <span className="avatar sm">AM</span>
          <span>
            <strong>Alex Morgan</strong>
            <small>Shop Manager</small>
          </span>
          <Icon name="more" size={18} />
        </div>
      </div>
    </aside>
  )
}

function Topbar({
  title,
  onRefresh,
}: {
  title: string
  onRefresh?: () => void
}) {
  return (
    <div className="topbar">
      <div className="crumb">
        <span>AutoCare</span>
        <Icon name="chevron" size={14} />
        <strong>{title}</strong>
      </div>
      <div className="top-actions">
        {onRefresh && (
          <button
            className="icon-button"
            onClick={onRefresh}
            title="Refresh data from backend"
          >
            <Icon name="refresh" size={17} />
          </button>
        )}
        <button className="icon-button notification" title="Live status">
          <Icon name="bell" />
          <i />
        </button>
        <span className="avatar sm">AM</span>
      </div>
    </div>
  )
}

// -------------------------------------------------------------
// Screen: DesignSystem
// -------------------------------------------------------------
function DesignSystem() {
  const colors = [
    ["Primary", "#007AFF"],
    ["Success", "#34C759"],
    ["Warning", "#FF9500"],
    ["Destructive", "#FF3B30"],
    ["Secondary", "#8E8E93"],
    ["Background", "#F5F5F7"],
    ["Surface", "#FFFFFF"],
    ["Separator", "#E5E5EA"],
  ]
  return (
    <>
      <PageHeader title="Design System" eyebrow="00 · Foundations & Components">
        <Segmented
          items={["Light", "Dark"]}
          active="Light"
          onChange={() => {}}
        />
      </PageHeader>
      <div className="design-grid">
        <Card className="span-2">
          <div className="card-head">
            <div>
              <h2>Colour variables</h2>
              <p>Semantic tokens with light and dark appearances</p>
            </div>
          </div>
          <div className="swatches">
            {colors.map(([name, color]) => (
              <div className="swatch" key={name}>
                <span style={{ background: color }} />
                <strong>{name}</strong>
                <small>{color}</small>
              </div>
            ))}
          </div>
        </Card>
        <Card>
          <div className="card-head">
            <h2>Type scale</h2>
          </div>
          <div className="type-list">
            <div className="large-title">Large Title</div>
            <h2>Title</h2>
            <h3>Headline</h3>
            <p>Body — SF Pro Display</p>
            <small>Footnote and supporting metadata</small>
          </div>
        </Card>
        <Card>
          <div className="card-head">
            <h2>Spacing & radii</h2>
          </div>
          <div className="spacing-demo">
            {[4, 8, 12, 16, 24, 32].map((x) => (
              <div key={x}>
                <span style={{ width: x * 2 }} />
                <small>{x}</small>
              </div>
            ))}
          </div>
          <div className="radius-demo">
            <i />
            <i />
            <i />
          </div>
        </Card>
        <Card className="span-2">
          <div className="card-head">
            <div>
              <h2>Controls</h2>
              <p>Variants, states, and 44pt minimum targets</p>
            </div>
          </div>
          <div className="component-row">
            <Button>Primary</Button>
            <Button tone="secondary">Secondary</Button>
            <Button tone="danger">Destructive</Button>
            <Button tone="primary" disabled>
              Disabled
            </Button>
          </div>
          <div className="component-row">
            <Field label="Text field" placeholder="Default" />
            <Field label="Focused" value="Focused value" />
            <Field label="Error" error="This field is required" />
            <Field label="Disabled" value="Not editable" disabled />
          </div>
          <div className="component-row badges">
            {[
              "PENDING",
              "IN_PROGRESS",
              "COMPLETED",
              "OPEN",
              "ISSUED",
              "PAID",
              "LOW STOCK",
            ].map((x) => (
              <Badge key={x} status={x} />
            ))}
          </div>
        </Card>
        <Card>
          <div className="card-head">
            <h2>Feedback</h2>
          </div>
          <ErrorBanner text="IApiError.message · email: already exists" />
          <div className="toast-inline">
            <Icon name="check" />
            <span>Customer saved</span>
          </div>
          <Skeleton />
        </Card>
        <Card>
          <div className="card-head">
            <h2>Empty state</h2>
          </div>
          <Empty
            title="Nothing here yet"
            text="New records will appear here."
          />
        </Card>
      </div>
    </>
  )
}

// -------------------------------------------------------------
// Screen: Dashboard
// -------------------------------------------------------------
function Dashboard({
  setPage,
  openSheet,
  tasks,
  inventory,
  invoices,
  onSelectInvoice,
  loading,
}: {
  setPage: (p: Page) => void
  openSheet: (s: string) => void
  tasks: ITask[]
  inventory: IInventoryItem[]
  invoices: IInvoiceSummary[]
  onSelectInvoice: (id: string) => void
  loading: boolean
}) {
  const pendingCount = tasks.filter((t) => t.status === "PENDING").length
  const inProgressCount = tasks.filter((t) => t.status === "IN_PROGRESS").length
  const lowStockItems = inventory.filter((i) => i.lowStock)
  const unpaidInvoices = invoices.filter((i) => i.status !== "PAID")
  const unpaidTotal = unpaidInvoices.reduce(
    (sum, inv) => sum + (inv.totalAmount || 0),
    0,
  )

  const stats = [
    {
      label: "Open tasks",
      value: pendingCount.toString(),
      note: `${pendingCount} awaiting start`,
      icon: "tasks" as IconName,
      tone: "blue",
    },
    {
      label: "In progress",
      value: inProgressCount.toString(),
      note: `Across active garage bays`,
      icon: "clock" as IconName,
      tone: "orange",
    },
    {
      label: "Low-stock parts",
      value: lowStockItems.length.toString(),
      note: lowStockItems.length > 0 ? "Needs reorder" : "Stock healthy",
      icon: "box" as IconName,
      tone: "red",
    },
    {
      label: "Unpaid invoices",
      value: unpaidInvoices.length.toString(),
      note: `$${unpaidTotal.toFixed(2)} due`,
      icon: "invoice" as IconName,
      tone: "gray",
    },
  ]

  return (
    <>
      <PageHeader
        title="Good morning, Alex"
        eyebrow={`Today · AutoCare Garage Console`}
      >
        <Button icon="plus" onClick={() => openSheet("customer")}>
          New Customer
        </Button>
      </PageHeader>
      <div className="stat-grid">
        {stats.map(({ label, value, note, icon, tone }) => (
          <Card className="stat" key={label}>
            <div className={`stat-icon ${tone}`}>
              <Icon name={icon} />
            </div>
            <div>
              <p>{label}</p>
              <strong>{loading ? "..." : value}</strong>
              <small>{note}</small>
            </div>
          </Card>
        ))}
      </div>
      <div className="dashboard-grid">
        <Card className="task-snapshot">
          <div className="card-head">
            <div>
              <h2>Task Board snapshot</h2>
              <p>Live progress across today’s workshop</p>
            </div>
            <button className="link" onClick={() => setPage("tasks")}>
              View board <Icon name="chevron" size={14} />
            </button>
          </div>
          <div className="mini-kanban">
            {[
              {
                s: "PENDING",
                c: tasks.filter((t) => t.status === "PENDING").length,
                dot: "k0",
              },
              {
                s: "IN PROGRESS",
                c: tasks.filter((t) => t.status === "IN_PROGRESS").length,
                dot: "k1",
              },
              {
                s: "COMPLETED",
                c: tasks.filter((t) => t.status === "COMPLETED").length,
                dot: "k2",
              },
            ].map(({ s, c, dot }) => (
              <div key={s}>
                <span className={`kanban-dot ${dot}`} />
                <small>{s}</small>
                <strong>{loading ? "-" : c}</strong>
              </div>
            ))}
          </div>
        </Card>
        <Card>
          <div className="card-head">
            <div>
              <h2>Quick actions</h2>
              <p>Common workshop tasks</p>
            </div>
          </div>
          <div className="quick-actions">
            <button onClick={() => openSheet("customer")}>
              <span>
                <Icon name="person" />
              </span>
              <div>
                <strong>New Customer</strong>
                <small>Create a customer profile</small>
              </div>
              <Icon name="chevron" />
            </button>
            <button onClick={() => openSheet("newInvoice")}>
              <span>
                <Icon name="invoice" />
              </span>
              <div>
                <strong>New Invoice</strong>
                <small>Start a service order</small>
              </div>
              <Icon name="chevron" />
            </button>
            <button onClick={() => openSheet("part")}>
              <span>
                <Icon name="box" />
              </span>
              <div>
                <strong>Add Part</strong>
                <small>Expand your inventory</small>
              </div>
              <Icon name="chevron" />
            </button>
          </div>
        </Card>
        <Card>
          <div className="card-head">
            <div>
              <h2>Low-stock alerts</h2>
              <p>Parts at or below reorder threshold</p>
            </div>
            <button className="link" onClick={() => setPage("inventory")}>
              Inventory
            </button>
          </div>
          <div className="stock-list">
            {lowStockItems.length === 0 ? (
              <p
                style={{
                  padding: "16px 20px",
                  color: "var(--secondary)",
                  fontSize: 13,
                }}
              >
                No parts currently below reorder threshold.
              </p>
            ) : (
              lowStockItems.slice(0, 5).map((item) => (
                <div key={item.partCode}>
                  <span>
                    <strong>{item.partName}</strong>
                    <small>
                      {item.stockQuantity} of {item.reorderThreshold} threshold
                    </small>
                  </span>
                  <div>
                    <i
                      style={{
                        width: `${Math.min(
                          100,
                          Math.max(
                            8,
                            (item.stockQuantity /
                              Math.max(item.reorderThreshold, 1)) *
                              100,
                          ),
                        )}%`,
                      }}
                    />
                  </div>
                </div>
              ))
            )}
          </div>
        </Card>
        <Card>
          <div className="card-head">
            <div>
              <h2>Recent invoices</h2>
              <p>Latest billing activity</p>
            </div>
            <button className="link" onClick={() => setPage("invoices")}>
              View all
            </button>
          </div>
          <div className="compact-table">
            {invoices.length === 0 ? (
              <p
                style={{
                  padding: "16px 20px",
                  color: "var(--secondary)",
                  fontSize: 13,
                }}
              >
                No invoices found.
              </p>
            ) : (
              invoices.slice(0, 5).map((row) => (
                <button
                  key={row.invoiceId}
                  onClick={() => {
                    onSelectInvoice(row.invoiceId)
                    setPage("invoice")
                  }}
                >
                  <span className="customer-cell">
                    <i>
                      {(row.customerName || "Customer")
                        .split(" ")
                        .map((x) => x[0])
                        .join("")
                        .slice(0, 2)
                        .toUpperCase()}
                    </i>
                    <span>
                      <strong>{row.customerName}</strong>
                      <small>{row.licensePlate}</small>
                    </span>
                  </span>
                  <Badge status={row.status} />
                  <strong>${row.totalAmount?.toFixed(2)}</strong>
                </button>
              ))
            )}
          </div>
        </Card>
      </div>
    </>
  )
}

// -------------------------------------------------------------
// Screen: Customers
// -------------------------------------------------------------
function Customers({
  customers,
  loading,
  search,
  setSearch,
  onSelectCustomer,
  setPage,
  openSheet,
}: {
  customers: ICustomer[]
  loading: boolean
  search: string
  setSearch: (s: string) => void
  onSelectCustomer: (id: string) => void
  setPage: (p: Page) => void
  openSheet: (s: string) => void
}) {
  const [view, setView] = useState("Active")
  const filtered = useMemo(() => {
    return customers.filter((c) => {
      if (view === "Active") {
        return c.vehicleCount > 0 || c.loyaltyPoints > 0
      }
      return true
    })
  }, [customers, view])

  return (
    <>
      <PageHeader
        title="Customers"
        eyebrow={`02 · ${customers.length} customer profiles loaded`}
      >
        <Button icon="plus" onClick={() => openSheet("customer")}>
          New Customer
        </Button>
      </PageHeader>
      <div className="toolbar">
        <Search
          placeholder="Search name, phone, or email"
          value={search}
          onChange={setSearch}
        />
        <Segmented items={["Active", "All"]} active={view} onChange={setView} />
      </div>
      <Card className="table-card">
        {loading ? (
          <Skeleton />
        ) : filtered.length === 0 ? (
          <Empty
            icon="people"
            title="No customers match your search"
            text="Try clearing your search filters or create a new customer profile."
            actionText="New Customer"
            onAction={() => openSheet("customer")}
          />
        ) : (
          <div className="data-table customer-table">
            <div className="table-head">
              <span>Customer</span>
              <span>Phone</span>
              <span>Email</span>
              <span>Loyalty</span>
              <span>Vehicles</span>
              <span />
            </div>
            {filtered.map((r, i) => (
              <button
                className="table-row"
                key={r.id}
                onClick={() => {
                  onSelectCustomer(r.id)
                  setPage("customer")
                }}
              >
                <span className="customer-cell">
                  <i className={`avatar c${i % 4}`}>
                    {r.name
                      .split(" ")
                      .map((x) => x[0])
                      .join("")
                      .slice(0, 2)
                      .toUpperCase()}
                  </i>
                  <strong>{r.name}</strong>
                </span>
                <span>{r.phone}</span>
                <span>{r.email}</span>
                <span className="points">
                  <Icon name="star" size={14} filled />
                  {r.loyaltyPoints}
                </span>
                <span>{r.vehicleCount}</span>
                <Icon name="chevron" size={16} />
              </button>
            ))}
          </div>
        )}
        <div className="table-footer">
          <span>
            Showing {filtered.length} of {customers.length} customers
          </span>
        </div>
      </Card>
    </>
  )
}

// -------------------------------------------------------------
// Screen: CustomerDetail
// -------------------------------------------------------------
function CustomerDetail({
  customer,
  vehicles,
  history,
  loading,
  openSheet,
  setPage,
  onSelectInvoice,
}: {
  customer: ICustomer | null
  vehicles: IVehicle[]
  history: IInvoiceSummary[]
  loading: boolean
  openSheet: (s: string) => void
  setPage: (p: Page) => void
  onSelectInvoice: (id: string) => void
}) {
  const [tab, setTab] = useState("Vehicles")

  if (loading) {
    return (
      <>
        <button className="back-link" onClick={() => setPage("customers")}>
          <span>‹</span> Customers
        </button>
        <Skeleton />
      </>
    )
  }

  if (!customer) {
    return (
      <>
        <button className="back-link" onClick={() => setPage("customers")}>
          <span>‹</span> Customers
        </button>
        <Empty
          title="No customer selected"
          text="Please select a customer from the customer directory."
          actionText="Back to Customers"
          onAction={() => setPage("customers")}
        />
      </>
    )
  }

  const initials = customer.name
    .split(" ")
    .map((x) => x[0])
    .join("")
    .slice(0, 2)
    .toUpperCase()

  return (
    <>
      <button className="back-link" onClick={() => setPage("customers")}>
        <span>‹</span> Customers
      </button>
      <div className="detail-header">
        <span className="avatar xl">{initials}</span>
        <div>
          <p className="eyebrow">
            CUSTOMER SINCE {customer.createdAt ? customer.createdAt.slice(0, 4) : "2026"}
          </p>
          <h1>{customer.name}</h1>
          <p>
            {customer.email} · {customer.phone}
          </p>
        </div>
        <div className="loyalty">
          <span>
            <Icon name="star" filled /> {customer.loyaltyPoints} points
          </span>
          <Button tone="secondary" onClick={() => openSheet("redeem")}>
            Redeem points
          </Button>
        </div>
      </div>
      <div className="section-tabs">
        <Segmented
          items={["Vehicles", "Service History"]}
          active={tab}
          onChange={setTab}
        />
      </div>
      {tab === "Vehicles" ? (
        <>
          <div className="section-heading">
            <div>
              <h2>Registered vehicles</h2>
              <p>{vehicles.length} vehicle(s) attached to this customer</p>
            </div>
            <Button icon="plus" onClick={() => openSheet("vehicle")}>
              Register Vehicle
            </Button>
          </div>
          {vehicles.length === 0 ? (
            <Empty
              icon="car"
              title="No vehicles registered"
              text="Register a vehicle for this customer to start opening repair orders and service tasks."
              actionText="Register Vehicle"
              onAction={() => openSheet("vehicle")}
            />
          ) : (
            <div className="vehicle-grid">
              {vehicles.map((v) => (
                <VehicleCard
                  key={v.licensePlate}
                  type={v.type}
                  plate={v.licensePlate}
                  name={`${v.brand} ${v.model}`}
                  detail={
                    v.type === "CAR"
                      ? `${v.seatingCapacity ?? 5} seats · Base service $${v.baseServiceFee.toFixed(2)}`
                      : v.type === "VAN"
                        ? `${v.cargoVolume ?? 0} m³ cargo · Base service $${v.baseServiceFee.toFixed(2)}`
                        : `${v.engineCC ?? 0} CC · Base service $${v.baseServiceFee.toFixed(2)}`
                  }
                  onViewHistory={() => setTab("Service History")}
                />
              ))}
            </div>
          )}
          <h2 className="subsection-title">Vehicle type polymorphism</h2>
          <div className="vehicle-variants">
            <VehicleMini type="Car" detail="Seating capacity · $50.00 base" />
            <VehicleMini type="Van" detail="Cargo volume (m³) · $85.00 base" />
            <VehicleMini type="Motorcycle" detail="Engine CC · $30.00 base" />
          </div>
        </>
      ) : (
        <ServiceHistory
          history={history}
          onSelectInvoice={(id) => {
            onSelectInvoice(id)
            setPage("invoice")
          }}
        />
      )}
    </>
  )
}

function VehicleCard({
  type,
  plate,
  name,
  detail,
  onViewHistory,
}: {
  type: string
  plate: string
  name: string
  detail: string
  onViewHistory?: () => void
}) {
  return (
    <Card className="vehicle-card">
      <div className="vehicle-top">
        <span className="vehicle-icon">
          <Icon name="car" size={26} />
        </span>
      </div>
      <div>
        <Badge status={type.toUpperCase()} />
        <h2>{name}</h2>
        <span className="plate">{plate}</span>
        <p>{detail}</p>
      </div>
      <div className="card-action" onClick={onViewHistory} style={{ cursor: "pointer" }}>
        <span>View service history</span>
        <Icon name="chevron" size={16} />
      </div>
    </Card>
  )
}

function VehicleMini({ type, detail }: { type: string; detail: string }) {
  return (
    <Card className="vehicle-mini">
      <Icon name={type === "Motorcycle" ? "wrench" : "car"} />
      <div>
        <strong>{type}</strong>
        <small>{detail}</small>
      </div>
      <Icon name="check" />
    </Card>
  )
}

function ServiceHistory({
  history,
  onSelectInvoice,
}: {
  history: IInvoiceSummary[]
  onSelectInvoice: (id: string) => void
}) {
  if (history.length === 0) {
    return (
      <Empty
        icon="invoice"
        title="No service history"
        text="No previous invoices or service tasks found for this customer."
      />
    )
  }
  return (
    <div className="timeline">
      {history.map((r, i) => (
        <div className="timeline-item" key={r.invoiceId}>
          <i />
          <div className="timeline-date">
            {r.issuedAt ? r.issuedAt.slice(0, 10) : `Record #${i + 1}`}
          </div>
          <Card>
            <div
              className="history-head"
              onClick={() => onSelectInvoice(r.invoiceId)}
              style={{ cursor: "pointer" }}
            >
              <div>
                <strong>#{r.invoiceId.slice(0, 8)}</strong>
                <span>Vehicle: {r.licensePlate}</span>
              </div>
              <Badge status={r.status} />
              <strong>${r.totalAmount?.toFixed(2)}</strong>
            </div>
          </Card>
        </div>
      ))}
    </div>
  )
}

// -------------------------------------------------------------
// Screen: Employees
// -------------------------------------------------------------
function Employees({
  employees,
  loading,
  openSheet,
  onLogOvertime,
}: {
  employees: IEmployee[]
  loading: boolean
  openSheet: (s: string) => void
  onLogOvertime: (id: string, hours: number) => void
}) {
  const [filter, setFilter] = useState("All")
  const [specFilter, setSpecFilter] = useState("All")

  const specs = useMemo(() => {
    const set = new Set<string>()
    employees.forEach((e) => {
      if (e.specialization) set.add(e.specialization)
    })
    return ["All", ...Array.from(set)]
  }, [employees])

  const filtered = useMemo(() => {
    return employees.filter((e) => {
      if (filter === "Mechanics" && e.type !== "MECHANIC") return false
      if (filter === "Managers" && e.type !== "MANAGER") return false
      if (
        specFilter !== "All" &&
        e.specialization?.toLowerCase() !== specFilter.toLowerCase()
      )
        return false
      return true
    })
  }, [employees, filter, specFilter])

  return (
    <>
      <PageHeader
        title="Employees"
        eyebrow={`04 · ${employees.length} team members`}
      >
        <Button icon="plus" onClick={() => openSheet("employee")}>
          New Employee
        </Button>
      </PageHeader>
      <div className="toolbar">
        <Segmented
          items={["All", "Mechanics", "Managers"]}
          active={filter}
          onChange={setFilter}
        />
        {specs.length > 1 && (
          <div className="chips">
            {specs.map((s) => (
              <button
                key={s}
                className={specFilter === s ? "active" : ""}
                onClick={() => setSpecFilter(s)}
              >
                {s}
              </button>
            ))}
          </div>
        )}
      </div>
      {loading ? (
        <Skeleton />
      ) : filtered.length === 0 ? (
        <Empty
          icon="wrench"
          title="No employees found"
          text="Create a new mechanic or manager in the system."
          actionText="New Employee"
          onAction={() => openSheet("employee")}
        />
      ) : (
        <div className="employee-grid">
          {filtered.map((e, i) => (
            <Card className="employee-card" key={e.id}>
              <div className="employee-head">
                <span className={`avatar lg c${i % 4}`}>
                  {e.name
                    .split(" ")
                    .map((x) => x[0])
                    .join("")
                    .slice(0, 2)
                    .toUpperCase()}
                </span>
                <Badge status={e.type} />
              </div>
              <h2>{e.name}</h2>
              <div className="role-line">
                <span>
                  {e.specialization
                    ? `Specialization: ${e.specialization}`
                    : e.type === "MANAGER"
                      ? "Workshop Operations"
                      : "General Technician"}
                </span>
              </div>
              <div className="salary">
                <small>Computed monthly salary (OOP Polymorphism)</small>
                <strong>
                  ${e.monthlySalary != null ? e.monthlySalary.toFixed(2) : e.baseSalary.toFixed(2)}
                </strong>
                <span>
                  {e.type === "MECHANIC"
                    ? `${e.overtimeHours ?? 0} overtime hrs @ $${e.hourlyRate ?? 20}/hr`
                    : `Base $${e.baseSalary.toFixed(2)} + Bonus $${(e.monthlyBonus ?? 0).toFixed(2)}`}
                </span>
              </div>
              {e.type === "MECHANIC" ? (
                <button
                  type="button"
                  className="card-button"
                  onClick={() => onLogOvertime(e.id, 1)}
                  title="Logs 1.0 hour overtime in backend"
                >
                  <Icon name="clock" /> Log overtime <span>+1.0h</span>
                </button>
              ) : (
                <div
                  style={{
                    padding: "8px 12px",
                    background: "#f5f5f7",
                    borderRadius: 9,
                    fontSize: 12,
                    color: "var(--secondary)",
                    marginTop: 12,
                    display: "flex",
                    alignItems: "center",
                    gap: 6,
                  }}
                >
                  <Icon name="star" size={14} /> Manager Bonus: $
                  {(e.monthlyBonus ?? 0).toFixed(2)}
                </div>
              )}
            </Card>
          ))}
        </div>
      )}
    </>
  )
}

// -------------------------------------------------------------
// Screen: TaskBoard
// -------------------------------------------------------------
function TaskBoard({
  tasks,
  loading,
  openSheet,
  onStartTask,
  onCompleteTask,
  onSelectTask,
  toast,
}: {
  tasks: ITask[]
  loading: boolean
  openSheet: (s: string) => void
  onStartTask: (id: string) => void
  onCompleteTask: (id: string) => void
  onSelectTask: (id: string) => void
  toast: (msg: string) => void
}) {
  const [search, setSearch] = useState("")

  const filteredTasks = useMemo(() => {
    if (!search.trim()) return tasks
    const q = search.toLowerCase()
    return tasks.filter(
      (t) =>
        t.description.toLowerCase().includes(q) ||
        t.assignedMechanicName?.toLowerCase().includes(q) ||
        t.taskId.toLowerCase().includes(q),
    )
  }, [tasks, search])

  return (
    <>
      <PageHeader
        title="Task Board"
        eyebrow={`05 · ${tasks.length} workshop service jobs`}
      >
        <Button icon="plus" onClick={() => openSheet("newTask")}>
          New Task
        </Button>
      </PageHeader>
      <div className="toolbar">
        <Search
          placeholder="Search task description, mechanic, or task ID"
          value={search}
          onChange={setSearch}
        />
      </div>
      {loading ? (
        <Skeleton />
      ) : (
        <div className="kanban">
          {(["PENDING", "IN_PROGRESS", "COMPLETED"] as TaskStatus[]).map(
            (status) => {
              const colTasks = filteredTasks.filter((t) => t.status === status)
              return (
                <section className="kanban-column" key={status}>
                  <div className={`column-head ${status.toLowerCase()}`}>
                    <span />
                    <strong>{status.replace("_", " ")}</strong>
                    <i>{colTasks.length}</i>
                  </div>
                  <div className="column-content">
                    {colTasks.length === 0 ? (
                      <p
                        style={{
                          padding: "20px 10px",
                          textAlign: "center",
                          color: "var(--gray)",
                          fontSize: 12,
                        }}
                      >
                        No {status.toLowerCase().replace("_", " ")} tasks
                      </p>
                    ) : (
                      colTasks.map((task) => (
                        <Card className="task-card" key={task.taskId}>
                          <button
                            type="button"
                            className="task-main"
                            onClick={() => {
                              onSelectTask(task.taskId)
                              openSheet("taskDetail")
                            }}
                          >
                            <div className="task-title">
                              <strong>{task.description}</strong>
                              <Icon name="more" size={18} />
                            </div>
                            <span className="plate">
                              Task #{task.taskId.slice(0, 8)}
                            </span>
                            <div className="task-meta">
                              <span className="avatar xs">
                                {(task.assignedMechanicName || "M")
                                  .split(" ")
                                  .map((x) => x[0])
                                  .join("")
                                  .slice(0, 2)
                                  .toUpperCase()}
                              </span>
                              <span>{task.assignedMechanicName}</span>
                            </div>
                            <div className="task-numbers">
                              <span>
                                <small>PARTS</small>
                                <strong>{task.usedParts?.length ?? 0}</strong>
                              </span>
                              <span>
                                <small>LABOUR</small>
                                <strong>{task.billableHours ?? 0}h</strong>
                              </span>
                              <span>
                                <small>TOTAL</small>
                                <strong>
                                  ${task.totalTaskCost?.toFixed(2) ?? "0.00"}
                                </strong>
                              </span>
                            </div>
                          </button>
                          {status === "PENDING" && (
                            <button
                              type="button"
                              className="state-action"
                              onClick={() => onStartTask(task.taskId)}
                            >
                              Start Work <Icon name="arrow" size={16} />
                            </button>
                          )}
                          {status === "IN_PROGRESS" && (
                            <button
                              type="button"
                              className="state-action"
                              onClick={() => onCompleteTask(task.taskId)}
                            >
                              Complete Task <Icon name="check" size={16} />
                            </button>
                          )}
                          {status === "COMPLETED" && (
                            <div className="locked">
                              <Icon name="lock" size={14} /> Completed · locked
                            </div>
                          )}
                        </Card>
                      ))
                    )}
                  </div>
                </section>
              )
            },
          )}
        </div>
      )}
      <button
        type="button"
        className="demo-error"
        onClick={() =>
          toast(
            "Domain Invariant: Completed tasks cannot transition back to pending or in-progress.",
          )
        }
      >
        <Icon name="warning" /> Verify OOP state-machine guard
      </button>
    </>
  )
}

// -------------------------------------------------------------
// Screen: Inventory
// -------------------------------------------------------------
function Inventory({
  inventory,
  loading,
  openSheet,
  onSelectPart,
}: {
  inventory: IInventoryItem[]
  loading: boolean
  openSheet: (s: string) => void
  onSelectPart: (partCode: string) => void
}) {
  const [filter, setFilter] = useState("All")
  const [search, setSearch] = useState("")

  const filtered = useMemo(() => {
    return inventory.filter((item) => {
      if (filter === "Low stock" && !item.lowStock) return false
      if (search.trim()) {
        const q = search.toLowerCase()
        return (
          item.partCode.toLowerCase().includes(q) ||
          item.partName.toLowerCase().includes(q)
        )
      }
      return true
    })
  }, [inventory, filter, search])

  return (
    <>
      <PageHeader
        title="Inventory"
        eyebrow={`06 · ${inventory.length} parts in catalog`}
      >
        <Button
          tone="secondary"
          icon="receipt"
          onClick={() => openSheet("report")}
        >
          Inventory Report
        </Button>
        <Button icon="plus" onClick={() => openSheet("part")}>
          New Part
        </Button>
      </PageHeader>
      <div className="toolbar">
        <Search
          placeholder="Search code or part name"
          value={search}
          onChange={setSearch}
        />
        <Segmented
          items={["All", "Low stock"]}
          active={filter}
          onChange={setFilter}
        />
      </div>
      <Card className="table-card">
        {loading ? (
          <Skeleton />
        ) : filtered.length === 0 ? (
          <Empty
            icon="box"
            title="No inventory parts match"
            text="Add a new spare part to your garage catalog."
            actionText="New Part"
            onAction={() => openSheet("part")}
          />
        ) : (
          <div className="data-table inventory-table">
            <div className="table-head">
              <span>Part</span>
              <span>Unit price</span>
              <span>Stock</span>
              <span>Threshold</span>
              <span>Last restocked</span>
              <span>Status</span>
              <span />
            </div>
            {filtered.map((r) => (
              <div
                className={`table-row ${r.lowStock ? "low-row" : ""}`}
                key={r.partCode}
              >
                <span>
                  <small>{r.partCode}</small>
                  <strong>{r.partName}</strong>
                </span>
                <strong>${r.unitPrice?.toFixed(2)}</strong>
                <span>{r.stockQuantity} units</span>
                <span>{r.reorderThreshold} units</span>
                <span>
                  {r.lastRestockedAt
                    ? r.lastRestockedAt.slice(0, 10)
                    : "Initial Stock"}
                </span>
                <Badge status={r.lowStock ? "LOW STOCK" : "IN STOCK"} />
                <button
                  type="button"
                  className="restock"
                  onClick={() => {
                    onSelectPart(r.partCode)
                    openSheet("restock")
                  }}
                >
                  Restock
                </button>
              </div>
            ))}
          </div>
        )}
      </Card>
    </>
  )
}

// -------------------------------------------------------------
// Screen: Invoices
// -------------------------------------------------------------
function Invoices({
  invoices,
  loading,
  openSheet,
  setPage,
  onSelectInvoice,
}: {
  invoices: IInvoiceSummary[]
  loading: boolean
  openSheet: (s: string) => void
  setPage: (p: Page) => void
  onSelectInvoice: (id: string) => void
}) {
  const [filter, setFilter] = useState("All")
  const [search, setSearch] = useState("")

  const filtered = useMemo(() => {
    return invoices.filter((inv) => {
      if (filter !== "All" && inv.status !== filter) return false
      if (search.trim()) {
        const q = search.toLowerCase()
        return (
          inv.customerName?.toLowerCase().includes(q) ||
          inv.licensePlate?.toLowerCase().includes(q) ||
          inv.invoiceId.toLowerCase().includes(q)
        )
      }
      return true
    })
  }, [invoices, filter, search])

  return (
    <>
      <PageHeader
        title="Invoices"
        eyebrow={`07 · ${invoices.length} service orders`}
      >
        <Button icon="plus" onClick={() => openSheet("newInvoice")}>
          New Invoice
        </Button>
      </PageHeader>
      <div className="toolbar">
        <Search
          placeholder="Search invoice, customer, or plate"
          value={search}
          onChange={setSearch}
        />
        <Segmented
          items={["All", "OPEN", "ISSUED", "PAID"]}
          active={filter}
          onChange={setFilter}
        />
      </div>
      <Card className="table-card">
        {loading ? (
          <Skeleton />
        ) : filtered.length === 0 ? (
          <Empty
            icon="invoice"
            title="No invoices found"
            text="Open a new service invoice for a customer vehicle."
            actionText="New Invoice"
            onAction={() => openSheet("newInvoice")}
          />
        ) : (
          <div className="data-table invoice-table">
            <div className="table-head">
              <span>Invoice</span>
              <span>Customer</span>
              <span>Vehicle</span>
              <span>Status</span>
              <span>Total</span>
              <span>Issued</span>
              <span />
            </div>
            {filtered.map((r) => (
              <button
                type="button"
                className="table-row"
                key={r.invoiceId}
                onClick={() => {
                  onSelectInvoice(r.invoiceId)
                  setPage("invoice")
                }}
              >
                <strong>#{r.invoiceId.slice(0, 8)}</strong>
                <span>{r.customerName}</span>
                <span className="plate-cell">
                  <Icon name="car" size={17} />
                  {r.licensePlate}
                </span>
                <Badge status={r.status} />
                <strong>${r.totalAmount?.toFixed(2)}</strong>
                <span>{r.issuedAt ? r.issuedAt.slice(0, 10) : "Draft"}</span>
                <Icon name="chevron" size={16} />
              </button>
            ))}
          </div>
        )}
      </Card>
    </>
  )
}

// -------------------------------------------------------------
// Screen: InvoiceDetail
// -------------------------------------------------------------
function InvoiceDetail({
  invoice,
  loading,
  openSheet,
  setPage,
  onIssue,
  onSettle,
}: {
  invoice: IInvoice | null
  loading: boolean
  openSheet: (s: string) => void
  setPage: (p: Page) => void
  onIssue: (id: string) => void
  onSettle: (id: string) => void
}) {
  if (loading) {
    return (
      <>
        <button className="back-link" onClick={() => setPage("invoices")}>
          ‹ Invoices
        </button>
        <Skeleton />
      </>
    )
  }

  if (!invoice) {
    return (
      <>
        <button className="back-link" onClick={() => setPage("invoices")}>
          ‹ Invoices
        </button>
        <Empty
          title="No invoice selected"
          text="Select an invoice from the invoice list."
          actionText="Back to Invoices"
          onAction={() => setPage("invoices")}
        />
      </>
    )
  }

  const status = invoice.status
  const isPendingTasks =
    invoice.tasks &&
    invoice.tasks.some((t) => t.status === "PENDING" || t.status === "IN_PROGRESS")

  return (
    <>
      <button className="back-link" onClick={() => setPage("invoices")}>
        ‹ Invoices
      </button>
      <div className="invoice-header">
        <div>
          <p className="eyebrow">INVOICE #{invoice.invoiceId.slice(0, 8)}</p>
          <h1>{invoice.customerName}</h1>
          <p>
            <Icon name="car" size={17} /> {invoice.vehicleType}{" "}
            <span className="plate">{invoice.licensePlate}</span>
          </p>
        </div>
        <div className={`status-stepper ${status.toLowerCase()}`}>
          {["OPEN", "ISSUED", "PAID"].map((s, i) => {
            const isDone =
              status === "PAID" ||
              status === s ||
              (status === "ISSUED" && i < 2)
            return (
              <div className={isDone ? "done" : ""} key={s}>
                <span>
                  {status === "PAID" || (status === "ISSUED" && i === 0) ? (
                    <Icon name="check" size={14} />
                  ) : (
                    i + 1
                  )}
                </span>
                <small>{s}</small>
              </div>
            )
          })}
        </div>
      </div>

      {status === "PAID" && (
        <div className="paid-banner">
          <span>
            <Icon name="check" size={22} />
          </span>
          <div>
            <strong>Payment complete</strong>
            <p>
              Settled at{" "}
              {invoice.settledAt
                ? new Date(invoice.settledAt).toLocaleTimeString([], {
                    hour: "2-digit",
                    minute: "2-digit",
                  })
                : "Today"}
              . Customer awarded loyalty points for full settlement.
            </p>
          </div>
        </div>
      )}

      <div className="billing-grid">
        <div>
          <Card className="line-items">
            <div className="card-head">
              <div>
                <h2>Service details</h2>
                <p>
                  {status === "OPEN"
                    ? "Editable until this invoice is issued"
                    : "Totals frozen at issue time"}
                </p>
              </div>
              {status !== "OPEN" ? (
                <span className="lock-label">
                  <Icon name="lock" size={15} /> Locked
                </span>
              ) : (
                <Button
                  tone="secondary"
                  icon="plus"
                  onClick={() => openSheet("newTask")}
                >
                  Add Task
                </Button>
              )}
            </div>
            <div className="line-row base">
              <span className="line-icon">
                <Icon name="car" />
              </span>
              <div>
                <strong>Base service fee ({invoice.vehicleType})</strong>
                <small>Calculated polymorphically by Vehicle subclass</small>
              </div>
              <strong>${invoice.baseServiceFee?.toFixed(2)}</strong>
            </div>

            {invoice.tasks && invoice.tasks.length > 0 ? (
              invoice.tasks.map((task) => (
                <div className="task-group" key={task.taskId}>
                  <div className="line-row">
                    <span className="line-icon blue">
                      <Icon name="wrench" />
                    </span>
                    <div>
                      <strong>{task.description}</strong>
                      <small>Assigned to {task.assignedMechanicName}</small>
                    </div>
                    <Badge status={task.status} />
                    <strong>${task.totalTaskCost?.toFixed(2)}</strong>
                  </div>
                  {task.usedParts &&
                    task.usedParts.map((part) => (
                      <div className="subline" key={part.partCode}>
                        <span>{part.partName}</span>
                        <span>
                          {part.quantity} × ${part.unitPrice?.toFixed(2)}
                        </span>
                        <strong>${part.subtotal?.toFixed(2)}</strong>
                      </div>
                    ))}
                  <div className="subline">
                    <span>Labour Charge</span>
                    <span>{task.billableHours ?? 0} hr</span>
                    <strong>${task.laborCharge?.toFixed(2)}</strong>
                  </div>
                </div>
              ))
            ) : (
              <p
                style={{
                  padding: "16px 20px",
                  color: "var(--secondary)",
                  fontSize: 13,
                }}
              >
                No service tasks added yet. Add a task to calculate labor and
                parts.
              </p>
            )}

            <div className="line-subtotal">
              <span>Service subtotal</span>
              <strong>${invoice.subtotal?.toFixed(2)}</strong>
            </div>
          </Card>
        </div>

        <Card className="totals-card">
          <div className="card-head">
            <h2>Totals</h2>
          </div>
          <div className="total-lines">
            <span>
              <label>Subtotal</label>
              <strong>${invoice.subtotal?.toFixed(2)}</strong>
            </span>
            <button
              type="button"
              disabled={status !== "OPEN"}
              onClick={() => openSheet("discount")}
              title={
                status === "OPEN" ? "Click to apply discount" : "Locked invoice"
              }
            >
              <label>
                Discount{" "}
                {invoice.discountAmount > 0 && (
                  <small>-${invoice.discountAmount.toFixed(2)}</small>
                )}
              </label>
              <strong className="discount">
                −${invoice.discountAmount?.toFixed(2) ?? "0.00"}
              </strong>
            </button>
            <span>
              <label>Taxable amount</label>
              <strong>
                $
                {Math.max(
                  0,
                  (invoice.subtotal || 0) - (invoice.discountAmount || 0),
                ).toFixed(2)}
              </strong>
            </span>
            <button
              type="button"
              disabled={status !== "OPEN"}
              onClick={() => openSheet("tax")}
              title={
                status === "OPEN" ? "Click to edit tax rate" : "Locked invoice"
              }
            >
              <label>
                Tax <small>{invoice.taxRate ?? 8}%</small>
              </label>
              <strong>${invoice.taxAmount?.toFixed(2)}</strong>
            </button>
          </div>
          <div className="grand-total">
            <span>Total</span>
            <strong>${invoice.totalAmount?.toFixed(2)}</strong>
            <small>USD · Tax included</small>
          </div>
          <div className="invoice-note">
            <Icon name={status === "OPEN" ? "clock" : "lock"} size={16} />
            <p>
              {status === "OPEN"
                ? "Amounts update as tasks and adjustments change."
                : "This invoice is frozen and locked from modification."}
            </p>
          </div>
        </Card>
      </div>

      <div className="action-bar">
        <div>
          <strong>
            {status === "OPEN" ? "Ready to issue" : "Invoice total"}
          </strong>
          <span>
            {status === "OPEN"
              ? isPendingTasks
                ? "Pending tasks must be completed before issue"
                : "All tasks ready"
              : `$${invoice.totalAmount?.toFixed(2)}`}
          </span>
        </div>
        {status === "OPEN" ? (
          <Button
            onClick={() => onIssue(invoice.invoiceId)}
            disabled={isPendingTasks}
            title={
              isPendingTasks
                ? "Complete all tasks before issuing"
                : "Freeze and issue invoice"
            }
          >
            Issue Invoice
          </Button>
        ) : status === "ISSUED" ? (
          <>
            <Button tone="secondary" onClick={() => setPage("receipt")}>
              View Receipt
            </Button>
            <Button onClick={() => onSettle(invoice.invoiceId)}>
              Settle & Take Payment
            </Button>
          </>
        ) : (
          <Button onClick={() => setPage("receipt")}>View Receipt</Button>
        )}
      </div>
    </>
  )
}

// -------------------------------------------------------------
// Screen: Receipt
// -------------------------------------------------------------
function Receipt({
  invoice,
  receiptText,
  setPage,
}: {
  invoice: IInvoice | null
  receiptText: string
  setPage: (p: Page) => void
}) {
  const handlePrint = () => {
    window.print()
  }

  const handleDownload = () => {
    const blob = new Blob([receiptText || "AutoCare Receipt"], {
      type: "text/plain",
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = `receipt-${invoice?.invoiceId?.slice(0, 8) || "autocare"}.txt`
    a.click()
    URL.revokeObjectURL(url)
  }

  return (
    <>
      <PageHeader title="Receipt" eyebrow="09 · Payment confirmation">
        <Button
          tone="secondary"
          icon="arrow"
          onClick={() => setPage("invoice")}
        >
          Back to invoice
        </Button>
        <Button tone="secondary" icon="download" onClick={handleDownload}>
          Download .txt
        </Button>
        <Button icon="print" onClick={handlePrint}>
          Print
        </Button>
      </PageHeader>
      <div className="receipt-stage">
        <article className="thermal">
          <div className="receipt-logo">
            <span>
              <Icon name="wrench" />
            </span>
            <h2>AUTOCARE</h2>
            <p>Garage & Service Studio</p>
          </div>
          <div className="receipt-meta">
            <span>RECEIPT</span>
            <strong>#{invoice?.invoiceId?.slice(0, 8) || "AC-1001"}</strong>
            <p>Customer: {invoice?.customerName || "Customer"}</p>
            <p>
              Vehicle: {invoice?.vehicleType || "CAR"} ·{" "}
              {invoice?.licensePlate || "PLATE"}
            </p>
            <p>
              Date:{" "}
              {invoice?.settledAt
                ? new Date(invoice.settledAt).toLocaleDateString()
                : new Date().toLocaleDateString()}
            </p>
          </div>
          <div className="receipt-rule" />
          <div className="receipt-lines">
            <span>
              <label>BASE SERVICE ({invoice?.vehicleType || "CAR"})</label>
              <b>${invoice?.baseServiceFee?.toFixed(2) || "50.00"}</b>
            </span>
            {invoice?.tasks?.map((t) => (
              <div key={t.taskId}>
                <span>
                  <label>{t.description.toUpperCase()}</label>
                  <b>${t.totalTaskCost?.toFixed(2)}</b>
                </span>
                {t.usedParts?.map((p) => (
                  <span className="indented" key={p.partCode}>
                    <label>
                      {p.partName} × {p.quantity}
                    </label>
                    <b>${p.subtotal?.toFixed(2)}</b>
                  </span>
                ))}
                {t.billableHours > 0 && (
                  <span className="indented">
                    <label>Labour · {t.billableHours} hr</label>
                    <b>${t.laborCharge?.toFixed(2)}</b>
                  </span>
                )}
              </div>
            ))}
          </div>
          <div className="receipt-rule" />
          <div className="receipt-totals">
            <span>
              <label>SUBTOTAL</label>
              <b>${invoice?.subtotal?.toFixed(2) || "0.00"}</b>
            </span>
            {invoice && invoice.discountAmount > 0 && (
              <span>
                <label>DISCOUNT</label>
                <b>−${invoice.discountAmount?.toFixed(2)}</b>
              </span>
            )}
            <span>
              <label>TAX ({invoice?.taxRate ?? 8}%)</label>
              <b>${invoice?.taxAmount?.toFixed(2) || "0.00"}</b>
            </span>
            <span className="receipt-grand">
              <label>TOTAL</label>
              <b>${invoice?.totalAmount?.toFixed(2) || "0.00"}</b>
            </span>
          </div>
          {invoice?.status === "PAID" && <div className="paid-stamp">PAID</div>}
          <footer>
            <p>Payment Status: {invoice?.status}</p>
            <strong>Thank you for choosing AutoCare.</strong>
          </footer>
        </article>
      </div>
    </>
  )
}

// -------------------------------------------------------------
// Screen: States & System Health
// -------------------------------------------------------------
function States({
  backendStatus,
  vehicleTypes,
}: {
  backendStatus: { connected: boolean; message: string; pingMs?: number }
  vehicleTypes: { type: string; baseServiceFee: number }[]
}) {
  return (
    <>
      <PageHeader
        title="States & System Health"
        eyebrow="10 · Live Backend Connectivity & Diagnostic States"
      />
      <div className="states-grid">
        <Card>
          <div className="state-label">LIVE BACKEND CONNECTION</div>
          <div style={{ padding: 20 }}>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 10,
                marginBottom: 12,
              }}
            >
              <Badge
                status={backendStatus.connected ? "CONNECTED" : "LOW STOCK"}
              />
              <strong>
                {backendStatus.connected
                  ? "Spring Boot 3.x Backend Online"
                  : "Backend Offline"}
              </strong>
            </div>
            <p
              style={{
                fontSize: 13,
                color: "var(--secondary)",
                marginBottom: 14,
              }}
            >
              {backendStatus.message}
            </p>
            {backendStatus.pingMs && (
              <p style={{ fontSize: 12, color: "var(--gray)" }}>
                Latency: {backendStatus.pingMs} ms · Supabase PostgreSQL active
              </p>
            )}
            <div style={{ marginTop: 14 }}>
              <small
                style={{
                  fontWeight: 600,
                  fontSize: 11,
                  color: "var(--secondary)",
                }}
              >
                POLYMORPHIC VEHICLE BASE FEES:
              </small>
              <ul
                style={{
                  margin: "8px 0 0",
                  paddingLeft: 18,
                  fontSize: 13,
                  lineHeight: 1.6,
                }}
              >
                {vehicleTypes.map((vt) => (
                  <li key={vt.type}>
                    <strong>{vt.type}:</strong> ${vt.baseServiceFee.toFixed(2)}
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </Card>
        <Card>
          <div className="state-label">LOADING · SKELETON</div>
          <Skeleton />
        </Card>
        <Card>
          <div className="state-label">EMPTY · NO RESULTS</div>
          <Empty
            icon="search"
            title="No records found"
            text="Try adjusting your search or filters."
          />
        </Card>
        <Card>
          <div className="state-label">400 · VALIDATION ERRORS</div>
          <ErrorBanner
            title="Validation Failure"
            text="name: Required · email: Invalid format · phone: Required"
          />
        </Card>
        <Card>
          <div className="state-label">409 · DOMAIN CONFLICT</div>
          <ErrorBanner
            title="Business Invariant Conflict"
            text="Stock deduction failed: Quantity requested exceeds stock available."
          />
        </Card>
        <Card>
          <div className="state-label">422 · BUSINESS RULE ENFORCEMENT</div>
          <ErrorBanner
            title="Discount Approval Required"
            text="Discounts exceeding 10% require authorization by a workshop Manager."
          />
        </Card>
      </div>
    </>
  )
}

// -------------------------------------------------------------
// Screen: PrototypeFlow
// -------------------------------------------------------------
function PrototypeFlow({ setPage }: { setPage: (p: Page) => void }) {
  const steps: [string, string, Page, IconName, string][] = [
    [
      "1",
      "Find customer",
      "customers",
      "people",
      "Search or create customer profile",
    ],
    [
      "2",
      "Register customer vehicle",
      "customer",
      "car",
      "Attach polymorphic Car, Van, or Bike",
    ],
    [
      "3",
      "Open service invoice",
      "invoices",
      "invoice",
      "Link customer & vehicle into invoice",
    ],
    [
      "4",
      "Add workshop tasks",
      "tasks",
      "tasks",
      "Assign task card to qualified mechanic",
    ],
    [
      "5",
      "Log labor & parts",
      "tasks",
      "wrench",
      "Deduct stock and compute billable charges",
    ],
    [
      "6",
      "Complete task",
      "tasks",
      "check",
      "Move PENDING -> IN_PROGRESS -> COMPLETED",
    ],
    [
      "7",
      "Issue & Settle",
      "invoice",
      "receipt",
      "Freeze totals and settle with loyalty points",
    ],
    [
      "8",
      "Print Receipt",
      "receipt",
      "print",
      "Thermal ticket with itemized breakdown",
    ],
  ]
  return (
    <>
      <PageHeader
        title="Workshop Lifecycle Flow"
        eyebrow="11 · End-to-End Garage Workflow"
      >
        <Button onClick={() => setPage("customers")} icon="arrow">
          Start customer journey
        </Button>
      </PageHeader>
      <Card className="flow-intro">
        <div>
          <span className="flow-icon">
            <Icon name="flow" size={30} />
          </span>
          <div>
            <h2>Customer to itemized receipt</h2>
            <p>
              Live full-stack workflow connecting Java 21 Spring Boot + Supabase
              Postgres. Select any node to jump straight to that stage.
            </p>
          </div>
        </div>
        <span>8 stages · OOP Architecture</span>
      </Card>
      <div className="flow-map">
        {steps.map((s, i) => (
          <div className="flow-node-wrap" key={s[0]}>
            <button
              type="button"
              className="flow-node"
              onClick={() => setPage(s[2])}
            >
              <span>
                <Icon name={s[3]} />
              </span>
              <small>STEP {s[0]}</small>
              <strong>{s[1]}</strong>
              <p>{s[4]}</p>
              <i>
                Open stage <Icon name="arrow" size={14} />
              </i>
            </button>
            {i < steps.length - 1 && (
              <div className="flow-connector">
                <Icon name="chevron" size={16} />
              </div>
            )}
          </div>
        ))}
      </div>
    </>
  )
}

// -------------------------------------------------------------
// Sheet Modals Container
// -------------------------------------------------------------
function SheetContent({
  sheet,
  close,
  setPage,
  toast,
  refreshAll,
  selectedCustomerId,
  selectedInvoiceId,
  selectedTaskId,
  selectedPartCode,
  customers,
  vehicles,
  employees,
  inventory,
  invoices,
  setSelectedInvoiceId,
  setSelectedCustomerId,
}: {
  sheet: string
  close: () => void
  setPage: (p: Page) => void
  toast: (msg: string) => void
  refreshAll: () => Promise<void>
  selectedCustomerId: string | null
  selectedInvoiceId: string | null
  selectedTaskId: string | null
  selectedPartCode: string | null
  customers: ICustomer[]
  vehicles: IVehicle[]
  employees: IEmployee[]
  inventory: IInventoryItem[]
  invoices: IInvoiceSummary[]
  setSelectedInvoiceId: (id: string) => void
  setSelectedCustomerId: (id: string) => void
}) {
  const [errorMsg, setErrorMsg] = useState("")
  const [loading, setLoading] = useState(false)

  // 1. New Customer Modal
  if (sheet === "customer") {
    const handleSaveCustomer = async (e: React.FormEvent<HTMLFormElement>) => {
      e.preventDefault()
      setErrorMsg("")
      const form = e.currentTarget
      const name = (form.elements.namedItem("name") as HTMLInputElement).value
      const phone = (form.elements.namedItem("phone") as HTMLInputElement).value
      const email = (form.elements.namedItem("email") as HTMLInputElement).value

      if (!name || !phone || !email) {
        setErrorMsg("Please provide name, phone, and email.")
        return
      }

      setLoading(true)
      try {
        const created = await customersApi.create({ name, phone, email })
        toast(`Customer ${created.name} created successfully!`)
        await refreshAll()
        setSelectedCustomerId(created.id)
        close()
        setPage("customer")
      } catch (err) {
        const msg =
          err instanceof ApiErrorResponse
            ? err.error.message || err.message
            : String(err)
        setErrorMsg(msg)
      } finally {
        setLoading(false)
      }
    }

    return (
      <Sheet title="New Customer" onClose={close}>
        <form onSubmit={handleSaveCustomer} className="sheet-body">
          <div className="form-grid">
            <Field label="Full name" name="name" placeholder="e.g. Olivia Martin" />
            <Field label="Phone" name="phone" placeholder="e.g. 0771234567" />
            <Field label="Email" name="email" placeholder="e.g. olivia@example.com" />
          </div>
          {errorMsg && <ErrorBanner text={errorMsg} />}
          <div className="sheet-footer">
            <Button tone="secondary" onClick={close}>
              Cancel
            </Button>
            <Button type="submit" disabled={loading}>
              {loading ? "Saving..." : "Save Customer"}
            </Button>
          </div>
        </form>
      </Sheet>
    )
  }

  // 2. Redeem Points Modal
  if (sheet === "redeem") {
    const customer = customers.find((c) => c.id === selectedCustomerId)
    const handleRedeem = async (e: React.FormEvent<HTMLFormElement>) => {
      e.preventDefault()
      if (!customer) return
      setErrorMsg("")
      const form = e.currentTarget
      const points = parseInt(
        (form.elements.namedItem("points") as HTMLInputElement).value,
        10,
      )

      if (isNaN(points) || points <= 0) {
        setErrorMsg("Please enter a positive points number.")
        return
      }

      setLoading(true)
      try {
        const res = await customersApi.redeemLoyalty(customer.id, points)
        toast(
          `Redeemed ${points} points! Remaining: ${res.remainingPoints} points.`,
        )
        await refreshAll()
        close()
      } catch (err) {
        const msg =
          err instanceof ApiErrorResponse
            ? err.error.message || err.message
            : String(err)
        setErrorMsg(msg)
      } finally {
        setLoading(false)
      }
    }

    return (
      <Sheet title="Redeem Loyalty Points" onClose={close}>
        <form onSubmit={handleRedeem} className="sheet-body">
          <div className="points-hero">
            <Icon name="star" filled />
            <strong>{customer?.loyaltyPoints ?? 0}</strong>
            <span>available points</span>
          </div>
          <Field
            label="Points to redeem"
            name="points"
            type="number"
            defaultValue="50"
          />
          <p className="form-note">
            100 points = $1.00 credit towards workshop services.
          </p>
          {errorMsg && <ErrorBanner text={errorMsg} />}
          <div className="sheet-footer">
            <Button tone="secondary" onClick={close}>
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={loading || (customer?.loyaltyPoints ?? 0) <= 0}
            >
              {loading ? "Redeeming..." : "Redeem Points"}
            </Button>
          </div>
        </form>
      </Sheet>
    )
  }

  // 3. Register Vehicle Modal
  if (sheet === "vehicle") {
    const [vType, setVType] = useState<VehicleType>("CAR")
    const customer = customers.find((c) => c.id === selectedCustomerId)

    const handleRegisterVehicle = async (
      e: React.FormEvent<HTMLFormElement>,
    ) => {
      e.preventDefault()
      if (!customer) {
        setErrorMsg("No customer selected to attach vehicle to.")
        return
      }
      setErrorMsg("")
      const form = e.currentTarget
      const licensePlate = (
        form.elements.namedItem("licensePlate") as HTMLInputElement
      ).value
      const brand = (form.elements.namedItem("brand") as HTMLInputElement).value
      const model = (form.elements.namedItem("model") as HTMLInputElement).value

      let seatingCapacity: number | undefined
      let cargoVolume: number | undefined
      let engineCC: number | undefined

      if (vType === "CAR") {
        seatingCapacity = parseInt(
          (form.elements.namedItem("seatingCapacity") as HTMLInputElement)
            ?.value || "5",
          10,
        )
      } else if (vType === "VAN") {
        cargoVolume = parseFloat(
          (form.elements.namedItem("cargoVolume") as HTMLInputElement)
            ?.value || "10",
        )
      } else {
        engineCC = parseInt(
          (form.elements.namedItem("engineCC") as HTMLInputElement)?.value ||
            "250",
          10,
        )
      }

      setLoading(true)
      try {
        await customersApi.registerVehicle(customer.id, {
          type: vType,
          licensePlate,
          brand,
          model,
          seatingCapacity,
          cargoVolume,
          engineCC,
        })
        toast(`Vehicle ${licensePlate} registered for ${customer.name}!`)
        await refreshAll()
        close()
      } catch (err) {
        const msg =
          err instanceof ApiErrorResponse
            ? err.error.message || err.message
            : String(err)
        setErrorMsg(msg)
      } finally {
        setLoading(false)
      }
    }

    return (
      <Sheet title="Register Vehicle" onClose={close}>
        <form onSubmit={handleRegisterVehicle} className="sheet-body">
          <Segmented
            items={["CAR", "VAN", "MOTORCYCLE"]}
            active={vType}
            onChange={(val) => setVType(val as VehicleType)}
          />
          <div className="form-grid two" style={{ marginTop: 14 }}>
            <Field label="License plate" name="licensePlate" placeholder="e.g. 8ABC123" />
            <Field label="Brand" name="brand" placeholder="e.g. Audi" />
            <Field label="Model" name="model" placeholder="e.g. A4" />
            {vType === "CAR" && (
              <Field
                label="Seating capacity"
                name="seatingCapacity"
                type="number"
                defaultValue="5"
              />
            )}
            {vType === "VAN" && (
              <Field
                label="Cargo volume (m³)"
                name="cargoVolume"
                type="number"
                step="0.1"
                defaultValue="8.5"
              />
            )}
            {vType === "MOTORCYCLE" && (
              <Field
                label="Engine CC"
                name="engineCC"
                type="number"
                defaultValue="660"
              />
            )}
          </div>
          <div className="variant-hint">
            <Icon name="car" />
            <p>
              <strong>OOP Subclass Polymorphism</strong>
              <br />
              Base fees vary: CAR ($50.00), VAN ($85.00), MOTORCYCLE ($30.00).
            </p>
          </div>
          {errorMsg && <ErrorBanner text={errorMsg} />}
          <div className="sheet-footer">
            <Button tone="secondary" onClick={close}>
              Cancel
            </Button>
            <Button type="submit" disabled={loading}>
              {loading ? "Registering..." : "Register Vehicle"}
            </Button>
          </div>
        </form>
      </Sheet>
    )
  }

  // 4. New Employee Modal
  if (sheet === "employee") {
    const [eType, setEType] = useState<EmployeeType>("MECHANIC")

    const handleCreateEmployee = async (
      e: React.FormEvent<HTMLFormElement>,
    ) => {
      e.preventDefault()
      setErrorMsg("")
      const form = e.currentTarget
      const name = (form.elements.namedItem("name") as HTMLInputElement).value
      const phone = (form.elements.namedItem("phone") as HTMLInputElement).value
      const email = (form.elements.namedItem("email") as HTMLInputElement).value
      const baseSalary = parseFloat(
        (form.elements.namedItem("baseSalary") as HTMLInputElement).value ||
          "1500",
      )

      let specialization: string | undefined
      let hourlyRate: number | undefined
      let monthlyBonus: number | undefined

      if (eType === "MECHANIC") {
        specialization = (
          form.elements.namedItem("specialization") as HTMLInputElement
        )?.value
        hourlyRate = parseFloat(
          (form.elements.namedItem("hourlyRate") as HTMLInputElement)?.value ||
            "20",
        )
      } else {
        monthlyBonus = parseFloat(
          (form.elements.namedItem("monthlyBonus") as HTMLInputElement)
            ?.value || "300",
        )
      }

      setLoading(true)
      try {
        const created = await employeesApi.create({
          type: eType,
          name,
          phone,
          email,
          baseSalary,
          specialization,
          hourlyRate,
          monthlyBonus,
        })
        toast(`${created.type} ${created.name} added to team!`)
        await refreshAll()
        close()
      } catch (err) {
        const msg =
          err instanceof ApiErrorResponse
            ? err.error.message || err.message
            : String(err)
        setErrorMsg(msg)
      } finally {
        setLoading(false)
      }
    }

    return (
      <Sheet title="New Employee" onClose={close}>
        <form onSubmit={handleCreateEmployee} className="sheet-body">
          <Segmented
            items={["MECHANIC", "MANAGER"]}
            active={eType}
            onChange={(val) => setEType(val as EmployeeType)}
          />
          <div className="form-grid two" style={{ marginTop: 14 }}>
            <Field label="Full name" name="name" placeholder="Employee Name" />
            <Field label="Phone" name="phone" placeholder="077xxxxxxx" />
            <Field label="Email" name="email" placeholder="employee@garage.com" />
            <Field
              label="Base salary ($)"
              name="baseSalary"
              type="number"
              defaultValue="1500"
            />
            {eType === "MECHANIC" ? (
              <>
                <Field
                  label="Specialization"
                  name="specialization"
                  placeholder="Engine / Brakes / Electrical"
                  defaultValue="Engine"
                />
                <Field
                  label="Overtime hourly rate ($)"
                  name="hourlyRate"
                  type="number"
                  defaultValue="25"
                />
              </>
            ) : (
              <Field
                label="Monthly bonus ($)"
                name="monthlyBonus"
                type="number"
                defaultValue="400"
              />
            )}
          </div>
          {errorMsg && <ErrorBanner text={errorMsg} />}
          <div className="sheet-footer">
            <Button tone="secondary" onClick={close}>
              Cancel
            </Button>
            <Button type="submit" disabled={loading}>
              {loading ? "Creating..." : "Create Employee"}
            </Button>
          </div>
        </form>
      </Sheet>
    )
  }

  // 5. New Task Modal
  if (sheet === "newTask") {
    const openInvoices = invoices.filter((i) => i.status === "OPEN")
    const mechanics = employees.filter((e) => e.type === "MECHANIC")

    const handleCreateTask = async (e: React.FormEvent<HTMLFormElement>) => {
      e.preventDefault()
      setErrorMsg("")
      const form = e.currentTarget
      const invoiceId = (
        form.elements.namedItem("invoiceId") as HTMLSelectElement
      ).value
      const description = (
        form.elements.namedItem("description") as HTMLTextAreaElement
      ).value
      const assignedMechanicId = (
        form.elements.namedItem("assignedMechanicId") as HTMLSelectElement
      ).value

      if (!invoiceId) {
        setErrorMsg("Please select an open invoice to attach this task to.")
        return
      }
      if (!description.trim()) {
        setErrorMsg("Please provide a task description.")
        return
      }
      if (!assignedMechanicId) {
        setErrorMsg("Please select an assigned mechanic.")
        return
      }

      setLoading(true)
      try {
        await tasksApi.create(invoiceId, { description, assignedMechanicId })
        toast("New service task created and assigned!")
        await refreshAll()
        close()
        setPage("tasks")
      } catch (err) {
        const msg =
          err instanceof ApiErrorResponse
            ? err.error.message || err.message
            : String(err)
        setErrorMsg(msg)
      } finally {
        setLoading(false)
      }
    }

    return (
      <Sheet title="New Workshop Task" onClose={close}>
        <form onSubmit={handleCreateTask} className="sheet-body">
          <label className="field">
            <span>Target Open Invoice</span>
            <select
              name="invoiceId"
              defaultValue={selectedInvoiceId || openInvoices[0]?.invoiceId || ""}
              style={{
                minHeight: 44,
                padding: "0 12px",
                borderRadius: 10,
                border: "1px solid #d5d5da",
              }}
            >
              {openInvoices.map((inv) => (
                <option key={inv.invoiceId} value={inv.invoiceId}>
                  #{inv.invoiceId.slice(0, 8)} · {inv.customerName} (
                  {inv.licensePlate})
                </option>
              ))}
            </select>
          </label>
          <label className="field" style={{ marginTop: 14 }}>
            <span>Task Description</span>
            <textarea
              name="description"
              defaultValue="Brake pad replacement and rotor check"
              style={{ minHeight: 80, padding: 12, borderRadius: 10 }}
            />
          </label>
          <label className="field" style={{ marginTop: 14 }}>
            <span>Assign Qualified Mechanic</span>
            <select
              name="assignedMechanicId"
              defaultValue={mechanics[0]?.id || ""}
              style={{
                minHeight: 44,
                padding: "0 12px",
                borderRadius: 10,
                border: "1px solid #d5d5da",
              }}
            >
              {mechanics.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.name} ({m.specialization || "General"})
                </option>
              ))}
            </select>
          </label>
          {errorMsg && <ErrorBanner text={errorMsg} />}
          <div className="sheet-footer">
            <Button tone="secondary" onClick={close}>
              Cancel
            </Button>
            <Button type="submit" disabled={loading || openInvoices.length === 0}>
              {loading ? "Creating..." : "Create Task"}
            </Button>
          </div>
        </form>
      </Sheet>
    )
  }

  // 6. Task Detail Modal (Add part, diagnostics, labor)
  if (sheet === "taskDetail") {
    const [selectedTask, setSelectedTask] = useState<ITask | null>(null)
    const [taskLoading, setTaskLoading] = useState(true)

    const loadTask = async () => {
      if (!selectedTaskId) return
      try {
        const t = await tasksApi.get(selectedTaskId)
        setSelectedTask(t)
      } catch (err) {
        console.error(err)
      } finally {
        setTaskLoading(false)
      }
    }

    useEffect(() => {
      loadTask()
    }, [selectedTaskId])

    const handleSaveDiagnostics = async (
      e: React.FormEvent<HTMLFormElement>,
    ) => {
      e.preventDefault()
      if (!selectedTaskId) return
      setErrorMsg("")
      const desc = (
        e.currentTarget.elements.namedItem("diag") as HTMLTextAreaElement
      ).value
      setLoading(true)
      try {
        await tasksApi.updateDiagnostics(selectedTaskId, desc)
        toast("Diagnostics updated!")
        await loadTask()
        await refreshAll()
      } catch (err) {
        const msg =
          err instanceof ApiErrorResponse
            ? err.error.message || err.message
            : String(err)
        setErrorMsg(msg)
      } finally {
        setLoading(false)
      }
    }

    const handleAddPart = async (e: React.FormEvent<HTMLFormElement>) => {
      e.preventDefault()
      if (!selectedTaskId) return
      setErrorMsg("")
      const form = e.currentTarget
      const partCode = (
        form.elements.namedItem("partCode") as HTMLSelectElement
      ).value
      const quantity = parseInt(
        (form.elements.namedItem("quantity") as HTMLInputElement).value || "1",
        10,
      )

      setLoading(true)
      try {
        await tasksApi.addPart(selectedTaskId, { partCode, quantity })
        toast("Part deducted from inventory and added to task!")
        await loadTask()
        await refreshAll()
      } catch (err) {
        const msg =
          err instanceof ApiErrorResponse
            ? err.error.message || err.message
            : String(err)
        setErrorMsg(msg)
      } finally {
        setLoading(false)
      }
    }

    const handleRecordLabor = async (e: React.FormEvent<HTMLFormElement>) => {
      e.preventDefault()
      if (!selectedTaskId) return
      setErrorMsg("")
      const form = e.currentTarget
      const billableHours = parseFloat(
        (form.elements.namedItem("hours") as HTMLInputElement).value || "1",
      )

      setLoading(true)
      try {
        await tasksApi.recordLabor(selectedTaskId, billableHours)
        toast("Labour hours recorded!")
        await loadTask()
        await refreshAll()
      } catch (err) {
        const msg =
          err instanceof ApiErrorResponse
            ? err.error.message || err.message
            : String(err)
        setErrorMsg(msg)
      } finally {
        setLoading(false)
      }
    }

    return (
      <Sheet title="Task Details & Consumption" onClose={close} wide>
        {taskLoading || !selectedTask ? (
          <Skeleton />
        ) : (
          <div className="sheet-body task-detail">
            <div className="detail-fields">
              <form onSubmit={handleSaveDiagnostics}>
                <label className="field">
                  <span>Diagnostic Findings</span>
                  <textarea
                    name="diag"
                    defaultValue={selectedTask.description}
                    disabled={selectedTask.status === "COMPLETED"}
                  />
                </label>
                {selectedTask.status !== "COMPLETED" && (
                  <Button
                    type="submit"
                    tone="secondary"
                    disabled={loading}
                    style={{ marginTop: 8 }}
                  >
                    Save Findings
                  </Button>
                )}
              </form>

              <div className="parts-box">
                <div className="parts-head">
                  <strong>Used Parts</strong>
                  <Badge status={selectedTask.status} />
                </div>
                {selectedTask.usedParts && selectedTask.usedParts.length > 0 ? (
                  selectedTask.usedParts.map((p) => (
                    <div className="part-row" key={p.partCode}>
                      <span>
                        {p.partName}
                        <small>Code: {p.partCode}</small>
                      </span>
                      <span>Qty: {p.quantity}</span>
                      <span>${p.unitPrice?.toFixed(2)}</span>
                      <strong>${p.subtotal?.toFixed(2)}</strong>
                    </div>
                  ))
                ) : (
                  <p
                    style={{
                      fontSize: 12,
                      color: "var(--secondary)",
                      marginTop: 8,
                    }}
                  >
                    No spare parts added to this task yet.
                  </p>
                )}

                {selectedTask.status !== "COMPLETED" && (
                  <form
                    onSubmit={handleAddPart}
                    style={{
                      display: "flex",
                      gap: 8,
                      alignItems: "center",
                      marginTop: 12,
                      paddingTop: 12,
                      borderTop: "1px solid var(--separator)",
                    }}
                  >
                    <select
                      name="partCode"
                      style={{
                        flex: 1,
                        minHeight: 38,
                        borderRadius: 8,
                        padding: "0 8px",
                        border: "1px solid #d5d5da",
                      }}
                    >
                      {inventory.map((inv) => (
                        <option key={inv.partCode} value={inv.partCode}>
                          {inv.partName} ({inv.stockQuantity} in stock)
                        </option>
                      ))}
                    </select>
                    <input
                      name="quantity"
                      type="number"
                      min="1"
                      defaultValue="1"
                      style={{
                        width: 60,
                        minHeight: 38,
                        borderRadius: 8,
                        padding: "0 8px",
                        border: "1px solid #d5d5da",
                      }}
                    />
                    <Button type="submit" tone="secondary" disabled={loading}>
                      Add Part
                    </Button>
                  </form>
                )}
              </div>

              <div className="labour-box">
                <div>
                  <strong>Mechanic Labour</strong>
                  <small>
                    {selectedTask.assignedMechanicName} · Billable Rate
                  </small>
                </div>
                {selectedTask.status !== "COMPLETED" ? (
                  <form
                    onSubmit={handleRecordLabor}
                    style={{ display: "flex", gap: 6, alignItems: "center" }}
                  >
                    <input
                      name="hours"
                      type="number"
                      step="0.25"
                      min="0.25"
                      defaultValue={selectedTask.billableHours || "1"}
                      style={{
                        width: 70,
                        minHeight: 34,
                        borderRadius: 8,
                        padding: "0 6px",
                        border: "1px solid #d5d5da",
                      }}
                    />
                    <Button type="submit" tone="secondary" disabled={loading}>
                      Set Hours
                    </Button>
                  </form>
                ) : (
                  <span>{selectedTask.billableHours} hrs</span>
                )}
              </div>

              {errorMsg && <ErrorBanner text={errorMsg} />}
            </div>

            <aside className="cost-box">
              <h3>Task Cost Summary</h3>
              <span>
                <label>Parts Total</label>
                <strong>${selectedTask.partsCost?.toFixed(2) ?? "0.00"}</strong>
              </span>
              <span>
                <label>Labour Charge</label>
                <strong>${selectedTask.laborCharge?.toFixed(2) ?? "0.00"}</strong>
              </span>
              <div>
                <label>Total Cost</label>
                <strong>
                  ${selectedTask.totalTaskCost?.toFixed(2) ?? "0.00"}
                </strong>
              </div>
            </aside>
          </div>
        )}
      </Sheet>
    )
  }

  // 7. Inventory Report Modal
  if (sheet === "report") {
    const managers = employees.filter((e) => e.type === "MANAGER")
    const [reportText, setReportText] = useState("")
    const [fetchingReport, setFetchingReport] = useState(false)

    const handleGenerateReport = async (mgrId: string) => {
      setFetchingReport(true)
      try {
        const res = await inventoryApi.getReport(mgrId)
        setReportText(res.report)
      } catch (err) {
        const msg =
          err instanceof ApiErrorResponse
            ? err.error.message || err.message
            : String(err)
        setErrorMsg(msg)
      } finally {
        setFetchingReport(false)
      }
    }

    return (
      <Sheet title="Inventory Report" onClose={close}>
        <div className="sheet-body">
          <label className="field">
            <span>Select Authorizing Manager</span>
            <select
              style={{
                minHeight: 44,
                padding: "0 12px",
                borderRadius: 10,
                border: "1px solid #d5d5da",
              }}
              onChange={(e) => handleGenerateReport(e.target.value)}
              defaultValue=""
            >
              <option value="" disabled>
                -- Choose Manager --
              </option>
              {managers.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.name} (Manager)
                </option>
              ))}
            </select>
          </label>
          <pre className="report" style={{ marginTop: 14 }}>
            {fetchingReport
              ? "Generating report from Manager instance..."
              : reportText ||
                "Select a workshop manager to invoke Manager.generateInventoryReport()."}
          </pre>
          {errorMsg && <ErrorBanner text={errorMsg} />}
          <div className="sheet-footer">
            <Button tone="secondary" onClick={close}>
              Close
            </Button>
            <Button
              icon="download"
              disabled={!reportText}
              onClick={() => {
                const blob = new Blob([reportText], { type: "text/plain" })
                const url = URL.createObjectURL(blob)
                const a = document.createElement("a")
                a.href = url
                a.download = "inventory-report.txt"
                a.click()
                URL.revokeObjectURL(url)
              }}
            >
              Export .txt
            </Button>
          </div>
        </div>
      </Sheet>
    )
  }

  // 8. New Part Modal
  if (sheet === "part") {
    const handleCreatePart = async (e: React.FormEvent<HTMLFormElement>) => {
      e.preventDefault()
      setErrorMsg("")
      const form = e.currentTarget
      const partCode = (
        form.elements.namedItem("partCode") as HTMLInputElement
      ).value
      const partName = (
        form.elements.namedItem("partName") as HTMLInputElement
      ).value
      const unitPrice = parseFloat(
        (form.elements.namedItem("unitPrice") as HTMLInputElement).value || "0",
      )
      const stockQuantity = parseInt(
        (form.elements.namedItem("stockQuantity") as HTMLInputElement).value ||
          "0",
        10,
      )
      const reorderThreshold = parseInt(
        (form.elements.namedItem("reorderThreshold") as HTMLInputElement)
          .value || "5",
        10,
      )

      setLoading(true)
      try {
        await inventoryApi.create({
          partCode,
          partName,
          unitPrice,
          stockQuantity,
          reorderThreshold,
        })
        toast(`Spare part ${partName} created!`)
        await refreshAll()
        close()
      } catch (err) {
        const msg =
          err instanceof ApiErrorResponse
            ? err.error.message || err.message
            : String(err)
        setErrorMsg(msg)
      } finally {
        setLoading(false)
      }
    }

    return (
      <Sheet title="New Inventory Part" onClose={close}>
        <form onSubmit={handleCreatePart} className="sheet-body">
          <div className="form-grid two">
            <Field label="Part code" name="partCode" placeholder="e.g. BRK-042" />
            <Field label="Part name" name="partName" placeholder="e.g. Ceramic Brake Pads" />
            <Field
              label="Unit price ($)"
              name="unitPrice"
              type="number"
              step="0.01"
              defaultValue="64.00"
            />
            <Field
              label="Opening stock"
              name="stockQuantity"
              type="number"
              defaultValue="10"
            />
            <Field
              label="Reorder threshold"
              name="reorderThreshold"
              type="number"
              defaultValue="5"
            />
          </div>
          {errorMsg && <ErrorBanner text={errorMsg} />}
          <div className="sheet-footer">
            <Button tone="secondary" onClick={close}>
              Cancel
            </Button>
            <Button type="submit" disabled={loading}>
              {loading ? "Creating..." : "Create Part"}
            </Button>
          </div>
        </form>
      </Sheet>
    )
  }

  // 9. Restock Modal
  if (sheet === "restock") {
    const item = inventory.find((i) => i.partCode === selectedPartCode)
    const handleRestock = async (e: React.FormEvent<HTMLFormElement>) => {
      e.preventDefault()
      if (!selectedPartCode) return
      setErrorMsg("")
      const form = e.currentTarget
      const quantity = parseInt(
        (form.elements.namedItem("quantity") as HTMLInputElement).value || "5",
        10,
      )

      setLoading(true)
      try {
        await inventoryApi.restock(selectedPartCode, quantity)
        toast(`Restocked ${quantity} units of ${selectedPartCode}!`)
        await refreshAll()
        close()
      } catch (err) {
        const msg =
          err instanceof ApiErrorResponse
            ? err.error.message || err.message
            : String(err)
        setErrorMsg(msg)
      } finally {
        setLoading(false)
      }
    }

    return (
      <Sheet title={`Restock ${selectedPartCode}`} onClose={close}>
        <form onSubmit={handleRestock} className="sheet-body">
          <p style={{ fontSize: 13, color: "var(--secondary)", marginBottom: 12 }}>
            Current stock: <strong>{item?.stockQuantity ?? 0} units</strong>
          </p>
          <Field
            label="Quantity to add"
            name="quantity"
            type="number"
            min="1"
            defaultValue="10"
          />
          {errorMsg && <ErrorBanner text={errorMsg} />}
          <div className="sheet-footer">
            <Button tone="secondary" onClick={close}>
              Cancel
            </Button>
            <Button type="submit" disabled={loading}>
              {loading ? "Restocking..." : "Confirm Restock"}
            </Button>
          </div>
        </form>
      </Sheet>
    )
  }

  // 10. New Invoice Modal
  if (sheet === "newInvoice") {
    const [chosenCustId, setChosenCustId] = useState(
      selectedCustomerId || customers[0]?.id || "",
    )
    const [custVehicles, setCustVehicles] = useState<IVehicle[]>([])

    useEffect(() => {
      if (!chosenCustId) return
      customersApi
        .getVehicles(chosenCustId)
        .then(setCustVehicles)
        .catch(console.error)
    }, [chosenCustId])

    const handleCreateInvoice = async (
      e: React.FormEvent<HTMLFormElement>,
    ) => {
      e.preventDefault()
      setErrorMsg("")
      const form = e.currentTarget
      const customerId = (
        form.elements.namedItem("customerId") as HTMLSelectElement
      ).value
      const licensePlate = (
        form.elements.namedItem("licensePlate") as HTMLSelectElement
      ).value

      if (!customerId || !licensePlate) {
        setErrorMsg("Please select both a customer and one of their vehicles.")
        return
      }

      setLoading(true)
      try {
        const inv = await invoicesApi.open({ customerId, licensePlate })
        toast(`Invoice #${inv.invoiceId.slice(0, 8)} opened!`)
        await refreshAll()
        setSelectedInvoiceId(inv.invoiceId)
        close()
        setPage("invoice")
      } catch (err) {
        const msg =
          err instanceof ApiErrorResponse
            ? err.error.message || err.message
            : String(err)
        setErrorMsg(msg)
      } finally {
        setLoading(false)
      }
    }

    return (
      <Sheet title="Open New Service Invoice" onClose={close}>
        <form onSubmit={handleCreateInvoice} className="sheet-body">
          <label className="field">
            <span>Customer</span>
            <select
              name="customerId"
              value={chosenCustId}
              onChange={(e) => setChosenCustId(e.target.value)}
              style={{
                minHeight: 44,
                padding: "0 12px",
                borderRadius: 10,
                border: "1px solid #d5d5da",
              }}
            >
              {customers.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.email})
                </option>
              ))}
            </select>
          </label>
          <label className="field" style={{ marginTop: 14 }}>
            <span>Customer Vehicle</span>
            <select
              name="licensePlate"
              disabled={custVehicles.length === 0}
              style={{
                minHeight: 44,
                padding: "0 12px",
                borderRadius: 10,
                border: "1px solid #d5d5da",
              }}
            >
              {custVehicles.length === 0 ? (
                <option value="">No registered vehicles for this customer</option>
              ) : (
                custVehicles.map((v) => (
                  <option key={v.licensePlate} value={v.licensePlate}>
                    {v.brand} {v.model} ({v.licensePlate}) · {v.type}
                  </option>
                ))
              )}
            </select>
          </label>
          {custVehicles.length === 0 && (
            <p className="form-note" style={{ color: "var(--red)" }}>
              Please register a vehicle for this customer first under Customer Detail.
            </p>
          )}
          {errorMsg && <ErrorBanner text={errorMsg} />}
          <div className="sheet-footer">
            <Button tone="secondary" onClick={close}>
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={loading || custVehicles.length === 0}
            >
              {loading ? "Opening..." : "Create Invoice"}
            </Button>
          </div>
        </form>
      </Sheet>
    )
  }

  // 11. Discount Modal (Overload 1: Percentage, Overload 2: Coupon)
  if (sheet === "discount") {
    const [discType, setDiscType] = useState<"Percentage" | "Coupon">(
      "Percentage",
    )
    const managers = employees.filter((e) => e.type === "MANAGER")

    const handleApplyDiscount = async (
      e: React.FormEvent<HTMLFormElement>,
    ) => {
      e.preventDefault()
      if (!selectedInvoiceId) return
      setErrorMsg("")
      const form = e.currentTarget

      setLoading(true)
      try {
        if (discType === "Percentage") {
          const percentage = parseFloat(
            (form.elements.namedItem("percentage") as HTMLInputElement).value ||
              "10",
          )
          const mgrSelect = form.elements.namedItem(
            "managerId",
          ) as HTMLSelectElement | null
          const managerId = mgrSelect?.value || null
          await invoicesApi.applyPercentageDiscount(selectedInvoiceId, {
            percentage,
            managerId,
          })
          toast(`Applied ${percentage}% discount!`)
        } else {
          const couponCode = (
            form.elements.namedItem("couponCode") as HTMLInputElement
          ).value
          const fixedReduction = parseFloat(
            (form.elements.namedItem("fixedReduction") as HTMLInputElement)
              .value || "10",
          )
          await invoicesApi.applyCouponDiscount(selectedInvoiceId, {
            couponCode,
            fixedReduction,
          })
          toast(`Coupon ${couponCode} applied!`)
        }
        await refreshAll()
        close()
      } catch (err) {
        const msg =
          err instanceof ApiErrorResponse
            ? err.error.message || err.message
            : String(err)
        setErrorMsg(msg)
      } finally {
        setLoading(false)
      }
    }

    return (
      <Sheet title="Apply Invoice Discount" onClose={close}>
        <form onSubmit={handleApplyDiscount} className="sheet-body">
          <Segmented
            items={["Percentage", "Coupon"]}
            active={discType}
            onChange={(val) => setDiscType(val as "Percentage" | "Coupon")}
          />
          {discType === "Percentage" ? (
            <div style={{ marginTop: 14 }}>
              <Field
                label="Discount percentage (%)"
                name="percentage"
                type="number"
                step="0.5"
                defaultValue="10"
              />
              <label className="field" style={{ marginTop: 12 }}>
                <span>Manager Approval (required if &gt; 10%)</span>
                <select
                  name="managerId"
                  style={{
                    minHeight: 44,
                    padding: "0 12px",
                    borderRadius: 10,
                    border: "1px solid #d5d5da",
                  }}
                >
                  <option value="">None (up to 10% unapproved)</option>
                  {managers.map((m) => (
                    <option key={m.id} value={m.id}>
                      {m.name} (Manager)
                    </option>
                  ))}
                </select>
              </label>
            </div>
          ) : (
            <div style={{ marginTop: 14 }}>
              <Field
                label="Coupon Code"
                name="couponCode"
                placeholder="e.g. SAVE10 / WELCOME20"
                defaultValue="SAVE10"
              />
              <Field
                label="Fixed reduction ($)"
                name="fixedReduction"
                type="number"
                defaultValue="10.00"
              />
            </div>
          )}
          {errorMsg && <ErrorBanner text={errorMsg} />}
          <div className="sheet-footer">
            <Button tone="secondary" onClick={close}>
              Cancel
            </Button>
            <Button type="submit" disabled={loading}>
              {loading ? "Applying..." : "Apply Discount"}
            </Button>
          </div>
        </form>
      </Sheet>
    )
  }

  // 12. Tax Modal
  if (sheet === "tax") {
    const handleSetTax = async (e: React.FormEvent<HTMLFormElement>) => {
      e.preventDefault()
      if (!selectedInvoiceId) return
      setErrorMsg("")
      const form = e.currentTarget
      const taxRate = parseFloat(
        (form.elements.namedItem("taxRate") as HTMLInputElement).value || "8",
      )

      setLoading(true)
      try {
        await invoicesApi.setTaxRate(selectedInvoiceId, taxRate)
        toast(`Updated tax rate to ${taxRate}%!`)
        await refreshAll()
        close()
      } catch (err) {
        const msg =
          err instanceof ApiErrorResponse
            ? err.error.message || err.message
            : String(err)
        setErrorMsg(msg)
      } finally {
        setLoading(false)
      }
    }

    return (
      <Sheet title="Edit Tax Rate" onClose={close}>
        <form onSubmit={handleSetTax} className="sheet-body">
          <Field
            label="Tax rate (%)"
            name="taxRate"
            type="number"
            step="0.1"
            defaultValue="8.0"
          />
          <p className="form-note">
            Tax rate can only be edited while the invoice is in OPEN state.
          </p>
          {errorMsg && <ErrorBanner text={errorMsg} />}
          <div className="sheet-footer">
            <Button tone="secondary" onClick={close}>
              Cancel
            </Button>
            <Button type="submit" disabled={loading}>
              {loading ? "Saving..." : "Update Tax"}
            </Button>
          </div>
        </form>
      </Sheet>
    )
  }

  return null
}

// -------------------------------------------------------------
// Root App Component
// -------------------------------------------------------------
export default function App() {
  const [page, setPage] = useState<Page>("dashboard")
  const [sheet, setSheet] = useState("")
  const [toast, setToast] = useState("")

  // Selected Entities
  const [selectedCustomerId, setSelectedCustomerId] = useState<string | null>(
    null,
  )
  const [selectedInvoiceId, setSelectedInvoiceId] = useState<string | null>(
    null,
  )
  const [selectedTaskId, setSelectedTaskId] = useState<string | null>(null)
  const [selectedPartCode, setSelectedPartCode] = useState<string | null>(null)

  // Remote data state
  const [customers, setCustomers] = useState<ICustomer[]>([])
  const [customerVehicles, setCustomerVehicles] = useState<IVehicle[]>([])
  const [customerHistory, setCustomerHistory] = useState<IInvoiceSummary[]>([])
  const [employees, setEmployees] = useState<IEmployee[]>([])
  const [inventory, setInventory] = useState<IInventoryItem[]>([])
  const [tasks, setTasks] = useState<ITask[]>([])
  const [invoices, setInvoices] = useState<IInvoiceSummary[]>([])
  const [currentInvoice, setCurrentInvoice] = useState<IInvoice | null>(null)
  const [receiptText, setReceiptText] = useState("")

  // Search filter for customers
  const [customerSearch, setCustomerSearch] = useState("")

  // System connection status
  const [backendStatus, setBackendStatus] = useState<{
    connected: boolean
    message: string
    pingMs?: number
  }>({
    connected: false,
    message: "Connecting to AutoCare REST API...",
  })
  const [vehicleTypes, setVehicleTypes] = useState<
    { type: string; baseServiceFee: number }[]
  >([])

  // Global loading states
  const [loading, setLoading] = useState(true)

  const showToast = (text: string) => {
    setToast(text)
    window.setTimeout(() => setToast(""), 3500)
  }

  // Primary loader to sync all resources from the backend
  const refreshAll = async () => {
    setLoading(true)
    const start = Date.now()
    try {
      const [vTypes, custs, emps, invs, tsks, invSummaries] = await Promise.all(
        [
          vehiclesApi.getTypes(),
          customersApi.list(customerSearch),
          employeesApi.list(),
          inventoryApi.list(),
          tasksApi.list(),
          invoicesApi.list(),
        ],
      )

      setVehicleTypes(vTypes)
      setCustomers(custs)
      setEmployees(emps)
      setInventory(invs)
      setTasks(tsks)
      setInvoices(invSummaries)

      // Auto-select first customer & invoice if none selected yet
      if (!selectedCustomerId && custs.length > 0) {
        setSelectedCustomerId(custs[0].id)
      }
      if (!selectedInvoiceId && invSummaries.length > 0) {
        setSelectedInvoiceId(invSummaries[0].invoiceId)
      }

      setBackendStatus({
        connected: true,
        message: `Connected to backend with ${custs.length} customers, ${invs.length} inventory parts, and ${emps.length} employees.`,
        pingMs: Date.now() - start,
      })
    } catch (err) {
      console.error("Backend fetch error:", err)
      setBackendStatus({
        connected: false,
        message: `Connection failed: ${err instanceof Error ? err.message : String(err)}. Check that Spring Boot is running on port 8080.`,
      })
      showToast("Could not connect to backend REST server.")
    } finally {
      setLoading(false)
    }
  }

  // Initial load
  useEffect(() => {
    refreshAll()
  }, [])

  // Refetch customers on search change
  useEffect(() => {
    customersApi
      .list(customerSearch)
      .then(setCustomers)
      .catch(console.error)
  }, [customerSearch])

  // Fetch detailed customer data whenever selectedCustomerId changes
  useEffect(() => {
    if (!selectedCustomerId) return
    Promise.all([
      customersApi.getVehicles(selectedCustomerId),
      customersApi.getServiceHistory(selectedCustomerId),
    ])
      .then(([v, h]) => {
        setCustomerVehicles(v)
        setCustomerHistory(h)
      })
      .catch(console.error)
  }, [selectedCustomerId])

  // Fetch detailed invoice whenever selectedInvoiceId changes
  useEffect(() => {
    if (!selectedInvoiceId) return
    Promise.all([
      invoicesApi.get(selectedInvoiceId),
      invoicesApi.getReceipt(selectedInvoiceId).catch(() => ""),
    ])
      .then(([inv, rcpt]) => {
        setCurrentInvoice(inv)
        setReceiptText(rcpt)
      })
      .catch(console.error)
  }, [selectedInvoiceId])

  // Current customer object
  const currentCustomer = useMemo(
    () => customers.find((c) => c.id === selectedCustomerId) || null,
    [customers, selectedCustomerId],
  )

  // Actions on tasks
  const handleStartTask = async (id: string) => {
    try {
      await tasksApi.start(id)
      showToast("Task started! (IN_PROGRESS)")
      await refreshAll()
    } catch (err) {
      const msg =
        err instanceof ApiErrorResponse
          ? err.error.message || err.message
          : String(err)
      showToast(msg)
    }
  }

  const handleCompleteTask = async (id: string) => {
    try {
      await tasksApi.complete(id)
      showToast("Task completed! (COMPLETED)")
      await refreshAll()
    } catch (err) {
      const msg =
        err instanceof ApiErrorResponse
          ? err.error.message || err.message
          : String(err)
      showToast(msg)
    }
  }

  // Action on invoice
  const handleIssueInvoice = async (id: string) => {
    try {
      await invoicesApi.issue(id)
      showToast("Invoice issued! Totals are now frozen.")
      await refreshAll()
      if (selectedInvoiceId === id) {
        const updated = await invoicesApi.get(id)
        setCurrentInvoice(updated)
      }
    } catch (err) {
      const msg =
        err instanceof ApiErrorResponse
          ? err.error.message || err.message
          : String(err)
      showToast(msg)
    }
  }

  const handleSettleInvoice = async (id: string) => {
    try {
      await invoicesApi.settle(id)
      showToast("Invoice settled! Payment recorded & loyalty points awarded.")
      await refreshAll()
      if (selectedInvoiceId === id) {
        const updated = await invoicesApi.get(id)
        setCurrentInvoice(updated)
        const rcpt = await invoicesApi.getReceipt(id).catch(() => "")
        setReceiptText(rcpt)
      }
    } catch (err) {
      const msg =
        err instanceof ApiErrorResponse
          ? err.error.message || err.message
          : String(err)
      showToast(msg)
    }
  }

  const handleLogOvertime = async (mechanicId: string, hours: number) => {
    try {
      await employeesApi.logOvertime(mechanicId, hours)
      showToast(`Logged ${hours} hour overtime for mechanic!`)
      await refreshAll()
    } catch (err) {
      const msg =
        err instanceof ApiErrorResponse
          ? err.error.message || err.message
          : String(err)
      showToast(msg)
    }
  }

  const currentTitle = useMemo(
    () => nav.find((n) => n.id === page)?.label ?? "AutoCare",
    [page],
  )

  const activeTaskCount = useMemo(
    () =>
      tasks.filter(
        (t) => t.status === "PENDING" || t.status === "IN_PROGRESS",
      ).length,
    [tasks],
  )

  const renderContent = () => {
    switch (page) {
      case "design":
        return <DesignSystem />
      case "dashboard":
        return (
          <Dashboard
            setPage={setPage}
            openSheet={setSheet}
            tasks={tasks}
            inventory={inventory}
            invoices={invoices}
            onSelectInvoice={setSelectedInvoiceId}
            loading={loading}
          />
        )
      case "customers":
        return (
          <Customers
            customers={customers}
            loading={loading}
            search={customerSearch}
            setSearch={setCustomerSearch}
            onSelectCustomer={setSelectedCustomerId}
            setPage={setPage}
            openSheet={setSheet}
          />
        )
      case "customer":
        return (
          <CustomerDetail
            customer={currentCustomer}
            vehicles={customerVehicles}
            history={customerHistory}
            loading={loading}
            openSheet={setSheet}
            setPage={setPage}
            onSelectInvoice={setSelectedInvoiceId}
          />
        )
      case "employees":
        return (
          <Employees
            employees={employees}
            loading={loading}
            openSheet={setSheet}
            onLogOvertime={handleLogOvertime}
          />
        )
      case "tasks":
        return (
          <TaskBoard
            tasks={tasks}
            loading={loading}
            openSheet={setSheet}
            onStartTask={handleStartTask}
            onCompleteTask={handleCompleteTask}
            onSelectTask={setSelectedTaskId}
            toast={showToast}
          />
        )
      case "inventory":
        return (
          <Inventory
            inventory={inventory}
            loading={loading}
            openSheet={setSheet}
            onSelectPart={setSelectedPartCode}
          />
        )
      case "invoices":
        return (
          <Invoices
            invoices={invoices}
            loading={loading}
            openSheet={setSheet}
            setPage={setPage}
            onSelectInvoice={setSelectedInvoiceId}
          />
        )
      case "invoice":
        return (
          <InvoiceDetail
            invoice={currentInvoice}
            loading={loading}
            openSheet={setSheet}
            setPage={setPage}
            onIssue={handleIssueInvoice}
            onSettle={handleSettleInvoice}
          />
        )
      case "receipt":
        return (
          <Receipt
            invoice={currentInvoice}
            receiptText={receiptText}
            setPage={setPage}
          />
        )
      case "states":
        return (
          <States
            backendStatus={backendStatus}
            vehicleTypes={vehicleTypes}
          />
        )
      case "flow":
        return <PrototypeFlow setPage={setPage} />
    }
  }

  return (
    <div className="app-shell">
      <Sidebar
        page={page}
        setPage={setPage}
        taskCount={activeTaskCount}
      />
      <main className="main">
        <Topbar title={currentTitle} onRefresh={refreshAll} />
        <div className="page-content">{renderContent()}</div>
      </main>

      {sheet && (
        <SheetContent
          sheet={sheet}
          close={() => setSheet("")}
          setPage={setPage}
          toast={showToast}
          refreshAll={refreshAll}
          selectedCustomerId={selectedCustomerId}
          selectedInvoiceId={selectedInvoiceId}
          selectedTaskId={selectedTaskId}
          selectedPartCode={selectedPartCode}
          customers={customers}
          vehicles={customerVehicles}
          employees={employees}
          inventory={inventory}
          invoices={invoices}
          setSelectedInvoiceId={setSelectedInvoiceId}
          setSelectedCustomerId={setSelectedCustomerId}
        />
      )}

      <div className={`toast ${toast ? "show" : ""}`}>
        <Icon name="check" />
        <span>{toast}</span>
        <button type="button" onClick={() => setToast("")}>
          <Icon name="x" size={16} />
        </button>
      </div>
    </div>
  )
}
