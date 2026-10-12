#!/usr/bin/env python3
import argparse
import os
import re
import subprocess
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument('--serial', default='emulator-5554')
parser.add_argument('--port', default='5039')
parser.add_argument('--output', type=Path, default=Path('artifacts/device-checks'))
parser.add_argument('--test', action='append')
args = parser.parse_args()
sdk = Path(os.environ.get('ANDROID_HOME', Path.home() / '.local/share/tv-android/sdk'))
adb = [str(sdk / 'platform-tools/adb'), '-P', args.port, '-s', args.serial]
tests = args.test or [
    'com.github.tvbox.osc.SourceEngineCompatibilityTest',
    'com.github.tvbox.osc.modern.SourceAccessIntegrationTest',
    'com.github.tvbox.osc.modern.HomeRemoteTest',
    'com.github.tvbox.osc.modern.HomePlaybackSmokeTest',
    'com.github.tvbox.osc.modern.LibraryScreenTest',
    'com.github.tvbox.osc.modern.SearchScreenTest',
]
args.output.mkdir(parents=True, exist_ok=True)
for test in tests:
    result = subprocess.run(adb + ['shell', 'am', 'instrument', '-w', '-e', 'class', test,
        'com.coke.tv.test/androidx.test.runner.AndroidJUnitRunner'], text=True, capture_output=True, timeout=90)
    output = result.stdout + result.stderr
    (args.output / (test.rsplit('.', 1)[-1] + '.log')).write_text(output)
    passed = re.search(r'OK \((\d+) tests?\)', output)
    if result.returncode != 0 or passed is None or 'FAILURES!!!' in output:
        raise RuntimeError('Device check failed: ' + test + '\n' + output)
    print(test, ':', passed.group(0), flush=True)
