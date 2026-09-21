# Admitionum Architecture

## 1. Overview

Admitionum is a monolithic web application developed with Java and Spring Boot.

The application combines the frontend, REST API, business logic, security, and database access in a single deployable application.

The production application runs as a Docker container in Azure Container Apps and stores its persistent data in Azure SQL Database.

```text
Users
  |
  | HTTPS
  v
Azure Container Apps
  |
  v
Admitionum
Spring Boot
  |
  | JDBC / TLS
  v
Azure SQL Database
```

The project intentionally uses a monolithic architecture.

For the current scope, separating the application into multiple services would add deployment, networking, authentication, and operational complexity without providing a meaningful benefit.

---

## 2. Main technologies

The application is built with:

| Area | Technology |
|---|---|
| Language | Java 25 |
| Application framework | Spring Boot 4.1.0 |
| Web layer | Spring Web MVC |
| Persistence | Spring Data JPA |
| Validation | Jakarta Bean Validation |
| Authentication | Spring Security |
| Local database | H2 |
| Production database | Azure SQL Database |
| Frontend | HTML, CSS and vanilla JavaScript |
| Build tool | Maven |
| Containerization | Docker |
| Container registry | Azure Container Registry |
| Hosting | Azure Container Apps |
| Automation | GitHub Actions |
| Azure CI/CD authentication | OpenID Connect (OIDC) |

No frontend JavaScript framework is required because the application has a relatively small and focused user interface.

---

## 3. High-level architecture

Admitionum contains two user-facing areas.

```text
                         Admitionum
                             |
             +---------------+---------------+
             |                               |
             v                               v
       Public area                    Administration area
             |                               |
    General RSVP form                 Protected dashboard
             |                               |
      /api/public/**                    /api/admin/**
             |                               |
             +---------------+---------------+
                             |
                             v
                       Spring Boot
                             |
                             v
                      Spring Data JPA
                             |
                             v
                         Database
```

The public area is used by wedding guests.

The administration area is used by the application administrator.

Both are part of the same Spring Boot application.

---

## 4. Monolithic application

Admitionum is deployed as one application rather than as separate frontend and backend services.

Spring Boot serves:

```text
Public HTML
Public JavaScript
Public CSS
Administration HTML
Administration JavaScript
REST API
Authentication
Business logic
Database access
```

This means that the browser and REST API normally use the same host.

For example:

```text
https://example.com/
https://example.com/admin/
https://example.com/api/public/health
https://example.com/api/public/registrations
https://example.com/api/public/invitations/{code}
https://example.com/api/admin/dashboard
```

This avoids the need for a separate frontend deployment and removes unnecessary cross-origin configuration.

---

## 5. Backend layers

The Java backend follows a simple layered structure:

```text
Controller
    |
    v
Service
    |
    v
Repository
    |
    v
Spring Data JPA
    |
    v
Database
```

Each layer has a different responsibility.

### Controller layer

Controllers receive HTTP requests and return HTTP responses.

Main controller package:

```text
src/main/java/nc/admitionum/controller/
```

Important controllers include:

```text
HealthController
PublicRegistrationController
PublicInvitationController
AdminController
```

A controller should not contain database access logic.

For example, the main public registration follows this path:

```text
POST /api/public/registrations
              |
              v
PublicRegistrationController
              |
              v
InvitationService
```

The controller receives the request and delegates the operation to the service layer.

---

## 6. Service layer

Services contain the main application and business rules.

Main package:

```text
src/main/java/nc/admitionum/service/
```

The current principal services are:

```text
InvitationService
AdminService
CsvExportService
```

### InvitationService

`InvitationService` handles both the main public registration workflow and the compatibility workflow based on invitation codes.

Its responsibilities include:

```text
Register a response without requiring a guest access code
Generate a random internal access code
Create an Invitation and its RsvpResponse in one transaction
Validate the attendee count
Normalize user input
Find an invitation by access code for compatibility
Validate that a compatibility invitation exists, is active and has not expired
Retrieve, create or update an RSVP through the compatibility endpoints
```

The controllers therefore do not need to know how these rules are implemented.

The new registration method is transactional. If either entity cannot be saved, Spring rolls back the complete operation so that an orphan `Invitation` is not retained.

### AdminService

`AdminService` prepares information for the protected administration area.

It reads invitations and responses and calculates information such as:

```text
Total invitations
Answered invitations
Pending invitations
Confirmed invitations
Declined invitations
Confirmed attendees
Responses containing intolerance information
```

