# 照片视频选择库

本库是结合目前在做的项目需求对[PhotoPicker](https://github.com/donglua/PhotoPicker)做的二次开发，可能不满足你的业务需求，请慎用！！！

## 用法
```Java
      PhotoPicker.configIpAddress("111","222");
            PhotoPicker.builder(this)
                    .modeType(PhotoPicker.ALL) //三种模式
                    .choiceVideoNumber(1) //视频选择最大数量，默认9
                    .choicePhotoNumber(9) //图片选择最大数量，默认9
                    .setSupportShare(true) //是否支持分享，默认不支持
                    .setIsTouPing(false) //是否是投屏，默认是完成
                    .setIsNeedPicEdit(false) //是否支持裁剪
                    .videoSaveDirectory(Environment.getExternalStorageDirectory().getPath()) //指定视频存储文件夹
                    .build(new OnResultListener() {

                        @Override
                        public void onPhotoResult(ArrayList<String> photos) {

                        }

                        @Override
                        public void onVideoResult(ArrayList<String> videos) {

                        }

                        @Override
                        public void onPhotoShareResult(ArrayList<String> files) {
                        }

                        @Override
                        public void onVideoShareResult(ArrayList<String> files) {

                        }

                    });
        }
```

## 引入方式
```
   implementation 'com.whj.picker:PhotoVideoPicker:2.0.4'

```
如果无法引用，在app目录build.gradle添加
```
   repositories {
       maven { url "https://dl.bintray.com/whj/Maven" }
   }

```
如果编译时和其他依赖库的support有冲突，在app目录build.gradle添加
```
  //解决依赖库版本不一致
  configurations.all {
          resolutionStrategy.eachDependency { DependencyResolveDetails details ->
              def requested = details.requested
              if (requested.group == 'com.android.support') {
                  if (!requested.name.startsWith("multidex")) {
                      //这里指定需要统一的依赖版本,这里统一为26.1.0
                      details.useVersion '26.1.0'
                  }
              }
          }
      }

```
摄像头直播单独配置ip和端口号
```
   //配置摄像头直播的ip和端口号
   PhotoPicker.configIpAddress(ZxingModelUtils.getInstance().getIp(),ZxingModelUtils.getInstance().getUDPPort());


```


## License
```
 Copyright 2019, WHuaJian

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```
