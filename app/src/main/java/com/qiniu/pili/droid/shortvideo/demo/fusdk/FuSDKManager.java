package com.qiniu.pili.droid.shortvideo.demo.fusdk;


import android.content.Context;

import com.faceunity.FURenderer;
import com.faceunity.entity.Effect;
import com.faceunity.wrapper.faceunity;

import java.util.ArrayList;

public class FuSDKManager {
    // 特效信息集合
    private volatile ArrayList<MagicModel> mMagicModelsList = new ArrayList<>();

    // TuSDK 处理引擎 负责预览时处理
    private FURenderer mPreviewFilterEngine;

    // TuSDK 处理引擎 负责保存时处理
    private FURenderer mSaveFilterEngine;

    private Context mContext;

    public FuSDKManager(Context context) {
        mContext = context;
    }

    /**
     * 初始化预览 TuSDKFilterEngine
     */
    public void setupPreviewFilterEngine() {
        if (mPreviewFilterEngine != null)
            return;
        mPreviewFilterEngine = createFilterEngine();
    }

    /**
     * 初始化保存 TuSDKFilterEngine
     */
    public void setupSaveFilterEngine() {
        if (mSaveFilterEngine != null)
            return;
        mSaveFilterEngine = createFilterEngine();
    }

    /**
     * 获取预览 TuSDKFilterEngine
     */
    public FURenderer getPreviewFilterEngine() {
        return mPreviewFilterEngine;
    }

    /**
     * 获取保存 TuSDKFilterEngine
     */
    public FURenderer getSaveFilterEngine() {
        return mSaveFilterEngine;
    }

    /**
     * 销毁预览 TuSDKFilterEngine
     */
    public void destroyPreviewFilterEngine() {
        if (mPreviewFilterEngine != null) {
            mPreviewFilterEngine.onSurfaceDestroyed();
            mPreviewFilterEngine = null;
        }
    }

    /**
     * 销毁保存 TuSDKFilterEngine
     */
    public void destroySaveFilterEngine() {
        if (mSaveFilterEngine != null) {
            mSaveFilterEngine.onSurfaceDestroyed();
            mSaveFilterEngine = null;
        }
    }

    /**
     * 添加一个场景特效信息
     *
     * @param magicModel
     */
    public synchronized void addMagicModel(MagicModel magicModel) {
        this.mMagicModelsList.add(magicModel);
    }

    /**
     * 获取设置的最后一个场景特效信息
     *
     * @return MagicModel
     */
    public synchronized MagicModel getLastMagicModel() {
        if (this.mMagicModelsList.size() == 0)
            return null;
        return this.mMagicModelsList.get(this.mMagicModelsList.size() - 1);
    }

    /**
     * 清除场景特效
     */
    public synchronized void reset() {
        this.mMagicModelsList.clear();
    }

    /**
     * 根据 position 找到该位置设置的场景特效信息
     *
     * @param position 毫秒
     * @return MagicModel
     */
    public synchronized MagicModel findMagicModelWithPosition(long position) {
        for (MagicModel magicModel : mMagicModelsList) {
            if ((magicModel.getTimeRange().start <= position && position <= magicModel.getTimeRange().end))
                return magicModel;
        }
        return null;
    }

    private FURenderer createFilterEngine() {
        // 美颜处理
        FURenderer filterEngine = new FURenderer
                .Builder(mContext)
                .inputTextureType(faceunity.FU_ADM_FLAG_ENABLE_READBACK)
                .build();

        // 设置是否输出原始图片朝向 false: 图像被转正后输出

        return filterEngine;
    }

    /**
     * 场景特效信息
     */
    public static class MagicModel {
        // 特效code
        private Effect mMagicCode;
        // 特效时间段
        private FuSDKManager.FuSDKTimeRange mTimeRange;

        public MagicModel(Effect magicCode, FuSDKManager.FuSDKTimeRange timeRange) {
            this.mMagicCode = magicCode;
            this.mTimeRange = timeRange;
        }

        public Effect getMagicCode() {
            return mMagicCode;
        }

        public FuSDKManager.FuSDKTimeRange getTimeRange() {
            return this.mTimeRange;
        }
    }

    public static class FuSDKTimeRange {
        public float start;
        public float end;

        public FuSDKTimeRange() {
        }

        public static FuSDKTimeRange makeRange(float var0, float var1) {
            FuSDKTimeRange var2 = new FuSDKTimeRange();
            var2.start = var0;
            var2.end = var1;
            return var2;
        }

        public boolean isValid() {
            return this.start >= 0.0F && this.end > this.start;
        }

        public float duration() {
            return !this.isValid() ? 0.0F : this.end - this.start;
        }

        public float durationTimeUS() {
            return this.duration() * 1000000.0F;
        }

        public float getStartTimeUS() {
            return this.start * 1000000.0F;
        }

        public float getEndTimeUS() {
            return this.end * 1000000.0F;
        }

        public boolean contains(FuSDKTimeRange var1) {
            return var1 != null && var1.isValid() && this.isValid() ? var1.start >= this.start && var1.start < this.end && var1.end <= this.end : false;
        }

        public FuSDKTimeRange convertTo(FuSDKTimeRange var1) {
            return var1 != null && var1.isValid() && this.isValid() ? makeRange(var1.start + this.start, var1.start + this.end) : this;
        }

        public boolean equals(Object var1) {
            if (var1 == null) {
                return false;
            } else if (!(var1 instanceof FuSDKTimeRange)) {
                return false;
            } else if (var1 == this) {
                return true;
            } else {
                FuSDKTimeRange var2 = (FuSDKTimeRange) var1;
                return var2.start == this.start && var2.end == this.end;
            }
        }

        public String toString() {
            return "Range start = " + this.start + " end = " + this.end;
        }
    }

}
