package nc.admitionum.dto.publicapi;

import java.time.LocalDateTime;

public class SaveRsvpResponse {

    private boolean success;
    private String message;
    private LocalDateTime updatedAt;

    public SaveRsvpResponse() {
    }

    public SaveRsvpResponse(
            boolean success,
            String message,
            LocalDateTime updatedAt) {

        this.success = success;
        this.message = message;
        this.updatedAt = updatedAt;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(
            LocalDateTime updatedAt) {

        this.updatedAt = updatedAt;
    }
}