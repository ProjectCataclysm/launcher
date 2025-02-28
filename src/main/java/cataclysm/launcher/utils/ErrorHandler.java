public class ErrorHandler {
    private String shortMessage;
    private String detailedMessage;
    private Throwable exception;

    public ErrorHandler(String shortMessage, String detailedMessage, Throwable exception) {
        this.shortMessage = shortMessage;
        this.detailedMessage = detailedMessage;
        this.exception = exception;
    }

    public String getShortMessage() {
        return shortMessage;
    }

    public String getDetailedMessage() {
        return detailedMessage;
    }

    public Throwable getException() {
        return exception;
    }

    public String getFullStackTrace() {
        if (exception == null) return detailedMessage;
        
        StringBuilder sb = new StringBuilder();
        sb.append(detailedMessage).append("\n\nStack trace:\n");
        for (StackTraceElement element : exception.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
} 