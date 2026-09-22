# Admitionum REST API

## 1. Overview

Admitionum exposes a small REST API used by the general public RSVP form and the protected administration dashboard.

The API is divided into two areas:

```text
/api/public/**
/api/admin/**
```

Public endpoints can be accessed without administrator authentication.

Administration endpoints require an authenticated Spring Security session with the `ADMIN` role.

The frontend and API are served by the same Spring Boot application.

The main guest endpoint is:

```http
POST /api/public/registrations
```

It accepts one RSVP and creates its `Invitation` and associated `RsvpResponse` automatically.

The earlier endpoints based on an invitation access code remain available for compatibility, but the main form at `/` no longer uses them.

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

## 3. Content types

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

## 5. Create a general public registration

This is the endpoint used by the main public form.

The guest does not need an access code, invitation identifier, account, password, or any other technical value.

### Request

```http
POST /api/public/registrations
Content-Type: application/json
```

### Authentication

Not required.

The endpoint is included under `/api/public/**` and is therefore publicly accessible through the existing Spring Security configuration.

### CSRF

CSRF protection is ignored for `/api/public/**` by the current security configuration, so this JSON request does not require a CSRF token.

---

### 5.1. Request body for confirmed attendance

```json
{
  "guestName": "Ana GarcÃ­a",
  "contact": "ana@example.com",
  "attendanceConfirmed": true,
  "attendeeCount": 3,
  "intolerances": "Una persona es intolerante a la lactosa",
  "additionalComment": "Llegaremos el viernes por la tarde"
}
```

---

### 5.2. Request body for declined attendance

```json
{
  "guestName": "Carlos LÃ³pez",
  "contact": "600123123",
  "attendanceConfirmed": false,
  "attendeeCount": 0,
  "intolerances": "",
  "additionalComment": "Sentimos no poder acompaÃ±aros"
}
```

---

### 5.3. Request fields

| Field | Type | Required | Validation |
|---|---|---:|---|
| `guestName` | String | Yes | Between 2 and 200 characters |
| `contact` | String | Yes | Between 3 and 200 characters |
| `attendanceConfirmed` | Boolean | Yes | Must be `true` or `false` |
| `attendeeCount` | Integer | Yes | Between 0 and 20 |
| `intolerances` | String | No | Maximum 500 characters |
| `additionalComment` | String | No | Maximum 1000 characters |

The `contact` field deliberately accepts different contact formats.

Valid examples include:

```text
600 123 123
+34 600 123 123
persona@example.com
```

The backend trims surrounding whitespace from the required text values.

Blank optional text is stored as `null` after normalization.

---

### 5.4. Attendance business rules

When attendance is confirmed:

```text
1 <= attendeeCount <= 20
```

When attendance is declined:

```text
attendeeCount = 0
```

These rules are enforced by the service layer in addition to Jakarta Bean Validation.

The general maximum is 20 because the automatically created invitation uses:

```text
maxGuests = 20
```

This preserves the existing domain model and database constraints without requiring a schema migration.

---

### 5.5. Transactional processing

For every valid request, the backend performs one transaction:

```text
POST /api/public/registrations
        |
        v
Validate SaveRsvpRequest
        |
        v
Generate unique random AccessCode
        |
        +--> Create Invitation
        |
        `--> Create associated RsvpResponse
                    |
                    v
                 Commit
```

The generated `Invitation` uses:

```text
accessCode = randomly generated internal value
displayName = guestName
maxGuests = 20
isActive = true
expiresAt = null
```

If either persistence operation fails, the transaction is rolled back.

Consequently, a failed response creation does not leave an orphan `Invitation` in the database.

---

### 5.6. Successful response

HTTP:

```text
201 Created
```

Example:

```json
{
  "success": true,
  "message": "Tu respuesta se ha guardado correctamente.",
  "updatedAt": "2027-03-15T17:30:00"
}
```

### Response fields

| Field | Type | Description |
|---|---|---|
| `success` | Boolean | Indicates that the registration was stored |
| `message` | String | Confirmation message shown to the guest |
| `updatedAt` | Date/time | Timestamp of the stored response |

The response deliberately does not expose:

```text
Invitation database ID
RsvpResponse database ID
Generated AccessCode
Administrator information
```

---

### 5.7. Independent registrations

Each successful `POST` creates a new `Invitation` and a new associated `RsvpResponse`.

The endpoint does not attempt to identify a returning guest or update an earlier general registration.

Two people can have the same name, so the API does not reject registrations using only `guestName` as a duplicate key.

Guest accounts, email verification, SMS verification, and advanced duplicate detection are outside the current scope.

---

## 6. Validation errors

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

The `fields` object contains only fields that failed validation.

Possible validation messages include:

| Field | Validation message |
|---|---|
| `guestName` | `El nombre del invitado es obligatorio.` |
| `guestName` | `El nombre debe tener entre 2 y 200 caracteres.` |
| `contact` | `El contacto es obligatorio.` |
| `contact` | `El contacto debe tener entre 3 y 200 caracteres.` |
| `attendanceConfirmed` | `Debes confirmar si asistirÃ¡s.` |
| `attendeeCount` | `El nÃºmero de asistentes es obligatorio.` |
| `attendeeCount` | `El nÃºmero de asistentes no puede ser negativo.` |
| `attendeeCount` | `El nÃºmero de asistentes no puede superar 20.` |
| `intolerances` | `Las intolerancias no pueden superar 500 caracteres.` |
| `additionalComment` | `El comentario no puede superar 1.000 caracteres.` |

---

### 6.1. Invalid attendee count

The service layer can also reject a combination that passes the basic field range but violates an attendance business rule.

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
    "message": "Debe asistir al menos una persona."
  }
}
```

