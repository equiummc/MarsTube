#!/usr/bin/env python3
"""MT2-002 isolated NewTube touch-flavor patch: add keyboard/TV remote bridge.

Only mutates the PINNED, disposable NewTube reference checkout. Never MarsTube 1.x.
Every anchor must be unique, or the build fails closed rather than patching other code.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("usage: python3 patch_tv_input.py <newtube-reference-root>")
root = Path(sys.argv[1]).resolve()
if not (root / "sabr-media3").is_dir():
    raise SystemExit("not a NewTube reference checkout")

def insert_before(relpath: str, needle: str, inserted: str):
    path = root / relpath
    original = path.read_text(encoding="utf-8")
    if original.count(needle) != 1:
        raise SystemExit(f"unsafe patch: {relpath}: anchor occurrences={original.count(needle)}")
    if "MT2-002 BEGIN" in original:
        raise SystemExit(f"patch applied twice: {relpath}")
    path.write_text(original.replace(needle, inserted + "\n" + needle, 1), encoding="utf-8")
    print("Patched", relpath)

browse = "smarttubetv/src/stmobile/java/com/newtube/mobile/ui/browse/MobileBrowseActivity.java"
playback = "smarttubetv/src/stmobile/java/com/newtube/mobile/ui/playback/MobilePlaybackActivity.java"
manifest = root / "smarttubetv/src/stmobile/AndroidManifest.xml"

browse_anchor = "    /** Hardware volume keys drive the TV while casting; everything else falls through. */"
browse_code = r"""
    // MT2-002 BEGIN: D-pad compatible temporary reference UI; no touch gestures required.
    private int mMarsTvSelection = 0;

    private void marsTvFocusCard(int index) {
        if (mContentGrid == null || mAdapter == null || mAdapter.getItemCount() == 0) return;
        int bounded = Math.max(0, Math.min(mAdapter.getItemCount() - 1, index));
        RecyclerView.ViewHolder previous = mContentGrid.findViewHolderForAdapterPosition(mMarsTvSelection);
        if (previous != null) previous.itemView.setForeground(null);
        mMarsTvSelection = bounded;
        mContentGrid.scrollToPosition(bounded);
        mContentGrid.post(() -> {
            if (mContentGrid == null) return;
            RecyclerView.ViewHolder target = mContentGrid.findViewHolderForAdapterPosition(mMarsTvSelection);
            if (target == null) return;
            android.graphics.drawable.GradientDrawable border =
                    new android.graphics.drawable.GradientDrawable();
            border.setColor(android.graphics.Color.TRANSPARENT);
            int width = Math.max(2, (int) (3 * getResources().getDisplayMetrics().density));
            border.setStroke(width, android.graphics.Color.rgb(42, 143, 255));
            target.itemView.setForeground(border);
            target.itemView.setFocusable(true);
            target.itemView.requestFocus();
        });
    }

    private boolean marsTvBrowseKey(KeyEvent event) {
        int key = event.getKeyCode();
        boolean mapped = key == KeyEvent.KEYCODE_DPAD_LEFT
                || key == KeyEvent.KEYCODE_DPAD_RIGHT
                || key == KeyEvent.KEYCODE_DPAD_UP
                || key == KeyEvent.KEYCODE_DPAD_DOWN
                || key == KeyEvent.KEYCODE_DPAD_CENTER
                || key == KeyEvent.KEYCODE_ENTER
                || key == KeyEvent.KEYCODE_NUMPAD_ENTER;
        if (!mapped) return false;
        if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
        if (mContentGrid == null || mAdapter == null || mAdapter.getItemCount() == 0) return true;
        int stride = mLayoutManager != null ? Math.max(1, mLayoutManager.getSpanCount()) : 1;
        switch (key) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                marsTvFocusCard(mMarsTvSelection - 1);
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                marsTvFocusCard(mMarsTvSelection + 1);
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                marsTvFocusCard(mMarsTvSelection - stride);
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                marsTvFocusCard(mMarsTvSelection + stride);
                break;
            default:
                RecyclerView.ViewHolder target =
                        mContentGrid.findViewHolderForAdapterPosition(mMarsTvSelection);
                if (target != null) target.itemView.performClick();
                break;
        }
        android.util.Log.d("MarsTube2TV", "browse-key=" + key + " selected=" + mMarsTvSelection);
        return true;
    }
    // MT2-002 END
