# Admitionum

Admitionum is a full-stack wedding RSVP management application built with Java and Spring Boot.

Guests can access an individual invitation, confirm or decline attendance, specify the number of attendees, provide contact information, report food intolerances, and add an optional comment.

Administrators have access to a protected dashboard where they can review RSVP information, filter responses, view attendance statistics, and export the data as CSV.

The application is containerized with Docker, deployed to Microsoft Azure, and automatically tested and deployed through GitHub Actions.

---

## Features

### Guest area

- Individual invitation access codes.
- Retrieve an existing invitation.
- Confirm or decline attendance.
- Specify the number of attendees.
- Flexible contact field for phone numbers or email addresses.
- Optional food intolerance information.
- Optional additional comments.
- Update an existing RSVP using the same invitation.
- Backend validation of invitation status and maximum attendee count.

### Administration area

- Protected administration page.
- Session-based authentication with Spring Security.
- RSVP overview dashboard.
- Total invitation count.
- Pending invitation count.
- Confirmed attendee count.
- Responses containing food intolerance information.
- Search by invitation name, guest name, or contact information.
- Filter responses by status.
- Filter responses containing intolerance information.
- CSV export.

---

## Technology stack

### Backend

- Java 25
- Spring Boot 4.1.0
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Jakarta Bean Validation
- Maven

### Frontend

- HTML5
- CSS3
- Vanilla JavaScript

No JavaScript frontend framework is used.

### Databases

- H2 for local development and automated tests
- Azure SQL Database for production

### Cloud and DevOps

- Docker
- Azure Container Registry
- Azure Container Apps
- GitHub Actions
- Azure OIDC authentication for CI/CD

---

## Architecture

Admitionum is intentionally implemented as a monolithic Spring Boot application.

The same application serves:

- The public HTML, CSS, and JavaScript frontend.
- The public REST API.
- The protected administration frontend.
- The protected administration REST API.

The frontend never connects directly to the database.

```text
                           GitHub
                              |
                       GitHub Actions
                              |
                             OIDC
                              |
                              v
Guest / Administrator --> Azure Container Apps
                              |
                        Admitionum
                        Spring Boot
                              |
                             JPA
                              |
                              v
                      Azure SQL Database

Docker images are stored in
Azure Container Registry.
```

The internal backend flow follows a simple layered structure:

```text
HTTP Request
     |
     v
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

More information is available in [Architecture](docs/architecture.md).

---

## RSVP flow

A guest receives an invitation URL containing an individual access code.

The browser uses that code to retrieve the invitation:

```http
GET /api/public/invitations/{code}
```

The API verifies that the invitation:

- Exists.
- Is active.
- Has not expired.

The guest then submits the RSVP:

```http
PUT /api/public/invitations/{code}/response
```

If the invitation already has an RSVP, the existing response is updated instead of creating a duplicate.

Each invitation can therefore have zero or one RSVP response.

```text
Invitation 1 -------- 0..1 RsvpResponse
```

---

## RSVP data

The current RSVP form contains:

| Field | Description |
|---|---|
| Guest name | Name entered by the guest |
| Contact | Phone number or email address |
| Attendance confirmed | Whether the guest will attend |
| Attendee count | Number of people attending |
| Intolerances | Optional food intolerance information |
| Additional comment | Optional additional information |

If attendance is confirmed, the attendee count must be between `1` and the maximum number configured for the invitation.

If attendance is declined, the attendee count must be `0`.

These rules are enforced by the backend.

---

## Public API

Main public endpoints:

```text
GET  /api/public/health

GET  /api/public/invitations/{code}

PUT  /api/public/invitations/{code}/response
```

The public API never exposes the complete invitation list or administration data.

See [API documentation](docs/api.md) for the complete contracts.

---

## Administration API

The administration endpoints are protected by Spring Security:

```text
GET /api/admin/dashboard

GET /api/admin/responses

GET /api/admin/responses.csv
```

Access requires an authenticated user with the `ADMIN` role.

The administration credentials are provided to the application through environment variables and are never stored directly in the source code.

---

## Security

The project applies several security measures:

- Administration routes are protected with Spring Security.
- Authentication uses an HTTP session rather than exposing credentials to JavaScript.
- Database credentials are provided through environment variables.
- GitHub Actions authenticates against Azure using OIDC.
- No Azure client secret is required by the deployment workflow.
- The frontend never receives database credentials.
- Public endpoints only expose data required by an individual invitation.
- Backend validation is authoritative.
- Production database traffic uses an encrypted Azure SQL connection.
- Secrets and local environment files are excluded from Git.

A more detailed explanation is available in [Security](docs/security.md).

---

## Local development

### Requirements

You need:

- Java 25
- Git
- An Internet connection for the first Maven dependency download

The project includes the Maven Wrapper, so installing Maven globally is not required.

Clone the repository:

```bash
git clone https://github.com/Franer-Narf/Admitionum.git
cd Admitionum
```

Run the automated tests:

### Windows PowerShell

```powershell
.\mvnw.cmd clean verify
```

### Git Bash / Linux / macOS

```bash
./mvnw clean verify
```

Start the application by first defining local administrator credentials:

### Windows PowerShell

```powershell
$env:ADMIN_USERNAME="admin"
$env:ADMIN_PASSWORD="choose-a-local-password"

.\mvnw.cmd spring-boot:run
```

### Git Bash / Linux / macOS

```bash
export ADMIN_USERNAME="admin"
export ADMIN_PASSWORD="choose-a-local-password"

