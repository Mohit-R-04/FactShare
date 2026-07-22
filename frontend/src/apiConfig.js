const API_BASE = window.location.hostname === 'localhost'
  ? 'http://localhost:5001'
  : 'https://factshare-api.onrender.com';

export default API_BASE;