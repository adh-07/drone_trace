@echo off
setlocal

echo Setting up PostgreSQL database...

REM Check if psql is available
where psql >nul 2>&1
if errorlevel 1 (
    echo ERROR: 'psql' not found on PATH. Please install PostgreSQL client tools or add psql to PATH.
    echo Download: https://www.postgresql.org/download/
    pause
    exit /b 1
)

REM Prompt for connection details with defaults
set "PGUSER=postgres"
set "PGHOST=localhost"
set "PGPORT=5432"
set "PGDBFILE=src/main/resources/db/init.sql"

set /p PGUSER=Enter PostgreSQL user [postgres]: 
if "%PGUSER%"=="" set "PGUSER=postgres"

set /p PGHOST=Enter host [localhost]: 
if "%PGHOST%"=="" set "PGHOST=localhost"

set /p PGPORT=Enter port [5432]: 
if "%PGPORT%"=="" set "PGPORT=5432"

REM Secure-ish password prompt via PowerShell (no echo). Falls back to visible if PS fails.
for /f "usebackq delims=" %%p in (`powershell -NoProfile -Command "$p = Read-Host -AsSecureString 'Enter password'; $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($p); [Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)" 2^>NUL`) do set "PGPASSWORD=%%p"

if not defined PGPASSWORD (
    echo Warning: Falling back to visible password input (PowerShell secure prompt unavailable).
    set /p PGPASSWORD=Enter password: 
)

set PSQL_CMD=psql -U "%PGUSER%" -h "%PGHOST%" -p %PGPORT%

echo Creating database and tables using %PGHOST%:%PGPORT% as %PGUSER% ...
setlocal enableextensions
set "_OLDPG=%PGPASSWORD%"
set PGPASSWORD=%PGPASSWORD%
"%PSQL_CMD%" -f "%PGDBFILE%"
set PGPASSWORD=%_OLDPG%
endlocal & set PGPASSWORD=

if %ERRORLEVEL% NEQ 0 (
    echo Error creating database and tables
    pause
    exit /b 1
)

echo Database setup completed successfully!
pause