@echo off
REM ============================================================
REM  Unit Creator - LAUNCHER
REM
REM  Starts BCU with Unit Creator loaded. Your BCU jar is not
REM  modified in any way. Delete this folder and BCU is exactly
REM  as it was.
REM
REM  Put this "Unit Creator" folder inside your BCU folder, then
REM  run this instead of starting BCU the usual way.
REM ============================================================

setlocal enabledelayedexpansion

set "HERE=%~dp0"
if "%HERE:~-1%"=="\" set "HERE=%HERE:~0,-1%"

set "AGENT=%HERE%\unit-creator.jar"
if not exist "%AGENT%" (
    echo [ERROR] unit-creator.jar is missing from this folder.
    pause
    exit /b 1
)

REM --- find a Java launcher -----------------------------------
set "JAVA="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javaw.exe" set "JAVA=%JAVA_HOME%\bin\javaw.exe"
if not defined JAVA set "JAVA=javaw"

REM --- find the BCU jar in the parent folder ------------------
set "BCU_JAR="
for %%f in ("%HERE%\..\BCU-0*.jar") do (
    echo %%~nxf | findstr /i "installer directedit speedscale unitcreator backup" >nul
    if errorlevel 1 if not defined BCU_JAR set "BCU_JAR=%%~ff"
)
if not defined BCU_JAR for %%f in ("%HERE%\..\BCU*.jar") do (
    echo %%~nxf | findstr /i "installer directedit speedscale unitcreator backup" >nul
    if errorlevel 1 if not defined BCU_JAR set "BCU_JAR=%%~ff"
)
if not defined BCU_JAR (
    echo [ERROR] No BCU jar found.
    echo Put this "Unit Creator" folder INSIDE your BCU folder
    echo ^(the folder that contains BCU-x-x-x-x.jar^), then run this again.
    pause
    exit /b 1
)

echo Starting BCU with Unit Creator...
echo   !BCU_JAR!
cd /d "%HERE%\.."
start "" "%JAVA%" -javaagent:"%AGENT%" -jar "!BCU_JAR!" %*
endlocal
