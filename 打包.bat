@echo off
chcp 65001 >nul
setlocal
set JAVA_HOME=C:\Users\YB\.workbuddy\toolchains\android\jdk-17
set ANDROID_HOME=C:\Users\YB\.workbuddy\toolchains\android\sdk
set ANDROID_SDK_ROOT=%ANDROID_HOME%
cd /d "%~dp0"
call "C:\Users\YB\.workbuddy\toolchains\android\gradle\bin\gradle.bat" assembleRelease --console=plain
if errorlevel 1 goto fail
echo.
echo 打包完成：app\build\outputs\apk\release\app-release.apk
goto end
:fail
echo.
echo 打包失败，请看上面的日志。
:end
pause
