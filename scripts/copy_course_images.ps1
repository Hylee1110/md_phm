# Copy 6 course cover images into static/photos (run in PowerShell).
$ErrorActionPreference = "Stop"
$Src = "C:\Users\31649\.cursor\projects\d-Code-md-phms\assets"
$Dst = Join-Path (Split-Path $PSScriptRoot -Parent) "static\photos"
$Names = @(
    "运动推荐-晨间快走.png",
    "运动推荐-舒缓拉伸.png",
    "运动推荐-基础核心训练.png",
    "运动推荐-室内单车入门.png",
    "运动推荐-慢跑耐力课.png",
    "运动推荐-徒手HIIT轻量.png"
)
New-Item -ItemType Directory -Force -Path $Dst | Out-Null
foreach ($n in $Names) {
    $from = Join-Path $Src $n
    if (-not (Test-Path -LiteralPath $from)) {
        Write-Host "MISSING: $from"
        continue
    }
    Copy-Item -LiteralPath $from -Destination (Join-Path $Dst $n) -Force
    Write-Host "OK: $n"
}
Write-Host "Done -> $Dst"
