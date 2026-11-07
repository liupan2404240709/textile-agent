# 重启后端（Windows PowerShell）：先停掉占用 8082 的进程，再后台拉起 jar
# 用法：powershell -ExecutionPolicy Bypass -File scripts\restart_backend.ps1
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

if ($env:JAVA_HOME) { $env:Path = "$env:JAVA_HOME\bin;$env:Path" }

# 只结束监听 8082 的进程，避免误杀其它 Java 程序
$conn = Get-NetTCPConnection -LocalPort 8082 -State Listen -ErrorAction SilentlyContinue
if ($conn) {
    $conn | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
    Write-Host "==> 已停止占用 8082 的进程"
    Start-Sleep -Seconds 2
} else {
    Write-Host "==> 8082 当前无监听进程"
}

Start-Process -FilePath 'java' `
    -ArgumentList '-jar', 'target\java-ChatLLM-langchain4j-1.0-SNAPSHOT.jar' `
    -WorkingDirectory $Root `
    -RedirectStandardOutput "$Root\backend.log" `
    -RedirectStandardError  "$Root\backend.err" `
    -PassThru | Out-Null

Write-Host "==> 后端已后台启动，日志见 backend.log / backend.err"
