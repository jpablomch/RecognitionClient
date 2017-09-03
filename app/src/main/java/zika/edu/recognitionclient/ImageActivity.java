package zika.edu.recognitionclient;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/*
 * Activity : Handles picking/taking pictures from the Camera or Gallery.
 */
public class ImageActivity extends AppCompatActivity {

    private Uri fileUri;
    private String timeStamp;
    private Location currLocation;
    private Button mCameraButton;
    private Button mGalleryButton;
    private TextView mLocationText;
    private SharedPreferences prefs;
    private Toolbar mToolbar;
    private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    private Handler mHandler;
    private boolean usingLastKnown = false;

    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    private static final int GALLERY_PICK_REQUEST_CODE = 200;
    private static final int MEDIA_TYPE_IMAGE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        if(!getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            Toast.makeText(getApplicationContext(),"Your device doesn't support camera", Toast.LENGTH_SHORT).show();
            finish();
        }

        prefs = getSharedPreferences("Recognition", MODE_PRIVATE);
        mCameraButton = (Button)findViewById(R.id.camera_button);
        mGalleryButton = (Button)findViewById(R.id.gallery_button);
        mLocationText = (TextView)findViewById(R.id.locale_text);
        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);
        getSupportActionBar().setTitle("Image");

        /*
         * Uses custom camera for now but can also use the native camera again
         */
        mCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
