import { useState } from "react";

const tokens = `
  :root {
    --bg: #DDE1EA;
    --surface: rgba(255,255,255,0.55);
    --surface-raised: rgba(255,255,255,0.75);
    --surface-glass: rgba(255,255,255,0.25);
    --border-light: rgba(255,255,255,0.85);
    --border-dim: rgba(255,255,255,0.35);
    --border-dark: rgba(0,0,0,0.08);
    --shadow-outer-sm: 4px 4px 8px rgba(163,177,198,0.6), -2px -2px 6px rgba(255,255,255,0.8);
    --shadow-outer-md: 6px 6px 14px rgba(163,177,198,0.7), -4px -4px 10px rgba(255,255,255,0.85);
    --shadow-outer-lg: 10px 10px 24px rgba(163,177,198,0.8), -6px -6px 16px rgba(255,255,255,0.9);
    --shadow-inner: inset 3px 3px 7px rgba(163,177,198,0.5), inset -2px -2px 5px rgba(255,255,255,0.9);
    --shadow-inner-sm: inset 2px 2px 5px rgba(163,177,198,0.4), inset -1px -1px 4px rgba(255,255,255,0.85);
    --shadow-pressed: inset 4px 4px 10px rgba(163,177,198,0.65), inset -2px -2px 6px rgba(255,255,255,0.7);
    --accent: #4D7BFF;
    --accent-grad: linear-gradient(160deg, #6B94FF 0%, #4361EE 100%);
    --accent-shadow: 0 8px 20px -4px rgba(67,97,238,0.45), inset 0 2px 3px rgba(255,255,255,0.4), inset 0 -2px 3px rgba(0,0,0,0.2);
    --accent-shadow-hover: 0 12px 28px -4px rgba(67,97,238,0.55), inset 0 2px 3px rgba(255,255,255,0.5), inset 0 -2px 3px rgba(0,0,0,0.2);
    --text-primary: #2D3250;
    --text-secondary: #5A6070;
    --text-muted: #9099AA;
    --danger: #E53E3E;
    --success: #38A169;
    --warn: #D69E2E;
    --radius-sm: 10px;
    --radius-md: 14px;
    --radius-lg: 18px;
    --radius-xl: 24px;
    --radius-pill: 100px;
    --blur: blur(12px);
    --blur-heavy: blur(20px);
    --font: 'DM Sans', system-ui, sans-serif;
  }
`;

