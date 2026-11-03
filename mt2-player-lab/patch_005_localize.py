#!/usr/bin/env python3
"""0.0.5: German settings for the TV lab; all app-brand labels say MarsTube.
Rewrites only UTF-8 string resources in the pinned, disposable reference checkout.
Never alters original NewTube source, attribution files, or MarsTube stable releases.
"""
from pathlib import Path
import re
from xml.sax.saxutils import escape
import sys

root = Path(sys.argv[1]).resolve()
if not (root / "sabr-media3").is_dir():
    raise SystemExit("Expected pinned disposable NewTube repository")

# Exact, reviewed translations in the same order as upstream strings_settings.xml.
# Indexing against upstream is deliberate: fail closed on source revisions.
translations = r"""
0|MarsTube neu starten, um diese Änderung zu übernehmen
1|Neu starten
2|Aus
3|Mehr
4|%1$s (Standard)
5|Löschen
6|Gelöscht
7|Auf diesem Gerät nicht verfügbar
8|%1$d Std. %2$d Min.
9|Konto
10|Nicht angemeldet · Melde dich an, um deine Abos zu sehen
11|Konto wechseln oder verwalten
12|App
13|Video und Audio
14|Sonstiges
15|Allgemein
16|Design, Sprache und Startseite
17|Design: %1$s, Sprache und Startseite
18|System
19|Hell
20|Dunkel
21|Menüs und Feeds
22|Bereiche, ausgeblendete Videos, Videomenü
23|Verlauf und Datenschutz
24|Wiedergabe- und Suchverlauf
25|Wiedergabe
26|Automatische Wiedergabe, Hintergrund, Tempo
27|Videoqualität
28|Untertitel
29|Darstellung, Größe und Position
30|Ein · überspringt Sponsoren und Intros
31|Bessere Titel und Vorschaubilder
32|Bessere Titel
33|Bessere Vorschaubilder
34|Sichern und Wiederherstellen
35|Einstellungen in einer Datei sichern
36|Erweitert
37|Streaming, Netzwerk und Fehlerbehebung
38|Über MarsTube
39|Sprache
40|Region
41|Startseite
42|Oberflächengröße
43|Suche sofort per Sprache starten
44|Beim Öffnen der Suche direkt zuhören
45|24-Stunden-Uhr verwenden
46|Für Startzeiten von Premieren und Livestreams
47|Menübereiche
48|Wähle die Bereiche für die untere Leiste und das Profilmenü
49|Untere Menüleiste
50|Profil
51|Halte einen Bereich gedrückt, um ihn zu verschieben, umzubenennen oder auszublenden.
52|Ausgeblendete Videos
53|Nichts ausgeblendet
54|Bereits angesehene Videos
55|Gesehene Videos auf der Startseite ausblenden
56|Gesehene Videos unter Abos ausblenden
57|Gesehene Videos aus Später ansehen entfernen
58|Entfernt ein Video nach dem vollständigen Ansehen aus Später ansehen
59|Nutzt deinen Wiedergabeverlauf und erfordert eine Anmeldung.
60|Anstehende Premieren und Livestreams
61|Auf der Startseite ausblenden
62|Unter Abos ausblenden
63|Auf Kanalseiten ausblenden
64|Livestreams
65|Livestreams unter Abos ausblenden
66|Videomenü
67|Einträge im Optionsmenü festlegen
68|Legt fest, welche Aktionen das Videomenü anbietet. Nur passende Aktionen werden angezeigt.
69|Wiedergabe
70|Zur Warteschlange hinzufügen
71|Speichern
72|In zuletzt verwendeter Wiedergabeliste speichern
73|Wiedergabeliste speichern oder entfernen
74|Kanal und Empfehlungen
75|Aus Abos ausblenden
76|Teilen und Details
77|Wiedergabelisten
78|Bereichsmenü
79|Nach oben oder unten verschieben
80|Standardreihenfolge
81|Setzt die Menüeinträge in die ursprüngliche Reihenfolge zurück
82|Menüreihenfolge zurückgesetzt
83|Vorschaubilder
84|Originales Vorschaubild
85|Ein Bild vom Videoanfang
86|Ein Bild aus der Videomitte
87|Ein Bild vom Videoende
88|Originaltitel
89|Titel in ihrer ursprünglichen Sprache statt als automatische Übersetzung anzeigen
90|Kanalreihenfolge im Profil
91|Zuletzt angesehene zuerst
92|Alphabetisch von A bis Z
93|Neue Videos zuerst
94|Angeheftete Kanäle zeigen
95|Die Startseite des Kanals
96|Nur die Videos des Kanals
97|Wiedergabelisten im Profil
98|Jede Wiedergabeliste einzeln
99|Alle Videos nacheinander
100|Wiedergabeverlauf
101|Kontoeinstellung übernehmen
102|Speichern, wenn der Verlauf im Konto aktiviert ist
103|Immer speichern
104|Aktiviert den Wiedergabeverlauf bei jedem MarsTube-Start erneut
105|Nicht speichern
106|MarsTube übermittelt keine angesehenen Videos für den Verlauf
107|Wiedergabeverlauf löschen
108|Entfernt angesehene Videos aus dem Verlauf deines Kontos
109|Wiedergabeverlauf löschen?
110|Alle Videos aus dem Wiedergabeverlauf werden entfernt. Dies kann nicht rückgängig gemacht werden.
111|Suchverlauf nicht speichern
112|Frühere Suchanfragen weder speichern noch vorschlagen
113|Suchverlauf löschen
114|Löscht die auf diesem Gerät gespeicherten Suchanfragen
115|Suchverlauf löschen?
116|Die auf diesem Gerät gespeicherten Suchanfragen werden gelöscht. Dies kann nicht rückgängig gemacht werden.
117|Jetzt sichern
118|Speichert die Einstellungen in %1$s
119|Sicherung gespeichert
120|Wiederherstellen
121|Aus einer Sicherung in %1$s
122|Automatische Sicherung
123|Nie
124|Eine Sicherung enthält die Einstellungen von MarsTube. Abos und Verlauf bleiben im jeweiligen Konto gespeichert.
125|Am Videoende
126|Nächstes Video abspielen
127|Video wiederholen
128|Wiedergabeliste zufällig abspielen
129|Wiedergabeliste beenden und stoppen
130|Wiedergabeliste rückwärts abspielen
131|Pausieren
132|Player schließen
133|Beim Verlassen der App
134|Video läuft in einem kleinen Fenster weiter
135|Nur der Ton wird weiter abgespielt
136|Wiedergabetempo merken
137|Für jeden Kanal
138|Für jedes Video
139|Gleich für alle Videos
140|Nicht merken
141|Bei jedem MarsTube-Start auf 1× zurücksetzen
142|Tempostufen unter Weitere Geschwindigkeiten
143|Kurze Auswahl
144|Feine Abstufungen
145|Sehr feine Abstufungen
146|Natürliche Stimmen bei anderem Tempo
147|Behält die normale Stimmlage bei schnellerer oder langsamerer Wiedergabe bei
148|Schlaftimer
149|Pausiert %1$s nach dem Öffnen eines Videos
150|Für andere Töne pausieren
151|Bei Anrufen oder Ton aus anderen Apps pausieren. Gilt ab dem nächsten Video
152|Fortsetzen
153|Kurze Videos fortsetzen
154|Videos unter sechs Minuten beginnen sonst wieder am Anfang
155|Livestreams fortsetzen
156|Livestream an der zuletzt gesehenen Stelle statt am Livepunkt fortsetzen
157|Unter dem Video
158|Weitere Videos ausblenden
159|Zeigt keine Videoempfehlungen unter dem Player
160|Dislike-Zahlen anzeigen
161|Über Return YouTube Dislike; dabei wird die Video-ID übertragen
162|Auf bezahlte Werbung hinweisen
163|Kurzer Hinweis, wenn ein Video eine bezahlte Produktplatzierung enthält
164|Legt die anfängliche Videoqualität fest. Pro Video im Player anpassbar.
165|Standardqualität
166|Automatisch (bis %1$dp)
167|Empfohlen. Beste zur Verbindung passende Qualität
168|Bis %1$s
169|Höherer Datenverbrauch
170|Geringerer Datenverbrauch
171|Spart Daten
172|Audio
173|Lautstärke angleichen
174|Regelt sehr laute Videos auf eine einheitliche Lautstärke herunter
175|Player-Lautstärke
176|Bei aktiviertem Android-Datensparmodus puffert MarsTube über mobile Daten weniger.
177|Darstellung
178|Größe
179|Abstand zum unteren Rand
180|Untertitel je Kanal merken
181|Aktivierte Untertitel gelten nur für diesen Kanal. Andernfalls gilt die zuletzt gewählte Untertitelsprache für alle Videos
182|Android-Untertiteleinstellungen
183|Werden bei der Systemdarstellung verwendet
184|Untertitel über die CC-Taste im Player aktivieren oder Sprache auswählen.
185|Überspringt von der Community markierte Videoabschnitte
186|Beim Beginn eines Abschnitts
187|Überspringen
188|Überspringen und Hinweis anzeigen
189|Nachfragen
190|Nicht überspringen
191|Markierungen in der Zeitleiste
192|%1$d von %2$d Arten markiert
193|Farbige Markierungen zeigen die Lage der Abschnitte, auch wenn diese nicht übersprungen werden.
194|Abschnitte kürzer als
195|Jeden Abschnitt nur einmal überspringen
196|Zurückgespulte übersprungene Abschnitte werden normal abgespielt
197|Bei Ladeproblemen der Abschnitte ausprobieren
198|Alternativen Server verwenden
199|Bessere Titel
200|Ersetzt reißerische Titel durch Community-Vorschläge
201|Bessere Vorschaubilder
202|Ersetzt Vorschaubilder durch Community-Auswahl
203|DeArrow erhält die IDs der in Listen angezeigten Videos.
204|Diese Optionen werden selten gebraucht. Sie verändern Streaming und Verbindung von MarsTube.
205|Streaming
206|Videoformat
207|Puffer
208|Etwa 25 Sekunden im Voraus. Geringster Speicherbedarf
209|Etwa 50 Sekunden im Voraus
210|Bis zu 75 Sekunden im Voraus
211|Bis zu zwei Minuten im Voraus. Höchster Speicherbedarf
212|Bei wiederholtem Laden die Qualität verringern
213|Reduziert die Qualität nach etwa 20 Sekunden Laden um eine Stufe. Abschalten kann bei VPN helfen
214|Formate mit hoher Bitrate
215|Aktiviert hochbitratige 1080p-Streams, sofern verfügbar. Erfordert eine zusätzliche Anfrage
216|SABR-Streaming (experimentell)
217|Neueres Streamingprotokoll. Gilt ab dem nächsten Video und verbraucht etwas weniger Daten
218|Einzeldatei-Streams
219|Spielt ältere Einzeldatei-Streams, meist 360p, ohne Qualitätswechsel. Nur für sehr langsame Geräte
220|Livestreams
221|Am Livepunkt starten
222|Startet ohne den üblichen Puffer von 15 Sekunden möglichst nah am Livepunkt
223|Livestreams über HLS abspielen
224|Kann auf langsamen Geräten flüssiger laufen. Kein Zurückspulen und maximal 1080p
225|Player
226|Bedienelemente beim nächsten Video anzeigen
227|Zeigt die Bedienelemente beim Start des nächsten Videos
228|Die gewählte Wiedergabeliste vollständig abspielen
229|Spielt beim Öffnen aus einer Wiedergabeliste die weiteren Videos in ihrer Reihenfolge
230|Warteschlange folgt der Einstellung für das Videoende
231|Bei Pausieren oder Player schließen hält auch die Warteschlange an
232|Kapitelhinweise
233|Zeigt den Namen jedes Kapitels beim Beginn. Antippen springt zum nächsten Kapitel
234|Netzwerk
235|Proxyserver
236|Leitet den MarsTube-Datenverkehr über einen HTTP- oder SOCKS-Proxy
237|Serververbindung und Namensauflösung
238|IPv4 bevorzugen
239|Standard. Hilft in Netzwerken mit fehlerhaftem IPv6
240|System-DNS des Geräts
241|Google DNS
242|Conscrypt
243|Interne Sicherheitsbibliothek für Verbindungen. Kann bei Netzwerkproblemen helfen
244|Konten
245|Getrennte Einstellungen je Konto
246|Jedes Konto verwendet eigene Einstellungen, zunächst die Standardwerte
247|Ältere Feeds
248|Lädt Abos, Kanäle und Wiedergabelisten wie die TV-App des Videodienstes. Bei leeren Listen ausprobieren
249|Version
250|Informiert über verfügbare neue Versionen
""".strip()

