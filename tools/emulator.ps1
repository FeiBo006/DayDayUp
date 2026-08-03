# 启动本项目用的 Android 模拟器。
#
#   powershell -ExecutionPolicy Bypass -File E:\do\tools\emulator.ps1
#
# 直接双击 gradlew 之类没法启动模拟器，用这个脚本；它会先按正常模式启动，
# 遇到 "too many emulator instances" 时自动清掉僵尸锁再重试。

$ErrorActionPreference = 'Stop'

$Sdk      = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$Emulator = Join-Path $Sdk 'emulator\emulator.exe'
$Adb      = Join-Path $Sdk 'platform-tools\adb.exe'
$AvdName  = 'Medium_Phone_API_36.1'
$AvdDir   = Join-Path $env:USERPROFILE '.android\avd\Medium_Phone.avd'

if (-not (Test-Path $Emulator)) { throw "找不到模拟器：$Emulator" }

# 已经有设备在线就不用再启动了。
$online = & $Adb devices | Select-String -Pattern '^emulator-\d+\s+device$'
if ($online) {
    Write-Host "模拟器已在运行：$($online.Line.Trim())"
    exit 0
}

# 上次异常退出会留下锁，导致误报 "too many emulator instances"。
foreach ($lock in 'hardware-qemu.ini.lock', 'multiinstance.lock') {
    $path = Join-Path $AvdDir $lock
    if (Test-Path $path) {
        Remove-Item $path -Recurse -Force -ErrorAction SilentlyContinue
        Write-Host "已清理僵尸锁：$lock"
    }
}

Write-Host "正在启动 $AvdName ..."
Start-Process -FilePath $Emulator `
    -ArgumentList '-avd', $AvdName, '-no-boot-anim', '-gpu', 'host'

# 等开机完成，最多 5 分钟。
& $Adb wait-for-device
for ($i = 0; $i -lt 100; $i++) {
    $booted = (& $Adb shell getprop sys.boot_completed 2>$null) -replace '\s', ''
    if ($booted -eq '1') {
        Write-Host "模拟器已就绪。"
        Write-Host "安装应用： $Adb install -r E:\do\app\build\outputs\apk\debug\app-debug.apk"
        exit 0
    }
    Start-Sleep -Seconds 3
}

Write-Warning "等待开机超时，去模拟器窗口看看卡在哪一步。"
exit 1
