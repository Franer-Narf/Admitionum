package nc.admitionum.exception;

public class InvalidAttendeeCountException
        extends RuntimeException {

    public InvalidAttendeeCountException(
            String message) {

        super(message);
    }
}