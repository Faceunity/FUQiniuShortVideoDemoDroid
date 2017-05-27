package com.qiniu.pili.droid.shortvideo.demo.activity;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.VideoView;

import com.gc.materialdesign.views.ProgressBarDeterminate;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.pili.droid.shortvideo.demo.R;
import com.qiniu.pili.droid.shortvideo.demo.manager.VideoUploadManager;
import com.qiniu.pili.droid.shortvideo.demo.utils.Config;
import com.qiniu.pili.droid.shortvideo.demo.utils.ToastUtils;

public class PlaybackActivity extends Activity implements
        VideoUploadManager.OnUploadProgressListener,
        VideoUploadManager.OnUploadStateListener {
    private static final String MP4_PATH = "MP4_PATH";

    private VideoView mVideoView;
    private Button mUploadBtn;
    private VideoUploadManager mVideoUploadManager;
    private ProgressBarDeterminate mProgressBarDeterminate;
    private boolean mIsUpload = false;
    private String mVideoPath;

    public static void start(Activity activity, String mp4Path) {
        Intent intent = new Intent(activity, PlaybackActivity.class);
        intent.putExtra(MP4_PATH, mp4Path);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_playback);

        mVideoUploadManager = VideoUploadManager.getInstance();
        mVideoUploadManager.init();
        mVideoUploadManager.setOnUploadProgressListener(this);
        mVideoUploadManager.setOnUploadStateListner(this);

        mUploadBtn = (Button) findViewById(R.id.upload_btn);
        mUploadBtn.setText(R.string.upload);
        mUploadBtn.setOnClickListener(new UploadOnClickListener());
        mProgressBarDeterminate = (ProgressBarDeterminate) findViewById(R.id.progressBar);
        mProgressBarDeterminate.setMin(0);
        mProgressBarDeterminate.setMax(100);
        mVideoView = (VideoView) findViewById(R.id.video);
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mVideoView.start();
            }
        });
        mVideoPath = getIntent().getStringExtra(MP4_PATH);
        mVideoView.setVideoPath(mVideoPath);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVideoView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoView.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVideoView.stopPlayback();
    }

    public class UploadOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (!mIsUpload) {
                mVideoUploadManager.startUpload(mVideoPath, Config.TOKEN);
                mProgressBarDeterminate.setVisibility(View.VISIBLE);
                mUploadBtn.setText(R.string.cancel_upload);
                mIsUpload = true;
            } else {
                mVideoUploadManager.stopUpload();
                mProgressBarDeterminate.setVisibility(View.INVISIBLE);
                mUploadBtn.setText(R.string.upload);
                mIsUpload = false;
            }
        }
    }

    @Override
    public void onUploadProgress(String key, double percent) {
        mProgressBarDeterminate.setProgress((int)(percent * 100));
        if (1.0 == percent) {
            mProgressBarDeterminate.setVisibility(View.INVISIBLE);
        }
    }

    public void copyToClipboard(String filePath) {
        ClipData clipData = ClipData.newPlainText("VideoFilePath", filePath);
        ClipboardManager clipboardManager = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardManager.setPrimaryClip(clipData);
    }

    @Override
    public void onUploadState(String key, ResponseInfo info) {
        if (info.isOK()) {
            String filePath = "http://" + Config.DOMAIN + "/" + key;
            copyToClipboard(filePath);
            ToastUtils.l(this, "文件上传成功，" + filePath + "已复制到粘贴板");
            mUploadBtn.setVisibility(View.INVISIBLE);
        } else {
            ToastUtils.l(this, "Upload failed, statusCode = " + info.statusCode + " error = " + info.error);
        }
    }
}
