"use strict";


const totalInvitationsElement =
    document.querySelector(
        "#dashboard-total-invitations"
    );

const pendingInvitationsElement =
    document.querySelector(
        "#dashboard-pending-invitations"
    );

const confirmedAttendeesElement =
    document.querySelector(
        "#dashboard-confirmed-attendees"
    );

const intolerancesElement =
    document.querySelector(
        "#dashboard-intolerances"
    );

const adminMessage =
    document.querySelector(
        "#admin-message"
    );

const searchInput =
    document.querySelector(
        "#admin-search"
    );

const statusFilter =
    document.querySelector(
        "#status-filter"
    );

const intolerancesFilter =
    document.querySelector(
        "#intolerances-filter"
    );

const resultsCount =
    document.querySelector(
        "#results-count"
    );

const responsesTableBody =
    document.querySelector(
        "#responses-table-body"
    );


const statusLabels = {
    CONFIRMED: "Confirmada",
    DECLINED: "Rechazada",
    PENDING: "Pendiente",
    DISABLED: "Desactivada",
    EXPIRED: "Expirada"
};


const statusClasses = {
    CONFIRMED: "status-badge--confirmed",
    DECLINED: "status-badge--declined",
    PENDING: "status-badge--pending",
    DISABLED: "status-badge--disabled",
    EXPIRED: "status-badge--expired"
};


let allResponses = [];


function showAdminMessage(
        message,
        isError = false) {

    adminMessage.textContent = message;

    adminMessage.classList.toggle(
        "admin-message--error",
        isError
    );
}


function setFiltersDisabled(disabled) {

    searchInput.disabled = disabled;
    statusFilter.disabled = disabled;
    intolerancesFilter.disabled = disabled;
}


function setMetric(element, value) {

    const numericValue =
        Number(value);

    if (!Number.isFinite(numericValue)) {

        element.textContent = "—";
        return;
    }

    element.textContent =
        String(numericValue);
}


function fillDashboard(data) {

    setMetric(
        totalInvitationsElement,
        data.totalInvitations
    );

    setMetric(
        pendingInvitationsElement,
        data.pendingInvitations
    );

    setMetric(
        confirmedAttendeesElement,
        data.confirmedAttendees
    );

    setMetric(
        intolerancesElement,
        data.responsesWithIntolerances
    );
}


function normalizeText(value) {

    if (value === null
            || value === undefined) {

        return "";
    }

    return String(value)
        .normalize("NFD")
        .replace(
            /[\u0300-\u036f]/g,
            ""
        )
        .toLowerCase()
        .trim();
}


function hasIntolerances(response) {

    return (
        typeof response.intolerances
            === "string"
        && response.intolerances.trim()
            !== ""
    );
}


function getFilteredResponses() {

    const search =
        normalizeText(
            searchInput.value
        );

    const selectedStatus =
        statusFilter.value;

    const onlyWithIntolerances =
        intolerancesFilter.checked;


    return allResponses.filter(
        (response) => {

            const matchesSearch =
                search === ""
                || normalizeText(
                    response.displayName
                ).includes(search)
                || normalizeText(
                    response.guestName
                ).includes(search)
                || normalizeText(
                    response.contact
                ).includes(search);


            const matchesStatus =
                selectedStatus === "ALL"
                || response.status
                    === selectedStatus;


            const matchesIntolerances =
                !onlyWithIntolerances
                || hasIntolerances(
                    response
                );


            return (
                matchesSearch
                && matchesStatus
                && matchesIntolerances
            );
        }
    );
}


function displayValue(value) {

    if (value === null
            || value === undefined) {

        return "—";
    }

    const text =
        String(value).trim();

    if (text === "") {
        return "—";
    }

    return text;
}


function formatUpdatedAt(value) {

    if (value === null
            || value === undefined
            || value === "") {

        return "—";
    }


    const valueText =
        String(value);


    const containsTimeZone =
        valueText.endsWith("Z")
        || /[+-]\d{2}:\d{2}$/
            .test(valueText);


    const utcValue =
        containsTimeZone
            ? valueText
            : valueText + "Z";


    const date =
        new Date(utcValue);


    if (Number.isNaN(date.getTime())) {

        return valueText;
    }


    return new Intl.DateTimeFormat(
        "es-ES",
        {
            dateStyle: "short",
            timeStyle: "short"
        }
    ).format(date);
}


