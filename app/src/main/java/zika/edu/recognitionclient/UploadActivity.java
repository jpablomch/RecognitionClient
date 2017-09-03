package zika.edu.recognitionclient;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.util.Base64;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.WriteMode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;

import zika.edu.recognitionclient.model.Photo;

/*
 * Activity : Handles the uploading of the image taken from the camera
 */
public class UploadActivity extends AppCompatActivity {

    private static DbxRequestConfig dbConfig;
    private static DbxClientV2 dbClient;
    private SharedPreferences prefs;
    private String fileUri = null;
    private String timeStamp = null;
    private Location location = null;
    private boolean usingLastKnown = false;
    private ImageView mImageView;
    private Button mUploadButton;
    private Button mBackButton;
    private Toolbar mToolbar;
    private ProgressBar mProgressBar;
    private boolean isUploaded;
    private String backend;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        prefs = getSharedPreferences("Recognition", MODE_PRIVATE);
        dbConfig = new DbxRequestConfig("RecognitionClient");
//        dbClient = new DbxClientV2(dbConfig, prefs.getString("accessToken", null));
        dbClient = new DbxClientV2(dbConfig, getString(R.string.DBoxAPI_KEY));

        mImageView = (ImageView)findViewById(R.id.img_preview);
        mUploadButton = (Button)findViewById(R.id.upload_button);
        mBackButton = (Button)findViewById(R.id.back_button);
        mProgressBar = (ProgressBar)findViewById(R.id.progress_bar);
        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Upload");
        isUploaded = false;
        fileUri = getIntent().getStringExtra("fileUri");
        timeStamp = getIntent().getStringExtra("timeStamp");
        location = getIntent().getParcelableExtra("location");
        usingLastKnown = getIntent().getBooleanExtra("last_known", false);
        backend = getString(R.string.backend);

