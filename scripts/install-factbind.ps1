<#
.SYNOPSIS
  把 FactBind 的"最小版"装进一个已有的 Spring Boot + React 项目（演示用，拷贝式安装）。

.DESCRIPTION
  它只做"装工具 + 改配置"，**不碰你的业务代码**：
    1. 拷入后端实现：backend/src/main/java/com/example/middemo/factbind/ 共 9 个类
    2. 拷入前端实现：frontend/src/factbind/index.ts
    3. 契约文件不存在时，拷入一份 contracts/api.yaml
    4. 改两处配置：application.yml、vite.config.ts
    5. 前端装一个依赖：yaml（契约是 YAML，前端要解析它）

  装完应用照常能跑（老的 @GetMapping 控制器继续工作）。接下来"人的活"是：
    · 把契约内容写对
    · 控制器注解换成 @FactBind / @FactBindParam
    · 前端 api 层把 URL 换成符号调用

.EXAMPLE
  ./scripts/install-factbind.ps1 -DryRun          # 先看会改什么
  ./scripts/install-factbind.ps1                  # 真装
  ./scripts/install-factbind.ps1 -Source factbind-min
#>
param(
    [string]$Target = ".",
    [string]$Source = "factbind-min",
    [switch]$DryRun
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path $Target).Path
$encoding = New-Object System.Text.UTF8Encoding $false
$changes = New-Object System.Collections.Generic.List[string]

$backendClasses = @(
    'ContractRegistry.java',
    'ResolvedOperation.java',
    'ResolvedParameter.java',
    'FactBind.java',
    'FactBindParam.java',
    'FactBindHandlerMapping.java',
    'FactBindParamArgumentResolver.java',
    'FactBindWebConfig.java',
    'FactBindException.java'
)

$backendDir = 'backend/src/main/java/com/example/middemo/factbind'
$frontendFile = 'frontend/src/factbind/index.ts'
$contractFile = 'contracts/api.yaml'

if (-not (Test-Path (Join-Path $root 'backend/pom.xml')) -or -not (Test-Path (Join-Path $root 'frontend/package.json'))) {
    throw "目标目录看起来不是本项目（缺少 backend/pom.xml 或 frontend/package.json）：$root"
}

function Read-Source([string]$relativePath) {
    $content = & git -C $root show "${Source}:$relativePath" 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw "拿不到 ${Source}:$relativePath —— 分支或文件不存在（可用 -Source 指定别的 ref）"
    }
    return $content
}

function Write-File([string]$relativePath, $content) {
    if ($DryRun) { return }
    $path = Join-Path $root $relativePath
    New-Item -ItemType Directory -Force -Path (Split-Path $path) | Out-Null
    [System.IO.File]::WriteAllLines($path, $content, $encoding)
}

# ── 1/2. 拷贝两边的实现 ────────────────────────────────────────────────────────
$existing = 0
foreach ($class in $backendClasses) {
    $relative = "$backendDir/$class"
    if (Test-Path (Join-Path $root $relative)) { $existing++ }
    Write-File $relative (Read-Source $relative)
}
$note = if ($existing -gt 0) { "（$existing 个已存在，已覆盖为 $Source 版本）" } else { "" }
$changes.Add("后端实现：$backendDir/ 9 个类$note")

$frontendExists = Test-Path (Join-Path $root $frontendFile)
Write-File $frontendFile (Read-Source $frontendFile)
$note = if ($frontendExists) { "（已存在，覆盖为 $Source 版本）" } else { "" }
$changes.Add("前端实现：$frontendFile$note")

# ── 3. 契约 ───────────────────────────────────────────────────────────────────
if (Test-Path (Join-Path $root $contractFile)) {
    $changes.Add("契约：$contractFile 已存在，不动它")
} else {
    Write-File $contractFile (Read-Source $contractFile)
    $changes.Add("契约：$contractFile（从 $Source 拷入）")
}