Another possible message is:

```text
Cuando no se confirma asistencia, el nÃºmero de asistentes debe ser cero.
```

For the compatibility endpoint, another possible message is:

```text
El nÃºmero de asistentes supera el mÃ¡ximo permitido.
```

The error code remains:

```text
INVALID_GUEST_COUNT
```

---

# Access-code compatibility API

## 7. Compatibility overview

The following endpoints belong to the earlier individual-invitation workflow:

```http
GET /api/public/invitations/{code}
PUT /api/public/invitations/{code}/response
```

They remain available to preserve compatibility with the existing architecture.

They are not used by the main public form at `/` and a guest using the general URL does not need to know an access code.

---

## 8. Retrieve a compatibility invitation

Retrieves the public information associated with one known invitation access code.

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
| `code` | String | Existing invitation access code |

The code is trimmed before it is used.

An empty or invalid code is treated as an invitation that cannot be found.

---

### 8.1. Successful response without an existing RSVP

HTTP:

```text
200 OK
```

Example:

```json
{
  "displayName": "Familia GarcÃ­a",
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

The response does not expose the internal invitation database ID or the complete invitation list.

---

### 8.2. Successful response with an existing RSVP

```json
{
  "displayName": "Familia GarcÃ­a",
  "maxGuests": 4,
  "expiresAt": "2027-05-01T22:00:00",
  "existingResponse": {
    "guestName": "Ana GarcÃ­a",
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

---

### 8.3. Invitation not found

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
    "message": "No se ha encontrado una invitaciÃ³n vÃ¡lida para el cÃ³digo indicado."
  }
}
```

---

### 8.4. Disabled invitation

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
    "message": "La invitaciÃ³n estÃ¡ desactivada."
  }
}
```

---

