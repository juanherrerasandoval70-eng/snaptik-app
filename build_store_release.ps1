$ErrorActionPreference = "Stop"

$BUILD_DIR = "C:\Users\Administrator\.gemini\antigravity\scratch\snaptik-apk-build"
$KIT_DIR = "C:\Users\Administrator\.gemini\antigravity\scratch\store_release_kit"
$KEYSTORE = "C:\Users\Administrator\.gemini\antigravity\scratch\snaptik-release-keys\snaptik_release.jks"
$KEY_PASS = "Sn@pT1kPr0#2026!"
$KEY_ALIAS = "snaptik_prod"

Set-Location $BUILD_DIR
New-Item -ItemType Directory -Force -Path $KIT_DIR | Out-Null

$AAPT2 = "C:\Android\build-tools\34.0.0\aapt2.exe"
$D8 = "C:\Android\build-tools\34.0.0\d8.bat"
$ZIPALIGN = "C:\Android\build-tools\34.0.0\zipalign.exe"
$APKSIGNER = "C:\Android\build-tools\34.0.0\apksigner.bat"
$ANDROID_JAR = "C:\Android\platforms\android-34\android.jar"
$JAVAC = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot\bin\javac.exe"

Write-Host "=== COMPILANDO APK RELEASE BLINDADO PARA TIENDAS OFICIALES ===" -ForegroundColor Cyan

# Limpiar directorios temporales
$dirs = @("build", "gen", "bin", "dex")
foreach ($d in $dirs) {
    if (Test-Path $d) { Remove-Item -Recurse -Force $d }
    New-Item -ItemType Directory -Force -Path $d | Out-Null
}

# Generar AndroidManifest limpio para tiendas con Package ID único libre de conflictos (com.snaptikpro.hd.app)
$manifestRaw = Get-Content "AndroidManifest.xml" -Raw
$storeManifest = $manifestRaw -replace '<uses-permission android:name="android\.permission\.REQUEST_INSTALL_PACKAGES"\s*/>', ''
$storeManifest = $storeManifest -replace 'android:usesCleartextTraffic="true"', 'android:usesCleartextTraffic="false" android:debuggable="false" android:networkSecurityConfig="@xml/network_security_config"'
$storeManifest = $storeManifest -replace 'android:allowBackup="true"', 'android:allowBackup="false"'
$storeManifest = $storeManifest -replace 'package="com\.snaptik\.app"', 'package="com.snaptikpro.hd.app"'
$storeManifest = $storeManifest -replace 'android:authorities="com\.snaptik\.app\.', 'android:authorities="com.snaptikpro.hd.app.'
$storeManifest = $storeManifest -replace 'android:name="\.MainActivity"', 'android:name="com.snaptik.app.MainActivity"'
$storeManifest = $storeManifest -replace 'android:name="\.AppUpdateFileProvider"', 'android:name="com.snaptik.app.AppUpdateFileProvider"'
$storeManifestPath = "build\AndroidManifest_store.xml"
Set-Content -Path $storeManifestPath -Value $storeManifest -Encoding UTF8

# 1. Compilar recursos
& $AAPT2 compile --dir res -o build\compiled_res.zip

# 2. Enlazar con manifest limpio de tiendas (--custom-package com.snaptik.app para mantener R.java intacto)
& $AAPT2 link -I $ANDROID_JAR --manifest $storeManifestPath --custom-package com.snaptik.app --java gen -o build\base.apk build\compiled_res.zip --auto-add-overlay

# 3. Compilar Java sin símbolos de depuración (-g:none anti-ingeniería inversa)
$javaFiles = (Get-ChildItem -Path "gen\*.java", "src\*.java" -Recurse).FullName
$libJars = @(Get-ChildItem -Path "libs\*.jar" -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName)
$classpath = "$ANDROID_JAR;" + ($libJars -join ";")
& $JAVAC -source 8 -target 8 -g:none -encoding UTF-8 -bootclasspath $ANDROID_JAR -cp "$classpath" -d bin $javaFiles

# 4. Dexificar en modo --release (optimizado y sin debug info)
$classFiles = (Get-ChildItem -Path "bin\*.class" -Recurse).FullName
$d8Inputs = ($classFiles + $libJars) -join " "
& cmd.exe /c "$D8 --release --min-api 21 --lib `"$ANDROID_JAR`" --output dex $d8Inputs"

# 5. Empaquetar
python -c "
import zipfile, os
with zipfile.ZipFile(r'build\base.apk', 'r') as zin, zipfile.ZipFile(r'build\unaligned_store.apk', 'w', compression=zipfile.ZIP_DEFLATED) as zout:
    for item in zin.infolist():
        zout.writestr(item, zin.read(item.filename))
    for f in os.listdir('dex'):
        if f.endswith('.dex'):
            zout.write(os.path.join('dex', f), f)
    for root, dirs, files in os.walk('assets'):
        for f in files:
            fp = os.path.join(root, f)
            zout.write(fp, fp.replace('\\', '/'))
"

# 6. Alinear y Firmar con Release Keystore Privada (25 años)
$alignedApk = "build\SnapTik_PRO_store_aligned.apk"
$finalStoreApk = "$KIT_DIR\SnapTik_PRO_STORE_v3.7.0_RELEASE.apk"
$legacyStoreApk = "$KIT_DIR\SnapTik_PRO_STORE_v3.6.0_RELEASE.apk"

& $ZIPALIGN -f -p 4 build\unaligned_store.apk $alignedApk
& cmd.exe /c "$APKSIGNER sign --ks `"$KEYSTORE`" --ks-pass `"pass:$KEY_PASS`" --ks-key-alias $KEY_ALIAS --key-pass `"pass:$KEY_PASS`" --out `"$finalStoreApk`" `"$alignedApk`""
Copy-Item -Force $finalStoreApk $legacyStoreApk
& cmd.exe /c "$APKSIGNER verify --verbose `"$finalStoreApk`""

Write-Host "`n[EXITO] APK RELEASE PARA TIENDAS LISTO: $finalStoreApk" -ForegroundColor Green
