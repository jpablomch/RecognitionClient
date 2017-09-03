package zika.edu.recognitionclient.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import zika.edu.recognitionclient.ImageActivity;
import zika.edu.recognitionclient.R;

/*
Fragment : Shows the functions of the app that the user can choose from.
 */
public class ClientFragment extends Fragment {

    private Button mUploadButton;
    private AppCompatActivity parent;
    private FragmentManager mFragmentManager;

    public ClientFragment(){}

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        parent = (AppCompatActivity)context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        if(!getActivity().getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            Toast.makeText(getActivity().getApplicationContext(), "Your device doesn't support camera", Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }

        mFragmentManager = parent.getSupportFragmentManager();
        setHasOptionsMenu(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_client, container, false);

        parent.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_user);
        parent.getSupportActionBar().setTitle("Client");

        mUploadButton = (Button)view.findViewById(R.id.image_upload);
        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), ImageActivity.class));
            }
        });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.menu_client, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.history_tab :
                HistoryFragment historyFragment = new HistoryFragment();
                mFragmentManager.beginTransaction().replace(R.id.fragment_container, historyFragment, "historyFrag").commit();
                return true;

            default :
                return super.onOptionsItemSelected(item);
        }
    }
}
