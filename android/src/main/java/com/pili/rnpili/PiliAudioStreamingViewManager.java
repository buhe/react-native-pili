package com.pili.rnpili;

import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.pili.rnpili.support.Config;
import com.pili.rnpili.support.RotateLayout;
import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.dns.IResolver;
import com.qiniu.android.dns.NetworkInfo;
import com.qiniu.android.dns.http.DnspodFree;
import com.qiniu.android.dns.local.AndroidDnsServer;
import com.qiniu.android.dns.local.Resolver;
import com.qiniu.pili.droid.streaming.AVCodecType;
import com.qiniu.pili.droid.streaming.CameraStreamingSetting;
import com.qiniu.pili.droid.streaming.CameraStreamingSetting.CAMERA_FACING_ID;
import com.qiniu.pili.droid.streaming.MediaStreamingManager;
import com.qiniu.pili.droid.streaming.MicrophoneStreamingSetting;
import com.qiniu.pili.droid.streaming.StreamingEnv;
import com.qiniu.pili.droid.streaming.StreamingProfile;
import com.qiniu.pili.droid.streaming.StreamingSessionListener;
import com.qiniu.pili.droid.streaming.StreamingState;
import com.qiniu.pili.droid.streaming.StreamingStateChangedListener;
import com.qiniu.pili.droid.streaming.widget.AspectFrameLayout;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Created by buhe on 16/4/29.
 */
public class PiliAudioStreamingViewManager extends SimpleViewManager<View>
        implements
//        CameraPreviewFrameView.Listener,
        StreamingSessionListener,
        StreamingStateChangedListener,
        LifecycleEventListener


