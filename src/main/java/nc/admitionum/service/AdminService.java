package nc.admitionum.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nc.admitionum.dto.admin.AdminDashboardResponse;
import nc.admitionum.dto.admin.AdminRsvpResponse;
import nc.admitionum.model.Invitation;
import nc.admitionum.model.RsvpResponse;
import nc.admitionum.repository.InvitationRepository;
import nc.admitionum.repository.RsvpResponseRepository;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private final InvitationRepository invitationRepository;
    private final RsvpResponseRepository rsvpResponseRepository;

    public AdminService(
            InvitationRepository invitationRepository,
            RsvpResponseRepository rsvpResponseRepository) {

        this.invitationRepository = invitationRepository;
        this.rsvpResponseRepository =
            rsvpResponseRepository;
    }

    public AdminDashboardResponse getDashboard() {

        List<Invitation> invitations =
            invitationRepository.findAll();

        List<RsvpResponse> responses =
            rsvpResponseRepository.findAll();

        int totalInvitations =
            invitations.size();

        int answeredInvitations =
            responses.size();

        int pendingInvitations = 0;
        int confirmedInvitations = 0;
        int declinedInvitations = 0;
        int confirmedAttendees = 0;
        int responsesWithIntolerances = 0;

        Map<Integer, RsvpResponse>
                responsesByInvitationId =
                    createResponseMap(responses);

        LocalDateTime nowUtc =
            LocalDateTime.now(ZoneOffset.UTC);

        for (Invitation invitation : invitations) {

            RsvpResponse response =
                responsesByInvitationId.get(
                    invitation.getId()
                );

            String status =
                determineStatus(
                    invitation,
                    response,
                    nowUtc
                );

            if ("PENDING".equals(status)) {
                pendingInvitations++;
            }
        }

        for (RsvpResponse response : responses) {

            if (Boolean.TRUE.equals(
                    response.getAttendanceConfirmed())) {

                confirmedInvitations++;

                Integer attendeeCount =
                    response.getAttendeeCount();

                if (attendeeCount != null) {
                    confirmedAttendees +=
                        attendeeCount;
                }

                if (hasText(
                        response.getIntolerances())) {

                    responsesWithIntolerances++;
                }

            } else {
                declinedInvitations++;
            }
        }

        return new AdminDashboardResponse(
            totalInvitations,
            answeredInvitations,
            pendingInvitations,
            confirmedInvitations,
            declinedInvitations,
            confirmedAttendees,
            responsesWithIntolerances
        );
    }

    public List<AdminRsvpResponse> getResponses() {

        List<Invitation> invitations =
            invitationRepository.findAll();

        List<RsvpResponse> responses =
            rsvpResponseRepository.findAll();

        Map<Integer, RsvpResponse>
                responsesByInvitationId =
                    createResponseMap(responses);

        LocalDateTime nowUtc =
            LocalDateTime.now(ZoneOffset.UTC);

        List<AdminRsvpResponse> result =
            new ArrayList<>();

        for (Invitation invitation : invitations) {

            RsvpResponse response =
                responsesByInvitationId.get(
                    invitation.getId()
                );

            result.add(
                createAdminResponse(
                    invitation,
                    response,
                    nowUtc
                )
            );
        }

        result.sort(
            Comparator.comparing(
                AdminRsvpResponse::getDisplayName,
                String.CASE_INSENSITIVE_ORDER
            )
        );

        return result;
    }

    private Map<Integer, RsvpResponse>
            createResponseMap(
                    List<RsvpResponse> responses) {

        Map<Integer, RsvpResponse> result =
            new HashMap<>();

        for (RsvpResponse response : responses) {

            Invitation invitation =
                response.getInvitation();

            if (invitation != null
                    && invitation.getId() != null) {

                result.put(
                    invitation.getId(),
                    response
                );
            }
        }

        return result;
    }

    private AdminRsvpResponse createAdminResponse(
            Invitation invitation,
            RsvpResponse response,
            LocalDateTime nowUtc) {

        String status =
            determineStatus(
                invitation,
                response,
                nowUtc
            );

        if (response == null) {

            return new AdminRsvpResponse(
                invitation.getId(),
                invitation.getDisplayName(),
                invitation.getMaxGuests(),
                status,
                null,
                null,
                null,
                null,
                null,
                null
            );
        }

        return new AdminRsvpResponse(
            invitation.getId(),
            invitation.getDisplayName(),
            invitation.getMaxGuests(),
            status,
            response.getGuestName(),
            response.getContact(),
            response.getAttendeeCount(),
            response.getIntolerances(),
            response.getAdditionalComment(),
            response.getUpdatedAt()
        );
    }

    private String determineStatus(
            Invitation invitation,
            RsvpResponse response,
            LocalDateTime nowUtc) {

        if (response != null) {

            if (Boolean.TRUE.equals(
                    response.getAttendanceConfirmed())) {

                return "CONFIRMED";
            }

            return "DECLINED";
        }

        if (!Boolean.TRUE.equals(
                invitation.getIsActive())) {

            return "DISABLED";
        }

        LocalDateTime expiresAt =
            invitation.getExpiresAt();

        if (expiresAt != null
                && !expiresAt.isAfter(nowUtc)) {

            return "EXPIRED";
        }

        return "PENDING";
    }

    private boolean hasText(String value) {

        return value != null
            && !value.isBlank();
    }
}