package com.qiniu.pili.droid.shortvideo.demo.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.faceunity.EffectEnum;
import com.faceunity.FURenderer;
import com.faceunity.entity.Effect;
import com.qiniu.pili.droid.shortvideo.PLShortVideoEditor;
import com.qiniu.pili.droid.shortvideo.PLVideoEditSetting;
import com.qiniu.pili.droid.shortvideo.PLVideoFilterListener;
import com.qiniu.pili.droid.shortvideo.PLVideoPlayerListener;
import com.qiniu.pili.droid.shortvideo.PLVideoSaveListener;
import com.qiniu.pili.droid.shortvideo.demo.R;
import com.qiniu.pili.droid.shortvideo.demo.fusdk.EffetsAdapter;
import com.qiniu.pili.droid.shortvideo.demo.fusdk.FuSDKManager;
import com.qiniu.pili.droid.shortvideo.demo.utils.Config;
import com.qiniu.pili.droid.shortvideo.demo.utils.GetPathFromUri;
import com.qiniu.pili.droid.shortvideo.demo.utils.ToastUtils;
import com.qiniu.pili.droid.shortvideo.demo.view.CustomProgressDialog;
import com.qiniu.pili.droid.shortvideo.demo.view.SectionProgressBar;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by ly on 18/8/28.
 */

public class VideoFuEditActivity extends Activity implements PLVideoSaveListener {

    private static final String TAG = "VideoFuEditActivity";
    private static final String MP4_PATH = "MP4_PATH";

    private static final int REQUEST_CODE_PICK_AUDIO_MIX_FILE = 0;
    private static final int REQUEST_CODE_DUB = 1;
    //    private TuEffectListAdapter mTuEffectListAdapter;
    EffetsAdapter mEffetsAdapter;
    private String[] mEffectColors = {
            "#33ffefa8", "#33fccaff", "#3361dcff", "#33ffc502", "#33ff8621", "#33FF4081", "#337777ff"
    };
    private PLShortVideoEditorStatus mShortVideoEditorStatus = PLShortVideoEditorStatus.Idle;
    private boolean mSceneMagicEditing = false;
    private boolean mIsEffectShow = false;

    private GLSurfaceView mPreviewView;
    private RecyclerView mFiltersList;
    private CustomProgressDialog mProcessingDialog;
    private ImageButton mPausePalybackButton;

    private PLShortVideoEditor mShortVideoEditor;
    private String mSelectedFilter;
    private String mSelectedMV;
    private String mSelectedMask;
    private SectionProgressBar mSectionProgressBar;
    private List<Effect> mEffects;
    //    private TuSDKManager mTuSDKManager;
    private FuSDKManager mFuSDKManager;
    private long mMixDuration = 5000; // ms
    private boolean mIsPlaying = true;
    private String mMp4path;
    private volatile boolean mCancelSave;
    private volatile boolean mIsVideoPlayCompleted;
    PLVideoPlayerListener mVideoPlayerListener = new PLVideoPlayerListener() {
        @Override
        public void onCompletion() {
            if (mIsEffectShow) {
                mIsVideoPlayCompleted = true;
                mShortVideoEditor.pausePlayback();
                Log.i(TAG, "松开时获取时间：" + mShortVideoEditor.getCurrentPosition());
            }
        }
    };
    /**
     * 预览时为视频添加场景特效
     */
    private PLVideoFilterListener mVideoPlayFilterListener = new PLVideoFilterListener() {

        @Override
        public void onSurfaceCreated() {
            Log.e(TAG, "onSurfaceCreated ");
            mFuSDKManager.setupPreviewFilterEngine();
            mFuSDKManager.getPreviewFilterEngine().loadItems();
        }

        @Override
        public void onSurfaceChanged(int width, int height) {
//            if (mIsEffectShow) {
//                resetEffects();
//            }
            Log.d(TAG, "onSurfaceChanged " + width + " " + height);
        }

        @Override
        public void onSurfaceDestroy() {
            if (mFuSDKManager.getPreviewFilterEngine() != null) {
                mFuSDKManager.getPreviewFilterEngine().destroyItems();
            }
            synchronized (VideoFuEditActivity.this) {
                mFuSDKManager.destroyPreviewFilterEngine();
            }
        }

        @Override
        public int onDrawFrame(int texId, int texWidth, int texHeight, long timestampNs, float[] transformMatrix) {
            if (mCancelSave && mFuSDKManager.getPreviewFilterEngine() == null) {
                mFuSDKManager.setupPreviewFilterEngine();
                mFuSDKManager.getPreviewFilterEngine().onSurfaceCreated();
                mCancelSave = false;
                pausePlayback();
            }

            int curPos = mShortVideoEditor.getCurrentPosition();

            if (mSceneMagicEditing && mFuSDKManager.getLastMagicModel() != null) {
                mFuSDKManager.getLastMagicModel().getTimeRange().end = curPos;
                Log.e(TAG, "onDrawFrame设置结束时间 " + "    mFuSDKManager.getLastMagicModel().getTimeRange().end" + " " + mFuSDKManager.getLastMagicModel().getTimeRange().end);
            }

            FuSDKManager.MagicModel model = mFuSDKManager.findMagicModelWithPosition(curPos);

            synchronized (VideoFuEditActivity.this) {
                if (mFuSDKManager.getPreviewFilterEngine() != null) {
                    if (model != null) {
                        Log.i(TAG, "mSceneMagicEditing:" + mSceneMagicEditing + "    model.getMagicCode():" + model.getMagicCode().bundleName());
                        FURenderer preRender = mFuSDKManager.getPreviewFilterEngine();
                        preRender.onEffectSelected(model.getMagicCode());
                        int id = preRender.onDrawFrame(texId, texWidth, texHeight);
                        Log.i(TAG, "mSceneMagicEditing:" + mSceneMagicEditing + " model:" + model + " texId:" + id + "--width="
                                + texWidth + "--height=" + texHeight);
                        return id;
                    } else {
                        Log.i(TAG, "model:" + model + "    curPos:" + curPos);
                    }
                }
            }
            return texId;
        }
    };

