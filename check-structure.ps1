# =============================================
# CEK STRUKTUR FILE YANG ADA SAAT INI
# =============================================

Write-Host "=== STRUKTUR FILE YANG ADA ===" -ForegroundColor Cyan
Write-Host "==============================" -ForegroundColor Cyan

# 1. CEK FOLDER MODELS
Write-Host "`n1. FOLDER MODELS:" -ForegroundColor Yellow
$modelsPath = "app\src\main\java\com\roadsense\logger\core\data\models"
if (Test-Path $modelsPath) {
    $files = Get-ChildItem $modelsPath -File
    if ($files.Count -eq 0) {
        Write-Host "   [WARN] Folder models kosong!" -ForegroundColor Yellow
    } else {
        foreach ($file in $files) {
            $status = if ($file.Name -eq "ModelExtensions.kt") { "[OK]" } else { "[DELETE]" }
            $color = if ($file.Name -eq "ModelExtensions.kt") { "Green" } else { "Red" }
            Write-Host "   $status $($file.Name)" -ForegroundColor $color
        }
    }
} else {
    Write-Host "   [ERROR] Folder models tidak ada!" -ForegroundColor Red
}

# 2. CEK FOLDER ENTITIES
Write-Host "`n2. FOLDER ENTITIES:" -ForegroundColor Yellow
$entitiesPath = "app\src\main\java\com\roadsense\logger\core\data\database\entities"
if (Test-Path $entitiesPath) {
    Get-ChildItem $entitiesPath -File | ForEach-Object {
        Write-Host "   [OK] $($_.Name)" -ForegroundColor Green
    }
} else {
    Write-Host "   [ERROR] Folder entities tidak ada!" -ForegroundColor Red
}

# 3. CEK FOLDER DAO
Write-Host "`n3. FOLDER DAO:" -ForegroundColor Yellow
$daoPath = "app\src\main\java\com\roadsense\logger\core\data\database\dao"
if (Test-Path $daoPath) {
    Get-ChildItem $daoPath -File | ForEach-Object {
        Write-Host "   [OK] $($_.Name)" -ForegroundColor Green
    }
} else {
    Write-Host "   [ERROR] Folder dao tidak ada!" -ForegroundColor Red
}

# 4. CEK FOLDER REPOSITORY
Write-Host "`n4. FOLDER REPOSITORY:" -ForegroundColor Yellow
$repoPath = "app\src\main\java\com\roadsense\logger\core\data\repository"
if (Test-Path $repoPath) {
    Get-ChildItem $repoPath -File | ForEach-Object {
        Write-Host "   [OK] $($_.Name)" -ForegroundColor Green
    }
} else {
    Write-Host "   [ERROR] Folder repository tidak ada!" -ForegroundColor Red
}

# 5. CEK FOLDER BLUETOOTH
Write-Host "`n5. FOLDER BLUETOOTH:" -ForegroundColor Yellow
$btPath = "app\src\main\java\com\roadsense\logger\core\bluetooth"
if (Test-Path $btPath) {
    Get-ChildItem $btPath -File | ForEach-Object {
        Write-Host "   [OK] $($_.Name)" -ForegroundColor Green
    }
} else {
    Write-Host "   [ERROR] Folder bluetooth tidak ada!" -ForegroundColor Red
}

# 6. CEK FOLDER VIEWMODELS
Write-Host "`n6. FOLDER VIEWMODELS:" -ForegroundColor Yellow
$vmPath = "app\src\main\java\com\roadsense\logger\ui\viewmodels"
if (Test-Path $vmPath) {
    Get-ChildItem $vmPath -File | ForEach-Object {
        Write-Host "   [OK] $($_.Name)" -ForegroundColor Green
    }
} else {
    Write-Host "   [ERROR] Folder viewmodels tidak ada!" -ForegroundColor Red
}

# 7. CEK FOLDER HOME (FRAGMENT)
Write-Host "`n7. FOLDER HOME:" -ForegroundColor Yellow
$homePath = "app\src\main\java\com\roadsense\logger\ui\home"
if (Test-Path $homePath) {
    Get-ChildItem $homePath -File | ForEach-Object {
        Write-Host "   [OK] $($_.Name)" -ForegroundColor Green
    }
} else {
    Write-Host "   [ERROR] Folder home tidak ada!" -ForegroundColor Red
}

# =============================================
# CEK DUPLICATE CLASS NAMES DI SELURUH PROYEK
# =============================================

Write-Host "`n`n=== CEK DUPLICATE CLASS NAMES ===" -ForegroundColor Cyan
Write-Host "==================================" -ForegroundColor Cyan

