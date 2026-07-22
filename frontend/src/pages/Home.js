import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { FaBrain, FaImage, FaStar } from "react-icons/fa";
import "../styles/global.css";

const Home = () => {
  const [showPopup, setShowPopup] = useState(true);
  const navigate = useNavigate();

  const tickerItems = [
    "80% of first-time voters face fake news on social media",
    "India ranks highest risk for misinformation globally",
    "False information spreads 6x faster than truth on social media",
    "Deepfake videos increased by 900% between 2019-2022",
    "Over 70% of fake news originates from unverified social sources",
    "WHO termed COVID misinformation an 'Infodemic'",
    "AI-generated fake news is now more believable than human-written",
    "A single fake article can reach 100,000 people within hours",
  ];

  return (
    <>
      {showPopup && (
        <div className="popup-overlay" onClick={() => setShowPopup(false)}>
          <div className="popup-content" onClick={(e) => e.stopPropagation()}>
            <img src={require("../assets/india.jpg")} alt="Misinformation Alert" />
            <h2>
              India ranks #1 in misinformation. It's time to verify what you read.
            </h2>
            <button className="btn btn-primary btn-lg" onClick={() => setShowPopup(false)}>
              Explore FactShare
            </button>
          </div>
        </div>
      )}

      {!showPopup && (
        <>
          <div className="ticker">
            <div className="ticker-inner">
              {tickerItems.concat(tickerItems).map((item, i) => (
                <span key={i} className="ticker-item">
                  <span>⚠</span> {item}
                </span>
              ))}
            </div>
          </div>

          <section className="hero">
            <h1>Unmask the Truth, One Click at a Time</h1>
            <p>
              AI-powered fact-checking at your fingertips. Detect fake news, verify sources, and stay informed with confidence.
            </p>
            <div className="hero-actions">
              <button className="btn btn-primary btn-lg" onClick={() => navigate("/submit")}>
                Verify News
              </button>
              <button className="btn btn-secondary btn-lg" onClick={() => navigate("/community")}>
                Browse Articles
              </button>
            </div>
          </section>

          <section className="features-section">
            <h2>How It Works</h2>
            <div className="features-grid">
              <div className="feature-card">
                <FaBrain className="feature-icon" />
                <h3>AI Fact-Checking</h3>
                <p>
                  Advanced AI models analyze news claims for accuracy, cross-referencing against known facts and patterns.
                </p>
              </div>
              <div className="feature-card">
                <FaImage className="feature-icon" />
                <h3>Content Verification</h3>
                <p>
                  Analyze text from screenshots, forwarded messages, and social media posts for authenticity indicators.
                </p>
              </div>
              <div className="feature-card">
                <FaStar className="feature-icon" />
                <h3>Credibility Scoring</h3>
                <p>
                  Get instant credibility scores through AI-driven analysis, helping you make informed decisions.
                </p>
              </div>
            </div>
          </section>

          <footer className="footer">
            <p>FactShare — Fighting misinformation with technology</p>
          </footer>
        </>
      )}
    </>
  );
};

export default Home;