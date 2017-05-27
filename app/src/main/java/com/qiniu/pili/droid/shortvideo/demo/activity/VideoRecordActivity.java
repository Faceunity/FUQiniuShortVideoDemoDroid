package com.qiniu.pili.droid.shortvideo.demo.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.faceunity.wrapper.faceunity;
import com.qiniu.pili.droid.shortvideo.PLAudioEncodeSetting;
import com.qiniu.pili.droid.shortvideo.PLCameraPreviewListener;
import com.qiniu.pili.droid.shortvideo.PLCameraSetting;
import com.qiniu.pili.droid.shortvideo.PLConcatStateListener;
import com.qiniu.pili.droid.shortvideo.PLFaceBeautySetting;
import com.qiniu.pili.droid.shortvideo.PLMicrophoneSetting;
import com.qiniu.pili.droid.shortvideo.PLRecordSetting;
import com.qiniu.pili.droid.shortvideo.PLRecordStateListener;
import com.qiniu.pili.droid.shortvideo.PLShortVideoRecorder;
import com.qiniu.pili.droid.shortvideo.PLVideoEncodeSetting;
import com.qiniu.pili.droid.shortvideo.PLVideoFilterListener;
import com.qiniu.pili.droid.shortvideo.demo.R;
import com.qiniu.pili.droid.shortvideo.demo.faceunity.EffectAndFilterSelectAdapter;
import com.qiniu.pili.droid.shortvideo.demo.faceunity.VideoRect;
import com.qiniu.pili.droid.shortvideo.demo.faceunity.authpack;
import com.qiniu.pili.droid.shortvideo.demo.utils.RecordSettings;
import com.qiniu.pili.droid.shortvideo.demo.utils.ToastUtils;
import com.qiniu.pili.droid.shortvideo.demo.view.SectionProgressBar;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;


public class VideoRecordActivity extends Activity implements View.OnClickListener, PLRecordStateListener, PLConcatStateListener, PLVideoFilterListener, PLCameraPreviewListener {
    private static final String TAG = "VideoRecordActivity";

    private PLShortVideoRecorder mShortVideoRecorder;
    private GLSurfaceView preview;

    private SectionProgressBar mSectionProgressBar;
    private ProgressDialog mProgressDialog;
    private View mRecordBtn;
    private View mDeleteBtn;
    private View mConcatBtn;
    private View mSwitchCameraBtn;
    private View mSwitchFlashBtn;

    private boolean mFlashEnabled;

    private RecyclerView mEffectRecyclerView;
    private EffectAndFilterSelectAdapter mEffectRecyclerAdapter;
    private RecyclerView mFilterRecyclerView;
    private EffectAndFilterSelectAdapter mFilterRecyclerAdapter;

    private LinearLayout mBlurLevelSelect;
    private LinearLayout mColorLevelSelect;
    private LinearLayout mFaceShapeSelect;
    private LinearLayout mRedLevelSelect;


    private Button mChooseEffectBtn;
    private Button mChooseFilterBtn;
    private Button mChooseBlurLevelBtn;
    private Button mChooseColorLevelBtn;
    private Button mChooseFaceShapeBtn;
    private Button mChooseRedLevelBtn;

    private int cameraFaing = Camera.CameraInfo.CAMERA_FACING_FRONT;

    private TextView[] mBlurLevels;
    private int[] BLUR_LEVEL_TV_ID = {R.id.blur_level0, R.id.blur_level1, R.id.blur_level2,
            R.id.blur_level3, R.id.blur_level4, R.id.blur_level5, R.id.blur_level6};

    private TextView mFaceShape0Nvshen;
    private TextView mFaceShape1Wanghong;
    private TextView mFaceShape2Ziran;
    private TextView mFaceShape3Default;

    protected ImageView mFaceTrackingStatusImageView;

    private int mFacebeautyItem = 0;
    private int mEffectItem = 0;

    private int mFrameId;

