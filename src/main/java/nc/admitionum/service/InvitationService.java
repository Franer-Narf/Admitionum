package nc.admitionum.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nc.admitionum.dto.publicapi.ExistingRsvpResponse;
import nc.admitionum.dto.publicapi.InvitationPublicResponse;
import nc.admitionum.dto.publicapi.SaveRsvpRequest;
import nc.admitionum.dto.publicapi.SaveRsvpResponse;
import nc.admitionum.exception.InvalidAttendeeCountException;
import nc.admitionum.exception.InvitationDisabledException;
import nc.admitionum.exception.InvitationExpiredException;
import nc.admitionum.exception.InvitationNotFoundException;
import nc.admitionum.model.Invitation;
import nc.admitionum.model.RsvpResponse;
import nc.admitionum.repository.InvitationRepository;
import nc.admitionum.repository.RsvpResponseRepository;

@Service
@Transactional(readOnly = true)
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final RsvpResponseRepository rsvpResponseRepository;

    public InvitationService(
            InvitationRepository invitationRepository,
            RsvpResponseRepository rsvpResponseRepository) {

        this.invitationRepository = invitationRepository;
        this.rsvpResponseRepository =
            rsvpResponseRepository;
    }

    public InvitationPublicResponse getPublicInvitation(
            String accessCode) {

        Invitation invitation =
            findValidInvitation(accessCode);

        ExistingRsvpResponse existingResponse =
            rsvpResponseRepository
                .findByInvitationId(invitation.getId())
                .map(this::toExistingRsvpResponse)
                .orElse(null);

        return new InvitationPublicResponse(
            invitation.getDisplayName(),
            invitation.getMaxGuests(),
            invitation.getExpiresAt(),
            existingResponse
        );
    }

    @Transactional
    public SaveRsvpResponse saveResponse(
            String accessCode,
            SaveRsvpRequest request) {

        Invitation invitation =
            findValidInvitation(accessCode);

        validateAttendance(
            invitation,
            request.getAttendanceConfirmed(),
            request.getAttendeeCount()
        );

        RsvpResponse response =
            rsvpResponseRepository
                .findByInvitationId(invitation.getId())
                .orElseGet(RsvpResponse::new);

        if (response.getId() == null) {
            response.setInvitation(invitation);
        }

        response.setGuestName(
            clean(request.getGuestName())
        );

        response.setContact(
            clean(request.getContact())
        );

        response.setAttendanceConfirmed(
            request.getAttendanceConfirmed()
        );

        if (Boolean.TRUE.equals(
                request.getAttendanceConfirmed())) {

            response.setAttendeeCount(
                request.getAttendeeCount()
            );
        } else {
            response.setAttendeeCount(0);
        }

        response.setIntolerances(
            cleanNullable(
                request.getIntolerances()
            )
        );

        response.setAdditionalComment(
            cleanNullable(
                request.getAdditionalComment()
            )
        );

        RsvpResponse storedResponse =
            rsvpResponseRepository
                .saveAndFlush(response);

        return new SaveRsvpResponse(
            true,
            "Tu respuesta se ha guardado correctamente.",
            storedResponse.getUpdatedAt()
        );
    }

    private Invitation findValidInvitation(
            String accessCode) {

        String normalizedAccessCode =
            normalizeAccessCode(accessCode);

        Invitation invitation =
            invitationRepository
                .findByAccessCode(normalizedAccessCode)
                .orElseThrow(
                    InvitationNotFoundException::new
                );

        verifyInvitationIsActive(invitation);
        verifyInvitationHasNotExpired(invitation);

        return invitation;
    }

    private void validateAttendance(
            Invitation invitation,
            Boolean attendanceConfirmed,
            Integer attendeeCount) {

        if (Boolean.TRUE.equals(
                attendanceConfirmed)) {

            if (attendeeCount == null
                    || attendeeCount < 1) {

                throw new InvalidAttendeeCountException(
                    "Debe asistir al menos una persona."
                );
            }

            if (attendeeCount
                    > invitation.getMaxGuests()) {

                throw new InvalidAttendeeCountException(
                    "El número de asistentes supera "
                        + "el máximo permitido."
                );
            }
        }

        if (Boolean.FALSE.equals(
                attendanceConfirmed)
                && (attendeeCount == null
                    || attendeeCount != 0)) {

            throw new InvalidAttendeeCountException(
                "Cuando no se confirma asistencia, "
                    + "el número de asistentes debe "
                    + "ser cero."
            );
        }
    }

    private String clean(String value) {

        if (value == null) {
            return null;
        }

        return value.trim();
    }

    private String cleanNullable(String value) {

        if (value == null) {
            return null;
        }

        String cleanedValue =
            value.trim();

        if (cleanedValue.isEmpty()) {
            return null;
        }

        return cleanedValue;
    }

    private ExistingRsvpResponse
            toExistingRsvpResponse(
                    RsvpResponse response) {

        return new ExistingRsvpResponse(
            response.getGuestName(),
            response.getContact(),
            response.getAttendanceConfirmed(),
            response.getAttendeeCount(),
            response.getIntolerances(),
            response.getAdditionalComment()
        );
    }

    private String normalizeAccessCode(
            String accessCode) {

        if (accessCode == null
                || accessCode.isBlank()) {

            throw new InvitationNotFoundException();
        }

        return accessCode.trim();
    }

    private void verifyInvitationIsActive(
            Invitation invitation) {

        if (!Boolean.TRUE.equals(
                invitation.getIsActive())) {

            throw new InvitationDisabledException();
        }
    }

    private void verifyInvitationHasNotExpired(
            Invitation invitation) {

        LocalDateTime expiresAt =
            invitation.getExpiresAt();

        if (expiresAt == null) {
            return;
        }

        LocalDateTime nowUtc =
            LocalDateTime.now(ZoneOffset.UTC);

        if (!expiresAt.isAfter(nowUtc)) {
            throw new InvitationExpiredException();
        }
    }
}