"use strict";

const rsvpForm = document.querySelector("#rsvp-form");

const invitationName =
    document.querySelector("#invitation-name");

const guestNameInput =
    document.querySelector("#guest-name");

const contactInput =
    document.querySelector("#contact");

const attendanceRadios =
    document.querySelectorAll(
        'input[name="attendanceConfirmed"]'
    );

const attendeeCountInput =
    document.querySelector("#attendee-count");

const attendeeCountHelp =
    document.querySelector(
        "#attendee-count-help"
    );

const intolerancesInput =
    document.querySelector("#intolerances");

const additionalCommentInput =
    document.querySelector(
        "#additional-comment"
    );

const submitButton =
    document.querySelector("#submit-button");

const formMessage =
    document.querySelector("#form-message");


let invitationCode = "";
let maxGuests = 0;


function getInvitationCode() {

    const params =
        new URLSearchParams(
            window.location.search
        );

    const code =
        params.get("code");

    if (code === null) {
        return "";
    }

    return code.trim();
}


function setFormDisabled(disabled) {

    const controls =
        rsvpForm.querySelectorAll(
            "input, select, textarea, button"
        );

    controls.forEach((control) => {
        control.disabled = disabled;
    });
}


function showMessage(message) {

    formMessage.textContent = message;
}

function fillExistingResponse(
        existingResponse) {

    if (existingResponse === null) {
        return;
    }

    guestNameInput.value =
        existingResponse.guestName ?? "";

    contactInput.value =
        existingResponse.contact ?? "";

    intolerancesInput.value =
        existingResponse.intolerances ?? "";

    additionalCommentInput.value =
        existingResponse.additionalComment ?? "";


    if (
        existingResponse
            .attendanceConfirmed === true
    ) {

        document
            .querySelector("#attendance-yes")
            .checked = true;

        attendeeCountInput.value =
            String(
                existingResponse
                    .attendeeCount ?? 1
            );

    } else if (
        existingResponse
            .attendanceConfirmed === false
    ) {

        document
            .querySelector("#attendance-no")
            .checked = true;

        attendeeCountInput.value = "0";
    }
}


function updateAttendeeCountState() {

    const selectedAttendance =
        document.querySelector(
            'input[name="attendanceConfirmed"]:checked'
        );

    if (selectedAttendance === null) {

        attendeeCountInput.value = "";

        attendeeCountInput.disabled = true;

        return;
    }

    const isAttending =
        selectedAttendance.value === "true";


    if (isAttending) {

        attendeeCountInput.disabled = false;

        if (
            attendeeCountInput.value === ""
            || attendeeCountInput.value === "0"
        ) {

            attendeeCountInput.value = "1";
        }

        return;
    }


    attendeeCountInput.value = "0";

    attendeeCountInput.disabled = true;
}


function getApiErrorMessage(
        data,
        fallbackMessage) {

    if (
        data === null
        || data === undefined
        || data.error === undefined
    ) {

        return fallbackMessage;
    }


    const messages = [];


    if (data.error.message) {

        messages.push(
            data.error.message
        );
    }


    if (data.error.fields) {

        Object
            .values(data.error.fields)
            .forEach((fieldMessage) => {

                if (fieldMessage) {

                    messages.push(
                        fieldMessage
                    );
                }
            });
    }


    if (messages.length === 0) {

        return fallbackMessage;
    }


    return messages.join(" ");
}


