# Admitionum Security

## 1. Purpose

This document describes the security model currently implemented by Admitionum.

It covers:

- Public and administration access.
- Spring Security.
- Administrator credentials.
- Anonymous public registration.
- Internal and compatibility invitation access codes.
- Database credentials.
- Azure SQL access.
- Browser data handling.
- CSV export protection.
- Docker secret handling.
- Azure Container Apps secrets.
- GitHub Actions and OIDC.
- Current limitations.

The purpose is to document the real application security model rather than describe an idealized enterprise architecture.

---

# 2. Security boundaries

Admitionum has two main security areas:

```text
PUBLIC AREA
    |
    v
/api/public/**
```

and:

```text
ADMINISTRATION AREA
    |
    v
/admin/**
/api/admin/**
```

They have different security requirements.

Guests do not create user accounts.

Administrators must authenticate through Spring Security.

---

# 3. Public area

The following resources are publicly accessible:

```text
/
/index.html
/css/**
/js/**
/api/public/**
/error
```

The public API provides only the operations required to submit an RSVP, use the retained compatibility flow, and verify application health.

Main endpoints:

```text
GET /api/public/health

POST /api/public/registrations

Compatibility:

GET /api/public/invitations/{code}

PUT /api/public/invitations/{code}/response
```

There is deliberately no public endpoint for retrieving all invitations or all RSVP responses.

For example, the application does not expose:

```text
GET /api/public/invitations
GET /api/public/responses
GET /api/public/registrations
```

The main public data boundary is:

```text
One anonymous POST request
          |
          v
Create one Invitation
          |
          v
Create one associated RSVP
```

The generated invitation access code and internal database identifiers are not returned to the guest.

The compatibility endpoints can retrieve or update only the invitation identified by a known code. They are no longer used by the main public form.

---

# 4. Administration area

Administrative resources are protected by Spring Security.

Protected routes include:

```text
/admin/**
/api/admin/**
```

Access requires:

```text
ROLE_ADMIN
```

An unauthenticated user cannot retrieve the administration dashboard or the complete RSVP information through these endpoints.

The main protected API endpoints are:

```text
GET /api/admin/dashboard
GET /api/admin/responses
GET /api/admin/responses.csv
```

---

# 5. Default-deny authorization

Admitionum uses an explicit authorization model.

Conceptually:

```text
Known public routes
        |
        v
permitAll()

Known administration routes
        |
        v
ROLE_ADMIN

Everything else
        |
        v
denyAll()
```

The Spring Security configuration ends with:

```text
anyRequest().denyAll()
```

This means a newly introduced route is not automatically accessible merely because a developer forgot to define its authorization rule.

It must be deliberately added to the security configuration.

---

# 6. Administrator authentication

Admitionum currently supports one administration account.

The administrator is created when Spring Boot starts.

It is stored using:

```text
InMemoryUserDetailsManager
```

The username and password come from application configuration:

```text
app.admin.username
app.admin.password
```

In production those values resolve to:

```text
ADMIN_USERNAME
ADMIN_PASSWORD
```

The source code therefore does not contain the real administration credentials.

---

# 7. Password handling

The administrator password is not stored directly inside Spring Security in plain text.

At application startup it is passed through the configured Spring Security password encoder.

Conceptually:

```text
ADMIN_PASSWORD
      |
      v
PasswordEncoder
      |
      v
Encoded password
      |
      v
In-memory administrator
```

The original environment variable is still required at startup, but the authentication system compares credentials through the password encoder rather than using a plain-text password entry in the user store.

---

# 8. Session-based authentication

Admitionum uses Spring Security form login.

It does not use:

```text
JWT
Bearer tokens
OAuth access tokens
Microsoft Entra login
```

After a successful administrator login, Spring Security maintains the authenticated state using an HTTP session.

Conceptually:

```text
Administrator
      |
      | username + password
      v
Spring Security
      |
      | authenticated session
      v
Administration area
```

The administration JavaScript uses:

```text
credentials: "same-origin"
```

when requesting protected API resources.

This allows the browser to send the same-origin session cookie created by Spring Security.

---

# 9. Logout

Spring Security provides the logout functionality.

The administration interface links to:

```text
/logout
```

Spring Security controls the logout process and redirects the user to:

```text
/
```

after logout succeeds.

The application does not implement its own JavaScript authentication token or manually delete authentication cookies.

---

# 10. CSRF protection

