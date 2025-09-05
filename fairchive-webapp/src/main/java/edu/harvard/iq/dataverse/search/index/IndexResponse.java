package edu.harvard.iq.dataverse.search.index;

public class IndexResponse {

    private final String message;

    public IndexResponse(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "IndexResponse{" + "message=" + message + '}';
    }

    public String getMessage() {
        return message;
    }
}
