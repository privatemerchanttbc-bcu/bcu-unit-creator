@echo off
REM ============================================================
REM  Unit Creator - UNINSTALLER
REM  Removes only the Unit Creator entries from the BCU jar.
REM  Everything else in the jar is left untouched.
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
    echo %%~nxf | findstr /i "installer directedit speedscale unitcreator backup" >nul
    if errorlevel 1 if not defined BCU_JAR set "BCU_JAR=%%~ff"
)
if not defined BCU_JAR for %%f in ("%HERE%\..\BCU*.jar") do (
    echo %%~nxf | findstr /i "installer directedit speedscale unitcreator backup" >nul
    if errorlevel 1 if not defined BCU_JAR set "BCU_JAR=%%~ff"
)
if not defined BCU_JAR for %%f in ("%HERE%\BCU*.jar") do (
    echo %%~nxf | findstr /i "installer directedit speedscale unitcreator backup" >nul
    if errorlevel 1 if not defined BCU_JAR set "BCU_JAR=%%~ff"
)
if not defined BCU_JAR (
    echo [ERROR] No BCU jar found next to this folder.
    pause
    exit /b 1
)

echo Removing Unit Creator from:
echo   !BCU_JAR!
echo.
"%JAVA%" -jar "%INST%" --uninstall "!BCU_JAR!"
echo.
pause
endlocal
