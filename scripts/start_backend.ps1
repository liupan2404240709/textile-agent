# 启动后端（Windows PowerShell）
# 用法：powershell -ExecutionPolicy Bypass -File scripts\start_backend.ps1
# macOS / Linux 直接执行：mvn -DskipTests package && java -jar target/java-ChatLLM-langchain4j-1.0-SNAPSHOT.jar

# 项目根目录 = 本脚本所在目录的上一级，无需硬编码路径
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

# 若已配置 JAVA_HOME 则优先使用（需为 JDK 17，而非 JRE）
if ($env:JAVA_HOME) { $env:Path = "$env:JAVA_HOME\bin;$env:Path" }

Write-Host "==> 项目目录: $Root"
Write-Host "==> 打包中（跳过测试）..."
mvn -DskipTests package
if ($LASTEXITCODE -ne 0) { Write-Error "Maven 打包失败，请检查 JDK 17 与依赖是否就绪"; exit 1 }

Write-Host "==> 启动服务，端口 8082，接口文档 http://localhost:8082/doc.html"
java -jar target\java-ChatLLM-langchain4j-1.0-SNAPSHOT.jar
