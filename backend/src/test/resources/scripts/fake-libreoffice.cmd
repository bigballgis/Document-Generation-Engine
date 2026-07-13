@echo off
setlocal EnableDelayedExpansion
set OUTDIR=
set CONVERT_TO=pdf
set INPUT=
set PROFILE=
:parse
if "%~1"=="" goto finalize
if "%~1"=="--outdir" (
  set OUTDIR=%~2
  shift
  shift
  goto parse
)
if "%~1"=="--convert-to" (
  set CONVERT_TO=%~2
  shift
  shift
  goto parse
)
if not "%~1"=="" (
  echo.%~1| findstr /B /C:"-env:UserInstallation=" >nul && (
    set PROFILE=%~1
    set PROFILE=!PROFILE:-env:UserInstallation=!
    set PROFILE=!PROFILE:file:///=!
    set PROFILE=!PROFILE:file://=!
    set PROFILE=!PROFILE:%%3A=:!
    set PROFILE=!PROFILE:%%20= !
  )
  if exist "%~1" set INPUT=%~1
)
shift
goto parse
:finalize
if not "%PROFILE%"=="" mkdir "%PROFILE%" 2>nul
if "%OUTDIR%"=="" exit /b 1
if /I "%CONVERT_TO%"=="docx" (
  if exist "%INPUT%" (
    copy /Y "%INPUT%" "%OUTDIR%\%~nx1" >nul
  ) else (
    echo PK> "%OUTDIR%\assembled-in.docx"
  )
  exit /b 0
)
echo %%PDF-1.4> "%OUTDIR%\input.pdf"
exit /b 0
