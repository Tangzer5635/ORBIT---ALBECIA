# Agents Windows ORBIT — test local

Sur chacune des deux VM, copier `orbit_agent_windows.py`, installer Python 3 puis ouvrir PowerShell dans le dossier :

```powershell
py -m pip install psutil
py .\orbit_agent_windows.py
```

Faire cela sur HOTE 2 (`192.168.147.129`) et HOTE 3 (`192.168.147.130`). L'agent utilise le port TCP 8777 et le token défini dans `application.yaml`.

Si le pare-feu bloque la connexion, dans PowerShell administrateur sur chaque VM, autoriser le port uniquement sur le réseau privé/local :

```powershell
New-NetFirewallRule -DisplayName "ORBIT agent 8777" -Direction Inbound -Protocol TCP -LocalPort 8777 -RemoteAddress LocalSubnet -Action Allow -Profile Private
```

Le backend doit pouvoir joindre les IP VMware indiquées. Ne pas exposer ce port à Internet. Pour les droits de terminaison de processus, démarrer l'agent en administrateur uniquement dans cet environnement de test.
