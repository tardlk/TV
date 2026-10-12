from http.server import SimpleHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse
import json
from pathlib import Path
import argparse
parser = argparse.ArgumentParser()
parser.add_argument("--directory", type=Path, required=True)
args = parser.parse_args()
root = args.directory.resolve()
video = {"vod_id": "local-smoke", "vod_name": "TV 播放验证", "vod_pic": "", "vod_year": "2026", "type_name": "本地验证", "vod_content": "固定视频片段，用于验证播放与界面接线。", "vod_play_from": "local", "vod_play_url": "片段一$http://10.0.2.2:48979/clip.mp4#片段二$http://10.0.2.2:48979/clip.mp4"}
config = {"spider": "", "sites": [{"key": "local-smoke", "name": "本地验证", "type": 1, "api": "http://10.0.2.2:48979/api", "searchable": 1, "filterable": 1}], "parses": [], "lives": [], "flags": [], "rules": []}
class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(root), **kwargs)
    def do_GET(self):
        path = urlparse(self.path).path
        if path in ["/api", "/config.json"]:
            data = json.dumps(config if path == "/config.json" else {"class": [{"type_id": "1", "type_name": "验证"}], "list": [video], "page": 1, "pagecount": 1}, ensure_ascii=False).encode()
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)
        else:
            super().do_GET()
HTTPServer(("127.0.0.1", 48979), Handler).serve_forever()
