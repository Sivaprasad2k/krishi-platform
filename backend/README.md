# Backend local configuration

The backend requires credentials from the environment. Do not place database passwords or JWT signing keys in tracked configuration files.

## Local application

Use a local PostgreSQL database named `krishi_db` (or set `SPRING_DATASOURCE_URL` to another JDBC URL). The default username is `postgres`; set `SPRING_DATASOURCE_USERNAME` to override it. Supply the PostgreSQL password and a newly generated JWT signing key in your shell or local secret store:

```powershell
$env:SPRING_DATASOURCE_PASSWORD = '<your local PostgreSQL password>'
$keyBytes = New-Object byte[] 48
$keyGenerator = [Security.Cryptography.RandomNumberGenerator]::Create()
$keyGenerator.GetBytes($keyBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($keyBytes)
$keyGenerator.Dispose()
Remove-Variable keyBytes, keyGenerator
.\mvnw.cmd spring-boot:run
```

The JWT secret must contain at least 32 UTF-8 bytes. Do not reuse a production signing key for local development.

## Backend integration tests

Tests use PostgreSQL at `jdbc:postgresql://localhost:5432/krishi_test_db`. `SPRING_TEST_DATASOURCE_URL`, when set, must resolve to that exact local test URL; other targets are rejected before any database connection. Tests use `SPRING_DATASOURCE_USERNAME` (default `postgres`) and require `SPRING_DATASOURCE_PASSWORD`. The initializer connects only to the local PostgreSQL `postgres` administrative database to check for and, if absent, create `krishi_test_db`.

Supply a per-run key rather than a committed test key:

```powershell
$env:SPRING_DATASOURCE_PASSWORD = '<your test PostgreSQL password>'
$keyBytes = New-Object byte[] 48
$keyGenerator = [Security.Cryptography.RandomNumberGenerator]::Create()
$keyGenerator.GetBytes($keyBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($keyBytes)
$keyGenerator.Dispose()
Remove-Variable keyBytes, keyGenerator
.\mvnw.cmd test
```

## Production

Activate the `prod` profile and supply `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`, and `CORS_ALLOWED_ORIGINS` through the deployment environment. `JWT_EXPIRATION` and `PORT` have non-secret defaults.
