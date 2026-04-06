# ZettelGeist

Eine native Android-App für das Zettelkasten-Prinzip – mit optionaler KI-Assistenz.

## Was ist ZettelGeist?

ZettelGeist ist ein persönliches Wissensmanagement-Tool nach der Zettelkasten-Methode. Jede Notiz enthält einen atomaren Gedanken und kann mit anderen Notizen über Wikilinks verknüpft werden.

## Features

### Fünf Tabs
- **Inbox** – Schnelle Gedanken erfassen, mit Spracherkennung (Walk & Talk)
- **Zettelkasten** – Dauerhaft ausgearbeitete, atomare Zettel
- **Quellen** – Literaturnotizen mit Metadaten (Buch, Artikel, URL, Podcast, Video)
- **Suche** – Volltextsuche über alle Notizen + filterbare Tag-Cloud
- **Graph** – Netzwerk-Visualisierung aller Notizen und ihrer Verknüpfungen

### Notiz-Editor
- Markdown-Unterstützung mit Toolbar (Fett, Kursiv, Links)
- Wikilinks `[[Notiz-Titel]]` mit Autocomplete-Dialog
- Inline-Tags `#tagname` werden automatisch geparst
- Backlinks-Anzeige
- Auto-Save nach 2 Sekunden Inaktivität
- Zeichenzähler mit Aufteilen-Hinweis bei >1500 Zeichen

### Walk & Talk
- Mikrofon-Button in der Inbox
- Spracherkennung über Android SpeechRecognizer (offline-fähig)
- Aufnahme-Overlay mit Live-Transkript
- Ergebnis wird automatisch als neue Inbox-Notiz gespeichert

### Graph-Ansicht
- Force-directed Layout auf Compose Canvas
- Farbcodierung: Inbox (Amber), Quellen (Blau), Zettel (Grün)
- Knotengröße proportional zur Link-Anzahl
- Tap auf Knoten öffnet die Notiz

### KI-Integration (optional)
- Unterstützte Anbieter: OpenAI, Google Gemini, Anthropic Claude
- **Formatieren**: KI formatiert Notizen mit Markdown und korrigiert Grammatik
- **Tags vorschlagen**: KI schlägt passende Tags basierend auf dem Inhalt vor
- **Chat**: Frag die KI über deine Notizen – sie durchsucht deinen Zettelkasten
- API-Keys werden verschlüsselt gespeichert (EncryptedSharedPreferences)
- Ohne API-Key sind alle KI-Features komplett unsichtbar

### Dark Mode
- Folgt automatisch dem System-Setting
- Manueller Toggle im Header

## Tech-Stack

- Kotlin + Jetpack Compose + Material 3
- Room (SQLite) mit FTS4 Volltextsuche
- MVVM-Architektur mit Repository-Pattern
- Markdown-Dateien als Datenbasis (`filesDir/vault/`)
- OkHttp für LLM-API-Calls
- EncryptedSharedPreferences für API-Keys
- Kein Firebase, keine Server, keine Accounts

## APK herunterladen

1. Gehe zu **Actions** → **Build APK** → neuester erfolgreicher Lauf
2. Scrolle zu **Artifacts**
3. Lade **ZettelGeist-debug** herunter
4. Installiere die APK auf deinem Android-Gerät (minSdk 26 / Android 8.0)

## Selbst bauen

```bash
git clone https://github.com/papendick/zettelgeist.git
cd zettelgeist
chmod +x gradlew
./gradlew assembleDebug
```

Die APK findest du unter `app/build/outputs/apk/debug/app-debug.apk`.

## Lizenz

Privates Projekt.