original = root / "smarttubetv/src/stmobile/res/values/strings_settings.xml"
xml = original.read_text(encoding="utf-8")
regex = re.compile(r'<string\s+name="([^"]+)"[^>]*>([\s\S]*?)</string>')
entries = regex.findall(xml)
translated = []
for line in translations.splitlines():
    n, value = line.split("|", 1)
    translated.append((int(n), value))
if len(entries) != 251 or len(translated) != 251:
    raise SystemExit(f"Unsafe upstream settings count: {len(entries)} / translations {len(translated)}")
for n, (number, value) in enumerate(translated):
    if number != n:
        raise SystemExit(f"Non-sequential German entry: {n} != {number}")
    original_formats = sorted(re.findall(r'%(?:\d+\$)?[dfs]', entries[n][1]))
    translated_formats = sorted(re.findall(r'%(?:\d+\$)?[dfs]', value))
    if original_formats != translated_formats:
        raise SystemExit(f"Format mismatch: {entries[n][0]}: {original_formats} != {translated_formats}")
    # The upstream resource is allowed to contain escaped percent and apostrophes.
    if "\n" in value:
        raise SystemExit(f"Invalid newline in value: {entries[n][0]}")
    translated[n] = (number, value.replace("'", "’"))

out = root / "smarttubetv/src/stmobile/res/values-de/strings_settings.xml"
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
               + "\n".join('    <string name="' + name + '">' + escape(value) + '</string>'
                           for (name, _), (_, value) in zip(entries, translated))
               + '\n</resources>\n', encoding="utf-8")

