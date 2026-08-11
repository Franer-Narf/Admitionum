package nc.admitionum.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import nc.admitionum.dto.admin.AdminDashboardResponse;
import nc.admitionum.dto.admin.AdminRsvpResponse;
import nc.admitionum.model.Invitation;
import nc.admitionum.model.RsvpResponse;
import nc.admitionum.repository.InvitationRepository;
import nc.admitionum.repository.RsvpResponseRepository;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private RsvpResponseRepository rsvpResponseRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void shouldCalculateDashboard() {

        Invitation invitation1 =
            createInvitation(
                1,
                "Familia Uno",
                true,
                null
            );

        Invitation invitation2 =
            createInvitation(
                2,
                "Familia Dos",
                true,
                null
            );

        Invitation invitation3 =
            createInvitation(
                3,
                "Familia Tres",
                true,
                null
            );

        Invitation invitation4 =
            createInvitation(
                4,
                "Familia Cuatro",
                true,
                null
            );

        RsvpResponse response1 =
            createResponse(
                1,
                invitation1,
                true,
                3,
                "Intolerancia a la lactosa"
            );

        RsvpResponse response2 =
            createResponse(
                2,
                invitation2,
                true,
                1,
                null
            );

        RsvpResponse response3 =
            createResponse(
                3,
                invitation3,
                false,
                0,
                null
            );

        given(
            invitationRepository.findAll()
        ).willReturn(
            List.of(
                invitation1,
                invitation2,
                invitation3,
                invitation4
            )
        );

        given(
            rsvpResponseRepository.findAll()
        ).willReturn(
            List.of(
                response1,
                response2,
                response3
            )
        );

        AdminDashboardResponse result =
            adminService.getDashboard();

        assertThat(
            result.getTotalInvitations()
        ).isEqualTo(4);

        assertThat(
            result.getAnsweredInvitations()
        ).isEqualTo(3);

        assertThat(
            result.getPendingInvitations()
        ).isEqualTo(1);

        assertThat(
            result.getConfirmedInvitations()
        ).isEqualTo(2);

        assertThat(
            result.getDeclinedInvitations()
        ).isEqualTo(1);

        assertThat(
            result.getConfirmedAttendees()
        ).isEqualTo(4);

        assertThat(
            result.getResponsesWithIntolerances()
        ).isEqualTo(1);
    }

    @Test
    void shouldReturnInvitationsWithCalculatedStatuses() {

        LocalDateTime nowUtc =
            LocalDateTime.now(ZoneOffset.UTC);

        Invitation confirmed =
            createInvitation(
                1,
                "A Confirmada",
                true,
                null
            );

        Invitation declined =
            createInvitation(
                2,
                "B Rechazada",
                true,
                null
            );

        Invitation pending =
            createInvitation(
                3,
                "C Pendiente",
                true,
                null
            );

        Invitation disabled =
            createInvitation(
                4,
                "D Desactivada",
                false,
                null
            );

        Invitation expired =
            createInvitation(
                5,
                "E Expirada",
                true,
                nowUtc.minusDays(1)
            );

        RsvpResponse confirmedResponse =
            createResponse(
                1,
                confirmed,
                true,
                2,
                "Sin gluten"
            );

        RsvpResponse declinedResponse =
            createResponse(
                2,
                declined,
                false,
                0,
                null
            );

        given(
            invitationRepository.findAll()
        ).willReturn(
            List.of(
                confirmed,
                declined,
                pending,
                disabled,
                expired
            )
        );

        given(
            rsvpResponseRepository.findAll()
        ).willReturn(
            List.of(
                confirmedResponse,
                declinedResponse
            )
        );

        List<AdminRsvpResponse> result =
            adminService.getResponses();

        assertThat(result)
            .hasSize(5);

        assertThat(result)
            .extracting(
                AdminRsvpResponse::getStatus
            )
            .containsExactly(
                "CONFIRMED",
                "DECLINED",
                "PENDING",
                "DISABLED",
                "EXPIRED"
            );

        assertThat(
            result.get(0).getGuestName()
        ).isEqualTo("Invitado 1");

        assertThat(
            result.get(0).getAttendeeCount()
        ).isEqualTo(2);

        assertThat(
            result.get(2).getGuestName()
        ).isNull();

        assertThat(
            result.get(2).getAttendeeCount()
        ).isNull();
    }

    private Invitation createInvitation(
            Integer id,
            String displayName,
            Boolean isActive,
            LocalDateTime expiresAt) {

        Invitation invitation =
            new Invitation(
                "ADMIN-CODE-" + id,
                displayName,
                4,
                isActive,
                expiresAt
            );

        invitation.setId(id);

        return invitation;
    }

    private RsvpResponse createResponse(
            Integer id,
            Invitation invitation,
            Boolean attendanceConfirmed,
            Integer attendeeCount,
            String intolerances) {

        RsvpResponse response =
            new RsvpResponse(
                invitation,
                "Invitado " + id,
                "contacto" + id + "@example.com",
                attendanceConfirmed,
                attendeeCount,
                intolerances,
                "Comentario " + id
            );

        response.setId(id);

        LocalDateTime submittedAt =
            LocalDateTime.of(
                2026,
                8,
                1,
                10,
                0
            ).plusMinutes(id);

        response.setSubmittedAt(submittedAt);
        response.setUpdatedAt(
            submittedAt.plusHours(1)
        );

        return response;
    }
}