async function loadInvitation() {

    invitationCode =
        getInvitationCode();

    setFormDisabled(true);

    invitationName.textContent = "";


    if (invitationCode === "") {

        showMessage(
            "No se ha indicado un código "
                + "de invitación válido."
        );

        return;
    }


    showMessage(
        "Cargando invitación..."
    );


    try {

        const response =
            await fetch(
                "/api/public/invitations/"
                    + encodeURIComponent(
                        invitationCode
                    ),
                {
                    method: "GET",

                    headers: {
                        Accept:
                            "application/json"
                    }
                }
            );


        const data =
            await response.json();


        if (!response.ok) {

            showMessage(
                getApiErrorMessage(
                    data,
                    "No se ha podido cargar "
                        + "la invitación."
                )
            );

            return;
        }


       maxGuests =
        Number(data.maxGuests);


    if (
        !Number.isInteger(maxGuests)
        || maxGuests < 1
        || maxGuests > 20
    ) {

            showMessage(
                "La invitación contiene "
                    + "un número máximo de "
                    + "asistentes no válido."
            );

            return;
        }


        invitationName.textContent =
            "Invitación para "
                + data.displayName;


        attendeeCountInput.max =
        String(maxGuests);

    attendeeCountHelp.textContent =
        "Puedes confirmar hasta "
            + maxGuests
            + " asistentes.";


        fillExistingResponse(
            data.existingResponse
        );


        setFormDisabled(false);

        updateAttendeeCountState();


        if (
            data.existingResponse !== null
        ) {

            showMessage(
                "Hemos recuperado tu "
                    + "respuesta anterior. "
                    + "Puedes modificarla "
                    + "y volver a enviarla."
            );

        } else {

            showMessage("");
        }

    } catch (error) {

        console.error(
            "Error al cargar "
                + "la invitación:",
            error
        );

        showMessage(
            "No se ha podido conectar "
                + "con el servidor. "
                + "Inténtalo de nuevo "
                + "más tarde."
        );
    }
}


function buildRequestBody() {

    const selectedAttendance =
        document.querySelector(
            'input[name="attendanceConfirmed"]:checked'
        );


    const attendanceConfirmed =
        selectedAttendance.value === "true";


    return {

        guestName:
            guestNameInput
                .value
                .trim(),

        contact:
            contactInput
                .value
                .trim(),

        attendanceConfirmed:
            attendanceConfirmed,

        attendeeCount:
            attendanceConfirmed
                ? Number(
                    attendeeCountInput.value
                )
                : 0,

        intolerances:
            intolerancesInput
                .value
                .trim(),

        additionalComment:
            additionalCommentInput
                .value
                .trim()
    };
}


async function handleSubmit(event) {

    event.preventDefault();

    showMessage("");


    if (invitationCode === "") {

        showMessage(
            "No se puede enviar la "
                + "respuesta sin un código "
                + "de invitación válido."
        );

        return;
    }


    if (!rsvpForm.reportValidity()) {

        return;
    }


    const requestBody =
        buildRequestBody();


    const originalButtonText =
        submitButton.textContent;


    setFormDisabled(true);

    submitButton.textContent =
        "Enviando...";

    showMessage(
        "Enviando respuesta..."
    );


    try {

        const response =
            await fetch(
                "/api/public/invitations/"
                    + encodeURIComponent(
                        invitationCode
                    )
                    + "/response",
                {
                    method: "PUT",

                    headers: {
                        Accept:
                            "application/json",

                        "Content-Type":
                            "application/json"
                    },

                    body:
                        JSON.stringify(
                            requestBody
                        )
                }
            );


        const data =
            await response.json();


        if (!response.ok) {

            showMessage(
                getApiErrorMessage(
                    data,
                    "No se ha podido guardar "
                        + "la respuesta."
                )
            );

            return;
        }


        showMessage(
            data.message
                ?? "Tu respuesta se ha "
                    + "guardado correctamente."
        );

    } catch (error) {

        console.error(
            "Error al guardar "
                + "la respuesta:",
            error
        );

        showMessage(
            "No se ha podido conectar "
                + "con el servidor. "
                + "Inténtalo de nuevo "
                + "más tarde."
        );

    } finally {

        submitButton.textContent =
            originalButtonText;

        setFormDisabled(false);

        updateAttendeeCountState();
    }
}


attendanceRadios.forEach((radio) => {

    radio.addEventListener(
        "change",
        updateAttendeeCountState
    );
});


rsvpForm.addEventListener(
    "submit",
    handleSubmit
);


loadInvitation();