It also determines the current status of each invitation.

Possible states are:

```text
PENDING
CONFIRMED
DECLINED
DISABLED
EXPIRED
```

### CsvExportService

`CsvExportService` generates the downloadable CSV file used from the administration panel.

The service obtains the application data, converts each response into CSV format, and applies output protection before returning the final file.

---

## 7. Repository layer

Repositories are responsible for database access.

Main package:

```text
src/main/java/nc/admitionum/repository/
```

The principal repositories are:

```text
InvitationRepository
RsvpResponseRepository
```

They use Spring Data JPA.

Conceptually:

```text
Service
   |
   v
Repository
   |
   v
Hibernate / JPA
   |
   v
SQL database
```

Spring Data JPA provides common persistence operations such as:

```text
findAll()
save()
saveAndFlush()
```

The project can also define repository methods based on property names, such as finding an invitation by its access code.

This avoids placing SQL queries directly inside controllers.

---

## 8. Domain model

The application has two principal persistent entities:

```text
Invitation
RsvpResponse
```

Their relationship is:

```text
Invitation 1 -------- 0..1 RsvpResponse
```

The main general registration creates one `Invitation` and one associated `RsvpResponse` in the same transaction.

An invitation can still exist without a response because the compatibility workflow remains available and the database relationship continues to allow zero or one response.

Each valid submission to the general registration endpoint creates an independent pair. The application does not attempt to identify or merge people by name or contact details.

Through the compatibility endpoint, a later submission for the same access code updates the existing response rather than creating a second response for that invitation.

---

## 9. Invitation entity

`Invitation` represents the persistent invitation associated with an RSVP.

In the main public flow it is created automatically by the backend. It can also represent a previously created invitation used through the compatibility API.

Important information includes:

| Field | Purpose |
|---|---|
| `id` | Internal database identifier |
| `accessCode` | Unique random internal code and compatibility identifier |
| `displayName` | Human-readable invitation name |
| `maxGuests` | Maximum allowed attendees |
| `isActive` | Whether the invitation can be used |
| `expiresAt` | Optional expiration date |
| `createdAt` | Creation timestamp |
| `updatedAt` | Last modification timestamp |

For a general registration, the backend generates the access code. The guest does not enter it, choose it, or receive it in the registration response.

The existing access-code API can still use it to identify a compatibility invitation.

The internal numeric identifier is not required by the public frontend.

---

## 10. RSVP response entity

`RsvpResponse` contains the data submitted by the guest.

The current form stores:

| Field | Purpose |
|---|---|
| `guestName` | Name entered by the guest |
| `contact` | Phone number or email address |
| `attendanceConfirmed` | Whether attendance was confirmed |
| `attendeeCount` | Number of attendees |
| `intolerances` | Optional food intolerance information |
| `additionalComment` | Optional additional comment |
| `submittedAt` | Initial submission timestamp |
| `updatedAt` | Most recent update timestamp |

`RsvpResponse` contains a one-to-one reference to its `Invitation`.

The database also enforces that an invitation cannot have multiple RSVP responses.

---

## 11. DTO layer

The REST API does not directly return JPA entities.

Instead, it uses Data Transfer Objects (DTOs).

DTO packages are located under:

```text
src/main/java/nc/admitionum/dto/
```

They are divided into public and administrative API models.

```text
dto/
|
|-- publicapi/
|   |-- InvitationPublicResponse
|   |-- ExistingRsvpResponse
|   |-- SaveRsvpRequest
|   `-- SaveRsvpResponse
|
`-- admin/
    |-- AdminDashboardResponse
    `-- AdminRsvpResponse
```

This creates a boundary between:

```text
Database model
      |
      X
      |
Public API model
```

The client therefore receives only the information that the API intentionally exposes.

For example, the public invitation response does not need to expose the internal database identifier.

---

## 12. Validation

Validation is performed at more than one level.

```text
Browser
   |
   | Basic user experience validation
   v
Spring MVC
   |
   | Jakarta Bean Validation
   v
Service layer
   |
   | Business rules
   v
Database
   |
   | SQL constraints
   v
