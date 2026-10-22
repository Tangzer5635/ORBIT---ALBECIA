#!/usr/bin/env python3
"""Agent ORBIT Windows : remonte uniquement les processus possédant une fenêtre visible.

- Toutes les 20 s, s'annonce au serveur ORBIT : POST {SERVEUR}/api/v1/agents/ {"mac", "nomMachine"}.
  Le serveur retient l'IP d'où vient l'annonce : c'est l'adresse MAC qui relie la machine à son UC dans ORBIT.
- GET  /processes   -> {"host", "mac", "os", "ram_go", "count", "processes": [{pid, name, username, cpu_percent, memory_percent}]}
- POST /kill/<pid>  -> arrête le processus (refusé : processus système ou sans fenêtre visible)
Tout passe par l'en-tête X-Token (même valeur que agent.token dans application.yaml).

À lancer dans la session Windows de l'utilisateur (tâche planifiée « à l'ouverture de session »), pas en service :
EnumWindows ne voit que les fenêtres du bureau de la session où tourne l'agent.
"""
import ctypes
from ctypes import wintypes
import hmac, json, os, socket, platform, sys, threading, time
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse
import psutil

# Lancé par pythonw (tâche planifiée) : pas de console, on écrit dans orbit_agent.log à côté du script
if sys.stdout is None:
    sys.stdout = sys.stderr = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "orbit_agent.log"),
                                   "a", buffering=1, encoding="utf-8")

TOKEN = os.environ.get("ORBIT_AGENT_TOKEN", "lJ0HxavQo-1dlAToADYOyOIGpzo_gKQY")
# Back Spring Boot vu depuis la VM : IP du PC hôte sur le réseau VMware (carte VMnet8)
SERVEUR = os.environ.get("ORBIT_SERVEUR", "http://192.168.147.1:8080")
HOST = "0.0.0.0"
PORT = int(os.environ.get("ORBIT_AGENT_PORT", "8777"))
ANNONCE_S = 20
PROTECTED = {
    "system", "registry", "smss.exe", "csrss.exe", "wininit.exe",
    "services.exe", "lsass.exe", "winlogon.exe", "svchost.exe",
    "fontdrvhost.exe", "dwm.exe", "system idle process", "explorer.exe"
}
# RAM totale de la VM, en Go (affichée dans ORBIT pour vérifier que l'UC correspond à la machine)
RAM_GO = round(psutil.virtual_memory().total / 1024 ** 3, 1)
# Sans proxy : le serveur ORBIT est sur le réseau local (un proxy Windows configuré ne doit pas s'en mêler)
SANS_PROXY = urllib.request.build_opener(urllib.request.ProxyHandler({}))


