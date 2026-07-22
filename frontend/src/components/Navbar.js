import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import "../styles/global.css";

const Navbar = ({ isAuthenticated, onLogout }) => {
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <nav className="navbar">
      <div className="nav-logo" onClick={() => navigate("/")}>
        <h1>FactShare</h1>
        <span className="motto">Verify · Trust · Share</span>
      </div>

      <button className="nav-hamburger" onClick={() => setMobileOpen(!mobileOpen)} aria-label="Menu">
        {mobileOpen ? "✕" : "☰"}
      </button>

      <div className={`nav-links ${mobileOpen ? "nav-links-open" : ""}`}>
        <button onClick={() => { navigate("/"); setMobileOpen(false); }}>Home</button>
        <button onClick={() => { navigate("/community"); setMobileOpen(false); }}>Community</button>
        <button onClick={() => { navigate("/submit"); setMobileOpen(false); }}>Verify</button>
        {isAuthenticated ? (
          <>
            <button onClick={() => { navigate("/dashboard"); setMobileOpen(false); }}>Dashboard</button>
            <button className="nav-cta nav-cta-logout" onClick={() => { onLogout(); setMobileOpen(false); }}>Sign Out</button>
          </>
        ) : (
          <>
            <button onClick={() => { navigate("/login"); setMobileOpen(false); }}>Sign In</button>
            <button className="nav-cta" onClick={() => { navigate("/register"); setMobileOpen(false); }}>Get Started</button>
          </>
        )}
      </div>
    </nav>
  );
};

export default Navbar;