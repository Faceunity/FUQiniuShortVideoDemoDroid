package com.qiniu.pili.droid.shortvideo.demo.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.media.AudioFormat;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.qiniu.pili.droid.shortvideo.PLAudioEncodeSetting;
import com.qiniu.pili.droid.shortvideo.PLAudioMixMode;
import com.qiniu.pili.droid.shortvideo.PLCameraSetting;
import com.qiniu.pili.droid.shortvideo.PLDisplayMode;
import com.qiniu.pili.droid.shortvideo.PLFaceBeautySetting;
import com.qiniu.pili.droid.shortvideo.PLFocusListener;
import com.qiniu.pili.droid.shortvideo.PLMediaFile;
import com.qiniu.pili.droid.shortvideo.PLMicrophoneSetting;
import com.qiniu.pili.droid.shortvideo.PLRecordSetting;
import com.qiniu.pili.droid.shortvideo.PLRecordStateListener;
import com.qiniu.pili.droid.shortvideo.PLShortVideoMixRecorder;
import com.qiniu.pili.droid.shortvideo.PLVideoEncodeSetting;
import com.qiniu.pili.droid.shortvideo.PLVideoMixSetting;
import com.qiniu.pili.droid.shortvideo.PLVideoSaveListener;
import com.qiniu.pili.droid.shortvideo.demo.R;
import com.qiniu.pili.droid.shortvideo.demo.utils.Config;
import com.qiniu.pili.droid.shortvideo.demo.utils.GetPathFromUri;
import com.qiniu.pili.droid.shortvideo.demo.utils.RecordSettings;
import com.qiniu.pili.droid.shortvideo.demo.utils.ToastUtils;
import com.qiniu.pili.droid.shortvideo.demo.view.CustomProgressDialog;
import com.qiniu.pili.droid.shortvideo.demo.view.FocusIndicator;
import com.qiniu.pili.droid.shortvideo.demo.view.SectionProgressBar;
import com.qiniu.pili.droid.shortvideo.demo.view.SquareGLSurfaceView;
import com.qiniu.pili.droid.shortvideo.demo.view.VideoMixGLSurfaceView;

import java.util.Stack;

import static com.qiniu.pili.droid.shortvideo.demo.utils.Config.CAMERA_RECORD_CACHE_PATH;
import static com.qiniu.pili.droid.shortvideo.demo.utils.RecordSettings.chooseCameraFacingId;

public class VideoMixRecordActivity extends Activity implements PLRecordStateListener, PLVideoSaveListener, PLFocusListener {
    private static final String TAG = "VideoMixRecordActivity";

    public static final String MIX_MODE = "mixMode";
    public static final int MIX_MODE_VERTICAL = 1;
    public static final int MIX_MODE_SAMPLE_ABOVE_CAMERA = 2;
    public static final int MIX_MODE_CAMERA_ABOVE_SAMPLE = 3;

    private static final int VIDEO_MIX_ENCODE_WIDTH = 720;
    private static final int VIDEO_MIX_ENCODE_HEIGHT = 720;

    private PLShortVideoMixRecorder mMixRecorder;

    private SectionProgressBar mSectionProgressBar;
    private CustomProgressDialog mProcessingDialog;
    private View mRecordBtn;
    private View mDeleteBtn;
    private View mConcatBtn;
    private View mSwitchCameraBtn;
    private View mSwitchFlashBtn;
    private FocusIndicator mFocusIndicator;
    private SeekBar mAdjustBrightnessSeekBar;
    private GLSurfaceView mCameraPreview;
    private GLSurfaceView mSamplePreview;
    private ViewGroup mPreviewParent;
    private CheckBox mMuteMicrophoneCheck;
    private CheckBox mMuteSampleCheck;
    private CheckBox mEarphoneModeCheck;

    private TextView mRecordingPercentageView;
    private long mLastRecordingPercentageViewUpdateTime = 0;

    private boolean mFlashEnabled;
    private boolean mIsEditVideo = false;

    private GestureDetector mGestureDetector;

    private PLCameraSetting mCameraSetting;
    private PLMicrophoneSetting mMicrophoneSetting;
    private PLRecordSetting mRecordSetting;
    private PLVideoEncodeSetting mVideoEncodeSetting;
    private PLAudioEncodeSetting mAudioEncodeSetting;
    private PLFaceBeautySetting mFaceBeautySetting;