{
    public enum Events {
        READY("onReady"),
        CONNECTING("onConnecting"),
        STREAMING("onStreaming"),
        SHUTDOWN("onShutdown"),
        IOERROR("onIOError"),
        DISCONNECTED("onDisconnected");

        private final String mName;

        Events(final String name) {
            mName = name;
        }

        @Override
        public String toString() {
            return mName;
        }
    }

    private static final String TAG = "PiliStreamingView";
    protected static final int MSG_START_STREAMING = 0;
    protected static final int MSG_STOP_STREAMING = 1;
    private static final int MSG_SET_ZOOM = 2;
    private static final int MSG_MUTE = 3;
    private static final int ZOOM_MINIMUM_WAIT_MILLIS = 33; //ms

    protected MediaStreamingManager mMediaStreamingManager;
    protected boolean mIsReady = false;

    private int mCurrentZoom = 0;
    private int mMaxZoom = 0;
    private StreamingProfile mProfile;
//    private CameraStreamingSetting setting;
//    private MicrophoneStreamingSetting microphoneSetting;
    private ThemedReactContext context;
    private RotateLayout mRotateLayout;
    private CameraPreviewFrameView previewFrameView;
    private View piliStreamPreview;
    private boolean focus = false;
    private boolean started = false;//default start attach on parent view
    private RCTEventEmitter mEventEmitter;


    private void initializeStreamingSessionIfNeeded(View view) {
        if (mMediaStreamingManager == null) {
            mMediaStreamingManager = new MediaStreamingManager(
                    context,
                    AVCodecType.SW_AUDIO_CODEC);  // soft codec
            mProfile = new StreamingProfile();
            mProfile
                    .setAudioQuality(StreamingProfile.AUDIO_QUALITY_LOW1)
                    .setEncoderRCMode(StreamingProfile.EncoderRCModes.QUALITY_PRIORITY)
                    .setDnsManager(getMyDnsManager())
                    .setStreamStatusConfig(new StreamingProfile.StreamStatusConfig(3))
                    .setSendingBufferProfile(new StreamingProfile.SendingBufferProfile(0.2f, 0.8f, 3.0f, 20 * 1000))
            ;

            mMediaStreamingManager.setStreamingStateListener(this);
            mMediaStreamingManager.setStreamingSessionListener(this);
            mMediaStreamingManager.prepare(mProfile);

            context.addLifecycleEventListener(this);

        }
    }

    @Override
    @Nullable
    public Map getExportedCustomDirectEventTypeConstants() {
        MapBuilder.Builder builder = MapBuilder.builder();
        for (Events event : Events.values()) {
            builder.put(event.toString(), MapBuilder.of("registrationName", event.toString()));
        }
        return builder.build();
    }

    @Override
    public View createViewInstance(ThemedReactContext context) {
        this.context = context;
        StreamingEnv.init(context.getApplicationContext());
        mEventEmitter = context.getJSModule(RCTEventEmitter.class);

        piliStreamPreview = new View(context);

        initializeStreamingSessionIfNeeded(piliStreamPreview);

        piliStreamPreview.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                mMediaStreamingManager.resume();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                mHandler.removeCallbacksAndMessages(null);
                mMediaStreamingManager.pause();
                mMediaStreamingManager.stopStreaming();
                mMediaStreamingManager.destroy();
                mMediaStreamingManager = null;
            }
        });

        return piliStreamPreview;
    }

    @Override
    /**
     * <Streaming />
     */
    public String getName() {
        return "RCTAudioStreaming";
    }

    @ReactProp(name = "rtmpURL")
    public void setRtmpURL(View view, @Nullable String rtmpURL) {
        try {
            mProfile.setPublishUrl(rtmpURL);
        } catch (URISyntaxException e) {
            Log.e(TAG, "RTMP URL is syntax error.");
        }
        mMediaStreamingManager.setStreamingProfile(mProfile);
    }

    @ReactProp(name = "profile")
    public void setProfile(View view, @Nullable ReadableMap profile) {
        ReadableMap audio = profile.getMap("audio");

        StreamingProfile.VideoProfile vProfile =
                new StreamingProfile.VideoProfile(0,0,0);//hack for bad API

        StreamingProfile.AudioProfile aProfile =
                new StreamingProfile.AudioProfile(audio.getInt("rate"), audio.getInt("bitrate")); //audio sample rate, audio bitrate
        StreamingProfile.AVProfile avProfile = new StreamingProfile.AVProfile(vProfile, aProfile);
        mProfile.setAVProfile(avProfile);
        mMediaStreamingManager.setStreamingProfile(mProfile);

    }

    @ReactProp(name = "muted")
    public void setMuted(View view, boolean muted) {
        mMediaStreamingManager.mute(muted);
    }


    @ReactProp(name = "started")
    public void setStarted(View view, boolean started) {
        if(this.started == started){
            //ignore
            return;
        }
        this.started = started;
        if (mIsReady) {  //没有准备好则只赋值,等待onStateChanged 唤起
            if (started) {
                startStreaming();
            } else {
                stopStreaming();
            }
        }
    }


    public int getTargetId() {
        return piliStreamPreview.getId();
    }

    @Override
    public void onStateChanged(StreamingState state, Object extra) {
        switch (state) {
            case PREPARING:
                break;
            case READY:
                mIsReady = true;
                mMaxZoom = mMediaStreamingManager.getMaxZoom();
                if (started) {
                    startStreaming();
                }
                mEventEmitter.receiveEvent(getTargetId(), Events.READY.toString(), Arguments.createMap());
                break;
            case CONNECTING:
                mEventEmitter.receiveEvent(getTargetId(), Events.CONNECTING.toString(), Arguments.createMap());
                break;
            case STREAMING:
                mEventEmitter.receiveEvent(getTargetId(), Events.STREAMING.toString(), Arguments.createMap());
                break;
            case SHUTDOWN:
                mEventEmitter.receiveEvent(getTargetId(), Events.SHUTDOWN.toString(), Arguments.createMap());
                break;
            case IOERROR:
                mEventEmitter.receiveEvent(getTargetId(), Events.IOERROR.toString(), Arguments.createMap());
                break;
            case UNKNOWN:
                break;
            case SENDING_BUFFER_EMPTY:
                break;
            case SENDING_BUFFER_FULL:
                break;
            case AUDIO_RECORDING_FAIL:
                break;
            case OPEN_CAMERA_FAIL:
                break;
            case DISCONNECTED:
                mEventEmitter.receiveEvent(getTargetId(), Events.DISCONNECTED.toString(), Arguments.createMap());
                break;
            case CAMERA_SWITCHED:
                if (extra != null) {
                    Log.i(TAG, "current camera id:" + (Integer) extra);
                }
                Log.i(TAG, "camera switched");
                break;
            case TORCH_INFO:
                if (extra != null) {
                    final boolean isSupportedTorch = (Boolean) extra;
                    Log.i(TAG, "isSupportedTorch=" + isSupportedTorch);
//                    this.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            if (isSupportedTorch) {
//                                mTorchBtn.setVisibility(View.VISIBLE);
//                            } else {
//                                mTorchBtn.setVisibility(View.GONE);
//                            }
//                        }
//                    });
                }
                break;
        }
    }


    @Override
    public boolean onRecordAudioFailedHandled(int err) {
        mMediaStreamingManager.updateEncodingType(AVCodecType.SW_VIDEO_CODEC);
        mMediaStreamingManager.startStreaming();
        return true;
    }

    @Override
    public boolean onRestartStreamingHandled(int err) {
        Log.i(TAG, "onRestartStreamingHandled");
        return mMediaStreamingManager.startStreaming();
    }

    @Override
    public Camera.Size onPreviewSizeSelected(List<Camera.Size> list) {
        Camera.Size size = null;
//        if (list != null) {
//            for (Camera.Size s : list) {
//                Log.i(TAG, "w:" + s.width + ", h:" + s.height);
//            }
//        }
//        Log.e(TAG, "selected size :" + size.width + "x" + size.height);
        return size;
    }

    @Override
    public void onHostResume() {
        mMediaStreamingManager.resume();
    }

    @Override
    public void onHostPause() {
        mHandler.removeCallbacksAndMessages(null);
        mMediaStreamingManager.pause();

    }

    @Override
    public void onHostDestroy() {
        mMediaStreamingManager.destroy();
    }


    protected Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_STREAMING:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            boolean res = mMediaStreamingManager.startStreaming();
                            Log.i(TAG, "res:" + res);
                        }
                    }).start();
                    break;
                case MSG_STOP_STREAMING:
                    boolean res = mMediaStreamingManager.stopStreaming();
                    break;
                case MSG_SET_ZOOM:
                    mMediaStreamingManager.setZoomValue(mCurrentZoom);
                    break;
                default:
                    Log.e(TAG, "Invalid message");
            }
        }
    };

    private void startStreaming() {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_START_STREAMING), 50);
    }

    private void stopStreaming() {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_STOP_STREAMING), 50);
    }

    private DnsManager getMyDnsManager() {
        IResolver r0 = new DnspodFree();
        IResolver r1 = AndroidDnsServer.defaultResolver();
        IResolver r2 = null;
        try {
            r2 = new Resolver(InetAddress.getByName("119.29.29.29"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return new DnsManager(NetworkInfo.normal, new IResolver[]{r0, r1, r2});
    }
}
