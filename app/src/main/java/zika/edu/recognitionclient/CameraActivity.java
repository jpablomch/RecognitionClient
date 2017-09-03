package zika.edu.recognitionclient;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import zika.edu.recognitionclient.zikacamera.CameraPreview;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

/*
 * Custom Android camera implementation
 */
@SuppressWarnings("deprecation")
public class CameraActivity extends AppCompatActivity {

    private Camera mCamera;
    private CameraPreview mCamPreview;
    private FrameLayout mFrameLayout;
    private ImageView mOverlay;
    private Camera.PictureCallback mPictureCallback;
    private ImageButton mCaptureButton;
    private ImageButton mOverlayMenuButton;
    private Uri fileUri;
    private String timeStamp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mCaptureButton = (ImageButton)findViewById(R.id.capture_button);
        mOverlayMenuButton = (ImageButton)findViewById(R.id.overlay_menu);
        mOverlay = (ImageView)findViewById(R.id.overlay_view);

        /*
         * The picture callback used when the camera takes a picture
         * Decodes the image bytes and converts it to PNG
         */
        mPictureCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                camera.stopPreview();
                fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
                File pictureFile = new File(fileUri.getPath());

                try {
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inSampleSize = 4;
                    Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
                    FileOutputStream stream = new FileOutputStream(pictureFile);
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    stream.close();
                } catch(IOException ioe){
                    Log.d("aaa", "Error converting file to png");
                }

