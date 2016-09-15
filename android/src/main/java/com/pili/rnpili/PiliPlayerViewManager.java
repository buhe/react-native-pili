package com.pili.rnpili;

import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.pili.pldroid.player.AVOptions;
import com.pili.pldroid.player.PLMediaPlayer;
import com.pili.pldroid.player.widget.PLVideoView;
import com.pili.rnpili.support.MediaController;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * Created by buhe on 16/4/29.
 */
public class PiliPlayerViewManager extends SimpleViewManager<PLVideoView> implements LifecycleEventListener {
    private ThemedReactContext reactContext;
    private static final String TAG = PiliPlayerViewManager.class.getSimpleName();
    private PLVideoView mVideoView;
    private RCTEventEmitter mEventEmitter;

    private static final int MEDIA_INFO_UNKNOWN = 1;
    private static final int MEDIA_INFO_VIDEO_RENDERING_START = 3;
    private static final int MEDIA_INFO_BUFFERING_START = 701;
    private static final int MEDIA_INFO_BUFFERING_END = 702;
    private static final int MEDIA_INFO_AUDIO_RENDERING_START = 10002;
    private boolean started;
    private int aspectRatio;

    public enum Events {
        LOADING("onLoading"),
        PAUSE("onPaused"),
        SHUTDOWN("onShutdown"),
        ERROR("onError"),
        PLAYING("onPlaying");

        private final String mName;

        Events(final String name) {
            mName = name;
        }

        @Override
        public String toString() {
            return mName;
        }
    }

    @Override
    public String getName() {
        return "RCTPlayer";
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
    protected PLVideoView createViewInstance(ThemedReactContext reactContext) {
        this.reactContext = reactContext;
        mEventEmitter = reactContext.getJSModule(RCTEventEmitter.class);
        mVideoView = new PLVideoView(reactContext);
        // Set some listeners
        mVideoView.setOnPreparedListener(mOnPreparedListener);
        mVideoView.setOnInfoListener(mOnInfoListener);
        mVideoView.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
        mVideoView.setOnBufferingUpdateListener(mOnBufferingUpdateListener);
        mVideoView.setOnCompletionListener(mOnCompletionListener);
        mVideoView.setOnSeekCompleteListener(mOnSeekCompleteListener);
        mVideoView.setOnErrorListener(mOnErrorListener);

        reactContext.addLifecycleEventListener(this);
        return mVideoView;
    }

    private boolean isLiveStreaming(String url) {
        if (url.startsWith("rtmp://")
                || (url.startsWith("http://") && url.endsWith(".m3u8"))
                || (url.startsWith("http://") && url.endsWith(".flv"))) {
            return true;
        }
        return false;
    }

    @ReactProp(name = "source")
    public void setSource(PLVideoView mVideoView, ReadableMap source) {
        AVOptions options = new AVOptions();
        String uri = source.getString("uri");
        boolean mediaController = source.hasKey("controller") && source.getBoolean("controller");
        int avFrameTimeout = source.hasKey("timeout") ? source.getInt("timeout") : -1;        //10 * 1000 ms
        boolean liveStreaming = source.hasKey("live") && source.getBoolean("live");  //1 or 0 // 1 -> live
        boolean codec = source.hasKey("hardCodec") && source.getBoolean("hardCodec");  //1 or 0  // 1 -> hw codec enable, 0 -> disable [recommended]
        // the unit of timeout is ms
        if (avFrameTimeout >= 0) {
            options.setInteger(AVOptions.KEY_GET_AV_FRAME_TIMEOUT, avFrameTimeout);
        }
        // Some optimization with buffering mechanism when be set to 1
        if (liveStreaming) {
            options.setInteger(AVOptions.KEY_LIVE_STREAMING, 1);
        } else {
            options.setInteger(AVOptions.KEY_LIVE_STREAMING, 0);
        }
//        }

        // 1 -> hw codec enable, 0 -> disable [recommended]
        if (codec) {
            options.setInteger(AVOptions.KEY_MEDIACODEC, 1);
        } else {
            options.setInteger(AVOptions.KEY_MEDIACODEC, 0);
        }

        mVideoView.setAVOptions(options);

        // After setVideoPath, the play will start automatically
        // mVideoView.start() is not required

        mVideoView.setVideoPath(uri);

        if (mediaController) {
            // You can also use a custom `MediaController` widget
            MediaController mMediaController = new MediaController(reactContext, false, isLiveStreaming(uri));
            mVideoView.setMediaController(mMediaController);
        }

    }

    @ReactProp(name = "aspectRatio")
    public void setAspectRatio(PLVideoView mVideoView, int aspectRatio) {
        /**
         *  ASPECT_RATIO_ORIGIN = 0;
         *  ASPECT_RATIO_FIT_PARENT = 1
         *  ASPECT_RATIO_PAVED_PARENT = 2
         *  ASPECT_RATIO_16_9 = 3
         *  ASPECT_RATIO_4_3 = 4
         */
        this.aspectRatio = aspectRatio;
        mVideoView.setDisplayAspectRatio(aspectRatio);
    }

    @ReactProp(name = "started")
    public void setStarted(PLVideoView mVideoView,  boolean started) {
        this.started = started;
        if (started) {
            mVideoView.start();
        } else {
            mVideoView.pause();
            mEventEmitter.receiveEvent(getTargetId(), Events.PAUSE.toString(), Arguments.createMap());
        }
    }

    @ReactProp(name = "muted")
    public void setMuted(PLVideoView mVideoView, boolean muted){
//        mVideoView.mute
        //Android not implements
    }

    private PLMediaPlayer.OnPreparedListener mOnPreparedListener = new PLMediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(PLMediaPlayer plMediaPlayer) {
            Log.d(TAG, "onPrepared ! ");
            mEventEmitter.receiveEvent(getTargetId(), Events.LOADING.toString(), Arguments.createMap());
        }
    };