./mvnw spring-boot:run
```

These credentials are only for local development and must not be committed to Git.

### Windows PowerShell

```powershell
.\mvnw.cmd spring-boot:run
```

### Git Bash / Linux / macOS

```bash
./mvnw spring-boot:run
```

The local application is then available at:

```text
http://localhost:8080
```

The health endpoint is:

```text
http://localhost:8080/api/public/health
```

Local development uses an H2 database and does not require an Azure SQL connection.

For a complete setup guide, see [Installation and deployment guide](docs/installation-guide.md).

---

## Profiles

Admitionum separates its runtime environments using Spring profiles.

### Local

The default local profile uses:

```text
H2 persistent database
```

This environment is intended for development.

### Test

Automated tests use:

```text
H2 in-memory database
```

This keeps test data isolated from the local development database.

### Production

The production profile uses:

```text
Azure SQL Database
```

Production connection settings are injected using environment variables.

---

## Docker

The application can be packaged as a JAR:

### Windows

```powershell
.\mvnw.cmd clean package
```

### Git Bash / Linux / macOS

```bash
./mvnw clean package
```

Build the container image:

```bash
docker build -t admitionum .
```

Run it locally:

### Windows PowerShell

```powershell
docker run `
    --rm `
    -p 8080:8080 `
    -e ADMIN_USERNAME="admin" `
    -e ADMIN_PASSWORD="choose-a-local-password" `
    admitionum
```

### Git Bash / Linux / macOS

```bash
docker run \
    --rm \
    -p 8080:8080 \
    -e ADMIN_USERNAME="admin" \
    -e ADMIN_PASSWORD="choose-a-local-password" \
    admitionum
```

Production database and administrator settings must be supplied through environment variables.

---

## Azure deployment

The production architecture uses:

```text
Azure Container Registry
        |
        | Docker image
        v
Azure Container Apps
        |
        | JDBC over TLS
        v
Azure SQL Database
```

The application runs using the `prod` Spring profile.

Sensitive values are configured as Azure Container Apps secrets and environment variables rather than being committed to Git.

The detailed replication procedure is documented in [Installation and deployment guide](docs/installation-guide.md).

---

## CI/CD

Admitionum includes a GitHub Actions CI/CD workflow.

### Pull requests to `main`

The workflow:

```text
Checkout
   |
Set up Java 25
   |
Run Maven clean verify
   |
Package tested JAR
```

No Azure deployment is performed from pull requests.

### Push to `main`

After the test job succeeds:

```text
Build Docker image
        |
Authenticate to Azure using OIDC
        |
Push image to Azure Container Registry
        |
Update Azure Container App
        |
Verify deployed image
        |
Verify public health endpoint
```

Each production image uses the Git commit SHA as its image tag.

This creates traceability between:

```text
Git commit
    |
Docker image
    |
Azure Container Apps revision
```

---

## Repository structure

```text
Admitionum/
|
|-- src/
|   |-- main/
|   |   |-- java/nc/admitionum/
|   |   |   |-- config/
|   |   |   |-- controller/
|   |   |   |-- dto/
|   |   |   |-- exception/
|   |   |   |-- model/
|   |   |   |-- repository/
|   |   |   `-- service/
|   |   |
|   |   `-- resources/
|   |       |-- static/
|   |       |   |-- admin/
|   |       |   |-- css/
|   |       |   `-- js/
|   |       |
|   |       |-- application.properties
|   |       |-- application-local.properties
|   |       `-- application-prod.properties
|   |
|   `-- test/
|
|-- database/
|   |-- schema.sql
|   `-- ...
|
|-- docs/
|   |-- architecture.md
|   |-- api.md
|   |-- installation-guide.md
|   `-- security.md
|
|-- .github/
|   `-- workflows/
|       `-- deploy.yml
|
|-- Dockerfile
|-- pom.xml
|-- .env.example
`-- README.md
```

---

## Testing

The project contains automated tests covering the main application layers and behaviours.

The complete test suite can be executed with:

```bash
./mvnw clean verify
```

or on Windows:

```powershell
.\mvnw.cmd clean verify
```

The same verification command is executed by GitHub Actions before a deployment is allowed.

---

## Design decisions

Some choices were deliberately kept simple because Admitionum is a focused application rather than a general-purpose event management platform.

Examples include:

- Monolithic architecture rather than microservices.
- Vanilla JavaScript rather than a frontend framework.
- Session authentication rather than JWT.
- A single administration role.
- H2 for local development.
- Azure SQL for production.
- Docker and Azure Container Apps for deployment.

These decisions reduce unnecessary infrastructure while still demonstrating a complete application lifecycle.

---

## Documentation

Detailed project documentation:

- [Architecture](docs/architecture.md)
- [REST API](docs/api.md)
- [Installation and deployment](docs/installation-guide.md)
- [Security](docs/security.md)

---

## What this project demonstrates

Admitionum was developed as a practical learning project covering the complete path from local application development to an automated Azure deployment.

The project demonstrates experience with:

- Java backend development.
- Spring Boot.
- REST API design.
- Relational database modelling.
- Spring Data JPA.
- Input validation.
- Spring Security.
- HTML, CSS, and JavaScript.
- Automated testing.
- Maven.
- Docker.
- Azure SQL Database.
- Azure Container Registry.
- Azure Container Apps.
- GitHub workflows.
- CI/CD.
- Azure authentication with OIDC.
- Configuration and secret management.

---

## License

A project license will be added before the final portfolio release.