package com.zzt.library;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by zzt on 2015/2/26.
 */
public class MaterialImageView extends ImageView {

    private GradientDrawable mMaskDrawable;
    private Paint mMaskedPaint;

    private boolean mCacheValid = false;
    private Bitmap mCacheBitmap;
    private int mCachedWidth;
    private int mCachedHeight;

    private Path mCornerShadowPath;
    private Paint mCornerShadowPaint;
    private Paint mEdgeShadowPaint;
    private Paint mPaint;

    private RectF mBoundsF = new RectF();

    private int mInsetShadow;
    private float mMaxShadowSize;
    private float mRawMaxShadowSize;
    private float mShadowSize;
    private float mRawShadowSize;

    private int radius;
    private Rect mBgBounds = new Rect();
    private Rect mBounds = new Rect();
    float mCornerRadius;

    private int mShadowStartColor;
    private int mShadowEndColor;

    private boolean mUseWhiteBackground = false;

    public MaterialImageView(Context context) {
        this(context, null);
    }

    public MaterialImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MaterialImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, context.getResources(), defStyleAttr);
    }

    public void init(Context context, AttributeSet attrs, Resources resources, int defStyleAttr){

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MaterialImageView,
                defStyleAttr, 0);

        int shadowSize = a.getInt(R.styleable.MaterialImageView_shadow_size, 8);
        radius = a.getInt(R.styleable.MaterialImageView_radius_size, 15);//radius size
        mUseWhiteBackground = a.getBoolean(R.styleable.MaterialImageView_use_white_bg, false);

        a.recycle();

        //遮罩  用于画出圆角图片
        mMaskDrawable = new GradientDrawable();
        mMaskDrawable.setShape(GradientDrawable.RECTANGLE);
        mMaskDrawable.setColor(0xff000000);
        mMaskDrawable.setCornerRadius(radius);

        mMaskedPaint = new Paint();
        mMaskedPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        mMaskedPaint.setAntiAlias(true);

        mCacheBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);

        mPaint = new Paint(Paint.DITHER_FLAG | Paint.HINTING_ON );
        mPaint.setColor(0xffffffff);

        mShadowStartColor = resources.getColor(R.color.shadow_start_color);
        mShadowEndColor = resources.getColor(R.color.shadow_end_color);

        mInsetShadow = resources.getDimensionPixelSize(R.dimen.cardview_compat_inset_shadow);
        mCornerRadius = ((int) (radius + 0.5F));

        mCornerShadowPaint = new Paint(Paint.DITHER_FLAG | Paint.HINTING_ON);
        mCornerShadowPaint.setStyle(Paint.Style.FILL);
        mCornerShadowPaint.setAntiAlias(true);

        mEdgeShadowPaint = new Paint(mCornerShadowPaint);
        mEdgeShadowPaint.setAntiAlias(true);

        setShadowSize(shadowSize, 20);
    }

    void setShadowSize(float shadowSize, float maxShadowSize) {
        if((shadowSize < 0.0f) || (maxShadowSize < 0.0f)){
            throw new IllegalArgumentException("invalid shadow size");
        }

        if(shadowSize > maxShadowSize){
            shadowSize = maxShadowSize;
        }

        mRawShadowSize = shadowSize;
        mRawMaxShadowSize = maxShadowSize;
        mShadowSize = ((int) (shadowSize * 1.5F + mInsetShadow + 0.5F));
        mMaxShadowSize = (maxShadowSize + mInsetShadow);
        invalidate();
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
        final boolean changed = super.setFrame(l, t, r, b);
        mBounds.set(0, 0, r - l, b - t);
        mBoundsF.set(0, 0, r - l, b - t);
        buildShadow(0, 0, r - l, b - t);

        mMaskDrawable.setBounds(mBgBounds.left, mBgBounds.top + (int)(mRawShadowSize / 2.0F),
                mBgBounds.right, mBgBounds.bottom + (int)(mRawShadowSize / 2.0F));

        if (changed) {
            mCacheValid = false;
        }
        return changed;
    }

    private void buildShadow(int left, int top, int right, int bottom){
        float verticalOffset = mRawMaxShadowSize * 1.5F;
        mBgBounds.set(left + (int)mRawMaxShadowSize, top +(int)verticalOffset,
                right - (int)mRawMaxShadowSize, bottom - (int)verticalOffset);
        buildShadowCorners();
    }

    private void buildShadowCorners() {
        RectF innerBounds = new RectF(-mCornerRadius, -mCornerRadius, mCornerRadius, mCornerRadius);
        RectF outerBounds = new RectF(innerBounds);
        outerBounds.inset(-mShadowSize, -mShadowSize);

        if (mCornerShadowPath == null)
            mCornerShadowPath = new Path();
        else {
            mCornerShadowPath.reset();
        }

        mCornerShadowPath.setFillType(Path.FillType.EVEN_ODD);
        mCornerShadowPath.moveTo(-mCornerRadius, 0.0F);
        mCornerShadowPath.rLineTo(-mShadowSize, 0.0F);
        mCornerShadowPath.arcTo(outerBounds, 180.0F, 90.0F, false);
        mCornerShadowPath.arcTo(innerBounds, 270.0F, -90.0F, false);
        mCornerShadowPath.close();

        float startRatio = mCornerRadius / (mCornerRadius + mShadowSize);
        mCornerShadowPaint.setShader(
                new RadialGradient(0.0F, 0.0F, mCornerRadius + mShadowSize,//阴影半径
                        new int[]{mShadowStartColor, mShadowStartColor, mShadowEndColor},
                        new float[]{0.0F, startRatio, 1.0F},
                        Shader.TileMode.CLAMP));

        mEdgeShadowPaint.setShader(
                new LinearGradient(
                        0.0F, -mCornerRadius  + mShadowSize, 0.0F, -mCornerRadius - mShadowSize,
                        new int[]{mShadowStartColor, mShadowStartColor, mShadowEndColor},
                        new float[]{0.0F, 0.5F, 1.0F}, Shader.TileMode.CLAMP));

        mEdgeShadowPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        canvas.translate(0.0F, mRawShadowSize / 2.0F);
        drawShadow(canvas);
        //白色背景
        if(mUseWhiteBackground) {
            canvas.translate(0.0F, -mRawShadowSize / 2.0F);
            canvas.drawRoundRect(
                    new RectF(mBgBounds.left, mBgBounds.top, mBgBounds.right, mBgBounds.bottom),
                    mCornerRadius, mCornerRadius, mPaint);
        }
        canvas.translate(0.0F, 0);

        if(mBounds == null){
            return ;
        }

        int width = mBounds.width();
        int height= mBounds.height();

        if (width == 0 || height == 0) {
            return;
        }

        if (!mCacheValid || width != mCachedWidth || height != mCachedHeight) {
            if(width == mCachedWidth && height == mCachedHeight){
                mCacheBitmap.eraseColor(0);
            }else{
                mCacheBitmap.recycle();
                mCacheBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                mCachedWidth = width;
                mCachedHeight = height;
            }

            Canvas cacheCanvas = new Canvas(mCacheBitmap);
            if (mMaskDrawable != null) {
                int sc = cacheCanvas.save();
                cacheCanvas.translate(0.0F, -mRawShadowSize/2.0f);
                mMaskDrawable.draw(cacheCanvas);
                cacheCanvas.saveLayer(mBoundsF, mMaskedPaint,
                        Canvas.HAS_ALPHA_LAYER_SAVE_FLAG | Canvas.FULL_COLOR_LAYER_SAVE_FLAG);
                super.onDraw(cacheCanvas);
                cacheCanvas.restoreToCount(sc);
            }
        }
        canvas.drawBitmap(mCacheBitmap, mBounds.left, mBounds.top, null);
    }

    private void drawShadow(Canvas canvas) {
        float edgeShadowTop = -mCornerRadius - mShadowSize;
        float inset = mCornerRadius + mInsetShadow + mRawShadowSize / 2.0F;
        boolean drawHorizontalEdges = mBgBounds.width() - 2.0F * inset > 0.0F;
        boolean drawVerticalEdges = mBgBounds.height() - 2.0F * inset > 0.0F;

        int saved;

        saved = canvas.save();
        canvas.translate(mBgBounds.left + inset, mBgBounds.top + inset);
        if (drawHorizontalEdges) {
            canvas.drawRect(0.0F, edgeShadowTop, mBgBounds.width() - 2.0F * inset, -mCornerRadius, mEdgeShadowPaint);
        }
        canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);//和下面不同  这句放在这里 才不会出现旋转后阴影缺角
        canvas.restoreToCount(saved);

        saved = canvas.save();
        canvas.translate(mBgBounds.left + inset, mBgBounds.bottom - inset);
        canvas.rotate(270.0F);
        canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
        if (drawVerticalEdges) {
            canvas.drawRect(0.0F, edgeShadowTop, mBgBounds.height() - 2.0F * inset, -mCornerRadius, mEdgeShadowPaint);
        }
        canvas.restoreToCount(saved);

        saved = canvas.save();
        canvas.translate(mBgBounds.right - inset, mBgBounds.top + inset);
        canvas.rotate(90.0F);
        canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
        if (drawVerticalEdges) {
            canvas.drawRect(0.0F, edgeShadowTop, mBgBounds.height() - 2.0F * inset, -mCornerRadius, mEdgeShadowPaint);
        }
        canvas.restoreToCount(saved);

        saved = canvas.save();
        canvas.translate(mBgBounds.right - inset, mBgBounds.bottom - inset);
        canvas.rotate(180.0F);
        canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
        if (drawHorizontalEdges) {
            canvas.drawRect(0.0F, edgeShadowTop, mBgBounds.width() - 2.0F * inset, -mCornerRadius + mShadowSize, mEdgeShadowPaint);
        }
        canvas.restoreToCount(saved);
    }

}