    private PLMediaPlayer.OnInfoListener mOnInfoListener = new PLMediaPlayer.OnInfoListener() {
        @Override
        public boolean onInfo(PLMediaPlayer plMediaPlayer, int what, int extra) {
            Log.d(TAG, "onInfo: " + what + ", " + extra);

            switch (what) {
                case MEDIA_INFO_VIDEO_RENDERING_START:
                    mEventEmitter.receiveEvent(getTargetId(), Events.PLAYING.toString(), Arguments.createMap());
                    break;
                case MEDIA_INFO_BUFFERING_START:
                    mEventEmitter.receiveEvent(getTargetId(), Events.LOADING.toString(), Arguments.createMap());
                    break;
                case MEDIA_INFO_BUFFERING_END:
                    mEventEmitter.receiveEvent(getTargetId(), Events.PLAYING.toString(), Arguments.createMap());
                    break;
            }
            return true;
        }
    };

    private PLMediaPlayer.OnErrorListener mOnErrorListener = new PLMediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(PLMediaPlayer plMediaPlayer, int errorCode) {
            Log.e(TAG, "Error happened, errorCode = " + errorCode);
            WritableMap event = Arguments.createMap();
            event.putInt("errorCode",errorCode);
            mEventEmitter.receiveEvent(getTargetId(), Events.ERROR.toString(), Arguments.createMap());
            return true;
        }
    };

    private PLMediaPlayer.OnCompletionListener mOnCompletionListener = new PLMediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(PLMediaPlayer plMediaPlayer) {
            Log.d(TAG, "Play Completed !");
            mEventEmitter.receiveEvent(getTargetId(), Events.SHUTDOWN.toString(), Arguments.createMap());
        }
    };

    private PLMediaPlayer.OnBufferingUpdateListener mOnBufferingUpdateListener = new PLMediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(PLMediaPlayer plMediaPlayer, int precent) {
            Log.d(TAG, "onBufferingUpdate: " + precent);
        }
    };

    private PLMediaPlayer.OnSeekCompleteListener mOnSeekCompleteListener = new PLMediaPlayer.OnSeekCompleteListener() {
        @Override
        public void onSeekComplete(PLMediaPlayer plMediaPlayer) {
            Log.d(TAG, "onSeekComplete !");
        }

        ;
    };

    private PLMediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener = new PLMediaPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(PLMediaPlayer plMediaPlayer, int width, int height) {
            Log.d(TAG, "onVideoSizeChanged: " + width + "," + height);
        }
    };

    @Override
    public void onHostResume() {
        mVideoView.start();
    }

    @Override
    public void onHostPause() {
        mVideoView.pause();
    }

    @Override
    public void onHostDestroy() {
        mVideoView.stopPlayback();
    }

    public int getTargetId() {
        return mVideoView.getId();
    }
}
