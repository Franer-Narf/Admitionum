package nc.admitionum.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import nc.admitionum.dto.admin.AdminRsvpResponse;
import nc.admitionum.model.Invitation;
import nc.admitionum.model.RsvpResponse;
import nc.admitionum.repository.RsvpResponseRepository;

@ExtendWith(MockitoExtension.class)
class CsvExportServiceTest {

    @Mock
    private AdminService adminService;

    @Mock
    private RsvpResponseRepository
        rsvpResponseRepository;

    @InjectMocks
    private CsvExportService csvExportService;

    @Test
    void shouldExportResponsesWithExpectedColumns() {

        LocalDateTime submittedAt =
            LocalDateTime.of(
                2026,
                8,
                1,
                10,
                0
            );

        LocalDateTime updatedAt =
            LocalDateTime.of(
                2026,
                8,
                2,
                11,
                30
            );

        AdminRsvpResponse confirmed =
            new AdminRsvpResponse(
                1,
                "Familia García",
                4,
                "CONFIRMED",
                "Ana García",
                "ana@example.com",
                3,
                "Lactosa",
                "Llegaremos el viernes",
                updatedAt
            );

        AdminRsvpResponse pending =
            new AdminRsvpResponse(
                2,
                "María López",
                1,
                "PENDING",
                null,
                null,
                null,
                null,
                null,
                null
            );

        Invitation invitation =
            new Invitation(
                "DEMO-FAMILY-001",
                "Familia García",
                4,
                true,
                null
            );

        invitation.setId(1);

        RsvpResponse storedResponse =
            new RsvpResponse(
                invitation,
                "Ana García",
                "ana@example.com",
                true,
                3,
                "Lactosa",
                "Llegaremos el viernes"
            );

        storedResponse.setSubmittedAt(
            submittedAt
        );

        storedResponse.setUpdatedAt(
            updatedAt
        );

        given(
            adminService.getResponses()
        ).willReturn(
            List.of(
                confirmed,
                pending
            )
        );

        given(
            rsvpResponseRepository.findAll()
        ).willReturn(
            List.of(storedResponse)
        );

        String csv =
            csvExportService.exportResponses();

        String expected =
            "Invitacion,"
                + "NombreInvitado,"
                + "Contacto,"
                + "Estado,"
                + "NumeroAsistentes,"
                + "Intolerancias,"
                + "ComentarioAdicional,"
                + "FechaEnvio,"
                + "UltimaActualizacion"
                + "\r\n"

                + "\"Familia García\","
                + "\"Ana García\","
                + "\"ana@example.com\","
                + "\"CONFIRMED\","
                + "\"3\","
                + "\"Lactosa\","
                + "\"Llegaremos el viernes\","
                + "\"2026-08-01T10:00:00\","
                + "\"2026-08-02T11:30:00\""
                + "\r\n"

                + "\"María López\","
                + "\"\","
                + "\"\","
                + "\"PENDING\","
                + "\"\","
                + "\"\","
                + "\"\","
                + "\"\","
                + "\"\""
                + "\r\n";

        assertThat(csv)
            .isEqualTo(expected);
    }

    @Test
    void shouldNeutralizeFormulaLikeValues() {

        LocalDateTime date =
            LocalDateTime.of(
                2026,
                8,
                2,
                11,
                30
            );

        AdminRsvpResponse response =
            new AdminRsvpResponse(
                1,
                "Familia Demo",
                2,
                "CONFIRMED",
                "=2+2",
                "+34 600 123 123",
                1,
                "-Sin gluten",
                "@SUM(A1:A2)",
                date
            );

        Invitation invitation =
            new Invitation(
                "DEMO-001",
                "Familia Demo",
                2,
                true,
                null
            );

        invitation.setId(1);

        RsvpResponse storedResponse =
            new RsvpResponse(
                invitation,
                "=2+2",
                "+34 600 123 123",
                true,
                1,
                "-Sin gluten",
                "@SUM(A1:A2)"
            );

        storedResponse.setSubmittedAt(date);
        storedResponse.setUpdatedAt(date);

        given(
            adminService.getResponses()
        ).willReturn(
            List.of(response)
        );

        given(
            rsvpResponseRepository.findAll()
        ).willReturn(
            List.of(storedResponse)
        );

        String csv =
            csvExportService.exportResponses();

        assertThat(csv)
            .contains("\"'=2+2\"");

        assertThat(csv)
            .contains(
                "\"'+34 600 123 123\""
            );

        assertThat(csv)
            .contains("\"'-Sin gluten\"");

        assertThat(csv)
            .contains(
                "\"'@SUM(A1:A2)\""
            );
    }

    @Test
    void shouldEscapeQuotesCommasAndLineBreaks() {

        LocalDateTime date =
            LocalDateTime.of(
                2026,
                8,
                2,
                11,
                30
            );

        String comment =
            "Llegaremos, \"quizá\"\n"
                + "el viernes";

        AdminRsvpResponse response =
            new AdminRsvpResponse(
                1,
                "Familia Demo",
                2,
                "CONFIRMED",
                "Ana",
                "ana@example.com",
                1,
                "",
                comment,
                date
            );

        Invitation invitation =
            new Invitation(
                "DEMO-001",
                "Familia Demo",
                2,
                true,
                null
            );

        invitation.setId(1);

        RsvpResponse storedResponse =
            new RsvpResponse(
                invitation,
                "Ana",
                "ana@example.com",
                true,
                1,
                "",
                comment
            );

        storedResponse.setSubmittedAt(date);
        storedResponse.setUpdatedAt(date);

        given(
            adminService.getResponses()
        ).willReturn(
            List.of(response)
        );

        given(
            rsvpResponseRepository.findAll()
        ).willReturn(
            List.of(storedResponse)
        );

        String csv =
            csvExportService.exportResponses();

        assertThat(csv)
            .contains(
                "\"Llegaremos, \"\"quizá\"\"\n"
                    + "el viernes\""
            );
    }
}