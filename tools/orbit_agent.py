#!/usr/bin/env python3
"""Agent de test ORBIT pour Alpine Linux. API locale protégée par un token."""
import json
import os
import socket
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse

import psutil

TOKEN = os.environ.get("ORBIT_AGENT_TOKEN", "lJ0HxavQo-1dlAToADYOyOIGpzo_gKQY")
HOST = os.environ.get("ORBIT_AGENT_HOST", "0.0.0.0")
PORT = int(os.environ.get("ORBIT_AGENT_PORT", "8777"))
PROTECTED = {"init", "systemd", "kthreadd", "ksoftirqd", "migration", "rcu_sched"}

# Amorcer les compteurs CPU pour obtenir des valeurs pertinentes dès la requête suivante.
for proc in psutil.process_iter():
    try:
        proc.cpu_percent(None)
    except (psutil.Error, OSError):
        pass

class Handler(BaseHTTPRequestHandler):
    def _authorized(self):
        import hmac
        return hmac.compare_digest(self.headers.get("X-Token", ""), TOKEN)

    def _json(self, status, value):
        body = json.dumps(value).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if urlparse(self.path).path != "/processes":
            return self._json(404, {"error": "not found"})
        if not self._authorized():
            return self._json(401, {"error": "unauthorized"})
        processes = []
        for proc in psutil.process_iter(["pid", "name", "username", "memory_percent"]):
            try:
                info = proc.info
                processes.append({
                    "pid": info.get("pid"),
                    "name": info.get("name") or "(inconnu)",
                    "username": info.get("username") or "root",
                    "cpu_percent": proc.cpu_percent(None),
                    "memory_percent": round(info.get("memory_percent") or 0.0, 2),
                })
            except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess, OSError):
                continue
        self._json(200, {
            "host": socket.gethostname(),
            "count": len(processes),
            "os": "Alpine Linux",
            "processes": processes,
        })

    def do_POST(self):
        parts = urlparse(self.path).path.strip("/").split("/")
        if len(parts) != 2 or parts[0] != "kill" or not parts[1].isdigit():
            return self._json(404, {"error": "not found"})
        if not self._authorized():
            return self._json(401, {"error": "unauthorized"})
        pid = int(parts[1])
        if pid <= 1 or pid == os.getpid():
            return self._json(403, {"error": "protected process"})
        try:
            proc = psutil.Process(pid)
            name = (proc.name() or "").lower()
            if name in PROTECTED:
                return self._json(403, {"error": "protected process"})
            proc.terminate()
            try:
                proc.wait(timeout=3)
            except psutil.TimeoutExpired:
                proc.kill()
            self._json(200, {"ok": True, "pid": pid})
        except psutil.NoSuchProcess:
            self._json(404, {"error": "process not found", "pid": pid})
        except psutil.AccessDenied:
            self._json(403, {"error": "access denied", "pid": pid})

    def log_message(self, fmt, *args):
        print("[%s] %s" % (self.log_date_time_string(), fmt % args))

print(f"ORBIT Alpine agent écoute sur {HOST}:{PORT}")
ThreadingHTTPServer((HOST, PORT), Handler).serve_forever()