    private float mFacebeautyColorLevel = 0.2f;
    private float mFacebeautyBlurLevel = 6.0f;
    private float mFacebeautyCheeckThin = 1.0f;
    private float mFacebeautyEnlargeEye = 0.5f;
    private float mFacebeautyRedLevel = 0.5f;
    private int mFaceShape = 3;
    private float mFaceShapeLevel = 0.5f;

    private String mFilterName = EffectAndFilterSelectAdapter.FILTERS_NAME[0];

    private boolean isNeedEffectItem = true;
    private String mEffectFileName = EffectAndFilterSelectAdapter.EFFECT_ITEM_FILE_NAME[1];

    private HandlerThread mCreateItemThread;
    private Handler mCreateItemHandler;


    private int faceTrackingStatus = 0;
    private byte[] mCameraNV21Byte;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_record);

        mSectionProgressBar = (SectionProgressBar) findViewById(R.id.record_progressbar);
        preview = (GLSurfaceView) findViewById(R.id.preview);
        mRecordBtn = findViewById(R.id.record);
        mDeleteBtn = findViewById(R.id.delete);
        mConcatBtn = findViewById(R.id.concat);
        mSwitchCameraBtn = findViewById(R.id.switch_camera);
        mSwitchFlashBtn = findViewById(R.id.switch_flash);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Processing...");
        mProgressDialog.setCancelable(false);

        mShortVideoRecorder = new PLShortVideoRecorder();
        mShortVideoRecorder.setRecordStateListener(this);

        int previewSizeRatio = getIntent().getIntExtra("PreviewSizeRatio", 0);
        int previewSizeLevel = getIntent().getIntExtra("PreviewSizeLevel", 0);

        int encodingSizeLevel = getIntent().getIntExtra("EncodingSizeLevel", 0);

        PLCameraSetting cameraSetting = new PLCameraSetting();
        cameraSetting.setCameraId(PLCameraSetting.CAMERA_FACING_ID.CAMERA_FACING_FRONT);
        cameraSetting.setCameraPreviewSizeRatio(getPreviewSizeRatio(previewSizeRatio));
        cameraSetting.setCameraPreviewSizeLevel(getPreviewSizeLevel(previewSizeLevel));

        PLMicrophoneSetting microphoneSetting = new PLMicrophoneSetting();

        PLVideoEncodeSetting videoEncodeSetting = new PLVideoEncodeSetting();
        videoEncodeSetting.setEncodingSizeLevel(getEncodingSizeLevel(encodingSizeLevel));

        PLAudioEncodeSetting audioEncodeSetting = new PLAudioEncodeSetting();

        PLFaceBeautySetting faceBeautySetting = new PLFaceBeautySetting(0.0f, 0.0f, 0.0f);
        faceBeautySetting.setEnable(true);

        PLRecordSetting recordSetting = new PLRecordSetting();
        recordSetting.setMaxRecordDuration(RecordSettings.DEFAULT_MAX_RECORD_DURATION);

        mShortVideoRecorder.prepare(preview, cameraSetting, microphoneSetting,
                videoEncodeSetting, audioEncodeSetting, faceBeautySetting, recordSetting);
        mShortVideoRecorder.setVideoFilterListener(this);
        mShortVideoRecorder.setCameraPreviewListener(this);

        mSectionProgressBar.setFirstPointTime(RecordSettings.DEFAULT_MIN_RECORD_DURATION);
        mSectionProgressBar.setTotalTime(RecordSettings.DEFAULT_MAX_RECORD_DURATION);

        mRecordBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    mSwitchCameraBtn.setEnabled(false);
                    v.setActivated(true);
                    mShortVideoRecorder.beginSection();
                    mSectionProgressBar.setCurrentState(SectionProgressBar.State.START);
                } else if (action == MotionEvent.ACTION_UP) {
                    mSwitchCameraBtn.setEnabled(true);
                    v.setActivated(false);
                    mShortVideoRecorder.endSection();
                    mSectionProgressBar.setCurrentState(SectionProgressBar.State.PAUSE);
                }

                return false;
            }
        });
        onSectionCountChanged(0, 0);

        initFaceunityBottomView();

        mCreateItemThread = new HandlerThread("CreateItemThread");
        mCreateItemThread.start();
        mCreateItemHandler = new CreateItemHandler(mCreateItemThread.getLooper(), this);
    }

    private void initFaceunityBottomView() {

        mEffectRecyclerView = (RecyclerView) findViewById(R.id.effect_recycle_view);
        mEffectRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mEffectRecyclerAdapter = new EffectAndFilterSelectAdapter(mEffectRecyclerView, EffectAndFilterSelectAdapter.RECYCLEVIEW_TYPE_EFFECT);
        mEffectRecyclerAdapter.setOnItemSelectedListener(new EffectAndFilterSelectAdapter.OnItemSelectedListener() {
            @Override
            public void onItemSelected(int itemPosition) {
                Log.d(TAG, "effect item selected " + itemPosition);
                onEffectItemSelected(EffectAndFilterSelectAdapter.EFFECT_ITEM_FILE_NAME[itemPosition]);
            }
        });
        mEffectRecyclerView.setAdapter(mEffectRecyclerAdapter);

        mFilterRecyclerView = (RecyclerView) findViewById(R.id.filter_recycle_view);
        mFilterRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mFilterRecyclerAdapter = new EffectAndFilterSelectAdapter(mFilterRecyclerView, EffectAndFilterSelectAdapter.RECYCLEVIEW_TYPE_FILTER);
        mFilterRecyclerAdapter.setOnItemSelectedListener(new EffectAndFilterSelectAdapter.OnItemSelectedListener() {
            @Override
            public void onItemSelected(int itemPosition) {
                Log.d(TAG, "filter item selected " + itemPosition);
                onFilterSelected(EffectAndFilterSelectAdapter.FILTERS_NAME[itemPosition]);
            }
        });
        mFilterRecyclerView.setAdapter(mFilterRecyclerAdapter);

        mChooseEffectBtn = (Button) findViewById(R.id.btn_choose_effect);
        mChooseFilterBtn = (Button) findViewById(R.id.btn_choose_filter);
        mChooseBlurLevelBtn = (Button) findViewById(R.id.btn_choose_blur_level);
        mChooseColorLevelBtn = (Button) findViewById(R.id.btn_choose_color_level);
        mChooseFaceShapeBtn = (Button) findViewById(R.id.btn_choose_face_shape);
        mChooseRedLevelBtn = (Button) findViewById(R.id.btn_choose_red_level);

        mFaceShape0Nvshen = (TextView) findViewById(R.id.face_shape_0_nvshen);
        mFaceShape1Wanghong = (TextView) findViewById(R.id.face_shape_1_wanghong);
        mFaceShape2Ziran = (TextView) findViewById(R.id.face_shape_2_ziran);
        mFaceShape3Default = (TextView) findViewById(R.id.face_shape_3_default);

        mBlurLevelSelect = (LinearLayout) findViewById(R.id.blur_level_select_block);
        mColorLevelSelect = (LinearLayout) findViewById(R.id.color_level_select_block);
        mFaceShapeSelect = (LinearLayout) findViewById(R.id.lin_face_shape);
        mRedLevelSelect = (LinearLayout) findViewById(R.id.red_level_select_block);

        mBlurLevels = new TextView[BLUR_LEVEL_TV_ID.length];
        for (int i = 0; i < BLUR_LEVEL_TV_ID.length; i++) {
            final int level = i;
            mBlurLevels[i] = (TextView) findViewById(BLUR_LEVEL_TV_ID[i]);
            mBlurLevels[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setBlurLevelTextBackground(mBlurLevels[level]);
                    onBlurLevelSelected(level);
                }
            });
        }

        DiscreteSeekBar colorLevelSeekbar = (DiscreteSeekBar) findViewById(R.id.color_level_seekbar);
        colorLevelSeekbar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                onColorLevelSelected(value, 100);
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {

            }
        });

        DiscreteSeekBar cheekThinSeekbar = (DiscreteSeekBar) findViewById(R.id.cheekthin_level_seekbar);
        cheekThinSeekbar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                onCheekThinSelected(value, 100);
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {

            }
        });

        DiscreteSeekBar enlargeEyeSeekbar = (DiscreteSeekBar) findViewById(R.id.enlarge_eye_level_seekbar);
        enlargeEyeSeekbar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                onEnlargeEyeSelected(value, 100);
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {

            }
        });

        DiscreteSeekBar faceShapeLevelSeekbar = (DiscreteSeekBar) findViewById(R.id.face_shape_seekbar);
        faceShapeLevelSeekbar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                onFaceShapeLevelSelected(value, 100);
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {

            }
        });

        DiscreteSeekBar redLevelShapeLevelSeekbar = (DiscreteSeekBar) findViewById(R.id.red_level_seekbar);
        redLevelShapeLevelSeekbar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                onRedLevelSelected(value, 100);
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {

            }
        });

        mFaceTrackingStatusImageView = (ImageView) findViewById(R.id.iv_face_detect);
    }

    @Override
    protected void onStart() {
        super.onStart();

        try {
            //加载读取人脸数据 v3.mp3 文件
            InputStream is = getAssets().open("v3.mp3");
            byte[] v3data = new byte[is.available()];
            is.read(v3data);
            is.close();
            faceunity.fuSetup(v3data, null, authpack.A());
            //faceunity.fuSetMaxFaces(1);
            Log.e(TAG, "fuSetup");

            //加载美颜 face_beautification.mp3 文件
            is = getAssets().open("face_beautification.mp3");
            byte[] itemData = new byte[is.available()];
            is.read(itemData);
            is.close();
            mFacebeautyItem = faceunity.fuCreateItemFromPackage(itemData);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        mShortVideoRecorder.pause();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mFrameId = 0;

        mCreateItemHandler.removeMessages(CreateItemHandler.HANDLE_CREATE_ITEM);

        preview.queueEvent(new Runnable() {
            @Override
            public void run() {
                //Note: 切忌使用一个已经destroy的item
                //faceunity.fuDestroyAllItems();
                faceunity.fuDestroyItem(mEffectItem);
                mEffectItem = 0;
                faceunity.fuDestroyItem(mFacebeautyItem);
                mFacebeautyItem = 0;
                faceunity.fuOnDeviceLost();
                isNeedEffectItem = true;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mShortVideoRecorder.destroy();
    }

    public void onClickDelete(View v) {
        if (!mShortVideoRecorder.deleteLastSection()) {
            ToastUtils.s(this, "delete last section failed!");
        }
    }

    public void onClickConcat(View v) {
        mProgressDialog.show();
        mShortVideoRecorder.concatSections(this);
    }

    public void onClickSwitchCamera(View v) {
        mShortVideoRecorder.switchCamera();
        if (cameraFaing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            cameraFaing = Camera.CameraInfo.CAMERA_FACING_BACK;
        } else {
            cameraFaing = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }
        mFrameId = 0;

        preview.queueEvent(new Runnable() {
            @Override
            public void run() {
                faceunity.fuOnCameraChange();
                faceunity.fuOnDeviceLost();
            }
        });
    }

    public void onClickSwitchFlash(View v) {
        mFlashEnabled = !mFlashEnabled;
        mShortVideoRecorder.setFlashEnabled(mFlashEnabled);
        mSwitchFlashBtn.setActivated(mFlashEnabled);
    }

    @Override
    public void onReady() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSwitchFlashBtn.setVisibility(mShortVideoRecorder.isFlashSupport() ? View.VISIBLE : View.GONE);
                mFlashEnabled = false;
                mSwitchFlashBtn.setActivated(mFlashEnabled);
                mRecordBtn.setEnabled(true);
                ToastUtils.s(VideoRecordActivity.this, "ready to record now");
            }
        });
    }

    @Override
    public void onDurationTooShort() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtils.s(VideoRecordActivity.this, "video too short");
            }
        });
    }

    @Override
    public void onSectionIncreased(long incDuration, long totalDuration, int sectionCount) {
        onSectionCountChanged(sectionCount, totalDuration);
        mSectionProgressBar.addBreakPointTime(totalDuration);
    }

    @Override
    public void onSectionDecreased(long decDuration, long totalDuration, int sectionCount) {
        onSectionCountChanged(sectionCount, totalDuration);
        mSectionProgressBar.removeLastBreakPoint();
    }

    @Override
    public void onRecordCompleted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtils.s(VideoRecordActivity.this, "record completed !");
            }
        });
    }

    @Override
    public void onConcatProgressUpdate(int num, int sectionCount) {

    }

    @Override
    public void onConcatFailed(final int errorCode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.dismiss();
                ToastUtils.s(VideoRecordActivity.this, "concat sections failed: " + errorCode);
            }
        });
    }

    @Override
    public void onConcatSuccess(final String filePath) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressDialog.dismiss();
                VideoEditActivity.start(VideoRecordActivity.this, filePath);
            }
        });
    }

    private void onSectionCountChanged(final int count, final long totalTime) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDeleteBtn.setEnabled(count > 0);
                mConcatBtn.setEnabled(totalTime >= RecordSettings.DEFAULT_MIN_RECORD_DURATION);
            }
        });
    }

    private PLCameraSetting.CAMERA_PREVIEW_SIZE_RATIO getPreviewSizeRatio(int position) {
        return RecordSettings.PREVIEW_SIZE_RATIO_ARRAY[position];
    }

    private PLCameraSetting.CAMERA_PREVIEW_SIZE_LEVEL getPreviewSizeLevel(int position) {
        return RecordSettings.PREVIEW_SIZE_LEVEL_ARRAY[position];
    }

    private PLVideoEncodeSetting.VIDEO_ENCODING_SIZE_LEVEL getEncodingSizeLevel(int position) {
        return RecordSettings.ENCODING_SIZE_LEVEL_ARRAY[position];
    }

    @Override
    public void onSurfaceCreated() {
        videoRect = new VideoRect();
    }

    @Override
    public void onSurfaceChanged(int i, int i1) {
    }

    @Override
    public void onSurfaceDestroy() {

    }

    @Override
    public int onDrawFrame(int i, int i1, int i2, long l) {

        //使用FBO，用于旋转texture
        videoRect.createFBO(i1, i2);
        videoRect.glBindFramebufferFBOId();

        final int isTracking = faceunity.fuIsTracking();
        if (isTracking != faceTrackingStatus) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isTracking == 0) {
                        Log.e(TAG, "detect1 fail");
                        mFaceTrackingStatusImageView.setVisibility(View.VISIBLE);
                    } else {
                        Log.e(TAG, "detect1 success");
                        mFaceTrackingStatusImageView.setVisibility(View.INVISIBLE);
                    }
                }
            });
            faceTrackingStatus = isTracking;
        }
        if (isNeedEffectItem) {
            isNeedEffectItem = false;
            mCreateItemHandler.sendEmptyMessage(CreateItemHandler.HANDLE_CREATE_ITEM);
        }

        faceunity.fuItemSetParam(mFacebeautyItem, "color_level", mFacebeautyColorLevel);
        faceunity.fuItemSetParam(mFacebeautyItem, "blur_level", mFacebeautyBlurLevel);
        faceunity.fuItemSetParam(mFacebeautyItem, "filter_name", mFilterName);
        faceunity.fuItemSetParam(mFacebeautyItem, "cheek_thinning", mFacebeautyCheeckThin);
        faceunity.fuItemSetParam(mFacebeautyItem, "eye_enlarging", mFacebeautyEnlargeEye);
        faceunity.fuItemSetParam(mFacebeautyItem, "face_shape", mFaceShape);
        faceunity.fuItemSetParam(mFacebeautyItem, "face_shape_level", mFaceShapeLevel);
        faceunity.fuItemSetParam(mFacebeautyItem, "red_level", mFacebeautyRedLevel);

        if (mCameraNV21Byte == null || mCameraNV21Byte.length == 0) {
            Log.e(TAG, "camera nv21 bytes null");
            return i;
        }
