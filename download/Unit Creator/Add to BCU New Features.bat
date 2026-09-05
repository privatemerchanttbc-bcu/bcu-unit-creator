@echo off
REM ============================================================
REM  Unit Creator - ADD TO THE SHARED LAUNCHER
REM
REM  Registers Unit Creator with "BCU New Features.bat", the
REM  launcher several BCU patches share. After this, that one
REM  launcher starts BCU with every registered feature at once.
REM
REM  Your BCU jar is not modified.
REM ============================================================

setlocal enabledelayedexpansion

set "HERE=%~dp0"
if "%HERE:~-1%"=="\" set "HERE=%HERE:~0,-1%"

set "INST=%HERE%\unit-creator.jar"
if not exist "%INST%" (
    echo [ERROR] unit-creator.jar is missing from this folder.
    pause
    exit /b 1
)

set "JAVA="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA=%JAVA_HOME%\bin\java.exe"
if not defined JAVA set "JAVA=java"

set "BCU_JAR="
for %%f in ("%HERE%\..\BCU-0*.jar") do (
    echo %%~nxf | findstr /i "installer directedit speedscale unitcreator attack-ld backup patch" >nul
    if errorlevel 1 if not defined BCU_JAR set "BCU_JAR=%%~ff"
)
if not defined BCU_JAR for %%f in ("%HERE%\..\BCU*.jar") do (
    echo %%~nxf | findstr /i "installer directedit speedscale unitcreator attack-ld backup patch" >nul
    if errorlevel 1 if not defined BCU_JAR set "BCU_JAR=%%~ff"
)
if not defined BCU_JAR (
    echo [ERROR] No BCU jar found.
    echo Put this "Unit Creator" folder INSIDE your BCU folder
    echo ^(the folder that contains BCU-x-x-x-x.jar^), then run this again.
    pause
    exit /b 1
)

echo Registering Unit Creator with the shared launcher...
echo.
"%JAVA%" -jar "%INST%" --register "!BCU_JAR!"
echo.
pause
endlocal
