## 对接第三方 Demo 的 faceunity 模块

本工程是第三方 是集成了 FaceUnity 美颜贴纸功能和 **七牛短视频** 的 Demo。

本文是 FaceUnity SDK 快速对接云信即时通讯的导读说明，SDK 版本为 **7.1.0**。关于 SDK 的详细说明，请参看 **[FULiveDemoDroid](https://github.com/Faceunity/FULiveDemoDroid/)**

--------

## 集成方法

### 一、导入 SDK

- 将 faceunity 模块添加到工程中，下面是对库文件的说明。

  - assets/sticker 文件夹下 \*.bundle 是特效贴纸文件。
  - assets/makeup 文件夹下 \*.bundle 是美妆素材文件。
  - com/faceunity/nama/authpack.java 是鉴权证书文件，必须提供有效的证书才能运行 Demo，请联系技术支持获取。

  通过 Maven 依赖最新版 SDK：`implementation 'com.faceunity:nama:7.1.0'`，方便升级，推荐使用。

  其中，AAR 包含以下内容：

  ```
      +libs                                  
        -nama.jar                        // JNI 接口
      +assets
        +graphic                         // 图形效果道具
          -body_slim.bundle              // 美体道具
          -controller.bundle             // Avatar 道具
          -face_beautification.bundle    // 美颜道具
          -face_makeup.bundle            // 美妆道具
          -fuzzytoonfilter.bundle        // 动漫滤镜道具
          -fxaa.bundle                   // 3D 绘制抗锯齿
          -tongue.bundle                 // 舌头跟踪数据包
        +model                           // 算法能力模型
          -ai_face_processor.bundle      // 人脸识别AI能力模型，需要默认加载
          -ai_face_processor_lite.bundle // 人脸识别AI能力模型，轻量版
          -ai_gesture.bundle             // 手势识别AI能力模型
          -ai_human_processor.bundle     // 人体点位AI能力模型
      +jni                               // CNama fuai 库
        +armeabi-v7a
          -libCNamaSDK.so
          -libfuai.so
        +arm64-v8a
          -libCNamaSDK.so
          -libfuai.so
        +x86
          -libCNamaSDK.so
          -libfuai.so
        +x86_64
          -libCNamaSDK.so
          -libfuai.so
  ```

  如需指定应用的 so 架构，请修改 app 模块 build.gradle：

  ```groovy
  android {
      // ...
      defaultConfig {
          // ...
          ndk {
              abiFilters 'armeabi-v7a', 'arm64-v8a'
          }
      }
  }
  ```

  如需剔除不必要的 assets 文件，请修改 app 模块 build.gradle：

  ```groovy
  android {
      // ...
      applicationVariants.all { variant ->
          variant.mergeAssetsProvider.configure {
              doLast {
                  delete(fileTree(dir: outputDir, includes: ['model/ai_face_processor_lite.bundle',
                                                             'model/ai_gesture.bundle',
                                                             'graphics/controller.bundle',
                                                             'graphics/fuzzytoonfilter.bundle',
                                                             'graphics/fxaa.bundle',
                                                             'graphics/tongue.bundle']))
              }
          }
      }
  }
  ```

### 

### 二、使用 SDK

#### 1. 初始化

在 `FURenderer` 类 的  `setup` 方法是对 FaceUnity SDK 全局数据初始化的封装，可以在工作线程调用，仅需初始化一次即可。

在MainActivity 的 onCreate方法中执行。

#### 2.创建

在 `FURenderer` 类 的  `onSurfaceCreated` 方法是对 FaceUnity SDK 使用前数据初始化的封装。

在 VideoRecordActivity 类中注册 PLVideoFilterListener 监听， 在对应的生命周期内执行 

```java
        mShortVideoRecorder.setVideoFilterListener(new PLVideoFilterListener() {

            @Override
            public void onSurfaceCreated() {
                Log.d(TAG, "onSurfaceCreated: ");
                if (mFURenderer != null) {
                    mFURenderer.onSurfaceCreated();
                }
            }

            @Override
            public void onSurfaceChanged(int width, int height) {
                Log.d(TAG, "onSurfaceChanged() width = [" + width + "], height = [" + height + "]");
            }

            @Override
            public void onSurfaceDestroy() {
                if (mFURenderer != null) {
                    mFURenderer.onSurfaceDestroyed();
                }
            }

            /**
             * 绘制帧时触发 GL线程回调
             *
             * @param texId           待渲染的 SurfaceTexture 对象的 texture ID
             * @param texWidth        绘制的 surface 宽度
             * @param texHeight       绘制的 surface 高度
             * @param timeStampNs     该帧的时间戳，单位 Ns
             * @param transformMatrix 一般为单位矩阵，除非返回的纹理类型为 OES
             * @return 新创建的制定给 SurfaceTexuture 对象的 texture ID，类型必须为 GL_TEXTURE_2D
             */
            @Override
            public int onDrawFrame(int texId, int texWidth, int texHeight, long timeStampNs, float[] transformMatrix) {
                int id;
                if (mFURenderer != null) {
                    // 参数 mCameraData = null 表示单输出，否则使用双输入
                    id = mFURenderer.onDrawFrameSingleInput(texId, texWidth, texHeight);
                } else {
                    id = texId;
                }
                return id;
            }
        });
```

#### 3. 图像处理

在 `FURenderer` 类 的  `onDrawFrameXXX` 方法是对 FaceUnity SDK 图像处理的封装，该方法有许多重载方法适用于不同数据类型的需求。

在 VideoRecordActivity 类中注册 PLVideoFilterListener 监听，在 onDrawFrame 方法中执行。（代码如上）

onDrawFrameSingleInput 是单输入，输入图像buffer数组或者纹理Id，输出纹理Id
onDrawFrameDualInput 双输入，输入图像buffer数组与纹理Id，输出纹理Id。性能上，双输入优于单输入

在onDrawFrameSingleInput 与onDrawFrameDualInput 方法内，在执行底层方法之前，都会执行prepareDrawFrame()方法(执行各个特效模块的任务，将美颜参数传给底层)。

#### 4. 销毁

在 `FURenderer` 类 的  `onSurfaceDestroyed` 方法是对 FaceUnity SDK 退出前数据销毁的封装。

在 VideoRecordActivity 类中注册 PLVideoFilterListener 监听，在 onSurfaceDestroy方法中执行。（代码如上）

#### 5. 切换相机

调用 `FURenderer` 类 的  `onCameraChanged` 方法，用于重新为 SDK 设置参数。

在 VideoRecordActivity  类  onClickSwitchCamera  方法中执行。

#### 6. 旋转手机

调用 `FURenderer` 类 的  `onDeviceOrientationChanged` 方法，用于重新为 SDK 设置参数。

```java
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mOrientationListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                int rotation = getScreenRotation(orientation);
                if (!mSectionProgressBar.isRecorded() && !mSectionBegan) {
                    mVideoEncodeSetting.setRotationInMetadata(rotation);
                    if (null != mFURenderer && deviceRotation != rotation) {
                        mFURenderer.onDeviceOrientationChanged((rotation + 180) % 360);
                        deviceRotation = rotation;
                    }
                }
            }
        };
        if (mOrientationListener.canDetectOrientation()) {
            mOrientationListener.enable();
        }
    }
   
   @Override
    protected void onDestroy() {
        super.onDestroy();
        mShortVideoRecorder.destroy();
        mOrientationListener.disable();
    }
```

### 三、接口介绍

- IFURenderer 是核心接口，提供了创建、销毁、渲染等接口。使用时通过 FURenderer.Builder 创建合适的 FURenderer 实例即可。
- IModuleManager 是模块管理接口，用于创建和销毁各个功能模块，FURenderer 是实现类。
- IFaceBeautyModule 是美颜模块的接口，用于调整美颜参数。使用时通过 FURenderer 拿到 FaceBeautyModule 实例，调用里面的接口方法即可。
- IStickerModule 是贴纸模块的接口，用于加载贴纸效果。使用时通过 FURenderer 拿到 StickerModule 实例，调用里面的接口方法即可。
- IMakeModule 是美妆模块的接口，用于加载美妆效果。使用时通过 FURenderer 拿到 MakeupModule 实例，调用里面的接口方法即可。
- IBodySlimModule 是美体模块的接口，用于调整美体参数。使用时通过 FURenderer 拿到 BodySlimModule 实例，调用里面的接口方法即可。

**关于 FaceUnity SDK 的更多详细说明，请参看 [FULiveDemoDroid](https://github.com/Faceunity/FULiveDemoDroid/)**。