//                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
//                startActivityForResult(captureIntent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
                Intent captureIntent = new Intent(getApplicationContext(), CameraActivity.class);
                startActivityForResult(captureIntent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
            }
        });

        mGalleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GALLERY_PICK_REQUEST_CODE);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
     * Restarts location updates upon returning to this activity
     */
    @Override
    public void onResume(){
        mLocationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        if(!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                !mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            Toast.makeText(this, R.string.no_gps, Toast.LENGTH_SHORT).show();
            backToRecognition();
        } else if(currLocation == null){
            controlButtons("disable");
            setUpLocation();
        } else {
            controlButtons("enable");
        }
        super.onResume();
    }

    /*
     * Makes sure to turn off location updates so that the app doesn't keep getting user's location
     * after they exit the app.
     */
    @Override
    public void onPause(){
        if(Build.VERSION.SDK_INT >= 23){
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && mLocationListener != null){
                mLocationManager.removeUpdates(mLocationListener);
                mLocationListener = null;
                mHandler.removeCallbacksAndMessages(null);
            }
        } else if(mLocationListener != null){
            mLocationManager.removeUpdates(mLocationListener);
            mLocationListener = null;
            mHandler.removeCallbacksAndMessages(null);
        }
        super.onPause();
    }

    /*
     * Handles results from either the camera or the gallery.
     * Currently only results from camera provide the most information when uploading to dropbox.
     * If the results are from gallery, the filenames will not parse well when uploading to dropbox.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        //handling each request depending on where it came from
        if(resultCode == RESULT_OK){
            switch(requestCode) {
                case CAMERA_CAPTURE_IMAGE_REQUEST_CODE :
//                    convertToPNG();
                    Intent cameraUpload = new Intent(ImageActivity.this, UploadActivity.class);
                    fileUri = data.getData();
                    cameraUpload.putExtra("fileUri", fileUri.getPath());
//                    cameraUpload.putExtra("timeStamp", timeStamp);
                    cameraUpload.putExtra("timeStamp", data.getStringExtra("timeStamp"));
                    cameraUpload.putExtra("location", currLocation);
                    cameraUpload.putExtra("last_known", usingLastKnown);
                    startActivity(cameraUpload);
                    break;
                case GALLERY_PICK_REQUEST_CODE :
                    fileUri = data.getData();
                    Intent galleryUpload = new Intent(ImageActivity.this, UploadActivity.class);
                    galleryUpload.putExtra("fileUri", getRealPathFromUri(fileUri));
                    startActivity(galleryUpload);
                    break;
            }
        } else if(resultCode == RESULT_CANCELED){
            finish();
        } else {
            switch (requestCode) {
                case CAMERA_CAPTURE_IMAGE_REQUEST_CODE:
                    Toast.makeText(getApplicationContext(), "Failed to capture image", Toast.LENGTH_SHORT).show();
                    break;
                case GALLERY_PICK_REQUEST_CODE:
                    Toast.makeText(getApplicationContext(), "Failed to pick an image", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    /*
     * Helper method that converts image taken from native camera from JPEG to PNG.
     *
     * This method is not needed when taking pictures from custom camera as it converts to PNG when the
     * picture is taken
     */
    private void convertToPNG() {
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 3;
            Bitmap bmp = BitmapFactory.decodeFile(fileUri.getPath(), opts);
            FileOutputStream out = new FileOutputStream(fileUri.getPath());
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
        } catch (IOException ioe){ Log.d("aaa", ioe.toString()); }
    }

    /*
     * Helper method to start getting location updates
     *
     * Currently set to get an update every 5 seconds and a timeout of 1 minute. If a location is found
     * within the timeout then it will be saved. After the timeout if the user has found a previous location
     * before, it will use that. If the previous location was found over an hour ago, it will search for location again.
     */
    private void setUpLocation() {
        String bestProvider;
        if(Utility.isWifiAvailable(this) && mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            bestProvider = LocationManager.NETWORK_PROVIDER;
        } else if(mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            bestProvider = LocationManager.GPS_PROVIDER;
        } else {
            bestProvider = mLocationManager.getBestProvider(new Criteria(), true);
        }

        Date prevTime = new Date(prefs.getLong("prev_time", 0));
        Date currTime = new Date();
        long timeDiff = currTime.getTime() - prevTime.getTime();

        if(timeDiff < 36000000 && mLocationManager.getLastKnownLocation(bestProvider) != null){
            currLocation = mLocationManager.getLastKnownLocation(bestProvider);
            mLocationText.setText(getString(R.string.found_gps_last) + "\n\n(" + currLocation.getLatitude() + " , " + currLocation.getLongitude() +")");
            controlButtons("enable");
            usingLastKnown = true;
        } else {
            usingLastKnown = false;
            mLocationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    currLocation = location;
                    if(currLocation != null) {
                        Date currTime = new Date();
                        prefs.edit().putString("prev_provider", location.getProvider()).apply();
                        prefs.edit().putString("prev_latitude", String.valueOf(currLocation.getLatitude())).apply();
                        prefs.edit().putString("prev_longitude", String.valueOf(currLocation.getLongitude())).apply();
                        prefs.edit().putLong("prev_time", currTime.getTime()).apply();
                        mLocationText.setText("Location found\n\n(" + currLocation.getLatitude() + " , " + currLocation.getLongitude() +")");
                        controlButtons("enable");
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}

                @Override
                public void onProviderEnabled(String provider) {}

                @Override
                public void onProviderDisabled(String provider) {
                    Toast.makeText(getApplicationContext(), "GPS was turned off.", Toast.LENGTH_SHORT).show();
                    backToRecognition();
                }
            };

            mHandler = new Handler();
            Runnable timeout = new Runnable() {
                @Override
                public void run() {
                    if(Build.VERSION.SDK_INT >= 23
                            && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            && currLocation == null) {
                        mLocationManager.removeUpdates(mLocationListener);
                        String provider = prefs.getString("prev_provider", null);
                        String latitude = prefs.getString("prev_latitude", null);
                        String longitude = prefs.getString("prev_longitude", null);
                        long timeFound = prefs.getLong("prev_time", 0);
                        if(provider != null && latitude != null && longitude != null && timeFound != 0) {
                            usingLastKnown = true;
                            controlButtons("enable");
                            Location prevLocation = new Location(provider);
                            prevLocation.setLatitude(Double.valueOf(latitude));
                            prevLocation.setLongitude(Double.valueOf(longitude));
                            currLocation = prevLocation;
                            mLocationText.setText(getString(R.string.gps_timeout_with_prev) + "\n(" + currLocation.getLatitude() + " , " + currLocation.getLongitude() +")");
                        } else {
                            usingLastKnown = false;
                            Toast.makeText(getApplicationContext(), R.string.gps_timeout, Toast.LENGTH_SHORT).show();
                            backToRecognition();
                        }
                    }
                }
            };

            if(Build.VERSION.SDK_INT >= 23){
                if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && currLocation == null){
                    mLocationManager.requestLocationUpdates(bestProvider, 5000, 0, mLocationListener);
                    mLocationText.setText(R.string.searching_gps);
                    mHandler.postDelayed(timeout, 60000);
                } else {
                    Toast.makeText(this, "GPS Permission not granted.", Toast.LENGTH_SHORT).show();
                    backToRecognition();
                }
            } else if(currLocation == null){
                mLocationManager.requestLocationUpdates(bestProvider, 5000, 0, mLocationListener);
                mLocationText.setText(R.string.searching_gps);
                mHandler.postDelayed(timeout, 60000);
            }
        }
    }

    /*
     * Methods to create a new image file with time stamp
     */
    protected Uri getOutputMediaFileUri(int mediaType){
        return Uri.fromFile(getOutputMediaFile(mediaType));
    }

    private File getOutputMediaFile(int mediaType){
        //Finds location of saving image
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "RecognitionClient");

        if(!mediaStorageDir.exists()){
            if(!mediaStorageDir.mkdir()){
                Log.d("aaa", "Failed to create RecognitionClient directory");
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

    /*
     * Method to find the Uri of picture picked from gallery
     */
    public String getRealPathFromUri(Uri contentUri){
        Cursor c = null;
        try{
            String[] proj = {MediaStore.Images.Media.DATA};
            c = getApplicationContext().getContentResolver().query(contentUri, proj, null, null, null);
            int col_index = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            c.moveToFirst();
            return c.getString(col_index);
        } finally {
            if(c != null){
                c.close();
            }
        }
    }

    /*
     * Method to return back to Recognition Activity
     */
    private void backToRecognition() {
        Intent recogIntent = new Intent(this, RecognitionActivity.class);
        recogIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(recogIntent);
    }

    /*
     * Method that changes the button states
     *
     * @param state - The button state, either disabled or enabled
     */
    private void controlButtons(String state){
        if(state.equals("enable")){
            mCameraButton.setEnabled(true);
            mGalleryButton.setEnabled(true);
        } else if(state.equals("disable")){
            mCameraButton.setEnabled(false);
            mGalleryButton.setEnabled(false);
        }
    }
}
