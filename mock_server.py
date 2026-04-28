import json
import os
import posixpath
import secrets
import time
from http import HTTPStatus
from http.cookies import SimpleCookie
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, unquote, urlparse


ROOT_DIR = os.path.join(os.path.dirname(__file__), "src", "main", "resources", "static")


def _json_response(handler, status, payload, headers=None, set_cookie=None):
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    handler.send_response(status)
    handler.send_header("Content-Type", "application/json; charset=utf-8")
    handler.send_header("Content-Length", str(len(body)))
    handler.send_header("Cache-Control", "no-store")
    if headers:
        for k, v in headers.items():
            handler.send_header(k, v)
    if set_cookie:
        handler.send_header("Set-Cookie", set_cookie)
    handler.end_headers()
    handler.wfile.write(body)


def _read_json(handler):
    length = int(handler.headers.get("Content-Length") or "0")
    if length <= 0:
        return {}
    raw = handler.rfile.read(length)
    try:
        return json.loads(raw.decode("utf-8"))
    except Exception:
        return {}


def _get_cookies(handler):
    cookie_header = handler.headers.get("Cookie")
    if not cookie_header:
        return {}
    c = SimpleCookie()
    c.load(cookie_header)
    return {k: morsel.value for k, morsel in c.items()}


def _guess_type(path):
    lower = path.lower()
    if lower.endswith(".html"):
        return "text/html; charset=utf-8"
    if lower.endswith(".css"):
        return "text/css; charset=utf-8"
    if lower.endswith(".js"):
        return "application/javascript; charset=utf-8"
    if lower.endswith(".json"):
        return "application/json; charset=utf-8"
    if lower.endswith(".png"):
        return "image/png"
    if lower.endswith(".jpg") or lower.endswith(".jpeg"):
        return "image/jpeg"
    if lower.endswith(".gif"):
        return "image/gif"
    if lower.endswith(".svg"):
        return "image/svg+xml"
    if lower.endswith(".woff2"):
        return "font/woff2"
    if lower.endswith(".woff"):
        return "font/woff"
    if lower.endswith(".ttf"):
        return "font/ttf"
    return "application/octet-stream"


def _safe_path(url_path):
    url_path = url_path.split("?", 1)[0].split("#", 1)[0]
    url_path = posixpath.normpath(unquote(url_path))
    while url_path.startswith("/"):
        url_path = url_path[1:]
    if url_path.startswith(".."):
        return None
    return url_path


