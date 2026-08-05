package nc.admitionum.dto.publicapi;

public class ApiErrorResponse {

    private boolean success;
    private ErrorDetails error;

    public ApiErrorResponse() {
    }

    public ApiErrorResponse(
            String code,
            String message) {

        this.success = false;
        this.error = new ErrorDetails(code, message);
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

        public ErrorDetails() {
        }

        public ErrorDetails(
                String code,
                String message) {

            this.code = code;
            this.message = message;
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
    }
}