Spring Security CSRF protection remains enabled for authenticated application functionality.

The public API is explicitly excluded:

```text
/api/public/**
```

The reason is that the public RSVP API does not rely on the administrator HTTP session for authentication.

The main registration endpoint is intentionally anonymous, and the retained invitation-code endpoints are also public compatibility operations.

Administrative routes are not globally excluded from CSRF protection.

At present, the administration REST API is read-only and uses GET requests.

If future versions introduce administrative write operations, their CSRF behaviour must be reviewed before implementation.

---

# 11. General public registration model

Guests do not authenticate with usernames or passwords.

All guests can open the same public URL:

```text
https://<DOMAIN>/
```

The public form submits:

```text
POST /api/public/registrations
```

The guest supplies only the RSVP data. The guest does not provide an access code, database identifier, password, or other technical value.

The backend validates the request and creates:

```text
Invitation
    |
    v
RsvpResponse
```

Both entities are created in one transaction. If either persistence operation fails, the complete operation is rolled back so that an orphan `Invitation` is not retained.

Every valid submission is treated as an independent registration. Admitionum does not use only the guest name to block duplicates because different people can share the same name.

---

# 12. Internal and compatibility access codes

The main public registration generates a unique random access code internally for each new `Invitation`.

This preserves the existing entity model and database relationship, but the code is not selected, entered, or returned to the guest using the general form.

The earlier access-code endpoints remain available for compatibility:

```text
GET /api/public/invitations/{code}
PUT /api/public/invitations/{code}/response
```

For that compatibility flow, the code remains capability-like: anyone who possesses a valid code can potentially access and update its associated RSVP.

Generated and compatibility codes should therefore be:

```text
Random
Long enough to resist guessing
Unique
Difficult to predict
```

They must not use predictable counters or obvious patterns such as:

```text
family-name-1
guest-2
wedding-001
```

Real invitation codes must not be published in:

```text
README files
Screenshots
GitHub issues
Logs
CSV examples
Public demo data
```

---

# 13. Current compatibility-code limitation

The current application stores the invitation access code as a database value and searches for it directly.

The current implementation does not hash invitation codes before persistence.

The general registration does not reveal its generated code, which reduces its exposure in the main guest journey. However, the code remains stored in the database and the compatibility API still accepts known codes.

This is acceptable for the current focused project architecture, but it is an important limitation to document.

A stronger future design could store a cryptographic representation of the token rather than the original value.

That improvement is not part of Admitionum v1.1.

---

# 14. No public database access

The browser never connects directly to Azure SQL.

The only supported architecture is:

```text
Browser
   |
   | HTTPS / JSON
   v
Spring Boot
   |
   | JPA / JDBC
   v
Azure SQL
```

The following design is deliberately avoided:

```text
Browser
   |
   X
   |
Azure SQL
```

Database credentials therefore remain on the server side.

---

# 15. DTO boundary

JPA entities are not returned directly through the REST API.

The application uses DTOs.

Conceptually:

```text
Database entity
      |
      v
Service
      |
      v
DTO
      |
      v
REST response
```

This prevents persistence entities from automatically becoming the external API contract.

It also makes it possible to expose only the information required by each client.

For example, the public invitation API does not need to return the internal database identifier.

---

# 16. Backend validation is authoritative

The browser performs validation for usability, but JavaScript is never treated as a security boundary.

A user could bypass the browser and send an HTTP request manually.

For that reason:

```text
Frontend validation
        |
        v
User experience
```

while:

```text
Backend validation
        |
        v
Authoritative rules
```

The backend verifies rules such as:

```text
Guest name is valid
Contact is valid
Text lengths are valid
Confirmed attendance requires 1 to 20 attendees
Declined attendance requires zero attendees
Compatibility invitation exists
Compatibility invitation is active
Compatibility invitation has not expired
Compatibility attendee count does not exceed invitation.maxGuests
```

For a general registration, the backend also generates the internal code and creates the `Invitation` and `RsvpResponse` atomically.

---

# 17. Database constraints

Important rules are also protected by the SQL schema.

Examples include:

```text
Unique invitation access codes
One RSVP per invitation
Foreign key between RSVP and invitation
Attendee count range
Attendance / attendee count consistency
```

Security and data integrity therefore do not depend only on JavaScript.

Conceptually:

```text
Browser validation
        |
        v
Spring validation
        |
        v
Business rules
        |
        v
SQL constraints
```

