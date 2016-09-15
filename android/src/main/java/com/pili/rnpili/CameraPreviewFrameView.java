package com.pili.rnpili;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.qiniu.android.dns.DnsManager;
import com.qiniu.android.dns.IResolver;
import com.qiniu.android.dns.NetworkInfo;
import com.qiniu.android.dns.http.DnspodFree;
import com.qiniu.android.dns.local.AndroidDnsServer;
import com.qiniu.android.dns.local.Resolver;

import java.io.IOException;
import java.net.InetAddress;

/**
 * Created by buhe on 16/4/29.
 * A Pili Streaming View
 */
public class CameraPreviewFrameView extends GLSurfaceView {

    private static final String TAG = "CameraPreviewFrameView";

    public interface Listener {
        boolean onSingleTapUp(MotionEvent e);
        boolean onZoomValueChanged(float factor);
    }

    private Listener mListener;
    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetector;

    public CameraPreviewFrameView(Context context) {
        super(context);
        initialize(context);
    }

    public CameraPreviewFrameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mGestureDetector.onTouchEvent(event)) {
            return mScaleDetector.onTouchEvent(event);
        }
        return false;
    }

    private GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (mListener != null) {
                mListener.onSingleTapUp(e);
            }
            return false;
        }
    };

    private ScaleGestureDetector.SimpleOnScaleGestureListener mScaleListener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {

        private float mScaleFactor = 1.0f;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            // factor > 1, zoom
            // factor < 1, pinch
            mScaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.01f, Math.min(mScaleFactor, 1.0f));

            return mListener != null && mListener.onZoomValueChanged(mScaleFactor);
        }
    };

    private void initialize(Context context) {
        Log.i(TAG, "initialize");
        mScaleDetector = new ScaleGestureDetector(context, mScaleListener);
        mGestureDetector = new GestureDetector(context, mGestureListener);
    }
}
