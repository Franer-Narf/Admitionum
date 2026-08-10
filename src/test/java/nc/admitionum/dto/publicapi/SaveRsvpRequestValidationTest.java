package nc.admitionum.dto.publicapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class SaveRsvpRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void configureValidator() {
        validatorFactory =
            Validation.buildDefaultValidatorFactory();

        validator =
            validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void shouldAcceptValidRequest() {
        SaveRsvpRequest request =
            createValidRequest();

        Set<ConstraintViolation<SaveRsvpRequest>>
                violations =
                    validator.validate(request);

        assertThat(violations)
            .isEmpty();
    }

    @Test
    void shouldAcceptPhoneOrEmailAsContact() {
        SaveRsvpRequest phoneRequest =
            createValidRequest();

        phoneRequest.setContact(
            "+34 600 123 123"
        );

        SaveRsvpRequest emailRequest =
            createValidRequest();

        emailRequest.setContact(
            "ana@example.com"
        );

        Set<ConstraintViolation<SaveRsvpRequest>>
                phoneViolations =
                    validator.validate(phoneRequest);

        Set<ConstraintViolation<SaveRsvpRequest>>
                emailViolations =
                    validator.validate(emailRequest);

        assertThat(phoneViolations)
            .isEmpty();

        assertThat(emailViolations)
            .isEmpty();
    }

    @Test
    void shouldRejectBlankRequiredTexts() {
        SaveRsvpRequest request =
            createValidRequest();

        request.setGuestName("   ");
        request.setContact("");

        Set<ConstraintViolation<SaveRsvpRequest>>
                violations =
                    validator.validate(request);

        assertThat(
            hasViolationFor(
                violations,
                "guestName"
            )
        ).isTrue();

        assertThat(
            hasViolationFor(
                violations,
                "contact"
            )
        ).isTrue();
    }

    @Test
    void shouldRejectNullRequiredValues() {
        SaveRsvpRequest request =
            createValidRequest();

        request.setAttendanceConfirmed(null);
        request.setAttendeeCount(null);

        Set<ConstraintViolation<SaveRsvpRequest>>
                violations =
                    validator.validate(request);

        assertThat(
            hasViolationFor(
                violations,
                "attendanceConfirmed"
            )
        ).isTrue();

        assertThat(
            hasViolationFor(
                violations,
                "attendeeCount"
            )
        ).isTrue();
    }

    @Test
    void shouldRejectNegativeAttendeeCount() {
        SaveRsvpRequest request =
            createValidRequest();

        request.setAttendeeCount(-1);

        Set<ConstraintViolation<SaveRsvpRequest>>
                violations =
                    validator.validate(request);

        assertThat(
            hasViolationFor(
                violations,
                "attendeeCount"
            )
        ).isTrue();
    }

    @Test
    void shouldRejectAttendeeCountAboveTwenty() {
        SaveRsvpRequest request =
            createValidRequest();

        request.setAttendeeCount(21);

        Set<ConstraintViolation<SaveRsvpRequest>>
            violations =
                validator.validate(request);

        assertThat(
         hasViolationFor(
               violations,
               "attendeeCount"
         )
        ).isTrue();
    }

    @Test
    void shouldAcceptAttendeeCountAtTwenty() {
        SaveRsvpRequest request =
            createValidRequest();

        request.setAttendeeCount(20);

        Set<ConstraintViolation<SaveRsvpRequest>>
                violations =
                    validator.validate(request);

        assertThat(violations)
            .isEmpty();
    }

    @Test
    void shouldRejectTextsAboveMaximumLength() {
        SaveRsvpRequest request =
            createValidRequest();

        request.setGuestName(
            "a".repeat(201)
        );

        request.setContact(
            "b".repeat(201)
        );

        request.setIntolerances(
            "c".repeat(501)
        );

        request.setAdditionalComment(
            "d".repeat(1001)
        );

        Set<ConstraintViolation<SaveRsvpRequest>>
                violations =
                    validator.validate(request);

        assertThat(
            hasViolationFor(
                violations,
                "guestName"
            )
        ).isTrue();

        assertThat(
            hasViolationFor(
                violations,
                "contact"
            )
        ).isTrue();

        assertThat(
            hasViolationFor(
                violations,
                "intolerances"
            )
        ).isTrue();

        assertThat(
            hasViolationFor(
                violations,
                "additionalComment"
            )
        ).isTrue();
    }

    @Test
    void shouldAllowOptionalTextsToBeNull() {
        SaveRsvpRequest request =
            createValidRequest();

        request.setIntolerances(null);
        request.setAdditionalComment(null);

        Set<ConstraintViolation<SaveRsvpRequest>>
                violations =
                    validator.validate(request);

        assertThat(violations)
            .isEmpty();
    }

    private static SaveRsvpRequest
            createValidRequest() {

        return new SaveRsvpRequest(
            "Ana García",
            "ana@example.com",
            true,
            3,
            "Intolerancia a la lactosa",
            "Llegaremos el viernes"
        );
    }

    private static boolean hasViolationFor(
            Set<ConstraintViolation<SaveRsvpRequest>>
                    violations,
            String propertyName) {

        return violations
            .stream()
            .anyMatch(
                violation ->
                    violation
                        .getPropertyPath()
                        .toString()
                        .equals(propertyName)
            );
    }
}