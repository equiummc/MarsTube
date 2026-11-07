#!/usr/bin/env python3
"""MT2-031: compact TV Shorts feed tiles matching the real Shorts player previews.

Runs AFTER patch_030_inapp_search_input.py in a separate pinned source checkout.
Only changes TV Shorts VideoCardAdapter; phones and regular video cards reset to
their original layout. No service/player/SABR/signing changes.
"""
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit("Usage: patch_031_compact_shorts_feed.py <pinned-source>")
root = Path(sys.argv[1]).resolve()
cards = root / "smarttubetv/src/stmobile/java/com/newtube/mobile/ui/browse/VideoCardAdapter.java"
gradle = root / "smarttubetv/build.gradle"
if not (root / "sabr-media3").is_dir():
    raise SystemExit("MT2-031: pinned SABR-Media3 source tree missing")

def replace_exact(path, before, after, expected=1):
    content = path.read_text(encoding="utf-8")
    n = content.count(before)
    if n != expected:
        raise SystemExit(f"MT2-031: unsafe anchor {path.name}; expected={expected}, found={n}, prefix={before[:110]!r}")
    path.write_text(content.replace(before, after), encoding="utf-8")

old = """        private void marsTvBindShortsAspect(Video video) {
            if (!com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(itemView.getContext())) {
                return;
            }
            android.view.ViewGroup.LayoutParams old = mThumbnailFrame.getLayoutParams();
            if (!(old instanceof androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)) {
                return;
            }
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams lp =
                    (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) old;
            String desired = video != null && video.isShorts ? "9:16" : "16:9";
            if (!desired.equals(lp.dimensionRatio)) {
                lp.dimensionRatio = desired;
                mThumbnailFrame.setLayoutParams(lp);
            }
        }
"""
new = """        /**
         * TV Shorts FEED cards use the SAME pixel image width calculation as
         * MarsTvShortsStage's left-side previews. Three grid slots can remain
         * spacious; the image itself is not inflated to occupy the whole slot.
         * The label underneath is two title lines and one blue creator line.
         *
         * Restore all original constraints when this recycled holder becomes
         * a regular video. Mobile phones keep their normal card layout.
         */
        private void marsTvBindShortsAspect(Video video) {
            if (!com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(itemView.getContext())) {
                return;
            }
            android.view.ViewGroup.LayoutParams old = mThumbnailFrame.getLayoutParams();
            if (!(old instanceof androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)) {
                return;
            }
            android.content.Context context = itemView.getContext();
            android.util.DisplayMetrics dm = context.getResources().getDisplayMetrics();
            int dp22 = Math.round(22f * dm.density);
            int dp10 = Math.round(10f * dm.density);
            int dp32 = Math.round(32f * dm.density);
            int dp34 = Math.round(34f * dm.density);
            int dp54 = Math.round(54f * dm.density);
            int dp80 = Math.round(80f * dm.density);
            int dp78 = Math.round(78f * dm.density);
            int videoWidth = Math.min(dm.widthPixels,
                    Math.round(Math.max(1, dm.heightPixels - dp22 * 2 - dp10) * 9f / 16f));
            int videoLeft = (dm.widthPixels - videoWidth) / 2;
            int videoHeight = Math.max(dp80, dm.heightPixels - dp54);
            // Must match the 0.0.29/0.0.30 in-player preview formula.
            int stageImageWidth = Math.min(Math.round((videoLeft - dp32) * 0.42f),
                    Math.max(dp78, Math.round((videoHeight - dp34) * 9f / 32f)));
            int span = Math.max(1, com.newtube.mobile.player.MarsTvPlayerPrefs.shortsSpanCount(context));
            int cellWidth = Math.max(dp78, (dm.widthPixels - Math.round(100f * dm.density)) / span);
            int width = Math.min(Math.max(dp78, stageImageWidth),
                    Math.max(dp78, cellWidth - Math.round(24f * dm.density)));
            boolean shorts = video != null && video.isShorts;
            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams frame =
                    (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) old;
            frame.width = shorts ? width : 0;
            frame.height = 0;
            frame.dimensionRatio = shorts ? "9:16" : "16:9";
            frame.horizontalBias = 0.5f;
            mThumbnailFrame.setLayoutParams(frame);

            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams title =
                    (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) mTitle.getLayoutParams();
            title.width = shorts ? width : 0;
            title.startToStart = shorts
                    ? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    : androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
            title.endToEnd = shorts
                    ? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    : androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
            title.startToEnd = shorts
                    ? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                    : com.liskovsoft.smartyoutubetv2.tv.R.id.video_channel_avatar;
            title.endToStart = shorts
                    ? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                    : com.liskovsoft.smartyoutubetv2.tv.R.id.video_overflow;
            title.leftMargin = shorts ? 0 : Math.round(12f * dm.density);
            title.rightMargin = shorts ? 0 : Math.round(4f * dm.density);
            title.topMargin = Math.round((shorts ? 7f : 10f) * dm.density);
            title.horizontalBias = 0.5f;
            mTitle.setLayoutParams(title);
            mTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, shorts ? 13f : 16f);
            mTitle.setMaxLines(2);

            androidx.constraintlayout.widget.ConstraintLayout.LayoutParams meta =
                    (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) mMeta.getLayoutParams();
            meta.width = shorts ? width : 0;
            meta.startToStart = shorts
                    ? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    : com.liskovsoft.smartyoutubetv2.tv.R.id.video_title;
            meta.endToEnd = shorts
                    ? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    : androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
            meta.endToStart = shorts
                    ? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                    : com.liskovsoft.smartyoutubetv2.tv.R.id.video_overflow;
            meta.leftMargin = 0;
            meta.rightMargin = Math.round((shorts ? 0f : 4f) * dm.density);
            meta.horizontalBias = 0.5f;
            mMeta.setLayoutParams(meta);
            mMeta.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, shorts ? 11f : 13f);
            mMeta.setMaxLines(1);
            mMeta.setTextColor(shorts ? 0xFF37B8FF
                    : androidx.core.content.ContextCompat.getColor(context,
                            com.liskovsoft.smartyoutubetv2.tv.R.color.mobile_color_on_surface_secondary));
            if (shorts && mOverflow != null) mOverflow.setVisibility(android.view.View.GONE);
        }
"""
replace_exact(cards,old,new)
old_meta="""            mMeta.setText(meta == null || meta.length() == 0 ? video.getAuthor() : meta);"""
new_meta="""            mMeta.setText(com.newtube.mobile.ui.common.MarsDeviceMode.isTelevision(context)
                    && video.isShorts ? video.getAuthor()
                    : (meta == null || meta.length() == 0 ? video.getAuthor() : meta));"""