class Handler(BaseHTTPRequestHandler):
    server_version = "MockServer/1.0"

    sessions = {}

    def log_message(self, fmt, *args):
        return

    def do_GET(self):
        parsed = urlparse(self.path)
        if parsed.path.startswith("/api/"):
            return self._handle_api("GET", parsed)
        return self._serve_static(parsed.path)

    def do_POST(self):
        parsed = urlparse(self.path)
        if parsed.path.startswith("/api/"):
            return self._handle_api("POST", parsed)
        return self._not_found()

    def do_PUT(self):
        parsed = urlparse(self.path)
        if parsed.path.startswith("/api/"):
            return self._handle_api("PUT", parsed)
        return self._not_found()

    def do_DELETE(self):
        parsed = urlparse(self.path)
        if parsed.path.startswith("/api/"):
            return self._handle_api("DELETE", parsed)
        return self._not_found()

    def _not_found(self):
        self.send_response(HTTPStatus.NOT_FOUND)
        self.end_headers()

    def _serve_static(self, path):
        safe = _safe_path(path)
        if safe is None:
            return self._not_found()

        if safe == "" or safe.endswith("/") or not os.path.splitext(safe)[1]:
            safe = "index.html"

        fs_path = os.path.join(ROOT_DIR, safe)
        if os.path.isdir(fs_path):
            fs_path = os.path.join(fs_path, "index.html")

        if not os.path.exists(fs_path):
            fs_path = os.path.join(ROOT_DIR, "index.html")

        try:
            with open(fs_path, "rb") as f:
                data = f.read()
        except Exception:
            return self._not_found()

        ctype = _guess_type(fs_path)
        self.send_response(HTTPStatus.OK)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(data)

    def _handle_api(self, method, parsed):
        path = parsed.path
        cookies = _get_cookies(self)
        session_id = cookies.get("sessionId")
        user = self.sessions.get(session_id)

        if method == "GET" and path == "/api/auth/check":
            if not user:
                sid = secrets.token_urlsafe(24)
                user = {"id": 1, "uid": "mock-" + sid[:8], "username": "mock_user", "createTime": int(time.time() * 1000), "avatar": ""}
                self.sessions[sid] = user
                set_cookie = f"sessionId={sid}; Path=/; SameSite=Lax"
                return _json_response(
                    self,
                    HTTPStatus.OK,
                    {"code": 200, "message": "已登录", "data": {"id": user["id"], "uid": user["uid"], "username": user["username"]}},
                    set_cookie=set_cookie,
                )
            return _json_response(
                self,
                HTTPStatus.OK,
                {"code": 200, "message": "已登录", "data": {"id": user["id"], "uid": user["uid"], "username": user["username"]}},
            )

        if method == "POST" and path in ("/api/auth/login", "/api/auth/register"):
            payload = _read_json(self)
            username = (payload.get("username") or "user").strip() or "user"
            sid = secrets.token_urlsafe(24)
            u = {"id": 1, "uid": "mock-" + sid[:8], "username": username, "createTime": int(time.time() * 1000), "avatar": ""}
            self.sessions[sid] = u
            set_cookie = f"sessionId={sid}; Path=/; SameSite=Lax"
            return _json_response(
                self,
                HTTPStatus.OK,
                {"code": 200, "message": "登录成功" if path.endswith("/login") else "注册成功", "data": {"id": u["id"], "uid": u["uid"], "username": u["username"]}},
                set_cookie=set_cookie,
            )

        if method == "POST" and path == "/api/auth/logout":
            if session_id and session_id in self.sessions:
                del self.sessions[session_id]
            set_cookie = "sessionId=; Path=/; Max-Age=0; SameSite=Lax"
            return _json_response(self, HTTPStatus.OK, {"code": 200, "message": "登出成功"}, set_cookie=set_cookie)

        if path.startswith("/api/profile/me") and method == "GET":
            if not user:
                return _json_response(self, HTTPStatus.UNAUTHORIZED, {"code": 401, "message": "未登录"})
            return _json_response(self, HTTPStatus.OK, {"code": 200, "message": "ok", "data": user})

        if path.startswith("/api/profile/avatar") and method == "POST":
            if not user:
                return _json_response(self, HTTPStatus.UNAUTHORIZED, {"code": 401, "message": "未登录"})
            payload = _read_json(self)
            user["avatar"] = payload.get("avatar") or user.get("avatar") or ""
            return _json_response(self, HTTPStatus.OK, {"code": 200, "message": "ok", "data": True})

        if path.startswith("/api/messages/unread-count") and method == "GET":
            return _json_response(self, HTTPStatus.OK, {"code": 200, "message": "ok", "data": {"likes": 0, "replies": 0, "mentions": 0, "likesReceived": 0, "system": 0, "total": 0}})

        if path.startswith("/api/messages/settings") and method in ("GET", "PUT"):
            if method == "GET":
                return _json_response(self, HTTPStatus.OK, {"code": 200, "message": "ok", "data": {"emailNotification": True, "pushNotification": True, "replyNotification": True, "likeNotification": True, "mentionNotification": True, "systemNotification": True}})
            return _json_response(self, HTTPStatus.OK, {"code": 200, "message": "ok", "data": True})

        if path.startswith("/api/") and method == "GET":
            qs = parse_qs(parsed.query or "")
            page = int((qs.get("page") or ["1"])[0] or "1")
            size = int((qs.get("size") or ["20"])[0] or "20")
            return _json_response(self, HTTPStatus.OK, {"code": 200, "message": "ok", "data": {"items": [], "total": 0, "page": page, "size": size}})

        if path.startswith("/api/") and method in ("POST", "PUT", "DELETE"):
            return _json_response(self, HTTPStatus.OK, {"code": 200, "message": "ok", "data": True})

        return _json_response(self, HTTPStatus.NOT_FOUND, {"code": 404, "message": "not found"})


def main():
    host = os.environ.get("HOST", "0.0.0.0")
    port = int(os.environ.get("PORT", "8080"))
    httpd = ThreadingHTTPServer((host, port), Handler)
    httpd.serve_forever()


if __name__ == "__main__":
    main()
