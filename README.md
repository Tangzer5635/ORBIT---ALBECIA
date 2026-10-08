# ORBIT — ALBECIA

Application web de gestion du parc informatique et des salles de formation.

## Structure

| Dossier     | Contenu                                   |
|-------------|-------------------------------------------|
| `bdd/`      | Scripts SQL : création des tables, jeu de données |
| `backend/`  | API Java Spring Boot                      |
| `frontend/` | Application React                         |

## Branches

```
main          → version stable (démos, rendus)
 └── develop  → intégration du travail de l'équipe
      ├── dev/tanguy
      ├── dev/paulux
      └── dev/besnard
```

`main` et `develop` sont protégées : on n'y pousse jamais directement, tout passe par une Pull Request.

## Workflow au quotidien

1. Se placer sur sa branche :
   ```
   git checkout dev/<prenom>
   ```
2. Récupérer le travail des autres avant de commencer :
   ```
   git pull origin develop
   ```
3. Coder, puis enregistrer :
   ```
   git add .
   git commit -m "feat(backend): ajout de l'entité Salle"
   git push
   ```
4. Quand un morceau fonctionne : ouvrir une **Pull Request `dev/<prenom>` → `develop`** sur GitHub.
5. Après relecture, la PR est fusionnée dans `develop`. `develop` est fusionnée dans `main` à chaque jalon.

**Règle d'or : des petites PR, souvent.** Une branche qui reste des semaines sans être fusionnée accumule les conflits.

## Messages de commit

`type(partie): description` — par exemple :
- `feat(frontend): page de connexion`
- `fix(backend): erreur 500 sur /api/salles`
- `chore(bdd): ajout du jeu de données de test`