Stored data
```

The backend remains authoritative.

JavaScript validation improves the user experience, but a client cannot bypass the backend rules by sending a custom HTTP request.

For example, `SaveRsvpRequest` validates basic properties such as:

```text
Guest name is required
Contact is required
Attendance decision is required
Attendee count is required
Maximum text lengths
```

The service then validates the relationship between the attendance decision and the attendee count.

For a confirmed general registration:

```text
1 <= attendeeCount <= 20
```

The automatically created invitation uses `maxGuests = 20`, which allows the compatibility model and existing database constraints to be reused without a schema change.

For a confirmed response submitted through the compatibility endpoint:

```text
1 <= attendeeCount <= invitation.maxGuests
```

For a declined invitation:

```text
attendeeCount = 0
```

---

## 13. General public registration flow

All guests can access the same public URL directly or by scanning a common QR code.

The main flow is:

```text
Shared URL or QR
       |
       v
GET /
       |
       v
General public form
       |
       | POST /api/public/registrations
       | JSON validated with @Valid SaveRsvpRequest
       v
PublicRegistrationController
       |
       v
InvitationService
       |
       | @Transactional
       +--> Generate a random unique access code
       +--> Create Invitation
       `--> Create associated RsvpResponse
                         |
                         v
                      Database
```

The guest provides only the RSVP form data. No access code, database identifier, password, or other technical value is required.

The automatically created invitation uses:

```text
displayName = submitted guest name
maxGuests = 20
isActive = true
expiresAt = null
```

The generated access code is stored internally to preserve the existing model and is protected by the database uniqueness constraint. It is not returned by the registration endpoint.

The invitation and response are saved within one transaction. If creating or saving either entity fails, the complete operation is rolled back.

---

## 14. Invitation-code compatibility flow

The earlier endpoints remain available so the existing architecture is not removed unnecessarily:

```text
GET /api/public/invitations/{code}
PUT /api/public/invitations/{code}/response
```

For these endpoints, `PublicInvitationController` delegates to `InvitationService`.

The service verifies that the invitation:

```text
Exists
Is active
Has not expired
```

The `GET` response can include an existing RSVP so a compatible client can populate previously submitted values.

The `PUT` operation creates a response when none exists or updates the single response already associated with that invitation. It also enforces the invitation-specific `maxGuests` value.

These endpoints are not used by the main form at `/`, which submits new registrations without an access code.

---

## 15. Administration flow

The administration area is located under:

```text
/admin/
```

Administrative API endpoints use:

```text
/api/admin/**
```

The request flow is:

```text
Administrator
      |
      v
Spring Security
      |
      | Authenticated with ROLE_ADMIN
      v
Administration frontend
      |
      v
AdminController
      |
      +--> AdminService
      |
      +--> CsvExportService
      |
      v
Repositories
      |
      v
Database
```

The administration interface is primarily read-only.

It allows the administrator to inspect responses, calculate summary information, apply frontend filters, and download a CSV export.

---

## 16. Spring Security

Security is configured in:

```text
src/main/java/nc/admitionum/config/SecurityConfig.java
```

Public resources include the application homepage, static assets, and public API.

Conceptually:

```text
/
index.html
/css/**
/js/**
/api/public/**
```

Administrative routes require the `ADMIN` role:

```text
/admin/**
/api/admin/**
```

Authentication uses Spring Security form login and an HTTP session.

The project does not use JWT authentication.

The current administrator account is created in memory at application startup.

Its username and password are provided through application configuration.

In production, those values come from environment variables.

The administrator password is therefore not stored directly in the Java source code.

---

## 17. Local development architecture

Local development uses H2.

```text
Browser
   |
   v
Spring Boot
   |
   v
H2
```

The default Spring profile is:

```text
local
```

The local database is persistent, allowing development data to remain available after restarting the application.

This environment does not require Azure resources.

---

## 18. Test architecture

Automated tests use a separate H2 database in memory.

```text
Automated tests
      |
      v
Spring Boot test context
      |
      v
H2 in-memory database
```

This prevents automated tests from modifying the developer's local persistent database.

The test database is created for the test execution and discarded afterwards.

This keeps:

```text
Local development data
```

separate from:

```text
Automated test data
```

---

## 19. Production architecture

Production uses Azure SQL Database.

```text
Internet
   |
   | HTTPS
   v
Azure Container Apps
   |
   | Spring Boot
   |
   | JDBC + encrypted connection
   v
Azure SQL Database
```

Spring Boot runs with the:

```text
prod
```

profile.

Production database settings are provided using environment variables:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
```

Administrator credentials are also externalized:

```text
ADMIN_USERNAME
ADMIN_PASSWORD
```

The production configuration therefore does not require real credentials to be stored in the Git repository.

---

## 20. Docker architecture

Admitionum is packaged as an executable Spring Boot JAR.

The build process creates:

```text
target/admitionum-0.0.1-SNAPSHOT.jar
```

The Docker image contains a Java 25 runtime and the application JAR.

Conceptually:

```text
Maven
  |
  | package
  v