"""
insert_before(browse,browse_anchor,browse_code)

player_anchor = "    /**\n     * Hardware volume keys drive the TV while casting (official-app behavior); everything else -"
player_code = r"""
    // MT2-002 BEGIN: explicitly handle TV remote and keyboard on the Media3 player.
    private boolean marsTvPlaybackKey(KeyEvent event) {
        int key = event.getKeyCode();
        boolean mapped = key == KeyEvent.KEYCODE_DPAD_LEFT
                || key == KeyEvent.KEYCODE_DPAD_RIGHT
                || key == KeyEvent.KEYCODE_DPAD_UP
                || key == KeyEvent.KEYCODE_DPAD_DOWN
                || key == KeyEvent.KEYCODE_DPAD_CENTER
                || key == KeyEvent.KEYCODE_ENTER
                || key == KeyEvent.KEYCODE_NUMPAD_ENTER
                || key == KeyEvent.KEYCODE_SPACE
                || key == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || key == KeyEvent.KEYCODE_MEDIA_REWIND
                || key == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
                || key == KeyEvent.KEYCODE_VOLUME_UP
                || key == KeyEvent.KEYCODE_VOLUME_DOWN
                || key == KeyEvent.KEYCODE_MENU
                || key == KeyEvent.KEYCODE_S
                || key == KeyEvent.KEYCODE_I
                || key == KeyEvent.KEYCODE_0;
        if (!mapped || mIsInPip) return false;
        if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
        if (mPlayer == null || mExoPlayerController == null) return true;

        String note = null;
        switch (key) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                if (mPlayer.isCurrentMediaItemSeekable()) {
                    long offset = (key == KeyEvent.KEYCODE_DPAD_LEFT
                            || key == KeyEvent.KEYCODE_MEDIA_REWIND) ? -10000L : 10000L;
                    long desired = Math.max(0L, mPlayer.getCurrentPosition() + offset);
                    long duration = mPlayer.getDuration();
                    if (duration != androidx.media3.common.C.TIME_UNSET && duration > 0L) {
                        desired = Math.min(desired, Math.max(0L, duration - 200L));
                    }
                    mPlayer.seekTo(desired);
                    note = (offset > 0 ? "+10 s" : "-10 s");
                    showControlsInternal(false);
                } else {
                    note = "Dieses Video ist nicht spulbar";
                }
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                togglePlayPause();
                note = mPlayer.getPlayWhenReady() ? "Wiedergabe" : "Pause";
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                float delta = (key == KeyEvent.KEYCODE_DPAD_UP
                        || key == KeyEvent.KEYCODE_VOLUME_UP) ? 0.10f : -0.10f;
                float volume = Math.max(0f, Math.min(1f, mExoPlayerController.getVolume() + delta));
                mExoPlayerController.setVolume(volume);
                note = String.format(java.util.Locale.GERMANY, "App-Lautstärke %d%%",
                        Math.round(volume * 100f));
                break;
            case KeyEvent.KEYCODE_MENU:
                if (mMoreButton != null) mMoreButton.performClick();
                return true;
            case KeyEvent.KEYCODE_0:
                applySpeed(1.0f);
                note = "Tempo 1,00x (normal)";
                break;
            case KeyEvent.KEYCODE_S:
                float before = mExoPlayerController.getSpeed();
                float after = before < 0.99f ? 1.0f
                        : before < 1.24f ? 1.25f
                        : before < 1.49f ? 1.5f
                        : before < 1.99f ? 2.0f : 0.75f;
                applySpeed(after);
                note = String.format(java.util.Locale.GERMANY, "Tempo %.2fx", after);
                break;
            case KeyEvent.KEYCODE_I:
                note = String.format(java.util.Locale.GERMANY,
                        "Tempo aktuell %.2fx | gewählt %.2fx | Lautstärke %d%%",
                        mExoPlayerController.getEffectiveSpeed(),
                        mExoPlayerController.getSpeed(),
                        Math.round(mExoPlayerController.getVolume() * 100f));
                break;
            default: return false;
        }
        if (note != null) {
            android.widget.Toast.makeText(this, note, android.widget.Toast.LENGTH_SHORT).show();
            android.util.Log.i("MarsTube2TV", "key=" + key + " " + note);
        }
        return true;
    }
    // MT2-002 END
"""
insert_before(playback,player_anchor,player_code)

# Two dispatchKeyEvent methods: one in each activity. Respect casting precedence
# and do not swallow keys the reference code already handles.
def replace_once(rel: str, original: str, replacement: str):
    path = root / rel
    old = path.read_text(encoding="utf-8")
    if old.count(original) != 1:
        raise SystemExit(f"unsafe dispatch replacement {rel}: {old.count(original)}")
    path.write_text(old.replace(original, replacement, 1), encoding="utf-8")

dispatch = """        if (CastVolumeKeys.onDispatchKeyEvent(this, event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);"""
replace_once(browse,dispatch,"""        if (CastVolumeKeys.onDispatchKeyEvent(this, event)) {
            return true;
        }
        if (marsTvBrowseKey(event)) return true;
        return super.dispatchKeyEvent(event);""")
replace_once(playback,dispatch,"""        if (CastVolumeKeys.onDispatchKeyEvent(this, event)) {
            return true;
        }
        if (marsTvPlaybackKey(event)) return true;
        return super.dispatchKeyEvent(event);""")

# Label explicitly denotes a temporary lab app; no changes to package, signatures,
# upstream released application or the stable MarsTube install.
old_manifest = manifest.read_text(encoding="utf-8")
needle = 'android:theme="@style/Theme.NewTube"\n        tools:replace="android:name,android:theme">'
replacement = ('android:theme="@style/Theme.NewTube"\n'
               '        android:label="MarsTube 2.0 Input Lab"\n'
               '        tools:replace="android:name,android:theme,android:label">')
if old_manifest.count(needle) != 1:
    raise SystemExit(f"manifest anchor changed: {old_manifest.count(needle)}")
manifest.write_text(old_manifest.replace(needle,replacement,1),encoding="utf-8")

print("MT2-002 remote lab patch OK; key map: navigation, OK, +/-10s, play/pause, volume, S/0/I.")
