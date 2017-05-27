package com.qiniu.pili.droid.shortvideo.demo.manager;

import android.util.Log;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCancellationSignal;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UpProgressHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.pili.droid.shortvideo.demo.utils.Config;

import org.json.JSONObject;

public class VideoUploadManager {
    private static final String TAG = "VideoUploadManager";

    private static VideoUploadManager _instance;
    private UploadManager mUploadManager;
    private volatile boolean mIsCancelled = false;
    private UploadOptions mUploadOptions;
    private OnUploadProgressListener mOnUploadProgressListener;
    private OnUploadStateListener mOnUploadStateListener;

    private VideoUploadManager() {}

    public static VideoUploadManager getInstance() {
        if (null == _instance) {
            _instance = new VideoUploadManager();
        }
        return _instance;
    }

    public interface OnUploadProgressListener {
        void onUploadProgress(String key, double percent);
    }

    public interface OnUploadStateListener {
        void onUploadState(String key, ResponseInfo info);
    }

    public void init() {
        if (null == mUploadManager) {
            Configuration config = new Configuration.Builder()
                    .chunkSize(256 * 1024)  //分片上传时，每片的大小。 默认256K
                    .putThreshhold(512 * 1024)  // 启用分片上传阀值。默认512K
                    .connectTimeout(10) // 链接超时。默认10秒
                    .responseTimeout(60) // 服务器响应超时。默认60秒
                    .zone(Config.DEFAULT_ZONE) // 设置区域，指定不同区域的上传域名、备用域名、备用IP。
                    .build();
            mUploadManager = new UploadManager(config);
        }
        mUploadOptions = new UploadOptions(null, null, false, mUpLoadProgressHandler, mUpLoadCancellationSignal);
    }

    public void startUpload(String filePath, String token) {
        mIsCancelled = false;
        String fileName = getFileName(filePath);
        Log.d(TAG, "file name = " + fileName);
        mUploadManager.put(filePath, fileName, token, mUpLoadCompletionHandler, mUploadOptions);
    }

    public void stopUpload() {
        Log.d(TAG, "stop upload");
        mIsCancelled = true;
    }

    private UpCancellationSignal mUpLoadCancellationSignal = new UpCancellationSignal() {
        @Override
        public boolean isCancelled() {
            return mIsCancelled;
        }
    };

    private UpProgressHandler mUpLoadProgressHandler = new UpProgressHandler() {
        @Override
        public void progress(String key, double percent) {
            mOnUploadProgressListener.onUploadProgress(key, percent);
        }
    };

    private UpCompletionHandler mUpLoadCompletionHandler = new UpCompletionHandler() {
        @Override
        public void complete(String key, ResponseInfo info, JSONObject response) {
            mOnUploadStateListener.onUploadState(key, info);
        }
    };

    public static String getFileName(String path) {
        String[] slices = path.split("/");
        if (slices.length > 1) {
            return slices[slices.length - 1];
        } else {
            return "";
        }
    }

    public void setOnUploadProgressListener(OnUploadProgressListener listener) {
        mOnUploadProgressListener = listener;
    }

    public void setOnUploadStateListner(OnUploadStateListener listener) {
        mOnUploadStateListener = listener;
    }
}
