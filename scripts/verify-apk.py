#!/usr/bin/env python3
import argparse
import hashlib
import json
import os
import re
import subprocess
import zipfile
from pathlib import Path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path)
    parser.add_argument("--abi", required=True)
    parser.add_argument("--test-url-file", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    sdk = Path(os.environ.get("ANDROID_HOME", Path.home() / ".local/share/tv-android/sdk"))
    badging = subprocess.check_output([str(sdk / "build-tools/33.0.2/aapt"), "dump", "badging", str(args.apk)], text=True)
    assert "package: name='com.coke.tv'" in badging, "Unexpected package identity"
    assert "application-label:'TV'" in badging, "Unexpected application label"
    assert "sdkVersion:'28'" in badging, "Unexpected Android minimum"
    with zipfile.ZipFile(args.apk) as archive:
        names = archive.namelist()
        abis = sorted({name.split("/")[1] for name in names if name.startswith("lib/")})
        assert abis == [args.abi], abis
        libraries = [name.rsplit("/", 1)[-1] for name in names if name.startswith("lib/")]
        assert any("python3.8" in name for name in libraries), "Python engine missing"
        assert any("quickjs" in name.lower() or "qjs" in name.lower() for name in libraries), "QuickJS engine missing"
        preloaded_configs = []
        for name in names:
            if name.startswith("assets/") and name.endswith(".json"):
                try:
                    value = json.loads(archive.read(name).decode("utf-8-sig"))
                    if isinstance(value, dict) and (value.get("sites") or value.get("lives")):
                        preloaded_configs.append(name)
                except (ValueError, UnicodeDecodeError):
                    pass
        assert not preloaded_configs, preloaded_configs
        if args.test_url_file:
            test_url = args.test_url_file.read_text().strip().encode()
            for name in names:
                if name.startswith(("classes", "assets/", "res/raw/")):
                    assert test_url not in archive.read(name), "Test subscription embedded in APK: " + name
    report = {"package": "com.coke.tv", "label": "TV", "minimum_api": 28, "abis": abis, "python": True, "quickjs": True, "preloaded_source_configs": preloaded_configs, "bytes": args.apk.stat().st_size, "sha256": hashlib.sha256(args.apk.read_bytes()).hexdigest()}
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