        if(fileUri != null){
            setImagePreview();
        }  else {
            Toast.makeText(getApplicationContext(), "File path is missing", Toast.LENGTH_LONG).show();
        }

        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Utility.isNetworkAvailable(getApplicationContext())){
                    if(backend.equals("dropbox")){
                        uploadFileToDbx();
                    } else if(backend.equals("dynamo")){
                        uploadFileToDynamo();
                    }
                } else {
                    prefs.edit().putString(fileUri, fileUri).apply();
                    showAlert("No internet connectivity\n\nPlease try again when you have better connection", "no_internet");
                }
            }
        });

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               goToActivity("image");
            }
        });

    }

    /*
     * Deletes the file if it wasn't uploaded so that it doesn't use up the user's phone storage
     */
    @Override
    public void onStop(){
        if(!isUploaded && fileUri != null){
            File file = new File(fileUri);
            file.delete();
        }
        super.onStop();
    }

    /*
     * Previews the image that was taken from camera or picked from gallery
     */
    private void setImagePreview(){
        mImageView.setVisibility(View.VISIBLE);
        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(fileUri, options);
        mImageView.setImageBitmap(bitmap);
    }

    /*
     * Method that holds an AsyncTask to upload the file to Dropbox
     */
    public void uploadFileToDbx() {
        new AsyncTask<Void, Integer, String>() {

            @Override
            protected void onPreExecute(){
                mUploadButton.setEnabled(false);
                mBackButton.setEnabled(false);
                mProgressBar.setVisibility(View.VISIBLE);
                mProgressBar.setIndeterminate(true);
            }

            @Override
            protected String doInBackground(Void... params) {
                if(handleUpload()){
                    isUploaded = true;
                    return getString(R.string.uploaded);
                } else {
                    return getString(R.string.error_dialog);
                }
            }

            @Override
            protected void onPostExecute(String result){
                mProgressBar.setVisibility(View.INVISIBLE);
                mBackButton.setEnabled(true);
                if(!isUploaded){
                    showAlert(result, "upload_fail");
                    mUploadButton.setEnabled(true);
                } else {
                    showAlert(result, "upload_success");
                }
                super.onPostExecute(result);
            }
        }.execute();
    }

    private void uploadFileToDynamo() {
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... params) {
                AmazonDynamoDBClient dynamo = DynamoClient.getClient(getParent());
                DynamoDBMapper mapper = new DynamoDBMapper(dynamo);

                Bitmap image = BitmapFactory.decodeFile(fileUri);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                image.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] imageBytes = Base64.encode(stream.toByteArray());

                Photo photo = new Photo();
                photo.setUsername(getUsername());
                photo.setTimestamp(timeStamp);
                photo.setimageUrl(ByteBuffer.wrap(imageBytes));

                if(usingLastKnown) {
                    photo.setLast_known_lat(location.getLatitude());
                    photo.setLast_known_long(location.getLongitude());
                } else {
                    photo.setLongitude(location.getLongitude());
                    photo.setLatitude(location.getLatitude());
                }

                mapper.save(photo);
                isUploaded = true;
                return getString(R.string.uploaded);
            }

            @Override
            protected void onPostExecute(String result) {
                mProgressBar.setVisibility(View.INVISIBLE);
                mBackButton.setEnabled(true);
                if (!isUploaded) {
                    showAlert(result, "upload_fail");
                    mUploadButton.setEnabled(true);
                } else {
                    showAlert(result, "upload_success");
                }
                super.onPostExecute(result);
            }
        }.execute();
    }

    /*
     * Shows an AlertDialog depending on which value the param reason is given
     *
     * @param msg - The message that the dialog will display
     * @param reason - The reason for showing the AlertDialog which will determine the function of it
     */
    private void showAlert(String msg, String reason) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        switch(reason) {
            case "upload_success" :
                builder.setMessage(msg)
                .setTitle("Upload")
                .setNegativeButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        goToActivity("image");
                    }
                })
                .setPositiveButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        goToActivity("recognition");
                    }
                });
                break;

            case "upload_fail" :
                builder.setMessage(msg)
                    .setTitle("Upload")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            goToActivity("image");
                        }
                    });
                break;

            case "no_internet" :
                builder.setMessage(msg)
                    .setTitle("Internet Connectivity")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                break;
        }
        AlertDialog alert = builder.create();
        alert.show();
    }

    /*
     * Handles the creating of csv files and uploading of both the image and the csv file that
     * was created to Dropbox.
     *
     * If the user uses a previous location then the .csv will use the last_known_lat and last_known_long columns.
     * If the user uses a location that was just found it will use latitude and longitude columns.
     */
    private boolean handleUpload() {
        String dbxFileName = getUsername() + "_" + timeStamp;
        File imgFile = new File(fileUri);
        File csvFile = new File(getApplication().getFilesDir() + "/" + dbxFileName + ".csv");

        try {
            PrintWriter csvPW = new PrintWriter(csvFile);
            csvPW.write("user_email,image_id,latitude,longitude,last_known_lat,last_known_long\n");
            csvPW.write(prefs.getString("email", "") + ",");
            csvPW.write(timeStamp + ",");

            if (location != null) {
                if(usingLastKnown){
                    csvPW.write(",," + location.getLatitude() + "," + location.getLongitude() + "\n");
                } else {
                    csvPW.write(location.getLatitude() + "," + location.getLongitude() + ",,\n");
                }
            } else {
                csvPW.write("NoGeolocation");
            }
            csvPW.close();

            InputStream imageStream = new FileInputStream(imgFile);
            InputStream csvStream = new FileInputStream(csvFile);

            dbClient.files()
                    .uploadBuilder("/UserPicturesToProcess/" + dbxFileName + ".png")
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(imageStream);
            dbClient.files()
                    .uploadBuilder("/TextFilesToProcess/" + dbxFileName + ".csv")
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(csvStream);

            return true;
        } catch (DbxException | IOException e) {
            isUploaded = false;
            return false;
        }
    }

    /*
     * Extracts the username from the image filename
     */
    private String getUsername() {
        String email = prefs.getString("email", null);
        if(email != null){
            for(int i = 0; i < email.length(); i++){
                if(email.charAt(i) == '@'){
                    return email.substring(0, i);
                }
            }
        }
        return "";
    }

    /*
     * Returns to an activity which was specified in the param activity.
     *
     * @param activity - Which activity the intent should be created for
     */
    private void goToActivity(String activity){
        Intent intent = null;

        switch(activity){
            case "image" :
                intent = new Intent(this, ImageActivity.class);
                break;
            case "recognition" :
                intent = new Intent(this, RecognitionActivity.class);
                break;
        }

        if(intent != null){
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }

}
