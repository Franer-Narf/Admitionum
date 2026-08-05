package nc.admitionum.dto.publicapi;

public class ExistingRsvpResponse {

    private String guestName;
    private String contact;
    private Boolean attendanceConfirmed;
    private Integer attendeeCount;
    private String intolerances;
    private String additionalComment;

    public ExistingRsvpResponse() {
    }

    public ExistingRsvpResponse(
            String guestName,
            String contact,
            Boolean attendanceConfirmed,
            Integer attendeeCount,
            String intolerances,
            String additionalComment) {

        this.guestName = guestName;
        this.contact = contact;
        this.attendanceConfirmed = attendanceConfirmed;
        this.attendeeCount = attendeeCount;
        this.intolerances = intolerances;
        this.additionalComment = additionalComment;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public Boolean getAttendanceConfirmed() {
        return attendanceConfirmed;
    }

    public void setAttendanceConfirmed(
            Boolean attendanceConfirmed) {

        this.attendanceConfirmed = attendanceConfirmed;
    }

    public Integer getAttendeeCount() {
        return attendeeCount;
    }

    public void setAttendeeCount(Integer attendeeCount) {
        this.attendeeCount = attendeeCount;
    }

    public String getIntolerances() {
        return intolerances;
    }

    public void setIntolerances(String intolerances) {
        this.intolerances = intolerances;
    }

    public String getAdditionalComment() {
        return additionalComment;
    }

    public void setAdditionalComment(
            String additionalComment) {

        this.additionalComment = additionalComment;
    }
}