                Intent returnData = new Intent();
                returnData.setData(fileUri);
                returnData.putExtra("timeStamp", timeStamp);
                setResult(RESULT_OK, returnData);
                finish();
            }
        };

        mCaptureButton.setOnClickListener(new  View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.takePicture(null, null, mPictureCallback);
                mCaptureButton.setEnabled(false);
            }
        });

        mOverlayMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu menu = new PopupMenu(CameraActivity.this, mOverlayMenuButton);
                menu.inflate(R.menu.menu_overlay);
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        handleOverlays(item);

                        return true;
                    }
                });
                menu.show();
            }
        });

        mFrameLayout = (FrameLayout)findViewById(R.id.camera_preview);

    }

    /*
     * Releases the camera so that other apps can use it
     */
    @Override
    public void onPause(){
        if(mCamera != null){
            mFrameLayout.removeView(mCamPreview);
            mCamera.release();
            mCamera = null;
        }
        super.onPause();
    }

    /*
     * Creates a new camera instance upon returning to the app
     */
    @Override
    public void onResume() {
        mCamera = getCameraInstance();
        mCamPreview = new CameraPreview(this, mCamera);
        mFrameLayout.addView(mCamPreview);
        super.onResume();
    }

    /*
     * Makes sure that the phone's camera can be used and gets the instance
     */
    private Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        } catch(Exception e) {
            Toast.makeText(this, "Camera doesn't exist or open in another app", Toast.LENGTH_SHORT).show();
        }
        return c;
    }

    /*
     * Methods for creating image file to save on to phone
     */
    private Uri getOutputMediaFileUri(int mediaType){
        return Uri.fromFile(getOutputMediaFile(mediaType));
    }

    private File getOutputMediaFile(int mediaType){
        //Finds location of saving image
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "RecognitionClient");

        if(!mediaStorageDir.exists()){
            if(!mediaStorageDir.mkdir()){
                Log.d("Print", "Failed to create RecognitionClient directory");
                return null;
            }
        }

        timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File mediaFile;
        if(mediaType == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".png");
        } else {
            return null;
        }
        return mediaFile;
    }

    private void handleOverlays(MenuItem item) {
        Drawable overlay;
        switch(item.getItemId()) {
            case R.id.aedes_egg :
                overlay = ContextCompat.getDrawable(getApplicationContext(), R.drawable.aedes_egg);
                if(mOverlay.getDrawable()!= null && mOverlay.getDrawable().getConstantState() != null) {
                    if(mOverlay.getDrawable().getConstantState().equals(overlay.getConstantState())) {
                        mOverlay.setImageDrawable(null);
                    } else {
                        mOverlay.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.aedes_egg));
                        mOverlay.bringToFront();
                    }
                } else {
                    mOverlay.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.aedes_egg));
                    mOverlay.bringToFront();
                }
                break;
            case R.id.aedes_larvae :
                overlay = ContextCompat.getDrawable(getApplicationContext(), R.drawable.aedes_larvae);
                if(mOverlay.getDrawable()!= null && mOverlay.getDrawable().getConstantState() != null) {
                    if(mOverlay.getDrawable().getConstantState().equals(overlay.getConstantState())) {
                        mOverlay.setImageDrawable(null);
                    } else {
                        mOverlay.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.aedes_larvae));
                        mOverlay.bringToFront();
                    }
                } else {
                    mOverlay.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.aedes_larvae));
                    mOverlay.bringToFront();
                }
                break;
            case R.id.aedes_pupa :
                overlay = ContextCompat.getDrawable(getApplicationContext(), R.drawable.aedes_pupa);
                if(mOverlay.getDrawable()!= null && mOverlay.getDrawable().getConstantState() != null) {
                    if(mOverlay.getDrawable().getConstantState().equals(overlay.getConstantState())) {
                        mOverlay.setImageDrawable(null);
                    } else {
                        mOverlay.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.aedes_pupa));
                        mOverlay.bringToFront();
                    }
                } else {
                    mOverlay.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.aedes_pupa));
                    mOverlay.bringToFront();
                }
                break;
            case R.id.anopheles_egg :
                overlay = ContextCompat.getDrawable(getApplicationContext(), R.drawable.anopheles_egg);
                if(mOverlay.getDrawable()!= null && mOverlay.getDrawable().getConstantState() != null) {
                    if(mOverlay.getDrawable().getConstantState().equals(overlay.getConstantState())) {
                        mOverlay.setImageDrawable(null);
                    } else {
                        mOverlay.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.anopheles_egg));
                        mOverlay.bringToFront();
                    }
                } else {
                    mOverlay.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.anopheles_egg));
                    mOverlay.bringToFront();
                }
                break;
            case R.id.anopheles_larvae :
                overlay = ContextCompat.getDrawable(getApplicationContext(), R.drawable.anopheles_larvae);
                if(mOverlay.getDrawable()!= null && mOverlay.getDrawable().getConstantState() != null) {
                    if(mOverlay.getDrawable().getConstantState().equals(overlay.getConstantState())) {
                        mOverlay.setImageDrawable(null);
                    } else {
                        mOverlay.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.anopheles_larvae));
                        mOverlay.bringToFront();
                    }
                } else {
                    mOverlay.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.anopheles_larvae));
                    mOverlay.bringToFront();
                }
                break;
            case R.id.anopheles_pupa :
                overlay = ContextCompat.getDrawable(getApplicationContext(), R.drawable.anopheles_pupa);
                if(mOverlay.getDrawable()!= null && mOverlay.getDrawable().getConstantState() != null) {
                    if(mOverlay.getDrawable().getConstantState().equals(overlay.getConstantState())) {
                        mOverlay.setImageDrawable(null);
                    } else {
                        mOverlay.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.anopheles_pupa));
                        mOverlay.bringToFront();
                    }
                } else {
                    mOverlay.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.anopheles_pupa));
                    mOverlay.bringToFront();
                }
                break;
            case R.id.culex_egg :
                overlay = ContextCompat.getDrawable(getApplicationContext(), R.drawable.culex_egg);
                if(mOverlay.getDrawable()!= null && mOverlay.getDrawable().getConstantState() != null) {
                    if(mOverlay.getDrawable().getConstantState().equals(overlay.getConstantState())) {
                        mOverlay.setImageDrawable(null);
                    } else {
                        mOverlay.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.culex_egg));
                        mOverlay.bringToFront();
                    }
                } else {
                    mOverlay.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.culex_egg));
                    mOverlay.bringToFront();
                }
                break;
            case R.id.culex_larvae :
                overlay = ContextCompat.getDrawable(getApplicationContext(), R.drawable.culex_larvae);
                if(mOverlay.getDrawable()!= null && mOverlay.getDrawable().getConstantState() != null) {
                    if(mOverlay.getDrawable().getConstantState().equals(overlay.getConstantState())) {
                        mOverlay.setImageDrawable(null);
                    } else {
                        mOverlay.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.culex_larvae));
                        mOverlay.bringToFront();
                    }
                } else {
                    mOverlay.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.culex_larvae));
                    mOverlay.bringToFront();
                }
                break;
            case R.id.culex_pupa :
                overlay = ContextCompat.getDrawable(getApplicationContext(), R.drawable.culex_pupa);
                if(mOverlay.getDrawable()!= null && mOverlay.getDrawable().getConstantState() != null) {
                    if(mOverlay.getDrawable().getConstantState().equals(overlay.getConstantState())) {
                        mOverlay.setImageDrawable(null);
                    } else {
                        mOverlay.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.culex_pupa));
                        mOverlay.bringToFront();
                    }
                } else {
                    mOverlay.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.culex_pupa));
                    mOverlay.bringToFront();
                }
                break;
            case R.id.water_line :
                overlay = ContextCompat.getDrawable(getApplicationContext(), R.drawable.water_line);
                if(mOverlay.getDrawable()!= null && mOverlay.getDrawable().getConstantState() != null) {
                    if(mOverlay.getDrawable().getConstantState().equals(overlay.getConstantState())) {
                        mOverlay.setImageDrawable(null);
                    } else {
                        mOverlay.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.water_line));
                        mOverlay.bringToFront();
                    }
                } else {
                    mOverlay.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.water_line));
                    mOverlay.bringToFront();
                }
        }
    }
}