# These settings were not translated in the reference, and are therefore English in
# all nested pages. Maintain German search labels too.
search = root / "smarttubetv/src/stmobile/res/values/strings_settings_search.xml"
search_xml = search.read_text(encoding="utf-8")
lookup = {
    "mobile_settings_search_hint": "Einstellungen durchsuchen",
    "mobile_settings_search_none": "Keine Einstellungen zu %1$s gefunden",
    "mobile_settings_search_kw_account": "Konto anmelden abmelden Profil wechseln",
    "mobile_settings_search_kw_theme": "dunkel hell Design Farben Darstellung",
    "mobile_settings_search_kw_pure_black": "schwarz OLED Energiesparen",
    "mobile_settings_search_kw_country": "Land Region Standort",
    "mobile_settings_search_kw_start_screen": "Startseite Startbildschirm beim Start",
    "mobile_settings_search_kw_interface_size": "Oberfläche Größe Schrift vergrößern verkleinern",
    "mobile_settings_search_kw_tabs": "Menü Bereiche Navigation Start Abos",
    "mobile_settings_search_kw_hidden": "ausblenden gesehen verstecken Premieren live",
    "mobile_settings_search_kw_video_menu": "Videomenü Optionen Menüeinträge",
    "mobile_settings_search_kw_channels_order": "Kanäle Abos sortieren Reihenfolge",
    "mobile_settings_search_kw_watch_history": "Verlauf pausieren Datenschutz",
    "mobile_settings_search_kw_video_ends": "Autoplay Wiederholen zufällig nächstes Video",
    "mobile_settings_search_kw_background": "Hintergrund Miniplayer Bild im Bild Ton",
    "mobile_settings_search_kw_natural_voice": "Stimme Tonhöhe Tempo",
    "mobile_settings_search_kw_audio_focus": "Anrufe Töne andere Apps",
    "mobile_settings_search_kw_hide_related": "Empfehlungen weitere Videos verbergen",
    "mobile_settings_search_kw_dislikes": "Dislikes Bewertungen",
    "mobile_settings_search_kw_quality": "Qualität Auflösung 4k 1080p 720p HD Daten sparen",
    "mobile_settings_search_kw_loudness": "Lautstärke Normalisierung leise laut",
    "mobile_settings_search_kw_player_volume": "Lautstärke Ton",
    "mobile_settings_search_kw_captions": "Untertitel Sprache Größe",
    "mobile_settings_search_kw_sponsorblock": "Sponsorblock überspringen Intro",
    "mobile_settings_search_kw_dearrow": "Vorschaubilder Titel",
    "mobile_settings_search_kw_backup": "Sicherung exportieren importieren wiederherstellen",
    "mobile_settings_search_kw_video_format": "Format Codec VP9 AV1 H264 HDR",
    "mobile_settings_search_kw_buffer": "Puffer laden Speicher",
    "mobile_settings_search_kw_dns": "DNS IPv4 IPv6 Namensauflösung",
    "mobile_settings_search_kw_conscrypt": "SSL TLS Sicherheit Verbindung",
    "mobile_settings_search_kw_multi_profiles": "Konten Profile getrennte Einstellungen",
    "mobile_settings_search_kw_updates": "Update Version neue Veröffentlichung",
    "mobile_settings_search_kw_diagnostic_log": "Diagnose Fehlerprotokoll",
    "mobile_settings_search_kw_gesture_levels": "Gesten Helligkeit Lautstärke",
    "mobile_settings_search_kw_gesture_seek": "Gesten spulen vor zurück",
}
search_entries = regex.findall(search_xml)
if len(search_entries) != len(lookup):
    raise SystemExit(f"Settings search source changed: {len(search_entries)} vs {len(lookup)}")
