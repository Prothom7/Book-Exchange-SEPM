# ========================================
# USER ROLE PROMOTION SCRIPT
# ========================================

$ErrorActionPreference = "Stop"

function Get-EnvValue {
    param(
        [string]$Key,
        [string]$DefaultValue
    )

    if (Test-Path ".env") {
        $line = Get-Content ".env" | Where-Object { $_ -match "^$Key=" } | Select-Object -First 1
        if ($line) {
            return ($line -split "=", 2)[1].Trim()
        }
    }

    $envValue = [Environment]::GetEnvironmentVariable($Key)
    if (![string]::IsNullOrWhiteSpace($envValue)) {
        return $envValue
    }

    return $DefaultValue
}

try {
    docker info | Out-Null
} catch {
    Write-Host "Docker engine is not reachable. Please start Docker Desktop first." -ForegroundColor Red
    exit 1
}

$dbUser = Get-EnvValue -Key "POSTGRES_USER" -DefaultValue "postgres"
$dbName = Get-EnvValue -Key "POSTGRES_DB" -DefaultValue "book_exchange"

$username = Read-Host "Enter username to promote"
$roleChoice = Read-Host "Promote to (1=MODERATOR, 2=ADMIN, 3=BOTH)"

Write-Host "`nConnecting to PostgreSQL container..." -ForegroundColor Yellow

switch ($roleChoice) {
    "1" {
        $sql = @"
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = '$username' AND r.name = 'ROLE_MODERATOR'
AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);
"@
        Write-Host "Adding MODERATOR role..." -ForegroundColor Cyan
    }
    "2" {
        $sql = @"
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = '$username' AND r.name = 'ROLE_ADMIN'
AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);
"@
        Write-Host "Adding ADMIN role..." -ForegroundColor Cyan
    }
    "3" {
        $sql = @"
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username = '$username' AND r.name IN ('ROLE_MODERATOR', 'ROLE_ADMIN')
AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id);
"@
        Write-Host "Adding MODERATOR and ADMIN roles..." -ForegroundColor Cyan
    }
    default {
        Write-Host "Invalid choice!" -ForegroundColor Red
        exit 1
    }
}

docker exec -i book_exchange_db psql -U $dbUser -d $dbName -c "$sql" | Out-Host

Write-Host "`nVerifying roles for '$username':" -ForegroundColor Yellow
$checkSql = "SELECT u.username, string_agg(r.name, ', ' ORDER BY r.name) as roles FROM users u LEFT JOIN user_roles ur ON u.id = ur.user_id LEFT JOIN roles r ON ur.role_id = r.id WHERE u.username = '$username' GROUP BY u.username;"
docker exec -i book_exchange_db psql -U $dbUser -d $dbName -c "$checkSql" | Out-Host

Write-Host "`nDone! Logout and login again to see new role access.`n" -ForegroundColor Green
