package nc.admitionum.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import nc.admitionum.dto.publicapi.ApiErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(
        InvitationNotFoundException.class
    )
    public ResponseEntity<ApiErrorResponse>
            handleInvitationNotFound(
                    InvitationNotFoundException exception) {

        ApiErrorResponse response =
            new ApiErrorResponse(
                "INVITATION_NOT_FOUND",
                exception.getMessage()
            );

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(response);
    }

    @ExceptionHandler(
        InvitationDisabledException.class
    )
    public ResponseEntity<ApiErrorResponse>
            handleInvitationDisabled(
                    InvitationDisabledException exception) {

        ApiErrorResponse response =
            new ApiErrorResponse(
                "INVITATION_DISABLED",
                exception.getMessage()
            );

        return ResponseEntity
            .status(HttpStatus.GONE)
            .body(response);
    }

    @ExceptionHandler(
        InvitationExpiredException.class
    )
    public ResponseEntity<ApiErrorResponse>
            handleInvitationExpired(
                    InvitationExpiredException exception) {

        ApiErrorResponse response =
            new ApiErrorResponse(
                "INVITATION_EXPIRED",
                exception.getMessage()
            );

        return ResponseEntity
            .status(HttpStatus.GONE)
            .body(response);
    }

    @ExceptionHandler(
        InvalidAttendeeCountException.class
    )
    public ResponseEntity<ApiErrorResponse>
            handleInvalidAttendeeCount(
                    InvalidAttendeeCountException exception) {

        ApiErrorResponse response =
            new ApiErrorResponse(
                "INVALID_GUEST_COUNT",
                exception.getMessage()
            );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }

    @ExceptionHandler(
        MethodArgumentNotValidException.class
    )
    public ResponseEntity<ApiErrorResponse>
            handleValidation(
                    MethodArgumentNotValidException exception) {

        Map<String, String> fields =
            new LinkedHashMap<>();

        for (FieldError fieldError :
                exception
                    .getBindingResult()
                    .getFieldErrors()) {

            String message =
                fieldError.getDefaultMessage();

            if (message == null) {
                message = "Valor no válido.";
            }

            fields.putIfAbsent(
                fieldError.getField(),
                message
            );
        }

        ApiErrorResponse response =
            new ApiErrorResponse(
                "VALIDATION_ERROR",
                "Revisa los campos indicados.",
                fields
            );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }
}