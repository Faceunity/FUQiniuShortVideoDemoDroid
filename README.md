# FUQiniuShortVideoDemo（android）

## 概述

Faceunity SDK与七牛短视频SDK对接demo。

## SDK使用介绍

 - Faceunity SDK的使用方法请参看 [**Faceunity/FULiveDemoDroid**][1]
 - 七牛短视频SDK的使用方法请参看《PLDroidShortVideo.pdf》

## 集成方法

本demo中把大部分关于faceunity的代码都封装在library（faceunity）中，其中核心类为FaceunityWrapper（GL的一系列操作，以及faceunity SDK具体调用实现）。而在VideoRecordActivity（录制视频界面）中仅需要添加相应的布局（可使用demo中已经封装好的自定义View，FaceunityControlView）以及对七牛接口的实现。
而在FaceunityWrapper中有几个主要的方法：

```
// onSurfaceCreated 当Surface创建时被回调，用于实现一系列的初始化操作
public void onSurfaceCreated(Context context) {...}
// onSurfaceDestroyed 当Surface销毁时被回调，用于实现一系列的资源销毁操作
public void onSurfaceDestroyed() {...}
// onDrawFrame 用于返回每帧画面的texture
public int onDrawFrame(int texId, int texWidth, int texHeight) {...}
// onDrawFrame 用于返回每帧画面的byte[]数据
public boolean onPreviewFrame(byte[] bytes, int i, int i1, int i2, int i3, long l) {...}
```

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

#### 图像数据旋转问题解决

由于在faceunity使用的是双输入的处理画面方式，因此需要texture以及byte[]两个数据。而在onDrawFrame得到的texture和在onPreviewFrame得到的byte[]的画面数据的方向不一致，因此使用faceunity.fuDualInputToTexture绘制出来的画面会出现贴纸与人脸错位的状况。因此需要使用两个FBO进行两次画面的转换。

```
mCameraMatrix1 = new float[16];
mCameraMatrix2 = new float[16];
if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
    Matrix.setRotateM(mCameraMatrix1, 0, -mCameraRotate, 0F, 0F, 1F);
    Matrix.setRotateM(mCameraMatrix2, 0, mCameraRotate, 0F, 0F, 1F);
} else {
    Matrix.setIdentityM(mCameraMatrix1, 0);
    mCameraMatrix1[0] = -1;
    Matrix.rotateM(mCameraMatrix1, 0, -mCameraRotate, 0F, 0F, 1F);
    Matrix.setIdentityM(mCameraMatrix2, 0);
    mCameraMatrix2[0] = -1;
    Matrix.rotateM(mCameraMatrix2, 0, -mCameraRotate, 0F, 0F, 1F);
}
```

其中mCameraMatrix1矩阵用于把texture转换成与byte[]相同方向。
其中mCameraMatrix2矩阵用于把绘制完成的换面转换成屏幕上可以正常显示的方向。


  [1]: https://github.com/Faceunity/FULiveDemoDroid