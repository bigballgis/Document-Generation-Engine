@echo off
REM IBL-D4 chaos: hang until killed (process-level timeout path)
ping -n 60 127.0.0.1 >NUL
exit /b 1