const globalStyles = `
  @import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@300;400;500;600;700&family=DM+Mono:wght@400;500&display=swap');
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: var(--font); background: var(--bg); color: var(--text-primary); }

  .spatial-btn {
    display: inline-flex; align-items: center; justify-content: center; gap: 7px;
    font-family: var(--font); font-weight: 600; cursor: pointer;
    border: none; outline: none; transition: all 0.15s cubic-bezier(0.34,1.56,0.64,1);
    -webkit-font-smoothing: antialiased; letter-spacing: -0.01em;
    user-select: none; position: relative;
  }
  .spatial-btn:active { transform: scale(0.97) translateY(1px) !important; }

  .btn-ghost {
    background: var(--surface);
    backdrop-filter: var(--blur);
    -webkit-backdrop-filter: var(--blur);
    border: 1px solid var(--border-light);
    box-shadow: var(--shadow-outer-sm), inset 0 1px 1px rgba(255,255,255,0.9);
    color: var(--text-secondary);
  }
  .btn-ghost:hover {
    background: rgba(255,255,255,0.7);
    box-shadow: var(--shadow-outer-md), inset 0 1px 1px rgba(255,255,255,1);
    color: var(--text-primary);
    transform: translateY(-1px);
  }

  .btn-solid {
    background: var(--accent-grad);
    border: 1px solid rgba(255,255,255,0.25);
    box-shadow: var(--accent-shadow);
    color: white;
  }
  .btn-solid:hover {
    box-shadow: var(--accent-shadow-hover);
    transform: translateY(-2px);
    filter: brightness(1.05);
  }

  .btn-neu {
    background: #DDE1EA;
    border: 1px solid var(--border-light);
    box-shadow: var(--shadow-outer-sm), inset 0 1px 1px rgba(255,255,255,0.9);
    color: var(--text-secondary);
  }
  .btn-neu:hover {
    box-shadow: var(--shadow-outer-md), inset 0 1px 1px rgba(255,255,255,1);
    color: var(--text-primary);
    transform: translateY(-1px);
  }
  .btn-neu:active {
    box-shadow: var(--shadow-pressed) !important;
    transform: none !important;
  }

  .btn-danger {
    background: linear-gradient(160deg, #FC8181 0%, #E53E3E 100%);
    border: 1px solid rgba(255,255,255,0.25);
    box-shadow: 0 8px 20px -4px rgba(229,62,62,0.4), inset 0 2px 3px rgba(255,255,255,0.35), inset 0 -2px 3px rgba(0,0,0,0.15);
    color: white;
  }
  .btn-danger:hover { transform: translateY(-2px); filter: brightness(1.05); }

  .btn-sm { padding: 8px 18px; border-radius: var(--radius-sm); font-size: 13px; }
  .btn-md { padding: 11px 24px; border-radius: var(--radius-md); font-size: 14px; }
  .btn-lg { padding: 14px 32px; border-radius: var(--radius-lg); font-size: 15px; }
  .btn-pill { border-radius: var(--radius-pill); }
  .btn-icon-only { padding: 10px; border-radius: var(--radius-md); }
  .btn-block { width: 100%; }

  .spatial-card {
    background: var(--surface);
    backdrop-filter: var(--blur);
    -webkit-backdrop-filter: var(--blur);
    border: 1px solid var(--border-light);
    border-bottom-color: var(--border-dim);
    border-right-color: var(--border-dim);
    box-shadow: var(--shadow-outer-lg);
    border-radius: var(--radius-xl);
    overflow: hidden;
  }

  .spatial-input {
    font-family: var(--font); font-size: 14px; color: var(--text-primary);
    background: rgba(255,255,255,0.4);
    backdrop-filter: var(--blur);
    -webkit-backdrop-filter: var(--blur);
    border: 1px solid var(--border-light);
    border-bottom-color: var(--border-dark);
    border-right-color: var(--border-dark);
    box-shadow: var(--shadow-inner-sm);
    border-radius: var(--radius-md);
    padding: 10px 14px; outline: none; width: 100%;
    transition: all 0.15s ease;
  }
  .spatial-input::placeholder { color: var(--text-muted); }
  .spatial-input:focus {
    background: rgba(255,255,255,0.6);
    border-color: rgba(77,123,255,0.4);
    box-shadow: var(--shadow-inner-sm), 0 0 0 3px rgba(77,123,255,0.12);
  }

  .spatial-badge {
    display: inline-flex; align-items: center; gap: 5px;
    font-family: var(--font); font-size: 11px; font-weight: 600;
    letter-spacing: 0.02em; padding: 4px 10px;
    border-radius: var(--radius-pill);
  }
  .badge-success { background: rgba(56,161,105,0.15); color: #276749; border: 1px solid rgba(56,161,105,0.3); }
  .badge-warn    { background: rgba(214,158,46,0.15); color: #975A16; border: 1px solid rgba(214,158,46,0.3); }
  .badge-danger  { background: rgba(229,62,62,0.15);  color: #9B2C2C; border: 1px solid rgba(229,62,62,0.3); }
  .badge-info    { background: rgba(77,123,255,0.12); color: #2B4C9B; border: 1px solid rgba(77,123,255,0.3); }
  .badge-neutral { background: rgba(255,255,255,0.5); color: var(--text-secondary); border: 1px solid var(--border-light); box-shadow: var(--shadow-outer-sm); }

  .spatial-toggle { position: relative; display: inline-block; }
  .toggle-track {
    width: 44px; height: 24px; border-radius: var(--radius-pill);
    background: #CBD5E0; box-shadow: var(--shadow-inner-sm);
    border: 1px solid var(--border-light);
    transition: background 0.2s ease;
    cursor: pointer; display: flex; align-items: center; padding: 2px 3px;
  }
  .toggle-track.on { background: var(--accent-grad); box-shadow: var(--shadow-inner-sm), 0 0 10px rgba(77,123,255,0.3); }
  .toggle-thumb {
    width: 18px; height: 18px; border-radius: 50%;
    background: white;
    box-shadow: 2px 2px 5px rgba(163,177,198,0.5);
    transition: transform 0.2s cubic-bezier(0.34,1.56,0.64,1);
    transform: translateX(0);
  }
  .toggle-track.on .toggle-thumb { transform: translateX(20px); }

  .spatial-select {
    font-family: var(--font); font-size: 14px; color: var(--text-primary);
    background: rgba(255,255,255,0.4);
    backdrop-filter: var(--blur);
    -webkit-backdrop-filter: var(--blur);
    border: 1px solid var(--border-light);
    border-bottom-color: var(--border-dark);
    box-shadow: var(--shadow-inner-sm);
    border-radius: var(--radius-md);
    padding: 10px 36px 10px 14px;
    outline: none; appearance: none; cursor: pointer;
    background-image: url("data:image/svg+xml,%3Csvg width='12' height='8' viewBox='0 0 12 8' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 1L6 6L11 1' stroke='%235A6070' stroke-width='1.5' stroke-linecap='round'/%3E%3C/svg%3E");
    background-repeat: no-repeat;
    background-position: right 12px center;
  }

  .spatial-divider {
    border: none; height: 1px;
    background: linear-gradient(90deg, transparent, rgba(163,177,198,0.4) 30%, rgba(163,177,198,0.4) 70%, transparent);
    margin: 0;
  }
`;