---

# 18. User-provided HTML is not rendered

Guests can submit values such as:

```text
Guest name
Contact
Intolerances
Additional comment
```

The administration JavaScript displays these values using:

```text
textContent
```

rather than injecting them as HTML.

Conceptually:

```text
Guest value:
<b>Example</b>

Displayed:
<b>Example</b>
```

instead of rendering:

**Example**

This reduces the risk of stored HTML or JavaScript being interpreted by the administration dashboard.

---

# 19. Public frontend output handling

The public JavaScript also uses text-based DOM properties when displaying API information and messages.

For example, confirmation and error messages are assigned using:

```text
textContent
```

The frontend does not need to interpret guest-controlled content as HTML.

This keeps the presentation layer simpler and safer.

---

# 20. CSV formula injection protection

CSV exports can create an additional security risk.

Spreadsheet applications may interpret cells beginning with characters such as:

```text
=
+
-
@
```

as formulas.

Admitionum checks exported values before writing them to the CSV.

If a value begins with one of those characters after leading spaces are ignored, the application prefixes the value with:

```text
'
```

Conceptually:

```text
=SUM(A1:A2)
```

becomes:

```text
'=SUM(A1:A2)
```

before export.

This reduces the risk of formula injection when the CSV is opened in spreadsheet software.

---

# 21. CSV escaping

Admitionum also escapes double quotation marks when creating CSV fields.

Every value is surrounded by quotation marks.

A value containing:

```text
"
```

is written using the CSV escaped form:

```text
""
```

This protects the structure of the exported CSV from user-provided commas, quotation marks, and other textual content.

---

# 22. Local configuration secrets

The local profile requires:

```text
ADMIN_USERNAME
ADMIN_PASSWORD
```

These values should be supplied through the shell environment.

For production-profile testing, developers can optionally use a private:

```text
.env
```

file.

The repository `.gitignore` excludes:

```text
.env
```

The Docker build configuration also excludes it.

A real `.env` file must never be committed.

---

# 23. Production configuration

Production uses:

```text
src/main/resources/application-prod.properties
```

This file does not contain real credentials.

It references environment variables:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
ADMIN_USERNAME
ADMIN_PASSWORD
```

For example:

```text
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}
```

is safe to store in the repository because it contains only a variable reference.

The real value is provided at runtime.

---

# 24. Azure SQL connection security

The production JDBC connection uses encryption.

The connection includes:

```text
encrypt=true
trustServerCertificate=false
```

Conceptually:

```text
Spring Boot
      |
      | encrypted JDBC connection
      v
Azure SQL Database
```

`trustServerCertificate=false` means the client does not simply accept an untrusted server certificate.

---

# 25. Dedicated database user

Admitionum should not run permanently using the Azure SQL administrator account.

The deployment uses an application-specific database user.

Its responsibilities are limited to the operations required by Admitionum.

The documented permissions are:

```text
db_datareader
db_datawriter
VIEW DEFINITION
```

`VIEW DEFINITION` allows Hibernate to inspect the database schema while production uses:

```text
ddl-auto=validate
```

The application account does not need to be the database administrator.

---

# 26. Production schema protection

Production uses:

```text
spring.jpa.hibernate.ddl-auto=validate
```

Hibernate therefore:

```text
Does not create production tables
Does not drop production tables
Does not automatically alter the schema
```

It verifies that the schema matches the entity model.

Production schema creation is handled explicitly through:

```text
database/schema.sql
```

This reduces the risk of accidental schema modification during application startup.

---

# 27. H2 is not exposed in production

The local development environment provides the H2 console.

Production explicitly disables it:

```text
spring.h2.console.enabled=false
```

The production application therefore does not intentionally expose the development database console.

---

# 28. Docker image and secrets

The Docker image contains:

```text
Java runtime
Admitionum JAR
Application startup command
```

It does not need to contain:

```text
SQL password
Administrator password
.env
Azure credentials
```

Runtime configuration is supplied when the container starts.

Conceptually:

```text
Docker image
   +
Runtime secrets
   |
   v
Running container
```

This allows the same image to be used with different environments without rebuilding it with credentials.

---

# 29. Azure Container Apps secrets

Sensitive production values are stored in the Azure Container App as secrets.

Examples include:

```text
SQL password
Administrator username
Administrator password
```

Environment variables can reference those secrets.

Conceptually:

```text
Azure secret
     |
     v