    private EffetsAdapter.ItemTouchListener mOnEffectTouchListener = new EffetsAdapter.ItemTouchListener() {
        private int PRESS_DELAY_TIME = 300;
        private Timer timer;
        private long downTime;

        private void onActionDown(Effect effectCode, String color) {
            //            if (mIsVideoPlayCompleted) {
            //                // 当预览至视频结束位置时，再次长按时重置场景特效信息
            ////                resetEffects();
            //                return;
            //            }

            // 获取当前视频位置
            float startPosition = mShortVideoEditor.getCurrentPosition();

            mSceneMagicEditing = true;
            // 切换场景特效
            mFuSDKManager.getPreviewFilterEngine().onEffectSelected(effectCode);
            // 将当前场景特效添加至场景纪录
            FuSDKManager.MagicModel m = new FuSDKManager.MagicModel(effectCode, FuSDKManager.FuSDKTimeRange.makeRange(startPosition, startPosition));
            mFuSDKManager.addMagicModel(m);

            mSectionProgressBar.setBarColor(Color.parseColor(color));
            SectionProgressBar.BreakPointInfo breakPointInfo = new SectionProgressBar.BreakPointInfo();
            breakPointInfo.setStartTime(mShortVideoEditor.getCurrentPosition());
            mSectionProgressBar.addBreakPointTime(breakPointInfo);
            mSectionProgressBar.setCurrentState(SectionProgressBar.State.START);
            startPlayback();
            Log.i(TAG, "onActionDown: duration:" + mShortVideoEditor.getDurationMs());
        }

        @Override
        public boolean touch(View v, MotionEvent motionEvent, final int position) {
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                timer = new Timer();
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                onActionDown(mEffects.get(position), mEffectColors[position % 7]);
                            }
                        });
                    }
                };
                timer.schedule(task, PRESS_DELAY_TIME);
                downTime = System.currentTimeMillis();

            } else if (motionEvent.getAction() == MotionEvent.ACTION_UP || motionEvent.getAction() == MotionEvent.ACTION_CANCEL || motionEvent.getAction() == MotionEvent.ACTION_POINTER_UP) {
                long curTime = System.currentTimeMillis();
                if (curTime - downTime < PRESS_DELAY_TIME && !mSceneMagicEditing) {
                    timer.cancel();
                    return false;
                }
                // 松手时暂停预览场景特效
                pausePlayback();
                if (mIsVideoPlayCompleted) {
//                    mSectionProgressBar.addBreakPointTime(mMixDuration);
                    mSectionProgressBar.getBreakPointInfoList().getLast().setTime(mMixDuration);

                    Log.i(TAG, "松开时获取时间：" + mMixDuration);
                } else {
//                    mSectionProgressBar.addBreakPointTime(mCurrentFragment.getCurrentPosition());
//                    Log.i(TAG,"松开时获取时间："+mCurrentFragment.getCurrentPosition());
                    mSectionProgressBar.getBreakPointInfoList().getLast().setTime(mShortVideoEditor.getCurrentPosition());
                }
                mSectionProgressBar.setCurrentState(SectionProgressBar.State.PAUSE);
                mSceneMagicEditing = false;
            }
            return true;
        }
    };
    /**
     * 保存存时为视频添加场景特效
     */
    private PLVideoFilterListener mVideoSaveFilterListener = new PLVideoFilterListener() {
        // 记录保存时的起始时间戳
        private long startTimeMs = 0;

        @Override
        public void onSurfaceCreated() {
            Log.i(TAG, "保存编辑视屏onSurfaceCreated");
            startTimeMs = 0;
            mFuSDKManager.setupSaveFilterEngine();
            mFuSDKManager.getSaveFilterEngine().loadItems();
        }

        @Override
        public void onSurfaceChanged(int i, int i1) {
            Log.i(TAG, "保存编辑视屏onSurfaceChanged");
        }

        @Override
        public void onSurfaceDestroy() {
            Log.i(TAG, "保存编辑视屏onSurfaceDestroy");

            mFuSDKManager.destroySaveFilterEngine();

        }

        @Override
        public int onDrawFrame(int texId, int texWidth, int texHeight, long timestampNs, float[] transformMatrix) {
            long currentTimeMs = (long) Math.ceil(timestampNs / 1000000L);
            if (startTimeMs == 0) {
                startTimeMs = currentTimeMs;
            }

            if (mFuSDKManager.getSaveFilterEngine() != null) {
                // 根据保存进度更新场景特效
                FuSDKManager.MagicModel magicModel = mFuSDKManager.findMagicModelWithPosition(currentTimeMs - startTimeMs);

                if (magicModel != null) {
                    FURenderer saveRunder = mFuSDKManager.getSaveFilterEngine();
                    saveRunder.onEffectSelected(magicModel.getMagicCode());
                    Log.i(TAG, "保存编辑视屏onDrawFrame" + magicModel.getMagicCode());
                    return saveRunder.onDrawFrame(texId, texWidth, texHeight);
                } else {
                    Log.i(TAG, "保存编辑视屏onDrawFrame是空");
                }
            } else {
                Log.i(TAG, "保存编辑视屏getSaveFilterEngine是空");
            }
            return texId;
        }
    };

    public static void start(Activity activity, String mp4Path) {
        Intent intent = new Intent(activity, VideoFuEditActivity.class);
        intent.putExtra(MP4_PATH, mp4Path);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        setContentView(R.layout.activity_video_edit_fu);
        mPausePalybackButton = (ImageButton) findViewById(R.id.pause_playback);
        mSectionProgressBar = (SectionProgressBar) findViewById(R.id.effect_progressbar);

        initPreviewView();

        initProcessingDialog();
        initShortVideoEditor();
        initEffects();
        initFiltersList();
    }

    /**
     * 启动预览
     */
    private void startPlayback() {
        if (mShortVideoEditorStatus == PLShortVideoEditorStatus.Idle) {
            mShortVideoEditor.startPlayback(mVideoPlayFilterListener);
            mShortVideoEditorStatus = PLShortVideoEditorStatus.Playing;
        } else if (mShortVideoEditorStatus == PLShortVideoEditorStatus.Paused) {
            mShortVideoEditor.resumePlayback();
            mShortVideoEditorStatus = PLShortVideoEditorStatus.Playing;
        }
    }

    /**
     * 停止预览
     */
    private void stopPlayback() {
        mShortVideoEditor.stopPlayback();
        mShortVideoEditorStatus = PLShortVideoEditorStatus.Idle;
    }

    /**
     * 暂停预览
     */
    private void pausePlayback() {
        mShortVideoEditor.pausePlayback();
        mShortVideoEditorStatus = PLShortVideoEditorStatus.Paused;
    }

    private void initEffects() {
        //        mTuSDKManager = new TuSDKManager(getBaseContext());
        mFuSDKManager = new FuSDKManager(getBaseContext());
        mSectionProgressBar.setTotalTime(this, mMixDuration);
        mSectionProgressBar.setFirstPointTime(0);

        mEffects = EffectEnum.getEffects();
        mEffetsAdapter = new EffetsAdapter(VideoFuEditActivity.this, mEffects);
        //
        //        mTuEffectListAdapter = new TuEffectListAdapter(this);
        //        mTuEffectListAdapter.setEffectOnTouchListener(mOnEffectTouchListener);
    }

    private void initFiltersList() {
        mFiltersList = (RecyclerView) findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mFiltersList.setLayoutManager(layoutManager);
    }

    private void initShortVideoEditor() {
        mMp4path = getIntent().getStringExtra(MP4_PATH);
        Log.i(TAG, "editing file: " + mMp4path);
        PLVideoEditSetting setting = new PLVideoEditSetting();
        setting.setSourceFilepath(mMp4path);
        setting.setDestFilepath(Config.EDITED_FILE_PATH);
        mShortVideoEditor = new PLShortVideoEditor(mPreviewView, setting);
        mShortVideoEditor.setVideoSaveListener(this);
        mShortVideoEditor.setVideoPlayerListener(mVideoPlayerListener);
        mMixDuration = mShortVideoEditor.getDurationMs();
    }

    private void initPreviewView() {
        mPreviewView = (GLSurfaceView) findViewById(R.id.preview);
        mPreviewView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

    private void initProcessingDialog() {
        mProcessingDialog = new CustomProgressDialog(this);
        mProcessingDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mShortVideoEditor.cancelSave();
            }
        });
    }

    public void onClickReset(View v) {
        mSelectedFilter = null;
        mSelectedMV = null;
        mSelectedMask = null;
        mShortVideoEditor.setBuiltinFilter(null);
        mShortVideoEditor.setMVEffect(null, null);
        mShortVideoEditor.setAudioMixFile(null);
    }

    public void onClickShowEffect(View v) {
        resetEffects();
        mEffetsAdapter.setItemTouchListener(mOnEffectTouchListener);
        mFiltersList.setAdapter(mEffetsAdapter);
    }

    public void onClickBack(View v) {
        finish();
    }

    public void onClickTogglePlayback(View v) {
        if (mShortVideoEditorStatus == PLShortVideoEditorStatus.Playing) {
            pausePlayback();
        } else {
            startPlayback();
        }
        mPausePalybackButton.setImageResource(mIsPlaying ? R.mipmap.btn_play : R.mipmap.btn_pause);
        mIsPlaying = !mIsPlaying;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_CODE_PICK_AUDIO_MIX_FILE) {
            String selectedFilepath = GetPathFromUri.getPath(this, data.getData());
            Log.i(TAG, "Select file: " + selectedFilepath);
            if (!TextUtils.isEmpty(selectedFilepath)) {
                mShortVideoEditor.setAudioMixFile(selectedFilepath);
            }
        } else if (requestCode == REQUEST_CODE_DUB) {
            String dubMp4Path = data.getStringExtra(VideoDubActivity.DUB_MP4_PATH);
            if (!TextUtils.isEmpty(dubMp4Path)) {
                finish();
                //VideoEditActivity.start(this, dubMp4Path);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPlayback();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mShortVideoEditor.setBuiltinFilter(mSelectedFilter);
        startPlayback();
    }

    public void onSaveEdit(View v) {
        synchronized (this) {
            mFuSDKManager.destroyPreviewFilterEngine();
        }
        mProcessingDialog.show();
        mShortVideoEditor.save(mVideoSaveFilterListener);
    }

    @Override
    public void onSaveVideoSuccess(String filePath) {
        Log.i(TAG, "save edit success filePath: " + filePath);
        mProcessingDialog.dismiss();
        PlaybackActivity.start(VideoFuEditActivity.this, filePath);
    }

    @Override
    public void onSaveVideoFailed(final int errorCode) {
        Log.e(TAG, "save edit failed errorCode:" + errorCode);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProcessingDialog.dismiss();
                ToastUtils.toastErrorCode(VideoFuEditActivity.this, errorCode);
            }
        });
    }

    @Override
    public void onSaveVideoCanceled() {
        mProcessingDialog.dismiss();
        mCancelSave = true;
        if (mIsEffectShow) {
            onResume();
        }
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

    /// ========================= FuSDK 相关 ========================= ///
    private void resetEffects() {
        mSectionProgressBar.reset();
        mFuSDKManager.reset();
        mShortVideoEditor.seekTo(0);
        mIsVideoPlayCompleted = false;
        pausePlayback();
    }

    // 视频编辑器预览状态
    private enum PLShortVideoEditorStatus {
        Idle,
        Playing,
        Paused,
    }


    //    TuEffectListAdapter.OnEffectTouchListener mOnEffectTouchListener = new TuEffectListAdapter.OnEffectTouchListener() {
    //        private int PRESS_DELAY_TIME = 300;
    //        private Timer timer;
    //        private long downTime;
    //
    //        private void onActionDown(String effectCode, int color) {
    //            if (mIsVideoPlayCompleted) {
    //                // 当预览至视频结束位置时，再次长按时重置场景特效信息
    //                resetEffects();
    //            }
    //
    //            // 获取当前视频位置
    //            float startPosition = mShortVideoEditor.getCurrentPosition();
    //
    //            if (!effectCode.isEmpty()) {
    //                mSceneMagicEditing = true;
    //                // 切换场景特效
    //                mTuSDKManager.getPreviewFilterEngine().switchFilter(effectCode);
    //                // 将当前场景特效添加至场景纪录
    //                TuSDKManager.MagicModel m = new TuSDKManager.MagicModel(effectCode, TuSDKTimeRange.makeRange(startPosition, startPosition));
    //                mTuSDKManager.addMagicModel(m);
    //            }
    //
    //            mSectionProgressBar.setBarColor(color);
    //            mSectionProgressBar.setCurrentState(SectionProgressBar.State.START);
    //            startPlayback();
    //        }
    //
    //        @Override
    //        public boolean onTouch(View v, MotionEvent motionEvent, final String effectCode, final int color) {
    //            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
    //                timer = new Timer();
    //                TimerTask task = new TimerTask() {
    //                    @Override
    //                    public void run() {
    //                        runOnUiThread(new Runnable() {
    //                            @Override
    //                            public void run() {
    //                                onActionDown(effectCode, color);
    //                            }
    //                        });
    //                    }
    //                };
    //                timer.schedule(task, PRESS_DELAY_TIME);
    //                downTime = System.currentTimeMillis();
    //
    //            } else if (motionEvent.getAction() == MotionEvent.ACTION_UP || motionEvent.getAction() == MotionEvent.ACTION_CANCEL || motionEvent.getAction() == MotionEvent.ACTION_POINTER_UP) {
    //                long curTime = System.currentTimeMillis();
    //                if (curTime - downTime < PRESS_DELAY_TIME && !mSceneMagicEditing) {
    //                    timer.cancel();
    //                    return false;
    //                }
    //                // 松手时暂停预览场景特效
    //                pausePlayback();
    //                mSectionProgressBar.setCurrentState(SectionProgressBar.State.PAUSE);
    //                mSectionProgressBar.addBreakPointTime(mShortVideoEditor.getCurrentPosition());
    //                mSceneMagicEditing = false;
    //            }
    //            return true;
    //        }
    //    };

    //    /**
    //     * 预览时为视频添加场景特效
    //     */
    //    private PLVideoFilterListener mVideoPlayFilterListener = new PLVideoFilterListener() {
    //        @Override
    //        public void onSurfaceCreated() {
    //            mTuSDKManager.setupPreviewFilterEngine();
    //            mTuSDKManager.getPreviewFilterEngine().onSurfaceCreated();
    //        }
    //
    //        @Override
    //        public void onSurfaceChanged(int i, int i1) {
    //            if (mIsEffectShow) {
    //                resetEffects();
    //            }
    //            if (mTuSDKManager.getPreviewFilterEngine() != null) {
    //                mTuSDKManager.getPreviewFilterEngine().onSurfaceChanged(i, i1);
    //            }
    //        }
    //
    //        @Override
    //        public void onSurfaceDestroy() {
    //            if (mTuSDKManager.getPreviewFilterEngine() != null) {
    //                mTuSDKManager.getPreviewFilterEngine().onSurfaceDestroy();
    //            }
    //            synchronized (VideoFuEditActivity.this) {
    //                mTuSDKManager.destroyPreviewFilterEngine();
    //            }
    //        }
    //
    //        @Override
    //        public int onDrawFrame(int texId, int texWidth, int texHeight, long timestampNs, float[] transformMatrix) {
    //            if (mCancelSave && mTuSDKManager.getPreviewFilterEngine() == null) {
    //                mTuSDKManager.setupPreviewFilterEngine();
    //                mTuSDKManager.getPreviewFilterEngine().onSurfaceCreated();
    //                mCancelSave = false;
    //                pausePlayback();
    //            }
    //
    //            int curPos = mShortVideoEditor.getCurrentPosition();
    //
    //            if (mSceneMagicEditing && mTuSDKManager.getLastMagicModel() != null) {
    //                mTuSDKManager.getLastMagicModel().getTimeRange().end = curPos;
    //            }
    //
    //            TuSDKManager.MagicModel model = mTuSDKManager.findMagicModelWithPosition(curPos);
    //            Log.i(TAG,"mSceneMagicEditing:"+mSceneMagicEditing+"      model:"+model);
    //            synchronized (VideoFuEditActivity.this) {
    //                if (mTuSDKManager.getPreviewFilterEngine() != null) {
    //                    if (model != null) {
    //                        mTuSDKManager.getPreviewFilterEngine().switchFilter(model.getMagicCode());
    //                        return mTuSDKManager.getPreviewFilterEngine().processFrame(texId, texWidth, texHeight);
    //                    }
    //                }
    //            }
    //            return texId;
    //        }
    //    };
    //
    //    /**
    //     * 保存存时为视频添加场景特效
    //     */
    //    private PLVideoFilterListener mVideoSaveFilterListener = new PLVideoFilterListener() {
    //        // 记录保存时的起始时间戳
    //        private long startTimeMs = 0;
    //
    //        @Override
    //        public void onSurfaceCreated() {
    //            Log.i(TAG,"保存编辑视屏onSurfaceCreated");
    //            startTimeMs = 0;
    //            mTuSDKManager.setupSaveFilterEngine();
    //            mTuSDKManager.getSaveFilterEngine().onSurfaceCreated();
    //        }
    //
    //        @Override
    //        public void onSurfaceChanged(int i, int i1) {
    //            Log.i(TAG,"保存编辑视屏onSurfaceChanged");
    //            if (mTuSDKManager.getSaveFilterEngine() != null) {
    //                mTuSDKManager.getSaveFilterEngine().onSurfaceChanged(i, i1);
    //            }
    //        }
    //
    //        @Override
    //        public void onSurfaceDestroy() {
    //            Log.i(TAG,"保存编辑视屏onSurfaceDestroy");
    //            if (mTuSDKManager.getSaveFilterEngine() != null) {
    //                mTuSDKManager.getSaveFilterEngine().onSurfaceDestroy();
    //            }
    //            mTuSDKManager.destroySaveFilterEngine();
    //        }
    //
    //        @Override
    //        public int onDrawFrame(int texId, int texWidth, int texHeight, long timestampNs, float[] transformMatrix) {
    //            long currentTimeMs = (long) Math.ceil(timestampNs / 1000000L);
    //            if (startTimeMs == 0) {
    //                startTimeMs = currentTimeMs;
    //            }
    //
    //            if (mTuSDKManager.getSaveFilterEngine() != null) {
    //                // 根据保存进度更新场景特效
    //                TuSDKManager.MagicModel magicModel = mTuSDKManager.findMagicModelWithPosition(currentTimeMs - startTimeMs);
    //
    //                if (magicModel != null) {
    //                    mTuSDKManager.getSaveFilterEngine().switchFilter(magicModel.getMagicCode());
    //                    int id=mTuSDKManager.getSaveFilterEngine().processFrame(texId, texWidth, texHeight);
    //                    Log.i(TAG,"保存编辑视屏onDrawFrame"+"    magicModel.getMagicCode():"+magicModel.getMagicCode()+"    id:"+id);
    //                    return id;
    //                }
    //            }
    //            return texId;
    //        }
    //    };

}
