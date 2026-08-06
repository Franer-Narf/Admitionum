package nc.admitionum.model;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "RsvpResponses")
public class RsvpResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @OneToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "InvitationId",
        nullable = false,
        unique = true,
        foreignKey = @ForeignKey(
            name = "FK_RsvpResponses_Invitations"
        )
    )
    private Invitation invitation;

    @Column(
        name = "GuestName",
        nullable = false,
        length = 200
    )
    private String guestName;

    @Column(
        name = "Contact",
        nullable = false,
        length = 200
    )
    private String contact;

    @Column(
        name = "AttendanceConfirmed",
        nullable = false
    )
    private Boolean attendanceConfirmed;

    @Column(
        name = "AttendeeCount",
        nullable = false
    )
    private Integer attendeeCount;

    @Column(
        name = "Intolerances",
        length = 500
    )
    private String intolerances;

    @Column(
        name = "AdditionalComment",
        length = 1000
    )
    private String additionalComment;

    @Column(
        name = "SubmittedAt",
        nullable = false,
        updatable = false
    )
    private LocalDateTime submittedAt;

    @Column(
        name = "UpdatedAt",
        nullable = false
    )
    private LocalDateTime updatedAt;

    public RsvpResponse() {
    }

    public RsvpResponse(
            Invitation invitation,
            String guestName,
            String contact,
            Boolean attendanceConfirmed,
            Integer attendeeCount,
            String intolerances,
            String additionalComment) {

        this.invitation = invitation;
        this.guestName = guestName;
        this.contact = contact;
        this.attendanceConfirmed = attendanceConfirmed;
        this.attendeeCount = attendeeCount;
        this.intolerances = intolerances;
        this.additionalComment = additionalComment;
    }

    @PrePersist
    private void prepareForInsert() {
        LocalDateTime nowUtc =
            LocalDateTime.now(ZoneOffset.UTC);

        if (this.submittedAt == null) {
            this.submittedAt = nowUtc;
        }

        this.updatedAt = nowUtc;
    }

    @PreUpdate
    private void prepareForUpdate() {
        this.updatedAt =
            LocalDateTime.now(ZoneOffset.UTC);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Invitation getInvitation() {
        return invitation;
    }

    public void setInvitation(Invitation invitation) {
        this.invitation = invitation;
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

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(
            LocalDateTime submittedAt) {

        this.submittedAt = submittedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(
            LocalDateTime updatedAt) {

        this.updatedAt = updatedAt;
    }
}