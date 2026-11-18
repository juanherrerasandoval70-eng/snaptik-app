param (
    [string]$VersionName = "2.1.0",
    [string]$Changelog = "Mejoras de velocidad en descargas Full HD y corrección de enlaces.",
    [string]$DownloadUrl = ""
)

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "    PREPARADOR DE NUEVA VERSIÓN - SNAPTIK PRO     " -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

$manifestPath = "AndroidManifest.xml"
[xml]$manifest = Get-Content $manifestPath

# 1. Leer y aumentar el versionCode actual
$currentCode = [int]$manifest.manifest.versionCode
$newCode = $currentCode + 1
$manifest.manifest.versionCode = $newCode.ToString()
$manifest.manifest.versionName = $VersionName

$manifest.Save($manifestPath)
Write-Host "[1/4] AndroidManifest.xml actualizado:" -ForegroundColor Green
Write-Host "      - versionCode anterior : $currentCode"
Write-Host "      - versionCode NUEVO    : $newCode"
Write-Host "      - versionName NUEVO    : $VersionName"

# 2. Compilar el APK con la nueva versión
Write-Host "`n[2/4] Compilando APK firmado v$VersionName..." -ForegroundColor Yellow
& powershell -ExecutionPolicy Bypass -File "build_apk.ps1"

$compiledApk = "dist\SnapTik_PRO_v1.0.apk"
$versionedApk = "dist\SnapTik_PRO_v$VersionName.apk"
if (Test-Path $compiledApk) {
    Copy-Item $compiledApk $versionedApk -Force
    Copy-Item $compiledApk "..\SnapTik_PRO_v$VersionName.apk" -Force
    Write-Host "`n[3/4] Archivo APK preparado: SnapTik_PRO_v$VersionName.apk" -ForegroundColor Green
} else {
    Write-Host "[ERROR] No se pudo encontrar el APK compilado." -ForegroundColor Red
    exit 1
}

# 3. Generar el archivo version.json listo para subir a la nube
if ([string]::IsNullOrWhiteSpace($DownloadUrl)) {
    $DownloadUrl = "https://github.com/juanherrerasandoval70-eng/snaptik-app/releases/download/v$VersionName/SnapTik_PRO_v$VersionName.apk"
}

$versionJson = @{
    versionCode = $newCode
    versionName = $VersionName
    apkUrl = $DownloadUrl
    changelog = $Changelog
    forceUpdate = $false
} | ConvertTo-Json -Depth 5

Set-Content -Path "version.json" -Value $versionJson -Encoding UTF8
Set-Content -Path "..\version.json" -Value $versionJson -Encoding UTF8

Write-Host "`n[4/4] Archivo version.json generado exitosamente!" -ForegroundColor Green
Write-Host "--------------------------------------------------" -ForegroundColor Gray
Write-Host $versionJson -ForegroundColor Yellow
Write-Host "--------------------------------------------------" -ForegroundColor Gray
Write-Host "`nLISTO. Solo subes 'SnapTik_PRO_v$VersionName.apk' y 'version.json' a tu servidor/GitHub." -ForegroundColor Cyan
