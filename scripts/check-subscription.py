#!/usr/bin/env python3
import argparse
import collections
import json
import time
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path


def fetch(url, params=None, headers=None):
    parts = urllib.parse.urlsplit(url)
    query = urllib.parse.parse_qsl(parts.query, keep_blank_values=True)
    query.extend((params or {}).items())
    target = urllib.parse.urlunsplit(parts._replace(query=urllib.parse.urlencode(query)))
    request_headers = {"User-Agent": "okhttp/3.15"}
    request_headers.update(headers or {})
    request = urllib.request.Request(target, headers=request_headers)
    with urllib.request.urlopen(request, timeout=20) as response:
        return response.status, response.read(5 * 1024 * 1024)


def parse(data, kind):
    if kind == 0:
        root = ET.fromstring(data)
        videos = root.findall(".//list/video")
        return len(root.findall(".//class/ty")), [video.findtext("id") for video in videos]
    value = json.loads(data.decode("utf-8-sig"))
    return len(value.get("class", [])), [str(video.get("vod_id", "")) for video in value.get("list", [])]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--url-file", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    url = args.url_file.read_text().strip()
    _, data = fetch(url)
    config = json.loads(data.decode("utf-8-sig"))
    sites = config.get("sites", [])
    report = {"site_count": len(sites), "types": dict(collections.Counter(str(site.get("type")) for site in sites)), "has_spider": bool(config.get("spider")), "checks": []}
    candidates = [site for site in sites if site.get("type") in (0, 1) and str(site.get("api", "")).startswith(("https://", "http://")) and ".js" not in str(site.get("api", ""))][:4]
    for site in candidates:
        entry = {"name": site.get("name"), "type": site["type"], "host": urllib.parse.urlsplit(site["api"]).hostname}
        start = time.monotonic()
        try:
            headers = site.get("header") if isinstance(site.get("header"), dict) else None
            status, home = fetch(site["api"], headers=headers)
            categories, videos = parse(home, site["type"])
            entry.update(status=status, categories=categories, home_items=len(videos))
            action = "videolist" if site["type"] == 0 else "detail"
            _, listing = fetch(site["api"], {"ac": action, "pg": "1"}, headers)
            _, ids = parse(listing, site["type"])
            entry["list_items"] = len(ids)
            if ids:
                _, detail = fetch(site["api"], {"ac": action, "ids": ids[0]}, headers)
                _, detail_ids = parse(detail, site["type"])
                entry["detail_items"] = len(detail_ids)
        except Exception as error:
            entry["error"] = type(error).__name__
        entry["elapsed_seconds"] = round(time.monotonic() - start, 2)
        report["checks"].append(entry)
        print(json.dumps(entry, ensure_ascii=False), flush=True)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")


if __name__ == "__main__":
    main()
