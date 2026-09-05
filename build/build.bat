@echo off
REM ============================================================
REM  Builds unit-creator-installer.jar from source.
REM
REM  Usage:   build.bat  [path\to\BCU-x-x-x-x.jar]
REM
REM  You need a JDK 17 or newer and a copy of the BCU jar. The
REM  BCU jar is used only as a compile reference - nothing from
REM  it is copied into the output.
REM ============================================================

setlocal enabledelayedexpansion

set "HERE=%~dp0"
if "%HERE:~-1%"=="\" set "HERE=%HERE:~0,-1%"
set "ROOT=%HERE%\.."

set "OUT=%ROOT%\out"
set "LIB=%ROOT%\lib"
set "SRC=%ROOT%\src"
set "SHIP=%ROOT%\download\Unit Creator"
set "OUTPUT=%SHIP%\unit-creator.jar"
set "MANIFEST=%HERE%\MANIFEST.MF"

REM --- locate a JDK -------------------------------------------
set "JAVAC="
set "JAR="
set "JAVA="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" (
    set "JAVAC=%JAVA_HOME%\bin\javac.exe"
    set "JAR=%JAVA_HOME%\bin\jar.exe"
    set "JAVA=%JAVA_HOME%\bin\java.exe"
)
if not defined JAVAC (
    where javac >nul 2>&1
    if not errorlevel 1 (
        set "JAVAC=javac"
        set "JAR=jar"
        set "JAVA=java"
    )
)
if not defined JAVAC (
    echo [build] ERROR: no JDK found.
    echo [build] Install a JDK 17 or newer, or set JAVA_HOME to point at one.
    echo [build] A plain JRE is not enough - the compiler is required.
    exit /b 1
)

REM --- locate the BCU jar -------------------------------------
set "BCU_JAR=%~1"
if not defined BCU_JAR if defined BCU_JAR_ENV set "BCU_JAR=%BCU_JAR_ENV%"
if not defined BCU_JAR for %%f in ("%ROOT%\bcu\BCU*.jar") do set "BCU_JAR=%%~ff"
if not defined BCU_JAR for %%f in ("%ROOT%\..\BCU-0*.jar") do set "BCU_JAR=%%~ff"
if not defined BCU_JAR for %%f in ("%ROOT%\..\BCU*.jar") do set "BCU_JAR=%%~ff"
if not defined BCU_JAR (
    echo [build] ERROR: BCU jar not found.
    echo [build] Pass it as an argument:
    echo [build]     build.bat "C:\path\to\BCU-0-5-8-8.jar"
    echo [build] or drop a copy into a "bcu" folder next to this repo.
    echo [build] It is only a compile reference and is never redistributed.
    exit /b 1
)
echo [build] BCU reference: !BCU_JAR!

set "CP=!BCU_JAR!"
for %%f in ("%LIB%\*.jar") do set "CP=!CP!;%%~ff"

echo [build] Checking source charset...
"%JAVA%" "%HERE%\CharsetGate.java" "%ROOT%"
if errorlevel 1 exit /b 1

echo [build] Cleaning...
if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%"

echo [build] Listing sources...
set "SRCLIST=%OUT%\sources.txt"
for /r "%SRC%" %%f in (*.java) do (
    set "P=%%~ff"
    set "P=!P:\=/!"
    echo "!P!">>"%SRCLIST%"
)

echo [build] Compiling for Java 8...
"%JAVAC%" --release 8 -encoding UTF-8 -cp "%CP%" -d "%OUT%" "@%SRCLIST%"
if errorlevel 1 (
    echo [build] ERROR: compilation failed
    exit /b 1
)

set /a SRCJAVA=0
for /r "%SRC%" %%f in (*.java) do set /a SRCJAVA+=1
echo [build] Compiled !SRCJAVA! source files.

echo [build] Bundling ASM...
pushd "%OUT%"
for %%f in ("%LIB%\*.jar") do "%JAR%" xf "%%~ff"
if exist "META-INF" rmdir /s /q "META-INF"
if exist "module-info.class" del "module-info.class"
del "sources.txt"
popd

echo [build] Packaging...
if not exist "%SHIP%" mkdir "%SHIP%"
"%JAR%" cfm "%OUTPUT%" "%MANIFEST%" -C "%OUT%" .
if errorlevel 1 (
    echo [build] ERROR: packaging failed
    exit /b 1
)

echo.
echo [build] SUCCESS: %OUTPUT%
for %%i in ("%OUTPUT%") do echo [build] Size: %%~zi bytes
echo.
endlocal
