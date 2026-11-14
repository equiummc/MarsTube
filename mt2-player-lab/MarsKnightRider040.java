package com.newtube.mobile.ui.browse;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

/** Non-focusable content loading indicator; driven only by LoadingManager callbacks. */
public final class MarsKnightRider040 extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator animator;
    private boolean loading;
    private float position;
    private final Runnable begin = () -> {
        if (!loading || !isAttachedToWindow() || getWindowVisibility() != VISIBLE) return;
        setVisibility(VISIBLE);
        animator = ValueAnimator.ofFloat(0f,1f);
        animator.setDuration(1100);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(a -> { position = (float)a.getAnimatedValue(); invalidate(); });
        animator.start();
    };
    public MarsKnightRider040(Context context) {
        super(context);
        setFocusable(false);
        setClickable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setVisibility(GONE);
    }
    public void setLoading(boolean value) {
        if (loading == value) return;
        loading = value;
        stop();
        if (value) postDelayed(begin,300);
    }
    private void stop() {
        removeCallbacks(begin);
        if (animator != null) { animator.cancel(); animator = null; }
        setVisibility(GONE);
    }
    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        stop();
        if (visibility == VISIBLE && loading) postDelayed(begin,300);
    }
    @Override protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) {
        paint.setShader(new LinearGradient(0,0,Math.max(1,w*0.23f),0,
                new int[]{0x0021BAFF,0xFF21BAFF,0xFFB8EAFF,0xFF21BAFF,0x0021BAFF},
                new float[]{0,.35f,.5f,.65f,1},Shader.TileMode.CLAMP));
    }
    @Override protected void onDraw(Canvas canvas) {
        float width = getWidth()*0.23f;
        canvas.save();
        canvas.translate(position*(getWidth()-width),0);
        canvas.drawRoundRect(0,0,width,getHeight(),getHeight()/2f,getHeight()/2f,paint);
        canvas.restore();
    }
}
