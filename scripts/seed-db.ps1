# Seed the Media Tracker PostgreSQL Database
param (
    [string]$Container = "local_postgres",
    [string]$User = "admin",
    [string]$Database = "media_app",
    [string]$SqlFile = "$PSScriptRoot/seed.sql"
)

Write-Host "Seeding database '$Database' in container '$Container' using '$SqlFile'..."

if (-not (Test-Path $SqlFile)) {
    Write-Error "Seed SQL file not found at $SqlFile"
    exit 1
}

Get-Content -Raw -Encoding UTF8 $SqlFile | docker exec -i $Container psql -U $User -d $Database

if ($LASTEXITCODE -eq 0) {
    Write-Host "Database seed completed successfully."
} else {
    Write-Error "Failed to seed database. psql exited with code $LASTEXITCODE"
    exit $LASTEXITCODE
}
