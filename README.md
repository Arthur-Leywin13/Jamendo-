# JamendoPlayer

Lecteur Android de musique libre via l'API officielle Jamendo. Kotlin, Jetpack Compose et Media3 : recherche, genres, pochettes, favoris et lecture en arrière-plan avec notification.

## Identifiant client Jamendo

1. Crée un compte sur https://devportal.jamendo.com.
2. Crée une application et copie son `client_id`.
3. Dans l'application, saisis cet identifiant dans Réglages, ou fournis-le pendant la compilation.

**Important : le `client_id` est un identifiant d'application, pas un mot de passe ni un jeton d'accès utilisateur.** Il est intégré à l'APK lorsqu'il est fourni à la compilation et peut donc être extrait de l'application. Ne place jamais dans `BuildConfig` un secret privé ou un jeton qui doit rester confidentiel.

## Compiler l'APK avec GitHub Actions

1. Ouvre les paramètres du dépôt GitHub.
2. Va dans **Settings → Secrets and variables → Actions**.
3. Ajoute le secret du dépôt `JAMENDO_CLIENT_ID`.
4. Ouvre **Actions → Build APK → Run workflow**.
5. Télécharge l'artefact `jamendoplayer-apk`, qui contient `app-debug.apk`.

Le secret GitHub évite d'écrire l'identifiant dans le code source, mais ne le cache pas une fois intégré à l'APK.

## Sécurité et diagnostic

- Les appels à l'API utilisent HTTPS.
- Les requêtes ont des délais d'attente limités et les erreurs réseau ou les réponses JSON invalides sont signalées à l'application.
- Aucun compte utilisateur, mot de passe, jeton d'accès ou jeton de renouvellement n'est géré par cette application dans sa version actuelle.
- N'ajoute pas de journalisation contenant l'URL complète des requêtes : elle peut inclure le paramètre `client_id`.

## Licence Jamendo

Vérifie les conditions de Jamendo pour ton usage. Un usage commercial peut nécessiter une licence spécifique.
