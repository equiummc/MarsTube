package com.newtube.mobile.ui.settings;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liskovsoft.mediaserviceinterfaces.oauth.Account;
import com.liskovsoft.sharedutils.helpers.AppInfoHelpers;
import com.liskovsoft.smartyoutubetv2.common.misc.DiagnosticLog;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerTweaksData;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.newtube.mobile.player.MarsTvPlayerPrefs;
import com.newtube.mobile.player.SabrSourcePreference;
import com.newtube.mobile.ui.browse.AccountsSheet;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * MarsTube 0.0.10 TV settings tree.
 *
 * <p>One central home for every TV option, in the order a remote user expects:
 * Video &amp; Wiedergabe, Darstellung, Bedienung/Fernbedienung, Untertitel,
 * Streaming/Netzwerk, Profil/Konto and Info. The phone tree is untouched and is
 * still reached through {@link SettingsPages#buildPhone(Context, String)} for
 * pages this class does not own.</p>
 *
 * <p>No option is duplicated: a value lives on exactly one page. Legacy pages
 * that still carry real data (SponsorBlock, DeArrow, Backup, Advanced) are
 * linked below "Weitere Einstellungen" instead of being silently dropped.</p>
 */
public final class MarsTvSettingsPages {
    public static final String TV_VIDEO = "tv_video";
    public static final String TV_APPEARANCE = "tv_appearance";
    public static final String TV_REMOTE = "tv_remote";
    public static final String TV_SUBTITLES = "tv_subtitles";
    public static final String TV_STREAMING = "tv_streaming";
    public static final String TV_PROFILE = "tv_profile";
    public static final String TV_INFO = "tv_info";
    public static final String TV_CONTENT_TOOLS = "tv_content_tools";

    private MarsTvSettingsPages() {
    }

    @NonNull
    static SettingsPages.Page build(@NonNull Context context, @NonNull String id) {
        switch (id) {
            case SettingsPages.ROOT:
                return root(context);
            case TV_VIDEO:
                return video(context);
            case TV_APPEARANCE:
                return appearance(context);
            case TV_REMOTE:
                return remote(context);
            case TV_SUBTITLES:
                return subtitles(context);
            case TV_STREAMING:
                return streaming(context);
            case TV_PROFILE:
                return appearance(context);
            case TV_CONTENT_TOOLS:
                return contentTools(context);
            case SettingsPages.ADVANCED:
                return advanced(context);
            case TV_INFO:
                return info(context);
            default:
                // Legacy page (SponsorBlock, DeArrow, Backup, Advanced, ...): keep its
                // existing rows and stored data, only the TV root entry point is new.
                return SettingsPages.buildPhone(context, id);
        }
    }

    private static SettingsPages.Page root(Context context) {
        List<SettingsRow> rows = new ArrayList<>();

        Account account = MediaServiceManager.instance().getSelectedAccount();
        boolean signedIn = account != null && !account.isEmpty();
        rows.add(SettingsRow.action(R.drawable.ic_settings_account,
                signedIn && account.getName() != null ? account.getName()
                        : context.getString(R.string.mobile_settings_account),
                () -> signedIn ? (account.getEmail() != null ? account.getEmail()
                        : context.getString(R.string.mobile_settings_account_switch))
                        : context.getString(R.string.mobile_settings_account_signed_out),
                page -> AccountsSheet.show(page.requireActivity(), page::rebuild)));

        rows.add(SettingsRow.header("Wiedergabe"));
        rows.add(SettingsRow.page(R.drawable.ic_settings_playback, "Video & Wiedergabe",
                videoSummary(context), TV_VIDEO));
        rows.add(SettingsRow.page(R.drawable.ic_settings_quality, "Streaming & Netzwerk",
                streamingSummary(context), TV_STREAMING));
        rows.add(SettingsRow.header("MarsTube-Oberfläche"));
        rows.add(SettingsRow.page(R.drawable.ic_settings_feeds, "Darstellung",
                "Startseite, Shorts, Profil, Verlauf und Abos", TV_APPEARANCE));
        rows.add(SettingsRow.page(R.drawable.ic_settings_general, "Bedienung & Fernbedienung",
                "Tasten, Playersteuerung und Miniplayer", TV_REMOTE));
        rows.add(SettingsRow.page(R.drawable.ic_settings_sponsorblock,
                "SponsorBlock & DeArrow", "Videosegmente, Vorschaubilder und Titel",
                TV_CONTENT_TOOLS));
        rows.add(SettingsRow.header("System"));
        rows.add(SettingsRow.page(R.drawable.ic_settings_backup, "Sichern & Wiederherstellen",
                context.getString(R.string.mobile_settings_backup_summary), SettingsPages.BACKUP));
        rows.add(SettingsRow.page(R.drawable.ic_settings_advanced, "Erweitert",
                "Netzwerk, Kompatibilität und zusätzliche Funktionen", SettingsPages.ADVANCED));
        rows.add(SettingsRow.action(R.drawable.ic_settings_about,
                versionLine(context), () -> "Auf Updates prüfen",
                page -> com.newtube.mobile.ui.update.MobileUpdateActivity.startCheck(page.requireContext())));
        rows.add(SettingsRow.page(R.drawable.ic_settings_about, "Informationen & Diagnose",
                "GitHub, Protokoll und Open-Source-Lizenzen", TV_INFO));

        return new SettingsPages.Page("Einstellungen", rows);
    }

    private static SettingsPages.Page video(Context context) {
        List<SettingsRow> rows = new ArrayList<>();
        rows.add(SettingsRow.note("Alle Werte sind Präferenzen bzw. Obergrenzen. "
                + "Ob eine Auflösung, Bildrate oder Bitrate tatsächlich angeboten wird, "
                + "hängt vom Video und den Decodern dieses Geräts ab."));

        rows.add(SettingsRow.header("Bild"));
        rows.add(intChoice(context, "Maximale Auflösung",
                "Obergrenze; Auto nutzt die Gerätevorgabe.",
                () -> MarsTvPlayerPrefs.getResolutionCeiling(context),
                value -> MarsTvPlayerPrefs.setResolutionCeiling(context, value),
                new String[]{"Auto", "2160p (4K)", "1440p", "1080p", "720p", "480p"},
                new int[]{MarsTvPlayerPrefs.RES_AUTO, MarsTvPlayerPrefs.RES_2160,
                        MarsTvPlayerPrefs.RES_1440, MarsTvPlayerPrefs.RES_1080,
                        MarsTvPlayerPrefs.RES_720, MarsTvPlayerPrefs.RES_480}));

        rows.add(intChoice(context, "Maximale Bildrate",
                "Obergrenze; Auto überlässt die Wahl dem Player.",
                () -> MarsTvPlayerPrefs.getMaxFps(context),
                value -> MarsTvPlayerPrefs.setMaxFps(context, value),
                new String[]{"Auto", "60 fps", "30 fps"},
                new int[]{MarsTvPlayerPrefs.FPS_AUTO, MarsTvPlayerPrefs.FPS_60,
                        MarsTvPlayerPrefs.FPS_30}));

        rows.add(stringChoice(context, "Bevorzugter Codec",
                "Weiche Präferenz; nicht jeder Codec ist auf jedem Gerät verfügbar.",
                () -> MarsTvPlayerPrefs.getPreferredCodec(context),
                value -> MarsTvPlayerPrefs.setPreferredCodec(context, value),
                new String[]{"Auto", "H.264", "VP9", "AV1"},
                new String[]{MarsTvPlayerPrefs.CODEC_AUTO, MarsTvPlayerPrefs.CODEC_H264,
                        MarsTvPlayerPrefs.CODEC_VP9, MarsTvPlayerPrefs.CODEC_AV1}));

        rows.add(intChoice(context, "HDR",
                "Aus ist eine bestmögliche SDR-Bevorzugung; der verfügbare Track kann abweichen.",
                () -> MarsTvPlayerPrefs.getHdrMode(context),
                value -> MarsTvPlayerPrefs.setHdrMode(context, value),
                new String[]{"Auto", "Aus"},
                new int[]{MarsTvPlayerPrefs.HDR_AUTO, MarsTvPlayerPrefs.HDR_OFF}));

        rows.add(SettingsRow.header("Daten & Start"));
        rows.add(intChoice(context, "Bitratenlimit",
                "Obergrenze pro Video; Auto bedeutet kein zusätzliches Limit.",
                () -> MarsTvPlayerPrefs.getBitrateLimitMbps(context),
                value -> MarsTvPlayerPrefs.setBitrateLimitMbps(context, value),
                new String[]{"Auto", "3 Mbit/s", "5 Mbit/s", "10 Mbit/s", "15 Mbit/s", "25 Mbit/s"},
                new int[]{MarsTvPlayerPrefs.BITRATE_AUTO, 3, 5, 10, 15, 25}));

        rows.add(intChoice(context, "Puffer",
                "Normal ist die Vorgabe und schützt kleine TV-Geräte vor unnötigem Speicherverbrauch.",
                () -> MarsTvPlayerPrefs.getBufferPreset(context),
                value -> MarsTvPlayerPrefs.setBufferPreset(context, value),
                new String[]{"Klein", "Normal", "Groß"},
                new int[]{MarsTvPlayerPrefs.BUFFER_LOW, MarsTvPlayerPrefs.BUFFER_NORMAL,
                        MarsTvPlayerPrefs.BUFFER_HIGH}));

        rows.add(intChoice(context, "Video-Start",
                "Qualität zuerst wartet etwas länger auf einen stabilen Start; Schnellstart startet früher.",
                () -> MarsTvPlayerPrefs.getStartStrategy(context),
                value -> MarsTvPlayerPrefs.setStartStrategy(context, value),
                new String[]{"Qualität zuerst", "Schnellstart"},
                new int[]{MarsTvPlayerPrefs.START_QUALITY_FIRST, MarsTvPlayerPrefs.START_FAST}));

        PlayerTweaksData tweaks = PlayerTweaksData.instance(context);
        rows.add(SettingsRow.header("Qualität & Kompatibilität"));
        rows.add(SettingsRow.toggle("Bei Ladeproblemen Qualität reduzieren",
                "Nach einer längeren Pufferpause eine niedrigere Qualitätsstufe versuchen.",
                () -> !tweaks.isNetworkErrorFixingDisabled(),
                on -> tweaks.setNetworkErrorFixingDisabled(!on)));
        rows.add(SettingsRow.toggle("Formate mit höherer Bitrate anfragen",
                "Zusätzliche hochwertige Streams verwenden, falls der Dienst sie anbietet.",
                tweaks::isHighBitrateFormatsEnabled, tweaks::setHighBitrateFormatsEnabled));
        rows.add(SettingsRow.header("Livestreams"));
        rows.add(SettingsRow.toggle("Näher an der Live-Position starten",
                "Mit kleinerem Live-Puffer; auf schwachen Verbindungen kann es öfter nachladen.",
                tweaks::isBufferOnStreamsDisabled, tweaks::setBufferOnStreamsDisabled));
        rows.add(SettingsRow.toggle("HLS für Livestreams bevorzugen",
                "Kompatibilitätsoption, falls die normale Live-Wiedergabe nicht funktioniert.",
                tweaks::isHlsStreamsForced, tweaks::setHlsStreamsForced));
        rows.add(SettingsRow.header("Untertitel"));
        rows.add(intChoice(context, "Untertitel beim Videostart",
                "Die CC-Taste im Player bleibt für jedes Video verfügbar.",
                () -> MarsTvPlayerPrefs.getSubtitlePolicy(context),
                value -> MarsTvPlayerPrefs.setSubtitlePolicy(context, value),
                new String[]{"Aus", "Nur erzwungene", "Reguläre Untertitel"},
                new int[]{MarsTvPlayerPrefs.SUBTITLES_OFF, MarsTvPlayerPrefs.SUBTITLES_FORCED_ONLY,
                        MarsTvPlayerPrefs.SUBTITLES_REGULAR}));
        return new SettingsPages.Page("Video & Wiedergabe", rows);
    }

    private static SettingsPages.Page appearance(Context context) {
        List<SettingsRow> rows = new ArrayList<>();
        rows.add(SettingsRow.note("TV- und Smartphone-Layouts sind getrennt. "
                + "Diese Werte gelten nur auf Android TV; die Touch-Oberfläche bleibt unverändert."));

        rows.add(SettingsRow.header("Startseite"));
        rows.add(intChoice(context, "Kacheln pro Zeile",
                "Vier, sechs oder neun Kacheln; die Karten bleiben im 16:9-Raster.",
                () -> MarsTvPlayerPrefs.getHomeTileCount(context),
                value -> MarsTvPlayerPrefs.setHomeTileCount(context, value),
                new String[]{"4 Kacheln", "6 Kacheln", "9 Kacheln"},
                new int[]{MarsTvPlayerPrefs.TILES_4, MarsTvPlayerPrefs.TILES_6,
                        MarsTvPlayerPrefs.TILES_9}));

        rows.add(intChoice(context, "Vorschaukarten im unteren Laufband",
                "Anzahl der Empfehlungen im Player; weniger Karten sparen Speicher.",
                () -> MarsTvPlayerPrefs.getCarouselPreviewCount(context),
                value -> MarsTvPlayerPrefs.setCarouselPreviewCount(context, value),
                new String[]{"4", "6", "8", "10"},
                new int[]{4, 6, 8, 10}));

        rows.add(SettingsRow.header("Shorts"));
        rows.add(SettingsRow.note("Zwei abgerundete Vorschauen links, ein mittig "
                + "positioniertes 9:16-Video und eine schmale Aktionsleiste rechts."));
        rows.add(SettingsRow.toggle("Shorts automatisch weiter",
                "Nach Ende eines Shorts automatisch das nächste Video abspielen.",
                () -> context.getSharedPreferences("mars_tv_shorts", Context.MODE_PRIVATE)
                        .getBoolean("auto_next", true),
                value -> context.getSharedPreferences("mars_tv_shorts", Context.MODE_PRIVATE)
                        .edit().putBoolean("auto_next", value).apply()));

        rows.add(SettingsRow.header("Profil, Verlauf & Abos"));
        rows.add(SettingsRow.toggle("Profil als Raster",
                "Konten und Profilbereiche als Kacheln statt Liste.",
                () -> MarsTvPlayerPrefs.isProfileGrid(context),
                value -> MarsTvPlayerPrefs.setProfileGrid(context, value)));
        rows.add(SettingsRow.toggle("Verlauf als 3×3-Raster",
                "Drei Spalten auf TV; auf kleinen Geräten bleibt es beim vorhandenen Raster.",
                () -> MarsTvPlayerPrefs.isHistoryGrid(context),
                value -> MarsTvPlayerPrefs.setHistoryGrid(context, value)));
        rows.add(SettingsRow.toggle("Abos als Raster",
                "Abos in derselben Kartenform wie Startseite und Verlauf.",
                () -> MarsTvPlayerPrefs.isSubscriptionsGrid(context),
                value -> MarsTvPlayerPrefs.setSubscriptionsGrid(context, value)));
        rows.add(SettingsRow.toggle("Ein Video pro Kanal in Abos",
                "Kanalfilter: zeigt pro Kanal nur das neueste Video, damit Abos übersichtlich bleiben.",
                () -> MarsTvPlayerPrefs.isSubscriptionsChannelFilter(context),
                value -> MarsTvPlayerPrefs.setSubscriptionsChannelFilter(context, value)));
        return new SettingsPages.Page("Darstellung", rows);
    }

    private static SettingsPages.Page remote(Context context) {
        List<SettingsRow> rows = new ArrayList<>();
        rows.add(SettingsRow.header("Fernbedienung"));
        rows.add(SettingsRow.note("BACK fokussiert auf jeder TV-Seite die linke Navigation, "
                + "auch aus Shorts, aus dem Videoplayer und aus Untermenüs. "
                + "Im Player läuft das Video währenddessen weiter. "
                + "Rechts aus der Navigation öffnet die Inhalte der aktuellen Seite."));
        rows.add(SettingsRow.note("OK kurz: Wiedergabe/Pause. OK gehalten: vorübergehend 2×, "
                + "beim Loslassen sicher zurück auf 1×. Kein Pause-Loop."));
        rows.add(SettingsRow.note("Shorts: zwei Vorschauen links, Like/Dislike rechts. "
                + "Links/Rechts wechseln zwischen Vorschau, Video und Reaktionen; "
                + "auf dem Fortschrittsbalken kann gespult werden."));
        rows.add(SettingsRow.note("Shorts haben eine eigene Fernbedienungsmaske: "
                + "Hoch/Runter = vorheriger/nächster Short, Links/Rechts = 10 Sekunden spulen, "
                + "OK kurz = Pause/Weiter, OK gehalten = vorübergehend 2×. MENU öffnet "
                + "die Navigation; die normalen Videokarten werden ausgeblendet."));
        rows.add(SettingsRow.toggle("Miniplayer",
                "Nur anzeigen, wenn eine Wiedergabe aktiv ist; bei ausgeschaltetem Player "
                        + "gibt es keinen leeren Menüpunkt.",
                () -> MarsTvPlayerPrefs.isMiniPlayerEnabled(context),
                value -> MarsTvPlayerPrefs.setMiniPlayerEnabled(context, value)));
        rows.add(SettingsRow.note("Downloads werden auf Android TV nicht angeboten, "
                + "auch wenn ein altes verborgenes Menü einen anderen Zustand speichert."));
        return new SettingsPages.Page("Bedienung / Fernbedienung", rows);
    }

    private static SettingsPages.Page subtitles(Context context) {
        List<SettingsRow> rows = new ArrayList<>();
        rows.add(SettingsRow.header("Untertitel"));
        rows.add(intChoice(context, "Untertitel-Modus",
                "Nur erzwungene Untertitel aktivieren ausschließlich explizit vorhandene "
                        + "forced-Tracks. Fehlen sie, bleibt der Ton ohne beliebige Untertitel.",
                () -> MarsTvPlayerPrefs.getSubtitlePolicy(context),
                value -> MarsTvPlayerPrefs.setSubtitlePolicy(context, value),
                new String[]{"Aus", "Nur erzwungene Übersetzungsuntertitel", "Reguläre Untertitel"},
                new int[]{MarsTvPlayerPrefs.SUBTITLES_OFF,
                        MarsTvPlayerPrefs.SUBTITLES_FORCED_ONLY,
                        MarsTvPlayerPrefs.SUBTITLES_REGULAR}));
        rows.add(SettingsRow.note("Die Auswahl wirkt ab dem nächsten Video. "
                + "Der CC-Knopf im Player kann Untertitel weiterhin manuell umschalten."));
        return new SettingsPages.Page("Untertitel", rows);
    }

    private static SettingsPages.Page streaming(Context context) {
        List<SettingsRow> rows = new ArrayList<>();
        rows.add(SettingsRow.header("Wiedergabequelle"));
        rows.add(SettingsRow.toggle("SABR bevorzugen (experimentell)",
                "Wirkt ab dem nächsten Video. SABR ist kein automatisches Datensparen: "
                        + "es kann Startzeit kosten und wird nur bei tatsächlichem Fehler "
                        + "auf DASH zurückgesetzt.",
                () -> SabrSourcePreference.isPreferred(context),
                value -> SabrSourcePreference.setPreferred(context, value)));
        rows.add(SettingsRow.note("Das tatsächlich genutzte Protokoll, einen eventuellen "
                + "SABR-Fallback und die aktuellen Formatwerte zeigt der Info-Knopf im Player."));
        rows.add(SettingsRow.note("Automatischer Quellen-Fallback ist nur bei einer echten "
                + "Fehlfunktion aktiv und bleibt in den Videoinfos sichtbar."));
        return new SettingsPages.Page("Streaming & Netzwerk", rows);
    }

    private static SettingsPages.Page contentTools(Context context) {
        List<SettingsRow> rows = new ArrayList<>();
        rows.add(SettingsRow.page(R.drawable.ic_settings_sponsorblock, "SponsorBlock",
                "Markierte Videosegmente automatisch überspringen", SettingsPages.SPONSORBLOCK));
        rows.add(SettingsRow.page(R.drawable.ic_settings_dearrow, "DeArrow",
                "Alternative Titel und Vorschaubilder", SettingsPages.DEARROW));
        return new SettingsPages.Page("SponsorBlock & DeArrow", rows);
    }

    /** Technical legacy options without a second copy of video/streaming prefs. */
    private static SettingsPages.Page advanced(Context context) {
        SettingsPages.Page legacy = SettingsPages.buildPhone(context, SettingsPages.ADVANCED);
        List<SettingsRow> rows = new ArrayList<>();
        String videoHeader = context.getString(R.string.mobile_settings_streaming);
        String liveHeader = context.getString(R.string.mobile_settings_live);
        boolean hiddenSection = false;
        for (SettingsRow row : legacy.rows) {
            if (row.kind == SettingsRow.KIND_HEADER) {
                String title = String.valueOf(row.title);
                hiddenSection = videoHeader.equals(title) || liveHeader.equals(title);
            }
            if (!hiddenSection) rows.add(row);
        }
        rows.add(SettingsRow.header("Kompatibilität"));
        PlayerData player = PlayerData.instance(context);
        rows.add(SettingsRow.toggle("Ältere Einzeldatei-Streams",
                "Nur bei Abspielproblemen: meist 360p ohne automatische Qualitätswechsel.",
                player::isLegacyCodecsForced, player::setLegacyCodecsForced));
        return new SettingsPages.Page("Erweitert", rows);
    }

    private static SettingsPages.Page info(Context context) {
        List<SettingsRow> rows = new ArrayList<>();
        rows.add(SettingsRow.note(versionLine(context)
                + " · Updates direkt in der Einstellungsübersicht prüfen."));
        rows.add(SettingsRow.divider());
        rows.add(SettingsRow.action(context.getString(R.string.diagnostic_log_send),
                context.getString(R.string.diagnostic_log_send_desc),
                page -> DiagnosticLog.share(page.requireContext())));
        rows.add(SettingsRow.action("MarsTube auf GitHub",
                "github.com/equiummc/MarsTube",
                page -> Utils.openLinkExt(page.requireContext(),
                        "https://github.com/equiummc/MarsTube")));
        rows.add(SettingsRow.action("Open-Source-Lizenzen",
                context.getString(R.string.about_license_name),
                page -> Utils.openLinkExt(page.requireContext(),
                        context.getString(R.string.about_license_url))));
        rows.add(SettingsRow.note("MarsTube "
                + com.liskovsoft.smartyoutubetv2.tv.BuildConfig.VERSION_NAME
                + " · reguläre MarsTube-App (" + context.getPackageName() + "). "
                + "Updates werden nur nach Signaturprüfung und Bestätigung installiert."));
        return new SettingsPages.Page("Info", rows);
    }

    // ------------------------------------------------------------------
    // Summary text
    // ------------------------------------------------------------------

    private static String videoSummary(Context context) {
        String resolution = resolutionLabel(MarsTvPlayerPrefs.getResolutionCeiling(context));
        String buffer = bufferLabel(MarsTvPlayerPrefs.getBufferPreset(context));
        return resolution + " · Puffer " + buffer;
    }

    private static String subtitleSummary(Context context) {
        switch (MarsTvPlayerPrefs.getSubtitlePolicy(context)) {
            case MarsTvPlayerPrefs.SUBTITLES_OFF:
                return "Aus";
            case MarsTvPlayerPrefs.SUBTITLES_FORCED_ONLY:
                return "Nur erzwungene";
            case MarsTvPlayerPrefs.SUBTITLES_REGULAR:
            default:
                return "Regulär";
        }
    }

    private static String streamingSummary(Context context) {
        return SabrSourcePreference.isPreferred(context) ? "SABR bevorzugt" : "DASH (SABR aus)";
    }

    private static String appearanceSummary(Context context) {
        return MarsTvPlayerPrefs.getHomeTileCount(context) + " Kacheln pro Zeile";
    }

    private static String resolutionLabel(int value) {
        switch (value) {
            case MarsTvPlayerPrefs.RES_2160:
                return "2160p";
            case MarsTvPlayerPrefs.RES_1440:
                return "1440p";
            case MarsTvPlayerPrefs.RES_1080:
                return "1080p";
            case MarsTvPlayerPrefs.RES_720:
                return "720p";
            case MarsTvPlayerPrefs.RES_480:
                return "480p";
            case MarsTvPlayerPrefs.RES_AUTO:
            default:
                return "Auto";
        }
    }

    private static String bufferLabel(int value) {
        switch (value) {
            case MarsTvPlayerPrefs.BUFFER_LOW:
                return "Klein";
            case MarsTvPlayerPrefs.BUFFER_HIGH:
                return "Groß";
            case MarsTvPlayerPrefs.BUFFER_NORMAL:
            default:
                return "Normal";
        }
    }

    private static String versionLine(Context context) {
        return context.getString(R.string.app_name) + " "
                + AppInfoHelpers.getAppVersionName(context);
    }

    // ------------------------------------------------------------------
    // Row helpers
    // ------------------------------------------------------------------

    private static SettingsRow intChoice(Context context, CharSequence title,
            @Nullable CharSequence summary, Supplier<Integer> current, IntConsumer setter,
            String[] labels, int[] values) {
        SettingsRow.Choice<Integer> choice = SettingsRow.choice(title);
        for (int i = 0; i < values.length; i++) {
            choice.option(labels[i], values[i]);
        }
        if (summary != null) {
            choice.summary(summary);
        }
        return choice.bind(current, value -> setter.accept(value));
    }

    private static SettingsRow stringChoice(Context context, CharSequence title,
            @Nullable CharSequence summary, Supplier<String> current,
            java.util.function.Consumer<String> setter, String[] labels, String[] values) {
        SettingsRow.Choice<String> choice = SettingsRow.choice(title);
        for (int i = 0; i < values.length; i++) {
            choice.option(labels[i], values[i]);
        }
        if (summary != null) {
            choice.summary(summary);
        }
        return choice.bind(current, value -> setter.accept(value));
    }
}