# 1) Docstring, lignes 4-5 : remplace par
- Toutes les 20 s, s'annonce au serveur ORBIT : POST {SERVEUR}/api/v1/agents/ {"mac", "nomMachine", "ip"}.
  Le serveur appelle l'agent à l'IP annoncée (derrière Docker, il ne verrait que l'IP de la passerelle Docker) :
  c'est l'adresse MAC qui relie la machine à son UC dans ORBIT.

# 2) Remplace la fonction mac_vers_serveur() par ces deux fonctions
def ip_vers_serveur():
    """Adresse IPv4 de la carte réseau qui sert à joindre le serveur ORBIT."""
    url = urlparse(SERVEUR)
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
        s.connect((url.hostname, url.port or 80))  # UDP : rien n'est envoyé, Windows choisit juste la carte
        return s.getsockname()[0]


def mac_vers_serveur():
    """Adresse MAC de la carte réseau qui sert à joindre le serveur ORBIT, au format 00:0C:29:AB:CD:EF."""
    ip_locale = ip_vers_serveur()
    for adresses in psutil.net_if_addrs().values():
        if any(a.family == socket.AF_INET and a.address == ip_locale for a in adresses):
            for a in adresses:
                if a.family == psutil.AF_LINK:
                    return a.address.replace("-", ":").upper()
    return None

# 3) Dans annoncer_en_boucle(), remplace la ligne « corps = ... » par
            corps = json.dumps({"mac": mac_vers_serveur(), "nomMachine": socket.gethostname(),
                                "ip": ip_vers_serveur()}).encode("utf-8")


def annoncer_en_boucle():
    while True:
        try:
            corps = json.dumps({"mac": mac_vers_serveur(), "nomMachine": socket.gethostname()}).encode("utf-8")
            requete = urllib.request.Request(SERVEUR + "/api/v1/agents/", data=corps, method="POST",
                                             headers={"Content-Type": "application/json", "X-Token": TOKEN})
            SANS_PROXY.open(requete, timeout=5).close()
        except Exception as e:
            print(f"Annonce impossible à {SERVEUR} : {e}")
        time.sleep(ANNONCE_S)


# EnumWindows permet de ne retenir que les PID ayant au moins une fenêtre visible.
def visible_window_pids():
    pids = set()
    user32 = ctypes.windll.user32
    enum_windows = user32.EnumWindows
    enum_windows.argtypes = [ctypes.WINFUNCTYPE(wintypes.BOOL, wintypes.HWND, wintypes.LPARAM), wintypes.LPARAM]
    enum_windows.restype = wintypes.BOOL
    is_visible = user32.IsWindowVisible
    get_pid = user32.GetWindowThreadProcessId

    @ctypes.WINFUNCTYPE(wintypes.BOOL, wintypes.HWND, wintypes.LPARAM)
    def callback(hwnd, _):
        if is_visible(hwnd):
            pid = wintypes.DWORD()
            get_pid(hwnd, ctypes.byref(pid))
            if pid.value:
                pids.add(pid.value)
        return True

    enum_windows(callback, 0)
    return pids


class Handler(BaseHTTPRequestHandler):
    def authorized(self):
        return hmac.compare_digest(self.headers.get("X-Token", ""), TOKEN)

    def send_json(self, status, data):
        body = json.dumps(data).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if urlparse(self.path).path != "/processes":
            return self.send_json(404, {"error": "not found"})
        if not self.authorized():
            return self.send_json(401, {"error": "unauthorized"})
        visible = visible_window_pids()
        rows = []
        for p in psutil.process_iter(["pid", "name", "username", "memory_percent"]):
            try:
                info = p.info
                if info["pid"] not in visible or (info.get("name") or "").lower() in PROTECTED:
                    continue
                rows.append({
                    "pid": info["pid"],
                    "name": info.get("name") or "(unknown)",
                    "username": info.get("username") or "",
                    "cpu_percent": p.cpu_percent(None),
                    "memory_percent": round(info.get("memory_percent") or 0, 2)
                })
            except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess):
                pass
        self.send_json(200, {
            "host": socket.gethostname(), "mac": mac_vers_serveur(), "os": platform.platform(), "ram_go": RAM_GO,
            "count": len(rows), "processes": rows
        })

    def do_POST(self):
        parts = urlparse(self.path).path.strip("/").split("/")
        if len(parts) != 2 or parts[0] != "kill" or not parts[1].isdigit():
            return self.send_json(404, {"error": "not found"})
        if not self.authorized():
            return self.send_json(401, {"error": "unauthorized"})
        pid = int(parts[1])
        try:
            p = psutil.Process(pid)
            name = (p.name() or "").lower()
            if pid <= 4 or pid == os.getpid() or name in PROTECTED:
                return self.send_json(403, {"error": "protected process"})
            # Sécurité supplémentaire : seules les applications ayant une fenêtre visible sont tuables.
            if pid not in visible_window_pids():
                return self.send_json(403, {"error": "process has no visible window"})
            p.terminate()
            try:
                p.wait(timeout=3)
            except psutil.TimeoutExpired:
                p.kill()
            print(f"Processus arrêté : {name} (PID {pid})")
            self.send_json(200, {"ok": True, "pid": pid, "name": name})
        except psutil.NoSuchProcess:
            self.send_json(404, {"error": "process not found"})
        except psutil.AccessDenied:
            self.send_json(403, {"error": "access denied; relance l'agent en administrateur si nécessaire"})

    def log_message(self, fmt, *args):
        print("[%s] %s" % (self.log_date_time_string(), fmt % args))


print(f"ORBIT Windows agent listening on {HOST}:{PORT} — {socket.gethostname()} ({RAM_GO} Go RAM)")
try:
    print(f"Adresse MAC à saisir dans ORBIT : {mac_vers_serveur()} — annonces vers {SERVEUR}")
except OSError as e:
    print(f"Serveur ORBIT {SERVEUR} injoignable pour l'instant : {e}")
threading.Thread(target=annoncer_en_boucle, daemon=True).start()
ThreadingHTTPServer((HOST, PORT), Handler).serve_forever()
