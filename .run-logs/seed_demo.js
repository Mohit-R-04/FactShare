// FactShare demo-data seed (screenshots only — no personal/real data).
// Populates the demo user's dashboard articles and the community feed with
// clean, varied, non-sensitive sample data.
const db = db.getSiblingDB("factshare");
const DEMO_USER = "6a81b3cde93d88060611ea92";

// 1. Reset demo user's article history (dashboard)
db.articles.deleteMany({ userId: DEMO_USER });

// 2. Reset community feed
db.communityArticles.deleteMany({});

// --- Dashboard articles (varied credibility + backdated months for a trend) ---
const articles = [
  { t: "Paris is the capital city of France.", c: "Multiple credible sources consistently identify Paris as the capital of France.", s: 100, d: "2026-08-16T10:00:00Z" },
  { t: "Drinking adequate water is essential for human health.", c: "Health authorities agree that proper hydration is important for bodily function.", s: 96, d: "2026-08-10T10:00:00Z" },
  { t: "Regular physical exercise supports cardiovascular health.", c: "Numerous studies link regular exercise with improved heart health.", s: 91, d: "2026-07-22T10:00:00Z" },
  { t: "Viral post claims cold weather directly causes the common cold.", c: "Colds are caused by viruses, not temperature; cold weather only correlates with indoor crowding.", s: 55, d: "2026-07-05T10:00:00Z" },
  { t: "Social media post claims drinking bleach cures infections.", c: "False and dangerous. Bleach is toxic and never a treatment for infections.", s: 8, d: "2026-06-18T10:00:00Z" }
];
articles.forEach(a => {
  db.articles.insertOne({
    userId: DEMO_USER,
    type: "news",
    title: a.t,
    content: a.c,
    credibilityScore: a.s,
    submissionDate: ISODate(a.d),
    _class: "com.factshare.model.Article"
  });
});

// --- Community feed (claim cards with review votes) ---
const votersFor = (count, voteType, prefix) => {
  const arr = [];
  for (let i = 0; i < count; i++) arr.push({ userId: `${prefix}-${i}`, voteType });
  return arr;
};
const community = [
  {
    claim: "Viral video claims gargling salt water cures all infections.",
    content: "No clinical evidence supports salt water as a cure for infections. Misleading health advice.",
    score: 12, verdict: "FALSE", category: "Health", status: "NEEDS_REVIEW",
    tv: 0, fv: 3, uv: 1, d: "2026-08-15T09:30:00Z"
  },
  {
    claim: "Social media post claims cold weather directly causes the common cold.",
    content: "Colds are caused by viruses, not cold air. The post conflates correlation with causation.",
    score: 45, verdict: "MISLEADING", category: "Health", status: "NEEDS_REVIEW",
    tv: 1, fv: 2, uv: 0, d: "2026-08-14T09:30:00Z"
  },
  {
    claim: "Message claims a popular phone model explodes while charging.",
    content: "No verified reports support this. The claim appears to originate from an unverified forward.",
    score: 38, verdict: "FALSE", category: "Technology", status: "REVIEWED",
    tv: 0, fv: 4, uv: 0, d: "2026-08-13T09:30:00Z"
  },
  {
    claim: "Article claims a new study shows coffee prevents all cancers.",
    content: "The cited study does not make this claim. Oversimplified and misleading headline.",
    score: 52, verdict: "MISLEADING", category: "Science", status: "OPEN",
    tv: 1, fv: 1, uv: 2, d: "2026-08-12T09:30:00Z"
  }
];
community.forEach(a => {
  db.communityArticles.insertOne({
    userId: DEMO_USER,
    type: "verification",
    title: a.claim,
    content: a.content,
    credibilityScore: a.score,
    submissionDate: ISODate(a.d),
    votes: { upvotes: 0, downvotes: 0 },
    voters: [
      ...votersFor(a.tv, "true", "rv-t"),
      ...votersFor(a.fv, "false", "rv-f"),
      ...votersFor(a.uv, "uncertain", "rv-u")
    ],
    category: a.category,
    reviewStatus: a.status,
    verdict: a.verdict,
    claim: a.claim,
    communityVotes: { trueVotes: a.tv, falseVotes: a.fv, uncertainVotes: a.uv },
    communityConfidence: a.score,
    aiScore: a.score,
    disputeCount: a.fv + a.uv,
    _class: "com.factshare.model.CommunityArticle"
  });
});

print("seeded: " + db.articles.countDocuments({ userId: DEMO_USER }) + " articles, " + db.communityArticles.countDocuments({}) + " community articles");
