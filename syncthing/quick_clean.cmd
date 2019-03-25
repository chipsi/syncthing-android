@echo off
cls
REM 
RD /S /Q "%LocalAppData%\gobuild"
RD /S /Q "%~dps0..\app\src\main\jniLibs"
REM 
echo Done.
timeout 2
