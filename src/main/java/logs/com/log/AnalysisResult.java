package logs.com.log;

public class AnalysisResult {
    private String id;
    private String summary;
    private String status;
    private String timestamp;

    public AnalysisResult(String id, String summary, String status) {
        this.id = id;
        this.summary = summary;
        this.status = status;
        this.timestamp = java.time.LocalDateTime.now().toString();
    }

    // Getters
    public String getId() { return id; }
    public String getSummary() { return summary; }
    public String getStatus() { return status; }
    public String getTimestamp() { return timestamp; }
}
