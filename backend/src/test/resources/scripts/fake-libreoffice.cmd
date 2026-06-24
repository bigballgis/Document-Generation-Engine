@echo off
setlocal EnableDelayedExpansion
set OUTDIR=
:parse
if "%~1"=="" goto write
if "%~1"=="--outdir" (
  set OUTDIR=%~2
  shift
  shift
  goto parse
)
shift
goto parse
:write
if "%OUTDIR%"=="" exit /b 1
echo %%PDF-1.4> "%OUTDIR%\input.pdf"
exit /b 0