# Cari semua data class dan class di seluruh proyek dengan regex yang lebih akurat
$allClasses = @{}
Get-ChildItem -Path "app\src\main\java" -Recurse -Filter "*.kt" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    # Cari semua class declarations menggunakan Regex.Matches
    $matches = [regex]::Matches($content, "(?:data\s+class|class)\s+(\w+)")
    foreach ($match in $matches) {
        $className = $match.Groups[1].Value
        if ($allClasses.ContainsKey($className)) {
            $allClasses[$className] += @($_.FullName)
        } else {
            $allClasses[$className] = @($_.FullName)
        }
    }
}

# Tampilkan yang duplicate
$hasDuplicates = $false
foreach ($className in $allClasses.Keys) {
    if ($allClasses[$className].Count -gt 1) {
        $hasDuplicates = $true
        Write-Host "`n[DUPLICATE] $className ($($allClasses[$className].Count) ditemukan)" -ForegroundColor Red
        $allClasses[$className] | ForEach-Object {
            Write-Host "   - $_" -ForegroundColor Yellow
        }
    }
}

if (-not $hasDuplicates) {
    Write-Host "`n[OK] Tidak ada duplicate class names" -ForegroundColor Green
}

# =============================================
# CEK FILE YANG PERLU DIBUAT/JANGAN ADA
# =============================================

Write-Host "`n`n=== FILE YANG HARUS ADA ===" -ForegroundColor Cyan
Write-Host "==============================" -ForegroundColor Cyan

$requiredFiles = @(
    # Core
    "app\src\main\java\com\roadsense\logger\MainActivity.kt",
    "app\src\main\java\com\roadsense\logger\RoadsenseApplication.kt",
    
    # Bluetooth
    "app\src\main\java\com\roadsense\logger\core\bluetooth\BluetoothHandler.kt",
    "app\src\main\java\com\roadsense\logger\core\bluetooth\BluetoothCallback.kt",
    "app\src\main\java\com\roadsense\logger\core\bluetooth\RoadsenseData.kt",
    
    # Database Entities
    "app\src\main\java\com\roadsense\logger\core\data\database\entities\ProjectEntity.kt",
    "app\src\main\java\com\roadsense\logger\core\data\database\entities\RoadSegmentEntity.kt",
    "app\src\main\java\com\roadsense\logger\core\data\database\entities\SurveyDataEntity.kt",
    
    # Database DAO
    "app\src\main\java\com\roadsense\logger\core\data\database\dao\ProjectDao.kt",
    "app\src\main\java\com\roadsense\logger\core\data\database\dao\SegmentDao.kt",
    "app\src\main\java\com\roadsense\logger\core\data\database\dao\SurveyDataDao.kt",
    
    # Models (HANYA SATU FILE!)
    "app\src\main\java\com\roadsense\logger\core\data\models\ModelExtensions.kt",
    
    # Repository
    "app\src\main\java\com\roadsense\logger\core\data\repository\RoadsenseRepository.kt",
    
    # ViewModels
    "app\src\main\java\com\roadsense\logger\ui\viewmodels\SharedViewModel.kt",
    
    # Home Fragment
    "app\src\main\java\com\roadsense\logger\ui\home\HomeFragment.kt"
)

Write-Host "`nFile yang harus ada (checklist):" -ForegroundColor Yellow
foreach ($file in $requiredFiles) {
    if (Test-Path $file) {
        Write-Host "   [OK] $($file.Split('\')[-1])" -ForegroundColor Green
    } else {
        Write-Host "   [MISSING] $($file.Split('\')[-1])" -ForegroundColor Red
    }
}

# =============================================
# HAPUS FILE YANG TIDAK PERLU
# =============================================

Write-Host "`n`n=== HAPUS FILE TIDAK PERLU ===" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan

$filesToDelete = @(
    # Hapus semua file di models kecuali ModelExtensions.kt
    "app\src\main\java\com\roadsense\logger\core\data\models\Project.kt",
    "app\src\main\java\com\roadsense\logger\core\data\models\SurveyData.kt",
    "app\src\main\java\com\roadsense\logger\core\data\models\RoadSegment.kt",
    "app\src\main\java\com\roadsense\logger\core\data\models\ModelExtension.kt",
    "app\src\main\java\com\roadsense\logger\core\data\models\Extensions.kt",
    
    # Hapus jika ada SharedBluetoothViewModel
    "app\src\main\java\com\roadsense\logger\viewmodels\SharedBluetoothViewModel.kt",
    "app\src\main\java\com\roadsense\logger\ui\viewmodels\SharedBluetoothViewModel.kt"
)