/* ── Primitive: Button ────────────────────────────────────────── */
export function Button({
  children, variant = "ghost", size = "md",
  pill = false, block = false, iconOnly = false,
  disabled = false, icon, onClick, className = "", ...rest
}) {
  const cls = [
    "spatial-btn",
    `btn-${variant}`,
    iconOnly ? "btn-icon-only" : `btn-${size}`,
    pill && "btn-pill",
    block && "btn-block",
    className,
  ].filter(Boolean).join(" ");

  return (
    <button className={cls} onClick={onClick} disabled={disabled}
      style={{ opacity: disabled ? 0.5 : 1, cursor: disabled ? "not-allowed" : "pointer", ...rest.style }}
    >
      {icon && <span style={{ display: "flex", width: 16, height: 16 }}>{icon}</span>}
      {children}
    </button>
  );
}

/* ── Primitive: Badge ─────────────────────────────────────────── */
export function Badge({ children, variant = "neutral", dot = false }) {
  return (
    <span className={`spatial-badge badge-${variant}`}>
      {dot && (
        <span style={{
          width: 6, height: 6, borderRadius: "50%",
          background: variant === "success" ? "#38A169" : variant === "danger" ? "#E53E3E" : variant === "warn" ? "#D69E2E" : "#4D7BFF",
          flexShrink: 0,
        }} />
      )}
      {children}
    </span>
  );
}

/* ── Primitive: Toggle ────────────────────────────────────────── */
export function Toggle({ checked, onChange }) {
  return (
    <div className={`toggle-track ${checked ? "on" : ""}`} onClick={() => onChange(!checked)}>
      <div className="toggle-thumb" />
    </div>
  );
}

/* ── Primitive: Input ─────────────────────────────────────────── */
export function Input({ label, placeholder, value, onChange, type = "text" }) {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
      {label && <label style={{ fontSize: 12, fontWeight: 600, color: "var(--text-secondary)", letterSpacing: "0.02em" }}>{label}</label>}
      <input className="spatial-input" type={type} placeholder={placeholder} value={value} onChange={onChange} />
    </div>
  );
}

/* ── Primitive: Select ────────────────────────────────────────── */
export function Select({ label, options = [], value, onChange }) {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
      {label && <label style={{ fontSize: 12, fontWeight: 600, color: "var(--text-secondary)", letterSpacing: "0.02em" }}>{label}</label>}
      <select className="spatial-select" value={value} onChange={onChange}>
        {options.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
      </select>
    </div>
  );
}

/* ── Primitive: Card ──────────────────────────────────────────── */
export function Card({ children, padding = "24px", style = {} }) {
  return (
    <div className="spatial-card" style={{ padding, ...style }}>
      {children}
    </div>
  );
}

/* ── Primitive: Divider ───────────────────────────────────────── */
export function Divider() {
  return <hr className="spatial-divider" />;
}

/* ── Showcase: the kitchen-sink demo ─────────────────────────── */
function Section({ title, children }) {
  return (
    <div style={{ marginBottom: 40 }}>
      <p style={{ fontSize: 11, fontWeight: 700, letterSpacing: "0.1em", textTransform: "uppercase", color: "var(--text-muted)", marginBottom: 16 }}>
        {title}
      </p>
      {children}
    </div>
  );
}

function Row({ children, wrap = false }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 12, flexWrap: wrap ? "wrap" : "nowrap" }}>
      {children}
    </div>
  );
}

