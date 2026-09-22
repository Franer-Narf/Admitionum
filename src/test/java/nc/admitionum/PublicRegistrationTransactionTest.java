package nc.admitionum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import nc.admitionum.dto.publicapi.SaveRsvpRequest;
import nc.admitionum.model.RsvpResponse;
import nc.admitionum.repository.InvitationRepository;
import nc.admitionum.repository.RsvpResponseRepository;
import nc.admitionum.service.InvitationService;

@SpringBootTest
@ActiveProfiles("test")
class PublicRegistrationTransactionTest {

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private InvitationRepository invitationRepository;

    @MockitoBean
    private RsvpResponseRepository
        rsvpResponseRepository;

    @Test
    void shouldRollbackInvitationWhenResponseFails() {

        long invitationsBefore =
            invitationRepository.count();

        given(
            rsvpResponseRepository.saveAndFlush(
                any(RsvpResponse.class)
            )
        ).willThrow(
            new IllegalStateException(
                "Fallo simulado al guardar la respuesta."
            )
        );

        SaveRsvpRequest request =
            new SaveRsvpRequest(
                "Ana García",
                "ana@example.com",
                true,
                2,
                null,
                null
            );

        assertThatThrownBy(
            () -> invitationService
                .registerPublicResponse(request)
        )
            .isInstanceOf(
                IllegalStateException.class
            )
            .hasMessage(
                "Fallo simulado al guardar la respuesta."
            );

        verify(rsvpResponseRepository)
            .saveAndFlush(
                any(RsvpResponse.class)
            );

        long invitationsAfter =
            invitationRepository.count();

        assertThat(invitationsAfter)
            .isEqualTo(invitationsBefore);
    }
}