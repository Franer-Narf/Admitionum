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
class RsvpResponseJpaTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldPersistRsvpResponseWithInvitationRelation() {
        Invitation invitation = new Invitation(
            "DEMO-RSVP-001",
            "Familia de prueba",
            4,
            true,
            LocalDateTime
                .now(ZoneOffset.UTC)
                .plusDays(30)
                .withNano(0)
        );

        Invitation persistedInvitation =
            entityManager.persistAndFlush(invitation);

        RsvpResponse response = new RsvpResponse(
            persistedInvitation,
            "Ana García",
            "ana@example.com",
            true,
            3,
            "Una persona es intolerante a la lactosa",
            "Llegaremos el viernes por la tarde"
        );

        RsvpResponse persistedResponse =
            entityManager.persistAndFlush(response);

        Integer generatedResponseId =
            persistedResponse.getId();

        entityManager.clear();

        RsvpResponse storedResponse =
            entityManager.find(
                RsvpResponse.class,
                generatedResponseId
            );

        assertThat(storedResponse)
            .isNotNull();

        assertThat(storedResponse.getId())
            .isNotNull();

        assertThat(storedResponse.getInvitation())
            .isNotNull();

        assertThat(storedResponse.getInvitation().getId())
            .isEqualTo(persistedInvitation.getId());

        assertThat(
            storedResponse.getInvitation().getAccessCode()
        ).isEqualTo("DEMO-RSVP-001");

        assertThat(storedResponse.getGuestName())
            .isEqualTo("Ana García");

        assertThat(storedResponse.getContact())
            .isEqualTo("ana@example.com");

        assertThat(storedResponse.getAttendanceConfirmed())
            .isTrue();

        assertThat(storedResponse.getAttendeeCount())
            .isEqualTo(3);

        assertThat(storedResponse.getIntolerances())
            .isEqualTo(
                "Una persona es intolerante a la lactosa"
            );

        assertThat(storedResponse.getAdditionalComment())
            .isEqualTo(
                "Llegaremos el viernes por la tarde"
            );

        assertThat(storedResponse.getSubmittedAt())
            .isNotNull();

        assertThat(storedResponse.getUpdatedAt())
            .isNotNull();

        assertThat(storedResponse.getUpdatedAt())
            .isAfterOrEqualTo(
                storedResponse.getSubmittedAt()
            );
    }
}