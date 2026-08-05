package nc.admitionum.dto.publicapi;

import java.time.LocalDateTime;

public class InvitationPublicResponse {

    private String displayName;
    private Integer maxGuests;
    private LocalDateTime expiresAt;
    private ExistingRsvpResponse existingResponse;

    public InvitationPublicResponse() {
    }

    public InvitationPublicResponse(
            String displayName,
            Integer maxGuests,
            LocalDateTime expiresAt,
            ExistingRsvpResponse existingResponse) {

        this.displayName = displayName;
        this.maxGuests = maxGuests;
        this.expiresAt = expiresAt;
        this.existingResponse = existingResponse;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getMaxGuests() {
        return maxGuests;
    }

    public void setMaxGuests(Integer maxGuests) {
        this.maxGuests = maxGuests;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public ExistingRsvpResponse getExistingResponse() {
        return existingResponse;
    }

    public void setExistingResponse(
            ExistingRsvpResponse existingResponse) {

        this.existingResponse = existingResponse;
    }
}