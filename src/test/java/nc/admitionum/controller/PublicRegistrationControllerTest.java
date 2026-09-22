package nc.admitionum.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import nc.admitionum.config.SecurityConfig;
import nc.admitionum.dto.publicapi.SaveRsvpRequest;
import nc.admitionum.dto.publicapi.SaveRsvpResponse;
import nc.admitionum.exception.GlobalExceptionHandler;
import nc.admitionum.exception.InvalidAttendeeCountException;
import nc.admitionum.service.InvitationService;

@WebMvcTest(PublicRegistrationController.class)
@Import({
    GlobalExceptionHandler.class,
    SecurityConfig.class
})
@ActiveProfiles("test")
class PublicRegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvitationService invitationService;

    @Test
    void shouldRegisterPublicResponse()
            throws Exception {

        LocalDateTime updatedAt =
            LocalDateTime.of(
                2027,
                4,
                1,
                17,
                30
            );

        SaveRsvpResponse serviceResponse =
            new SaveRsvpResponse(
                true,
                "Tu respuesta se ha guardado correctamente.",
                updatedAt
            );

        given(
            invitationService
                .registerPublicResponse(
                    any(SaveRsvpRequest.class)
                )
        ).willReturn(serviceResponse);

        String requestBody = """
            {
              "guestName": "Ana García",
              "contact": "ana@example.com",
              "attendanceConfirmed": true,
              "attendeeCount": 3,
              "intolerances": "Sin gluten",
              "additionalComment": "Llegaremos el viernes"
            }
            """;

        mockMvc.perform(
            post("/api/public/registrations")
                .contentType(
                    MediaType.APPLICATION_JSON
                )
                .accept(
                    MediaType.APPLICATION_JSON
                )
                .content(requestBody)
        )
            .andExpect(status().isCreated())
            .andExpect(
                content()
                    .contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                    )
            )
            .andExpect(
                jsonPath("$.success")
                    .value(true)
            )
            .andExpect(
                jsonPath("$.message")
                    .value(
                        "Tu respuesta se ha "
                            + "guardado correctamente."
                    )
            )
            .andExpect(
                jsonPath("$.updatedAt")
                    .value(
                        "2027-04-01T17:30:00"
                    )
            );

        ArgumentCaptor<SaveRsvpRequest> captor =
            ArgumentCaptor.forClass(
                SaveRsvpRequest.class
            );

        verify(invitationService)
            .registerPublicResponse(
                captor.capture()
            );

        SaveRsvpRequest capturedRequest =
            captor.getValue();

        assertThat(
            capturedRequest.getGuestName()
        ).isEqualTo("Ana García");

        assertThat(
            capturedRequest.getContact()
        ).isEqualTo("ana@example.com");

        assertThat(
            capturedRequest
                .getAttendanceConfirmed()
        ).isTrue();

        assertThat(
            capturedRequest.getAttendeeCount()
        ).isEqualTo(3);
    }

    @Test
    void shouldRejectInvalidPublicRegistration()
            throws Exception {

        String requestBody = """
            {
              "guestName": " ",
              "contact": "",
              "attendanceConfirmed": true,
              "attendeeCount": 21,
              "intolerances": "",
              "additionalComment": ""
            }
            """;

        mockMvc.perform(
            post("/api/public/registrations")
                .contentType(
                    MediaType.APPLICATION_JSON
                )
                .accept(
                    MediaType.APPLICATION_JSON
                )
                .content(requestBody)
        )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.success")
                    .value(false)
            )
            .andExpect(
                jsonPath("$.error.code")
                    .value("VALIDATION_ERROR")
            )
            .andExpect(
                jsonPath(
                    "$.error.fields.guestName"
                ).exists()
            )
            .andExpect(
                jsonPath(
                    "$.error.fields.contact"
                ).exists()
            )
            .andExpect(
                jsonPath(
                    "$.error.fields.attendeeCount"
                ).exists()
            );

        verifyNoInteractions(invitationService);
    }

    @Test
    void shouldReturnBadRequestForInvalidAttendeeCount()
            throws Exception {

        given(
            invitationService
                .registerPublicResponse(
                    any(SaveRsvpRequest.class)
                )
        ).willThrow(
            new InvalidAttendeeCountException(
                "Debe asistir al menos una persona."
            )
        );

        String requestBody = """
            {
              "guestName": "Ana García",
              "contact": "ana@example.com",
              "attendanceConfirmed": true,
              "attendeeCount": 0,
              "intolerances": "",
              "additionalComment": ""
            }
            """;

        mockMvc.perform(
            post("/api/public/registrations")
                .contentType(
                    MediaType.APPLICATION_JSON
                )
                .accept(
                    MediaType.APPLICATION_JSON
                )
                .content(requestBody)
        )
            .andExpect(status().isBadRequest())
            .andExpect(
                jsonPath("$.success")
                    .value(false)
            )
            .andExpect(
                jsonPath("$.error.code")
                    .value(
                        "INVALID_GUEST_COUNT"
                    )
            )
            .andExpect(
                jsonPath("$.error.message")
                    .value(
                        "Debe asistir al menos una persona."
                    )
            );
    }
}