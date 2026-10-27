@echo off
setlocal
where uv >nul 2>nul
if errorlevel 1 (
    echo ERROR: uv is not installed or not in PATH.
    if /i not "%~1"=="--no-pause" pause
    exit /b 1
)
uv run --locked --project "%~dp0..\apps\ai-service" python "%~dp0start-services.py"
set "result=%errorlevel%"
if /i not "%~1"=="--no-pause" pause
exit /b %result%
