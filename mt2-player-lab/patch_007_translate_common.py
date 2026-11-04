#!/usr/bin/env python3
"""MT2-007 missing shared German strings, without changing existing translations.

The new stmobile translations already cover its three own resources.
This fills missing UI and diagnostic labels from common/. Source and licence
URLs stay unchanged and legally attributed to their original projects.
"""
from pathlib import Path
from xml.sax.saxutils import escape
import re
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_007_translate_common.py <pinned checkout>")
root = Path(sys.argv[1]).resolve()
values = root / "common/src/main/res/values/strings.xml"
german = root / "common/src/main/res/values-de/strings.xml"
if not (values.is_file() and german.is_file() and (root/"sabr-media3").is_dir()):
    raise SystemExit("Fail closed: shared resources not found")
translations = {
    "header_downloads": "Downloads",
    "dialog_download": "Herunterladen",
    "msg_sign_in_to_save": "Melde dich an, um Videos in Wiedergabelisten zu speichern",
    "about_app": "Über %1$s",
    "about_source_code": "Quellcode",
    "about_license": "Lizenz",
    "diagnostic_log_send": "Diagnoseprotokoll teilen",
    "diagnostic_log_send_desc": "Für Fehlermeldungen. Enthält geöffnete Videos und Kanäle; Passwörter und Anmeldetoken werden entfernt.",
    "diagnostic_log_subject": "%1$s %2$s Diagnoseprotokoll",
    "diagnostic_log_failed": "Diagnoseprotokoll konnte nicht erstellt werden",
    "signed_in_success": "Angemeldet",
    "feedback_failed": "Die Rückmeldung konnte nicht an YouTube gesendet werden. Erneut versuchen.",
    "subscribed_to_channel_name": "%1$s abonniert",
    "unsubscribed_from_channel_name": "Abo von %1$s beendet",
    "subscribe_failed": "Kanal konnte nicht abonniert werden. Erneut versuchen.",
    "unsubscribe_failed": "Abo konnte nicht beendet werden. Erneut versuchen.",
    "action_undo": "Rückgängig",
    "msg_player_no_connection_retry": "Keine Verbindung. MarsTube versucht es wieder, sobald du online bist. Oder Wiedergabe drücken.",
    "msg_player_no_connection_short": "Keine Verbindung – neuer Versuch läuft …",
    "msg_player_no_connection_tap": "Keine Verbindung – Wiedergabe drücken, um es erneut zu versuchen",
    "player_unexpected_error": "Unerwarteter Wiedergabefehler",
    "msg_quality_pin_fallback": "%1$s funktioniert nicht – dieses Video wird automatisch eingestellt",
    "msg_audio_pin_fallback": "Audiotrack funktioniert nicht – dieses Video verwendet die Standardtonspur",
    "update_check_failed": "Updates konnten nicht geprüft werden. Später erneut versuchen.",
    "playlist_name_hint": "Name der Wiedergabeliste",
    "remove_from_search_history": "Aus dem Suchverlauf entfernen",
    "sidebar_notification": "Hinweis in der Seitenleiste anzeigen",
    "player_return_youtube_dislike": "Dislike-Zahlen (Return YouTube Dislike)",
    "color_scheme_dark_blue_oled": "Dunkelblau (OLED)",
    "header_live": "Live",
    "player_time_stretching": "Ton bei anderem Wiedergabetempo",
    "player_time_stretching_desc": "Stimmen klingen bei verändertem Tempo natürlicher. Auf einigen Geräten sinkt dadurch die Leistung deutlich.",
    "queue_respects_playback_mode": "Warteschlange berücksichtigt den Wiedergabemodus",
    "ignore_short_segments": "Kurze Abschnitte ignorieren",
    "install_bridge": "ATV-/Amazon-Brücke installieren",
    "fix_empty_subs_and_channels": "Leere Abos und Kanallisten reparieren",
    "local_backup_not_supported": "Automatische Sicherungen werden auf Android TV 11 und neuer wegen Systembeschränkungen nicht unterstützt. Stattdessen Google Drive nutzen.",
    "rounded_card_corners": "Abgerundete Karten",
    "app_restore_desc": "Die Sicherungsdatei (ZIP) muss in diesen Ordner kopiert werden. Alternativ im Dateimanager mit „Öffnen mit“ auswählen.",
    "app_backup_desc": "Die Sicherung wird als ZIP-Datei erstellt.",
    "add_video_to_watch_later": "Für Später ansehen speichern",
    "enable_conscrypt": "Conscrypt aktivieren",
    "enable_conscrypt_desc": "Verbessert auf manchen Geräten die HTTPS-, TLS- und VPN-Kompatibilität, kann jedoch Verbindungsprobleme oder langsamere Wiedergabe verursachen.",
    "network_settings": "Netzwerkeinstellungen",
    "comments_disabled": "Kommentare sind für dieses Video deaktiviert",
}
# Keep third-party attribution pointers and the license name byte-for-byte.
source = values.read_text(encoding="utf-8")
translated = german.read_text(encoding="utf-8")
def names(text):
    return set(re.findall(r'<string\s+name="([^"]+)"', text))
before = names(source)
already = names(translated)
unknown = set(translations) - before
conflicts = set(translations) & already
if unknown or conflicts or len(translations) != 45:
    raise SystemExit(f"Unexpected shared strings: unknown={unknown}, conflicts={conflicts}, count={len(translations)}")
missing = before - already
untouched = {"about_source_code_url","about_license_name","about_license_url"}
if missing != set(translations) | untouched:
    raise SystemExit(f"Shared English entries drifted; refusing to guess: {sorted(missing - set(translations) - untouched)}")
origin_values = dict(re.findall(r'<string\s+name="([^"]+)"[^>]*>(.*?)</string>',source, re.DOTALL))
out = root/"common/src/main/res/values-de/mars_strings_extra.xml"
if out.exists():
    raise SystemExit("Destination already exists: " + str(out))
for key,value in translations.items():
    formats = lambda v: sorted(re.findall(r'%(?:\d+\$)?[dfs]',v))
    if formats(origin_values[key]) != formats(value):
        raise SystemExit(f"Placeholder mismatch: {key}")
out.write_text('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
               + '\n'.join('    <string name="' + k + '">' + escape(v) + '</string>'
                           for k,v in translations.items())
               + '\n</resources>\n', encoding="utf-8")
print(f"MT2-007: localized {len(translations)} previously untranslated shared strings; kept {len(untouched)} original license/source values.")
