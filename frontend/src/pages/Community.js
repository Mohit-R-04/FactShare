import API_BASE from "../apiConfig";
import React, { useState } from "react";
import axios from "axios";
import "../styles/global.css";


const fallbackArticles = [
  { _id: "f1", title: "The Reality of Climate Change", credibilityScore: 95, content: "Climate change is a scientifically proven phenomenon. The IPCC states that Earth's temperature has risen by 1.1°C since the late 19th century due to human activities.", votes: { upvotes: 24, downvotes: 3 } },
  { _id: "f2", title: "COVID-19 Vaccines: Are They Safe?", credibilityScore: 92, content: "COVID-19 vaccines have undergone rigorous testing and have been proven safe and effective. WHO and CDC confirm vaccines reduce severe illness risk by more than 90%.", votes: { upvotes: 18, downvotes: 2 } },
  { _id: "f3", title: "The Truth About 5G and Health Risks", credibilityScore: 88, content: "Scientific studies confirm that 5G is safe. Unlike ionizing radiation, 5G uses non-ionizing radio waves that do not damage human DNA.", votes: { upvotes: 15, downvotes: 4 } },
  { _id: "f4", title: "Electric Vehicles: Are They Truly Green?", credibilityScore: 85, content: "EVs produce 50% fewer lifetime carbon emissions than gas-powered cars, even when factoring in battery production.", votes: { upvotes: 20, downvotes: 5 } },
  { _id: "f5", title: "AI: Will It Replace Human Jobs?", credibilityScore: 80, content: "While AI will eliminate some repetitive jobs, it will also create new opportunities in fields like data science, cybersecurity, and AI ethics.", votes: { upvotes: 12, downvotes: 3 } },
];

const Community = () => {
  const [articles, setArticles] = useState([]);
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(true);
  const token = localStorage.getItem("token");

  useEffect(() => {
    axios.get(`${API_BASE}/community/articles`)
      .then((res) => setArticles(res.data))
      .catch(() => setArticles(fallbackArticles))
      .finally(() => setLoading(false));
  }, []);

  const handleVote = async (articleId, voteType) => {
    if (!token) { alert("Please sign in to vote."); return; }
    try {
      const res = await axios.post(`${API_BASE}/community/articles/${articleId}/vote`, { voteType }, { headers: { Authorization: `Bearer ${token}` } });
      const updated = res.data.article;
      setArticles((prev) => prev.map((a) => (a._id === articleId ? updated : a)));
      if (selected?._id === articleId) setSelected(updated);
    } catch (err) { console.error("Vote error:", err); }
  };

  const getScoreClass = (score) => score >= 80 ? "score-high" : score >= 50 ? "score-medium" : "score-low";

  return (
    <div className="page">
      <div className="page-header">
        <h2>Community</h2>
        <p>Explore fact-checked articles and vote on their credibility</p>
      </div>

      {!selected ? (
        loading ? <div className="loading">Loading articles...</div> : (
          <div className="articles-grid">
            {articles.map((article) => (
              <div key={article._id} className="article-card" onClick={() => setSelected(article)}>
                <h3>{article.title}</h3>
                <div className="article-meta">
                  <span className={`score-badge ${getScoreClass(article.credibilityScore)}`}>
                    {article.credibilityScore}%
                  </span>
                  <span>👍 {article.votes?.upvotes || 0} · 👎 {article.votes?.downvotes || 0}</span>
                </div>
              </div>
            ))}
          </div>
        )
      ) : (
        <div className="article-detail">
          <button className="btn btn-ghost btn-sm" onClick={() => setSelected(null)} style={{ marginBottom: "16px" }}>
            ← Back to articles
          </button>
          <h2>{selected.title}</h2>
          <div style={{ display: "flex", alignItems: "center", gap: "12px", marginBottom: "20px" }}>
            <span className={`score-badge ${getScoreClass(selected.credibilityScore)}`}>
              Credibility: {selected.credibilityScore}%
            </span>
          </div>
          <p className="article-body">{selected.content}</p>
          <div className="vote-bar">
            <button className={`vote-btn`} onClick={() => handleVote(selected._id, "upvote")}>
              👍 {selected.votes?.upvotes || 0}
            </button>
            <button className={`vote-btn`} onClick={() => handleVote(selected._id, "downvote")}>
              👎 {selected.votes?.downvotes || 0}
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default Community;