# Admitionum Installation and Deployment Guide

## 1. Purpose

This guide explains how to reproduce Admitionum from a fresh repository clone.

It covers:

- Local development with H2.
- Automated tests.
- Building the Spring Boot JAR.
- Running the application with Docker.
- Preparing Azure SQL Database.
- Creating an Azure Container Registry.
- Deploying to Azure Container Apps.
- Configuring runtime secrets.
- Configuring GitHub Actions.
- Configuring Azure authentication through OIDC.

The examples deliberately use placeholders instead of the infrastructure names or credentials of the original deployment.

Replace values such as:

```text
<YOUR_RESOURCE_GROUP>
<YOUR_LOCATION>
<YOUR_SQL_SERVER>
<YOUR_DATABASE>
<YOUR_ACR_NAME>
<YOUR_CONTAINER_APP>
<YOUR_CONTAINER_APP_ENVIRONMENT>
<YOUR_SQL_USERNAME>
<YOUR_ADMIN_USERNAME>
```

with values belonging to your own environment.

Never copy production passwords into this document or into the Git repository.

---

# 2. Architecture being reproduced

The final deployment architecture is:

```text
GitHub
   |
   | GitHub Actions
   v
Azure authentication through OIDC
   |
   +----------------------------+
   |                            |
   v                            v
Azure Container Registry   Azure Container Apps
        |                        |
        | Docker image           | Spring Boot
        +-----------------------> |
                                 |
                                 | JDBC / TLS
                                 v
                          Azure SQL Database
```

For local development:

```text
Browser
   |
   v
Spring Boot
   |
   v
H2 persistent database
```

For automated tests:

```text
JUnit / Spring tests
   |
   v
H2 in-memory database
```

---

# 3. Required software

For local development you need:

```text
Git
Java 25
Docker Desktop
```

For Azure deployment you additionally need:

```text
Azure account
Azure CLI
GitHub account
```

Admitionum includes the Maven Wrapper.

Installing Maven globally is therefore not required.

---

# 4. Clone the repository

Clone the project:

```bash
git clone https://github.com/Franer-Narf/Admitionum.git
```

Enter the project:

```bash
cd Admitionum
```

Check the repository:

```bash
git status
```

Expected result:

```text
On branch main
nothing to commit, working tree clean
```

---

# 5. Verify Java

Check Java:

```bash
java -version
```

Check the compiler:

```bash
javac -version
```

The project requires:

```text
Java 25
```

Also verify the Java version used by Maven.

## Windows PowerShell

```powershell
.\mvnw.cmd -version
```

## Git Bash / Linux / macOS

```bash
./mvnw -version
```

The output should indicate:

```text
Java version: 25
```

If Maven uses a different JDK, review the local `JAVA_HOME` configuration.

---

# 6. Understand the Spring profiles

Admitionum uses three environments.

```text
local
test
prod
```

They have different purposes.

## Local profile

Used during normal development.

Database:

```text
H2 persistent
```

Configuration:

```text
src/main/resources/application-local.properties
```

Database URL:

```text
jdbc:h2:file:./data/admitionum
```

## Test profile

Used by automated tests.

Database:

```text
H2 in-memory
```

Configuration:

```text
src/test/resources/application-test.properties
```

Database URL:

```text
jdbc:h2:mem:admitionum-test
```

## Production profile

Used when running against Azure SQL.

Configuration:

```text
src/main/resources/application-prod.properties
```

