# Admitionum REST API

## 1. Overview

Admitionum exposes a small REST API used by the public RSVP form and the protected administration dashboard.

The API is divided into two areas:

```text
/api/public/**
/api/admin/**
```

Public endpoints can be accessed without administrator authentication.

Administration endpoints require an authenticated Spring Security session with the `ADMIN` role.

The frontend and API are served by the same Spring Boot application.

---

## 2. Base URL

During local development:

```text
http://localhost:8080
```

Example:

```text
http://localhost:8080/api/public/health
```

In production, the base URL is the HTTPS hostname assigned to the Azure Container App.

This documentation uses relative endpoint paths so that no production-specific hostname is required.

---

## 3. Content type

JSON API requests use:

```http
Content-Type: application/json
```

Most REST responses also use:

```http
Content-Type: application/json
```

The CSV export endpoint returns:

```http
Content-Type: text/csv;charset=UTF-8
```

---

# Public API

## 4. Health endpoint

Checks that the Admitionum application is responding.

### Request

```http
GET /api/public/health
```

### Authentication

Not required.

### Request body

None.

### Successful response

HTTP:

```text
200 OK
```

JSON:

```json
{
  "status": "ok",
  "application": "Admitionum"
}
```

### Purpose

This endpoint is used for:

- Manual health verification.
- Deployment verification.
- GitHub Actions post-deployment checks.

It does not query or expose RSVP information.

---

# 5. Retrieve an invitation

Retrieves the public information associated with one invitation access code.

### Request

```http
GET /api/public/invitations/{code}
```

Example:

```http
GET /api/public/invitations/DEMO-FAMILY-001
```

### Authentication

Not required.

### Path parameter

| Parameter | Type | Description |
|---|---|---|
| `code` | String | Individual invitation access code |

The code is trimmed before it is used.

An empty or invalid code is treated as an invitation that cannot be found.

---

## 5.1. Successful response without an existing RSVP

HTTP:

```text
200 OK
```

Example:

```json
{
  "displayName": "Familia García",
  "maxGuests": 4,
  "expiresAt": "2027-05-01T22:00:00",
  "existingResponse": null
}
```

### Response fields

| Field | Type | Description |
|---|---|---|
| `displayName` | String | Name associated with the invitation |
| `maxGuests` | Integer | Maximum number of attendees allowed |
| `expiresAt` | Date/time or null | Optional invitation expiration |
| `existingResponse` | Object or null | Previously stored RSVP |

The public response does not expose the internal invitation database ID.

It also does not return the complete invitation list.

---

## 5.2. Successful response with an existing RSVP

Example:

```json
{
  "displayName": "Familia García",
  "maxGuests": 4,
  "expiresAt": "2027-05-01T22:00:00",
  "existingResponse": {
    "guestName": "Ana García",
    "contact": "ana@example.com",
    "attendanceConfirmed": true,
    "attendeeCount": 3,
    "intolerances": "Una persona es intolerante a la lactosa",
    "additionalComment": "Llegaremos el viernes"
  }
}
```

### Existing RSVP fields

| Field | Type | Description |
|---|---|---|
| `guestName` | String | Guest name entered in the form |
| `contact` | String | Phone number, email address, or other contact text |
| `attendanceConfirmed` | Boolean | Whether attendance is confirmed |
| `attendeeCount` | Integer | Number of attendees |
| `intolerances` | String or null | Food intolerance information |
| `additionalComment` | String or null | Optional additional comment |

This allows the public form to restore previously submitted information.

---

## 5.3. Invitation not found

If no invitation exists for the supplied code:

HTTP:

```text
404 Not Found
```

Response:

```json
{
  "success": false,
  "error": {
    "code": "INVITATION_NOT_FOUND",
    "message": "No se ha encontrado una invitación válida para el código indicado."
  }
}
```

---

## 5.4. Disabled invitation

If the invitation exists but is disabled:

HTTP:

```text
410 Gone
```

Response:

```json
{
  "success": false,
  "error": {
    "code": "INVITATION_DISABLED",
    "message": "La invitación está desactivada."
  }
}
```

---

## 5.5. Expired invitation

If the invitation expiration date has already passed:

HTTP:

```text
410 Gone
```

Response:

```json
{
  "success": false,
  "error": {
    "code": "INVITATION_EXPIRED",
    "message": "El plazo para responder a la invitación ha finalizado."
  }
}
```

---

# 6. Create or update an RSVP

Stores the RSVP associated with an invitation.

If no RSVP exists yet, a new response is created.

If an RSVP already exists for the same invitation, the existing response is updated.

### Request

```http
PUT /api/public/invitations/{code}/response
```

Example:

```http
PUT /api/public/invitations/DEMO-FAMILY-001/response
```

### Authentication

Not required.

### Content type

```http
Content-Type: application/json
```

---

## 6.1. Request body

Example of a confirmed RSVP:

```json
{
  "guestName": "Ana García",
  "contact": "ana@example.com",
  "attendanceConfirmed": true,
  "attendeeCount": 3,
  "intolerances": "Una persona es intolerante a la lactosa",
  "additionalComment": "Llegaremos el viernes por la tarde"
}
```

Example of a declined RSVP:

```json
{
  "guestName": "Ana García",
  "contact": "600123123",
  "attendanceConfirmed": false,
  "attendeeCount": 0,
  "intolerances": "",
  "additionalComment": "Sentimos no poder acompañaros"
}
```

---

## 6.2. Request fields

| Field | Type | Required | Validation |
|---|---|---:|---|
| `guestName` | String | Yes | 2–200 characters |
| `contact` | String | Yes | 3–200 characters |
| `attendanceConfirmed` | Boolean | Yes | `true` or `false` |
| `attendeeCount` | Integer | Yes | Between 0 and 20 at DTO level |
| `intolerances` | String | No | Maximum 500 characters |
| `additionalComment` | String | No | Maximum 1000 characters |

The `contact` field deliberately does not enforce only one contact format.

Valid examples include:

```text
600 123 123
+34 600 123 123
persona@example.com
```

---

## 6.3. Attendance business rules

DTO validation provides the general numerical range.

The service layer applies the invitation-specific business rule.

When:

```json
{
  "attendanceConfirmed": true
}
```

the rule is:

```text
1 <= attendeeCount <= invitation.maxGuests
```

When:

```json
{
  "attendanceConfirmed": false
}
```

the required value is:

```text
attendeeCount = 0
```

Therefore, even if the general DTO allows values up to 20, a guest cannot exceed the `maxGuests` configured for their invitation.

Example:

```text
Invitation maxGuests = 4
```

Valid:

```text
1
2
3
4
```

Invalid:

```text
0
5
6
...
20
```

when attendance has been confirmed.

---

## 6.4. Successful response

HTTP:

```text
200 OK
```

Example:

```json
{
  "success": true,
  "message": "Tu respuesta se ha guardado correctamente.",
  "updatedAt": "2027-03-15T17:30:00"
}
```

The same response structure is used whether the operation creates the first RSVP or updates the existing RSVP.

---

# 7. Validation errors

Jakarta Bean Validation checks the request before the business operation is executed.

If one or more request fields are invalid:

HTTP:

```text
400 Bad Request
```