export default function App() {
  const [toggle1, setToggle1] = useState(true);
  const [toggle2, setToggle2] = useState(false);
  const [inputVal, setInputVal] = useState("");
  const [selectVal, setSelectVal] = useState("week");

  return (
    <>
      <style>{tokens}{globalStyles}</style>
      <div style={{ minHeight: "100vh", background: "var(--bg)", padding: "48px 32px", fontFamily: "var(--font)" }}>

        <div style={{ maxWidth: 820, margin: "0 auto" }}>
          {/* Header */}
          <div style={{ marginBottom: 48 }}>
            <h1 style={{ fontSize: 28, fontWeight: 700, color: "var(--text-primary)", letterSpacing: "-0.03em" }}>
              spatial<span style={{ color: "var(--accent)" }}>UI</span>
            </h1>
            <p style={{ fontSize: 14, color: "var(--text-muted)", marginTop: 4 }}>
              Glassmorphism 2.0 · Neu-Skeuomorphism · Spatial depth
            </p>
          </div>

          {/* Buttons */}
          <Section title="Buttons — variants">
            <Row>
              <Button variant="ghost">See all</Button>
              <Button variant="solid">Check</Button>
              <Button variant="neu">Logout</Button>
              <Button variant="danger">Delete</Button>
            </Row>
          </Section>

          <Section title="Buttons — sizes">
            <Row>
              <Button variant="solid" size="sm">Small</Button>
              <Button variant="solid" size="md">Medium</Button>
              <Button variant="solid" size="lg">Large</Button>
            </Row>
          </Section>

          <Section title="Buttons — pill + icon">
            <Row>
              <Button variant="solid" pill>Get started</Button>
              <Button variant="ghost" pill
                icon={<svg viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"><circle cx="7" cy="7" r="5"/><path d="M12 12l2.5 2.5"/></svg>}
              >Search</Button>
              <Button variant="neu" iconOnly
                icon={<svg viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"><path d="M8 1v14M1 8h14"/></svg>}
              />
              <Button variant="ghost" disabled>Disabled</Button>
            </Row>
          </Section>

          <Divider />
          <div style={{ height: 32 }} />

          {/* Badges */}
          <Section title="Badges">
            <Row wrap>
              <Badge variant="success" dot>Completed</Badge>
              <Badge variant="warn" dot>Pending</Badge>
              <Badge variant="danger" dot>Failed</Badge>
              <Badge variant="info">New</Badge>
              <Badge variant="neutral">This week</Badge>
            </Row>
          </Section>

          <Divider />
          <div style={{ height: 32 }} />

          {/* Toggle */}
          <Section title="Toggle">
            <Row>
              <Toggle checked={toggle1} onChange={setToggle1} />
              <span style={{ fontSize: 13, color: "var(--text-secondary)" }}>{toggle1 ? "Enabled" : "Disabled"}</span>
              <Toggle checked={toggle2} onChange={setToggle2} />
              <span style={{ fontSize: 13, color: "var(--text-secondary)" }}>{toggle2 ? "Enabled" : "Disabled"}</span>
            </Row>
          </Section>

          <Divider />
          <div style={{ height: 32 }} />

          {/* Inputs */}
          <Section title="Inputs &amp; Select">
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16 }}>
              <Input label="Car number" placeholder="Enter registration" value={inputVal} onChange={e => setInputVal(e.target.value)} />
              <Select label="Time range" value={selectVal} onChange={e => setSelectVal(e.target.value)}
                options={[
                  { value: "day", label: "Today" },
                  { value: "week", label: "This week" },
                  { value: "month", label: "This month" },
                ]}
              />
            </div>
          </Section>

          <Divider />
          <div style={{ height: 32 }} />

          {/* Card */}
          <Section title="Card — glassmorphism surface">
            <Card>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 20 }}>
                <div>
                  <p style={{ fontSize: 12, fontWeight: 600, color: "var(--text-muted)", letterSpacing: "0.04em", textTransform: "uppercase" }}>Weekly income</p>
                  <p style={{ fontSize: 32, fontWeight: 700, color: "var(--text-primary)", letterSpacing: "-0.03em", marginTop: 4 }}>$8,500</p>
                </div>
                <Badge variant="success" dot>+2% this week</Badge>
              </div>
              <Divider />
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: 12, marginTop: 20 }}>
                {[
                  { label: "Total Hired", val: "54%", v: "info" },
                  { label: "Cancelled", val: "20%", v: "warn" },
                  { label: "Pending", val: "26%", v: "danger" },
                ].map(stat => (
                  <div key={stat.label} style={{
                    background: "rgba(255,255,255,0.45)", borderRadius: "var(--radius-md)",
                    border: "1px solid var(--border-light)",
                    boxShadow: "var(--shadow-outer-sm)",
                    padding: "14px 16px",
                  }}>
                    <p style={{ fontSize: 11, color: "var(--text-muted)", fontWeight: 600, letterSpacing: "0.03em", textTransform: "uppercase" }}>{stat.label}</p>
                    <p style={{ fontSize: 22, fontWeight: 700, color: "var(--text-primary)", letterSpacing: "-0.02em", marginTop: 4 }}>{stat.val}</p>
                    <Badge variant={stat.v} style={{ marginTop: 6 }}>{stat.v === "info" ? "↑ Up" : stat.v === "warn" ? "→ Flat" : "↓ Down"}</Badge>
                  </div>
                ))}
              </div>
              <div style={{ display: "flex", gap: 10, marginTop: 20 }}>
                <Button variant="solid" style={{ flex: 1 }}>See details</Button>
                <Button variant="ghost" style={{ flex: 1 }}>Export</Button>
              </div>
            </Card>
          </Section>

        </div>
      </div>
    </>
  );
}
