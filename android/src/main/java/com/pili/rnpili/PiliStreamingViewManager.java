package com.pili.rnpili;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
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
import com.pili.pldroid.streaming.CameraStreamingManager;
import com.pili.pldroid.streaming.CameraStreamingSetting;
import com.pili.pldroid.streaming.MicrophoneStreamingSetting;
import com.pili.pldroid.streaming.StreamingProfile;
import com.pili.pldroid.streaming.widget.AspectFrameLayout;
import com.pili.rnpili.support.Config;
import com.pili.rnpili.support.FocusIndicatorRotateLayout;
import com.pili.rnpili.support.Jsons;
import com.pili.rnpili.support.RotateLayout;
import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.dns.IResolver;
import com.qiniu.android.dns.NetworkInfo;
import com.qiniu.android.dns.http.DnspodFree;
import com.qiniu.android.dns.local.AndroidDnsServer;
import com.qiniu.android.dns.local.Resolver;

import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Created by buhe on 16/4/29.
 */
public class PiliStreamingViewManager extends SimpleViewManager<AspectFrameLayout>
        implements
        CameraPreviewFrameView.Listener,
        CameraStreamingManager.StreamingSessionListener,
        CameraStreamingManager.StreamingStateListener,
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

    protected CameraStreamingManager mCameraStreamingManager;
    protected boolean mIsReady = false;

    private int mCurrentZoom = 0;
    private int mMaxZoom = 0;
    private StreamingProfile mProfile;
    private CameraStreamingSetting setting;
    private MicrophoneStreamingSetting microphoneSetting;
    private ThemedReactContext context;
    private RotateLayout mRotateLayout;
    private CameraPreviewFrameView previewFrameView;
    private AspectFrameLayout piliStreamPreview;
    private boolean focus = false;
    private boolean started = true;//default start attach on parent view
    private RCTEventEmitter mEventEmitter;


    private void initializeStreamingSessionIfNeeded(AspectFrameLayout afl, CameraPreviewFrameView previewFrameView) {
        if (mCameraStreamingManager == null) {
            mCameraStreamingManager = new CameraStreamingManager(
                    context,
                    afl,
                    previewFrameView,
                    CameraStreamingManager.EncodingType.SW_VIDEO_WITH_SW_AUDIO_CODEC);  // soft codec
            mProfile = new StreamingProfile();
            StreamingProfile.AudioProfile aProfile = new StreamingProfile.AudioProfile(44100, 96 * 1024); //audio sample rate, audio bitrate
            StreamingProfile.VideoProfile vProfile = new StreamingProfile.VideoProfile(30, 1000 * 1024, 48);//fps bps maxFrameInterval
            StreamingProfile.AVProfile avProfile = new StreamingProfile.AVProfile(vProfile, aProfile);
            mProfile.setVideoQuality(StreamingProfile.VIDEO_QUALITY_HIGH3)
                    .setAudioQuality(StreamingProfile.AUDIO_QUALITY_MEDIUM2)
//                .setPreferredVideoEncodingSize(960, 544)
                    .setEncodingSizeLevel(Config.ENCODING_LEVEL)
                    .setEncoderRCMode(StreamingProfile.EncoderRCModes.QUALITY_PRIORITY)
//                    .setStream(stream)   //set Stream
                    .setAVProfile(avProfile)
                    .setDnsManager(getMyDnsManager())
                    .setStreamStatusConfig(new StreamingProfile.StreamStatusConfig(3))
//                .setEncodingOrientation(StreamingProfile.ENCODING_ORIENTATION.PORT)
                    .setSendingBufferProfile(new StreamingProfile.SendingBufferProfile(0.2f, 0.8f, 3.0f, 20 * 1000));

            setting = new CameraStreamingSetting();
            setting.setCameraId(Camera.CameraInfo.CAMERA_FACING_BACK)
                    .setContinuousFocusModeEnabled(true)
                    .setRecordingHint(false)
                    .setResetTouchFocusDelayInMs(3000)
                    .setFocusMode(CameraStreamingSetting.FOCUS_MODE_CONTINUOUS_PICTURE)
                    .setCameraPrvSizeLevel(CameraStreamingSetting.PREVIEW_SIZE_LEVEL.MEDIUM)
                    .setCameraPrvSizeRatio(CameraStreamingSetting.PREVIEW_SIZE_RATIO.RATIO_16_9);

            microphoneSetting = new MicrophoneStreamingSetting();
            microphoneSetting.setBluetoothSCOEnabled(false);

            mCameraStreamingManager.prepare(setting, microphoneSetting, mProfile);
            mCameraStreamingManager.setStreamingStateListener(this);
            mCameraStreamingManager.setStreamingSessionListener(this);
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
    public AspectFrameLayout createViewInstance(ThemedReactContext context) {
        this.context = context;
        mEventEmitter = context.getJSModule(RCTEventEmitter.class);

        piliStreamPreview = new AspectFrameLayout(context);

        piliStreamPreview.setShowMode(AspectFrameLayout.SHOW_MODE.REAL);

        previewFrameView = new CameraPreviewFrameView(context);
        previewFrameView.setListener(this);
        previewFrameView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        piliStreamPreview.addView(previewFrameView);
        initializeStreamingSessionIfNeeded(piliStreamPreview, previewFrameView);

        piliStreamPreview.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                mCameraStreamingManager.resume();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                mCameraStreamingManager.destroy();
            }
        });

        return piliStreamPreview;
    }

    @Override
    /**
     * <Streaming />
     */
    public String getName() {
        return "RCTStreaming";
    }

    @ReactProp(name = "stream")
    public void setStream(AspectFrameLayout view, @Nullable ReadableMap stream) {
        mProfile.setStream(new StreamingProfile.Stream(Jsons.readableMapToJson(stream)));
        mCameraStreamingManager.setStreamingProfile(mProfile);
    }

    @ReactProp(name = "profile")
    public void setProfile(AspectFrameLayout view, @Nullable ReadableMap profile) {
        ReadableMap video = profile.getMap("video");
        ReadableMap audio = profile.getMap("audio");
        int encodingSize = profile.getInt("encodingSize");

        StreamingProfile.AudioProfile aProfile =
                new StreamingProfile.AudioProfile(audio.getInt("rate"), audio.getInt("bitrate")); //audio sample rate, audio bitrate
        StreamingProfile.VideoProfile vProfile =
                new StreamingProfile.VideoProfile(video.getInt("fps"), video.getInt("bps"), video.getInt("maxFrameInterval"));//fps bps maxFrameInterval
        StreamingProfile.AVProfile avProfile = new StreamingProfile.AVProfile(vProfile, aProfile);
        mProfile.setAVProfile(avProfile);
        mProfile.setEncodingSizeLevel(encodingSize);
        mCameraStreamingManager.setStreamingProfile(mProfile);

    }

    @ReactProp(name = "muted")
    public void setMuted(AspectFrameLayout view, boolean muted) {
        mCameraStreamingManager.mute(muted);
    }

    @ReactProp(name = "zoom")
    public void setZoom(AspectFrameLayout view, int zoom) {
        mCurrentZoom = zoom;
        mCurrentZoom = Math.min(mCurrentZoom, mMaxZoom);
        mCurrentZoom = Math.max(0, mCurrentZoom);
        mCameraStreamingManager.setZoomValue(zoom);
    }

    @ReactProp(name = "focus")
    public void setFocus(AspectFrameLayout view, boolean focus) {
        this.focus = focus;
    }

    @ReactProp(name = "started")
    public void setStarted(AspectFrameLayout view, boolean started) {
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

    protected void setFocusAreaIndicator() {
        if (mRotateLayout == null) {
            mRotateLayout = new FocusIndicatorRotateLayout(context, null);
            mRotateLayout
                    .setLayoutParams(new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            Gravity.CENTER
                    ));
            View indicator = new View(context);
            indicator.setLayoutParams(new ViewGroup.LayoutParams(120, 120));
            mRotateLayout.addView(indicator);
            mRotateLayout.setChild(indicator);
            piliStreamPreview.addView(mRotateLayout);
            mCameraStreamingManager.setFocusAreaIndicator(mRotateLayout,
                    indicator);
        }
    }

    public int getTargetId() {
        return piliStreamPreview.getId();
    }

    @Override
    public void onStateChanged(int state, Object extra) {
        switch (state) {
            case CameraStreamingManager.STATE.PREPARING:
                break;
            case CameraStreamingManager.STATE.READY:
                mIsReady = true;
                mMaxZoom = mCameraStreamingManager.getMaxZoom();
                if (started) {
                    startStreaming();
                }
                mEventEmitter.receiveEvent(getTargetId(), Events.READY.toString(), Arguments.createMap());
                break;
            case CameraStreamingManager.STATE.CONNECTING:
                mEventEmitter.receiveEvent(getTargetId(), Events.CONNECTING.toString(), Arguments.createMap());
                break;
            case CameraStreamingManager.STATE.STREAMING:
                mEventEmitter.receiveEvent(getTargetId(), Events.STREAMING.toString(), Arguments.createMap());
                break;
            case CameraStreamingManager.STATE.SHUTDOWN:
                mEventEmitter.receiveEvent(getTargetId(), Events.SHUTDOWN.toString(), Arguments.createMap());
                break;
            case CameraStreamingManager.STATE.IOERROR:
                mEventEmitter.receiveEvent(getTargetId(), Events.IOERROR.toString(), Arguments.createMap());
                break;
            case CameraStreamingManager.STATE.UNKNOWN:
                break;
            case CameraStreamingManager.STATE.SENDING_BUFFER_EMPTY:
                break;
            case CameraStreamingManager.STATE.SENDING_BUFFER_FULL:
                break;
            case CameraStreamingManager.STATE.AUDIO_RECORDING_FAIL:
                break;
            case CameraStreamingManager.STATE.OPEN_CAMERA_FAIL:
                break;
            case CameraStreamingManager.STATE.DISCONNECTED:
                mEventEmitter.receiveEvent(getTargetId(), Events.DISCONNECTED.toString(), Arguments.createMap());
                break;
            case CameraStreamingManager.STATE.CAMERA_SWITCHED:
                if (extra != null) {
                    Log.i(TAG, "current camera id:" + (Integer) extra);
                }
                Log.i(TAG, "camera switched");
                break;
            case CameraStreamingManager.STATE.TORCH_INFO:
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
        mCameraStreamingManager.updateEncodingType(CameraStreamingManager.EncodingType.SW_VIDEO_CODEC);
        mCameraStreamingManager.startStreaming();
        return true;
    }

    @Override
    public boolean onRestartStreamingHandled(int err) {
        Log.i(TAG, "onRestartStreamingHandled");
        return mCameraStreamingManager.startStreaming();
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
    public boolean onSingleTapUp(MotionEvent e) {
        Log.i(TAG, "onSingleTapUp X:" + e.getX() + ",Y:" + e.getY());

        if (mIsReady && focus) {
            setFocusAreaIndicator();
            try {
                mCameraStreamingManager.doSingleTapUp((int) e.getX(), (int) e.getY());
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());
            }

            return true;
        }
        return false;
    }

    @Override
    public boolean onZoomValueChanged(float factor) {
        if (mIsReady && mCameraStreamingManager.isZoomSupported()) {
            mCurrentZoom = (int) (mMaxZoom * factor);
            mCurrentZoom = Math.min(mCurrentZoom, mMaxZoom);
            mCurrentZoom = Math.max(0, mCurrentZoom);

            Log.d(TAG, "zoom ongoing, scale: " + mCurrentZoom + ",factor:" + factor + ",maxZoom:" + mMaxZoom);
            if (!mHandler.hasMessages(MSG_SET_ZOOM)) {
                mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SET_ZOOM), ZOOM_MINIMUM_WAIT_MILLIS);
                return true;
            }
        }
        return false;
    }


    @Override
    public boolean onStateHandled(final int state, Object extra) {
        switch (state) {
            case CameraStreamingManager.STATE.SENDING_BUFFER_HAS_FEW_ITEMS:
                return false;
            case CameraStreamingManager.STATE.SENDING_BUFFER_HAS_MANY_ITEMS:
                return false;
        }
        return false;
    }


    @Override
    public void onHostResume() {
        mCameraStreamingManager.resume();
    }

    @Override
    public void onHostPause() {
        mCameraStreamingManager.pause();
    }

    @Override
    public void onHostDestroy() {
        mCameraStreamingManager.destroy();
    }


    protected Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START_STREAMING:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            boolean res = mCameraStreamingManager.startStreaming();
                            Log.i(TAG, "res:" + res);
                        }
                    }).start();
                    break;
                case MSG_STOP_STREAMING:
                    boolean res = mCameraStreamingManager.stopStreaming();
                    break;
                case MSG_SET_ZOOM:
                    mCameraStreamingManager.setZoomValue(mCurrentZoom);
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
