@echo off
cls
SET SCRIPT_PATH=%~dps0
cd /d "%SCRIPT_PATH%"
REM 
REM Script Consts.
REM 
REM 	SET PACKAGE_NAME=com.github.catfriend1.syncthingandroid
SET PACKAGE_NAME=com.github.catfriend1.syncthingandroid.debug
REM 
REM 	SET DATA_ROOT=/data/user/0
SET DATA_ROOT=/data/data
REM 
:loopMe
cls
adb shell "su root cat %DATA_ROOT%/%PACKAGE_NAME%/files/config.xml" > "%SCRIPT_PATH%config.xml"
IF EXIST "%SCRIPT_PATH%config.xml" TYPE "%SCRIPT_PATH%config.xml" | more
echo.
pause
echo.
echo ==========================================================
echo.
goto :loopMe
