package nc.admitionum.dto.admin;

import java.time.LocalDateTime;

public class AdminRsvpResponse {

    private Integer invitationId;
    private String displayName;
    private Integer maxGuests;
    private String status;

    private String guestName;
    private String contact;
    private Integer attendeeCount;
    private String intolerances;
    private String additionalComment;

    private LocalDateTime updatedAt;

    public AdminRsvpResponse() {
    }

    public AdminRsvpResponse(
            Integer invitationId,
            String displayName,
            Integer maxGuests,
            String status,
            String guestName,
            String contact,
            Integer attendeeCount,
            String intolerances,
            String additionalComment,
            LocalDateTime updatedAt) {

        this.invitationId = invitationId;
        this.displayName = displayName;
        this.maxGuests = maxGuests;
        this.status = status;
        this.guestName = guestName;
        this.contact = contact;
        this.attendeeCount = attendeeCount;
        this.intolerances = intolerances;
        this.additionalComment = additionalComment;
        this.updatedAt = updatedAt;
    }

    public Integer getInvitationId() {
        return invitationId;
    }

    public void setInvitationId(
            Integer invitationId) {

        this.invitationId = invitationId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(
            String displayName) {

        this.displayName = displayName;
    }

    public Integer getMaxGuests() {
        return maxGuests;
    }

    public void setMaxGuests(
            Integer maxGuests) {

        this.maxGuests = maxGuests;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(
            String status) {

        this.status = status;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(
            String guestName) {

        this.guestName = guestName;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(
            String contact) {

        this.contact = contact;
    }

    public Integer getAttendeeCount() {
        return attendeeCount;
    }

    public void setAttendeeCount(
            Integer attendeeCount) {

        this.attendeeCount = attendeeCount;
    }

    public String getIntolerances() {
        return intolerances;
    }

    public void setIntolerances(
            String intolerances) {

        this.intolerances = intolerances;
    }

    public String getAdditionalComment() {
        return additionalComment;
    }

    public void setAdditionalComment(
            String additionalComment) {

        this.additionalComment = additionalComment;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(
            LocalDateTime updatedAt) {

        this.updatedAt = updatedAt;
    }
}