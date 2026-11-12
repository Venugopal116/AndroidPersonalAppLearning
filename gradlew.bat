@echo off
setlocal
if not exist "%~dp0gradle\wrapper\gradle-wrapper.jar" (
    echo Wrapper JAR is missing. Run scripts/bootstrap-wrapper.ps1 in an authorized environment; see README.
    exit /b 1
)
if defined JAVA_HOME (
    set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_CMD=java.exe"
)
"%JAVA_CMD%" -classpath "%~dp0gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%