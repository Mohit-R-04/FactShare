package com.factshare.dto;
public class AuthResponse {
    private String status; private String message; private String token; private String userId; private String username;
    public AuthResponse() {}
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }
    public String getToken() { return token; }
    public void setToken(String v) { this.token = v; }
    public String getUserId() { return userId; }
    public void setUserId(String v) { this.userId = v; }
    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private AuthResponse r = new AuthResponse();
        public Builder status(String v) { r.status = v; return this; }
        public Builder message(String v) { r.message = v; return this; }
        public Builder token(String v) { r.token = v; return this; }
        public Builder userId(String v) { r.userId = v; return this; }
        public Builder username(String v) { r.username = v; return this; }
        public AuthResponse build() { return r; }
    }
}
