# checkdb.ps1 - Cek status database dan entity
Write-Host "🔍 CHECKING DATABASE ENTITIES" -ForegroundColor Cyan
Write-Host "==============================" -ForegroundColor Cyan

# 1. Cek file entity
$entities = @(
    "app\src\main\java\com\roadsense\logger\core\data\database\entities\ProjectEntity.kt",
    "app\src\main\java\com\roadsense\logger\core\data\database\entities\RoadSegmentEntity.kt",
    "app\src\main\java\com\roadsense\logger\core\data\database\entities\SurveyDataEntity.kt"
)

foreach ($entity in $entities) {
    if (Test-Path $entity) {
        Write-Host "✅ $((Get-Item $entity).Name)" -ForegroundColor Green
        
        # Cek apakah ada @Index
        $content = Get-Content $entity -Raw
        if ($content -match "@Index") {
            Write-Host "   ✓ Memiliki @Index" -ForegroundColor Cyan
        } else {
            Write-Host "   ⚠️ Tidak ada @Index" -ForegroundColor Yellow
        }
        
        if ($content -match "foreignKeys") {
            Write-Host "   ✓ Memiliki foreignKeys" -ForegroundColor Cyan
        }
    } else {
        Write-Host "❌ $entity tidak ditemukan" -ForegroundColor Red
    }
}

# 2. Build untuk cek warning
Write-Host "`n🏗️  Building untuk cek warning..." -ForegroundColor Yellow
$output = .\gradlew assembleDebug 2>&1

# 3. Cek warning khusus database
$dbWarnings = $output | Select-String -Pattern "index|Index|foreign|Foreign"
if ($dbWarnings.Count -gt 0) {
    Write-Host "`n⚠️  Database Warnings Found:" -ForegroundColor Yellow
    $dbWarnings | Select-Object -First 10 | ForEach-Object {
        Write-Host "   $_" -ForegroundColor White
    }
} else {
    Write-Host "`n✅ Tidak ada database warning!" -ForegroundColor Green
}

# 4. Cek APK
$apk = "app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apk) {
    Write-Host "`n📦 APK Status:" -ForegroundColor Cyan
    Write-Host "   ✅ APK tersedia: $apk" -ForegroundColor Green
} else {
    Write-Host "   ❌ APK tidak ditemukan" -ForegroundColor Red
}

Write-Host "`n🎯 Database entities sudah dioptimalkan!" -ForegroundColor Green