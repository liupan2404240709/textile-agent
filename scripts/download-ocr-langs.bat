@echo off
chcp 65001 >nul
echo ========================================
echo OCR 语言包自动下载脚本
echo ========================================
echo.

REM 创建 tessdata 目录
if not exist "tessdata" (
    echo [1/3] 创建 tessdata 目录...
    mkdir tessdata
    echo ✓ 目录创建成功
) else (
    echo [1/3] tessdata 目录已存在
)
echo.

REM 检查 curl 是否可用
where curl >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 curl 命令
    echo 请手动下载以下文件并放入 tessdata 目录:
    echo - chi_sim.traineddata
    echo - eng.traineddata
    echo 下载地址: https://github.com/tesseract-ocr/tessdata
    pause
    exit /b 1
)

echo [2/3] 下载中文简体语言包 (chi_sim.traineddata)...
curl -L -o tessdata\chi_sim.traineddata https://github.com/tesseract-ocr/tessdata/raw/main/chi_sim.traineddata
if %errorlevel% equ 0 (
    echo ✓ 中文语言包下载成功
) else (
    echo ✗ 中文语言包下载失败，请检查网络连接
    pause
    exit /b 1
)
echo.

echo [3/3] 下载英文语言包 (eng.traineddata)...
curl -L -o tessdata\eng.traineddata https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata
if %errorlevel% equ 0 (
    echo ✓ 英文语言包下载成功
) else (
    echo ✗ 英文语言包下载失败，请检查网络连接
    pause
    exit /b 1
)
echo.

echo ========================================
echo ✓ 所有语言包下载完成！
echo ========================================
echo.
echo 文件位置: %cd%\tessdata\
dir tessdata\*.traineddata
echo.
echo 下一步:
echo 1. 安装 Tesseract OCR (如果还未安装)
echo 2. 启动 Spring Boot 应用
echo 3. 上传扫描件 PDF 测试 OCR 功能
echo.
pause
