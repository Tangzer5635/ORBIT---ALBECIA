#!/usr/bin/env python3
"""Agent ORBIT Windows : remonte uniquement les processus possédant une fenêtre visible.

L'agent ne reçoit aucune connexion : c'est lui qui ouvre un WebSocket vers le serveur ORBIT (sortant, donc rien à
autoriser dans le pare-feu de la machine). Le WebSocket passe par Nginx (port des agents) qui le relaie au back.
Il reste ouvert : l'agent s'y reconnecte tout seul s'il tombe.

Messages JSON (un seul WebSocket, dans les deux sens) :
- agent -> back  {"type": "hello", "mac", "nomMachine", "ip", "os", "ram_go"}   à la connexion
- agent -> back  {"type": "processus", "processes": [{pid, name, username, cpu_percent, memory_percent}]}
                 dès que la liste des applications change, et au moins toutes les 20 s
- back -> agent  {"type": "kill", "id", "pid"}                                  ordre d'arrêter un processus
- agent -> back  {"type": "resultat", "id", "ok", "statut", "erreur"}           réponse à l'ordre
  statut : 200 arrêté, 403 refusé (processus système ou sans fenêtre visible), 404 déjà fermé
Le token (même valeur que agent.token côté back) est envoyé dans l'en-tête X-Token de la connexion.

À lancer dans la session Windows de l'utilisateur (tâche planifiée « à l'ouverture de session »), pas en service :
EnumWindows ne voit que les fenêtres du bureau de la session où tourne l'agent.
Dépendances : py -m pip install psutil websocket-client
"""
import ctypes
from ctypes import wintypes
import json, os, socket, platform, sys, time
from urllib.parse import urlparse
import psutil
import websocket  # paquet « websocket-client »

# Lancé par pythonw (tâche planifiée) : pas de console, on écrit dans orbit_agent.log à côté du script
if sys.stdout is None:
    sys.stdout = sys.stderr = open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "orbit_agent.log"),
                                   "a", buffering=1, encoding="utf-8")

TOKEN = os.environ.get("ORBIT_AGENT_TOKEN", "lJ0HxavQo-1dlAToADYOyOIGpzo_gKQY")
# Nginx du serveur ORBIT vu depuis la VM, sur le port réservé aux agents : IP du PC hôte sur le réseau VMware (VMnet8)
SERVEUR = os.environ.get("ORBIT_SERVEUR", "ws://192.168.147.1:8444")
URL_WS = SERVEUR + "/api/v1/agents/ws"
VERIF_S = 2          # la liste des fenêtres est regardée toutes les 2 s (en local), envoyée seulement si elle change
ANNONCE_S = 20       # ... et au moins toutes les 20 s, pour dire au serveur que l'agent est toujours là
RECONNEXION_S = 5
PROTECTED = {
    "system", "registry", "smss.exe", "csrss.exe", "wininit.exe",
    "services.exe", "lsass.exe", "winlogon.exe", "svchost.exe",
    "fontdrvhost.exe", "dwm.exe", "system idle process", "explorer.exe"
}
# RAM totale de la VM, en Go (affichée dans ORBIT pour vérifier que l'UC correspond à la machine)
RAM_GO = round(psutil.virtual_memory().total / 1024 ** 3, 1)


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


def processus_visibles():
    """Les applications ouvertes (fenêtre visible), hors processus système protégés."""
    visible = visible_window_pids()
    lignes = []
    for p in psutil.process_iter(["pid", "name", "username", "memory_percent"]):
        try:
            info = p.info
            if info["pid"] not in visible or (info.get("name") or "").lower() in PROTECTED:
                continue
            lignes.append({
                "pid": info["pid"],
                "name": info.get("name") or "(unknown)",
                "username": info.get("username") or "",
                "cpu_percent": p.cpu_percent(None),
                "memory_percent": round(info.get("memory_percent") or 0, 2)
            })
        except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess):
            pass
    return lignes


def tuer(pid):
    """Arrête le processus. Renvoie (statut, erreur) : 200 arrêté, 403 refusé, 404 déjà fermé."""
    try:
        p = psutil.Process(pid)
        name = (p.name() or "").lower()
        if pid <= 4 or pid == os.getpid() or name in PROTECTED:
            return 403, "processus protégé"
        # Sécurité supplémentaire : seules les applications ayant une fenêtre visible sont tuables.
        if pid not in visible_window_pids():
            return 403, "processus sans fenêtre visible"
        p.terminate()
        try:
            p.wait(timeout=3)
        except psutil.TimeoutExpired:
            p.kill()
        print(f"Processus arrêté : {name} (PID {pid})")
        return 200, None
    except psutil.NoSuchProcess:
        return 404, "processus introuvable"
    except psutil.AccessDenied:
        return 403, "accès refusé ; relance l'agent en administrateur si nécessaire"


def envoyer(ws, message):
    ws.send(json.dumps(message))


def session(ws):
    """Une connexion : se présente, envoie la liste des applications et exécute les ordres du serveur."""
    envoyer(ws, {"type": "hello", "mac": mac_vers_serveur(), "nomMachine": socket.gethostname(),
                 "ip": ip_vers_serveur(), "os": platform.platform(), "ram_go": RAM_GO})
    cle_envoyee, dernier_envoi = None, 0.0
    while True:
        try:
            texte = ws.recv()  # attend au plus VERIF_S secondes
            commande = json.loads(texte) if texte else {}
            if commande.get("type") == "kill":
                statut, erreur = tuer(int(commande["pid"]))
                envoyer(ws, {"type": "resultat", "id": commande["id"], "ok": statut == 200,
                             "statut": statut, "erreur": erreur})
        except websocket.WebSocketTimeoutException:
            pass  # rien reçu : on regarde si la liste a changé

        lignes = processus_visibles()
        cle = sorted((p["pid"], p["name"]) for p in lignes)
        if cle != cle_envoyee or time.time() - dernier_envoi >= ANNONCE_S:
            envoyer(ws, {"type": "processus", "processes": lignes})
            cle_envoyee, dernier_envoi = cle, time.time()


def boucle():
    """Se (re)connecte au serveur ORBIT tant que l'agent tourne."""
    while True:
        try:
            ws = websocket.create_connection(URL_WS, header=["X-Token: " + TOKEN], timeout=VERIF_S,
                                             suppress_origin=True,  # l'agent n'est pas un navigateur
                                             http_no_proxy=[urlparse(SERVEUR).hostname])  # réseau local : sans proxy
            print(f"Connecté à {URL_WS}")
            try:
                session(ws)
            finally:
                ws.close()
        except Exception as e:
            print(f"Connexion à {URL_WS} perdue ou impossible : {e}")
        time.sleep(RECONNEXION_S)


if __name__ == "__main__":
    print(f"ORBIT Windows agent — {socket.gethostname()} ({RAM_GO} Go RAM)")
    try:
        print(f"Adresse MAC à saisir dans ORBIT : {mac_vers_serveur()} — connexion à {URL_WS}")
    except OSError as e:
        print(f"Serveur ORBIT {SERVEUR} injoignable pour l'instant : {e}")
    boucle()
