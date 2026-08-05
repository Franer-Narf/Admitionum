package nc.admitionum.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import nc.admitionum.dto.publicapi.InvitationPublicResponse;
import nc.admitionum.exception.GlobalExceptionHandler;
import nc.admitionum.exception.InvitationDisabledException;
import nc.admitionum.exception.InvitationExpiredException;
import nc.admitionum.exception.InvitationNotFoundException;
import nc.admitionum.service.InvitationService;

@WebMvcTest(PublicInvitationController.class)
@Import(GlobalExceptionHandler.class)
class PublicInvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvitationService invitationService;

    @Test
    void shouldReturnPublicInvitation() throws Exception {
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
                jsonPath("$.expiresAt")
                    .value(
                        "2027-05-01T22:00:00"
                    )
            )
            .andExpect(
                jsonPath("$.existingResponse")
                    .value(nullValue())
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
            ).accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isGone())
            .andExpect(
                jsonPath("$.success")
                    .value(false)
            )
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
            ).accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isGone())
            .andExpect(
                jsonPath("$.success")
                    .value(false)
            )
            .andExpect(
                jsonPath("$.error.code")
                    .value(
                        "INVITATION_EXPIRED"
                    )
            );
    }
}