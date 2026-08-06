package nc.admitionum.dto.publicapi;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SaveRsvpRequest {

    @NotBlank(
        message =
            "El nombre del invitado es obligatorio."
    )
    @Size(
        min = 2,
        max = 200,
        message =
            "El nombre debe tener entre 2 y 200 caracteres."
    )
    private String guestName;

    @NotBlank(
        message =
            "El contacto es obligatorio."
    )
    @Size(
        min = 3,
        max = 200,
        message =
            "El contacto debe tener entre 3 y 200 caracteres."
    )
    private String contact;

    @NotNull(
        message =
            "Debes confirmar si asistirás."
    )
    private Boolean attendanceConfirmed;

    @NotNull(
        message =
            "El número de asistentes es obligatorio."
    )
    @Min(
        value = 0,
        message =
            "El número de asistentes no puede ser negativo."
    )
    @Max(
        value = 10,
        message =
            "El número de asistentes no puede superar 10."
    )
    private Integer attendeeCount;

    @Size(
        max = 500,
        message =
            "Las intolerancias no pueden superar "
                + "500 caracteres."
    )
    private String intolerances;

    @Size(
        max = 1000,
        message =
            "El comentario no puede superar "
                + "1.000 caracteres."
    )
    private String additionalComment;

    public SaveRsvpRequest() {
    }

    public SaveRsvpRequest(
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