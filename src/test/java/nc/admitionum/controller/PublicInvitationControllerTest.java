package nc.admitionum.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import nc.admitionum.dto.publicapi.ExistingRsvpResponse;
import nc.admitionum.dto.publicapi.InvitationPublicResponse;
import nc.admitionum.dto.publicapi.SaveRsvpRequest;
import nc.admitionum.dto.publicapi.SaveRsvpResponse;
import nc.admitionum.exception.GlobalExceptionHandler;
import nc.admitionum.exception.InvitationDisabledException;
import nc.admitionum.exception.InvitationExpiredException;
import nc.admitionum.exception.InvitationNotFoundException;
import nc.admitionum.service.InvitationService;

import static org.mockito.Mockito.verifyNoInteractions;

import nc.admitionum.exception.InvalidAttendeeCountException;

@WebMvcTest(PublicInvitationController.class)
@Import(GlobalExceptionHandler.class)
class PublicInvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvitationService invitationService;

    @Test
    void shouldReturnPublicInvitationWithoutResponse()
            throws Exception {

        InvitationPublicResponse response =
            new InvitationPublicResponse(
                "Familia García",
                4,
                LocalDateTime.of(
                    2027,
                    5,
                    1,
                    22,
                    0
                ),
                null
            );

        given(
            invitationService.getPublicInvitation(
                "DEMO-FAMILY-001"
            )
        ).willReturn(response);

        mockMvc.perform(
            get(
                "/api/public/invitations/"
                    + "DEMO-FAMILY-001"
            ).accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(
                content()
                    .contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                    )
            )
            .andExpect(
                jsonPath("$.displayName")
                    .value("Familia García")
            )
            .andExpect(
                jsonPath("$.maxGuests")
                    .value(4)
            )
            .andExpect(
                jsonPath("$.existingResponse")
                    .value(nullValue())
            );
    }

    @Test
    void shouldReturnPublicInvitationWithResponse()
            throws Exception {

        ExistingRsvpResponse existingResponse =
            new ExistingRsvpResponse(
                "Ana García",
                "ana@example.com",
                true,
                3,
                "Intolerancia a la lactosa",
                "Llegaremos el viernes"
            );

        InvitationPublicResponse response =
            new InvitationPublicResponse(
                "Familia García",
                4,
                LocalDateTime.of(
                    2027,
                    5,
                    1,
                    22,
                    0
                ),
                existingResponse
            );

        given(
            invitationService.getPublicInvitation(
                "DEMO-FAMILY-001"
            )
        ).willReturn(response);

        mockMvc.perform(
            get(
                "/api/public/invitations/"
                    + "DEMO-FAMILY-001"
            ).accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath(
                    "$.existingResponse.guestName"
                ).value("Ana García")
            )
            .andExpect(
                jsonPath(
                    "$.existingResponse.contact"
                ).value("ana@example.com")
            )
            .andExpect(
                jsonPath(
                    "$.existingResponse"
                        + ".attendanceConfirmed"
                ).value(true)
            )
            .andExpect(
                jsonPath(
                    "$.existingResponse.attendeeCount"
                ).value(3)
            );
    }

    @Test
    void shouldSaveRsvpResponse()
            throws Exception {

        LocalDateTime updatedAt =
            LocalDateTime.of(
                2027,
                3,
                15,
                17,
                30
            );

        SaveRsvpResponse response =
            new SaveRsvpResponse(
                true,
                "Tu respuesta se ha guardado correctamente.",
                updatedAt
            );

        given(
            invitationService.saveResponse(
                eq("DEMO-FAMILY-001"),
                any(SaveRsvpRequest.class)
            )
        ).willReturn(response);

        String requestBody = """
            {
              "guestName": "Ana García",
              "contact": "ana@example.com",
              "attendanceConfirmed": true,
              "attendeeCount": 3,
              "intolerances": "Intolerancia a la lactosa",
              "additionalComment": "Llegaremos el viernes"
            }
            """;

        mockMvc.perform(
            put(
                "/api/public/invitations/"
                    + "DEMO-FAMILY-001/response"
            )
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk())
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
                        "2027-03-15T17:30:00"
                    )
            );

        ArgumentCaptor<SaveRsvpRequest> captor =
            ArgumentCaptor.forClass(
                SaveRsvpRequest.class
            );

        verify(invitationService)
            .saveResponse(
                eq("DEMO-FAMILY-001"),
                captor.capture()
            );

        SaveRsvpRequest capturedRequest =
            captor.getValue();

        assertThat(capturedRequest.getGuestName())
            .isEqualTo("Ana García");

        assertThat(capturedRequest.getContact())
            .isEqualTo("ana@example.com");

        assertThat(
            capturedRequest.getAttendanceConfirmed()
        ).isTrue();

        assertThat(capturedRequest.getAttendeeCount())
            .isEqualTo(3);
    }

    @Test
    void shouldReturnNotFoundWhenSavingUnknownCode()
            throws Exception {

        given(
            invitationService.saveResponse(
                eq("UNKNOWN-CODE"),
                any(SaveRsvpRequest.class)
            )
        ).willThrow(
            new InvitationNotFoundException()
        );

        String requestBody = """
            {
              "guestName": "Ana García",
              "contact": "ana@example.com",
              "attendanceConfirmed": true,
              "attendeeCount": 2,
              "intolerances": "",
              "additionalComment": ""
            }
            """;

        mockMvc.perform(
            put(
                "/api/public/invitations/"
                    + "UNKNOWN-CODE/response"
            )
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.success")
                    .value(false)
            )
            .andExpect(
                jsonPath("$.error.code")
                    .value(
                        "INVITATION_NOT_FOUND"
                    )
            );
    }

    @Test
    void shouldReturnNotFoundForUnknownCode()
            throws Exception {

        given(
            invitationService.getPublicInvitation(
                "UNKNOWN-CODE"
            )
        ).willThrow(
            new InvitationNotFoundException()
        );

        mockMvc.perform(
            get(
                "/api/public/invitations/"
                    + "UNKNOWN-CODE"
            ).accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound())
            .andExpect(
                jsonPath("$.error.code")
                    .value(
                        "INVITATION_NOT_FOUND"
                    )
            );
    }

    @Test
    void shouldReturnGoneForDisabledInvitation()
            throws Exception {

        given(
            invitationService.getPublicInvitation(
                "DISABLED-CODE"
            )
        ).willThrow(
            new InvitationDisabledException()
        );

        mockMvc.perform(
            get(
                "/api/public/invitations/"
                    + "DISABLED-CODE"
            )
        )
            .andExpect(status().isGone())
            .andExpect(
                jsonPath("$.error.code")
                    .value(
                        "INVITATION_DISABLED"
                    )
            );
    }

    @Test
    void shouldReturnGoneForExpiredInvitation()
            throws Exception {

        given(
            invitationService.getPublicInvitation(
                "EXPIRED-CODE"
            )
        ).willThrow(
            new InvitationExpiredException()
        );

        mockMvc.perform(
            get(
                "/api/public/invitations/"
                    + "EXPIRED-CODE"
            )
        )
            .andExpect(status().isGone())
            .andExpect(
                jsonPath("$.error.code")
                    .value(
                        "INVITATION_EXPIRED"
                    )
            );
    }

    @Test
    void shouldReturnValidationErrorsForInvalidRequest()
        throws Exception {

    String requestBody = """
        {
          "guestName": " ",
          "contact": "",
          "attendanceConfirmed": true,
          "attendeeCount": 11,
          "intolerances": "",
          "additionalComment": ""
        }
        """;

    mockMvc.perform(
        put(
            "/api/public/invitations/"
                + "DEMO-FAMILY-001/response"
        )
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
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
            jsonPath("$.error.message")
                .value(
                    "Revisa los campos indicados."
                )
        )
        .andExpect(
            jsonPath("$.error.fields.guestName")
                .exists()
        )
        .andExpect(
            jsonPath("$.error.fields.contact")
                .exists()
        )
        .andExpect(
            jsonPath("$.error.fields.attendeeCount")
                .exists()
        );

    verifyNoInteractions(invitationService);
    }

    @Test
    void shouldReturnBadRequestForInvalidGuestCount()
        throws Exception {

    given(
        invitationService.saveResponse(
            eq("DEMO-FAMILY-001"),
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
        put(
            "/api/public/invitations/"
                + "DEMO-FAMILY-001/response"
        )
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .content(requestBody)
    )
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.success")
                .value(false)
        )
        .andExpect(
            jsonPath("$.error.code")
                .value("INVALID_GUEST_COUNT")
        )
        .andExpect(
            jsonPath("$.error.message")
                .value(
                    "Debe asistir al menos una persona."
                )
        );
    }
}