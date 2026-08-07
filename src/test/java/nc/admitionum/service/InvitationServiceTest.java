package nc.admitionum.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import nc.admitionum.dto.publicapi.InvitationPublicResponse;
import nc.admitionum.dto.publicapi.SaveRsvpRequest;
import nc.admitionum.dto.publicapi.SaveRsvpResponse;
import nc.admitionum.exception.InvitationDisabledException;
import nc.admitionum.exception.InvitationExpiredException;
import nc.admitionum.exception.InvitationNotFoundException;
import nc.admitionum.model.Invitation;
import nc.admitionum.model.RsvpResponse;
import nc.admitionum.repository.InvitationRepository;
import nc.admitionum.repository.RsvpResponseRepository;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTest {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private RsvpResponseRepository rsvpResponseRepository;

    @InjectMocks
    private InvitationService invitationService;

    @Test
    void shouldReturnPublicInvitationWithoutResponse() {
        Invitation invitation =
            createValidInvitation();

        given(
            invitationRepository.findByAccessCode(
                "DEMO-FAMILY-001"
            )
        ).willReturn(
            Optional.of(invitation)
        );

        given(
            rsvpResponseRepository.findByInvitationId(1)
        ).willReturn(
            Optional.empty()
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

        assertThat(response.getExistingResponse())
            .isNull();

        verify(rsvpResponseRepository)
            .findByInvitationId(1);
    }

    @Test
    void shouldReturnPublicInvitationWithExistingResponse() {
        Invitation invitation =
            createValidInvitation();

        RsvpResponse storedResponse =
            new RsvpResponse(
                invitation,
                "Ana García",
                "ana@example.com",
                true,
                3,
                "Intolerancia a la lactosa",
                "Llegaremos el viernes"
            );

        storedResponse.setId(10);

        given(
            invitationRepository.findByAccessCode(
                "DEMO-FAMILY-001"
            )
        ).willReturn(
            Optional.of(invitation)
        );

        given(
            rsvpResponseRepository.findByInvitationId(1)
        ).willReturn(
            Optional.of(storedResponse)
        );

        InvitationPublicResponse response =
            invitationService
                .getPublicInvitation(
                    "DEMO-FAMILY-001"
                );

        assertThat(response.getExistingResponse())
            .isNotNull();

        assertThat(
            response
                .getExistingResponse()
                .getGuestName()
        ).isEqualTo("Ana García");

        assertThat(
            response
                .getExistingResponse()
                .getContact()
        ).isEqualTo("ana@example.com");

        assertThat(
            response
                .getExistingResponse()
                .getAttendanceConfirmed()
        ).isTrue();

        assertThat(
            response
                .getExistingResponse()
                .getAttendeeCount()
        ).isEqualTo(3);
    }

    @Test
    void shouldCreateResponseWhenNoneExists() {
        Invitation invitation =
            createValidInvitation();

        SaveRsvpRequest request =
            createSaveRequest();

        LocalDateTime storedAt =
            LocalDateTime.of(
                2027,
                3,
                15,
                17,
                30
            );

        given(
            invitationRepository.findByAccessCode(
                "DEMO-FAMILY-001"
            )
        ).willReturn(
            Optional.of(invitation)
        );

        given(
            rsvpResponseRepository.findByInvitationId(1)
        ).willReturn(
            Optional.empty()
        );

        given(
            rsvpResponseRepository.saveAndFlush(
                any(RsvpResponse.class)
            )
        ).willAnswer(
            invocation -> {
                RsvpResponse savedResponse =
                    invocation.getArgument(0);

                savedResponse.setId(10);
                savedResponse.setUpdatedAt(storedAt);

                return savedResponse;
            }
        );

        SaveRsvpResponse result =
            invitationService.saveResponse(
                "DEMO-FAMILY-001",
                request
            );

        ArgumentCaptor<RsvpResponse> captor =
            ArgumentCaptor.forClass(
                RsvpResponse.class
            );

        verify(rsvpResponseRepository)
            .saveAndFlush(captor.capture());

        RsvpResponse capturedResponse =
            captor.getValue();

        assertThat(capturedResponse.getInvitation())
            .isSameAs(invitation);

        assertThat(capturedResponse.getGuestName())
            .isEqualTo("Ana García");

        assertThat(capturedResponse.getContact())
            .isEqualTo("ana@example.com");

        assertThat(
            capturedResponse.getAttendanceConfirmed()
        ).isTrue();

        assertThat(capturedResponse.getAttendeeCount())
            .isEqualTo(3);

        assertThat(result.isSuccess())
            .isTrue();

        assertThat(result.getMessage())
            .isEqualTo(
                "Tu respuesta se ha guardado correctamente."
            );

        assertThat(result.getUpdatedAt())
            .isEqualTo(storedAt);
    }

    @Test
    void shouldUpdateExistingResponse() {
        Invitation invitation =
            createValidInvitation();

        LocalDateTime submittedAt =
            LocalDateTime.of(
                2027,
                3,
                1,
                12,
                0
            );

        LocalDateTime originalUpdatedAt =
            LocalDateTime.of(
                2027,
                3,
                1,
                12,
                0
            );

        LocalDateTime newUpdatedAt =
            LocalDateTime.of(
                2027,
                3,
                15,
                18,
                0
            );

        RsvpResponse existingResponse =
            new RsvpResponse(
                invitation,
                "Ana García",
                "ana@example.com",
                true,
                2,
                null,
                null
            );

        existingResponse.setId(10);
        existingResponse.setSubmittedAt(submittedAt);
        existingResponse.setUpdatedAt(originalUpdatedAt);

        SaveRsvpRequest request =
            new SaveRsvpRequest(
                "Ana y familia",
                "600 123 123",
                true,
                4,
                "Sin gluten",
                "Llegaremos el sábado"
            );

        given(
            invitationRepository.findByAccessCode(
                "DEMO-FAMILY-001"
            )
        ).willReturn(
            Optional.of(invitation)
        );

        given(
            rsvpResponseRepository.findByInvitationId(1)
        ).willReturn(
            Optional.of(existingResponse)
        );

        given(
            rsvpResponseRepository.saveAndFlush(
                existingResponse
            )
        ).willAnswer(
            invocation -> {
                RsvpResponse savedResponse =
                    invocation.getArgument(0);

                savedResponse.setUpdatedAt(newUpdatedAt);

                return savedResponse;
            }
        );

        SaveRsvpResponse result =
            invitationService.saveResponse(
                "DEMO-FAMILY-001",
                request
            );

        assertThat(existingResponse.getId())
            .isEqualTo(10);

        assertThat(existingResponse.getGuestName())
            .isEqualTo("Ana y familia");

        assertThat(existingResponse.getContact())
            .isEqualTo("600 123 123");

        assertThat(existingResponse.getAttendeeCount())
            .isEqualTo(4);

        assertThat(existingResponse.getIntolerances())
            .isEqualTo("Sin gluten");

        assertThat(existingResponse.getSubmittedAt())
            .isEqualTo(submittedAt);

        assertThat(result.getUpdatedAt())
            .isEqualTo(newUpdatedAt);

        verify(rsvpResponseRepository)
            .saveAndFlush(existingResponse);
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
        Invitation invitation =
            new Invitation(
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
        Invitation invitation =
            new Invitation(
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

    private Invitation createValidInvitation() {
        Invitation invitation =
            new Invitation(
                "DEMO-FAMILY-001",
                "Familia García",
                4,
                true,
                LocalDateTime
                    .now(ZoneOffset.UTC)
                    .plusDays(30)
                    .withNano(0)
            );

        invitation.setId(1);

        return invitation;
    }

    private SaveRsvpRequest createSaveRequest() {
        return new SaveRsvpRequest(
            "Ana García",
            "ana@example.com",
            true,
            3,
            "Intolerancia a la lactosa",
            "Llegaremos el viernes"
        );
    }
}