# ── 4. 两处配置 ───────────────────────────────────────────────────────────────
$yamlPath = Join-Path $root 'backend/src/main/resources/application.yml'
if (Test-Path $yamlPath) {
    $yaml = [System.IO.File]::ReadAllText($yamlPath)
    if ($yaml -match '(?m)^factbind:') {
        $changes.Add("配置：application.yml 已有 factbind 段，跳过")
    } else {
        $addition = "`n# FactBind：契约文件的位置（默认是 classpath:contracts/api.yaml）`nfactbind:`n  contract: file:../contracts/api.yaml`n"
        if (-not $DryRun) { [System.IO.File]::WriteAllText($yamlPath, $yaml + $addition, $encoding) }
        $changes.Add("配置：application.yml 追加 factbind.contract")
    }
}

$vitePath = Join-Path $root 'frontend/vite.config.ts'
if (Test-Path $vitePath) {
    $vite = [System.IO.File]::ReadAllText($vitePath)
    if ($vite -match 'fs:\s*\{') {
        $changes.Add("配置：vite.config.ts 已有 fs.allow，跳过")
    } else {
        $patched = $vite -replace '(server:\s*\{\r?\n)', "`$1    // 契约在仓库根，Vite 默认只允许读项目目录内的文件`n    fs: { allow: ['..'] },`n"
        if ($patched -eq $vite) { throw "vite.config.ts 里找不到 `server: {`，请手动加 fs.allow" }
        if (-not $DryRun) { [System.IO.File]::WriteAllText($vitePath, $patched, $encoding) }
        $changes.Add("配置：vite.config.ts 加 fs.allow")
    }
}

# ── 5. 前端依赖：契约是 YAML，前端需要解析器 ───────────────────────────────────
$pkgPath = Join-Path $root 'frontend/package.json'
if (Test-Path $pkgPath) {
    $pkg = [System.IO.File]::ReadAllText($pkgPath)
    if ($pkg -match '"yaml"\s*:') {
        $changes.Add("依赖：frontend/package.json 已有 yaml，跳过")
    } elseif ($DryRun) {
        $changes.Add("依赖：frontend 需要 yaml（会执行 npm install yaml）")
    } else {
        $npm = Get-Command npm.cmd -ErrorAction SilentlyContinue
        if ($null -eq $npm) {
            $npm = Get-Command npm -ErrorAction SilentlyContinue
        }
        if ($null -eq $npm) {
            $changes.Add("依赖：没找到 npm —— 请手动执行   cd frontend; npm install yaml")
        } else {
            Push-Location (Join-Path $root 'frontend')
            try {
                & $npm.Source install yaml 2>&1 | Out-Null
                if ($LASTEXITCODE -eq 0) {
                    $changes.Add("依赖：frontend 装好 yaml（契约解析用）")
                } else {
                    $changes.Add("依赖：npm install yaml 失败 —— 请手动执行   cd frontend; npm install yaml")
                }
            } finally {
                Pop-Location
            }
        }
    }
}

# ── 汇总 ─────────────────────────────────────────────────────────────────────
$prefix = if ($DryRun) { '[dry-run] ' } else { '' }
Write-Output "${prefix}FactBind 安装完成："
$changes | ForEach-Object { Write-Output "  - $_" }
Write-Output @"

接下来是"人的活"（脚本不碰业务代码）：
  1. 把 contracts/api.yaml 写对：每条 operation 一个 operationId，参数写清 name / in / required
  2. 后端控制器：@GetMapping("/{id}") → @FactBind("User.Get")，@PathVariable("id") → @FactBindParam
  3. 前端 api 层：request(``/api/users/`${id}``) → request('User.Get', { params: { id } })
  （异常类里的状态码可以先留着不动，想集中到契约时再去掉）

然后照常跑：
  cd backend  && ./mvnw spring-boot:run "-Dspring-boot.run.profiles=h2"
  cd frontend && npm install && npm run dev
"@
