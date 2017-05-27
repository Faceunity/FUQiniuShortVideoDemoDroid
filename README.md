# FUQiniuShortVideoDemo（android）

## 概述

Faceunity SDK与七牛短视频SDK对接demo。

## SDK使用介绍

 - Faceunity SDK的使用方法请参看 [**Faceunity/FULiveDemoDroid**][1]
 - 七牛短视频SDK的使用方法请参看《PLDroidShortVideo.pdf》

## 集成方法

首先添加相应的SDK库文件与数据文件，然后在VideoRecordActivity（录制视频界面）中完成布局相应的界面（界面不作过多的赘述）。

### 环境初始化
加载Faceunity SDK所需要的数据文件（读取人脸数据文件、美颜数据文件）：
```
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
```

### 实现PLCameraPreviewListener接口
接口PLCameraPreviewListener在Camera.PreviewCallback接口中被回调，用于于获取摄像头返回的byte[]图像数据。需要重写onPreviewFrame方法：
```
@Override
public boolean onPreviewFrame(byte[] bytes, int i, int i1, int i2, int i3, long l) {
    mCameraNV21Byte = bytes;
    return true;
}
```

### 实现PLVideoFilterListener接口
接口PLVideoFilterListener在GLSurfaceView.Renderer接口中被回调，用于获取纹理ID以及视频宽高、时间戳等数据。主要在PLVideoFilterListener接口的onDrawFrame方法中处理图像数据，并返回所需要显示的纹理ID。
#### 人脸识别状态
获取人脸识别状态，判断并修改UI以提示用户。
```
final int isTracking = faceunity.fuIsTracking();
if (isTracking != faceTrackingStatus) {
    runOnUiThread(new Runnable() {
        @Override
        public void run() {
            if (isTracking == 0) {
                mFaceTrackingStatusImageView.setVisibility(View.VISIBLE);
            } else {
                mFaceTrackingStatusImageView.setVisibility(View.INVISIBLE);
            }
        }
    });
    faceTrackingStatus = isTracking;
}
```
#### 道具加载
判断isNeedEffectItem（是否需要加载新道具数据flag），由于加载数据比较耗时，防止画面卡顿采用异步加载形式。
##### 发送加载道具Message
```
if (isNeedEffectItem) {
    isNeedEffectItem = false;
    mCreateItemHandler.sendEmptyMessage(CreateItemHandler.HANDLE_CREATE_ITEM);
}
```
##### 自定义Handler，收到Message异步加载道具
```
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
```
#### 美颜参数设置
```
faceunity.fuItemSetParam(mFacebeautyItem, "color_level", mFacebeautyColorLevel);
faceunity.fuItemSetParam(mFacebeautyItem, "blur_level", mFacebeautyBlurLevel);
faceunity.fuItemSetParam(mFacebeautyItem, "filter_name", mFilterName);
faceunity.fuItemSetParam(mFacebeautyItem, "cheek_thinning", mFacebeautyCheeckThin);
faceunity.fuItemSetParam(mFacebeautyItem, "eye_enlarging", mFacebeautyEnlargeEye);
faceunity.fuItemSetParam(mFacebeautyItem, "face_shape", mFaceShape);
faceunity.fuItemSetParam(mFacebeautyItem, "face_shape_level", mFaceShapeLevel);
faceunity.fuItemSetParam(mFacebeautyItem, "red_level", mFacebeautyRedLevel);
```
#### 处理图像数据
使用fuDualInputToTexture后会得到新的texture，返回的texture类型为TEXTURE_2D，其中要求输入的图像分别以内存数组byte[]以及OpenGL纹理的方式。下方代码中mCameraNV21Byte为在PLCameraPreviewListener接口回调中获取的图像数据，i为PLVideoFilterListener接口的onDrawFrame方法的纹理ID参数，i2与i1为图像数据的宽高。
```
int fuTex = faceunity.fuDualInputToTexture(mCameraNV21Byte, i, 1,
        i2, i1, mFrameId++, new int[]{mEffectItem, mFacebeautyItem});
```
#### 图像数据90度旋转问题解决
由于使用七牛短视频SDK提供的图像数据是旋转90度的，而七牛短视频SDK没有提供旋转camera画面的API，因此需要使用FBO方法对已经使用了Faceunity SDK处理过的纹理进行旋转操作。旋转采用MVP矩阵的形式来实现（具体的绘制过程代码不作过多赘述）：
```
final float[] mMVPMatrix = new float[16];
float[] scratch = new float[16];
final float[] mProjectionMatrix = new float[16];
final float[] mViewMatrix = new float[16];
final float[] mRotationMatrix = new float[16];
Matrix.frustumM(mProjectionMatrix, 0, -1, 1, -1, 1, 3, 100);
Matrix.setLookAtM(mViewMatrix, 0, 0, 0, cameraFaing == Camera.CameraInfo.CAMERA_FACING_FRONT ? 3 : -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
Matrix.multiplyMM(scratch, 0, mProjectionMatrix, 0, mViewMatrix, 0);
Matrix.setRotateM(mRotationMatrix, 0, cameraFaing == Camera.CameraInfo.CAMERA_FACING_FRONT ? 90 : 270, 0, 0, 1.0f);
Matrix.multiplyMM(mMVPMatrix, 0, scratch, 0, mRotationMatrix, 0);
```


  [1]: https://github.com/Faceunity/FULiveDemoDroid