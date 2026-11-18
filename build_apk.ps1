$ErrorActionPreference = "Stop"

$BUILD_DIR = "C:\Users\Administrator\.gemini\antigravity\scratch\snaptik-apk-build"
Set-Location $BUILD_DIR

$AAPT2 = "C:\Android\build-tools\34.0.0\aapt2.exe"
$D8 = "C:\Android\build-tools\34.0.0\d8.bat"
$ZIPALIGN = "C:\Android\build-tools\34.0.0\zipalign.exe"
$APKSIGNER = "C:\Android\build-tools\34.0.0\apksigner.bat"
$ANDROID_JAR = "C:\Android\platforms\android-34\android.jar"
$JAVAC = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot\bin\javac.exe"
$KEYSTORE = "C:\tools\re-arsenal\debug.keystore"

Write-Host "=== INICIANDO COMPILACIÓN DE APK NATIVO (SnapTik PRO) ===" -ForegroundColor Cyan

# Limpiar directorios de trabajo
$dirs = @("build", "gen", "bin", "dex", "dist")
foreach ($d in $dirs) {
    if (Test-Path $d) { Remove-Item -Recurse -Force $d }
    New-Item -ItemType Directory -Force -Path $d | Out-Null
}

# 1. Compilar recursos con AAPT2
Write-Host "[1/6] Compilando recursos con aapt2..." -ForegroundColor Yellow
& $AAPT2 compile --dir res -o build\compiled_res.zip

# 2. Enlazar recursos y generar R.java
Write-Host "[2/6] Enlazando recursos y generando R.java..." -ForegroundColor Yellow
& $AAPT2 link -I $ANDROID_JAR --manifest AndroidManifest.xml --java gen -o build\base.apk build\compiled_res.zip --auto-add-overlay

# 3. Compilar fuentes Java con javac (target 8)
Write-Host "[3/6] Compilando archivos Java con javac..." -ForegroundColor Yellow
$javaFiles = (Get-ChildItem -Path "gen\*.java", "src\*.java" -Recurse).FullName
$libJars = @(Get-ChildItem -Path "libs\*.jar" -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName)
if ($libJars.Count -gt 0) {
    $classpath = "$ANDROID_JAR;" + ($libJars -join ";")
    $d8Inputs = ($classFiles + $libJars) -join " "
} else {
    $classpath = "$ANDROID_JAR"
    $d8Inputs = $classFiles -join " "
}
& $JAVAC -source 8 -target 8 -g -encoding UTF-8 -bootclasspath $ANDROID_JAR -cp "$classpath" -d bin $javaFiles

# 4. Convertir .class y libs a classes.dex con d8
Write-Host "[4/6] Desensamblando y dexificando con d8..." -ForegroundColor Yellow
$classFiles = (Get-ChildItem -Path "bin\*.class" -Recurse).FullName
if ($libJars.Count -gt 0) {
    $d8Inputs = ($classFiles + $libJars) -join " "
} else {
    $d8Inputs = $classFiles -join " "
}
& cmd.exe /c "$D8 --min-api 21 --lib `"$ANDROID_JAR`" --output dex $d8Inputs"

# 5. Empaquetar classes.dex y assets en unaligned.apk
Write-Host "[5/6] Empaquetando classes.dex y assets en el APK..." -ForegroundColor Yellow
python -c "
import zipfile, os

base_apk = r'build\base.apk'
unaligned_apk = r'build\unaligned.apk'

with zipfile.ZipFile(base_apk, 'r') as zin, zipfile.ZipFile(unaligned_apk, 'w', compression=zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        zout.writestr(item, zin.read(item.filename))
    
    # Agregar todos los classes*.dex
    if os.path.exists('dex'):
        for f in os.listdir('dex'):
            if f.endswith('.dex'):
                zout.write(os.path.join('dex', f), f)
    
    # Agregar assets/
    for root, dirs, files in os.walk('assets'):
        for f in files:
            full_path = os.path.join(root, f)
            arcname = full_path.replace('\\', '/')
            zout.write(full_path, arcname)

print('Empaquetado completado.')
"

# 6. Alinear y Firmar APK
Write-Host "[6/6] Zipalign y Firma criptográfica (v1, v2, v3)..." -ForegroundColor Yellow
$alignedApk = "build\SnapTik_PRO_aligned.apk"
$finalApk = "dist\SnapTik_PRO_v1.0.apk"

& $ZIPALIGN -f -p 4 build\unaligned.apk $alignedApk
& cmd.exe /c "$APKSIGNER sign --ks `"$KEYSTORE`" --ks-pass pass:android --ks-key-alias androiddebugkey --out `"$finalApk`" `"$alignedApk`""

# Verificar firma
Write-Host "`n=== VERIFICACIÓN DE FIRMA DEL APK ===" -ForegroundColor Green
& cmd.exe /c "$APKSIGNER verify --verbose `"$finalApk`""

$apkSize = (Get-Item $finalApk).Length / 1MB
Write-Host "`n[EXITO] APK construido exitosamente: $finalApk ($([math]::Round($apkSize, 2)) MB)" -ForegroundColor Green
