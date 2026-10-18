# JamendoPlayer

Streaming de musique libre (Creative Commons) via l'API officielle Jamendo. Kotlin + Compose + Media3 : recherche, genres, pochettes, favoris, lecture en arrière-plan avec notification.

## Client ID Jamendo (gratuit)
1. Crée un compte sur https://devportal.jamendo.com
2. Crée une application → copie le `client_id`.

## Build avec GitHub
1. Pousse ce dossier sur un dépôt GitHub.
2. Settings → Secrets and variables → Actions → New repository secret : `JAMENDO_CLIENT_ID`.
3. Actions → "Build APK" → Run workflow.
4. Télécharge l'artefact `jamendoplayer-apk` (`app-debug.apk`) et installe-le.

Le client_id peut aussi être saisi dans Réglages de l'app.

Note : usage non commercial gratuit ; un usage commercial nécessite une licence Jamendo.
