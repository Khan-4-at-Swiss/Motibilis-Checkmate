@echo off
setlocal
set DIRNAME=%~dp0
java -jar "%DIRNAME%gradle\wrapper\gradle-wrapper.jar" %*
exit /b %ERRORLEVEL%
