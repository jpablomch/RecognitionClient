package zika.edu.recognitionclient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/*
 * Activity : Asks the user to input the email they want to log in with. User can still change the
 * email after being logged in.
 */
public class WelcomeActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private Button mNextButton;
    private TextView mInvalidText;
    private EditText mEmailEdit;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        prefs = getSharedPreferences("Recognition", MODE_PRIVATE);
        if(prefs.getString("email", null) != null) {
            startRecognition();
        }

        mNextButton = (Button)findViewById(R.id.next_button);
        mEmailEdit = (EditText)findViewById(R.id.email_edit_text);
        mInvalidText = (TextView)findViewById(R.id.email_valid_text);
        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Welcome");

        mEmailEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(isValidEmail(s.toString())){
                    mNextButton.setEnabled(true);
                    mInvalidText.setVisibility(View.INVISIBLE);
                } else {
                    mNextButton.setEnabled(false);
                    mInvalidText.setVisibility(View.VISIBLE);
                }
            }
        });

        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.edit().putString("email", mEmailEdit.getText().toString()).apply();
                if(prefs.getString("email", null) != null) {
                    startRecognition();
                }
            }
        });

    }

    private void startRecognition(){
        Intent recognitionIntent = new Intent(WelcomeActivity.this, RecognitionActivity.class);
        recognitionIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(recognitionIntent);
        finish();
    }

    private boolean isValidEmail(CharSequence email){
        if(email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            return true;
        } else {
            return false;
        }
    }

}
