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

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
class InvitationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private InvitationRepository invitationRepository;

    @Test
    void shouldFindInvitationByAccessCode() {
        Invitation invitation = new Invitation(
            "DEMO-FAMILY-001",
            "Familia García",
            4,
            true,
            LocalDateTime
                .now(ZoneOffset.UTC)
                .plusDays(30)
                .withNano(0)
        );

        entityManager.persistAndFlush(invitation);
        entityManager.clear();

        Optional<Invitation> result =
            invitationRepository
                .findByAccessCode(
                    "DEMO-FAMILY-001"
                );

        assertThat(result)
            .isPresent();

        assertThat(result.get().getDisplayName())
            .isEqualTo("Familia García");

        assertThat(result.get().getMaxGuests())
            .isEqualTo(4);

        assertThat(result.get().getIsActive())
            .isTrue();
    }

    @Test
    void shouldReturnEmptyWhenAccessCodeDoesNotExist() {
        Optional<Invitation> result =
            invitationRepository
                .findByAccessCode(
                    "UNKNOWN-CODE"
                );

        assertThat(result)
            .isEmpty();
    }
}