package nc.admitionum.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import nc.admitionum.dto.publicapi.InvitationPublicResponse;
import nc.admitionum.exception.InvitationDisabledException;
import nc.admitionum.exception.InvitationExpiredException;
import nc.admitionum.exception.InvitationNotFoundException;
import nc.admitionum.model.Invitation;
import nc.admitionum.repository.InvitationRepository;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private InvitationRepository invitationRepository;

    @InjectMocks
    private InvitationService invitationService;

    @Test
    void shouldReturnPublicInvitationWhenCodeIsValid() {
        LocalDateTime expiresAt =
            LocalDateTime
                .now(ZoneOffset.UTC)
                .plusDays(30)
                .withNano(0);

        Invitation invitation = new Invitation(
            "DEMO-FAMILY-001",
            "Familia García",
            4,
            true,
            expiresAt
        );

        given(
            invitationRepository.findByAccessCode(
                "DEMO-FAMILY-001"
            )
        ).willReturn(
            Optional.of(invitation)
        );

        InvitationPublicResponse response =
            invitationService
                .getPublicInvitation(
                    "DEMO-FAMILY-001"
                );

        assertThat(response.getDisplayName())
            .isEqualTo("Familia García");

        assertThat(response.getMaxGuests())
            .isEqualTo(4);

        assertThat(response.getExpiresAt())
            .isEqualTo(expiresAt);

        assertThat(response.getExistingResponse())
            .isNull();

        verify(invitationRepository)
            .findByAccessCode(
                "DEMO-FAMILY-001"
            );
    }

    @Test
    void shouldRejectUnknownAccessCode() {
        given(
            invitationRepository.findByAccessCode(
                "UNKNOWN-CODE"
            )
        ).willReturn(
            Optional.empty()
        );

        assertThatThrownBy(
            () -> invitationService
                .getPublicInvitation(
                    "UNKNOWN-CODE"
                )
        ).isInstanceOf(
            InvitationNotFoundException.class
        );
    }

    @Test
    void shouldRejectDisabledInvitation() {
        Invitation invitation = new Invitation(
            "DISABLED-CODE",
            "Invitación desactivada",
            2,
            false,
            null
        );

        given(
            invitationRepository.findByAccessCode(
                "DISABLED-CODE"
            )
        ).willReturn(
            Optional.of(invitation)
        );

        assertThatThrownBy(
            () -> invitationService
                .getPublicInvitation(
                    "DISABLED-CODE"
                )
        ).isInstanceOf(
            InvitationDisabledException.class
        );
    }

    @Test
    void shouldRejectExpiredInvitation() {
        Invitation invitation = new Invitation(
            "EXPIRED-CODE",
            "Invitación expirada",
            2,
            true,
            LocalDateTime
                .now(ZoneOffset.UTC)
                .minusDays(1)
        );

        given(
            invitationRepository.findByAccessCode(
                "EXPIRED-CODE"
            )
        ).willReturn(
            Optional.of(invitation)
        );

        assertThatThrownBy(
            () -> invitationService
                .getPublicInvitation(
                    "EXPIRED-CODE"
                )
        ).isInstanceOf(
            InvitationExpiredException.class
        );
    }
}