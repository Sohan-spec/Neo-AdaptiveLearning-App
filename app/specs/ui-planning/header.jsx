import React, { useState } from "react";
import { MessageSquare, Radio, Archive } from "lucide-react";

// --- Spatial UI Tokens & Global Styles ---
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
    --text-primary: #2D3250;
    --text-secondary: #5A6070;
    --text-muted: #9099AA;
    --radius-sm: 10px;
    --radius-md: 14px;
    --radius-lg: 18px;
    --radius-xl: 24px;
    --radius-pill: 100px;
    --blur: blur(12px);
    --font: 'DM Sans', system-ui, sans-serif;
  }
`;

const globalStyles = `
  @import url('https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&display=swap');
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: var(--font); background: var(--bg); color: var(--text-primary); -webkit-font-smoothing: antialiased; }

  .spatial-card {
    background: var(--surface);
    backdrop-filter: var(--blur);
    -webkit-backdrop-filter: var(--blur);
    border: 1px solid var(--border-light);
    box-shadow: var(--shadow-outer-md);
    border-radius: var(--radius-xl);
  }

  .nav-tab {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 24px;
    border-radius: var(--radius-md);
    cursor: pointer;
    transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
    color: var(--text-secondary);
    font-weight: 600;
    font-size: 14px;
    border: 1px solid transparent;
    user-select: none;
  }

  .nav-tab:hover {
    color: var(--text-primary);
    background: rgba(255, 255, 255, 0.3);
  }

  .nav-tab.active {
    color: var(--accent);
    background: var(--surface-raised);
    border-color: var(--border-light);
    box-shadow: var(--shadow-outer-sm);
    transform: translateY(-1px);
  }
`;

export default function App() {
  const [activeTab, setActiveTab] = useState("chat");

  const tabs = [
    { id: "chat", label: "Chat", icon: <MessageSquare size={18} /> },
    { id: "live", label: "Live", icon: <Radio size={18} /> },
    { id: "archive", label: "Archive", icon: <Archive size={18} /> },
  ];

  return (
    <>
      <style>{tokens}{globalStyles}</style>
      <div style={{ minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", padding: "20px" }}>
        
        <header 
          className="spatial-card" 
          style={{ 
            width: "100%",
            maxWidth: "600px", 
            padding: "16px", 
            display: "flex", 
            justifyContent: "center",
            alignItems: "center"
          }}
        >
          {/* Navigation Tabs */}
          <nav 
            style={{ 
              display: "flex", 
              gap: "8px", 
              background: "rgba(0,0,0,0.03)", 
              padding: "6px", 
              borderRadius: "20px", 
              boxShadow: "var(--shadow-inner-sm)",
              width: "fit-content"
            }}
          >
            {tabs.map((tab) => (
              <div
                key={tab.id}
                className={`nav-tab ${activeTab === tab.id ? "active" : ""}`}
                onClick={() => setActiveTab(tab.id)}
              >
                {tab.icon}
                {tab.label}
              </div>
            ))}
          </nav>
        </header>

      </div>
    </>
  );
}