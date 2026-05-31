@echo off
setlocal enabledelayedexpansion

REM JXCache Windows 构建脚本
REM 用法：script\build.bat [选项]
REM 可选参数：
REM   -s, --skip-tests    跳过测试
REM   -u, --skip-ui       跳过前端构建
REM   -q, --quick         快速构建（跳过测试和前端）
REM   -h, --help          显示帮助

set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
set "UI_DIR=%PROJECT_ROOT%\jxcache-ui"

set SKIP_TESTS=false
set SKIP_UI=false

:parse_args
if "%~1"=="" goto :start_build
if "%~1"=="-s" (set SKIP_TESTS=true& shift& goto :parse_args)
if "%~1"=="--skip-tests" (set SKIP_TESTS=true& shift& goto :parse_args)
if "%~1"=="-u" (set SKIP_UI=true& shift& goto :parse_args)
if "%~1"=="--skip-ui" (set SKIP_UI=true& shift& goto :parse_args)
if "%~1"=="-q" (set SKIP_TESTS=true& set SKIP_UI=true& shift& goto :parse_args)
if "%~1"=="--quick" (set SKIP_TESTS=true& set SKIP_UI=true& shift& goto :parse_args)
if "%~1"=="-h" goto :show_help
if "%~1"=="--help" goto :show_help
echo [ERROR] 未知参数：%~1
exit /b 1

:show_help
echo 用法：script\build.bat [选项]
echo 可选参数：
echo   -s, --skip-tests    跳过测试
echo   -u, --skip-ui       跳过前端构建
echo   -q, --quick         快速构建（跳过测试和前端）
echo   -h, --help          显示帮助
exit /b 0

:start_build
cd /d "%PROJECT_ROOT%"

echo ============================================
echo JXCache 构建
echo ============================================
echo 项目目录：%PROJECT_ROOT%
echo 跳过测试：%SKIP_TESTS%
echo 跳过前端：%SKIP_UI%
echo ============================================

REM 检查 Maven 环境
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] 未找到 Maven，请先安装。
    exit /b 1
)

echo.
echo [INFO] Maven 版本：
for /f "tokens=*" %%i in ('mvn -version 2^>^&1 ^| findstr /n "." ^| findstr "^1:"') do (
    set "line=%%i"
    echo !line:~2!
)

REM 构建前端
if "%SKIP_UI%"=="false" (
    if exist "%UI_DIR%" (
        echo.
        echo ============================================
        echo [STEP] 构建前端...
        echo ============================================
        
        where npm >nul 2>&1
        if %errorlevel% neq 0 (
            echo [WARN] 未找到 npm，跳过前端构建
        ) else (
            cd /d "%UI_DIR%"
            
            if not exist "node_modules" (
                echo [INFO] 安装 npm 依赖...
                call npm install --silent
            )
            
            echo [INFO] 开始构建前端...
            call npm run build --silent
            
            if %errorlevel% neq 0 (
                echo [ERROR] 前端构建失败
                exit /b 1
            )
            echo [OK] 前端构建完成
            
            cd /d "%PROJECT_ROOT%"
        )
    )
)

REM 构建 Maven 项目
echo.
echo ============================================
echo [STEP] 构建 Maven 项目...
echo ============================================

set "MVN_OPTS=-q"

if "%SKIP_TESTS%"=="true" (
    set "MVN_OPTS=%MVN_OPTS% -DskipTests"
)

echo [INFO] 执行：mvn clean install %MVN_OPTS%
call mvn clean install %MVN_OPTS%

if %errorlevel% neq 0 (
    echo [ERROR] Maven 构建失败
    exit /b 1
)

echo.
echo ============================================
echo [OK] 构建完成
echo ============================================
echo.
echo 已安装的构件：
echo   - jxcache-common
echo   - jxcache-observer
echo   - jxcache-aggregator-core
echo   - jxcache-registry-spi
echo   - jxcache-registry-nacos
echo   - jxcache-registry-fixed
echo   - jxcache-dubbo
echo   - jxcache-starter-observer
echo   - jxcache-starter-aggregator-core
echo   - jxcache-starter-aggregator-nacos
echo   - jxcache-starter-dubbo
echo.
echo Maven 本地仓库：%USERPROFILE%\.m2\repository\dev\yibin\
echo.
echo 依赖示例：
echo   ^<dependency^>
echo       ^<groupId^>dev.yibin^</groupId^>
echo       ^<artifactId^>jxcache-starter-observer^</artifactId^>
echo       ^<version^>1.0.0^</version^>
echo   ^</dependency^>

endlocal
