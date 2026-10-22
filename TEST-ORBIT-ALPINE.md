# Test minimal ORBIT + VM Alpine

## Données de test
Après réinitialisation de la base de test puis redémarrage du backend, `Init` crée :
- un compte `admin` (mot de passe initial `1234`) ;
- un bâtiment `TEST` ;
- une salle de cours `101` et une salle de stockage `STOCK` ;
- cinq postes `TEST-S101-P01` à `P05` ;
- une seule UC réelle, `hote1`, affectée à `P01`. Les postes P02 à P05 restent volontairement sans UC tant qu'il n'existe pas de VM associée.

## Réinitialiser la base
1. Arrêter le backend.
2. Faire une sauvegarde si nécessaire.
3. Dans la base PostgreSQL de TEST `orbit`, exécuter `backend/orbit/reset-test-database.sql`. Cette opération efface les données applicatives de cette base.
4. Redémarrer le backend : l'initialiseur recrée le jeu minimal.

## Agent Alpine
Dans la VM `hote1`, installer Python et psutil si nécessaire : `apk add python3 py3-psutil`. Copier `tools/orbit_agent.py` dans la VM, puis lancer :

```sh
python3 orbit_agent.py
```

L'agent écoute sur le port TCP 8777 et utilise le même token que `application.yaml`. Pour l'essai, l'URL backend par défaut est `http://192.168.147.128:8777` côté application : remplace-la par l'IP actuelle de la VM si elle change, ou définis `AGENT_URL`.

L'agent autorise la lecture des processus et la terminaison par PID avec token. Ne publie pas le port 8777 sur Internet ; limite l'accès au réseau de test.
