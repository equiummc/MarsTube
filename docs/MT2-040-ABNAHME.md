# MarsTube 0.0.40 – Issue #72

Basis: `98bdb5fa42bdacc20354f2975e2805915833c964`, eigener Branch
`feature/mt2-040-tv-grid-profiles-20261114`. Kein Merge, kein öffentliches Release.

## Arbeitsumfang und tatsächlicher Produktionspfad

Die ARM32-Produktion entsteht weiterhin aus dem unveränderten Pin
`6f96a1b42bb9272935360d7e5aec34d35f2753f0` plus Patchkette 001–040.
Das ist die bestehende App `de.equiummc.marstube2`, kein zweites Projekt.
Die im Ticket genannten Leanback-Fragmente sind nicht der gerenderte stmobile-TV-Pfad.
Die gemeinsame RecyclerView in `MobileBrowseActivity` zeigt dort auch die vom
BrowsePresenter gelieferten TYPE_ROW-Inhalte. Deshalb werden ihre reale Größe,
Spaltenzahl und `VideoCardAdapter` korrigiert.

## Implementierung

- `patch_040_tv_consistency.py`, `MarsTvGrid040`, `MarsTvGridPolicy040`:
  fünf reguläre Seiten standardmäßig 3×2, Shorts 2×2. Größe aus gemessenem
  Inhaltsviewport, Text unter regulären Bildern bzw. rechts neben Hochkantbildern.
  FIT_CENTER im TV-Pfad, keine Änderung der Ergebnislisten/Pagination.
  Abo-Kanalfilter bleibt erhalten. Keine sichtbare dritte Rasterreihe am Seitenanfang.
- Bisher gespeicherte 4/6/9-Werte bleiben auf Datenträger erhalten. Der neue TV-Standard
  ist sechs; eine erneute ausdrückliche Auswahl aktiviert 4/6/9 über denselben
  Einstellungswert für die fünf regulären TV-Seiten. Shorts bleiben vier.
- `MarsKnightRider040`: schmaler blauer Verlauf, reversierende Animation,
  300 ms Entprellung. An vorhandene LoadingManager→showProgressBar-Aufrufe
  gebunden, auch bei Pagination. Kein Thumbnail-Trigger und keine Fokusfläche.
  Stopp bei Load-Ende, Seitenwechsel, Pause, Fensterverdeckung und Detach.
  Die getrennte 5-Sekunden-Regel des Shorts-Players bleibt unverändert.
- Profilroute öffnet direkt `MarsTvProfiles038` mit echten Service-Accounts.
  Veraltete verzögerte Grid-Fokusanfragen werden durch Epochennummer und
  Dialogstatus verworfen. Links/Rechts verlassen keine Auswahlzeile;
  Hoch/Runter/OK/BACK bleiben native Dialognavigation.
- Einheitliches Hauptmenü: Shorts Slot 4, Startseite Slot 5. SidebarService
  migriert nur das berichtete alte Slotpaar 4/5 und speichert pro Profil einen
  Migrationsmarker, ohne andere Pins oder den Startbereich zu überschreiben.
- Bestätigter Codepfad: `MobilePlaybackActivity.onStop` konnte die
  Telefon-Hintergrundaudiologik auf TV weiterführen. TV ohne sichtbaren
  Miniplayer/PiP pausiert jetzt. Auch der versteckte Browse-Miniplayer wird
  bei Prozess-Hintergrundstatus pausiert. Player-BACK und sichtbarer Mini
  bleiben erhalten. KEEP_SCREEN_ON wurde bereits bei onStop aufgehoben.
- Linux-Protobuf-Hardcodierung in zwei bestehenden Gradle-Modulen auf
  Windows/Linux-Auswahl umgestellt. Keine Dependency-Version angehoben.

## Backend-Grenze und CEC

`MediaServiceManager.loadAccounts` reicht `mSignInService.getAccounts()` durch.
Der gepinnte `OAuth2AccountManager` liefert gespeicherte Anmeldeidentitäten;
seine zusätzliche Unterkanal-Abfrage ist auskommentiert. Nur diese tatsächlich
angebotenen Identitäten werden angezeigt. Eine vollständige automatische
Auflistung aller Brand-/Unterkanäle eines Google-Kontos ist damit **offen**.
Keine erfundenen Profile und kein Login-Bypass.

Es gibt keinen Nachweis, dass MarsTube den Thomson per HDMI-CEC aufweckt.
Keine OS-/CEC-Einstellung verändert. Ein geräteübergreifender A/B-Test mit
ausgeschaltetem Fire-TV-CEC bzw. getrenntem HDMI bleibt offen.

## Geplante Prüfung und Nachweise

1. Vollständige Patch-Anwendung; Python- und PS7-Syntax.
2. SABR-, App- und Common-Unit-Tests, signiertes ARM32-Release, Lint.
3. GitHub Actions für exakt den Quellcommit; Paket, Version 12040/0.0.40,
   Originalzertifikat und ABI-Prüfung, SHA-256.
4. AFTR/raven und Altversion 12039 vor Installation bestätigt. Vorhandene APK
   lokal gesichert und Signatur `f80f712f9dbbd16c692e5d5eaff7598db73a660c82911bfd3ff0d0215c8f84e9`
   verifiziert. Ausschließlich `adb install -r` nach vollständiger Prüfung.
5. Physische D-Pad-/Raster-/Profil-/Playback-/HOME-Rückkehrtests separat protokollieren.
   Kein Datenlöschen, kein Downgrade, keine Deinstallation. APK-Sicherung ist
   keine Sicherung privater Kontodaten; Androids Updateverfahren erhält den Datenbereich.

Status dieser Datei bei Erstellung: lokale Kompilierung/Tests laufen;
CI und Installation noch nicht abgeschlossen. Endstand in `PROJEKTSTATUS.md`.
