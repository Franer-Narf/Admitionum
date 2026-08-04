package nc.admitionum.exception;

public class InvitationNotFoundException
        extends RuntimeException {

    public InvitationNotFoundException() {
        super(
            "No se ha encontrado una invitación válida "
                + "para el código indicado."
        );
    }
}