Example:

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Revisa los campos indicados.",
    "fields": {
      "guestName": "El nombre del invitado es obligatorio.",
      "contact": "El contacto es obligatorio."
    }
  }
}
```

The `fields` object only contains fields that failed validation.

---

## 7.1. Invalid attendee count

Business validation may also reject the attendee count.

HTTP:

```text
400 Bad Request
```

Example:

```json
{
  "success": false,
  "error": {
    "code": "INVALID_GUEST_COUNT",
    "message": "El número de asistentes supera el máximo permitido."
  }
}
```

Another possible message is:

```text
Debe asistir al menos una persona.
```

or:

```text
Cuando no se confirma asistencia, el número de asistentes debe ser cero.
```

The error code remains:

```text
INVALID_GUEST_COUNT
```

---

# Administration API

## 8. Authentication model

Administration endpoints are located under:

```text
/api/admin/**
```

They are protected by Spring Security.

The user must have an authenticated HTTP session with:

```text
ROLE_ADMIN
```

Admitionum currently uses Spring Security form login.

The API does not use:

```text
JWT
Bearer tokens
API keys
OAuth tokens
```

for administrator access.

When an unauthenticated browser tries to access a protected resource, Spring Security's form-login flow can redirect the client to the login page.

For that reason, the administration API is designed primarily to be consumed by the administration frontend running in the same Spring Boot application and authenticated session.

---

# 9. Dashboard

Returns aggregate RSVP statistics.

### Request

```http
GET /api/admin/dashboard
```

### Authentication

Required.

Role:

```text
ADMIN
```

### Request body

None.

### Successful response

HTTP:

```text
200 OK
```

Example:

```json
{
  "totalInvitations": 80,
  "answeredInvitations": 52,
  "pendingInvitations": 28,
  "confirmedInvitations": 40,
  "declinedInvitations": 12,
  "confirmedAttendees": 93,
  "responsesWithIntolerances": 8
}
```

### Response fields

| Field | Description |
|---|---|
| `totalInvitations` | Total invitations stored |
| `answeredInvitations` | Invitations with an RSVP |
| `pendingInvitations` | Active, non-expired invitations without an RSVP |
| `confirmedInvitations` | RSVP records confirming attendance |
| `declinedInvitations` | RSVP records declining attendance |
| `confirmedAttendees` | Sum of attendee counts for confirmed RSVPs |
| `responsesWithIntolerances` | Confirmed responses containing intolerance information |

---

# 10. List invitations and responses

Returns the administrative view of all invitations.

### Request

```http
GET /api/admin/responses
```

### Authentication

Required.

Role:

```text
ADMIN
```

### Request body

None.

### Query parameters

None in the current API implementation.

Search and status filtering are performed by the administration JavaScript after the list has been retrieved.

---

## 10.1. Successful response

HTTP:

```text
200 OK
```

Example:

```json
[
  {
    "invitationId": 1,
    "displayName": "Familia García",
    "maxGuests": 4,
    "status": "CONFIRMED",
    "guestName": "Ana García",
    "contact": "ana@example.com",
    "attendeeCount": 3,
    "intolerances": "Lactosa",
    "additionalComment": "Llegaremos el viernes",
    "updatedAt": "2027-03-15T17:30:00"
  },
  {
    "invitationId": 2,
    "displayName": "María López",
    "maxGuests": 1,
    "status": "PENDING",
    "guestName": null,
    "contact": null,
    "attendeeCount": null,
    "intolerances": null,
    "additionalComment": null,
    "updatedAt": null
  }
]
```

---

## 10.2. Administrative status

Possible values are:

```text
CONFIRMED
DECLINED
PENDING
DISABLED
EXPIRED
```

The status is calculated by the application.

It is not stored as a separate RSVP status column.

### CONFIRMED

An RSVP exists and:

```text
attendanceConfirmed = true
```

### DECLINED

An RSVP exists and:

```text
attendanceConfirmed = false
```

### DISABLED

No RSVP exists and the invitation is not active.

### EXPIRED

No RSVP exists and the expiration date has passed.

### PENDING

No RSVP exists, the invitation is active, and it has not expired.

If an RSVP already exists, `CONFIRMED` or `DECLINED` takes precedence over the invitation's later active or expiration state in the current administrative status calculation.

---

# 11. Export responses as CSV

Downloads the administrative invitation data as a CSV document.

### Request

```http
GET /api/admin/responses.csv
```

### Authentication

Required.

Role:

```text
ADMIN
```

### Successful response

HTTP:

```text
200 OK
```

Content type:

```text
text/csv;charset=UTF-8
```

The response is sent as an attachment with the filename:

```text
wedding-responses.csv
```

---

## 11.1. CSV columns

The exported file contains:

```text
Invitacion
NombreInvitado
Contacto
Estado
NumeroAsistentes
Intolerancias
ComentarioAdicional
FechaEnvio
UltimaActualizacion
```

Header:

```csv
Invitacion,NombreInvitado,Contacto,Estado,NumeroAsistentes,Intolerancias,ComentarioAdicional,FechaEnvio,UltimaActualizacion
```

Example row:

```csv
"Familia García","Ana García","ana@example.com","CONFIRMED","3","Lactosa","Llegaremos el viernes","2027-03-10T12:00:00","2027-03-15T17:30:00"
```

The exact quoting of a value depends on its contents and CSV escaping requirements.

---

# 12. API error format

Public application errors follow this structure:

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable message."
  }
}
```

Validation errors can additionally contain:

```json
{
  "fields": {
    "fieldName": "Validation message"
  }
}
```

Complete example:

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Revisa los campos indicados.",
    "fields": {
      "contact": "El contacto es obligatorio."
    }
  }
}
```

The `fields` property is omitted when there are no field-specific validation errors.

---

# 13. Documented application error codes

The application currently defines the following public error codes:

| Error code | HTTP status | Meaning |
|---|---:|---|
| `INVITATION_NOT_FOUND` | 404 | No invitation matches the supplied code |
| `INVITATION_DISABLED` | 410 | The invitation has been disabled |
| `INVITATION_EXPIRED` | 410 | The invitation response period has expired |
| `INVALID_GUEST_COUNT` | 400 | Attendance count violates a business rule |
| `VALIDATION_ERROR` | 400 | Request fields failed Jakarta Bean Validation |

Unexpected framework or infrastructure failures are not part of the normal public API contract documented here.

---

# 14. Public API data boundary

The public API deliberately exposes only the information needed to complete one invitation.

It does not provide endpoints such as:

```text
GET /api/public/invitations
GET /api/public/responses
```

A public client cannot request the complete invitation database through the documented API.

The intended public flow is always:

```text
Invitation access code
        |
        v
One invitation
        |
        v
One RSVP
```

---

# 15. Administration API data boundary

Complete invitation information is only available through:

```text
/api/admin/**
```

These endpoints require Spring Security authentication.

The separation is therefore:

```text
Guest
  |
  v
/api/public/**
  |
  v
Individual invitation
```

and:

```text
Administrator
  |
  v
Authenticated session
  |
  v
/api/admin/**
  |
  v
Complete RSVP administration data
```

---

# 16. Date and time representation

Internal application timestamps are generated using UTC.

The current DTOs use Java `LocalDateTime`, so JSON date/time values are represented without an explicit timezone suffix.

Example:

```json
{
  "updatedAt": "2027-03-15T17:30:00"
}
```

Within Admitionum, these internal values are treated as UTC even though the serialized `LocalDateTime` value does not contain `Z` or a numeric offset.

---

# 17. Example public flow

A complete RSVP interaction can be summarized as follows.

### Step 1 — Retrieve invitation

```http
GET /api/public/invitations/DEMO-FAMILY-001
```

Response:

```json
{
  "displayName": "Familia García",
  "maxGuests": 4,
  "expiresAt": null,
  "existingResponse": null
}
```

### Step 2 — Submit RSVP

```http
PUT /api/public/invitations/DEMO-FAMILY-001/response
Content-Type: application/json
```

Body:

```json
{
  "guestName": "Ana García",
  "contact": "ana@example.com",
  "attendanceConfirmed": true,
  "attendeeCount": 3,
  "intolerances": "",
  "additionalComment": ""
}
```

Response:

```json
{
  "success": true,
  "message": "Tu respuesta se ha guardado correctamente.",
  "updatedAt": "2027-03-15T17:30:00"
}
```

### Step 3 — Retrieve the invitation again

```http
GET /api/public/invitations/DEMO-FAMILY-001
```

Response now contains:

```json
{
  "displayName": "Familia García",
  "maxGuests": 4,
  "expiresAt": null,
  "existingResponse": {
    "guestName": "Ana García",
    "contact": "ana@example.com",
    "attendanceConfirmed": true,
    "attendeeCount": 3,
    "intolerances": null,
    "additionalComment": null
  }
}
```

The database still contains only one RSVP associated with that invitation.

---

# 18. Endpoint summary

| Method | Endpoint | Authentication | Purpose |
|---|---|---|---|
| `GET` | `/api/public/health` | Public | Application health |
| `GET` | `/api/public/invitations/{code}` | Public | Retrieve one invitation |
| `PUT` | `/api/public/invitations/{code}/response` | Public | Create or update RSVP |
| `GET` | `/api/admin/dashboard` | ADMIN | Retrieve summary statistics |
| `GET` | `/api/admin/responses` | ADMIN | Retrieve administration list |
| `GET` | `/api/admin/responses.csv` | ADMIN | Export RSVP information |

This is the complete application API currently intended for Admitionum v1.1.