$deletedCount = 0
$failedCount = 0

Write-Host "`nFile yang akan dihapus:" -ForegroundColor Yellow
foreach ($file in $filesToDelete) {
    if (Test-Path $file) {
        Write-Host "   - $($file.Split('\')[-1])" -ForegroundColor Gray
    }
}

Write-Host "`nProses penghapusan:" -ForegroundColor Yellow
foreach ($file in $filesToDelete) {
    if (Test-Path $file) {
        try {
            Remove-Item $file -Force
            Write-Host "   [DELETED] $($file.Split('\')[-1])" -ForegroundColor Gray
            $deletedCount++
        }
        catch {
            Write-Host "   [FAILED] $($file.Split('\')[-1]) - $($_.Exception.Message)" -ForegroundColor Red
            $failedCount++
        }
    }
}

if ($deletedCount -eq 0 -and $failedCount -eq 0) {
    Write-Host "   [INFO] Tidak ada file yang perlu dihapus" -ForegroundColor Cyan
} else {
    Write-Host "`n   Ringkasan penghapusan:" -ForegroundColor White
    Write-Host "   - Berhasil dihapus: $deletedCount" -ForegroundColor Green
    if ($failedCount -gt 0) {
        Write-Host "   - Gagal dihapus: $failedCount" -ForegroundColor Red
    }
}

# =============================================
# STRUKTUR IDEAL YANG HARUS ADA
# =============================================

Write-Host "`n`n=== STRUKTUR IDEAL ===" -ForegroundColor Cyan
Write-Host "=======================" -ForegroundColor Cyan

$idealStructure = @"
com/roadsense/logger/
├── MainActivity.kt
├── RoadsenseApplication.kt
├── core/
│   ├── bluetooth/
│   │   ├── BluetoothHandler.kt
│   │   ├── BluetoothCallback.kt (interface)
│   │   └── RoadsenseData.kt (data class)
│   └── data/
│       ├── database/
│       │   ├── entities/
│       │   │   ├── ProjectEntity.kt
│       │   │   ├── RoadSegmentEntity.kt
│       │   │   └── SurveyDataEntity.kt
│       │   ├── dao/
│       │   │   ├── ProjectDao.kt
│       │   │   ├── SegmentDao.kt
│       │   │   └── SurveyDataDao.kt
│       │   └── RoadsenseDatabase.kt (jika ada)
│       ├── models/
│       │   └── ModelExtensions.kt (HANYA INI!)
│       └── repository/
│           └── RoadsenseRepository.kt
└── ui/
    ├── home/
    │   └── HomeFragment.kt
    └── viewmodels/
        └── SharedViewModel.kt
"@

Write-Host $idealStructure -ForegroundColor White

# =============================================
# REKOMENDASI PERBAIKAN
# =============================================

Write-Host "`n`n=== REKOMENDASI PERBAIKAN ===" -ForegroundColor Cyan
Write-Host "===============================" -ForegroundColor Cyan

# Cek apakah ada SharedBluetoothViewModel yang harus dihapus
$bluetoothVM1 = "app\src\main\java\com\roadsense\logger\viewmodels\SharedBluetoothViewModel.kt"
$bluetoothVM2 = "app\src\main\java\com\roadsense\logger\ui\viewmodels\SharedBluetoothViewModel.kt"
if (Test-Path $bluetoothVM1 -or Test-Path $bluetoothVM2) {
    Write-Host "1. [ISSUE] SharedBluetoothViewModel.kt ditemukan" -ForegroundColor Red
    Write-Host "   Rekomendasi: Hapus file ini dan gunakan SharedViewModel.kt saja untuk semua data termasuk bluetooth" -ForegroundColor Yellow
} else {
    Write-Host "1. [OK] SharedBluetoothViewModel.kt tidak ditemukan (sudah benar)" -ForegroundColor Green
}

