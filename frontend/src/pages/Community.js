import API_BASE from "../apiConfig";
import React, { useState, useEffect } from "react";
import axios from "axios";
import "../styles/global.css";

const CATEGORIES = ["Politics", "Business", "Technology", "Sports", "Entertainment", "Science", "Health", "World", "Local", "Crime", "Other"];
const STATUSES = ["NEEDS_REVIEW", "REVIEWED", "OPEN"];

const scoreRanges = [
  { label: "Any score", value: "" },
  { label: "0 – 60 (untrusted)", value: "0-60" },
  { label: "60 – 80 (disputed)", value: "60-80" },
  { label: "80 – 100 (credible)", value: "80-100" },
];

const Community = () => {
  const [articles, setArticles] = useState([]);
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [myVote, setMyVote] = useState(null);
  const token = localStorage.getItem("token");

  // Filters
  const [category, setCategory] = useState("");
  const [status, setStatus] = useState("");
  const [scoreRange, setScoreRange] = useState("");
  const [sort, setSort] = useState("recent");

  useEffect(() => {
    fetchArticles();
  }, [category, status, scoreRange, sort]);

  const fetchArticles = () => {
    setLoading(true);
    setError(null);
    const params = new URLSearchParams();
    if (category) params.set("category", category);
    if (status) params.set("status", status);
    if (scoreRange) {
      const [min, max] = scoreRange.split("-").map(Number);
      if (!Number.isNaN(min)) params.set("minScore", String(min));
      if (!Number.isNaN(max)) params.set("maxScore", String(max));
    }
    params.set("sort", sort);
    axios
      .get(`${API_BASE}/community/articles?${params.toString()}`)
      .then((res) => setArticles(res.data))
      .catch((err) => {
        console.error("Failed to load community feed:", err);
        setError("Could not load the community feed. Please try again.");
      })
      .finally(() => setLoading(false));
  };

  const handleVote = async (articleId, voteType) => {
    if (!token) { alert("Please sign in to vote."); return; }
    try {
      const res = await axios.post(
        `${API_BASE}/community/articles/${articleId}/vote`,
        { voteType },
        { headers: { Authorization: `Bearer ${token}` } }
      );
      const updated = res.data;
      setArticles((prev) => prev.map((a) => (a.id === articleId ? updated : a)));
      if (selected?.id === articleId) {
        setSelected(updated);
        setMyVote(voteType);
      }
    } catch (err) {
      console.error("Vote error:", err);
      alert("Voting failed. Please try again.");
    }
  };

  const getScoreClass = (score) => (score >= 80 ? "score-high" : score >= 50 ? "score-medium" : "score-low");

  const getStatusBadge = (s) => {
    if (s === "NEEDS_REVIEW") return { label: "Needs Review", color: "#c68e31", bg: "rgba(198,142,49,0.12)" };
    if (s === "REVIEWED") return { label: "Reviewed", color: "#6f8854", bg: "rgba(111,136,84,0.12)" };
    return { label: "Open", color: "#3e7096", bg: "rgba(62,112,150,0.12)" };
  };

  const selectArticle = (article) => {
    setSelected(article);
    setMyVote(null);
    if (token && Array.isArray(article.voters)) {
      // voters only carry userIds; the current user's vote is derived client-side
      // from the vote button clicks. Kept simple: highlight only after voting.
    }
  };

  const renderFilters = () => (
    <div
      style={{
        display: "flex",
        flexWrap: "wrap",
        gap: "12px",
        marginBottom: "20px",
        padding: "14px 16px",
        borderRadius: "10px",
        border: "1px solid var(--border, #e5e5e5)",
        background: "var(--bg-secondary, #fafafa)",
      }}
    >
      <select value={category} onChange={(e) => setCategory(e.target.value)} className="filter-select">
        <option value="">All categories</option>
        {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
      </select>
      <select value={status} onChange={(e) => setStatus(e.target.value)} className="filter-select">
        <option value="">All review statuses</option>
        {STATUSES.map((s) => <option key={s} value={s}>{s.replace(/_/g, " ")}</option>)}
      </select>
      <select value={scoreRange} onChange={(e) => setScoreRange(e.target.value)} className="filter-select">
        {scoreRanges.map((r) => <option key={r.label} value={r.value}>{r.label}</option>)}
      </select>
      <select value={sort} onChange={(e) => setSort(e.target.value)} className="filter-select">
        <option value="recent">Most recent</option>
        <option value="disputed">Most disputed</option>
      </select>
    </div>
  );

  const renderCard = (article) => {
    const badge = getStatusBadge(article.reviewStatus);
    const cv = article.communityVotes || {};
    const reviewers = Array.isArray(article.voters) ? article.voters.length : 0;
    return (
      <div key={article.id} className="article-card" onClick={() => selectArticle(article)}>
        <h3>{article.title}</h3>
        <div className="article-meta" style={{ flexWrap: "wrap", gap: "8px" }}>
          <span className={`score-badge ${getScoreClass(article.credibilityScore)}`}>
            {article.credibilityScore}%
          </span>
          <span style={{ fontSize: "12px", padding: "2px 8px", borderRadius: "10px", color: badge.color, background: badge.bg }}>
            {badge.label}
          </span>
          <span style={{ fontSize: "12px", padding: "2px 8px", borderRadius: "10px", color: "var(--text-secondary, #888)", background: "var(--bg-tertiary, #f0f0f0)" }}>
            {article.category || "Other"}
          </span>
          <span style={{ fontSize: "13px", color: "var(--text-secondary, #888)" }}>
            True {cv.trueVotes || 0} · False {cv.falseVotes || 0} · Uncertain {cv.uncertainVotes || 0} · {reviewers} reviewer{reviewers === 1 ? "" : "s"}
          </span>
        </div>
      </div>
    );
  };

  const renderDetail = (article) => {
    const badge = getStatusBadge(article.reviewStatus);
    const cv = article.communityVotes || {};
    const total = (cv.trueVotes || 0) + (cv.falseVotes || 0) + (cv.uncertainVotes || 0);
    const reviewers = Array.isArray(article.voters) ? article.voters.length : 0;
    const truePct = total > 0 ? Math.round(((cv.trueVotes || 0) / total) * 100) : null;

    const voteBtn = (label, type, count) => (
      <button
        className={`vote-btn ${myVote === type ? "active" : ""}`}
        onClick={() => handleVote(article.id, type)}
        style={{
          padding: "8px 18px",
          borderRadius: "8px",
          border: `1px solid ${myVote === type ? "var(--primary, #6366f1)" : "var(--border, #e5e5e5)"}`,
          background: myVote === type ? "rgba(99,102,241,0.1)" : "var(--bg-secondary, #fafafa)",
          cursor: "pointer",
          fontSize: "14px",
          fontWeight: 600,
          color: myVote === type ? "var(--primary, #6366f1)" : "var(--text, #333)",
        }}
      >
        {label} ({count})
      </button>
    );

    return (
      <div className="article-detail">
        <button className="btn btn-ghost btn-sm" onClick={() => { setSelected(null); setMyVote(null); }} style={{ marginBottom: "16px" }}>
          ← Back to feed
        </button>
        <h2>{article.title}</h2>
        <div style={{ display: "flex", alignItems: "center", gap: "12px", marginBottom: "12px", flexWrap: "wrap" }}>
          <span className={`score-badge ${getScoreClass(article.credibilityScore)}`}>
            Credibility: {article.credibilityScore}%
          </span>
          <span style={{ fontSize: "12px", padding: "2px 8px", borderRadius: "10px", color: badge.color, background: badge.bg }}>
            {badge.label}
          </span>
          <span style={{ fontSize: "12px", padding: "2px 8px", borderRadius: "10px", color: "var(--text-secondary, #888)", background: "var(--bg-tertiary, #f0f0f0)" }}>
            {article.category || "Other"}
          </span>
        </div>

        {article.verdict && (
          <div style={{ marginBottom: "12px", fontSize: "14px" }}>
            <b>AI verdict:</b> <span style={{ fontWeight: 600 }}>{article.verdict}</span>
            {truePct != null && (
              <span style={{ marginLeft: "12px", color: "var(--text-secondary, #888)" }}>
                Community confidence: {article.communityConfidence != null ? Math.round(article.communityConfidence) : article.credibilityScore}%
              </span>
            )}
          </div>
        )}

        <p className="article-body">{article.content}</p>

        {total >= 3 && (
          <div style={{ marginBottom: "14px", fontSize: "13px", color: "var(--text-secondary, #888)" }}>
            Final verdict after community review: <b style={{ color: "var(--text, #333)" }}>{article.verdict}</b> (majority of {total} votes).
          </div>
        )}

        <div className="vote-bar" style={{ gap: "10px" }}>
          {voteBtn("True", "true", cv.trueVotes || 0)}
          {voteBtn("False", "false", cv.falseVotes || 0)}
          {voteBtn("Uncertain", "uncertain", cv.uncertainVotes || 0)}
        </div>
        <div style={{ marginTop: "10px", fontSize: "12px", color: "var(--text-secondary, #888)" }}>
          {reviewers} reviewer{reviewers === 1 ? "" : "s"} · {total} vote{total === 1 ? "" : "s"} · True {truePct != null ? truePct : 0}% · False {total > 0 ? Math.round(((cv.falseVotes || 0) / total) * 100) : 0}% · Uncertain {total > 0 ? Math.round(((cv.uncertainVotes || 0) / total) * 100) : 0}%
        </div>
      </div>
    );
  };

  return (
    <div className="page">
      <div className="page-header">
        <h2>Community Review Feed</h2>
        <p>Claims with low credibility are auto-published here for journalists &amp; community reviewers to vote True, False, or Uncertain</p>
      </div>

      {renderFilters()}

      {!selected ? (
        loading ? (
          <div className="loading">Loading feed...</div>
        ) : error ? (
          <div className="error-message">{error}</div>
        ) : articles.length === 0 ? (
          <div className="loading" style={{ color: "var(--text-secondary, #888)" }}>
            No claims match these filters yet. Verify a news claim to seed the feed.
          </div>
        ) : (
          <div className="articles-grid">
            {articles.map(renderCard)}
          </div>
        )
      ) : (
        renderDetail(selected)
      )}
    </div>
  );
};

export default Community;
