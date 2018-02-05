package com.faceunity.wrapper;

import android.content.Context;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.faceunity.wrapper.gles.FullFrameRect;
import com.faceunity.wrapper.gles.Texture2dProgram;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;


public class FaceunityWrapper {
    private static final String TAG = FaceunityWrapper.class.getName();

    public final static String[][] EFFECT_ITEM_FILE_NAME = {
            /*Animoji*/
            {"none", "xiongmao2.bundle"},

            /*3D*/
            {"none", "sdx2.bundle", "itd.bundle"},

            /*2D*/
            {"none", "multi_dianji_crt_fu.bundle", "lhudie_zh_fu.bundle"},

            /*face change*/
            {"none", "afd.bundle", "baozi.mp3"},

            /*Avatar*/
            {"none", "nick.bundle"},

            /*magic*/
            {"none", "hez_ztt_fu.bundle", "xiandai_ztt_fu.bundle"},

            /*gesture*/
            {"none", "fu_ztt_live520.bundle", "fu_zh_baoquan.bundle",},

            /*film filter*/
            {"none", "f_gradient.bundle", "f_movie.bundle"},
    };

    private Context mContext;

    private int mCameraRotate;
    private int mCameraId;
    private float[] mCameraMatrix1;
    private float[] mCameraMatrix2;

    private byte[] mCameraNV21Byte;
    private byte[] fuImgNV21Bytes;

    private int mFrameId = 0;

    private int mFaceBeautyItem = 0; //美颜道具
    private volatile EffectItem mEffectItem = new EffectItem(); //贴纸道具

    private float mFilterLevel = 1.0f;
    private float mFaceBeautyColorLevel = 0.2f;
    private float mFaceBeautyBlurLevel = 6.0f;
    private float mFaceBeautyALLBlurLevel = 0.0f;
    private float mFaceBeautyCheekThin = 1.0f;
    private float mFaceBeautyEnlargeEye = 0.5f;
    private float mFaceBeautyRedLevel = 0.5f;
    private int mFaceShape = 3;
    private float mFaceShapeLevel = 0.5f;

    private String mFilterName = EffectAndFilterSelectAdapter.FILTERS_NAME[0];

    private boolean isNeedEffectItem = true;
    private String mEffectFileName = EFFECT_ITEM_FILE_NAME[0][1];

    private int mCurrentCameraId;
    private int inputImageOrientation;

    private HandlerThread mCreateItemThread;
    private Handler mCreateItemHandler;

    private int faceTrackingStatus = 0;

    private long lastOneHundredFrameTimeStamp = 0;
    private int currentFrameCnt = 0;
    private long oneHundredFrameFUTime = 0;

    private boolean isBenchmarkFPS = true;
    private boolean isBenchmarkTime = false;

    private FullFrameRect mFullScreenFUDisplay;

    public FaceunityWrapper(Context context, int cameraFaceId) {
        Log.e(TAG, "FaceunityWrapper = " + Thread.currentThread().getId());
        mContext = context;
        setCameraId(cameraFaceId);
    }

