package edu.harvard.iq.dataverse.api.datadeposit;

final class UrlManager {
    private String originalUrl;
    private String servlet;
    private String targetType;
    private String targetIdentifier;
    private int port;
    private String warning;

    // -------------------- GETTERS --------------------
    String getOriginalUrl() {
        return originalUrl;
    }

    int getPort() {
        return port;
    }

    String getServlet() {
        return servlet;
    }

    String getTargetIdentifier() {
        return targetIdentifier;
    }

    String getTargetType() {
        return targetType;
    }

    // -------------------- SETTERS --------------------
    void setPort(int port) {
        this.port = port;
    }

    void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    void setServlet(String servlet) {
        this.servlet = servlet;
    }

    void setTargetIdentifier(String targetIdentifier) {
        this.targetIdentifier = targetIdentifier;
    }

    void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }
}
