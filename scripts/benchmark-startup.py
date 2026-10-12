#!/usr/bin/env python3
import argparse
import json
import os
import re
import subprocess
import time
from pathlib import Path

parser = argparse.ArgumentParser()
parser.add_argument('--output', type=Path, required=True)
parser.add_argument('--serial', default='emulator-5554')
parser.add_argument('--port', default='5039')
parser.add_argument('--runs', type=int, default=5)
args = parser.parse_args()
sdk = Path(os.environ.get('ANDROID_HOME', Path.home() / '.local/share/tv-android/sdk'))
adb = [str(sdk / 'platform-tools/adb'), '-P', args.port, '-s', args.serial]

def run(*command):
    return subprocess.check_output(adb + list(command), text=True)

samples = []
for index in range(args.runs):
    run('shell', 'am', 'force-stop', 'com.coke.tv')
    launch = run('shell', 'am', 'start', '-W', '-n', 'com.coke.tv/com.github.tvbox.osc.ui.activity.HomeActivity')
    elapsed = re.search(r'TotalTime:\s*(\d+)', launch)
    if elapsed is None:
        raise RuntimeError('Activity cold start did not return TotalTime: ' + launch)
    time.sleep(5)
    memory = run('shell', 'dumpsys', 'meminfo', 'com.coke.tv')
    pss = re.search(r'TOTAL PSS:\s*(\d+)', memory)
    if pss is None:
        raise RuntimeError('Process has no PSS sample')
    sample = {'total_time_ms': int(elapsed.group(1)), 'steady_pss_kb': int(pss.group(1))}
    samples.append(sample)
    print('Sample', index + 1, sample, flush=True)
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps(samples, indent=2) + '\n')
