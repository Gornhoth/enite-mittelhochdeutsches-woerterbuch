<div align="center">
  <img src="src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" width="96">
  <h1>Enite – Mittelhochdeutsches Wörterbuch</h1>

  [![GitHub Release](https://img.shields.io/github/v/release/Gornhoth/enite-mittelhochdeutsches-woerterbuch)](https://github.com/Gornhoth/enite-mittelhochdeutsches-woerterbuch/releases)
  [![F-Droid](https://img.shields.io/f-droid/v/at.jschatteiner.enitemhdtranslator)](https://f-droid.org/packages/at.jschatteiner.enitemhdtranslator/)
  [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
</div>

Android-App zur Suche in mittelhochdeutschen Wörterbüchern über die [Wörterbuchnetz](https://www.woerterbuchnetz.de/)-API des Kompetenzzentrums für elektronische Erschließungs- und Publikationsverfahren in den Geisteswissenschaften an der Universität Trier.

## Screenshots

<p>
  <img src="screenshots/1.png" width="200">
  <img src="screenshots/2.png" width="200">
  <img src="screenshots/3.png" width="200">
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
