<div align="center">
  <img src="src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" width="96" alt="Enite App Icon">
  <h1>Enite – Mittelhochdeutsches Wörterbuch</h1>

  [![GitHub Release](https://img.shields.io/github/v/release/Gornhoth/enite-mittelhochdeutsches-woerterbuch)](https://github.com/Gornhoth/enite-mittelhochdeutsches-woerterbuch/releases)
  [![Google Play](https://img.shields.io/badge/Google_Play-Enite-3DDC84?logo=google-play&logoColor=white)](https://play.google.com/store/apps/details?id=at.jschatteiner.enitemhdtranslator)
  [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
</div>

Android-App zur Suche in mittelhochdeutschen Wörterbüchern über die [Wörterbuchnetz](https://www.woerterbuchnetz.de/)-API des Kompetenzzentrums für elektronische Erschließungs- und Publikationsverfahren in den Geisteswissenschaften an der Universität Trier.

## Screenshots

<p>
  <img src="screenshots/1.png" width="200" alt="Screenshot Stichwortsuche">
  <img src="screenshots/2.png" width="200" alt="Screenshot Autovervollständigung">
  <img src="screenshots/3.png" width="200" alt="Screenshot Suchergebnisse">
  <img src="screenshots/4.png" width="200" alt="Screenshot Wörterbuchartikel">
</p>

## Funktionen

- **Stichwortsuche** — Direkte Suche nach MHD-Lemmata im Lexer
- **Definitionssuche** — Deutsche Begriffe in den Definitionen aller verlinkten Wörterbücher finden (Lexer, BMZ, FindeB, MWB, AWB, DWB u. a.)
- **Wörterbuchartikel** — Vollständige Artikeltexte mit Formatierung und Quellenverweisen
- **Autovervollständigung** — Vorschläge während der Eingabe
- **Dark Mode** — System-, Hell- und Dunkelmodus

## Voraussetzungen

- Android 6.0 (API 23) oder höher
- Internetverbindung

## Bauen

Für eine signierte Release-APK:

```bash
export KEYSTORE_PASSWORD="..."
export KEY_PASSWORD="..."
./gradlew assembleRelease
```

## Hinweise

Die Wörterbuchdaten werden über die öffentliche API des [Wörterbuchnetzes](https://www.woerterbuchnetz.de/) abgerufen. Diese App ist ein unabhängiges Open-Source-Projekt und steht in keiner Verbindung zur Universität Trier oder zum Kompetenzzentrum.

## Datenschutz

[Datenschutzerklärung / Privacy Policy](PRIVACY.md)

## Lizenz

[MIT](LICENSE)

## Unterstützen

Wenn dir die App gefällt würde ich mich über eine kleine Unterstützung freuen ☕:

[![Ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/johannesschatteiner)
