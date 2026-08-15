import API_BASE from "../apiConfig";
import React, { useState, useRef } from "react";
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

  // Image upload state
  const [imageFile, setImageFile] = useState(null);
  const [imagePreview, setImagePreview] = useState(null);
  const [imageLoading, setImageLoading] = useState(false);
  const [imageResult, setImageResult] = useState(null);
  const [imageError, setImageError] = useState(null);
  const [dragOver, setDragOver] = useState(false);
  const [inputMode, setInputMode] = useState("text"); // "text" or "image"
  const fileInputRef = useRef(null);

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

  const handleImageSelect = (file) => {
    if (!file) return;
    // Validate file type
    const validTypes = ["image/png", "image/jpeg", "image/jpg", "image/bmp", "image/gif", "image/webp"];
    if (!validTypes.includes(file.type)) {
      setImageError("Please upload a PNG, JPG, BMP, GIF, or WebP image.");
      return;
    }
    // Validate file size (10MB)
    if (file.size > 10 * 1024 * 1024) {
      setImageError("Image must be smaller than 10MB.");
      return;
    }
    setImageFile(file);
    setImageError(null);
    setImageResult(null);

    // Create preview
    const reader = new FileReader();
    reader.onload = (e) => setImagePreview(e.target.result);
    reader.readAsDataURL(file);
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    setDragOver(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    setDragOver(false);
  };

  const handleDrop = (e) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    handleImageSelect(file);
  };

  const handleFileInputChange = (e) => {
    const file = e.target.files[0];
    handleImageSelect(file);
  };

  const clearImage = () => {
    setImageFile(null);
    setImagePreview(null);
    setImageResult(null);
    setImageError(null);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  const verifyImageUpload = async () => {
    if (!imageFile) return;
    setImageLoading(true);
    setImageError(null);
    setImageResult(null);
    try {
      const formData = new FormData();
      formData.append("image", imageFile);
      const res = await axios.post(`${API_BASE}/verify-image-upload`, formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      setImageResult(res.data);
    } catch (err) {
      setImageError(err.response?.data?.explanation || "Failed to process image. Please try again.");
    } finally {
      setImageLoading(false);
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
        <p>Submit news claims, suspicious content, or images for AI-powered analysis</p>
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
        <button
          className={`tab-btn ${activeTab === "image" ? "active" : ""}`}
          onClick={() => setActiveTab("image")}
        >
          Image Upload
        </button>
      </div>

      {/* NEWS TAB */}
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

      {/* CONTENT CHECK TAB (text input) */}
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

      {/* IMAGE UPLOAD TAB */}
      {activeTab === "image" && (
        <div>
          <div className="info-message" style={{ marginBottom: "16px" }}>
            Upload an image containing text (screenshot, forwarded message, social media post, article screenshot).
            The system will extract the text using OCR and analyze it for authenticity.
          </div>

          {/* Drag & Drop Area */}
          <div
            className={`image-drop-zone ${dragOver ? "drag-over" : ""} ${imagePreview ? "has-image" : ""}`}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            onClick={() => !imagePreview && fileInputRef.current?.click()}
            style={{
              border: "2px dashed " + (dragOver ? "var(--primary)" : "var(--border, #ccc)"),
              borderRadius: "12px",
              padding: imagePreview ? "0" : "40px",
              textAlign: "center",
              cursor: imagePreview ? "default" : "pointer",
              background: dragOver ? "rgba(99, 102, 241, 0.05)" : "var(--bg-secondary, #f9f9f9)",
              transition: "all 0.2s ease",
              overflow: "hidden",
              minHeight: imagePreview ? "200px" : "160px",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              flexDirection: "column",
              position: "relative",
            }}
          >
            <input
              ref={fileInputRef}
              type="file"
              accept="image/png,image/jpeg,image/jpg,image/bmp,image/gif,image/webp"
              onChange={handleFileInputChange}
              style={{ display: "none" }}
            />

            {!imagePreview ? (
              <>
                <div style={{ fontSize: "48px", marginBottom: "12px", opacity: 0.5 }}>📷</div>
                <p style={{ margin: 0, fontWeight: 600, color: "var(--text, #333)" }}>
                  Drag & drop an image here, or click to browse
                </p>
                <p style={{ margin: "8px 0 0", fontSize: "13px", color: "var(--text-secondary, #888)" }}>
                  Supports PNG, JPG, BMP, GIF, WebP (max 10MB)
                </p>
              </>
            ) : (
              <div style={{ width: "100%", position: "relative" }}>
                <img
                  src={imagePreview}
                  alt="Upload preview"
                  style={{
                    width: "100%",
                    maxHeight: "400px",
                    objectFit: "contain",
                    display: "block",
                  }}
                />
                <button
                  onClick={(e) => { e.stopPropagation(); clearImage(); }}
                  style={{
                    position: "absolute",
                    top: "12px",
                    right: "12px",
                    background: "rgba(0,0,0,0.7)",
                    color: "white",
                    border: "none",
                    borderRadius: "50%",
                    width: "32px",
                    height: "32px",
                    cursor: "pointer",
                    fontSize: "16px",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                  title="Remove image"
                >
                  ✕
                </button>
              </div>
            )}
          </div>

          {imagePreview && (
            <div style={{ marginTop: "8px", fontSize: "13px", color: "var(--text-secondary, #888)" }}>
              {imageFile.name} ({(imageFile.size / 1024).toFixed(1)} KB)
            </div>
          )}

          <button
            className="btn btn-primary"
            onClick={verifyImageUpload}
            disabled={imageLoading || !imageFile}
            style={{ marginTop: "16px" }}
          >
            {imageLoading ? "Extracting text & analyzing..." : "Analyze Image"}
          </button>

          {imageError && <div className="error-message" style={{ marginTop: "16px" }}>{imageError}</div>}

          {imageResult && (
            <div className="result-card">
              <h3>Image Analysis Result</h3>

              {imageResult.extracted_text && imageResult.extracted_text.trim() && (
                <div className="result-row">
                  <span className="result-label">Extracted Text</span>
                  <span
                    className="result-value"
                    style={{
                      maxHeight: "150px",
                      overflow: "auto",
                      whiteSpace: "pre-wrap",
                      fontFamily: "monospace",
                      fontSize: "13px",
                      background: "var(--bg-secondary, #f5f5f5)",
                      padding: "8px 12px",
                      borderRadius: "6px",
                    }}
                  >
                    {imageResult.extracted_text}
                  </span>
                </div>
              )}

              <div className="result-row">
                <span className="result-label">Verdict</span>
                <span className="result-value" style={{ color: getVerdictColor(imageResult.verdict), fontWeight: 600 }}>
                  {imageResult.verdict}
                </span>
              </div>
              <div className="result-row">
                <span className="result-label">Confidence</span>
                <span className="result-value">{imageResult.confidence}%</span>
              </div>
              <div className="result-row">
                <span className="result-label">Analysis</span>
                <span className="result-value">{imageResult.explanation}</span>
              </div>
              {imageResult.flags && imageResult.flags.length > 0 && (
                <div className="result-row">
                  <span className="result-label">Flags</span>
                  <span className="result-value">{imageResult.flags.join(", ")}</span>
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