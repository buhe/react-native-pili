#Pili Streaming Cloud React Native SDK
##Installation
```
Run npm install react-native-pili --save
```
###iOS
1. In XCode, in the project navigator, right click Libraries ➜ Add Files to [your project's name]
2. Go to node_modules ➜ react-native-pili ➜ RCTPili and add RCTPili.xcodeproj
3. In XCode, in the project navigator, select your project. Add libPTCPili.a ,libz.tbd,libc++.tbd to your project's Build Phases ➜ Link Binary With Libraries
4. Run your project (Cmd+R)

###Android

**android/settings.gradle**
```
include ':react-native-pili'
project(':react-native-pili').projectDir = new File(settingsDir, '../node_modules/react-native-pili/android')
```
**android/app/build.gradle**
```
dependencies {
    ...
    compile project(':react-native-pili')
}
```
**MainActivity.java**

On top, where imports are:
```java
import com.pili.rnpili.PiliPackage;
```

Modify getPackages method
```java
 @Override
    protected List<ReactPackage> getPackages() {
        return Arrays.<ReactPackage>asList(
                new MainReactPackage(),
                new PiliPackage(this)
        );
    }
```


##TODO
- [x] Android Player
- [x] Android Streaming
- [x] iOS Player
- [x] iOS Streaming
- [ ] 美颜和水印支持
- [ ] 单元测试
- [ ] 自定义实时滤镜支持

##Usage
###1. 推流
```javascript
<Streaming
    stream={{ 
        id:"xxx", //pili id
        title:"title", //pili title
        hub:"hubname", //pili hub name
        publishKey:"<PK>", //pili key
        publishSecurity:"static", //pili secrity policy (static or dynamic)
        hosts:{
          publish:{ //pili Streaming url (support rtmp)
            rtmp:"pili-publish.pilitest.qiniucdn.com"
          }
        }
        }}
    style={{
        height:400,
        width:400,
    }}
    zoom={1} //zoom 
    muted={true} //muted
    focus={false} //focus
    profile={{  //video and audio profile
       video:{
         fps:30,
         bps:1000 * 1024,
         maxFrameInterval:48
       },
       audio:{
         rate:44100,
         bitrate:96 * 1024
       },
    started={false} //streaming status
    onReady={()=>{}} //onReady event
    onConnecting={()=>{}} //onConnecting event
    onStreaming={()=>{}} //onStreaming event
    onShutdown={()=>{}} //onShutdown event
    onIOError={()=>{}} //onIOError event
    onDisconnected={()=>{}} //onDisconnected event
    />
```
###2. 直播播放
```javascript
<Player
  source={{
    uri:"rtmp://pili-live-rtmp.pilitest.qiniucdn.com/pilitest/xxx",
    controller: true, //Controller ui  Android only
    timeout: 10 * 1000, //live streaming timeout (ms) Android only
    live:true, //live streaming ? Android only
    hardCodec:false, //hard codec [recommended false]  Android only
    }}
    started={true} //iOS only
    muted={false} //iOS only
    style={{
      height:200,
      width:200,
    }}
    onLoading={()=>{}} //loading from remote or local
    onPaused={()=>{}} //pause event
    onShutdown={()=>{}} //stopped event
    onError={()=>{}} //error event
    onPlaying={()=>{}} //play event
    />
```
##Release Note
##0.1.0
- [x] Android Player
- [x] Android Streaming
- [x] iOS Player
- [x] iOS Streaming 
实现了所有基本 API ,处于可以尝试使用阶段，后续会用实际项目推动 SDK 完善