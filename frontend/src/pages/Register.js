import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import API_BASE from "../apiConfig";
import "../styles/global.css";

const Register = ({ onRegister }) => {
  const [formData, setFormData] = useState({
    firstName: "",
    lastName: "",
    username: "",
    email: "",
    password: "",
    confirmPassword: "",
    phoneNumber: "",
    gender: "prefer-not-to-say",
    termsAccepted: false,
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData({ ...formData, [name]: type === "checkbox" ? checked : value });
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setError("");

    if (formData.password !== formData.confirmPassword) {
      setError("Passwords do not match");
      return;
    }
    if (!formData.termsAccepted) {
      setError("You must accept the terms and conditions");
      return;
    }

    setLoading(true);
    try {
      const response = await fetch(`${API_BASE}/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          firstName: formData.firstName,
          lastName: formData.lastName,
          username: formData.username,
          email: formData.email,
          password: formData.password,
          phoneNumber: formData.phoneNumber,
          gender: formData.gender,
          termsAccepted: formData.termsAccepted,
        }),
      });

      const data = await response.json();
      if (response.ok) {
        alert("Registration successful! Please sign in.");
        if (onRegister) onRegister();
        navigate("/login");
      } else {
        setError(data.message);
      }
    } catch (err) {
      console.error("Registration Error:", err);
      setError("Something went wrong. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h2>Create account</h2>
        <p className="auth-subtitle">Join FactShare to start verifying news</p>
        <form className="auth-form" onSubmit={handleRegister}>
          <div className="form-row">
            <div className="form-group">
              <label>First Name</label>
              <input type="text" name="firstName" placeholder="John" value={formData.firstName} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label>Last Name</label>
              <input type="text" name="lastName" placeholder="Doe" value={formData.lastName} onChange={handleChange} required />
            </div>
          </div>
          <div className="form-group">
            <label>Username</label>
            <input type="text" name="username" placeholder="johndoe" value={formData.username} onChange={handleChange} required />
          </div>
          <div className="form-group">
            <label>Email</label>
            <input type="email" name="email" placeholder="you@example.com" value={formData.email} onChange={handleChange} required />
          </div>
          <div className="form-group">
            <label>Phone Number</label>
            <input type="tel" name="phoneNumber" placeholder="+1234567890" value={formData.phoneNumber} onChange={handleChange} required />
          </div>
          <div className="form-row">
            <div className="form-group">
              <label>Password</label>
              <input type="password" name="password" placeholder="Min 6 characters" value={formData.password} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label>Confirm Password</label>
              <input type="password" name="confirmPassword" placeholder="Re-enter password" value={formData.confirmPassword} onChange={handleChange} required />
            </div>
          </div>
          <div className="form-group">
            <label>Gender</label>
            <select name="gender" value={formData.gender} onChange={handleChange}>
              <option value="male">Male</option>
              <option value="female">Female</option>
              <option value="other">Other</option>
              <option value="prefer-not-to-say">Prefer not to say</option>
            </select>
          </div>
          <label className="form-checkbox">
            <input type="checkbox" name="termsAccepted" checked={formData.termsAccepted} onChange={handleChange} required />
            I accept the terms and conditions
          </label>
          {error && <div className="error-message">{error}</div>}
          <button type="submit" className="btn btn-primary btn-lg" style={{ width: "100%" }} disabled={loading}>
            {loading ? "Creating account..." : "Create Account"}
          </button>
        </form>
        <div className="auth-footer">
          Already have an account?{" "}
          <a href="#" onClick={(e) => { e.preventDefault(); navigate("/login"); }}>
            Sign in
          </a>
        </div>
      </div>
    </div>
  );
};

export default Register;