#!/usr/bin/env python3
"""MT2-037: YouTube account picker, Live/Music rail, reject-first Shorts, 50k SQLite.
Runs LAST on pinned/isolated MT2-036 ABC source. No package/signature changes.
"""
from pathlib import Path
import sys

if len(sys.argv)!=2: raise SystemExit("Usage: patch_037_profiles_navigation.py <isolated-source>")
root=Path(sys.argv[1])
here=Path(__file__).resolve().parent
tv=root/"smarttubetv/src/stmobile/java/com/newtube/mobile/ui"
browse=tv/"browse/MobileBrowseActivity.java"
stage=tv/"playback/MarsTvShortsStage.java"
shell=tv/"playback/MarsTvShell.java"
nav=tv/"common/MarsTvNavigationModel.java"
theme=tv/"common/MarsTvNavigationTheme.java"
discover=tv/"common/MarsShortsDiscovery.java"
playback=tv/"playback/MobilePlaybackActivity.java"
gradle=root/"smarttubetv/build.gradle"

def once(path,old,new):
    s=path.read_text(encoding="utf-8")
    n=s.count(old)
    if n!=1:raise SystemExit(f"MT2-037 fail-closed {path.name}: {n} x {old[:100]!r}")
    path.write_text(s.replace(old,new,1),encoding="utf-8")

def replace_existing(path,old,new):
    s=path.read_text(encoding="utf-8")
    if old not in s:raise SystemExit(f"MT2-037 fail-closed missing {old} in {path.name}")
    path.write_text(s.replace(old,new),encoding="utf-8")

for source, dest in (
    ("MarsTvAccountPicker037.java",tv/"browse/MarsTvAccountPicker037.java"),
    ("MarsTvInterest037.java",tv/"common/MarsTvInterest037.java"),
):
    if not (here/source).is_file() or not dest.parent.is_dir():
        raise SystemExit(f"MT2-037 missing {source} or destination")
    dest.write_bytes((here/source).read_bytes())

# Respect the backend account list. Brand profiles only appear when the
# existing YouTube service actually exposes those identities.
once(browse,
    "accountRow.setOnClickListener(v -> AccountsSheet.show(this, this::updateAccountRow));",
    """accountRow.setOnClickListener(v -> {
            if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this))
                MarsTvAccountPicker037.show(this, this::updateAccountRow);
            else AccountsSheet.show(this, this::updateAccountRow);
        });""")

# An explicit "switch profile" entry in TV Settings > Advanced > Accounts.
pages=tv/"settings/PlayerPages.java"
once(pages,
    """        rows.add(SettingsRow.header(context.getString(R.string.mobile_settings_accounts)));
        AppPrefs prefs = AppPrefs.instance(context);""",
    """        rows.add(SettingsRow.header(context.getString(R.string.mobile_settings_accounts)));
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(context)) {
            rows.add(SettingsRow.action("YouTube-Profil wechseln",
                    "Gespeicherte Konten und Kanalprofile auswählen",
                    page -> {
                        android.app.Activity host = page.getActivity();
                        if (host != null) {
                            com.newtube.mobile.ui.browse.MarsTvAccountPicker037.show(host,
                                    () -> BrowsePresenter.instance(host).refresh(false));
                        }
                    }));
        }
        AppPrefs prefs = AppPrefs.instance(context);""")

# Live and Music are real BrowsePresenter sections; no dummy UI or second feed.
once(nav,
    '            new Entry("Shorts", 8),',
    '            new Entry("Shorts", 8),\n'
    '            new Entry("Live", 9),\n'
    '            new Entry("Musik", 10),')
once(theme,
    '            case 8: return R.drawable.mars_nav_shorts_025;',
    '            case 8: return R.drawable.mars_tv_ic_nav_shorts;\n'
    '            case 9: return com.liskovsoft.smartyoutubetv2.common.R.drawable.icon_live;\n'
    '            case 10: return com.liskovsoft.smartyoutubetv2.common.R.drawable.icon_music;')
once(browse,
    '''        int destination = intent.getIntExtra("mars_tv_section", 0);
        intent.removeExtra("mars_tv_section");
        if (destination == 4 && com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) {''',
    '''        int destination = intent.getIntExtra("mars_tv_section", 0);
        intent.removeExtra("mars_tv_section");
        if (com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)
                && (destination == 9 || destination == 10)) {
            final int sectionId = destination == 9
                    ? MediaGroup.TYPE_LIVE : MediaGroup.TYPE_MUSIC;
            mBottomNav.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                BrowseSection selectedSection = null;
                for (BrowseSection section : mSections) {
                    if (section.isEnabled() && section.getId() == sectionId) {
                        selectedSection = section;
                        break;
                    }
                }
                if (selectedSection == null) {
                    android.widget.Toast.makeText(this,
                            sectionId == MediaGroup.TYPE_LIVE
                                    ? "Live-Bereich ist in den Rubriken deaktiviert"
                                    : "Musik-Bereich ist in den Rubriken deaktiviert",
                            android.widget.Toast.LENGTH_LONG).show();
                    return;
                }
                hideYouPanel();
                onSectionChosen(sectionId);
                mContentGrid.post(() -> marsTvFocusCard(0));
            });
            return;
        }
        if (destination == 4 && com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(this)) {''')