secret reference
     |
     v
Environment variable
     |
     v
Spring Boot
```

Inspecting the environment configuration should show the secret reference rather than requiring the password value to be printed.

---

# 30. HTTPS

The public Azure Container Apps ingress provides HTTPS access to Admitionum.

Users access:

```text
https://...
```

rather than connecting directly to the internal Spring Boot port.

Inside the container, the application listens on:

```text
8080
```

Azure Container Apps provides the public HTTPS endpoint in front of the application.

---

# 31. GitHub repository secrets

The CI/CD workflow uses three GitHub repository secrets:

```text
AZURE_CLIENT_ID
AZURE_TENANT_ID
AZURE_SUBSCRIPTION_ID
```

These are identifiers used during the Azure OIDC login flow.

The workflow does not require:

```text
AZURE_CLIENT_SECRET
```

The SQL password and Admitionum administrator password are also not stored in GitHub Actions for the deployment process.

They remain configured in Azure Container Apps.

---

# 32. GitHub repository variables

Infrastructure names are stored as GitHub repository variables:

```text
AZURE_RESOURCE_GROUP
AZURE_CONTAINER_APP_NAME
AZURE_CONTAINER_REGISTRY
AZURE_CONTAINER_REGISTRY_LOGIN_SERVER
IMAGE_NAME
```

Variables must not be treated as secret storage.

They contain infrastructure identifiers rather than passwords.

Sensitive values must not be moved into these variables merely for convenience.

---

# 33. OIDC authentication

GitHub Actions authenticates against Azure using OpenID Connect.

Conceptually:

```text
GitHub Actions
      |
      | short-lived OIDC token
      v
Microsoft Entra ID
      |
      | federated identity validation
      v
Azure
```

There is no permanent Azure client password stored in the repository for this deployment identity.

This removes the need to manage and rotate a long-lived:

```text
AZURE_CLIENT_SECRET
```

inside GitHub.

---

# 34. OIDC trust is repository-specific

The federated credential is configured for the intended GitHub repository and branch.

The production deployment is associated with:

```text
main
```

A different repository does not automatically inherit that trust.

If Admitionum is copied into a separate private repository, that repository requires its own OIDC configuration.

This prevents a new unrelated repository from automatically gaining the deployment rights of the portfolio repository.

---

# 35. Least-privilege workflow permissions

The GitHub Actions test job uses:

```yaml
permissions:
  contents: read
```

It does not need an Azure identity token.

The deployment job uses:

```yaml
permissions:
  contents: read
  id-token: write
```

`id-token: write` exists only where Azure authentication is required.

The workflow does not grant unnecessary repository write permissions to the jobs.

---

# 36. Pinned GitHub Actions

The workflow references third-party and official GitHub Actions using complete commit SHAs.

Conceptually:

```text
uses: action@<FULL_COMMIT_SHA>
```

rather than relying only on a mutable version tag.

This reduces the risk of an external action tag later pointing to different code without a corresponding change in the Admitionum repository.

---

# 37. Git checkout credentials

The checkout step uses:

```text
persist-credentials: false
```

The GitHub token used to check out the repository is therefore not intentionally left configured in the local Git checkout used by later workflow steps.

---

# 38. Deployment only after tests

Production deployment depends on the successful test job.

The flow is:

```text
Source code
    |
    v
clean verify
    |
    | SUCCESS
    v
Docker build
    |
    v
