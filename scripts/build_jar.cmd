@echo off
setlocal enabledelayedexpansion

rem Resolve root as one level up from this script's directory
for %%I in ("%~dp0..") do set "ROOT=%%~fI"
set "SRC=%ROOT%\src"
set "DATA=%ROOT%\data"
set "BUILD=%ROOT%\build"
set "CLASSES=%BUILD%\classes"
set "JAR=%BUILD%\SuperPuperShooter.jar"
set "MAIN_CLASS=Main"

if not exist "%SRC%" (
  echo [ERROR] src folder not found: "%SRC%"
  set "EXITCODE=1"
  goto :finish
)

rem Clean build output
if exist "%BUILD%" rmdir /s /q "%BUILD%"
mkdir "%CLASSES%"

rem Compile Java sources
echo [INFO] Compiling sources...
set "SOURCES_LIST=%BUILD%\sources.txt"
if exist "%SOURCES_LIST%" del /q "%SOURCES_LIST%"
dir /b /s "%SRC%\*.java" > "%SOURCES_LIST%"
for %%A in ("%SOURCES_LIST%") do if %%~zA==0 (
  echo [ERROR] No .java files found under: "%SRC%"
  set "EXITCODE=1"
  goto :finish
)

javac -encoding UTF-8 -d "%CLASSES%" @"%SOURCES_LIST%"
if errorlevel 1 (
  echo [ERROR] javac failed.
  set "EXITCODE=1"
  goto :finish
)

rem Copy data assets into classes output so they get packed into the jar
if exist "%DATA%" (
  echo [INFO] Copying data assets...
  xcopy /e /i /y "%DATA%" "%CLASSES%\data" >nul
) else (
  echo [WARN] data folder not found: "%DATA%"
)

rem Build runnable jar
echo [INFO] Building jar...
jar cfe "%JAR%" "%MAIN_CLASS%" -C "%CLASSES%" .
if errorlevel 1 (
  echo [ERROR] jar failed.
  set "EXITCODE=1"
  goto :finish
)

echo [OK] Built: "%JAR%"
set "EXITCODE=0"

:finish
echo.
if /i "%CI%"=="true" goto :nopause
if /i "%CI%"=="1" goto :nopause
if defined CI goto :nopause
if /i "%NO_PAUSE%"=="1" goto :nopause
pause
:nopause
endlocal & exit /b %EXITCODE%
