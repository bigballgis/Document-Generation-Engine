@echo off
REM Windows double for DockerExecPdfConversionServiceTest (FOS-W12-1).
setlocal EnableDelayedExpansion
set "POINTER=%TMP%\docgen-fake-docker-state.pointer"
if defined DOCGEN_FAKE_DOCKER_STATE_POINTER set "POINTER=%DOCGEN_FAKE_DOCKER_STATE_POINTER%"
if exist "%POINTER%" (
  set /p STATE_ROOT=<"%POINTER%"
) else if defined DOCGEN_FAKE_DOCKER_STATE (
  set "STATE_ROOT=%DOCGEN_FAKE_DOCKER_STATE%"
) else (
  set "STATE_ROOT=%TMP%\docgen-fake-docker-state"
)
if not exist "%STATE_ROOT%" mkdir "%STATE_ROOT%"
set "CMD=%~1"
shift
if /I "%CMD%"=="cp" goto :cp
if /I "%CMD%"=="exec" goto :exec
exit /b 1

:cp
set "SRC=%~1"
set "DEST=%~2"
echo %DEST% | findstr ":" >nul
if not errorlevel 1 (
  for /f "tokens=1* delims=:" %%A in ("%DEST%") do (
    set "CONTAINER=%%A"
    set "REMOTE=%%B"
  )
  set "TARGET=%STATE_ROOT%\!CONTAINER!!REMOTE!"
  for %%F in ("!TARGET!") do if not exist "%%~dpF" mkdir "%%~dpF"
  copy /Y "%SRC%" "!TARGET!" >nul
  exit /b 0
)
echo %SRC% | findstr ":" >nul
if not errorlevel 1 (
  for /f "tokens=1* delims=:" %%A in ("%SRC%") do (
    set "CONTAINER=%%A"
    set "REMOTE=%%B"
  )
  set "SOURCE_FILE=%STATE_ROOT%\!CONTAINER!!REMOTE!"
  if exist "!SOURCE_FILE!" (
    copy /Y "!SOURCE_FILE!" "%DEST%" >nul
  ) else (
    >"%DEST%" echo %%PDF-1.4
  )
  exit /b 0
)
exit /b 1

:exec
REM Best-effort: invoke remaining args after remapping /tmp paths is hard in cmd;
REM for Windows unit coverage we only need convert success via cp PDF stub above.
REM When libreoffice is invoked, call through.
set "CONTAINER=%~1"
shift
"%*"
exit /b %ERRORLEVEL%