Azure deployment
```

A failing test job prevents the deployment job from proceeding.

---

# 39. Pull requests do not deploy

For pull requests targeting `main`:

```text
Tests        -> run
Deployment   -> skipped
```

The Azure deployment only occurs from the authorized production branch flow.

This prevents unmerged pull-request code from being automatically deployed to the production Container App.

---

# 40. Immutable-style image identification

The automated workflow tags the production Docker image using:

```text
github.sha
```

rather than:

```text
latest
```

This creates a direct relationship between:

```text
Git commit
Docker image
Azure Container App revision
```

The workflow also verifies that the Container App is configured with the image it just built.

---

# 41. Post-deployment health verification

After updating Azure Container Apps, the workflow calls:

```text
/api/public/health
```

The expected response includes:

```json
{
  "status": "ok",
  "application": "Admitionum"
}
```

The workflow therefore performs more than an image upload.

It verifies that the deployed application can answer HTTP requests.

---

# 42. Deployment identity permissions

The GitHub deployment identity requires only the Azure permissions needed by the workflow.

The current deployment model uses:

```text
AcrPush
```

for publishing the image to Azure Container Registry.

It also uses permission to update the Azure Container App.

The deployment identity is separate from:

```text
Azure SQL user
Admitionum administrator
Wedding guests
```

These identities have different responsibilities.

---

# 43. Personal data

Admitionum can store personal information such as:

```text
Names
Contact information
Attendance decisions
Food intolerance information
Comments
```

For that reason, public demonstration environments must use fictitious information.

The public GitHub repository must never contain real wedding data.

This includes:

```text
Guest lists
Database backups
Real RSVP CSV files
Real access codes
Phone numbers
Email addresses
Food intolerance details
```

---

# 44. Logs

Application logs should not be used to print complete RSVP request bodies or invitation codes.

In particular, logs should avoid exposing:

```text
Contact information
Invitation access codes
Food intolerance information
Comments
Passwords
Connection strings containing credentials
```

Operational logs should contain only the information required to diagnose the application.

---

# 45. Public repository history

Removing a sensitive file from the latest Git commit does not remove it from previous commits.

Therefore:

```text
Never commit a secret first
```

is preferable to trying to remove it afterwards.

If a real credential is ever committed:

```text
1. Treat it as exposed.
2. Rotate or revoke it.
3. Then decide whether Git history also needs rewriting.
```

Deleting the current file alone is not a credential-rotation strategy.

---

# 46. Security of `.env.example`

The public repository can include:

```text
.env.example
```

because it contains only placeholders.

Example:

```text
SPRING_DATASOURCE_PASSWORD=<YOUR_SQL_PASSWORD>
```

The repository must not contain:

```text
SPRING_DATASOURCE_PASSWORD=real-password
```

`.env.example` explains the required configuration.

`.env` contains the private configuration.

Only the example file belongs in Git.

---

# 47. Current scaling limitation

The current administrator session is stored in application memory.

For that reason, the production Container App is deliberately configured with:

```text
max replicas = 1
```

With multiple replicas:

```text
Request 1 -> replica A
Request 2 -> replica B
```

the second replica would not automatically share the authenticated session stored in replica A.

Supporting several replicas correctly would require a different session architecture.

That is outside the current project scope.

---

# 48. Current administrator limitation

Admitionum currently has:

```text
One in-memory administrator
One ADMIN role
```

It does not provide:

```text
Administrator registration
Password recovery
Several administrator accounts
Role management
Microsoft Entra authentication
Multi-factor authentication
```

This was a deliberate simplification for the project scope.

A larger production system would require a stronger identity lifecycle.

---

# 49. Current anonymous-registration limitation

The main public registration endpoint is intentionally anonymous so every guest can use the same QR code or URL.

Admitionum currently does not provide:

```text
Guest passwords
Guest accounts
One-time codes
Secondary verification
Token hashing
Rate limiting
Complex duplicate detection
Lockout after failed compatibility-code attempts
```

Each valid general-form submission can therefore create a separate `Invitation` and `RsvpResponse`.

The application does not treat a matching name as proof that two submissions belong to the same person.

For the retained compatibility flow, the protection continues to depend on the unpredictability and confidentiality of each invitation code.

Real codes should consequently be generated randomly and should never be easily guessable.

---

# 50. No rate limiting

The current application does not implement an application-level rate limiter.

This means there is currently no explicit Admitionum rule such as:

```text
Maximum 5 invitation-code attempts per minute
```

or:

```text
Maximum 10 RSVP submissions per minute
```

For the current small event application this functionality was not included.

A public Internet deployment with a significantly larger threat model should consider rate limiting or an upstream protective service.

---

# 51. No CAPTCHA

The public RSVP form does not use a CAPTCHA.

The application therefore relies on:

```text
Backend validation
Transactional persistence
Restricted public response data
Small application scope
```

These controls do not provide bot verification, but they preserve validation and data boundaries within the current project scope.

Invitation-code secrecy additionally applies to the retained compatibility endpoints.

CAPTCHA was deliberately excluded from the MVP.

---

# 52. No dedicated Web Application Firewall

The current architecture does not include:

```text
Azure Front Door WAF
Application Gateway WAF
Third-party WAF
```

Traffic reaches the public Azure Container Apps ingress directly.

This keeps the architecture simple but means those additional edge-security capabilities are not part of the current project.

---

# 53. Azure SQL networking limitation

The current MVP Azure architecture uses public Azure SQL networking rather than:

```text
Private Endpoint
Private Link
Custom VNet integration
```

Authentication, firewall configuration, encrypted JDBC traffic, and database permissions still apply.

However, a stricter production environment could replace this network design with private connectivity.

That improvement is outside Admitionum v1.1.

---

# 54. Azure Container Registry limitation

The original manual deployment used registry credentials so that Azure Container Apps could pull the private image.

GitHub Actions itself publishes images through Azure authentication and `AcrPush`.

A stronger future infrastructure design could use a managed identity for the Container App to retrieve images from ACR and then disable legacy registry administrator credentials where they are no longer required.

This migration has not been made part of the current project scope.

Registry access must not be disabled blindly without first confirming how the running Container App retrieves its image.

---

# 55. No Azure Key Vault

Admitionum currently stores runtime secrets using Azure Container Apps secrets.

It does not use:

```text
Azure Key Vault
```

This was an intentional scope decision.

For this project:

```text
Container Apps secrets
```

provide the runtime configuration mechanism.

A larger environment with centralized secret governance could introduce Key Vault separately.

---

# 56. No infrastructure as code

Azure resources were created and configured through Azure CLI and the Azure platform rather than through:

```text
Bicep
Terraform
Pulumi
```

This means infrastructure changes are not currently versioned as declarative infrastructure code.

The GitHub workflow automates application deployment, not complete Azure infrastructure creation.

---

# 57. No automated secret rotation

The project does not currently automate rotation of:

```text
SQL passwords
Admitionum administrator password
Registry credentials
```

Those values must be changed manually when required.

The GitHub Azure deployment identity avoids this problem for CI/CD because it uses OIDC instead of a long-lived client secret.

---

# 58. Security responsibilities by layer

The security model can be summarized as:

```text
Browser
  |
  | Safe DOM rendering
  | Basic validation
  v
