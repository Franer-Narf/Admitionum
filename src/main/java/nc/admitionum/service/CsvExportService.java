package nc.admitionum.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nc.admitionum.dto.admin.AdminRsvpResponse;
import nc.admitionum.model.Invitation;
import nc.admitionum.model.RsvpResponse;
import nc.admitionum.repository.RsvpResponseRepository;

@Service
@Transactional(readOnly = true)
public class CsvExportService {

    private static final String HEADER =
        "Invitacion,"
            + "NombreInvitado,"
            + "Contacto,"
            + "Estado,"
            + "NumeroAsistentes,"
            + "Intolerancias,"
            + "ComentarioAdicional,"
            + "FechaEnvio,"
            + "UltimaActualizacion";

    private static final String LINE_SEPARATOR =
        "\r\n";

    private static final DateTimeFormatter
            DATE_TIME_FORMATTER =
                DateTimeFormatter.ofPattern(
                    "uuuu-MM-dd'T'HH:mm:ss"
                );

    private final AdminService adminService;

    private final RsvpResponseRepository
        rsvpResponseRepository;

    public CsvExportService(
            AdminService adminService,
            RsvpResponseRepository
                rsvpResponseRepository) {

        this.adminService = adminService;
        this.rsvpResponseRepository =
            rsvpResponseRepository;
    }

    public String exportResponses() {

        List<AdminRsvpResponse> adminResponses =
            adminService.getResponses();

        List<RsvpResponse> storedResponses =
            rsvpResponseRepository.findAll();

        Map<Integer, RsvpResponse>
                responsesByInvitationId =
                    createResponseMap(
                        storedResponses
                    );

        StringBuilder csv =
            new StringBuilder();

        csv.append(HEADER)
            .append(LINE_SEPARATOR);

        for (AdminRsvpResponse adminResponse
                : adminResponses) {

            RsvpResponse storedResponse =
                responsesByInvitationId.get(
                    adminResponse.getInvitationId()
                );

            appendRow(
                csv,
                adminResponse,
                storedResponse
            );
        }

        return csv.toString();
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

    private void appendRow(
            StringBuilder csv,
            AdminRsvpResponse adminResponse,
            RsvpResponse storedResponse) {

        appendCsvValue(
            csv,
            adminResponse.getDisplayName()
        );

        csv.append(',');

        appendCsvValue(
            csv,
            adminResponse.getGuestName()
        );

        csv.append(',');

        appendCsvValue(
            csv,
            adminResponse.getContact()
        );

        csv.append(',');

        appendCsvValue(
            csv,
            adminResponse.getStatus()
        );

        csv.append(',');

        appendCsvValue(
            csv,
            adminResponse.getAttendeeCount()
        );

        csv.append(',');

        appendCsvValue(
            csv,
            adminResponse.getIntolerances()
        );

        csv.append(',');

        appendCsvValue(
            csv,
            adminResponse.getAdditionalComment()
        );

        csv.append(',');

        appendCsvValue(
            csv,
            storedResponse == null
                ? null
                : formatDateTime(
                    storedResponse.getSubmittedAt()
                )
        );

        csv.append(',');

        appendCsvValue(
            csv,
            formatDateTime(
                adminResponse.getUpdatedAt()
            )
        );

        csv.append(LINE_SEPARATOR);
    }

    private void appendCsvValue(
            StringBuilder csv,
            Object value) {

        String text =
            value == null
                ? ""
                : String.valueOf(value);

        String safeText =
            neutralizeFormula(text);

        String escapedText =
            safeText.replace(
                "\"",
                "\"\""
            );

        csv.append('"')
            .append(escapedText)
            .append('"');
    }

    private String neutralizeFormula(
            String value) {

        if (value.isEmpty()) {
            return value;
        }

        String valueWithoutLeadingSpaces =
            value.stripLeading();

        if (valueWithoutLeadingSpaces.isEmpty()) {
            return value;
        }

        char firstCharacter =
            valueWithoutLeadingSpaces.charAt(0);

        if (firstCharacter == '='
                || firstCharacter == '+'
                || firstCharacter == '-'
                || firstCharacter == '@') {

            return "'" + value;
        }

        return value;
    }

    private String formatDateTime(
            LocalDateTime value) {

        if (value == null) {
            return "";
        }

        return DATE_TIME_FORMATTER
            .format(value);
    }
}