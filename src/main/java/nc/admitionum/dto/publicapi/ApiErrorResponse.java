package nc.admitionum.dto.publicapi;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

public class ApiErrorResponse {

    private boolean success;
    private ErrorDetails error;

    public ApiErrorResponse() {
    }

    public ApiErrorResponse(
            String code,
            String message) {

        this.success = false;
        this.error =
            new ErrorDetails(
                code,
                message
            );
    }

    public ApiErrorResponse(
            String code,
            String message,
            Map<String, String> fields) {

        this.success = false;
        this.error =
            new ErrorDetails(
                code,
                message,
                fields
            );
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ErrorDetails getError() {
        return error;
    }

    public void setError(ErrorDetails error) {
        this.error = error;
    }

    public static class ErrorDetails {

        private String code;
        private String message;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private Map<String, String> fields;

        public ErrorDetails() {
        }

        public ErrorDetails(
                String code,
                String message) {

            this.code = code;
            this.message = message;
        }

        public ErrorDetails(
                String code,
                String message,
                Map<String, String> fields) {

            this.code = code;
            this.message = message;
            this.fields = fields;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Map<String, String> getFields() {
            return fields;
        }

        public void setFields(
                Map<String, String> fields) {

            this.fields = fields;
        }
    }
}