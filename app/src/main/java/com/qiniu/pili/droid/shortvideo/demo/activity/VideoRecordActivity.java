package com.qiniu.pili.droid.shortvideo.demo.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.faceunity.core.enumeration.CameraFacingEnum;
import com.faceunity.core.enumeration.FUAIProcessorEnum;
import com.faceunity.core.enumeration.FUInputTextureEnum;
import com.faceunity.core.enumeration.FUTransformMatrixEnum;
import com.faceunity.core.faceunity.FUAIKit;
import com.faceunity.core.faceunity.FURenderKit;
import com.faceunity.core.model.facebeauty.FaceBeautyBlurTypeEnum;
import com.faceunity.core.utils.CameraUtils;
import com.faceunity.nama.FUConfig;
import com.faceunity.nama.FURenderer;
import com.faceunity.nama.data.FaceUnityDataFactory;
import com.faceunity.nama.listener.FURendererListener;
import com.faceunity.nama.ui.FaceUnityView;
import com.faceunity.nama.utils.FuDeviceUtils;
import com.qiniu.pili.droid.shortvideo.PLAudioEncodeSetting;
import com.qiniu.pili.droid.shortvideo.PLCameraPreviewListener;
import com.qiniu.pili.droid.shortvideo.PLCameraSetting;
import com.qiniu.pili.droid.shortvideo.PLCaptureFrameListener;
import com.qiniu.pili.droid.shortvideo.PLDraft;
import com.qiniu.pili.droid.shortvideo.PLDraftBox;
import com.qiniu.pili.droid.shortvideo.PLFaceBeautySetting;
import com.qiniu.pili.droid.shortvideo.PLFocusListener;
import com.qiniu.pili.droid.shortvideo.PLFourCC;
import com.qiniu.pili.droid.shortvideo.PLMicrophoneSetting;
import com.qiniu.pili.droid.shortvideo.PLRecordSetting;
import com.qiniu.pili.droid.shortvideo.PLRecordStateListener;
import com.qiniu.pili.droid.shortvideo.PLShortVideoRecorder;
import com.qiniu.pili.droid.shortvideo.PLVideoEncodeSetting;
import com.qiniu.pili.droid.shortvideo.PLVideoFilterListener;
import com.qiniu.pili.droid.shortvideo.PLVideoFrame;
import com.qiniu.pili.droid.shortvideo.PLVideoSaveListener;
import com.qiniu.pili.droid.shortvideo.demo.R;
import com.qiniu.pili.droid.shortvideo.demo.profile.CSVUtils;
import com.qiniu.pili.droid.shortvideo.demo.profile.Constant;
import com.qiniu.pili.droid.shortvideo.demo.utils.Config;
import com.qiniu.pili.droid.shortvideo.demo.utils.GetPathFromUri;
import com.qiniu.pili.droid.shortvideo.demo.utils.PreferenceUtil;
import com.qiniu.pili.droid.shortvideo.demo.utils.RecordSettings;
import com.qiniu.pili.droid.shortvideo.demo.utils.ToastUtils;
import com.qiniu.pili.droid.shortvideo.demo.view.CustomProgressDialog;
import com.qiniu.pili.droid.shortvideo.demo.view.FocusIndicator;
import com.qiniu.pili.droid.shortvideo.demo.view.SectionProgressBar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Stack;

import static com.qiniu.pili.droid.shortvideo.demo.utils.RecordSettings.RECORD_SPEED_ARRAY;
import static com.qiniu.pili.droid.shortvideo.demo.utils.RecordSettings.chooseCameraFacingId;

