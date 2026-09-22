"use strict";

const rsvpForm =
    document.querySelector("#rsvp-form");

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

    if (!rsvpForm.reportValidity()) {

        return;
    }

    const requestBody =
        buildRequestBody();

    const originalButtonText =
        submitButton.textContent;

    let registrationSucceeded = false;

    setFormDisabled(true);

    submitButton.textContent =
        "Enviando...";

    showMessage(
        "Enviando respuesta..."
    );

    try {

        const response =
            await fetch(
                "/api/public/registrations",
                {
                    method: "POST",

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

        registrationSucceeded = true;

        showMessage(
            data.message
                ?? "Tu respuesta se ha "
                    + "guardado correctamente."
        );

    } catch (error) {

        console.error(
            "Error al registrar "
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

        if (registrationSucceeded) {

            submitButton.textContent =
                "Respuesta enviada";

        } else {

            submitButton.textContent =
                originalButtonText;

            setFormDisabled(false);

            updateAttendeeCountState();
        }
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


updateAttendeeCountState();