# Shorts: reject X is the FIRST action on entering the right rail. Importantly,
# the SAME ImageView performs both remote-OK and touch clicks, preventing drift.
xBlock='''        addSlimAction(controls, android.R.drawable.ic_menu_close_clear_cancel,
                "Empfehlungsoptionen – Kein Interesse", "", onNoInterest);
'''
repeatAnchor='''        addSlimAction(controls, com.liskovsoft.smartyoutubetv2.tv.R.drawable.ic_player_repeat,
'''
s=stage.read_text(encoding="utf-8")
if s.count(xBlock)!=1 or s.count(repeatAnchor)!=1:
    raise SystemExit("MT2-037: Shorts actions changed upstream")
s=s.replace(xBlock,"")
s=s.replace(repeatAnchor,xBlock+repeatAnchor,1)
stage.write_text(s,encoding="utf-8")
once(stage,"        likeCountLabel = rightLabels.get(1);",
    "        likeCountLabel = rightLabels.get(2);")
once(stage,"        commentsCountLabel = rightLabels.get(2);",
    "        commentsCountLabel = rightLabels.get(3);")
once(stage,
    '''        if (!rightLabels.isEmpty()) rightLabels.get(0).setText(enabled ? "Weiter" : "Wdh.");''',
    '''        if (rightLabels.size() > 1) rightLabels.get(1).setText(enabled ? "Weiter" : "Wdh.");''')
once(stage,
    '''            rightButtons.get(0).setImageResource(enabled''',
    '''            rightButtons.get(1).setImageResource(enabled''')
once(stage,
    '''            rightButtons.get(0).setColorFilter(WHITE);''',
    '''            rightButtons.get(1).setColorFilter(WHITE);''')
once(stage,
    "    public int reactionCount() { return rightButtons.size(); }",
    """    public int reactionCount() { return rightButtons.size(); }
    /** Shared onClick path for TV remote and touch; never duplicate action indices. */
    public boolean activateAction(int index) {
        if (index < 0 || index >= rightButtons.size()) return false;
        return rightButtons.get(index).performClick();
    }""")
begin='''            } else if (focus == Focus.SHORTS_REACTIONS) {
                if (shortsReactionPosition == 0) {'''
s=shell.read_text(encoding="utf-8")
if s.count(begin)!=1:
    raise SystemExit(f"MT2-037: Shorts key-map anchor changed (count={s.count(begin)})")
a=s.index(begin)
# Find the next *peer* focus branch, not a specific branch removed in v029.
import re
match=re.search(r"(?m)^            \} else (?:if \(focus == Focus\.[A-Z_]+\) )?\{$",
                s[a+len(begin):])
if match is None:
    raise SystemExit("MT2-037: Shorts activation block boundary missing")
b=a+len(begin)+match.start()
s=s[:a]+'''            } else if (focus == Focus.SHORTS_REACTIONS) {
                // 0 = reject, 1 = Weiter/Wdh., 2 = Like, 3 = Kommentare.
                // Both remote and row clicks use exactly one onClick mapping.
                if (shortsReactionPosition == 3) shortsCommentsDetached = false;
                shortsStage.activateAction(shortsReactionPosition);
'''+s[b:]
shell.write_text(s,encoding="utf-8")

# Migrate the 1000 legacy IDs into SQLite once, then use the new 50k store
# everywhere. 036 legacy data remains untouched for safety/downgrade.
replace_existing(browse,"MarsTvInterest036","MarsTvInterest037")
replace_existing(playback,"MarsTvInterest036","MarsTvInterest037")
replace_existing(discover,"MarsTvInterest036","MarsTvInterest037")

once(gradle,"versionCode 12036","versionCode 12037")
once(gradle,'versionName "0.0.36"','versionName "0.0.37"')
assert 'applicationId "de.equiummc.marstube2"' in gradle.read_text(encoding="utf-8")
assert "new Entry(\"Live\", 9)" in nav.read_text(encoding="utf-8")
assert "new Entry(\"Musik\", 10)" in nav.read_text(encoding="utf-8")
assert "shortsStage.activateAction(shortsReactionPosition)" in shell.read_text(encoding="utf-8")
assert "get(1).setImageResource(enabled" in stage.read_text(encoding="utf-8")
assert "MarsTvAccountPicker037.show" in browse.read_text(encoding="utf-8")
assert "MarsTvInterest037.instance" in discover.read_text(encoding="utf-8")
print("MT2-037: 50k-ID migration, TV account picker, Live/Music navigation, reject first with unified key mapping.")
