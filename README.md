#Pili Streaming Cloud React Native SDK

##Introduction

###Warning

This lib dependency  PLMediaStreamingKit (2.1.1) and PLPlayerKit (2.2.4) , the latest native sdk break origin API, i will match the latest version be free..

这*可能*是**第一个**在 React Native 上实现全功能的直播 SDK 了，底层基于 [Pili-SDK](https://github.com/pili-engineering)，把 iOS 和 Android 的 API 尽量统一。

2.0 版本为了更容易集成第三方 SDK ，对原有的 React Native 项目进行了改造，iOS 的依赖采用了 Cocoapod 进行管理，当然你也可以采用原来的方式，毕竟是可以共存的。具体可以参考 [AirApps](https://github.com/airapps/airapps) 可以查看如何进行集成。项目的简单的例子是 [react-native-pili](https://github.com/airapps/react-native-living) 

This may be the **first** React Native to achieve full-featured live SDK, the bottom based on [Pili-SDK] (https://github.com/pili-engineering), the iOS and Android API as unified as possible.

2.0 version In order to more easily integrate third-party SDK, the original React Native project has been modified, iOS rely on the use of Cocoapod management, of course, you can also use the original way, after all, can coexist. Specifically, you can see how to integrate with AirApps (https://github.com/airapps/airapps). A simple example of a project is [react-native-pili] (https://github.com/airapps/react-native-living) 


##Installation

```bash
git clone https://github.com/buhe/pili-startkit YourProjectName

cd YourProjectName/js && npm install

cd ../ios && pod install
```

###Javascript

```bash
cd YourProjectName/js
npm start
```

###iOS
1. Open ios/YourProjectName.xcworkspace (这里请注意是打开 .xcworkspace!请确认) (Please note that it opens .xcworkspace! Confirm it does)
2. Just run your project (Cmd+R)
3. 如果是 iOS 10 需要在 info 中额外添加如下权限: (If it is iOS 10 you need to add the following additional auth in info:)
```
    <key>NSCameraUsageDescription</key>    
    <string>cameraDesciption</string>

    <key>NSContactsUsageDescription</key>    
    <string>contactsDesciption</string>

    <key>NSMicrophoneUsageDescription</key>    
    <string>microphoneDesciption</string>
```    
ref: [iOS 10](http://www.jianshu.com/p/c212cde86877)


###Android
1. Open android use Android Studio
2. Just run your project

##TODO
- [x] Android Player
- [x] Android Streaming
- [x] iOS Player
- [x] iOS Streaming
- [ ] 美颜和水印支持 (Design and watermark support)

##Usage
###1. 推流 (Streaming)
```javascript
<Streaming
    rtmpURL={"rtmp://pili-publish.pilitest.qiniucdn.com/pilitest/demo_test?key=6eeee8a82246636e"}
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
###2. 直播播放 (Live player)
```javascript
<Player
  source={{
    uri:"rtmp://pili-live-rtmp.pilitest.qiniucdn.com/pilitest/xxx",
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
##2.1.1
- [x] Android Player
- [x] Android Streaming
- [x] iOS Player
- [x] iOS Streaming 