Application JAR
  |
  | docker build
  v
Docker image
  |
  v
Container
```

The container starts the application with:

```text
java -jar app.jar
```

The application listens on port:

```text
8080
```

---

## 21. Azure container architecture

Production introduces two Azure container services.

```text
Azure Container Registry
          |
          | Stores Docker image
          v
Azure Container Apps
          |
          | Runs Docker image
          v
Admitionum
```

### Azure Container Registry

Azure Container Registry stores versioned Admitionum container images.

It is not the application host.

Its responsibility is to act as a private container image registry.

### Azure Container Apps

Azure Container Apps runs the application container.

Its responsibilities include:

```text
Container execution
HTTPS ingress
Application revisions
Environment variable configuration
Secret references
Scaling
```

The container itself contains the Spring Boot application but not the Azure SQL database.

---

## 22. Complete production architecture

The complete production flow is:

```text
                        GitHub repository
                              |
                              v
                        GitHub Actions
                              |
                             OIDC
                              |
                              v
                          Microsoft Azure
                              |
             +----------------+----------------+
             |                                 |
             v                                 v
Azure Container Registry             Azure Container Apps
             |                                 |
             | Docker image                    | Runs image
             +---------------->----------------+
                                               |
                                               | JDBC / TLS
                                               v
                                      Azure SQL Database
```

For application users:

```text
Guest
  |
  | HTTPS
  v
Azure Container Apps
  |
  v
Public frontend + public API


Administrator
  |
  | HTTPS
  v
Azure Container Apps
  |
  v
Spring Security
  |
  v
Admin frontend + admin API
```

---

## 23. CI/CD architecture

The repository contains a GitHub Actions workflow under:

```text
.github/workflows/deploy.yml
```

The pipeline separates validation from deployment.

### Pull request flow

For pull requests targeting `main`:

```text
Pull request
    |
    v
Checkout
    |
    v
Java 25
    |
    v
Maven clean verify
    |
    v
Package application
```

A pull request does not deploy to Azure.

This allows changes to be tested before they reach the production branch.

### Main branch flow

For a push to `main`:

```text
Push to main
     |
     v
Test job
     |
     | Maven clean verify
     v
Tested JAR
     |
     v
Docker build
     |
     v
Azure login using OIDC
     |
     v
Push image to ACR
     |
     v
Update Container App
     |
     v
Verify deployed image
     |
     v
Verify health endpoint
```

Deployment only occurs after the test job succeeds.

---

## 24. OIDC authentication

GitHub Actions authenticates against Azure using OpenID Connect.

The deployment workflow uses identifiers such as:

```text
AZURE_CLIENT_ID
AZURE_TENANT_ID
AZURE_SUBSCRIPTION_ID
```

GitHub requests a short-lived identity token during the workflow.

Conceptually:

```text
GitHub Actions
      |
      | OIDC token
      v
Microsoft Entra ID
      |
      | Federated identity validated
      v
Azure access
```

This means the repository does not need to store a long-lived Azure client secret for the deployment identity.

OIDC is used for deployment authentication only.

It is not used for the application's administrator login.

---

## 25. Docker image traceability

Production Docker images are tagged using the Git commit SHA.

Conceptually:

```text
Git commit
    |
    v
GitHub Actions
    |
    v
Docker image
    |
    | tag = commit SHA
    v
Azure Container Registry
    |
    v
Azure Container Apps revision
```

This makes it possible to identify which source revision produced a deployed container.

---

## 26. Health verification

Admitionum exposes:

```http
GET /api/public/health
```

The endpoint provides a lightweight way to confirm that the application is responding.

After deployment, GitHub Actions calls this endpoint.

The deployment is considered successful only when the expected application health response is returned.

This adds a final verification step after updating Azure Container Apps.

---

## 27. Data boundaries

The browser never connects directly to Azure SQL.

The correct path is:

```text
Browser
   |
   | HTTP / JSON
   v
Spring Boot API
   |
   | JPA / JDBC
   v
Azure SQL
```

The following architecture is deliberately avoided:

```text
Browser
   |
   X
   |