public class VideoRecordActivity extends AppCompatActivity implements PLRecordStateListener,
        PLVideoSaveListener, PLFocusListener {
    private static final String TAG = "VideoRecordActivity";

    public static final String PREVIEW_SIZE_RATIO = "PreviewSizeRatio";
    public static final String PREVIEW_SIZE_LEVEL = "PreviewSizeLevel";
    public static final String ENCODING_MODE = "EncodingMode";
    public static final String ENCODING_SIZE_LEVEL = "EncodingSizeLevel";
    public static final String ENCODING_BITRATE_LEVEL = "EncodingBitrateLevel";
    public static final String AUDIO_CHANNEL_NUM = "AudioChannelNum";
    public static final String DRAFT = "draft";

    /**
     * NOTICE: TUSDK needs extra cost
     */
    private static final boolean USE_TUSDK = true;

    private PLShortVideoRecorder mShortVideoRecorder;

    private SectionProgressBar mSectionProgressBar;
    private CustomProgressDialog mProcessingDialog;
    private View mRecordBtn;
    private View mDeleteBtn;
    private View mConcatBtn;
    private View mSwitchCameraBtn;
    private View mSwitchFlashBtn;
    private FocusIndicator mFocusIndicator;
    private SeekBar mAdjustBrightnessSeekBar;

    private TextView mRecordingPercentageView;
    private long mLastRecordingPercentageViewUpdateTime = 0;

    private boolean mFlashEnabled;

    private GestureDetector mGestureDetector;

    private PLCameraSetting mCameraSetting;
    private PLMicrophoneSetting mMicrophoneSetting;
    private PLRecordSetting mRecordSetting;
    private PLVideoEncodeSetting mVideoEncodeSetting;
    private PLAudioEncodeSetting mAudioEncodeSetting;
    private PLFaceBeautySetting mFaceBeautySetting;
    private ViewGroup mBottomControlPanel;
    private TextView mTvFPS;

    private FURenderer mFURenderer;
    private FaceUnityDataFactory mFaceUnityDataFactory;
    private CSVUtils mCSVUtils;
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private int deviceRotation = 90;

    private int mFocusIndicatorX;
    private int mFocusIndicatorY;

    private double mRecordSpeed;
    private TextView mSpeedTextView;

    private Stack<Long> mDurationRecordStack = new Stack();
    private Stack<Double> mDurationVideoStack = new Stack();

    private OrientationEventListener mOrientationListener;
    private boolean mSectionBegan;

    private FURendererListener mFURendererListener = new FURendererListener() {

        @Override
        public void onPrepare() {
            mFaceUnityDataFactory.bindCurrentRenderer();
        }

        @Override
        public void onTrackStatusChanged(FUAIProcessorEnum type, int status) {
            Log.e(TAG, "onTrackStatusChanged: type: " + type + ", size: " + status);
        }

        @Override
        public void onFpsChanged(double fps, double callTime) {
            Log.d(TAG, "onFpsChanged FPS: " + String.format("%.2f", fps) + ", callTime: " + String.format("%.2f", callTime));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTvFPS.setText(String.format("FPS: %.2f", fps));
                }
            });
        }

        @Override
        public void onRelease() {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_record);

        mSectionProgressBar = (SectionProgressBar) findViewById(R.id.record_progressbar);
        GLSurfaceView preview = (GLSurfaceView) findViewById(R.id.preview);
        mRecordBtn = findViewById(R.id.record);
        mDeleteBtn = findViewById(R.id.delete);
        mConcatBtn = findViewById(R.id.concat);
        mSwitchCameraBtn = findViewById(R.id.switch_camera);
        mSwitchFlashBtn = findViewById(R.id.switch_flash);
        mFocusIndicator = (FocusIndicator) findViewById(R.id.focus_indicator);
        mAdjustBrightnessSeekBar = (SeekBar) findViewById(R.id.adjust_brightness);
        mBottomControlPanel = (ViewGroup) findViewById(R.id.bottom_control_panel);
        mRecordingPercentageView = (TextView) findViewById(R.id.recording_percentage);
        mTvFPS = findViewById(R.id.tvFPS);

        mProcessingDialog = new CustomProgressDialog(this);
        mProcessingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mShortVideoRecorder.cancelConcat();
            }
        });

        mShortVideoRecorder = new PLShortVideoRecorder();
        mShortVideoRecorder.setRecordStateListener(this);

        mRecordSpeed = RECORD_SPEED_ARRAY[2];
        mSpeedTextView = (TextView) findViewById(R.id.normal_speed_text);

        String draftTag = getIntent().getStringExtra(DRAFT);
        if (draftTag == null) {
            int previewSizeRatioPos = getIntent().getIntExtra(PREVIEW_SIZE_RATIO, 0);
            int previewSizeLevelPos = getIntent().getIntExtra(PREVIEW_SIZE_LEVEL, 0);
            int encodingModePos = getIntent().getIntExtra(ENCODING_MODE, 0);
            int encodingSizeLevelPos = getIntent().getIntExtra(ENCODING_SIZE_LEVEL, 0);
            int encodingBitrateLevelPos = getIntent().getIntExtra(ENCODING_BITRATE_LEVEL, 0);
            int audioChannelNumPos = getIntent().getIntExtra(AUDIO_CHANNEL_NUM, 0);

            mCameraSetting = new PLCameraSetting();
            PLCameraSetting.CAMERA_FACING_ID facingId = chooseCameraFacingId();
            mCameraSetting.setCameraId(facingId);
            mCameraSetting.setCameraPreviewSizeRatio(RecordSettings.PREVIEW_SIZE_RATIO_ARRAY[previewSizeRatioPos]);
            mCameraSetting.setCameraPreviewSizeLevel(RecordSettings.PREVIEW_SIZE_LEVEL_ARRAY[previewSizeLevelPos]);

            mMicrophoneSetting = new PLMicrophoneSetting();
            mMicrophoneSetting.setChannelConfig(RecordSettings.AUDIO_CHANNEL_NUM_ARRAY[audioChannelNumPos] == 1 ?
                    AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO);

            mVideoEncodeSetting = new PLVideoEncodeSetting(this);
            mVideoEncodeSetting.setEncodingSizeLevel(RecordSettings.ENCODING_SIZE_LEVEL_ARRAY[encodingSizeLevelPos]);
            mVideoEncodeSetting.setEncodingBitrate(RecordSettings.ENCODING_BITRATE_LEVEL_ARRAY[encodingBitrateLevelPos]);
            mVideoEncodeSetting.setHWCodecEnabled(encodingModePos == 0);
            mVideoEncodeSetting.setConstFrameRateEnabled(true);

            mAudioEncodeSetting = new PLAudioEncodeSetting();
            mAudioEncodeSetting.setHWCodecEnabled(encodingModePos == 0);
            mAudioEncodeSetting.setChannels(RecordSettings.AUDIO_CHANNEL_NUM_ARRAY[audioChannelNumPos]);

            mRecordSetting = new PLRecordSetting();
            mRecordSetting.setMaxRecordDuration(RecordSettings.DEFAULT_MAX_RECORD_DURATION);
            mRecordSetting.setRecordSpeedVariable(true);
            mRecordSetting.setVideoCacheDir(Config.VIDEO_STORAGE_DIR);
            mRecordSetting.setVideoFilepath(Config.RECORD_FILE_PATH);

            mFaceBeautySetting = new PLFaceBeautySetting(1.0f, 0.5f, 0.5f);

            mShortVideoRecorder.prepare(preview, mCameraSetting, mMicrophoneSetting, mVideoEncodeSetting,
                    mAudioEncodeSetting, USE_TUSDK ? null : mFaceBeautySetting, mRecordSetting);
            mSectionProgressBar.setFirstPointTime(RecordSettings.DEFAULT_MIN_RECORD_DURATION);
            onSectionCountChanged(0, 0);
        } else {
            PLDraft draft = PLDraftBox.getInstance(this).getDraftByTag(draftTag);
            if (draft == null) {
                ToastUtils.s(this, getString(R.string.toast_draft_recover_fail));
                finish();
            }

            mCameraSetting = draft.getCameraSetting();
            mMicrophoneSetting = draft.getMicrophoneSetting();
            mVideoEncodeSetting = draft.getVideoEncodeSetting();
            mAudioEncodeSetting = draft.getAudioEncodeSetting();
            mRecordSetting = draft.getRecordSetting();
            mFaceBeautySetting = draft.getFaceBeautySetting();

            if (mShortVideoRecorder.recoverFromDraft(preview, draft)) {
                long draftDuration = 0;
                for (int i = 0; i < draft.getSectionCount(); ++i) {
                    long currentDuration = draft.getSectionDuration(i);
                    draftDuration += draft.getSectionDuration(i);
                    onSectionIncreased(currentDuration, draftDuration, i + 1);
                    if (!mDurationRecordStack.isEmpty()) {
                        mDurationRecordStack.pop();
                    }
                }
                mSectionProgressBar.setFirstPointTime(draftDuration);
                ToastUtils.s(this, getString(R.string.toast_draft_recover_success));
            } else {
                onSectionCountChanged(0, 0);
                mSectionProgressBar.setFirstPointTime(RecordSettings.DEFAULT_MIN_RECORD_DURATION );
                ToastUtils.s(this, getString(R.string.toast_draft_recover_fail));
            }
        }
        mShortVideoRecorder.setRecordSpeed(mRecordSpeed);
        mSectionProgressBar.setProceedingSpeed(mRecordSpeed);
        mSectionProgressBar.setTotalTime(this, mRecordSetting.getMaxRecordDuration());


        String isOn = PreferenceUtil.getString(this, PreferenceUtil.KEY_FACEUNITY_ISON);
        FaceUnityView beautyControlView = findViewById(R.id.faceunity_control);
        if ("true".equals(isOn)) {
            if (beautyControlView != null) {
                mFURenderer = FURenderer.getInstance();
                mFURenderer.setInputTextureType(FUInputTextureEnum.FU_ADM_FLAG_COMMON_TEXTURE);
                mFURenderer.setMarkFPSEnable(true);
                mFaceUnityDataFactory = new FaceUnityDataFactory(-1);
                beautyControlView.bindDataFactory(mFaceUnityDataFactory);
            }
        } else {
            beautyControlView.setVisibility(View.GONE);
        }

        mShortVideoRecorder.setVideoFilterListener(new PLVideoFilterListener() {

            /**
             * 检查当前人脸数量
             */
            private void cheekFaceNum() {
                //根据有无人脸 + 设备性能 判断开启的磨皮类型
                float faceProcessorGetConfidenceScore = FUAIKit.getInstance().getFaceProcessorGetConfidenceScore(0);
                if (faceProcessorGetConfidenceScore >= 0.95) {
                    //高端手机并且检测到人脸开启均匀磨皮，人脸点位质
                    if (FURenderKit.getInstance().getFaceBeauty() != null && FURenderKit.getInstance().getFaceBeauty().getBlurType() != FaceBeautyBlurTypeEnum.EquallySkin) {
                        FURenderKit.getInstance().getFaceBeauty().setBlurType(FaceBeautyBlurTypeEnum.EquallySkin);
                        FURenderKit.getInstance().getFaceBeauty().setEnableBlurUseMask(true);
                    }
                } else {
                    if (FURenderKit.getInstance().getFaceBeauty() != null && FURenderKit.getInstance().getFaceBeauty().getBlurType() != FaceBeautyBlurTypeEnum.FineSkin) {
                        FURenderKit.getInstance().getFaceBeauty().setBlurType(FaceBeautyBlurTypeEnum.FineSkin);
                        FURenderKit.getInstance().getFaceBeauty().setEnableBlurUseMask(false);
                    }
                }
            }


            @Override
            public void onSurfaceCreated() {
                Log.d(TAG, "onSurfaceCreated: ");
                initCsvUtil(VideoRecordActivity.this);
                if (mFURenderer != null) {
                    mFURenderer.prepareRenderer(mFURendererListener);
                }
            }

            @Override
            public void onSurfaceChanged(int width, int height) {
                Log.d(TAG, "onSurfaceChanged() width = [" + width + "], height = [" + height + "]");
            }

            @Override
            public void onSurfaceDestroy() {
                Log.d(TAG, "onSurfaceDestroy: ");
                if (mCSVUtils != null) {
                    mCSVUtils.close();
                }
                mFURenderer.release();
            }

            /**
             * 绘制帧时触发 GL线程回调
             *
             * @param texId           待渲染的 SurfaceTexture 对象的 texture ID
             * @param width           绘制的 surface 宽度
             * @param height          绘制的 surface 高度
             * @param timeStampNs     该帧的时间戳，单位 Ns
             * @param transformMatrix 一般为单位矩阵，除非返回的纹理类型为 OES
             * @return 新创建的制定给 SurfaceTexuture 对象的 texture ID，类型必须为 GL_TEXTURE_2D
             */
            @Override
            public int onDrawFrame(int texId, int width, int height, long timeStampNs, float[] transformMatrix) {
                if (mFURenderer == null) {
                    return texId;
                }

                if (FUConfig.DEVICE_LEVEL > FuDeviceUtils.DEVICE_LEVEL_MID) {
                    cheekFaceNum();
                }

                long start = System.nanoTime();
                int fuTexId = mFURenderer.onDrawFrameSingleInput(texId, width, height);
                long renderTime = System.nanoTime() - start;

                if (mCSVUtils != null) {
                    mCSVUtils.writeCsv(null, renderTime);
                }
                Log.d(TAG, "onDrawFrame: texId:" + texId + ", fuTexId:" + fuTexId + ", texWidth:" + width + ", texHeight:"
                        + height + ", time:" + timeStampNs + ", t:" + Thread.currentThread().getId() + ", mtx:" + Arrays.toString(transformMatrix));

                return fuTexId;
            }
        });


        mRecordBtn.setOnTouchListener(new View.OnTouchListener() {
            private long mSectionBeginTSMs;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    if (!mSectionBegan && mShortVideoRecorder.beginSection()) {
                        mSectionBegan = true;
                        mSectionBeginTSMs = System.currentTimeMillis();
                        mSectionProgressBar.setCurrentState(SectionProgressBar.State.START);
                        updateRecordingBtns(true);
                    } else {
                        ToastUtils.s(VideoRecordActivity.this, "无法开始视频段录制");
                    }
                } else if (action == MotionEvent.ACTION_UP) {
                    if (mSectionBegan) {
                        long sectionRecordDurationMs = System.currentTimeMillis() - mSectionBeginTSMs;
                        long totalRecordDurationMs = sectionRecordDurationMs + (mDurationRecordStack.isEmpty() ? 0 : mDurationRecordStack.peek().longValue());
                        double sectionVideoDurationMs = sectionRecordDurationMs / mRecordSpeed;
                        double totalVideoDurationMs = sectionVideoDurationMs + (mDurationVideoStack.isEmpty() ? 0 : mDurationVideoStack.peek().doubleValue());
                        mDurationRecordStack.push(new Long(totalRecordDurationMs));
                        mDurationVideoStack.push(new Double(totalVideoDurationMs));
                        if (mRecordSetting.IsRecordSpeedVariable()) {
                            Log.d(TAG,"SectionRecordDuration: " + sectionRecordDurationMs + "; sectionVideoDuration: " + sectionVideoDurationMs + "; totalVideoDurationMs: " + totalVideoDurationMs + "Section count: " + mDurationVideoStack.size());
                            mSectionProgressBar.addBreakPointTime((long) totalVideoDurationMs);
                        } else {
                            mSectionProgressBar.addBreakPointTime(totalRecordDurationMs);
                        }

                        mSectionProgressBar.setCurrentState(SectionProgressBar.State.PAUSE);
                        mShortVideoRecorder.endSection();
                        mSectionBegan = false;
                    }
                }

                return false;
            }
        });
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                mFocusIndicatorX = (int) e.getX() - mFocusIndicator.getWidth() / 2;
                mFocusIndicatorY = (int) e.getY() - mFocusIndicator.getHeight() / 2;
                mShortVideoRecorder.manualFocus(mFocusIndicator.getWidth(), mFocusIndicator.getHeight(), (int) e.getX(), (int) e.getY());
                return false;
            }
        });
        preview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mGestureDetector.onTouchEvent(motionEvent);
                return true;
            }
        });

        mOrientationListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                int rotation = getScreenRotation(orientation);
                if (!mSectionProgressBar.isRecorded() && !mSectionBegan) {
                    mVideoEncodeSetting.setRotationInMetadata(rotation);
                }
                if (null != mFURenderer && deviceRotation != rotation) {
                    mFURenderer.setDeviceOrientation((rotation + 180) % 360);
                    deviceRotation = rotation;
                }
            }
        };
        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable();
        }
    }

    private void initCsvUtil(Context context) {
        mCSVUtils = new CSVUtils(context);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
        String dateStrDir = format.format(new Date(System.currentTimeMillis()));
        dateStrDir = dateStrDir.replaceAll("-", "").replaceAll("_", "");
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault());
        String dateStrFile = df.format(new Date());
        String filePath = Constant.filePath + dateStrDir + File.separator + "excel-" + dateStrFile + ".csv";
        Log.d(TAG, "initLog: CSV file path:" + filePath);
        StringBuilder headerInfo = new StringBuilder();
        headerInfo.append("version：").append(FURenderer.getInstance().getVersion()).append(CSVUtils.COMMA)
                .append("机型：").append(android.os.Build.MANUFACTURER).append(android.os.Build.MODEL)
                .append("处理方式：Texture").append(CSVUtils.COMMA);
        mCSVUtils.initHeader(filePath, headerInfo);
    }

    private int getScreenRotation(int orientation) {
        int screenRotation = 0;
        boolean isPortraitScreen = getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        if (orientation >= 315 || orientation < 45) {
            screenRotation = isPortraitScreen ? 0 : 90;
        } else if (orientation >= 45 && orientation < 135) {
            screenRotation = isPortraitScreen ? 90 : 180;
        } else if (orientation >= 135 && orientation < 225) {
            screenRotation = isPortraitScreen ? 180 : 270;
        } else if (orientation >= 225 && orientation < 315) {
            screenRotation = isPortraitScreen ? 270 : 0;
        }
        return screenRotation;
    }

    private void updateRecordingBtns(boolean isRecording) {
        mSwitchCameraBtn.setEnabled(!isRecording);
        mRecordBtn.setActivated(isRecording);
    }

    public void onScreenRotation(View v) {
        if (mDeleteBtn.isEnabled()) {
            ToastUtils.s(this, "已经开始拍摄，无法旋转屏幕。");
        } else {
            setRequestedOrientation(
                    getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ?
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE :
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    public void onCaptureFrame(View v) {
        mShortVideoRecorder.captureFrame(new PLCaptureFrameListener() {
            @Override
            public void onFrameCaptured(PLVideoFrame capturedFrame) {
                if (capturedFrame == null) {
                    Log.e(TAG, "capture frame failed");
                    return;
                }

                Log.i(TAG, "captured frame width: " + capturedFrame.getWidth() + " height: " + capturedFrame.getHeight() + " timestamp: " + capturedFrame.getTimestampMs());
                try {
                    FileOutputStream fos = new FileOutputStream(Config.CAPTURED_FRAME_FILE_PATH);
                    capturedFrame.toBitmap().compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ToastUtils.s(VideoRecordActivity.this, "截帧已保存到路径：" + Config.CAPTURED_FRAME_FILE_PATH);
                        }
                    });
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        mRecordBtn.setEnabled(false);
        mShortVideoRecorder.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateRecordingBtns(false);
        mShortVideoRecorder.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mShortVideoRecorder.destroy();
        mOrientationListener.disable();
    }

    public void onClickDelete(View v) {
        if (!mShortVideoRecorder.deleteLastSection()) {
            ToastUtils.s(this, "回删视频段失败");
        }
    }

    public void onClickConcat(View v) {
        mProcessingDialog.show();
        mShortVideoRecorder.concatSections(this);
    }

    public void onClickBrightness(View v) {
        boolean isVisible = mAdjustBrightnessSeekBar.getVisibility() == View.VISIBLE;
        mAdjustBrightnessSeekBar.setVisibility(isVisible ? View.GONE : View.VISIBLE);
    }

    public void onClickSwitchCamera(View v) {
        mShortVideoRecorder.switchCamera();
        mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT - mCameraId;
        mFocusIndicator.focusCancel();
    }

    public void onClickSwitchFlash(View v) {
        mFlashEnabled = !mFlashEnabled;
        mShortVideoRecorder.setFlashEnabled(mFlashEnabled);
        mSwitchFlashBtn.setActivated(mFlashEnabled);
    }

    public void onClickAddMixAudio(View v) {
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT < 19) {
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/*");
        } else {
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("audio/*");
        }
        startActivityForResult(Intent.createChooser(intent, "请选择混音文件："), 0);
    }

    public void onClickSaveToDraft(View v) {
        final EditText editText = new EditText(this);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this)
                .setView(editText)
                .setTitle(getString(R.string.dlg_save_draft_title))
                .setPositiveButton(getString(R.string.dlg_save_draft_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ToastUtils.s(VideoRecordActivity.this,
                                mShortVideoRecorder.saveToDraftBox(editText.getText().toString()) ?
                                        getString(R.string.toast_draft_save_success) : getString(R.string.toast_draft_save_fail));
                    }
                });
        alertDialog.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            String selectedFilepath = GetPathFromUri.getPath(this, data.getData());
            Log.i(TAG, "Select file: " + selectedFilepath);
            if (selectedFilepath != null && !"".equals(selectedFilepath)) {
                mShortVideoRecorder.setMusicFile(selectedFilepath);
            }
        }
    }

    @Override
    public void onReady() {
        mShortVideoRecorder.setFocusListener(this);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSwitchFlashBtn.setVisibility(mShortVideoRecorder.isFlashSupport() ? View.VISIBLE : View.GONE);
                mFlashEnabled = false;
                mSwitchFlashBtn.setActivated(mFlashEnabled);
                mRecordBtn.setEnabled(true);
                refreshSeekBar();
                ToastUtils.s(VideoRecordActivity.this, "可以开始拍摄咯");
            }
        });
    }

    @Override
    public void onError(final int code) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtils.toastErrorCode(VideoRecordActivity.this, code);
            }
        });
    }

    @Override
    public void onDurationTooShort() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtils.s(VideoRecordActivity.this, "该视频段太短了");
            }
        });
    }

    @Override
    public void onRecordStarted() {
        Log.i(TAG, "record start time: " + System.currentTimeMillis());
    }

    @Override
    public void onRecordStopped() {
        Log.i(TAG, "record stop time: " + System.currentTimeMillis());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateRecordingBtns(false);
            }
        });
    }

    @Override
    public void onSectionRecording(long sectionDurationMs, long videoDurationMs, int sectionCount) {
        Log.d(TAG, "sectionDurationMs: " + sectionDurationMs + "; videoDurationMs: " + videoDurationMs + "; sectionCount: " + sectionCount);
        updateRecordingPercentageView(videoDurationMs);
    }

    @Override
    public void onSectionIncreased(long incDuration, long totalDuration, int sectionCount) {
        double videoSectionDuration = mDurationVideoStack.isEmpty() ? 0 : mDurationVideoStack.peek().doubleValue();
        if ((videoSectionDuration + incDuration / mRecordSpeed) >= mRecordSetting.getMaxRecordDuration()) {
            videoSectionDuration = mRecordSetting.getMaxRecordDuration();
        }
        Log.d(TAG, "videoSectionDuration: " + videoSectionDuration + "; incDuration: " + incDuration);
        onSectionCountChanged(sectionCount, (long) videoSectionDuration);
    }

    @Override
    public void onSectionDecreased(long decDuration, long totalDuration, int sectionCount) {
        mSectionProgressBar.removeLastBreakPoint();
        if (!mDurationVideoStack.isEmpty()) {
            mDurationVideoStack.pop();
        }
        if (!mDurationRecordStack.isEmpty()) {
            mDurationRecordStack.pop();
        }
        double currentDuration = mDurationVideoStack.isEmpty() ? 0 : mDurationVideoStack.peek().doubleValue();
        onSectionCountChanged(sectionCount, (long) currentDuration);
        updateRecordingPercentageView((long) currentDuration);
    }

    @Override
    public void onRecordCompleted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtils.s(VideoRecordActivity.this, "已达到拍摄总时长");
            }
        });
    }

    @Override
    public void onProgressUpdate(final float percentage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProcessingDialog.setProgress((int) (100 * percentage));
            }
        });
    }

    @Override
    public void onSaveVideoFailed(final int errorCode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProcessingDialog.dismiss();
                ToastUtils.s(VideoRecordActivity.this, "拼接视频段失败: " + errorCode);
            }
        });
    }

    @Override
    public void onSaveVideoCanceled() {
        mProcessingDialog.dismiss();
    }

    @Override
    public void onSaveVideoSuccess(final String filePath) {
        Log.i(TAG, "concat sections success filePath: " + filePath);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProcessingDialog.dismiss();
                int screenOrientation = (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE == getRequestedOrientation()) ? 0 : 1;
                PlaybackActivity.start(VideoRecordActivity.this, filePath, screenOrientation);
            }
        });
    }

    private void updateRecordingPercentageView(long currentDuration) {
        final int per = (int) (100 * currentDuration / mRecordSetting.getMaxRecordDuration());
        final long curTime = System.currentTimeMillis();
        if ((mLastRecordingPercentageViewUpdateTime != 0) && (curTime - mLastRecordingPercentageViewUpdateTime < 100)) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecordingPercentageView.setText((per > 100 ? 100 : per) + "%");
                mLastRecordingPercentageViewUpdateTime = curTime;
            }
        });
    }

    private void refreshSeekBar() {
        final int max = mShortVideoRecorder.getMaxExposureCompensation();
        final int min = mShortVideoRecorder.getMinExposureCompensation();
        boolean brightnessAdjustAvailable = (max != 0 || min != 0);
        Log.e(TAG, "max/min exposure compensation: " + max + "/" + min + " brightness adjust available: " + brightnessAdjustAvailable);

        findViewById(R.id.brightness_panel).setVisibility(brightnessAdjustAvailable ? View.VISIBLE : View.GONE);
        mAdjustBrightnessSeekBar.setOnSeekBarChangeListener(!brightnessAdjustAvailable ? null : new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (i <= Math.abs(min)) {
                    mShortVideoRecorder.setExposureCompensation(i + min);
                } else {
                    mShortVideoRecorder.setExposureCompensation(i - max);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mAdjustBrightnessSeekBar.setMax(max + Math.abs(min));
        mAdjustBrightnessSeekBar.setProgress(Math.abs(min));
    }

    private void onSectionCountChanged(final int count, final long totalTime) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDeleteBtn.setEnabled(count > 0);
                mConcatBtn.setEnabled(totalTime >= (RecordSettings.DEFAULT_MIN_RECORD_DURATION));
            }
        });
    }

    public void onSpeedClicked(View view) {
        if (!mVideoEncodeSetting.IsConstFrameRateEnabled() || !mRecordSetting.IsRecordSpeedVariable()) {
            if (mSectionProgressBar.isRecorded()) {
                ToastUtils.s(this, "变帧率模式下，无法在拍摄中途修改拍摄倍数！");
                return;
            }
        }

        if (mSpeedTextView != null) {
            mSpeedTextView.setTextColor(getResources().getColor(R.color.speedTextNormal));
        }

        TextView textView = (TextView) view;
        textView.setTextColor(getResources().getColor(R.color.colorAccent));
        mSpeedTextView = textView;

        switch (view.getId()) {
            case R.id.super_slow_speed_text:
                mRecordSpeed = RECORD_SPEED_ARRAY[0];
                break;
            case R.id.slow_speed_text:
                mRecordSpeed = RECORD_SPEED_ARRAY[1];
                break;
            case R.id.normal_speed_text:
                mRecordSpeed = RECORD_SPEED_ARRAY[2];
                break;
            case R.id.fast_speed_text:
                mRecordSpeed = RECORD_SPEED_ARRAY[3];
                break;
            case R.id.super_fast_speed_text:
                mRecordSpeed = RECORD_SPEED_ARRAY[4];
                break;
        }

        mShortVideoRecorder.setRecordSpeed(mRecordSpeed);
        if (mRecordSetting.IsRecordSpeedVariable() && mVideoEncodeSetting.IsConstFrameRateEnabled()) {
            mSectionProgressBar.setProceedingSpeed(mRecordSpeed);
            mRecordSetting.setMaxRecordDuration(RecordSettings.DEFAULT_MAX_RECORD_DURATION);
            mSectionProgressBar.setFirstPointTime(RecordSettings.DEFAULT_MIN_RECORD_DURATION);
        } else {
            mRecordSetting.setMaxRecordDuration((long) (RecordSettings.DEFAULT_MAX_RECORD_DURATION * mRecordSpeed));
            mSectionProgressBar.setFirstPointTime((long) (RecordSettings.DEFAULT_MIN_RECORD_DURATION * mRecordSpeed));
        }

        mSectionProgressBar.setTotalTime(this, mRecordSetting.getMaxRecordDuration());
    }

    @Override
    public void onManualFocusStart(boolean result) {
        if (result) {
            Log.i(TAG, "manual focus begin success");
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mFocusIndicator.getLayoutParams();
            lp.leftMargin = mFocusIndicatorX;
            lp.topMargin = mFocusIndicatorY;
            mFocusIndicator.setLayoutParams(lp);
            mFocusIndicator.focus();
        } else {
            mFocusIndicator.focusCancel();
            Log.i(TAG, "manual focus not supported");
        }
    }

    @Override
    public void onManualFocusStop(boolean result) {
        Log.i(TAG, "manual focus end result: " + result);
        if (result) {
            mFocusIndicator.focusSuccess();
        } else {
            mFocusIndicator.focusFail();
        }
    }

    @Override
    public void onManualFocusCancel() {
        Log.i(TAG, "manual focus canceled");
        mFocusIndicator.focusCancel();
    }

    @Override
    public void onAutoFocusStart() {
        Log.i(TAG, "auto focus start");
    }

    @Override
    public void onAutoFocusStop() {
        Log.i(TAG, "auto focus stop");
    }

}
