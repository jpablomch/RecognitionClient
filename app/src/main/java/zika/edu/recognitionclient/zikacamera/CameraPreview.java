package zika.edu.recognitionclient.zikacamera;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
 * The camera preview that will be used as the display for the custom camera
 *
 * Currently supports tap to focus and 2 finger zoom
 */
@SuppressWarnings("deprecation")
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private Camera mCamera;
    private SurfaceHolder mHolder;
    private Camera.Parameters mCamParams;
    private float mDist;

    public CameraPreview(Context ctx, Camera camera) {
        super(ctx);
        mCamera = camera;
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.setDisplayOrientation(90);
            mCamera.startPreview();
        } catch(IOException ioe){
            Log.d("aaa", "Error setting up camera preview 1 " + ioe.toString());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(mHolder.getSurface() == null){
            return;
        }

        try {
            mCamera.stopPreview();
        } catch(Exception e){
            Log.d("aaa", "Tried to stop non existent preview");
        }

        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch(IOException ioe){
            Log.d("aaa", "Error setting up camera preview 2 " + ioe.toString());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if(mCamera != null){
            mCamera.stopPreview();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();

        if(event.getPointerCount() > 1) {
            if(event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
                mDist = getFingerDistance(event);
            } else if (action == MotionEvent.ACTION_MOVE && mCamParams.isZoomSupported()) {
                handleZoom(event);
            }
        } else if(action == MotionEvent.ACTION_DOWN){
            handleFocus(event);
        }
        return true;
    }

    /*
     * Handles the tap to focus feature
     *
     * @param event - information about the pointer that the user tapped
     */
    private void handleFocus(MotionEvent event) {
        mCamParams = mCamera.getParameters();
        int x = (int) event.getX();
        int y = (int) event.getY();
        Rect rect = new Rect(x - 100, y - 100, x + 100, y + 100);
        final Rect focusRect = new Rect(Math.max(rect.left * 1000 / getWidth(), -1000),
                Math.max(rect.top * 1000 / getHeight(), -1000),
                Math.min(rect.right * 1000 / getWidth(), 1000),
                Math.min(rect.bottom * 1000 / getHeight(), 1000));

        if (mCamParams.getMaxNumFocusAreas() > 0) {
            List<Camera.Area> areaList = new ArrayList<>();
            areaList.add(new Camera.Area(focusRect, 1000));
            mCamParams.setFocusAreas(areaList);
            mCamParams.setMeteringAreas(areaList);
        }

        mCamera.setParameters(mCamParams);
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                mCamera.cancelAutoFocus();
                mCamera.setParameters(mCamParams);
            }
        });

    }

    /*
     * Handles the 2 finger zoom feature
     *
     * @param event - information about the pointers that the user tapped
     */
    private void handleZoom(MotionEvent event) {
        mCamParams = mCamera.getParameters();
        int maxZoom = mCamParams.getMaxZoom();
        int zoom = mCamParams.getZoom();
        float newDist = getFingerDistance(event);

        if(newDist > mDist) {
            if(zoom < maxZoom) {
                zoom+=2;
                zoom = Math.min(zoom, maxZoom);
            }
        } else {
            if(zoom > 0) {
                zoom-=2;
                zoom = Math.max(zoom, 0);
            }
        }

        mDist = newDist;
        mCamParams.setZoom(zoom);
        mCamera.setParameters(mCamParams);
    }

    /*
     * Gets the distance between each finger needed to calculate the zoom
     *
     * @param event - information about pointers that the user tapped
     */
    private float getFingerDistance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getX(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }
}