Azure SQL
```

Database credentials therefore remain on the server side.

The public client only interacts with the REST API.

---

## 28. Public and private data

The main public API accepts one anonymous RSVP registration at a time.

It does not expose the complete RSVP database, internal database identifiers, or the access code generated for a new registration.

The compatibility API can retrieve and update only the invitation identified by a known access code.

Administrative information is accessed through authenticated endpoints.

```text
Public user
    |
    +--> POST /api/public/registrations
    |         |
    |         `--> Submit one new RSVP
    |
    `--> Compatibility endpoints with known code
              |
              `--> Retrieve or update one invitation
```

Compared with:

```text
Administrator
    |
    v
/api/admin/**
    |
    v
All invitation and RSVP information
```

Spring Security provides the boundary between both areas.

---

## 29. Time handling

Internal application timestamps are handled using UTC.

Examples include:

```text
Invitation creation
Invitation updates
RSVP submission
RSVP updates
Expiration checks
```

Using one time reference avoids mixing local server time with production cloud time.

Presentation-specific local time conversion can be performed separately when necessary.

---

## 30. Database schema ownership

The production SQL structure is documented in:

```text
database/schema.sql
```

The production profile uses:

```text
spring.jpa.hibernate.ddl-auto=validate
```

This is an important architectural decision.

In production, Hibernate verifies that the database matches the entity model but does not automatically create or modify the production schema.

Therefore:

```text
schema.sql
    |
    | Creates database structure
    v
Azure SQL
```

and:

```text
Spring Boot
    |
    | validate
    v
Checks schema compatibility
```

This separates database creation from normal application startup.

---

## 31. Why H2 and Azure SQL are both used

The application uses different databases for different environments.

```text
Local development --> H2 persistent
Automated tests   --> H2 in-memory
Production        --> Azure SQL
```

H2 keeps local development simple.

Azure SQL provides the production relational database.

JPA provides the abstraction used by the Java application, while production-specific differences are validated before deployment.

---

## 32. Project package structure

The main Java package is:

```text
nc.admitionum
```

Its main structure is:

```text
nc.admitionum
|
|-- config
|   `-- SecurityConfig
|
|-- controller
|   |-- HealthController
|   |-- PublicRegistrationController
|   |-- PublicInvitationController
|   `-- AdminController
|
|-- dto
|   |-- publicapi
|   `-- admin
|
|-- exception
|
|-- model
|   |-- Invitation
|   `-- RsvpResponse
|
|-- repository
|   |-- InvitationRepository
|   `-- RsvpResponseRepository
|
`-- service
    |-- InvitationService
    |-- AdminService
    `-- CsvExportService
```

The main application class remains at the package root:

```text
nc.admitionum.AdmitionumApplication
```

Keeping it at the root allows Spring Boot component scanning to discover the application classes in its subpackages.

---

## 33. Frontend structure

Static frontend resources are served directly by Spring Boot.

They are located under:

```text
src/main/resources/static/
```

The structure includes:

```text
static/
|
|-- index.html
|
|-- admin/
|   `-- index.html
|
|-- css/
|   `-- styles.css
|
`-- js/
    |-- public-app.js
    `-- admin-app.js
```

`public-app.js` submits the general guest form to `POST /api/public/registrations` and presents validation or confirmation messages.

`admin-app.js` handles dashboard data, filters, and administration presentation.

Neither JavaScript file connects directly to the database.

---

## 34. Deliberate architecture limitations

Admitionum is a focused application and intentionally avoids unnecessary infrastructure.

The current architecture does not use:

```text
Microservices
Kubernetes
React
Angular
Vue
JWT
Azure Functions
Azure Spring Apps
A separate frontend service
A separate authentication service
Guest accounts
Email or SMS verification
Advanced identity or duplicate detection
CAPTCHA
Complex application-level rate limiting
```

These are not missing accidentally.

They were excluded because the application requirements can be satisfied with a simpler architecture.

Each valid general-form submission is therefore treated as an independent registration. Two people may share the same name, so a name alone is not used to reject a submission as a duplicate.

The goal is to demonstrate a complete and understandable application lifecycle rather than maximize the number of technologies used.

---

## 35. Architecture summary

Admitionum can be summarized as:

```text
HTML / CSS / JavaScript
          |
          v
Spring Boot MVC
          |
          v
Controllers
          |
          v
Services
          |
          v
Spring Data JPA
          |
          v
Azure SQL
```

with:

```text
Spring Security
```

protecting administration access, and:

```text
GitHub Actions
      |
      v
Docker
      |
      v
Azure Container Registry
      |
      v
Azure Container Apps
```

providing the production delivery pipeline.

The result is a small but complete full-stack application covering frontend development, REST APIs, validation, persistence, authentication, containerization, cloud deployment, and CI/CD.