    public void onSurfaceCreated(Context context) {
        Log.e(TAG, "onSurfaceCreated = " + Thread.currentThread().getId());
        mCreateItemThread = new HandlerThread("faceunity-efect");
        mCreateItemThread.start();
        mCreateItemHandler = new CreateItemHandler(mCreateItemThread.getLooper());

        mFullScreenFUDisplay = new FullFrameRect(new Texture2dProgram(
                Texture2dProgram.ProgramType.TEXTURE_2D));

        try {
            InputStream is = context.getAssets().open("v3.bundle");
            byte[] v3data = new byte[is.available()];
            int len = is.read(v3data);
            is.close();
            faceunity.fuSetup(v3data, null, authpack.A());
            //faceunity.fuSetMaxFaces(1);//设置最大识别人脸数目
            Log.e(TAG, "fuSetup v3 len " + len);

            is = context.getAssets().open("anim_model.bundle");
            byte[] animModelData = new byte[is.available()];
            is.read(animModelData);
            is.close();
            faceunity.fuLoadAnimModel(animModelData);
            faceunity.fuSetExpressionCalibration(1);

            is = context.getAssets().open("face_beautification.bundle");
            byte[] itemData = new byte[is.available()];
            len = is.read(itemData);
            Log.e(TAG, "beautification len " + len);
            is.close();
            mFaceBeautyItem = faceunity.fuCreateItemFromPackage(itemData);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void onSurfaceDestroyed() {
        Log.e(TAG, "onSurfaceDestroyed = " + Thread.currentThread().getId());
        mFrameId = 0;

        mCreateItemHandler.removeMessages(CreateItemHandler.HANDLE_CREATE_ITEM);
        mCreateItemHandler = null;
        mCreateItemThread.quitSafely();
        mCreateItemThread = null;

        mEffectItem = new EffectItem();
        mFaceBeautyItem = 0;
        //Note: 切忌使用一个已经destroy的item
        faceunity.fuDestroyAllItems();
        isNeedEffectItem = true;
        faceunity.fuOnDeviceLost();

        deleteFBO();

        lastOneHundredFrameTimeStamp = 0;
        oneHundredFrameFUTime = 0;
    }

    public void setCameraId(int CameraId) {
        mCurrentCameraId = CameraId;
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(mCurrentCameraId, info);
        inputImageOrientation = info.orientation;
        mEffectItem.paramReady = false;
    }

    /**
     * 对纹理进行特效处理
     *
     * @param texId     YUV格式纹理
     * @param texWidth  纹理宽度
     * @param texHeight 纹理高度
     * @return 特效处理后的纹理
     */
    public int onDrawFrame(int texId, int texWidth, int texHeight, float[] transformMatrix) {
        if (++currentFrameCnt == 100) {
            currentFrameCnt = 0;
            long tmp = System.nanoTime();
            if (isBenchmarkFPS)
                Log.e(TAG, "dualInput FPS : " + (1000.0f * MiscUtil.NANO_IN_ONE_MILLI_SECOND / ((tmp - lastOneHundredFrameTimeStamp) / 100.0f)));
            lastOneHundredFrameTimeStamp = tmp;
            if (isBenchmarkTime)
                Log.e(TAG, "dualInput cost time avg : " + oneHundredFrameFUTime / 100.f / MiscUtil.NANO_IN_ONE_MILLI_SECOND);
            oneHundredFrameFUTime = 0;
        }

        if (mCameraMatrix1 == null || mCameraMatrix2 == null) {
            Log.e(TAG, "mCameraMatrix1 or mCameraMatrix1 is null.");
            return texId;
        }

        if (mCameraNV21Byte == null || mCameraNV21Byte.length == 0) {
            Log.e(TAG, "camera nv21 bytes null");
            return texId;
        }

        createFBO(texWidth, texHeight);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId[0]);

        mFullScreenFUDisplay.drawFrame(texId, mCameraMatrix1);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId[1]);

        final int isTracking = faceunity.fuIsTracking();
        if (isTracking != faceTrackingStatus) {
            faceTrackingStatus = isTracking;
        }

        if (isNeedEffectItem) {
            isNeedEffectItem = false;
            mCreateItemHandler.sendEmptyMessage(CreateItemHandler.HANDLE_CREATE_ITEM);
        }

        if (!mEffectItem.paramReady) {
            faceunity.fuItemSetParam(mEffectItem.item, "isAndroid", 1.0);
            faceunity.fuItemSetParam(mEffectItem.item, "rotationAngle", (360 - inputImageOrientation));
            mEffectItem.paramReady = true;
        }

        faceunity.fuItemSetParam(mFaceBeautyItem, "filter_level", mFilterLevel);
        faceunity.fuItemSetParam(mFaceBeautyItem, "color_level", mFaceBeautyColorLevel);
        faceunity.fuItemSetParam(mFaceBeautyItem, "blur_level", mFaceBeautyBlurLevel);
        faceunity.fuItemSetParam(mFaceBeautyItem, "skin_detect", mFaceBeautyALLBlurLevel);
        faceunity.fuItemSetParam(mFaceBeautyItem, "filter_name", mFilterName);
        faceunity.fuItemSetParam(mFaceBeautyItem, "cheek_thinning", mFaceBeautyCheekThin);
        faceunity.fuItemSetParam(mFaceBeautyItem, "eye_enlarging", mFaceBeautyEnlargeEye);
        faceunity.fuItemSetParam(mFaceBeautyItem, "face_shape", mFaceShape);
        faceunity.fuItemSetParam(mFaceBeautyItem, "face_shape_level", mFaceShapeLevel);
        faceunity.fuItemSetParam(mFaceBeautyItem, "red_level", mFaceBeautyRedLevel);

        //faceunity.fuItemSetParam(mFacebeautyItem, "use_old_blur", 1);

        boolean isOESTexture = false; //camera默认的是OES的
        int flags = isOESTexture ? faceunity.FU_ADM_FLAG_EXTERNAL_OES_TEXTURE : 0;
        boolean isNeedReadBack = false; //是否需要写回，如果是，则入参的byte[]会被修改为带有fu特效的
        flags = isNeedReadBack ? flags | faceunity.FU_ADM_FLAG_ENABLE_READBACK : flags;
        if (isNeedReadBack) {
            if (fuImgNV21Bytes == null) {
                fuImgNV21Bytes = new byte[mCameraNV21Byte.length];
            }
            System.arraycopy(mCameraNV21Byte, 0, fuImgNV21Bytes, 0, mCameraNV21Byte.length);
        } else {
            fuImgNV21Bytes = mCameraNV21Byte;
        }
        flags |= mCurrentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT ? 0 : faceunity.FU_ADM_FLAG_FLIP_X;

        long fuStartTime = System.nanoTime();
        /**
         * 这里拿到fu处理过后的texture，可以对这个texture做后续操作，如硬编、预览。
         */
        int fuTex;
        if (mEffectItem.isAvatar) {
            fuTex = drawAvatar(texHeight, texWidth);
        } else {
            fuTex = faceunity.fuDualInputToTexture(fuImgNV21Bytes, fboTex[0], flags,
                    texHeight, texWidth, mFrameId++, new int[]{mFaceBeautyItem, mEffectItem.item});
        }
        long fuEndTime = System.nanoTime();
        oneHundredFrameFUTime += fuEndTime - fuStartTime;

        mFullScreenFUDisplay.drawFrame(fuTex, mCameraMatrix2);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        return fboTex[1];
    }

