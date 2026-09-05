@echo off
REM ============================================================
REM  Unit Creator - INSTALLER
REM  Put this folder inside your BCU folder (next to BCU-x.jar),
REM  close BCU, then run this. Do it once, and again after each
REM  BCU update. A backup of the original jar is kept.
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

REM --- find a Java launcher -----------------------------------
set "JAVA="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA=%JAVA_HOME%\bin\java.exe"
if not defined JAVA set "JAVA=java"

REM --- find the BCU jar (parent folder first, then here) ------
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
    echo [ERROR] No BCU jar found.
    echo Put this "Unit-Creator" folder INSIDE your BCU folder
    echo ^(the folder that contains BCU-x-x-x-x.jar^), then run this again.
    pause
    exit /b 1
)

echo Installing Unit Creator into:
echo   !BCU_JAR!
echo.
"%JAVA%" -jar "%INST%" "!BCU_JAR!"
echo.
pause
endlocal
