import API_BASE from "../apiConfig";
import React, { useState } from "react";
import { LineChart, Line, BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer } from "recharts";
import "../styles/global.css";


const Dashboard = () => {
  const [stats, setStats] = useState(null);
  const [articles, setArticles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState("overview");

  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token) { setLoading(false); return; }

    Promise.all([
      fetch(`${API_BASE}/stats/user`, { headers: { Authorization: `Bearer ${token}` } }).then((r) => r.json()),
      fetch(`${API_BASE}/article-history`, { headers: { Authorization: `Bearer ${token}` } }).then((r) => r.json()),
    ])
      .then(([statsData, articlesData]) => {
        setStats(statsData);
        setArticles(articlesData);
      })
      .catch((err) => console.error("Dashboard error:", err))
      .finally(() => setLoading(false));
  }, []);

  const getScoreClass = (score) => score >= 80 ? "score-high" : score >= 50 ? "score-medium" : "score-low";

  if (loading) return <div className="page"><div className="loading">Loading dashboard...</div></div>;

  if (!localStorage.getItem("token")) {
    return (
      <div className="page">
        <div className="page-header">
          <h2>Dashboard</h2>
          <p>Please sign in to view your verification dashboard</p>
        </div>
      </div>
    );
  }

  return (
    <div className="page">
      <div className="page-header">
        <h2>Dashboard</h2>
        <p>Track your verification activity and credibility trends</p>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-value">{stats?.totalArticles || 0}</div>
          <div className="stat-label">Articles Verified</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{stats?.avgCredibility || 0}%</div>
          <div className="stat-label">Avg Credibility</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{stats?.scoreDistribution?.[0]?.score || 0}</div>
          <div className="stat-label">High Credibility</div>
        </div>
      </div>

      <div className="tabs">
        <button className={`tab-btn ${activeTab === "overview" ? "active" : ""}`} onClick={() => setActiveTab("overview")}>
          Overview
        </button>
        <button className={`tab-btn ${activeTab === "articles" ? "active" : ""}`} onClick={() => setActiveTab("articles")}>
          My Articles
        </button>
      </div>

      {activeTab === "overview" && (
        <>
          {stats?.credibilityTrend?.length > 0 ? (
            <div className="chart-container">
              <h3>Credibility Trend</h3>
              <ResponsiveContainer width="100%" height={250}>
                <LineChart data={stats.credibilityTrend}>
                  <XAxis dataKey="name" stroke="#555" fontSize={12} />
                  <YAxis domain={[0, 100]} stroke="#555" fontSize={12} />
                  <Tooltip contentStyle={{ background: "#1a1a1a", border: "1px solid #2a2a2a", borderRadius: 8, color: "#f5f5f5" }} />
                  <Line type="monotone" dataKey="credibility" stroke="#3b82f6" strokeWidth={2} dot={{ fill: "#3b82f6", r: 4 }} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          ) : (
            <div className="chart-container">
              <h3>Credibility Trend</h3>
              <div className="empty-state">Submit articles to see your credibility trend</div>
            </div>
          )}

          <div className="chart-container">
            <h3>Score Distribution</h3>
            <ResponsiveContainer width="100%" height={250}>
              <BarChart data={stats?.scoreDistribution || [{ name: "High", score: 0 }, { name: "Medium", score: 0 }, { name: "Low", score: 0 }]}>
                <XAxis dataKey="name" stroke="#555" fontSize={12} />
                <YAxis allowDecimals={false} stroke="#555" fontSize={12} />
                <Tooltip contentStyle={{ background: "#1a1a1a", border: "1px solid #2a2a2a", borderRadius: 8, color: "#f5f5f5" }} />
                <Bar dataKey="score" fill="#3b82f6" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>

          {stats?.recentArticles?.length > 0 && (
            <div>
              <h3 style={{ fontSize: "0.875rem", fontWeight: 600, color: "var(--text)", marginBottom: "16px", textTransform: "uppercase", letterSpacing: "0.5px" }}>
                Recent Activity
              </h3>
              <div className="history-list">
                {stats.recentArticles.map((article) => (
                  <div key={article._id} className="history-item">
                    <span className="history-date">{new Date(article.submissionDate).toLocaleDateString()}</span>
                    <span className="history-text">{article.title}</span>
                    <span className={`score-badge ${getScoreClass(article.credibilityScore)}`}>{article.credibilityScore}%</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </>
      )}

      {activeTab === "articles" && (
        <>
          {articles.length === 0 ? (
            <div className="empty-state">
              <p>No articles submitted yet.</p>
              <p>Go to Verify to get started with AI-powered fact checking.</p>
            </div>
          ) : (
            <div className="articles-grid">
              {articles.map((article) => (
                <div key={article._id} className="article-card">
                  <h3>{article.title}</h3>
                  <div className="article-meta">
                    <span className={`score-badge ${getScoreClass(article.credibilityScore)}`}>{article.credibilityScore}%</span>
                    <span>{new Date(article.submissionDate).toLocaleDateString()}</span>
                  </div>
                  <p style={{ fontSize: "0.8rem", color: "var(--text-muted)", marginTop: "8px" }}>
                    {article.content?.substring(0, 100)}...
                  </p>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default Dashboard;