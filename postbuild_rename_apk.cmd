@echo off
title %~x0
setlocal enabledelayedexpansion
cls
SET SCRIPT_PATH=%~dps0
REM 
SET PATH=%PATH%;"%ProgramFiles%\Git\bin"
REM 
REM Get "applicationId"
FOR /F "tokens=2 delims= " %%A IN ('type "app\build.gradle" 2^>^&1 ^| findstr "applicationId"') DO SET APPLICATION_ID=%%A
SET APPLICATION_ID=%APPLICATION_ID:"=%
echo applicationId="%APPLICATION_ID%"
REM 
REM Get "versionName"
FOR /F "tokens=2 delims= " %%A IN ('type "app\build.gradle" 2^>^&1 ^| findstr "versionName"') DO SET VERSION_NAME=%%A
SET VERSION_NAME=%VERSION_NAME:"=%
echo versionName="%VERSION_NAME%"
REM 
REM Get short hash of last commit.
FOR /F "tokens=1" %%A IN ('git rev-parse --short --verify HEAD 2^>NUL:') DO SET COMMIT_SHORT_HASH=%%A
echo commit="%COMMIT_SHORT_HASH%"
REM 
REM Rename APK to be ready for upload to the GitHub release page.
call :renIfExist %SCRIPT_PATH%app\build\outputs\apk\debug\app-debug.apk com.github.catfriend1.syncthingandroid_%VERSION_NAME%_%COMMIT_SHORT_HASH%.apk
call :renIfExist %SCRIPT_PATH%app\build\outputs\apk\release\app-release-unsigned.apk com.github.catfriend1.syncthingandroid_%VERSION_NAME%_%COMMIT_SHORT_HASH%.apk
REM 
echo [INFO] APK files renamed.
timeout 3
goto :eof

:renIfExist
REM 
REM Syntax:
REM 	call :renIfExist [FULL_FN_ORIGINAL] [FILENAME_RENAMED]
IF EXIST %1 REN %1 %2 & goto :eof
echo [INFO] File not found: %1
REM 
goto :eof
