@echo off
setlocal
cd /d "%~dp0"

if exist out rmdir /s /q out
mkdir out

javac -d out src\NetworkRouteFinder.java src\NetworkRouteFinderDemo.java test\LabTestRunner.java test\ComplexityExperiment.java
if errorlevel 1 exit /b 1

java -ea -cp out LabTestRunner
exit /b %errorlevel%