Spring Security
  |
  | Public/admin authorization
  | Session authentication
  v
Controllers / DTO
  |
  | API data boundary
  v
Services
  |
  | Business validation
  v
Repositories / JPA
  |
  v
Azure SQL
  |
  | Constraints
  | Limited application user
```

Infrastructure adds:

```text
GitHub Actions
      |
      | OIDC
      v
Azure

Docker
      |
      | no embedded passwords
      v
Container Apps
      |
      | runtime secrets
      v
Spring Boot
```

---

# 59. Security checklist for a public portfolio

Before publishing a version of Admitionum, verify:

```text
[ ] No .env file is committed.
[ ] No real SQL password exists in Git.
[ ] No administrator password exists in Git.
[ ] No ACR password exists in Git.
[ ] No Azure access token exists in Git.
[ ] No AZURE_CLIENT_SECRET exists.
[ ] No real invitation code is published.
[ ] No real guest name is published.
[ ] No real phone number is published.
[ ] No real email address is published.
[ ] No real food intolerance information is published.
[ ] No real RSVP CSV file is published.
[ ] No database backup is published.
[ ] Demo data is fictitious.
[ ] application-prod.properties contains only variable references.
[ ] .env remains ignored.
[ ] GitHub OIDC federation targets the intended repository.
[ ] Pull requests cannot deploy to production.
```

---

# 60. Security summary

Admitionum applies security appropriate to its current focused scope:

```text
Spring Security
Session-based administrator authentication
ROLE_ADMIN authorization
Default-deny route policy
Public/admin API separation
Anonymous general registration with restricted responses
Random internal invitation access codes
DTO-based API boundaries
Backend validation
SQL constraints
Safe text rendering
CSV formula neutralization
Environment-based secrets
Encrypted Azure SQL connection
Dedicated SQL application user
Docker images without embedded credentials
Azure Container Apps secrets
GitHub Actions OIDC
CI before deployment
SHA-based container image tags
Post-deployment health verification
```

It deliberately does not claim to provide:

```text
Enterprise identity management
Multi-factor authentication
Distributed sessions
Rate limiting
CAPTCHA
Private Azure networking
Web Application Firewall
Key Vault integration
Automatic secret rotation
Infrastructure as code
```

These limitations are documented rather than hidden.

The objective of the project is to provide a clear, understandable, and reasonably secure architecture for a small RSVP application while demonstrating the complete path from application development to automated cloud deployment.