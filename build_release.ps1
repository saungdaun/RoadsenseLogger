# =======================================================
# Build & Validate Android Release APK/AAB - RoadsenseLogger
# =======================================================

# 1️⃣ Setup paths
$projectPath = "C:\Users\USER\StudioProjects\RoadsenseLogger"
$toolsPath = "C:\Users\USER\Tools"
$bundletoolUrl = "https://github.com/google/bundletool/releases/download/1.14.0/bundletool-all-1.14.0.jar"
$bundletoolJar = Join-Path $toolsPath "bundletool.jar"

# 2️⃣ Buat folder Tools kalau belum ada
if (-not (Test-Path $toolsPath)) {
    New-Item -ItemType Directory -Path $toolsPath
}

# 3️⃣ Download bundletool kalau belum ada
if (-not (Test-Path $bundletoolJar)) {
    Write-Host "Downloading bundletool..."
    Invoke-WebRequest -Uri $bundletoolUrl -OutFile $bundletoolJar
    Write-Host "Downloaded bundletool.jar ✅"
}

# 4️⃣ Masuk folder project
Set-Location $projectPath

# 5️⃣ Bersihkan project
Write-Host "Cleaning project..."
.\gradlew clean

# 6️⃣ Build release AAB & APK
Write-Host "Building release APK & AAB..."
.\gradlew assembleRelease
.\gradlew bundleRelease

# 7️⃣ Paths AAB & APK
$aabPath = Join-Path $projectPath "app\build\outputs\bundle\release\app-release.aab"
$apkPath = Join-Path $projectPath "app\build\outputs\apk\release\app-release.apk"

# 8️⃣ Validate AAB
Write-Host "Validating AAB..."
java -jar $bundletoolJar validate --bundle $aabPath

# 9️⃣ Optional: Build universal APK dari AAB untuk testing
$universalApkOutput = Join-Path $projectPath "app\build\outputs\bundle\release\universal.apks"
Write-Host "Building universal APK (optional)..."
java -jar $bundletoolJar build-apks --bundle $aabPath --output $universalApkOutput --mode=universal

Write-Host "`n✅ Build & validate complete!"
Write-Host "Release APK: $apkPath"
Write-Host "Release AAB: $aabPath"
Write-Host "Universal APK archive: $universalApkOutput"