missing = set(name for name, _ in search_entries) - lookup.keys()
if missing:
    raise SystemExit(f"Search translations missing: {missing}")
search_out = root / "smarttubetv/src/stmobile/res/values-de/strings_settings_search.xml"
search_out.write_text('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
                      + "\n".join('    <string name="' + name + '">' + escape(lookup[name]) + '</string>'
                                  for name, _ in search_entries)
                      + '\n</resources>\n', encoding="utf-8")

# Rename only UI strings (including fallback locale resources). Preserve package and
# Java identifiers, attribution sources, license files, and service-specific references.
changed = []
for base in [root / "smarttubetv/src/stmobile/res",
             root / "smarttubetv/src/main/res",
             root / "common/src/main/res"]:
    for path in base.rglob("strings*.xml"):
        if not path.parent.name.startswith("values"):
            continue
        value = path.read_text(encoding="utf-8")
        updated = value.replace("NewTube", "MarsTube").replace("MeTube", "MarsTube")
        if updated != value:
            path.write_text(updated, encoding="utf-8")
            changed.append(str(path.relative_to(root)))

# Unique version and independent package prevent debug-signing conflicts with 0.0.4 and 1.x.
gradle = root / "smarttubetv/build.gradle"
text = gradle.read_text(encoding="utf-8")
for old, new in [
    ('applicationId "de.equiummc.marstube2.tvui"', 'applicationId "de.equiummc.marstube2.v005"'),
    ('versionCode 11701', 'versionCode 12005'),
    ('versionName "1.17.1"', 'versionName "0.0.5"')
]:
    if text.count(old) != 1:
        raise SystemExit(f"Unsafe version identity anchor: {old} [{text.count(old)}]")
    text = text.replace(old, new, 1)
gradle.write_text(text, encoding="utf-8")
print("MarsTube 0.0.5: 251 German settings, 35 search translations,"
      f" {len(changed)} branding resources; independent de.equiummc.marstube2.v005.debug")
