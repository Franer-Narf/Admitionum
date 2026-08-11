package nc.admitionum.dto.admin;

public class AdminDashboardResponse {

    private int totalInvitations;
    private int answeredInvitations;
    private int pendingInvitations;
    private int confirmedInvitations;
    private int declinedInvitations;
    private int confirmedAttendees;
    private int responsesWithIntolerances;

    public AdminDashboardResponse() {
    }

    public AdminDashboardResponse(
            int totalInvitations,
            int answeredInvitations,
            int pendingInvitations,
            int confirmedInvitations,
            int declinedInvitations,
            int confirmedAttendees,
            int responsesWithIntolerances) {

        this.totalInvitations = totalInvitations;
        this.answeredInvitations = answeredInvitations;
        this.pendingInvitations = pendingInvitations;
        this.confirmedInvitations = confirmedInvitations;
        this.declinedInvitations = declinedInvitations;
        this.confirmedAttendees = confirmedAttendees;
        this.responsesWithIntolerances =
            responsesWithIntolerances;
    }

    public int getTotalInvitations() {
        return totalInvitations;
    }

    public void setTotalInvitations(
            int totalInvitations) {

        this.totalInvitations = totalInvitations;
    }

    public int getAnsweredInvitations() {
        return answeredInvitations;
    }

    public void setAnsweredInvitations(
            int answeredInvitations) {

        this.answeredInvitations = answeredInvitations;
    }

    public int getPendingInvitations() {
        return pendingInvitations;
    }

    public void setPendingInvitations(
            int pendingInvitations) {

        this.pendingInvitations = pendingInvitations;
    }

    public int getConfirmedInvitations() {
        return confirmedInvitations;
    }

    public void setConfirmedInvitations(
            int confirmedInvitations) {

        this.confirmedInvitations = confirmedInvitations;
    }

    public int getDeclinedInvitations() {
        return declinedInvitations;
    }

    public void setDeclinedInvitations(
            int declinedInvitations) {

        this.declinedInvitations = declinedInvitations;
    }

    public int getConfirmedAttendees() {
        return confirmedAttendees;
    }

    public void setConfirmedAttendees(
            int confirmedAttendees) {

        this.confirmedAttendees = confirmedAttendees;
    }

    public int getResponsesWithIntolerances() {
        return responsesWithIntolerances;
    }

    public void setResponsesWithIntolerances(
            int responsesWithIntolerances) {

        this.responsesWithIntolerances =
            responsesWithIntolerances;
    }
}