package nc.admitionum.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import nc.admitionum.dto.publicapi.ApiErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvitationNotFoundException.class)
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

    @ExceptionHandler(InvitationDisabledException.class)
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

    @ExceptionHandler(InvitationExpiredException.class)
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
}