function createTextCell(
        value,
        className = "") {

    const cell =
        document.createElement("td");

    cell.textContent =
        displayValue(value);

    if (className !== "") {

        cell.classList.add(
            className
        );
    }

    return cell;
}


function createStatusCell(status) {

    const cell =
        document.createElement("td");

    const badge =
        document.createElement("span");


    badge.classList.add(
        "status-badge"
    );


    const statusClass =
        statusClasses[status]
        ?? "status-badge--unknown";


    badge.classList.add(
        statusClass
    );


    badge.textContent =
        statusLabels[status]
        ?? displayValue(status);


    cell.appendChild(badge);

    return cell;
}


function createResponseRow(response) {

    const row =
        document.createElement("tr");


    row.appendChild(
        createTextCell(
            response.displayName
        )
    );

    row.appendChild(
        createTextCell(
            response.guestName
        )
    );

    row.appendChild(
        createTextCell(
            response.contact
        )
    );

    row.appendChild(
        createStatusCell(
            response.status
        )
    );

    row.appendChild(
        createTextCell(
            response.attendeeCount
        )
    );

    row.appendChild(
        createTextCell(
            response.intolerances,
            "admin-table__long-text"
        )
    );

    row.appendChild(
        createTextCell(
            response.additionalComment,
            "admin-table__long-text"
        )
    );

    row.appendChild(
        createTextCell(
            formatUpdatedAt(
                response.updatedAt
            )
        )
    );


    return row;
}


function renderResponses() {

    const filteredResponses =
        getFilteredResponses();


    responsesTableBody
        .replaceChildren();


    resultsCount.textContent =
        "Mostrando "
        + filteredResponses.length
        + " de "
        + allResponses.length
        + " invitaciones";


    if (filteredResponses.length === 0) {

        const row =
            document.createElement("tr");

        const cell =
            document.createElement("td");


        cell.colSpan = 8;

        cell.textContent =
            "No hay resultados para "
                + "los filtros seleccionados.";


        row.appendChild(cell);

        responsesTableBody
            .appendChild(row);

        return;
    }


    const fragment =
        document.createDocumentFragment();


    filteredResponses.forEach(
        (response) => {

            fragment.appendChild(
                createResponseRow(
                    response
                )
            );
        }
    );


    responsesTableBody
        .appendChild(fragment);
}


async function fetchJson(url) {

    const response =
        await fetch(
            url,
            {
                method: "GET",

                headers: {
                    Accept:
                        "application/json"
                },

                credentials:
                    "same-origin"
            }
        );


    if (response.redirected) {

        const finalUrl =
            new URL(
                response.url,
                window.location.origin
            );


        if (finalUrl.pathname === "/login") {

            window.location.assign(
                "/login"
            );

            throw new Error(
                "AUTHENTICATION_REQUIRED"
            );
        }
    }


    if (!response.ok) {

        throw new Error(
            "HTTP_"
                + response.status
        );
    }


    const contentType =
        response.headers.get(
            "content-type"
        ) ?? "";


    if (!contentType.includes(
            "application/json")) {

        throw new Error(
            "INVALID_RESPONSE_TYPE"
        );
    }


    return response.json();
}


async function loadAdminPanel() {

    setFiltersDisabled(true);

    showAdminMessage(
        "Cargando información..."
    );


    try {

        const [
            dashboard,
            responses
        ] = await Promise.all([
            fetchJson(
                "/api/admin/dashboard"
            ),

            fetchJson(
                "/api/admin/responses"
            )
        ]);


        fillDashboard(
            dashboard
        );


        if (!Array.isArray(responses)) {

            throw new Error(
                "INVALID_RESPONSES_DATA"
            );
        }


        allResponses =
            responses;


        renderResponses();

        setFiltersDisabled(false);

        showAdminMessage("");


    } catch (error) {

        if (
            error.message
                === "AUTHENTICATION_REQUIRED"
        ) {

            return;
        }


        console.error(
            "Error al cargar "
                + "el panel administrativo:",
            error
        );


        responsesTableBody
            .replaceChildren();


        resultsCount.textContent = "";


        showAdminMessage(
            "No se ha podido cargar "
                + "la información administrativa. "
                + "Inténtalo de nuevo más tarde.",
            true
        );
    }
}


searchInput.addEventListener(
    "input",
    renderResponses
);


statusFilter.addEventListener(
    "change",
    renderResponses
);


intolerancesFilter.addEventListener(
    "change",
    renderResponses
);


loadAdminPanel();