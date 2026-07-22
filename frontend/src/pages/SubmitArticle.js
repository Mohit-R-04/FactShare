import API_BASE from "../apiConfig";
import React, { useState } from "react";
import axios from "axios";
import "../styles/global.css";


const SubmitArticle = () => {
  const [activeTab, setActiveTab] = useState("news");
  const [newsText, setNewsText] = useState("");
  const [contentText, setContentText] = useState("");
  const [newsResult, setNewsResult] = useState(null);
  const [contentResult, setContentResult] = useState(null);
  const [newsLoading, setNewsLoading] = useState(false);
  const [contentLoading, setContentLoading] = useState(false);
  const [newsError, setNewsError] = useState(null);
  const [contentError, setContentError] = useState(null);

  const verifyNews = async () => {
    if (!newsText.trim()) return;
    setNewsLoading(true);
    setNewsError(null);
    setNewsResult(null);
    try {
      const res = await axios.post(`${API_BASE}/verify-news`, { claim: newsText });
      setNewsResult(res.data);
    } catch (err) {
      setNewsError(err.response?.data?.message || "Failed to verify. Please try again.");
    } finally {
      setNewsLoading(false);
    }
  };

  const verifyContent = async () => {
    if (!contentText.trim()) return;
    setContentLoading(true);
    setContentError(null);
    setContentResult(null);
    try {
      const res = await axios.post(`${API_BASE}/verify-image`, { imageText: contentText });
      setContentResult(res.data);
    } catch (err) {
      setContentError(err.response?.data?.message || "Failed to analyze. Please try again.");
    } finally {
      setContentLoading(false);
    }
  };

  const getVerdictColor = (verdict) => {
    if (!verdict) return "var(--text)";
    const v = verdict.toLowerCase();
    if (v === "true" || v === "authentic") return "var(--success)";
    if (v === "false" || v === "manipulated") return "var(--danger)";
    if (v === "misleading") return "var(--warning)";
    return "var(--text-secondary)";
  };

  return (
    <div className="page">
      <div className="page-header">
        <h2>Verify Content</h2>
        <p>Submit news claims or suspicious content for AI-powered analysis</p>
      </div>

      <div className="tabs">
        <button
          className={`tab-btn ${activeTab === "news" ? "active" : ""}`}
          onClick={() => setActiveTab("news")}
        >
          News Claim
        </button>
        <button
          className={`tab-btn ${activeTab === "content" ? "active" : ""}`}
          onClick={() => setActiveTab("content")}
        >
          Content Check
        </button>
      </div>

      {activeTab === "news" && (
        <div>
          <div className="form-group">
            <textarea
              value={newsText}
              onChange={(e) => setNewsText(e.target.value)}
              placeholder="Paste a news article, headline, or claim to fact-check..."
              rows={6}
            />
          </div>
          <button
            className="btn btn-primary"
            onClick={verifyNews}
            disabled={newsLoading || !newsText.trim()}
            style={{ marginTop: "12px" }}
          >
            {newsLoading ? "Analyzing..." : "Verify Claim"}
          </button>

          {newsError && <div className="error-message" style={{ marginTop: "16px" }}>{newsError}</div>}

          {newsResult && (
            <div className="result-card">
              <h3>Verification Result</h3>
              <div className="result-row">
                <span className="result-label">Verdict</span>
                <span className="result-value" style={{ color: getVerdictColor(newsResult.verdict), fontWeight: 600 }}>
                  {newsResult.verdict}
                </span>
              </div>
              <div className="result-row">
                <span className="result-label">Confidence</span>
                <span className="result-value">{newsResult.confidence}%</span>
              </div>
              <div className="result-row">
                <span className="result-label">Category</span>
                <span className="result-value">{newsResult.category}</span>
              </div>
              <div className="result-row">
                <span className="result-label">Analysis</span>
                <span className="result-value">{newsResult.explanation}</span>
              </div>
              {newsResult.sources && (
                <div className="result-row">
                  <span className="result-label">Sources</span>
                  <span className="result-value">{newsResult.sources.join(", ")}</span>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {activeTab === "content" && (
        <div>
          <div className="info-message" style={{ marginBottom: "16px" }}>
            Paste text from a screenshot, forwarded message, or social media post for authenticity analysis.
          </div>
          <div className="form-group">
            <textarea
              value={contentText}
              onChange={(e) => setContentText(e.target.value)}
              placeholder="Paste text content to analyze for authenticity..."
              rows={5}
            />
          </div>
          <button
            className="btn btn-primary"
            onClick={verifyContent}
            disabled={contentLoading || !contentText.trim()}
            style={{ marginTop: "12px" }}
          >
            {contentLoading ? "Analyzing..." : "Analyze Content"}
          </button>

          {contentError && <div className="error-message" style={{ marginTop: "16px" }}>{contentError}</div>}

          {contentResult && (
            <div className="result-card">
              <h3>Analysis Result</h3>
              <div className="result-row">
                <span className="result-label">Verdict</span>
                <span className="result-value" style={{ color: getVerdictColor(contentResult.verdict), fontWeight: 600 }}>
                  {contentResult.verdict}
                </span>
              </div>
              <div className="result-row">
                <span className="result-label">Confidence</span>
                <span className="result-value">{contentResult.confidence}%</span>
              </div>
              <div className="result-row">
                <span className="result-label">Analysis</span>
                <span className="result-value">{contentResult.explanation}</span>
              </div>
              {contentResult.flags && contentResult.flags.length > 0 && (
                <div className="result-row">
                  <span className="result-label">Flags</span>
                  <span className="result-value">{contentResult.flags.join(", ")}</span>
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default SubmitArticle;