### 8.5. Expired invitation

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
    "message": "El plazo para responder a la invitaciÃ³n ha finalizado."
  }
}
```

---

## 9. Create or update a compatibility RSVP

Stores the RSVP associated with an existing invitation code.

If no RSVP exists, a new response is created.

If an RSVP already exists for the same invitation, that response is updated.

### Request

```http
PUT /api/public/invitations/{code}/response
Content-Type: application/json
```

Example:

```http
PUT /api/public/invitations/DEMO-FAMILY-001/response
```

### Authentication

Not required.

### Request body

The request uses the same fields and Bean Validation rules as `POST /api/public/registrations`.

Example:

```json
{
  "guestName": "Ana GarcÃ­a",
  "contact": "ana@example.com",
  "attendanceConfirmed": true,
  "attendeeCount": 3,
  "intolerances": "Lactosa",
  "additionalComment": "Llegaremos el viernes"
}
```

### Invitation-specific attendance rule

For confirmed attendance:

```text
1 <= attendeeCount <= invitation.maxGuests
```

For declined attendance:

```text
attendeeCount = 0
```

### Successful response

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

# Administration API

## 10. Authentication model

Administration endpoints are located under:

```text
/api/admin/**
```

They are protected by Spring Security.

The user must have an authenticated HTTP session with:

```text
ROLE_ADMIN
```

Admitionum uses Spring Security form login.

The administration API does not use:

```text
JWT
Bearer tokens
API keys
OAuth tokens
```

When an unauthenticated browser requests a protected resource, Spring Security can redirect it to the login page.

The administration API is primarily consumed by the administration frontend running in the same Spring Boot application and authenticated session.

---

## 11. Dashboard

Returns aggregate invitation and RSVP statistics.

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

Registrations created through `POST /api/public/registrations` are included automatically because they use the existing `Invitation` and `RsvpResponse` entities and repositories.

---

## 12. List invitations and responses

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

### 12.1. Successful response

HTTP:

```text
200 OK
```

Example:

```json
[
  {
    "invitationId": 1,
    "displayName": "Ana GarcÃ­a",
    "maxGuests": 20,
    "status": "CONFIRMED",
    "guestName": "Ana GarcÃ­a",
    "contact": "ana@example.com",
    "attendeeCount": 3,
    "intolerances": "Lactosa",
    "additionalComment": "Llegaremos el viernes",
    "updatedAt": "2027-03-15T17:30:00"
  },
  {
    "invitationId": 2,
    "displayName": "Familia LÃ³pez",
    "maxGuests": 4,
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

The first item illustrates a registration created automatically through the general public endpoint.

The second item illustrates an unanswered invitation that can still exist through the compatibility model.

---

### 12.2. Administrative status

Possible values are:

```text
CONFIRMED
DECLINED
PENDING
DISABLED
EXPIRED
```

The status is calculated by the application. It is not stored as a separate RSVP status column.

#### CONFIRMED

An RSVP exists and:

```text
attendanceConfirmed = true
```

#### DECLINED

An RSVP exists and:

```text
attendanceConfirmed = false
```

#### DISABLED

No RSVP exists and the invitation is not active.

#### EXPIRED

No RSVP exists and the expiration date has passed.

#### PENDING

No RSVP exists, the invitation is active, and it has not expired.

If an RSVP already exists, `CONFIRMED` or `DECLINED` takes precedence over the invitation's later active or expiration state in the current administrative status calculation.

---

## 13. Export responses as CSV

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

Registrations created through the general public endpoint are included automatically.

---

### 13.1. CSV columns

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
"Ana GarcÃ­a","Ana GarcÃ­a","ana@example.com","CONFIRMED","3","Lactosa","Llegaremos el viernes","2027-03-10T12:00:00","2027-03-15T17:30:00"
```

The exact quoting of a value depends on its contents and CSV escaping requirements.

Values beginning with spreadsheet formula characters are neutralized before export to reduce CSV formula injection risk.

---

## 14. API error format

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

## 15. Documented application error codes

The application currently defines the following public error codes:

| Error code | HTTP status | Meaning |
|---|---:|---|
| `INVITATION_NOT_FOUND` | 404 | No compatibility invitation matches the supplied code |
| `INVITATION_DISABLED` | 410 | The compatibility invitation has been disabled |
| `INVITATION_EXPIRED` | 410 | The compatibility invitation response period has expired |
| `INVALID_GUEST_COUNT` | 400 | Attendance count violates a business rule |
| `VALIDATION_ERROR` | 400 | Request fields failed Jakarta Bean Validation |

Unexpected framework, transaction, or infrastructure failures are not part of the normal public API contract documented here.

---

## 16. Public API data boundary

The main public endpoint accepts one new registration and returns only its confirmation result.

It does not expose the generated access code or any internal database identifier.

The compatibility endpoints expose information for only one known invitation code.

The public API does not provide endpoints such as:

```text
GET /api/public/invitations
GET /api/public/responses
GET /api/public/registrations
```

A public client therefore cannot request the complete invitation or RSVP database through the documented API.

The main public flow is:

```text
Shared URL or QR
       |
       v
POST /api/public/registrations
       |
       v
One new Invitation and RsvpResponse
```

The compatibility flow is:

```text
Known access code
       |
       v
One invitation
       |
       v
Zero or one RSVP
```

---

## 17. Administration API data boundary

Complete invitation information is only available through:

```text
/api/admin/**
```

These endpoints require Spring Security authentication.

The separation is:

```text
Guest
  |
  v
/api/public/**
  |
  v
Submit one registration or use one known compatibility code
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

## 18. Date and time representation

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

## 19. Example main public flow

The complete general RSVP interaction requires one API request.

### Step 1 â€” Open the shared form

```http
GET /
```

The page is public and does not require a query parameter such as `?code=...`.

### Step 2 â€” Submit the RSVP

```http
POST /api/public/registrations
Content-Type: application/json
```

Body:

```json
{
  "guestName": "Ana GarcÃ­a",
  "contact": "ana@example.com",
  "attendanceConfirmed": true,
  "attendeeCount": 3,
  "intolerances": "",
  "additionalComment": ""
}
```

Response:

```text
201 Created
```

```json
{
  "success": true,
  "message": "Tu respuesta se ha guardado correctamente.",
  "updatedAt": "2027-03-15T17:30:00"
}
```

The backend has now committed one new `Invitation` and its associated `RsvpResponse`.

### Step 3 â€” Administrative visibility

After authenticating as an administrator, the new record is available through:

```http
GET /api/admin/responses
```

It is also included in:

```http
GET /api/admin/dashboard
GET /api/admin/responses.csv
```

---

## 20. Example compatibility flow

The earlier access-code interaction remains available but is no longer the main guest journey.

### Step 1 â€” Retrieve the invitation

```http
GET /api/public/invitations/DEMO-FAMILY-001
```

### Step 2 â€” Create or update its RSVP

```http
PUT /api/public/invitations/DEMO-FAMILY-001/response
Content-Type: application/json
```

Repeated successful `PUT` requests for the same invitation update its single associated RSVP.

---

## 21. Endpoint summary

| Method | Endpoint | Authentication | Purpose |
|---|---|---|---|
| `GET` | `/api/public/health` | Public | Application health |
| `POST` | `/api/public/registrations` | Public | Create a new Invitation and RSVP |
| `GET` | `/api/public/invitations/{code}` | Public, compatibility | Retrieve one existing invitation |
| `PUT` | `/api/public/invitations/{code}/response` | Public, compatibility | Create or update one existing invitation's RSVP |
| `GET` | `/api/admin/dashboard` | ADMIN | Retrieve summary statistics |
| `GET` | `/api/admin/responses` | ADMIN | Retrieve administration list |
| `GET` | `/api/admin/responses.csv` | ADMIN | Export RSVP information |

This is the complete application API currently intended for Admitionum after Phase 20.