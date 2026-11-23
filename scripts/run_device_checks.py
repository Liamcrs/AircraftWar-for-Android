#!/usr/bin/env python3
"""Build/install and run real device checks. Requires a running emulator or USB-debug device."""
import argparse
import os
from pathlib import Path
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
sdk = Path(os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT') or Path.home() / 'Library/Android/sdk')
parser = argparse.ArgumentParser()
parser.add_argument('--serial', help='ADB device serial, required if multiple devices are connected')
parser.add_argument('--start-emulator', action='store_true', help='Start the first available AVD when no device is present')
args = parser.parse_args()
adb = [str(sdk / 'platform-tools/adb')]
if args.serial:
    adb.extend(['-s', args.serial])
subprocess.run([sys.executable, str(root / 'scripts/build_offline.py')], check=True)
subprocess.run([sys.executable, str(root / 'scripts/build_offline.py'), '--test'], check=True)
devices = subprocess.run([str(sdk / 'platform-tools/adb'), 'devices'], capture_output=True, text=True, check=True)
online = [line.split()[0] for line in devices.stdout.splitlines()[1:] if line.strip().endswith('\tdevice')]
if not online and args.start_emulator:
    avds = subprocess.check_output([str(sdk / 'emulator/emulator'), '-list-avds'], text=True).splitlines()
    if not avds:
        raise SystemExit('No AVD found. Create one in Android Studio Device Manager.')
    subprocess.Popen([str(sdk / 'emulator/emulator'), '-avd', avds[0]], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    subprocess.run(adb + ['wait-for-device'], check=True, timeout=180)
    import time
    deadline = time.monotonic() + 180
    while subprocess.check_output(adb + ['shell', 'getprop', 'sys.boot_completed'], text=True).strip() != '1':
        if time.monotonic() > deadline:
            raise SystemExit('Emulator boot timed out.')
        time.sleep(1)
elif not online:
    raise SystemExit('No online device. Start an emulator, enable USB debugging, or add --start-emulator.')
elif len(online) > 1 and not args.serial:
    raise SystemExit('More than one device: choose --serial ' + ' or '.join(online))
for apk in ['debug/app-debug.apk', 'androidTest/app-debug-androidTest.apk']:
    subprocess.run(adb + ['install', '-r', str(root / 'app/build/offline' / apk)], check=True)
run = subprocess.run(adb + ['shell', 'am', 'instrument', '-w',
                          'edu.hitsz.aircraftwar.test/edu.hitsz.test.GameInstrumentation'], capture_output=True, text=True)
report = run.stdout + run.stderr
out = root / 'app/build/device-checks.txt'
out.write_text(report)
print(report)
if run.returncode or 'RESULT: PASS' not in report:
    raise SystemExit('Device checks did not pass; see ' + str(out))
subprocess.run(adb + ['shell', 'am', 'start', '-n', 'edu.hitsz.aircraftwar/edu.hitsz.application.MainActivity'], check=True)