    private int mFocusIndicatorX;
    private int mFocusIndicatorY;
    private int mSampleVideoWidth;
    private int mSampleVideoHeight;

    private Stack<Long> mDurationRecordStack = new Stack();
    private Stack<Double> mDurationVideoStack = new Stack();

    private boolean mSectionBegan;
    private int mMixMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_mix_record);

        mSectionProgressBar = (SectionProgressBar) findViewById(R.id.record_progressbar);
        mRecordBtn = findViewById(R.id.record);
        mDeleteBtn = findViewById(R.id.delete);
        mConcatBtn = findViewById(R.id.concat);
        mSwitchCameraBtn = findViewById(R.id.switch_camera);
        mSwitchFlashBtn = findViewById(R.id.switch_flash);
        mFocusIndicator = (FocusIndicator) findViewById(R.id.focus_indicator);
        mAdjustBrightnessSeekBar = (SeekBar) findViewById(R.id.adjust_brightness);
        mRecordingPercentageView = (TextView) findViewById(R.id.recording_percentage);
        mEarphoneModeCheck = (CheckBox) findViewById(R.id.earphone_mode);
        mEarphoneModeCheck.setOnCheckedChangeListener(audioCheckedListener);
        mMuteSampleCheck = (CheckBox) findViewById(R.id.mute_sample);
        mMuteSampleCheck.setOnCheckedChangeListener(audioCheckedListener);
        mMuteMicrophoneCheck = (CheckBox) findViewById(R.id.mute_microphone);
        mMuteMicrophoneCheck.setOnCheckedChangeListener(audioCheckedListener);

        mProcessingDialog = new CustomProgressDialog(this);
        mProcessingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mMixRecorder.cancel();
            }
        });

        mSectionProgressBar.setFirstPointTime(RecordSettings.DEFAULT_MIN_RECORD_DURATION);
        onSectionCountChanged(0, 0);

        mRecordBtn.setOnTouchListener(new View.OnTouchListener() {
            private long mSectionBeginTSMs;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    if (!mSectionBegan && mMixRecorder.beginSection()) {
                        mSectionBegan = true;
                        mSectionBeginTSMs = System.currentTimeMillis();
                        mSectionProgressBar.setCurrentState(SectionProgressBar.State.START);
                        updateRecordingBtns(true);
                    } else {
                        ToastUtils.s(VideoMixRecordActivity.this, "无法开始视频段录制");
                    }
                } else if (action == MotionEvent.ACTION_UP) {
                    if (mSectionBegan) {
                        long sectionRecordDurationMs = System.currentTimeMillis() - mSectionBeginTSMs;
                        long totalRecordDurationMs = sectionRecordDurationMs + (mDurationRecordStack.isEmpty() ? 0 : mDurationRecordStack.peek().longValue());
                        double sectionVideoDurationMs = sectionRecordDurationMs;
                        double totalVideoDurationMs = sectionVideoDurationMs + (mDurationVideoStack.isEmpty() ? 0 : mDurationVideoStack.peek().doubleValue());
                        mDurationRecordStack.push(new Long(totalRecordDurationMs));
                        mDurationVideoStack.push(new Double(totalVideoDurationMs));
                        if (mRecordSetting.IsRecordSpeedVariable()) {
                            Log.d(TAG, "SectionRecordDuration: " + sectionRecordDurationMs + "; sectionVideoDuration: " + sectionVideoDurationMs + "; totalVideoDurationMs: " + totalVideoDurationMs + "Section count: " + mDurationVideoStack.size());
                            mSectionProgressBar.addBreakPointTime((long) totalVideoDurationMs);
                        } else {
                            mSectionProgressBar.addBreakPointTime(totalRecordDurationMs);
                        }

                        mSectionProgressBar.setCurrentState(SectionProgressBar.State.PAUSE);
                        mMixRecorder.endSection();
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
                mMixRecorder.manualFocus(mFocusIndicator.getWidth(), mFocusIndicator.getHeight(), (int) e.getX(), (int) e.getY());
                return false;
            }
        });

        mMixMode = getIntent().getIntExtra(MIX_MODE, MIX_MODE_VERTICAL);

        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT < 19) {
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("video/*");
        } else {
            intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("video/*");
        }
        startActivityForResult(Intent.createChooser(intent, "选择要导入的视频"), 0);
    }

    private void configPreview() {
        switch (mMixMode) {
            case MIX_MODE_CAMERA_ABOVE_SAMPLE:
                configCameraAboveSampleMixPreview();
                break;
            case MIX_MODE_SAMPLE_ABOVE_CAMERA:
                configSampleAboveCameraMixPreview();
                break;
            case MIX_MODE_VERTICAL:
                configVerticalMixPreview();
                break;
        }
    }

    private void configCameraAboveSampleMixPreview() {
        mPreviewParent = (ViewGroup) findViewById(R.id.framePreviewParent);
        mSamplePreview = new SquareGLSurfaceView(this, null);
        mPreviewParent.addView(mSamplePreview);
        mCameraPreview = new SquareGLSurfaceView(this, null);
        Display display = getWindowManager().getDefaultDisplay(); // 为获取屏幕宽、高
        mCameraPreview.setLayoutParams(new FrameLayout.LayoutParams(display.getWidth() / 2, display.getWidth() / 2));
        mPreviewParent.addView(mCameraPreview);
        mCameraPreview.setZOrderMediaOverlay(true);
    }

    private void configSampleAboveCameraMixPreview() {
        mPreviewParent = (ViewGroup) findViewById(R.id.framePreviewParent);
        mCameraPreview = new SquareGLSurfaceView(this, null);
        mPreviewParent.addView(mCameraPreview);
        mSamplePreview = new SquareGLSurfaceView(this, null);
        Display display = getWindowManager().getDefaultDisplay(); // 为获取屏幕宽、高
        mSamplePreview.setLayoutParams(new FrameLayout.LayoutParams(display.getWidth() / 2, display.getWidth() / 2));
        mPreviewParent.addView(mSamplePreview);
        mSamplePreview.setZOrderMediaOverlay(true);
    }

    private void configVerticalMixPreview() {
        mPreviewParent = (ViewGroup) findViewById(R.id.linearPreviewParent);
        mCameraPreview = new VideoMixGLSurfaceView(this, null);
        mCameraPreview.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        mPreviewParent.addView(mCameraPreview);
        mSamplePreview = new VideoMixGLSurfaceView(this, null);
        mSamplePreview.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        mPreviewParent.addView(mSamplePreview);
    }

    private PLVideoMixSetting getVerticalMixSetting(String sampleVideoPath) {
        float sampleRatio = (float) mSampleVideoWidth / mSampleVideoHeight;
        //固定宽度
        int sampleRectWidth = VIDEO_MIX_ENCODE_WIDTH / 2;
        //高度根据长宽比来计算，防止拉伸
        int sampleRectHeight = (int) (sampleRectWidth / sampleRatio);
        //顶部坐标
        int sampleRectTop = (VIDEO_MIX_ENCODE_HEIGHT - sampleRectHeight) / 2;
        //底部坐标
        int sampleRectBottom = sampleRectTop + sampleRectHeight;

        float cameraRatio = (float) PLCameraSetting.calcCameraPreviewSizeRatio(mCameraSetting.getCameraPreviewSizeRatio());
        //固定宽度
        int cameraRectWidth = VIDEO_MIX_ENCODE_WIDTH / 2;
        //高度根据长宽比来计算，防止拉伸
        int cameraRectHeight = (int) (cameraRectWidth * cameraRatio);
        //顶部坐标
        int cameraRectTop = (VIDEO_MIX_ENCODE_HEIGHT - cameraRectHeight) / 2;
        //底部坐标
        int cameraRectBottom = cameraRectTop + cameraRectHeight;

        PLDisplayMode sampleDisplayMode = PLDisplayMode.FULL;
        if (mMixMode == MIX_MODE_VERTICAL && mSampleVideoWidth < mSampleVideoHeight) {
            sampleDisplayMode = PLDisplayMode.FIT;
        }

        //摄像机录制的视频在最后画面的矩形区域
        Rect cameraVideoRect = new Rect(0, cameraRectTop, VIDEO_MIX_ENCODE_WIDTH / 2, cameraRectBottom);
        //样本视频在最后画面的矩形区域
        Rect sampleVideoRect = new Rect(VIDEO_MIX_ENCODE_WIDTH / 2, sampleRectTop, VIDEO_MIX_ENCODE_WIDTH, sampleRectBottom);
        if (sampleDisplayMode == PLDisplayMode.FULL) {
            //如果样本视频的宽 > 高，那么选用 PLDisplayMode.FULL 模式可以居中剪裁，使之完全填充区域
            //此时，只需要把样本矩形区域尺寸和 camera 一致即可，无需再单独计算
            sampleVideoRect = new Rect(VIDEO_MIX_ENCODE_WIDTH / 2, cameraRectTop, VIDEO_MIX_ENCODE_WIDTH, cameraRectBottom);
        }

        //其中：CAMERA_RECORD_CACHE_PATH 为摄像机录制之后的片段最后拼接的缓存视频的路径，它用于和样本视频最后进行合成
        PLVideoMixSetting mixSetting = new PLVideoMixSetting(cameraVideoRect, sampleVideoRect, sampleVideoPath, sampleDisplayMode, CAMERA_RECORD_CACHE_PATH);
        return mixSetting;
    }

    private PLVideoMixSetting getSampleAboveCameraMixSetting(String sampleVideoPath) {
        Rect cameraVideoRect = new Rect(0, 0, VIDEO_MIX_ENCODE_WIDTH, VIDEO_MIX_ENCODE_HEIGHT);
        Rect sampleVideoRect = new Rect(0, 0, VIDEO_MIX_ENCODE_WIDTH / 2, VIDEO_MIX_ENCODE_WIDTH / 2);
        PLDisplayMode sampleDisplayMode = PLDisplayMode.FULL;
        PLVideoMixSetting mixSetting = new PLVideoMixSetting(cameraVideoRect, sampleVideoRect, sampleVideoPath, sampleDisplayMode, false, CAMERA_RECORD_CACHE_PATH);
        return mixSetting;
    }

    private PLVideoMixSetting getCameraAboveSampleMixSetting(String sampleVideoPath) {
        Rect sampleVideoRect = new Rect(0, 0, VIDEO_MIX_ENCODE_WIDTH, VIDEO_MIX_ENCODE_HEIGHT);
        Rect cameraVideoRect = new Rect(0, 0, VIDEO_MIX_ENCODE_WIDTH / 2, VIDEO_MIX_ENCODE_WIDTH / 2);
        PLDisplayMode sampleDisplayMode = PLDisplayMode.FULL;
        PLVideoMixSetting mixSetting = new PLVideoMixSetting(cameraVideoRect, sampleVideoRect, sampleVideoPath, sampleDisplayMode, true, CAMERA_RECORD_CACHE_PATH);
        return mixSetting;
    }

    private void init(String sampleVideoPath) {
        configPreview();

        mMixRecorder = new PLShortVideoMixRecorder(this);
        mMixRecorder.setRecordStateListener(this);
        mMixRecorder.setFocusListener(this);

        mCameraSetting = new PLCameraSetting();
        PLCameraSetting.CAMERA_FACING_ID facingId = chooseCameraFacingId();
        mCameraSetting.setCameraId(facingId);
        mCameraSetting.setCameraPreviewSizeRatio(PLCameraSetting.CAMERA_PREVIEW_SIZE_RATIO.RATIO_16_9);
        mCameraSetting.setCameraPreviewSizeLevel(PLCameraSetting.CAMERA_PREVIEW_SIZE_LEVEL.PREVIEW_SIZE_LEVEL_720P);

        mMicrophoneSetting = new PLMicrophoneSetting();
        mMicrophoneSetting.setChannelConfig(AudioFormat.CHANNEL_IN_MONO);


        mVideoEncodeSetting = new PLVideoEncodeSetting(this);
        mVideoEncodeSetting.setPreferredEncodingSize(VIDEO_MIX_ENCODE_WIDTH, VIDEO_MIX_ENCODE_HEIGHT);
        mVideoEncodeSetting.setEncodingBitrate(2000 * 1000);
        mVideoEncodeSetting.setEncodingFps(30);

        mAudioEncodeSetting = new PLAudioEncodeSetting();
        mAudioEncodeSetting.setChannels(1);

        PLMediaFile mediaFile = new PLMediaFile(sampleVideoPath);
        long maxDuration = mediaFile.getDurationMs();

        float sampleRotation = mediaFile.getVideoRotation();
        mSampleVideoWidth = (sampleRotation == 90 || sampleRotation == 270) ? mediaFile.getVideoHeight() : mediaFile.getVideoWidth();
        mSampleVideoHeight = (sampleRotation == 90 || sampleRotation == 270) ? mediaFile.getVideoWidth() : mediaFile.getVideoHeight();
        mediaFile.release();
        mRecordSetting = new PLRecordSetting();
        mRecordSetting.setMaxRecordDuration(maxDuration);
        mRecordSetting.setVideoCacheDir(Config.VIDEO_STORAGE_DIR);
        mRecordSetting.setVideoFilepath(Config.MIX_RECORD_FILE_PATH);
        mRecordSetting.setRecordSpeedVariable(true);

        PLDisplayMode cameraDisplayMode = (mMixMode == MIX_MODE_VERTICAL ? PLDisplayMode.FIT : PLDisplayMode.FULL);
        mRecordSetting.setDisplayMode(cameraDisplayMode);

        mFaceBeautySetting = new PLFaceBeautySetting(1.0f, 0.5f, 0.5f);

        PLVideoMixSetting mixSetting;
        if (mMixMode == MIX_MODE_CAMERA_ABOVE_SAMPLE) {
            mixSetting = getCameraAboveSampleMixSetting(sampleVideoPath);
        } else if (mMixMode == MIX_MODE_SAMPLE_ABOVE_CAMERA) {
            mixSetting = getSampleAboveCameraMixSetting(sampleVideoPath);
        } else {
            mixSetting = getVerticalMixSetting(sampleVideoPath);
        }
        mMixRecorder.prepare(mCameraPreview, mSamplePreview, mixSetting, mCameraSetting, mMicrophoneSetting, mVideoEncodeSetting,
                mAudioEncodeSetting, mFaceBeautySetting, mRecordSetting);

        mMuteSampleCheck.setChecked(false);
        mMuteMicrophoneCheck.setChecked(true);

        mMixRecorder.setAudioMixMode(PLAudioMixMode.EARPHONE_MODE);

        mSectionProgressBar.setTotalTime(this, mRecordSetting.getMaxRecordDuration());
        mCameraPreview.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mGestureDetector.onTouchEvent(motionEvent);
                return true;
            }
        });
    }

    private void updateRecordingBtns(boolean isRecording) {
        mSwitchCameraBtn.setEnabled(!isRecording);
        mRecordBtn.setActivated(isRecording);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRecordBtn.setEnabled(false);
        if (mMixRecorder != null) {
            mMixRecorder.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateRecordingBtns(false);
        if (mMixRecorder != null) {
            mMixRecorder.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMixRecorder != null) {
            mMixRecorder.destroy();
        }
    }

    public void onClickDelete(View v) {
        if (!mMixRecorder.deleteLastSection()) {
            ToastUtils.s(this, "回删视频段失败");
        }
    }

    public void onClickConcat(View v) {
        mProcessingDialog.show();
        showChooseDialog();
    }

    public void onClickBrightness(View v) {
        boolean isVisible = mAdjustBrightnessSeekBar.getVisibility() == View.VISIBLE;
        mAdjustBrightnessSeekBar.setVisibility(isVisible ? View.GONE : View.VISIBLE);
    }

    public void onClickSwitchCamera(View v) {
        mMixRecorder.switchCamera();
        mFocusIndicator.focusCancel();
    }

    public void onClickSwitchFlash(View v) {
        mFlashEnabled = !mFlashEnabled;
        mMixRecorder.setFlashEnabled(mFlashEnabled);
        mSwitchFlashBtn.setActivated(mFlashEnabled);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            String selectedFilepath = GetPathFromUri.getPath(this, data.getData());
            Log.i(TAG, "Select file: " + selectedFilepath);
            if (selectedFilepath != null && !"".equals(selectedFilepath)) {
                init(selectedFilepath);
            }
        } else {
            finish();
        }
    }

    @Override
    public void onReady() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSwitchFlashBtn.setVisibility(mMixRecorder.isFlashSupport() ? View.VISIBLE : View.GONE);
                mFlashEnabled = false;
                mSwitchFlashBtn.setActivated(mFlashEnabled);
                mRecordBtn.setEnabled(true);
                refreshSeekBar();
                ToastUtils.s(VideoMixRecordActivity.this, "可以开始拍摄咯");
            }
        });
    }

    @Override
    public void onError(final int code) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtils.toastErrorCode(VideoMixRecordActivity.this, code);
            }
        });
    }

    @Override
    public void onDurationTooShort() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtils.s(VideoMixRecordActivity.this, "该视频段太短了");
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
        if ((videoSectionDuration + incDuration) >= mRecordSetting.getMaxRecordDuration()) {
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
                ToastUtils.s(VideoMixRecordActivity.this, "已达到拍摄总时长");
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
                ToastUtils.s(VideoMixRecordActivity.this, "拼接视频段失败: " + errorCode);
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
                if (mIsEditVideo) {
                    VideoEditActivity.start(VideoMixRecordActivity.this, filePath, screenOrientation);
                } else {
                    PlaybackActivity.start(VideoMixRecordActivity.this, filePath, screenOrientation);
                }
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
        final int max = mMixRecorder.getMaxExposureCompensation();
        final int min = mMixRecorder.getMinExposureCompensation();
        boolean brightnessAdjustAvailable = (max != 0 || min != 0);
        Log.e(TAG, "max/min exposure compensation: " + max + "/" + min + " brightness adjust available: " + brightnessAdjustAvailable);

        findViewById(R.id.brightness_panel).setVisibility(brightnessAdjustAvailable ? View.VISIBLE : View.GONE);
        mAdjustBrightnessSeekBar.setOnSeekBarChangeListener(!brightnessAdjustAvailable ? null : new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (i <= Math.abs(min)) {
                    mMixRecorder.setExposureCompensation(i + min);
                } else {
                    mMixRecorder.setExposureCompensation(i - max);
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
                updateCheckBoxClickable(count <= 0);
                boolean isConcatBtnEnabled = (totalTime >= RecordSettings.DEFAULT_MIN_RECORD_DURATION)
                        || (mRecordSetting != null && totalTime >= mRecordSetting.getMaxRecordDuration());
                mConcatBtn.setEnabled(isConcatBtnEnabled);
            }
        });
    }

    private void showChooseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.if_edit_video));
        builder.setPositiveButton(getString(R.string.dlg_yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mIsEditVideo = true;
                mMixRecorder.save(VideoMixRecordActivity.this);
            }
        });
        builder.setNegativeButton(getString(R.string.dlg_no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mIsEditVideo = false;
                mMixRecorder.save(VideoMixRecordActivity.this);
            }
        });
        builder.setCancelable(false);
        builder.create().show();
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

    private void updateCheckBoxVisible() {
        if (!mMuteMicrophoneCheck.isChecked() && !mMuteSampleCheck.isChecked()) {
            mEarphoneModeCheck.setVisibility(View.VISIBLE);
        } else {
            mEarphoneModeCheck.setVisibility(View.GONE);
        }
    }

    private void updateCheckBoxClickable(boolean clickable) {
        mEarphoneModeCheck.setClickable(clickable);
        mMuteMicrophoneCheck.setClickable(clickable);
        mMuteSampleCheck.setClickable(clickable);
    }

    private CompoundButton.OnCheckedChangeListener audioCheckedListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            switch (buttonView.getId()) {
                case R.id.mute_microphone:
                    mMixRecorder.muteMicrophone(isChecked);
                    updateCheckBoxVisible();
                    break;
                case R.id.mute_sample:
                    mMixRecorder.muteSampleVideo(isChecked);
                    updateCheckBoxVisible();
                    break;
                case R.id.earphone_mode:
                    mMixRecorder.setAudioMixMode(isChecked ? PLAudioMixMode.EARPHONE_MODE : PLAudioMixMode.SPEAKERPHONE_MODE);
            }
        }
    };
}