# Cek ModelExtensions.kt
$modelExt = "app\src\main\java\com\roadsense\logger\core\data\models\ModelExtensions.kt"
if (Test-Path $modelExt) {
    try {
        $content = Get-Content $modelExt -Raw -Encoding UTF8
        if ($content -match "data class Project\(" -and $content -match "projectName: String") {
            Write-Host "2. [OK] ModelExtensions.kt sudah benar (menggunakan 'projectName')" -ForegroundColor Green
        } elseif ($content -match "data class Project\(" -and $content -match "name: String") {
            Write-Host "2. [WARN] ModelExtensions.kt perlu diperbaiki" -ForegroundColor Yellow
            Write-Host "   Rekomendasi: Ubah properti 'name' menjadi 'projectName' pada data class Project" -ForegroundColor Yellow
        } else {
            Write-Host "2. [WARN] ModelExtensions.kt tidak mengandung data class Project" -ForegroundColor Yellow
            Write-Host "   Rekomendasi: Pastikan ada data class Project dengan properti projectName" -ForegroundColor Yellow
        }
    }
    catch {
        Write-Host "2. [ERROR] Gagal membaca ModelExtensions.kt: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "2. [MISSING] ModelExtensions.kt tidak ditemukan" -ForegroundColor Red
}

# Cek apakah HomeFragment menggunakan SharedViewModel
$homeFragment = "app\src\main\java\com\roadsense\logger\ui\home\HomeFragment.kt"
if (Test-Path $homeFragment) {
    try {
        $content = Get-Content $homeFragment -Raw -Encoding UTF8
        if ($content -match "SharedViewModel" -and $content -match "activityViewModels") {
            Write-Host "3. [OK] HomeFragment menggunakan SharedViewModel dengan benar" -ForegroundColor Green
        } else {
            Write-Host "3. [WARN] HomeFragment perlu update ke SharedViewModel" -ForegroundColor Yellow
            Write-Host "   Rekomendasi: Pastikan menggunakan: by activityViewModels<SharedViewModel>()" -ForegroundColor Yellow
        }
    }
    catch {
        Write-Host "3. [ERROR] Gagal membaca HomeFragment.kt: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "3. [MISSING] HomeFragment.kt tidak ditemukan" -ForegroundColor Red
}

# =============================================
# PETUNJUK SELANJUTNYA
# =============================================

Write-Host "`n=== PETUNJUK SELANJUTNYA ===" -ForegroundColor Cyan
Write-Host "=============================" -ForegroundColor Cyan

Write-Host "`n[IMPORTANT] LANGKAH-LANGKAH:" -ForegroundColor Yellow

# Periksa apakah ada issue yang perlu diperbaiki
$hasIssues = $false
$issues = @()

# Cek issue dari rekomendasi sebelumnya
if (Test-Path $bluetoothVM1 -or Test-Path $bluetoothVM2) {
    $hasIssues = $true
    $issues += "- Hapus SharedBluetoothViewModel.kt"
}

if (Test-Path $modelExt) {
    $content = Get-Content $modelExt -Raw -Encoding UTF8 -ErrorAction SilentlyContinue
    if ($content -and $content -match "data class Project\(" -and $content -match "name: String" -and !($content -match "projectName: String")) {
        $hasIssues = $true
        $issues += "- Ubah properti 'name' menjadi 'projectName' di ModelExtensions.kt"
    }
} else {
    $hasIssues = $true
    $issues += "- Buat file ModelExtensions.kt di folder models"
}

if (Test-Path $homeFragment) {
    $content = Get-Content $homeFragment -Raw -Encoding UTF8 -ErrorAction SilentlyContinue
    if ($content -and !($content -match "SharedViewModel" -and $content -match "activityViewModels")) {
        $hasIssues = $true
        $issues += "- Update HomeFragment untuk menggunakan SharedViewModel"
    }
} else {
    $hasIssues = $true
    $issues += "- Buat HomeFragment.kt di folder ui/home"
}

if ($hasIssues) {
    Write-Host "`n[PERINGATAN] Ada issue yang perlu diperbaiki sebelum build:" -ForegroundColor Red
    foreach ($issue in $issues) {
        Write-Host "  $issue" -ForegroundColor Yellow
    }
    Write-Host "`nJANGAN BUILD PROYEK sebelum memperbaiki issue di atas!" -ForegroundColor Red
} else {
    Write-Host "`n[SUKSES] Semua struktur sudah benar. Siap untuk build!" -ForegroundColor Green
}

Write-Host "`n[INSTRUKSI BUILD] Setelah semua issue diperbaiki:" -ForegroundColor Cyan
Write-Host "1. Buka terminal di folder proyek" -ForegroundColor White
Write-Host "2. Jalankan: .\gradlew clean" -ForegroundColor White
Write-Host "3. Jalankan: .\gradlew assembleDebug" -ForegroundColor White
Write-Host "4. Atau untuk build lengkap: .\gradlew build" -ForegroundColor White

Write-Host "`n=== AUDIT STRUKTUR SELESAI ===" -ForegroundColor Cyan