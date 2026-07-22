import API_BASE from "../apiConfig";
import React, { useState } from "react";
import axios from "axios";
import DOMPurify from "dompurify";
import "../styles/global.css";


function Chatbot() {
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [isOpen, setIsOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const sendMessage = async () => {
    if (!input.trim() || isLoading) return;

    const userMessage = { role: "user", content: input };
    setMessages((prev) => [...prev, userMessage]);
    setInput("");
    setIsLoading(true);

    try {
      const response = await axios.post(`${API_BASE}/chat`, { question: input });
      setMessages((prev) => [...prev, { role: "assistant", content: response.data.response }]);
    } catch (error) {
      console.error("Chat error:", error);
      setMessages((prev) => [...prev, { role: "assistant", content: "<p>Sorry, I couldn't process that. Please try again.</p>" }]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  return (
    <div className="chatbot-container">
      <button className="chat-toggle" onClick={() => setIsOpen(!isOpen)} title="Open FactBot">
        {isOpen ? "✕" : "💬"}
      </button>

      {isOpen && (
        <div className="chat-box">
          <div className="chat-header">
            <div style={{ display: "flex", alignItems: "center" }}>
              <h2>FactBot</h2>
              <span className="chat-subtitle">AI Assistant</span>
            </div>
            <button className="chat-clear" onClick={() => setMessages([])} title="Clear chat">🗑</button>
          </div>

          <div className="chat-messages">
            {messages.length === 0 && (
              <div className="chat-welcome">
                <p>👋 Hi! I'm FactBot.</p>
                <p>Ask me to fact-check any claim or news article.</p>
              </div>
            )}
            {messages.map((msg, index) => (
              <div key={index} className={msg.role === "user" ? "user-msg" : "bot-msg"}>
                {msg.role === "user" ? (
                  <span>{msg.content}</span>
                ) : (
                  <div dangerouslySetInnerHTML={{ __html: DOMPurify.sanitize(msg.content, { ADD_ATTR: ["href", "target"] }) }} />
                )}
              </div>
            ))}
            {isLoading && <div className="bot-msg typing-indicator"><p>Analyzing...</p></div>}
            <div ref={messagesEndRef} />
          </div>

          <div className="chat-input">
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyPress={handleKeyPress}
              placeholder="Ask me anything..."
              disabled={isLoading}
            />
            <button onClick={sendMessage} disabled={isLoading || !input.trim()}>
              Send
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default Chatbot;