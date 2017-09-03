package zika.edu.recognitionclient.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import zika.edu.recognitionclient.R;

/*
 * Fragment : Allows the user to edit the email that they logged in with.
 */
public class UserFragment extends Fragment {

    private Button mSaveButton;
    private EditText mEmailEdit;
    private TextView mInvalidText;
    private SharedPreferences prefs;
    private AppCompatActivity parent;

    public UserFragment(){}

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        parent = (AppCompatActivity)context;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState){
        View view = layoutInflater.inflate(R.layout.fragment_user, container, false);

        mSaveButton = (Button)view.findViewById(R.id.save_user_changes);
        mEmailEdit = (EditText)view.findViewById(R.id.email_change_text);
        mInvalidText = (TextView)view.findViewById(R.id.email_valid_text);
        prefs = getActivity().getSharedPreferences("Recognition", 0);
        mEmailEdit.setText(prefs.getString("email",""));

        parent.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);
        parent.getSupportActionBar().setTitle("User");

        mEmailEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                updateUI(s.toString());
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                updateUI(s.toString());
            }
        });

        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = mEmailEdit.getText().toString();
                if(isValidEmail(email)){
                    prefs.edit().putString("email", email).apply();
                    Toast.makeText(getActivity(), "Email changed to : " + email, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "Not a valid email address", Toast.LENGTH_SHORT).show();
                    mEmailEdit.setText("");
                }
            }
        });

        return view;
    }

    private boolean isValidEmail(CharSequence email){
        if(email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            return true;
        } else {
            return false;
        }
    }

    private void updateUI(String s){
        if(isValidEmail(s)){
            mSaveButton.setEnabled(true);
            mInvalidText.setVisibility(View.INVISIBLE);
        } else {
            mSaveButton.setEnabled(false);
            mInvalidText.setVisibility(View.VISIBLE);
        }
    }
}
