package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.leanback.widget.RowPresenter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.tv.R;

/** Real scrollable hero row. It leaves the viewport with the first rows of videos. */
public final class MarsFeatureRowPresenter extends RowPresenter {
    public interface VideoClickListener {
        void onVideoClick(Video video);
    }

    private VideoClickListener clickListener;

    public void setOnVideoClickListener(VideoClickListener listener) {
        clickListener = listener;
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private RowPresenter.ViewHolder attached;
    private Video selected;
    private Runnable pending;
    private String displayedUrl;

    @Override
    protected RowPresenter.ViewHolder createRowViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.mars_hero_banner, parent, false);
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null) {
            params.height = Math.round(218 * parent.getResources().getDisplayMetrics().density);
            view.setLayoutParams(params);
        }
        // The banner is a real selectable video entry, not a decorative wallpaper.
        view.setFocusable(true);
        view.setClickable(true);
        view.setOnClickListener(v -> {
            if (selected != null && clickListener != null) {
                clickListener.onVideoClick(selected);
            }
        });
        setSelectEffectEnabled(false);
        return new RowPresenter.ViewHolder(view);
    }

    @Override
    protected void onBindRowViewHolder(RowPresenter.ViewHolder holder, Object item) {
        super.onBindRowViewHolder(holder, item);
        attached = holder;
        showSelected();
    }

    @Override
    protected void onUnbindRowViewHolder(RowPresenter.ViewHolder holder) {
        if (attached == holder) {
            cancelPending();
            Glide.with(holder.view).clear((ImageView) holder.view.findViewById(R.id.mars_hero_image));
            attached = null;
            displayedUrl = null;
        }
        super.onUnbindRowViewHolder(holder);
    }

    public void setSelectedVideo(Video video) {
        selected = video;
        cancelPending();
        // Avoid thumbnail decoding during continuous DPAD scrolling.
        pending = this::showSelected;
        handler.postDelayed(pending, 650);
    }

    private void cancelPending() {
        if (pending != null) {
            handler.removeCallbacks(pending);
            pending = null;
        }
    }

    private void showSelected() {
        if (attached == null) return;
        View v = attached.view;
        TextView title = v.findViewById(R.id.mars_hero_title);
        TextView subtitle = v.findViewById(R.id.mars_hero_subtitle);
        ImageView image = v.findViewById(R.id.mars_hero_image);

        title.setText(selected != null && selected.getTitle() != null
                ? selected.getTitle() : v.getContext().getString(R.string.app_name));
        CharSequence info = selected != null ? selected.getSecondTitle() : null;
        subtitle.setText(info != null ? info : "");
        v.setContentDescription(selected != null && selected.getTitle() != null
                ? selected.getTitle() : v.getContext().getString(R.string.app_name));

        String imageUrl = selected != null ? selected.getCardImageUrl() : null;
        if (TextUtils.isEmpty(imageUrl)) {
            Glide.with(v).clear(image);
            image.setImageDrawable(null);
            displayedUrl = null;
        } else if (!imageUrl.equals(displayedUrl)) {
            displayedUrl = imageUrl;
            Glide.with(v)
                    .asBitmap()
                    .load(imageUrl)
                    .apply(new RequestOptions()
                            .override(720, 320)
                            .centerCrop()
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC))
                    .into(image);
        }
    }
}
