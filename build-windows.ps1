param([string[]]$Tasks = @(':app:testDebugUnitTest', ':app:lintDebug', ':app:assembleDebug'))
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = 'E:\IntelliJ IDEA Community Edition 2024.3.5\jbr'
$env:GRADLE_USER_HOME = Join-Path $PSScriptRoot '.gradle'
$env:ANDROID_USER_HOME = Join-Path $PSScriptRoot '.android-home'
$env:ANDROID_HOME = 'E:\Class schedule\.android-sdk'
Remove-Item Env:ANDROID_SDK_HOME -ErrorAction SilentlyContinue
Remove-Item Env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue
$env:JAVA_TOOL_OPTIONS = '-Duser.home=E:/Class schedule'
New-Item -ItemType Directory -Force -Path $env:ANDROID_USER_HOME | Out-Null
Push-Location $PSScriptRoot
try {
    & .\gradlew.bat @Tasks --console=plain
    exit $LASTEXITCODE
} finally { Pop-Location }