replace_exact(cards,old_meta,new_meta,expected=2)
# A Shorts card intentionally has no avatar beside its caption.
old_avatar="if (onChannelClick == null || video == null || !hasChannelAvatar(video)) {"
new_avatar="""if (onChannelClick == null || video == null || !hasChannelAvatar(video)
                    || (video.isShorts && com.newtube.mobile.ui.common.MarsDeviceMode
                            .isTelevision(itemView.getContext()))) {"""
replace_exact(cards,old_avatar,new_avatar)
old_focus="""                    if (!hasFocus) {
                        view.setForeground(original);
                        return;
                    }
                    float density = view.getResources().getDisplayMetrics().density;
                    android.graphics.drawable.GradientDrawable focus =
                            new android.graphics.drawable.GradientDrawable();
                    focus.setColor(0x22305B9E);
                    focus.setCornerRadius(12f * density);
                    focus.setStroke(Math.max(2, Math.round(2f * density)), 0xFF16B8FF);
                    view.setForeground(focus);"""
new_focus="""                    if (!hasFocus) {
                        view.setForeground(original);
                        mThumbnailFrame.setForeground(null);
                        return;
                    }
                    float density = view.getResources().getDisplayMetrics().density;
                    android.graphics.drawable.GradientDrawable focus =
                            new android.graphics.drawable.GradientDrawable();
                    focus.setColor(0x22305B9E);
                    focus.setCornerRadius(12f * density);
                    focus.setStroke(Math.max(2, Math.round(2f * density)), 0xFF16B8FF);
                    if (mVideo != null && mVideo.isShorts) {
                        // Focus around the REAL image, not the otherwise wide grid slot.
                        view.setForeground(original);
                        mThumbnailFrame.setForeground(focus);
                    } else {
                        mThumbnailFrame.setForeground(null);
                        view.setForeground(focus);
                    }"""
replace_exact(cards,old_focus,new_focus)

# Patch 016 uses production-version.json for a single permanent package and
# identity; bump only versionCode and versionName, not appId or update channel.
replace_exact(gradle,'versionCode 12030','versionCode 12031')
replace_exact(gradle,'versionName "0.0.30"','versionName "0.0.31"')

src=cards.read_text(encoding="utf-8")
assert "int stageImageWidth = Math.min(" in src
assert "shorts ? 13f : 16f" in src
assert "mMeta.setTextColor(shorts ? 0xFF37B8FF" in src
assert "mThumbnailFrame.setForeground(focus)" in src
assert 'versionCode 12031' in gradle.read_text(encoding="utf-8")
print("MT2-031: compact 3-up Shorts browse tiles, two-line title, blue creator, focused portrait image.")
