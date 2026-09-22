package nc.admitionum;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import nc.admitionum.dto.admin.AdminRsvpResponse;
import nc.admitionum.dto.publicapi.SaveRsvpRequest;
import nc.admitionum.dto.publicapi.SaveRsvpResponse;
import nc.admitionum.model.Invitation;
import nc.admitionum.model.RsvpResponse;
import nc.admitionum.repository.InvitationRepository;
import nc.admitionum.repository.RsvpResponseRepository;
import nc.admitionum.service.AdminService;
import nc.admitionum.service.CsvExportService;
import nc.admitionum.service.InvitationService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PublicRegistrationFlowTest {

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private RsvpResponseRepository
        rsvpResponseRepository;

    @Autowired
    private AdminService adminService;

    @Autowired
    private CsvExportService csvExportService;

    @Test
    void shouldPersistAndExposePublicRegistration() {

        rsvpResponseRepository.deleteAll();
        invitationRepository.deleteAll();

        SaveRsvpRequest request =
            new SaveRsvpRequest(
                "Ana García",
                "ana@example.com",
                true,
                3,
                "Sin gluten",
                "Llegaremos el viernes"
            );

        SaveRsvpResponse result =
            invitationService
                .registerPublicResponse(request);

        List<Invitation> invitations =
            invitationRepository.findAll();

        assertThat(invitations)
            .hasSize(1);

        Invitation invitation =
            invitations.get(0);

        assertThat(invitation.getAccessCode())
            .startsWith("REG-")
            .hasSize(40);

        assertThat(invitation.getDisplayName())
            .isEqualTo("Ana García");

        assertThat(invitation.getMaxGuests())
            .isEqualTo(20);

        assertThat(invitation.getIsActive())
            .isTrue();

        assertThat(invitation.getExpiresAt())
            .isNull();

        RsvpResponse storedResponse =
            rsvpResponseRepository
                .findByInvitationId(
                    invitation.getId()
                )
                .orElseThrow();

        assertThat(
            storedResponse.getInvitation().getId()
        ).isEqualTo(invitation.getId());

        assertThat(storedResponse.getGuestName())
            .isEqualTo("Ana García");

        assertThat(storedResponse.getContact())
            .isEqualTo("ana@example.com");

        assertThat(
            storedResponse
                .getAttendanceConfirmed()
        ).isTrue();

        assertThat(storedResponse.getAttendeeCount())
            .isEqualTo(3);

        assertThat(result.isSuccess())
            .isTrue();

        assertThat(result.getUpdatedAt())
            .isNotNull();

        List<AdminRsvpResponse>
                adminResponses =
            adminService.getResponses();

        assertThat(adminResponses)
            .hasSize(1);

        AdminRsvpResponse adminResponse =
            adminResponses.get(0);

        assertThat(adminResponse.getDisplayName())
            .isEqualTo("Ana García");

        assertThat(adminResponse.getGuestName())
            .isEqualTo("Ana García");

        assertThat(adminResponse.getStatus())
            .isEqualTo("CONFIRMED");

        assertThat(adminResponse.getAttendeeCount())
            .isEqualTo(3);

        String csv =
            csvExportService.exportResponses();

        assertThat(csv)
            .contains(
                "Ana García",
                "ana@example.com",
                "CONFIRMED",
                "Sin gluten"
            );
    }
}