Required environment variables:

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
ADMIN_USERNAME
ADMIN_PASSWORD
```

The production profile uses:

```text
spring.jpa.hibernate.ddl-auto=validate
```

Hibernate therefore validates the production schema but does not create or modify it automatically.

---

# 7. Configure local administrator credentials

The local profile also requires administrator credentials.

Do not write these values into:

```text
application-local.properties
README.md
Git
```

Set them only in the terminal used to run the application.

## Windows PowerShell

```powershell
$env:ADMIN_USERNAME="admin"
$env:ADMIN_PASSWORD="choose-a-local-password"
```

## Git Bash

```bash
export ADMIN_USERNAME="admin"
export ADMIN_PASSWORD="choose-a-local-password"
```

These credentials protect:

```text
/admin/**
/api/admin/**
```

They are development credentials only.

Do not reuse a production password.

---

# 8. Run the automated tests

Before running the application, verify the project.

## Windows PowerShell

```powershell
.\mvnw.cmd clean verify
```

## Git Bash / Linux / macOS

```bash
./mvnw clean verify
```

The build must finish with:

```text
BUILD SUCCESS
```

Tests use the `test` profile and the H2 in-memory database.

They do not need Azure SQL.

---

# 9. Run Admitionum locally

After defining:

```text
ADMIN_USERNAME
ADMIN_PASSWORD
```

start Spring Boot.

## Windows PowerShell

```powershell
.\mvnw.cmd spring-boot:run
```

## Git Bash / Linux / macOS

```bash
./mvnw spring-boot:run
```

The default Spring profile is:

```text
local
```

The application should start on:

```text
http://localhost:8080
```

---

# 10. Verify the health endpoint

Open:

```text
http://localhost:8080/api/public/health
```

Expected response:

```json
{
  "status": "ok",
  "application": "Admitionum"
}
```

From PowerShell:

```powershell
Invoke-RestMethod `
    -Method Get `
    -Uri "http://localhost:8080/api/public/health"
```

From Git Bash:

```bash
curl http://localhost:8080/api/public/health
```

---

# 11. Access the local administration area

Open:

```text
http://localhost:8080/admin/
```

Spring Security should request authentication.

Use the values configured in:

```text
ADMIN_USERNAME
ADMIN_PASSWORD
```

The administration area should not be accessible anonymously.

---

# 12. H2 local database

The local database is stored under:

```text
data/
```

The generated H2 file is excluded from Git.

The H2 console is available locally at:

```text
http://localhost:8080/h2-console
```

Use:

| Setting | Value |
|---|---|
| Driver Class | `org.h2.Driver` |
| JDBC URL | `jdbc:h2:file:./data/admitionum` |
| User Name | `sa` |
| Password | empty |

The H2 console is enabled only for local development.

It is disabled in production.

---

# 13. Stop the local application

Use:

```text
Ctrl + C
```

This stops the embedded Tomcat server and closes the local database connection.

---

# 14. Build the executable JAR

Create the production JAR.

## Windows PowerShell

```powershell
.\mvnw.cmd clean package
```

## Git Bash / Linux / macOS

```bash
./mvnw clean package
```

The generated application is:

```text
target/admitionum-0.0.1-SNAPSHOT.jar
```

Verify it exists.

## PowerShell

```powershell
Get-ChildItem target\admitionum-0.0.1-SNAPSHOT.jar
```

## Git Bash

```bash
ls -l target/admitionum-0.0.1-SNAPSHOT.jar
```

---

# 15. Verify Docker

Start Docker Desktop.

Then run:

```bash
docker --version
```

And:

```bash
docker info
```

A useful independent test is:

```bash
docker run --rm hello-world
```

`docker --version` only proves that the Docker CLI exists.

`docker info` proves that the Docker engine can also be contacted.

---

# 16. Build the Admitionum Docker image

The repository contains:

```text
Dockerfile
```

The image uses Java 25 and copies the packaged JAR.

Build it:

```bash
docker build -t admitionum:local .
```

Verify:

```bash
docker images admitionum
```

You should see:

```text
REPOSITORY    TAG
admitionum    local
```

---

# 17. Run the container with the local profile

The default profile remains `local`.

Run:

## PowerShell

```powershell
docker run `
    --name admitionum-local `
    --rm `
    -p 8080:8080 `
    -e ADMIN_USERNAME="admin" `
    -e ADMIN_PASSWORD="choose-a-local-password" `
    admitionum:local
```

## Git Bash / Linux / macOS

```bash
docker run \
    --name admitionum-local \
    --rm \
    -p 8080:8080 \
    -e ADMIN_USERNAME="admin" \
    -e ADMIN_PASSWORD="choose-a-local-password" \
    admitionum:local
```

Open:

```text
http://localhost:8080/api/public/health
```

And:

```text
http://localhost:8080/admin/
```

Stop the container with:

```text
Ctrl + C
```

Because `--rm` is used, Docker removes the stopped container automatically.

The image remains available.

---

# 18. Production configuration variables

Production requires:

```text
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
ADMIN_USERNAME
ADMIN_PASSWORD
```

Conceptually:

```text
Spring Boot
    |
    +--> SPRING_DATASOURCE_URL
    +--> SPRING_DATASOURCE_USERNAME
    +--> SPRING_DATASOURCE_PASSWORD
    |
    +--> ADMIN_USERNAME
    `--> ADMIN_PASSWORD
```

The SQL credentials and the administration credentials are different concepts.

```text
SPRING_DATASOURCE_PASSWORD
```

authenticates against Azure SQL.

```text
ADMIN_PASSWORD
```

authenticates the administrator inside Admitionum.

Never reuse or confuse these values.

---

# 19. Optional local `.env` file for Docker

For local production-profile testing, create a file named:

```text
.env
```

The repository already excludes `.env` from Git.

Example:

```env
SPRING_PROFILES_ACTIVE=prod

SPRING_DATASOURCE_URL=jdbc:sqlserver://<YOUR_SQL_SERVER>.database.windows.net:1433;databaseName=<YOUR_DATABASE>;encrypt=true;trustServerCertificate=false;loginTimeout=30;

SPRING_DATASOURCE_USERNAME=<YOUR_SQL_USERNAME>
SPRING_DATASOURCE_PASSWORD=<YOUR_SQL_PASSWORD>

ADMIN_USERNAME=<YOUR_ADMIN_USERNAME>
ADMIN_PASSWORD=<YOUR_ADMIN_PASSWORD>
```

This file is private.

Never run:

```bash
git add .env
```

The public repository will contain `.env.example` instead.

---

# 20. Azure preparation

Log in to Azure CLI:

```bash
az login
```

Check the selected subscription:

```bash
az account show --output table
```

If several subscriptions exist, verify that the correct one is selected before creating resources.

Install or update the Container Apps extension:

```bash
az extension add \
    --name containerapp \
    --upgrade \
    --yes
```

On PowerShell the same command can be written:

```powershell
az extension add `
    --name containerapp `
    --upgrade `
    --yes
```

---

# 21. Define generic Azure names

The following examples use PowerShell variables.

Choose names that belong to your own subscription.

```powershell
$ResourceGroup = "<YOUR_RESOURCE_GROUP>"
$Location = "<YOUR_LOCATION>"

$SqlServer = "<YOUR_SQL_SERVER>"
$Database = "<YOUR_DATABASE>"

$Registry = "<YOUR_ACR_NAME>"
$ContainerEnvironment = "<YOUR_CONTAINER_APP_ENVIRONMENT>"
$ContainerApp = "<YOUR_CONTAINER_APP>"

$ImageName = "admitionum"
$ImageTag = "initial"
```

Important:

Azure Container Registry names must be globally available.

Do not copy the registry name used by the original Admitionum deployment.

---

# 22. Resource group

If you need a new resource group:

```powershell
az group create `
    --name $ResourceGroup `
    --location $Location
```

Verify:

```powershell
az group show `
    --name $ResourceGroup `
    --output table
```

If you already have a suitable resource group, reuse it instead of creating another one unnecessarily.

---

# 23. Azure SQL prerequisites

Admitionum requires:

```text
Azure SQL logical server
Azure SQL Database
SQL administrator for initial setup
Application database user
```

The exact pricing and compute configuration are infrastructure choices and can change over time.

Choose an Azure SQL configuration appropriate for your own environment and expected usage.

The application only requires a SQL Database compatible with the schema in:

```text
database/schema.sql
```

---

# 24. Allow your development computer to reach Azure SQL

For local testing against Azure SQL, the SQL server firewall must permit your current public IP.

This can be configured from:

```text
Azure Portal
    |
    v
SQL server
    |
    v
Networking
    |
    v
Add current client IPv4 address
```

Firewall access does not replace authentication.

The connection still requires:

```text
SQL username
SQL password
Database permissions
```

---

# 25. Create the production schema

Open the Azure SQL Query Editor or another SQL client and connect as the SQL server administrator.

Before executing anything, verify the active database:

```sql
SELECT
    DB_NAME() AS CurrentDatabase;
```

It must return your Admitionum database.

Then execute:

```text
database/schema.sql
```

Do not rely on Hibernate to create production tables.

The production configuration uses:

```text
ddl-auto=validate
```

---

# 26. Verify the production tables

Run:

```sql
SELECT
    TABLE_SCHEMA,
    TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_NAME IN (
    'Invitations',
    'RsvpResponses'
)
ORDER BY TABLE_NAME;
```

Expected tables:

```text
dbo.Invitations
dbo.RsvpResponses
```

---

# 27. Create the application SQL user

Do not run Admitionum permanently with the SQL server administrator account.

Create a dedicated user inside the Admitionum database.

Example:

```sql
CREATE USER [admitionum_app]
WITH PASSWORD = '<STRONG_APPLICATION_PASSWORD>';
```

Grant read permissions:

```sql
ALTER ROLE db_datareader
ADD MEMBER [admitionum_app];
```

Grant write permissions:

```sql
ALTER ROLE db_datawriter
ADD MEMBER [admitionum_app];
```

Allow Hibernate to inspect the schema:

```sql
GRANT VIEW DEFINITION
TO [admitionum_app];
```

This user is a database user.

It is not the administrator of the Azure SQL server.

---

# 28. Build the JDBC connection

The production JDBC URL follows this structure:

```text
jdbc:sqlserver://<YOUR_SQL_SERVER>.database.windows.net:1433;databaseName=<YOUR_DATABASE>;encrypt=true;trustServerCertificate=false;loginTimeout=30;
```

For example:

```text
SPRING_DATASOURCE_URL=
jdbc:sqlserver://<YOUR_SQL_SERVER>.database.windows.net:1433;
databaseName=<YOUR_DATABASE>;
encrypt=true;
trustServerCertificate=false;
loginTimeout=30;
```

It is a JDBC connection string.

It is not a website URL and should not be opened in a browser.

---

# 29. Test Azure SQL from Spring Boot locally

Set:

## PowerShell

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"

$env:SPRING_DATASOURCE_URL="jdbc:sqlserver://<YOUR_SQL_SERVER>.database.windows.net:1433;databaseName=<YOUR_DATABASE>;encrypt=true;trustServerCertificate=false;loginTimeout=30;"

$env:SPRING_DATASOURCE_USERNAME="admitionum_app"

$env:SPRING_DATASOURCE_PASSWORD="<YOUR_SQL_PASSWORD>"

$env:ADMIN_USERNAME="<YOUR_ADMIN_USERNAME>"

$env:ADMIN_PASSWORD="<YOUR_ADMIN_PASSWORD>"
```

Then:

```powershell
.\mvnw.cmd spring-boot:run
```

Git Bash equivalent:

```bash
export SPRING_PROFILES_ACTIVE="prod"

export SPRING_DATASOURCE_URL="jdbc:sqlserver://<YOUR_SQL_SERVER>.database.windows.net:1433;databaseName=<YOUR_DATABASE>;encrypt=true;trustServerCertificate=false;loginTimeout=30;"

export SPRING_DATASOURCE_USERNAME="admitionum_app"

export SPRING_DATASOURCE_PASSWORD="<YOUR_SQL_PASSWORD>"

export ADMIN_USERNAME="<YOUR_ADMIN_USERNAME>"

export ADMIN_PASSWORD="<YOUR_ADMIN_PASSWORD>"

./mvnw spring-boot:run
```

A correct startup should reach:

```text
Started AdmitionumApplication
```

Hibernate should validate the existing SQL schema.

---

# 30. Test Docker against Azure SQL

Instead of passing each value manually, the private `.env` file can be used:

```bash
docker run \
    --name admitionum-prod-test \
    --rm \
    -p 8080:8080 \
    --env-file .env \
    admitionum:local
```

PowerShell:

```powershell
docker run `
    --name admitionum-prod-test `
    --rm `
    -p 8080:8080 `
    --env-file .env `
    admitionum:local
```

Verify:

```text
http://localhost:8080/api/public/health
```

and:

```text
http://localhost:8080/admin/
```

This confirms that the same Docker image can run against Azure SQL when the production configuration is supplied at runtime.

---

# 31. Create Azure Container Registry

Create a registry:

```powershell
az acr create `
    --resource-group $ResourceGroup `
    --name $Registry `
    --location $Location `
    --sku Basic
```

Log in:

```powershell
az acr login `
    --name $Registry
```

Obtain the login server:

```powershell
$RegistryLoginServer = az acr show `
    --name $Registry `
    --query loginServer `
    --output tsv
```

The result will resemble:

```text
<YOUR_ACR_NAME>.azurecr.io
```

This is a container registry address, not an application website.

---

# 32. Push the image to ACR

Tag the existing image:

```powershell
docker tag `
    admitionum:local `
    "$RegistryLoginServer/${ImageName}:${ImageTag}"
```

Push:

```powershell
docker push `
    "$RegistryLoginServer/${ImageName}:${ImageTag}"
```

Verify:

```powershell
az acr repository show-tags `
    --name $Registry `
    --repository $ImageName `
    --output table
```

You should see:

```text
initial
```

or the tag that you selected.

---

# 33. Create the Container Apps environment

Create:

```powershell
az containerapp env create `
    --name $ContainerEnvironment `
    --resource-group $ResourceGroup `
    --location $Location
```

The environment is the managed Azure environment in which the Container App runs.

It is not the application itself.

---

# 34. Allow Azure-hosted Admitionum to reach Azure SQL

Admitionum running in Container Apps must be able to reach the Azure SQL server.

The original MVP architecture uses Azure SQL public networking rather than a custom VNet or Private Endpoint.

A simple Azure SQL configuration can therefore allow Azure-hosted services to reach the SQL server while still requiring SQL authentication and database permissions.

For production systems with stricter network requirements, a private-network design should be evaluated separately.

Admitionum's current portfolio architecture does not implement that additional network layer.

---

# 35. Prepare application secrets

The Container App needs:

```text
SQL password
Administrator username
Administrator password
```

Do not place them directly in the repository.

For PowerShell, request passwords interactively when possible.

Example:

```powershell
$SqlPasswordSecure = Read-Host `
    "SQL application password" `
    -AsSecureString

$SqlPassword = [System.Net.NetworkCredential]::new(
    "",
    $SqlPasswordSecure
).Password
```

Administrator password:

```powershell
$AdminPasswordSecure = Read-Host `
    "Admitionum administrator password" `
    -AsSecureString

$AdminPassword = [System.Net.NetworkCredential]::new(
    "",
    $AdminPasswordSecure
).Password
```

Administrator username:

```powershell
$AdminUsername = "<YOUR_ADMIN_USERNAME>"
```

JDBC URL:

```powershell
$JdbcUrl = "jdbc:sqlserver://$SqlServer.database.windows.net:1433;databaseName=$Database;encrypt=true;trustServerCertificate=false;loginTimeout=30;"
```

---

# 36. Registry credentials for the initial manual deployment

For a simple first manual deployment, the registry administrator account can be enabled temporarily:

```powershell
az acr update `
    --name $Registry `
    --admin-enabled true
```

Get the registry username:

```powershell
$RegistryUsername = az acr credential show `
    --name $Registry `
    --query username `
    --output tsv
```

Get a registry password into the current terminal variable:

```powershell
$RegistryPassword = az acr credential show `
    --name $Registry `
    --query "passwords[0].value" `
    --output tsv
```

Do not print or commit this value.

This registry credential is used only so that the Container App can pull the initial private image in this simple deployment model.

---

# 37. Create the Azure Container App

Create the application:

```powershell
az containerapp create `
    --name $ContainerApp `
    --resource-group $ResourceGroup `
    --environment $ContainerEnvironment `
    --image "$RegistryLoginServer/${ImageName}:${ImageTag}" `
    --registry-server $RegistryLoginServer `
    --registry-username $RegistryUsername `
    --registry-password $RegistryPassword `
    --ingress external `
    --target-port 8080 `
    --transport auto `
    --cpu 0.5 `
    --memory 1.0Gi `
    --min-replicas 0 `
    --max-replicas 1 `
    --secrets `
        "sql-password=$SqlPassword" `
        "admin-username=$AdminUsername" `
        "admin-password=$AdminPassword" `
    --env-vars `
        "SPRING_PROFILES_ACTIVE=prod" `
        "SPRING_DATASOURCE_URL=$JdbcUrl" `
        "SPRING_DATASOURCE_USERNAME=admitionum_app" `
        "SPRING_DATASOURCE_PASSWORD=secretref:sql-password" `
        "ADMIN_USERNAME=secretref:admin-username" `
        "ADMIN_PASSWORD=secretref:admin-password"
```

The application deliberately uses:

```text
minimum replicas: 0
maximum replicas: 1
```

The maximum is one because the current Spring Security administrator session is stored in application memory.

Running several replicas would require a different session strategy.

---

# 38. Obtain the public URL

Get the Container App hostname:

```powershell
$Fqdn = az containerapp show `
    --name $ContainerApp `
    --resource-group $ResourceGroup `
    --query properties.configuration.ingress.fqdn `
    --output tsv
```

Build the URL:

```powershell
$BaseUrl = "https://$Fqdn"
```

Print only the public URL:

```powershell
$BaseUrl
```

---

# 39. Verify the Azure deployment

Health endpoint:

```powershell
Invoke-RestMethod `
    -Method Get `
    -Uri "$BaseUrl/api/public/health"
```

Expected:

```text
status       ok
application  Admitionum
```

Open the public application:

```powershell
Start-Process $BaseUrl
```

Open administration:

```powershell
Start-Process "$BaseUrl/admin/"
```

The administration page must require authentication.

---

# 40. Verify the Container App revision

List revisions:

```powershell
az containerapp revision list `
    --name $ContainerApp `
    --resource-group $ResourceGroup `
    --query "[].{Revision:name,Active:properties.active,Health:properties.healthState,State:properties.runningState}" `
    --output table
```

Healthy states can include:

```text
Running
ScaledToZero
```

when the application is configured with:

```text
min replicas = 0
```

A scaled-to-zero application is not necessarily failing.

The next request can cause Azure to start a new replica.

---

# 41. Troubleshoot startup failures

If the public application does not respond, do not immediately rebuild everything.

First inspect the revision and application logs.

Example:

```powershell
az containerapp logs show `
    --name $ContainerApp `
    --resource-group $ResourceGroup `
    --type console `
    --tail 300 `
    --format text
```

Look for the first real exception.

Common categories include:

```text
Image pull problem
Database networking problem
Azure SQL temporarily unavailable
SQL authentication failure
Application configuration error
```

A later Hibernate error may be a consequence of not obtaining a JDBC connection rather than the original cause.

---

# 42. Azure SQL Serverless startup consideration

If the selected Azure SQL configuration supports automatic pause, the database may need time to resume after a period without activity.

A first connection can therefore fail or take longer while the database resumes.

Do not automatically rebuild the Docker image when the error is caused by database availability.

Wait for the database to become available and retry the connection.

---

# 43. Remove sensitive PowerShell variables

After secrets have been stored successfully in Azure, clear local sensitive variables.

Example:

```powershell
$SqlPassword = $null
$SqlPasswordSecure = $null

$AdminPassword = $null
$AdminPasswordSecure = $null

$RegistryPassword = $null
```

Optional:

```powershell
Remove-Variable SqlPassword -ErrorAction SilentlyContinue
Remove-Variable SqlPasswordSecure -ErrorAction SilentlyContinue
Remove-Variable AdminPassword -ErrorAction SilentlyContinue
Remove-Variable AdminPasswordSecure -ErrorAction SilentlyContinue
Remove-Variable RegistryPassword -ErrorAction SilentlyContinue
```

Removing a PowerShell variable does not delete the secret stored in Azure.

---

# 44. Verify secret references without printing secret values

Do not print secret values merely to check whether they exist.

List secret names:

```powershell
az containerapp secret list `
    --name $ContainerApp `
    --resource-group $ResourceGroup `
    --query "[].name" `
    --output table
```

Inspect environment configuration:

```powershell
az containerapp show `
    --name $ContainerApp `
    --resource-group $ResourceGroup `
    --query "properties.template.containers[0].env" `
    --output table
```

A secret-backed environment variable can show a secret reference rather than the secret itself.

That is the expected configuration.

---

# 45. GitHub Actions deployment

The repository already contains:

```text
.github/workflows/deploy.yml
```

The workflow expects an existing:

```text
Azure Container Registry
Azure Container App
Azure deployment identity
OIDC federated credential
```

It does not create the Azure infrastructure.

Its responsibility is deployment automation.

---

# 46. CI/CD behaviour

For a pull request targeting:

```text
main
```

the workflow performs:

```text
Checkout
    |
    v
Set up Java 25
    |
    v
Maven clean verify
    |
    v
Package tested JAR
```

The deployment job is intentionally skipped.

For a push to:

```text
main
```

the workflow performs:

```text
Tests
   |
   v
Tested JAR
   |
   v
Docker build
   |
   v
Azure login through OIDC
   |
   v
Push image to ACR
   |
   v
Update Container App
   |
   v
Verify image
   |
   v
Verify health endpoint
```

---

# 47. Create an Azure deployment identity

Create a dedicated Azure identity for GitHub Actions.

This identity should be used for deployment only.

It is separate from:

```text
Admitionum ADMIN_USERNAME
Azure SQL users
Guest invitation access
```

Configure a GitHub OIDC federated credential for the repository and the `main` branch.

The trust configuration must correspond to the repository that actually runs the workflow.

Do not reuse the OIDC credential of another repository.

If a new independent repository is created, configure a new federation for that repository.

---

# 48. OIDC permissions

The GitHub deployment identity requires only the permissions needed by the workflow.

Admitionum requires:

```text
AcrPush
```

on the Azure Container Registry, so that GitHub Actions can push images.

It also requires permission to update the Azure Container App.

The original deployment uses:

```text
Container Apps Contributor
```

with an appropriate Azure scope.

Avoid granting broad subscription-level permissions when a narrower resource scope is sufficient.

---

# 49. OIDC does not require a client secret

The deployment workflow authenticates using:

```text
GitHub Actions
      |
      | OIDC
      v
Azure
```

It does not require:

```text
AZURE_CLIENT_SECRET
```

or a JSON `AZURE_CREDENTIALS` secret.

The workflow receives a short-lived identity token during execution.

The repository therefore stores Azure identifiers, not an Azure deployment password.

---

# 50. GitHub repository secrets

Open:

```text
GitHub repository
    |
    v
Settings
    |
    v
Secrets and variables
    |
    v
Actions
```

Create these repository secrets:

```text
AZURE_CLIENT_ID
AZURE_TENANT_ID
AZURE_SUBSCRIPTION_ID
```

Enter the actual identifier values.

Do not enter strings such as:

```text
$ClientId
$TenantId
$SubscriptionId
```

Those would only be variable names from a local shell, not their values.

---

# 51. GitHub repository variables

Create:

```text
AZURE_RESOURCE_GROUP
AZURE_CONTAINER_APP_NAME
AZURE_CONTAINER_REGISTRY
AZURE_CONTAINER_REGISTRY_LOGIN_SERVER
IMAGE_NAME
```

Example conceptual values:

```text
AZURE_RESOURCE_GROUP=<YOUR_RESOURCE_GROUP>

AZURE_CONTAINER_APP_NAME=<YOUR_CONTAINER_APP>

AZURE_CONTAINER_REGISTRY=<YOUR_ACR_NAME>

AZURE_CONTAINER_REGISTRY_LOGIN_SERVER=<YOUR_ACR_NAME>.azurecr.io

IMAGE_NAME=admitionum
```

These are infrastructure identifiers.

Do not place SQL passwords or administrator passwords in repository variables.

Application runtime passwords belong in Azure Container Apps secrets.

---

# 52. GitHub Actions permissions

The deployment job requires:

```yaml
permissions:
  contents: read
  id-token: write
```

`id-token: write` allows the workflow to request an OIDC token.

It does not expose an Azure password.

The test job does not need deployment permissions and uses:

```yaml
permissions:
  contents: read
```

---

# 53. Production image tags

The automated workflow tags images using:

```text
github.sha
```

Conceptually:

```text
<YOUR_ACR_LOGIN_SERVER>/admitionum/<commit>
```

More precisely, the current workflow produces:

```text
<YOUR_ACR_LOGIN_SERVER>/admitionum:<FULL_GIT_SHA>
```

It does not deploy using:

```text
latest
```

This creates traceability between:

```text
Git commit
    |
    v
Docker image
    |
    v
Container App revision
```

---

# 54. Test the CI flow with a pull request

Create a branch:

```bash
git switch -c test-ci
```

Make a harmless test change.

Commit and push it:

```bash
git add .
git commit -m "test: verify CI workflow"
git push -u origin test-ci
```

Create a pull request targeting:

```text
main
```

Expected behaviour:

```text
Test and package              -> executes
Build image and deploy Azure  -> skipped
```

The skipped deployment is correct.

A pull request should not deploy to production.

After testing, do not merge a meaningless change solely to exercise production deployment.

Use a real reviewed change when testing the full CD flow.

---

# 55. Production deployment from main

When an approved pull request is merged:

```text
merge
  |
  v
push to main
  |
  v
test job
  |
  v
deploy job
```

The deployment job:

1. Reuses the tested JAR.
2. Builds the Docker image.
3. Logs in to Azure using OIDC.
4. Logs in to ACR.
5. Pushes the image.
6. Updates Azure Container Apps.
7. Confirms the deployed image.
8. Calls the public health endpoint.

A deployment should not be considered successful merely because the Docker image was pushed.

The application health check must also succeed.

---

# 56. Verify which commit is deployed

Get the current Git commit:

```bash
git rev-parse HEAD
```

Inspect the image configured in Azure:

```powershell
az containerapp show `
    --name $ContainerApp `
    --resource-group $ResourceGroup `
    --query "properties.template.containers[0].image" `
    --output tsv
```

The image tag should correspond to the Git SHA deployed by the workflow.

If it does not match immediately after a merge, first verify that the GitHub Actions deployment has actually finished.

Do not recreate Azure resources merely because the workflow is still running.

---

# 57. Recommended verification after deployment

Verify:

```text
GitHub Actions completed successfully
Container App revision is healthy
Configured image matches expected commit
/api/public/health returns HTTP 200
Public form loads
Administrator login works
Dashboard loads
Azure SQL data can be read
RSVP can be created
RSVP can be updated
CSV can be exported
```

This validates the complete application rather than only the infrastructure.

---

# 58. Data for a public portfolio deployment

A public portfolio environment should only contain fictitious information.

Do not use:

```text
Real guest names
Real phone numbers
Real email addresses
Real food intolerance information
Real invitation access codes
Real wedding comments
Real CSV exports
```

Use fabricated demonstration data instead.

Real wedding information should belong to an independent private deployment.

---

# 59. Values that must never be committed

Never commit:

```text
.env
SQL passwords
Administration passwords
Azure access tokens
Registry passwords
Personal guest data
Production database exports
Real RSVP CSV files
Real invitation access codes
```

Before committing configuration or documentation, review:

```bash
git status
```

and:

```bash
git diff
```

You can also search for suspicious terms:

```bash
git grep -n -i "password"
git grep -n -i "secret"
git grep -n -i "jdbc:sqlserver"
```

Finding a variable name such as:

```text
${SPRING_DATASOURCE_PASSWORD}
```

is normal.

Finding a real password is not.

---

# 60. Common local errors

## Maven uses the wrong Java version

Check:

```bash
./mvnw -version
```

or:

```powershell
.\mvnw.cmd -version
```

Review `JAVA_HOME`.

---

## Port 8080 is already in use

Windows:

```powershell
netstat -ano | findstr :8080
```

Git Bash:

```bash
netstat -ano | grep 8080
```

Stop the process currently using the port before running Admitionum.

---

## ADMIN_USERNAME cannot be resolved

The local profile requires:

```text
ADMIN_USERNAME
ADMIN_PASSWORD
```

Define both variables before starting Spring Boot.

---

## H2 database is locked

Do not run multiple local Admitionum processes against the same file database.

Stop the existing application before starting another local instance.

Automated tests do not use this file because they use a separate H2 in-memory database.

---

# 61. Common Docker errors

## Docker command exists but containers do not run

Run:

```bash
docker info
```

If Docker Desktop is not running, start it.

---

## JAR not found during docker build

Run:

```bash
./mvnw clean package
```

or:

```powershell
.\mvnw.cmd clean package
```

Verify:

```text
target/admitionum-0.0.1-SNAPSHOT.jar
```

---

## Container starts but administration configuration is missing

Provide:

```text
ADMIN_USERNAME
ADMIN_PASSWORD
```

as environment variables.

---

# 62. Common Azure SQL errors

## TCP connection fails

Check firewall/network access before changing Spring Boot.

From PowerShell:

```powershell
Test-NetConnection `
    -ComputerName "<YOUR_SQL_SERVER>.database.windows.net" `
    -Port 1433
```

This tests only network connectivity.

It does not validate:

```text
SQL password
SQL user
Database permissions
Hibernate
```

---

## Login failed

Verify:

```text
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
```

and confirm that the database user exists inside the intended database.

Do not replace the application account permanently with the SQL administrator as a shortcut.

---

## Hibernate cannot determine the dialect

If this appears after a JDBC connection error, diagnose the database connection first.

Hibernate cannot inspect database metadata without a working JDBC connection.

Do not add configuration merely to hide a secondary symptom.

---

# 63. Common Azure Container Apps errors

## Application temporarily does not answer

With:

```text
min replicas = 0
```

the Container App may need to start a new replica.

Check:

```powershell
az containerapp revision list ...
```

and application logs before changing the image.

---

## SQL database is resuming

If the Azure SQL configuration can pause, allow time for it to resume.

Retry after verifying its state.

---

## Image pull error

Check:

```text
ACR image exists
Registry configuration
Registry credentials or identity
Image tag
```

Do not change Spring Boot code when Azure cannot download the image.

---

# 64. Common GitHub Actions errors

## Azure login fails

Check:

```text
Repository
Branch
OIDC federated subject
AZURE_CLIENT_ID
AZURE_TENANT_ID
AZURE_SUBSCRIPTION_ID
```

Do not create an Azure client secret as the first workaround.

---

## ACR push returns unauthorized

Check that the deployment identity has:

```text
AcrPush
```

at the correct ACR scope.

Role assignment propagation can also take time.

---

## Container App update returns AuthorizationFailed

Check the Azure role assignment and its scope.

The deployment identity needs permission to update the target Container App.

---

## Deploy job is skipped on a pull request

This is expected.

The workflow only deploys from:

```text
main
```

after the changes are integrated.

---

# 65. Clean local secrets

When production testing is finished, close the terminal or remove temporary environment variables.

PowerShell example:

```powershell
Remove-Item Env:SPRING_PROFILES_ACTIVE `
    -ErrorAction SilentlyContinue

Remove-Item Env:SPRING_DATASOURCE_URL `
    -ErrorAction SilentlyContinue

Remove-Item Env:SPRING_DATASOURCE_USERNAME `
    -ErrorAction SilentlyContinue

Remove-Item Env:SPRING_DATASOURCE_PASSWORD `
    -ErrorAction SilentlyContinue

Remove-Item Env:ADMIN_USERNAME `
    -ErrorAction SilentlyContinue

Remove-Item Env:ADMIN_PASSWORD `
    -ErrorAction SilentlyContinue
```

A new clean terminal will also avoid accidentally reusing an old environment configuration.

---

# 66. Final replication checklist

Local development:

```text
[ ] Repository cloned.
[ ] Java 25 available.
[ ] Maven Wrapper uses Java 25.
[ ] ADMIN_USERNAME configured locally.
[ ] ADMIN_PASSWORD configured locally.
[ ] clean verify succeeds.
[ ] Spring Boot starts.
[ ] Local profile is active.
[ ] H2 works.
[ ] Health endpoint works.
[ ] Administration login works.
```

Docker:

```text
[ ] Docker engine works.
[ ] JAR created.
[ ] Docker image built.
[ ] Container starts.
[ ] Health endpoint works from the container.
[ ] No secrets exist inside the Dockerfile.
```

Azure SQL:

```text
[ ] SQL server and database exist.
[ ] schema.sql executed.
[ ] Invitations table exists.
[ ] RsvpResponses table exists.
[ ] Application SQL user exists.
[ ] db_datareader granted.
[ ] db_datawriter granted.
[ ] VIEW DEFINITION granted.
[ ] JDBC connection works.
[ ] Hibernate validates the schema.
```

Azure Container deployment:

```text
[ ] ACR exists.
[ ] Docker image pushed to ACR.
[ ] Container Apps environment exists.
[ ] Container App exists.
[ ] External HTTPS ingress works.
[ ] Target port is 8080.
[ ] Production profile is active.
[ ] Runtime secrets configured.
[ ] Minimum replicas is 0.
[ ] Maximum replicas is 1.
[ ] Health endpoint works over HTTPS.
[ ] Administration login works.
```

CI/CD:

```text
[ ] Dedicated Azure deployment identity exists.
[ ] GitHub OIDC federation configured.
[ ] Federation is limited to the intended repository and branch.
[ ] AcrPush assigned.
[ ] Container App update permission assigned.
[ ] AZURE_CLIENT_ID configured.
[ ] AZURE_TENANT_ID configured.
[ ] AZURE_SUBSCRIPTION_ID configured.
[ ] Five repository variables configured.
[ ] Pull requests run tests without deployment.
[ ] Pushes to main can deploy.
[ ] Images use Git commit SHA tags.
[ ] Deployed image is verified.
[ ] Public health endpoint is verified automatically.
```

Security:

```text
[ ] No .env file committed.
[ ] No SQL password committed.
[ ] No administration password committed.
[ ] No registry password committed.
[ ] No real guest data committed.
[ ] No Azure client secret required by GitHub Actions.
```

When every relevant item has been verified, the reproduced Admitionum environment follows the same architecture as the original project.