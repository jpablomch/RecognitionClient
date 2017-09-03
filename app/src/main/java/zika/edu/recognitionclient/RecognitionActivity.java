package zika.edu.recognitionclient;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import zika.edu.recognitionclient.fragments.ClientFragment;
import zika.edu.recognitionclient.fragments.UserFragment;

/*
 * Activity : Holds the fragments used for the client.
 * Fragments : Client, User, History
 */
public class RecognitionActivity extends AppCompatActivity {

    private FragmentManager fm = getSupportFragmentManager();
    private SharedPreferences prefs;
    private Toolbar mToolbar;
    private Fragment currentFrag;
    private final String[] perms = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA};
//    private final int AUTH_REQUEST_CODE = 100;
    private final int PERMS_REQUEST_CODE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition);
        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        prefs = getSharedPreferences("Recognition", 0);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            requestPermissions(perms, PERMS_REQUEST_CODE);
        }

//        dropboxOAuth();

        Fragment clientFrag = new ClientFragment();
        if(!clientFrag.isAdded()){
            fm.beginTransaction().add(R.id.fragment_container, clientFrag, "clientFrag").commit();
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_user);
        } else {
            fm.beginTransaction().replace(R.id.fragment_container, clientFrag, "clientFrag").commit();
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_user);
        }

    }

    /*
     * User permission answers, if not all the permissions are granted then it returns to the main page
     */
    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults){
        switch(permsRequestCode){
            case PERMS_REQUEST_CODE :
                boolean readExternal = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean writeExternal = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                boolean coarseLocation = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                boolean fineLocation = grantResults[3] == PackageManager.PERMISSION_GRANTED;
                boolean camera = grantResults[4] == PackageManager.PERMISSION_GRANTED;
                if(!(writeExternal && readExternal && coarseLocation && fineLocation && camera)) {
                    Toast.makeText(getApplicationContext(), R.string.permission_denied, Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        Fragment frag;

        switch(item.getItemId()){
            case android.R.id.home :
                if(currentFrag.getTag().equals("clientFrag")){
                    frag = new UserFragment();
                    fm.beginTransaction().replace(R.id.fragment_container, frag, "userFrag").commit();
                } else if(currentFrag.getTag().equals("userFrag")){
                    frag = new ClientFragment();
                    fm.beginTransaction().replace(R.id.fragment_container, frag, "clientFrag").commit();
                } else if(currentFrag.getTag().equals("historyFrag")){
                    frag = new ClientFragment();
                    fm.beginTransaction().replace(R.id.fragment_container, frag, "clientFrag").commit();
                }
                return true;

            default :
                return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Calls the Dropbox OAuth2 Activity to get access token needed for Dropbox API calls and saves it to
     * the app's SharedPreferences. Makes sure the OAuth2 Activity doesn't get called again if the
     * access token is already in the SharedPreferences.
     */
//    private void dropboxOAuth() {
//        String accessToken = prefs.getString("accessToken", null);
//        if(accessToken == null){
//            Intent authIntent = AuthActivity.makeIntent(this, getString(R.string.DBoxAPI_KEY), null, null);
//            startActivityForResult(authIntent, AUTH_REQUEST_CODE);
//        }
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data){
//        if(requestCode == AUTH_REQUEST_CODE && Auth.getOAuth2Token() != null){
//            prefs.edit().putString("accessToken", Auth.getOAuth2Token()).apply();
//        } else {
//            finish();
//        }
//    }

    @Override
    public void onAttachFragment(Fragment fragment){
        currentFrag = fragment;
    }
}
