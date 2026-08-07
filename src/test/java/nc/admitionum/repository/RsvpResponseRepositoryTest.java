package nc.admitionum.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import nc.admitionum.model.Invitation;
import nc.admitionum.model.RsvpResponse;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
class RsvpResponseRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RsvpResponseRepository rsvpResponseRepository;

    @Test
    void shouldFindResponseByInvitationId() {
        Invitation invitation =
            persistInvitation("REPOSITORY-TEST-001");

        RsvpResponse response =
            new RsvpResponse(
                invitation,
                "Ana García",
                "ana@example.com",
                true,
                3,
                "Intolerancia a la lactosa",
                "Llegaremos el viernes"
            );

        entityManager.persistAndFlush(response);
        entityManager.clear();

        Optional<RsvpResponse> result =
            rsvpResponseRepository
                .findByInvitationId(
                    invitation.getId()
                );

        assertThat(result)
            .isPresent();

        assertThat(result.get().getGuestName())
            .isEqualTo("Ana García");

        assertThat(result.get().getContact())
            .isEqualTo("ana@example.com");

        assertThat(result.get().getAttendeeCount())
            .isEqualTo(3);
    }

    @Test
    void shouldReturnEmptyWhenInvitationHasNoResponse() {
        Invitation invitation =
            persistInvitation("REPOSITORY-TEST-002");

        Optional<RsvpResponse> result =
            rsvpResponseRepository
                .findByInvitationId(
                    invitation.getId()
                );

        assertThat(result)
            .isEmpty();
    }

    @Test
    void shouldUpdateExistingResponseWithoutCreatingAnother() {
        Invitation invitation =
            persistInvitation("REPOSITORY-TEST-003");

        RsvpResponse response =
            new RsvpResponse(
                invitation,
                "Ana García",
                "ana@example.com",
                true,
                2,
                null,
                null
            );

        RsvpResponse persistedResponse =
            rsvpResponseRepository
                .saveAndFlush(response);

        Integer originalId =
            persistedResponse.getId();

        persistedResponse.setContact(
            "600 123 123"
        );

        persistedResponse.setAttendeeCount(3);

        rsvpResponseRepository
            .saveAndFlush(persistedResponse);

        entityManager.clear();

        Optional<RsvpResponse> storedResponse =
            rsvpResponseRepository
                .findByInvitationId(
                    invitation.getId()
                );

        assertThat(storedResponse)
            .isPresent();

        assertThat(storedResponse.get().getId())
            .isEqualTo(originalId);

        assertThat(storedResponse.get().getContact())
            .isEqualTo("600 123 123");

        assertThat(storedResponse.get().getAttendeeCount())
            .isEqualTo(3);

        assertThat(rsvpResponseRepository.count())
            .isEqualTo(1);
    }

    private Invitation persistInvitation(
            String accessCode) {

        Invitation invitation =
            new Invitation(
                accessCode,
                "Invitación de prueba",
                4,
                true,
                LocalDateTime
                    .now(ZoneOffset.UTC)
                    .plusDays(30)
                    .withNano(0)
            );

        return entityManager
            .persistAndFlush(invitation);
    }
}