@echo off
REM
REM Runtime Variables.
IF NOT DEFINED JAVA_HOME SET JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr
REM
where /q java
IF NOT "%ERRORLEVEL%" == "0" SET PATH=%PATH%;%JAVA_HOME%\bin
where /q java
IF NOT "%ERRORLEVEL%" == "0" echo [ERROR] java.exe not found on PATH env var. Download 'https://www.oracle.com/java/technologies/downloads/#java17' and run the installer & pause & goto :checkPrerequisites
REM
goto :eof
