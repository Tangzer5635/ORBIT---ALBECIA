# ORBIT — page de test des processus visibles sur les deux VM Windows

## Ce que fait cette version
- Ajoute la page de navigation `Processus actifs` (route `/processus-actifs`, réservée au rôle ADMINISTRATEUR).
- Interroge les deux agents toutes les 5 secondes via le backend Spring Boot (`/api/agent/processes?host=hote2|hote3`).
- Regroupe les processus de même nom dans une seule ligne et montre le nombre d'instances sur chaque VM.
- L'agent Windows ne remonte que les PID qui possèdent une fenêtre Windows visible (`EnumWindows` + `IsWindowVisible`).
- Le bouton Arrêter demande confirmation et tente d'arrêter toutes les instances du nom sélectionné sur les deux VM. L'agent refuse les processus système protégés et les processus sans fenêtre visible.

## Fichiers changés
- `frontend/orbitfront/src/App.tsx` : branche le composant ProcessMonitor à une route.
- `frontend/orbitfront/src/config/acces.ts` : ajoute l'entrée de menu `Processus actifs`.
- `frontend/orbitfront/src/components/ProcessMonitor.tsx` : tableau agrégé des deux VM, rafraîchissement 5 s, filtre et arrêt par groupe.
- `tools/orbit_agent_windows.py` : inventaire limité aux processus avec fenêtre visible et garde-fous pour l'arrêt.

## Vérifications requises sur le PC hôte
1. Dans `backend/orbit/src/main/resources/application.yaml`, les paramètres doivent rester :
   - `agent.windows2-url: http://192.168.147.129:8777`
   - `agent.windows3-url: http://192.168.147.130:8777`
   - `agent.token: lJ0HxavQo-1dlAToADYOyOIGpzo_gKQY`
2. Le même token par défaut est dans `tools/orbit_agent_windows.py`. Si tu le modifies, modifie les deux côtés.
3. Sur chaque VM Windows, installer Python 3 et `psutil`, puis remplacer le fichier de l'agent par celui-ci et lancer depuis PowerShell :
   `python -m pip install psutil`
   `python .\orbit_agent_windows.py`
4. Autoriser TCP entrant 8777 depuis le réseau VMware privé sur les deux VM. Les tests `Test-NetConnection` ont déjà réussi.
5. Redémarrer le backend Spring Boot et le frontend Vite. Connecte-toi comme administrateur, puis clique sur `Processus actifs`.

## Diagnostic
- `401` : token différent entre le backend et l'agent.
- `404` : chemin HTTP incorrect ou agent non à jour.
- `Hors ligne` : IP, pare-feu, ou agent arrêté.
- Le tableau est vide si aucune fenêtre n'est visible, ou si les requêtes des agents échouent.

## Limites
Le build frontend n'a pas pu être validé dans cet environnement : les dépendances natives Rolldown/Vite présentes dans l'archive ne contiennent pas le binding Linux requis. Le projet a donc besoin d'un `npm install` / `npm ci` dans l'environnement Windows de développement avant son propre build si Vite y signale une erreur de dépendances.