//        int fuTex = faceunity.fuRenderToNV21Image(mCameraNV21Byte,
//                i2, i1, mFrameId++, new int[]{mEffectItem, mFacebeautyItem});
        int fuTex = faceunity.fuDualInputToTexture(mCameraNV21Byte, i, 1,
                i2, i1, mFrameId++, new int[]{mEffectItem, mFacebeautyItem});
        videoRect.draw(fuTex, cameraFaing);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        return videoRect.getFboTex();
    }


    private VideoRect videoRect;

    @Override
    public boolean onPreviewFrame(byte[] bytes, int i, int i1, int i2, int i3, long l) {
        Log.d(TAG, "onDrawFrame " + mFrameId + " : i = " + i + " ; i1 = " + i1 + " ; i2 = " + i2 + " ; i3 = " + i3 + " ; l = " + l);
        mCameraNV21Byte = bytes;
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_choose_effect:
                setEffectFilterBeautyChooseBtnTextColor(mChooseEffectBtn);
                setEffectFilterBeautyChooseBlock(mEffectRecyclerView);
                break;
            case R.id.btn_choose_filter:
                setEffectFilterBeautyChooseBtnTextColor(mChooseFilterBtn);
                setEffectFilterBeautyChooseBlock(mFilterRecyclerView);
                break;
            case R.id.btn_choose_blur_level:
                setEffectFilterBeautyChooseBtnTextColor(mChooseBlurLevelBtn);
                setEffectFilterBeautyChooseBlock(mBlurLevelSelect);
                break;
            case R.id.btn_choose_color_level:
                setEffectFilterBeautyChooseBtnTextColor(mChooseColorLevelBtn);
                setEffectFilterBeautyChooseBlock(mColorLevelSelect);
                break;
            case R.id.btn_choose_face_shape:
                setEffectFilterBeautyChooseBtnTextColor(mChooseFaceShapeBtn);
                setEffectFilterBeautyChooseBlock(mFaceShapeSelect);
                break;
            case R.id.btn_choose_red_level:
                setEffectFilterBeautyChooseBtnTextColor(mChooseRedLevelBtn);
                setEffectFilterBeautyChooseBlock(mRedLevelSelect);
                break;
            case R.id.face_shape_0_nvshen:
                setFaceShapeBackground(mFaceShape0Nvshen);
                onFaceShapeSelected(0);
                break;
            case R.id.face_shape_1_wanghong:
                setFaceShapeBackground(mFaceShape1Wanghong);
                onFaceShapeSelected(1);
                break;
            case R.id.face_shape_2_ziran:
                setFaceShapeBackground(mFaceShape2Ziran);
                onFaceShapeSelected(2);
                break;
            case R.id.face_shape_3_default:
                setFaceShapeBackground(mFaceShape3Default);
                onFaceShapeSelected(3);
                break;
        }
    }

    private void setBlurLevelTextBackground(TextView tv) {
        mBlurLevels[0].setBackground(getResources().getDrawable(R.drawable.zero_blur_level_item_unselected));
        for (int i = 1; i < BLUR_LEVEL_TV_ID.length; i++) {
            mBlurLevels[i].setBackground(getResources().getDrawable(R.drawable.blur_level_item_unselected));
        }
        if (tv == mBlurLevels[0]) {
            tv.setBackground(getResources().getDrawable(R.drawable.zero_blur_level_item_selected));
        } else {
            tv.setBackground(getResources().getDrawable(R.drawable.blur_level_item_selected));
        }
    }

    private void setFaceShapeBackground(TextView tv) {
        mFaceShape0Nvshen.setBackground(getResources().getDrawable(R.color.unselect_gray));
        mFaceShape1Wanghong.setBackground(getResources().getDrawable(R.color.unselect_gray));
        mFaceShape2Ziran.setBackground(getResources().getDrawable(R.color.unselect_gray));
        mFaceShape3Default.setBackground(getResources().getDrawable(R.color.unselect_gray));
        tv.setBackground(getResources().getDrawable(R.color.faceunityYellow));
    }

    private void setEffectFilterBeautyChooseBlock(View v) {
        mEffectRecyclerView.setVisibility(View.INVISIBLE);
        mFilterRecyclerView.setVisibility(View.INVISIBLE);
        mFaceShapeSelect.setVisibility(View.INVISIBLE);
        mBlurLevelSelect.setVisibility(View.INVISIBLE);
        mColorLevelSelect.setVisibility(View.INVISIBLE);
        mRedLevelSelect.setVisibility(View.INVISIBLE);
        v.setVisibility(View.VISIBLE);
    }

    private void setEffectFilterBeautyChooseBtnTextColor(Button selectedBtn) {
        mChooseEffectBtn.setTextColor(getResources().getColor(R.color.colorWhite));
        mChooseColorLevelBtn.setTextColor(getResources().getColor(R.color.colorWhite));
        mChooseBlurLevelBtn.setTextColor(getResources().getColor(R.color.colorWhite));
        mChooseFilterBtn.setTextColor(getResources().getColor(R.color.colorWhite));
        mChooseFaceShapeBtn.setTextColor(getResources().getColor(R.color.colorWhite));
        mChooseRedLevelBtn.setTextColor(getResources().getColor(R.color.white));
        selectedBtn.setTextColor(getResources().getColor(R.color.faceunityYellow));
    }

    private void onBlurLevelSelected(int level) {
        switch (level) {
            case 0:
                mFacebeautyBlurLevel = 0;
                break;
            case 1:
                mFacebeautyBlurLevel = 1.0f;
                break;
            case 2:
                mFacebeautyBlurLevel = 2.0f;
                break;
            case 3:
                mFacebeautyBlurLevel = 3.0f;
                break;
            case 4:
                mFacebeautyBlurLevel = 4.0f;
                break;
            case 5:
                mFacebeautyBlurLevel = 5.0f;
                break;
            case 6:
                mFacebeautyBlurLevel = 6.0f;
                break;
        }
    }

    private void onCheekThinSelected(int progress, int max) {
        mFacebeautyCheeckThin = 1.0f * progress / max;
    }

    private void onColorLevelSelected(int progress, int max) {
        mFacebeautyColorLevel = 1.0f * progress / max;
    }

    private void onEffectItemSelected(String effectItemName) {
        if (effectItemName.equals(mEffectFileName)) {
            return;
        }
        mCreateItemHandler.removeMessages(CreateItemHandler.HANDLE_CREATE_ITEM);
        mEffectFileName = effectItemName;
        isNeedEffectItem = true;
    }

    private void onEnlargeEyeSelected(int progress, int max) {
        mFacebeautyEnlargeEye = 1.0f * progress / max;
    }

    private void onFilterSelected(String filterName) {
        mFilterName = filterName;
    }

    private void onRedLevelSelected(int progress, int max) {
        mFacebeautyRedLevel = 1.0f * progress / max;
    }

    private void onFaceShapeLevelSelected(int progress, int max) {
        mFaceShapeLevel = (1.0f * progress) / max;
        Log.e(TAG, "faceshape level " + mFaceShapeLevel);
    }

    private void onFaceShapeSelected(int faceShape) {
        mFaceShape = faceShape;
        Log.e(TAG, "faceshape " + mFaceShape);
    }

    class CreateItemHandler extends Handler {

        static final int HANDLE_CREATE_ITEM = 1;

        WeakReference<Context> mContext;

        CreateItemHandler(Looper looper, Context context) {
            super(looper);
            mContext = new WeakReference<Context>(context);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLE_CREATE_ITEM:
                    Log.e(TAG, "HANDLE_CREATE_ITEM = " + mEffectFileName);
                    try {
                        if (mEffectFileName.equals("none")) {
                            mEffectItem = 0;
                        } else {
                            InputStream is = mContext.get().getAssets().open(mEffectFileName);
                            byte[] itemData = new byte[is.available()];
                            is.read(itemData);
                            is.close();
                            int tmp = mEffectItem;
                            mEffectItem = faceunity.fuCreateItemFromPackage(itemData);
                            faceunity.fuItemSetParam(mEffectItem, "isAndroid", 1.0);
                            if (tmp != 0) {
                                faceunity.fuDestroyItem(tmp);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }
}
