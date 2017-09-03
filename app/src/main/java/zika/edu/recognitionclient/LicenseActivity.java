package zika.edu.recognitionclient;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

/*
 * Activity : Prompts the user to accept to the license and terms. Answer is saved in the
 * SharedPreferences so that the user isn't asked every time they open the app.
 */
public class LicenseActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);

        prefs = getSharedPreferences("Recognition", MODE_PRIVATE);
        if(!prefs.getBoolean("license", false)){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("License & Terms of Use");
            builder.setMessage(R.string.disclaimer);
            builder.setCancelable(false);
            builder.setPositiveButton("Agree", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    prefs.edit().putBoolean("license", true).apply();
                    startWelcome();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finishAffinity();
                }
            });

            AlertDialog licenseDialog = builder.create();
            licenseDialog.show();
        } else {
            startWelcome();
        }

    }

    private void startWelcome() {
        Intent welcomeIntent = new Intent(LicenseActivity.this, WelcomeActivity.class);
        startActivity(welcomeIntent);
        finish();
    }

    private void startLogin() {
        Intent loginIntent = new Intent(LicenseActivity.this, LoginActivity.class);
        startActivity(loginIntent);
        finish();
    }
}
