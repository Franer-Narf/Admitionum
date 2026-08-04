package nc.admitionum.exception;

public class InvitationExpiredException
        extends RuntimeException {

    public InvitationExpiredException() {
        super(
            "El plazo para responder a la invitación " + "ha finalizado."
        );
    }
}