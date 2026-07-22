const API_BASE = window.location.hostname === 'localhost'
  ? 'http://localhost:5001'
  : window.location.hostname === 'factshare.ssnce.dev'
    ? ''  // Same origin — Caddy proxies API routes to backend
    : 'https://factshare-api.onrender.com';

export default API_BASE;