    public boolean onPreviewFrame(byte[] data, int width, int height, int rotation, int fmt, long timestampNs) {

        mCameraNV21Byte = data;

        if (mCameraRotate != rotation || mCameraId != mCurrentCameraId || mCameraMatrix1 == null || mCameraMatrix2 == null) {
            mCameraRotate = rotation;
            mCameraId = mCurrentCameraId;
            mCameraMatrix1 = new float[16];
            mCameraMatrix2 = new float[16];
            if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                Matrix.setRotateM(mCameraMatrix1, 0, mCameraRotate, 0F, 0F, 1F);
                Matrix.setRotateM(mCameraMatrix2, 0, -mCameraRotate, 0F, 0F, 1F);
            } else {
                Matrix.setIdentityM(mCameraMatrix1, 0);
                mCameraMatrix1[0] = -1;
                Matrix.rotateM(mCameraMatrix1, 0, -mCameraRotate, 0F, 0F, 1F);
                Matrix.setIdentityM(mCameraMatrix2, 0);
                mCameraMatrix2[0] = -1;
                Matrix.rotateM(mCameraMatrix2, 0, -mCameraRotate, 0F, 0F, 1F);
            }
        }
        return false;
    }

    float[] expressionData = new float[46];
    float[] rotationData = new float[4];
    float[] pupilPosData = new float[2];
    float[] rotationModeData = new float[1];

    int drawAvatar(int mCameraWidth, int mCameraHeight) {
        faceunity.fuTrackFace(mCameraNV21Byte, 0, mCameraWidth, mCameraHeight);

        /**
         *rotation
         */
        Arrays.fill(rotationData, 0.0f);
        faceunity.fuGetFaceInfo(0, "rotation", rotationData);
        /**
         * expression
         */
        Arrays.fill(expressionData, 0.0f);
        faceunity.fuGetFaceInfo(0, "expression", expressionData);

        /**
         * pupil pos
         */
        Arrays.fill(pupilPosData, 0.0f);
        faceunity.fuGetFaceInfo(0, "pupil_pos", pupilPosData);

        /**
         * rotation mode
         */
        Arrays.fill(rotationModeData, 0.0f);
        faceunity.fuGetFaceInfo(0, "rotation_mode", rotationModeData);

        int isTracking = faceunity.fuIsTracking();

        //rotation 是一个4元数，如果还没获取到，就使用1,0,0,0
        if (isTracking <= 0) {
            rotationData[3] = 1.0f;
        }

        /**
         * adjust rotation mode
         */
        if (isTracking <= 0) {
            rotationModeData[0] = (360 - mCameraRotate) / 90;
        }

        return faceunity.fuAvatarToTexture(pupilPosData,
                expressionData,
                rotationData,
                rotationModeData,
                0,
                mCameraWidth,
                mCameraHeight,
                mFrameId++,
                new int[]{mEffectItem.item},
                isTracking);
    }

    public FaceunityControlView.OnViewEventListener initUIEventListener() {

        FaceunityControlView.OnViewEventListener eventListener = new FaceunityControlView.OnViewEventListener() {
            @Override
            public void onEffectSelected(String effectItemName) {
                if (effectItemName.equals(mEffectFileName)) {
                    return;
                }
                mCreateItemHandler.removeMessages(CreateItemHandler.HANDLE_CREATE_ITEM);
                mEffectFileName = effectItemName;
                isNeedEffectItem = true;
            }

            @Override
            public void onFilterLevelSelected(int progress, int max) {
                mFilterLevel = 1.0f * progress / max;
            }

            @Override
            public void onFilterSelected(String filterName) {
                mFilterName = filterName;
            }

            @Override
            public void onBlurLevelSelected(int level) {
                mFaceBeautyBlurLevel = level;
            }

            @Override
            public void onALLBlurLevelSelected(int isAll) {
                mFaceBeautyALLBlurLevel = isAll;
            }

            @Override
            public void onColorLevelSelected(int progress, int max) {
                mFaceBeautyColorLevel = 1.0f * progress / max;
            }

            @Override
            public void onCheekThinSelected(int progress, int max) {
                mFaceBeautyCheekThin = 1.0f * progress / max;
            }

            @Override
            public void onEnlargeEyeSelected(int progress, int max) {
                mFaceBeautyEnlargeEye = 1.0f * progress / max;
            }

            @Override
            public void onCameraChange() {

            }

            @Override
            public void onStartRecording() {

            }

            @Override
            public void onStopRecording() {

            }

            @Override
            public void onFaceShapeSelected(int faceShape) {
                mFaceShape = faceShape;
            }

            @Override
            public void onFaceShapeLevelSelected(int progress, int max) {
                mFaceShapeLevel = (1.0f * progress) / max;
            }

            @Override
            public void onRedLevelSelected(int progress, int max) {
                mFaceBeautyRedLevel = 1.0f * progress / max;
            }
        };

        return eventListener;
    }

    private class EffectItem {
        int item;
        boolean paramReady;
        boolean isAvatar;
    }

    class CreateItemHandler extends Handler {

        static final int HANDLE_CREATE_ITEM = 1;

        CreateItemHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLE_CREATE_ITEM:
                    try {
                        final int tmp = mEffectItem.item;
                        if (mEffectFileName.equals("none")) {
                            mEffectItem = new EffectItem();
                        } else {
                            InputStream is = mContext.getAssets().open(mEffectFileName);
                            byte[] itemData = new byte[is.available()];
                            int len = is.read(itemData);
                            Log.e("FU", "effect len " + len);
                            is.close();
                            EffectItem effectItem = new EffectItem();
                            effectItem.item = faceunity.fuCreateItemFromPackage(itemData);
                            effectItem.isAvatar = Arrays.asList(EffectAndFilterSelectAdapter.AVATAR_EFFECT).contains(mEffectFileName);
                            mEffectItem = effectItem;
                        }
                        if (tmp != 0) {
                            faceunity.fuDestroyItem(tmp);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    private int fboId[];
    private int fboTex[];
    private int renderBufferId[];

    private void createFBO(int width, int height) {
        if (fboTex == null) {
            fboId = new int[2];
            fboTex = new int[2];
            renderBufferId = new int[2];

//generate fbo id
            GLES20.glGenFramebuffers(2, fboId, 0);
//generate texture
            GLES20.glGenTextures(2, fboTex, 0);
//generate render buffer
            GLES20.glGenRenderbuffers(2, renderBufferId, 0);

            for (int i = 0; i < fboId.length; i++) {
//Bind Frame buffer
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId[i]);
//Bind texture
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTex[i]);
//Define texture parameters
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
//Bind render buffer and define buffer dimension
                GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, renderBufferId[i]);
                GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height);
//Attach texture FBO color attachment
                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, fboTex[i], 0);
//Attach render buffer to depth attachment
                GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, renderBufferId[i]);
//we are done, reset
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
                GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            }
        }
    }

    private void deleteFBO() {
        if (fboId == null || fboTex == null || renderBufferId == null) {
            return;
        }
        GLES20.glDeleteFramebuffers(2, fboId, 0);
        GLES20.glDeleteTextures(2, fboTex, 0);
        GLES20.glDeleteRenderbuffers(2, renderBufferId, 0);
        fboId = null;
        fboTex = null;
        renderBufferId = null;
    }

    public int getFaceTrackingStatus() {
        return faceTrackingStatus;
    }
}
