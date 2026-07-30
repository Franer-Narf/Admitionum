package nc.admitionum.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
class InvitationJpaTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldPersistInvitationAndGenerateAuditFields() {
        LocalDateTime expiresAt =
            LocalDateTime.now(ZoneOffset.UTC)
                .plusDays(30)
                .withNano(0);

        Invitation invitation = new Invitation(
            "DEMO-FAMILY-001",
            "Familia García",
            4,
            null,
            expiresAt
        );

        Invitation persistedInvitation =
            entityManager.persistAndFlush(invitation);

        Integer generatedId =
            persistedInvitation.getId();

        entityManager.clear();

        Invitation storedInvitation =
            entityManager.find(
                Invitation.class,
                generatedId
            );

        assertThat(storedInvitation)
            .isNotNull();

        assertThat(storedInvitation.getId())
            .isNotNull();

        assertThat(storedInvitation.getAccessCode())
            .isEqualTo("DEMO-FAMILY-001");

        assertThat(storedInvitation.getDisplayName())
            .isEqualTo("Familia García");

        assertThat(storedInvitation.getMaxGuests())
            .isEqualTo(4);

        assertThat(storedInvitation.getIsActive())
            .isTrue();

        assertThat(storedInvitation.getExpiresAt())
            .isEqualTo(expiresAt);

        assertThat(storedInvitation.getCreatedAt())
            .isNotNull();

        assertThat(storedInvitation.getUpdatedAt())
            .isNotNull();

        assertThat(storedInvitation.getUpdatedAt())
            .isAfterOrEqualTo(
                storedInvitation.getCreatedAt()
            );
    }
}