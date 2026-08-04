package nc.admitionum.exception;

public class InvitationDisabledException
        extends RuntimeException {

    public InvitationDisabledException() {
        super("La invitación está desactivada.");
    }
}