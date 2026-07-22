package com.factshare.dto;
public class ErrorResponse {
    private String status; private String message;
    public ErrorResponse() {}
    public ErrorResponse(String status, String message) { this.status = status; this.message = message; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getMessage() { return message; }
    public void setMessage(String v) { this.message = v; }
}
