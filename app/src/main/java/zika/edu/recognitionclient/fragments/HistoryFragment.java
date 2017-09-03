package zika.edu.recognitionclient.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import zika.edu.recognitionclient.R;
import zika.edu.recognitionclient.Utility;

/*
 * Fragment : Reads files from the results folder in Dropbox and displays images in the application's
 * image folder that matches the timestamp of the result file.
 */
public class HistoryFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private TextView mTextView;
    private ProgressBar mProgressBar;
    private HashMap<String, ArrayList<String>> mDownloadedResults = new LinkedHashMap<>();
    private DbxRequestConfig dbConfig;
    private DbxClientV2 dbClient;
    private SharedPreferences prefs;
    private AppCompatActivity parent;

    public HistoryFragment(){}

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        parent = (AppCompatActivity)context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        prefs = parent.getSharedPreferences("Recognition", Context.MODE_PRIVATE);
        dbConfig = new DbxRequestConfig("RecognitionClient");
        dbClient = new DbxClientV2(dbConfig, getString(R.string.DBoxAPI_KEY));
//        dbClient = new DbxClientV2(dbConfig, prefs.getString("accessToken", null));
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        parent.getSupportActionBar().setTitle("History");
        parent.getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back);

        mRecyclerView = (RecyclerView) view.findViewById(R.id.history_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(parent));
        mRecyclerView.setAdapter(new ResultAdapter(mDownloadedResults));
        mTextView = (TextView) view.findViewById(R.id.no_uploads_text);
        mProgressBar = (ProgressBar) view.findViewById(R.id.history_progress);

        return view;
    }

    @Override
    public void onResume(){
        new CheckImagesTask().execute("/ExpertAccepted", "/ExpertRejected");
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.menu_history, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.check_results_tab :
                new FetchResultsTask().execute("/RecognitionResults", "/ExpertAccepted", "/ExpertRejected");
                return true;

            default :
                return super.onOptionsItemSelected(item);
        }
    }

    private class ResultHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ImageView mImageView;
        private TextView mTextView;
        private TextView mValidated;
        private String mResults;
        private String mFilePath;

        private ResultHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            mImageView = (ImageView)itemView.findViewById(R.id.item_thumbnail);
            mTextView = (TextView)itemView.findViewById(R.id.item_result);
            mValidated = (TextView)itemView.findViewById(R.id.validated_text);
        }

        private void bindBitmap(String path){
            mFilePath = path;
            Picasso.with(parent).load(new File(path)).into(mImageView);
        }

        private void bindText(String text){
            mTextView.setText(text);
        }

        private void bindValidated(String text){
            mValidated.setText(text);
            switch(text){
                case "Rejected" :
                    mValidated.setTextColor(Color.parseColor("#ff3232"));
                    break;
                case "Accepted" :
                    mValidated.setTextColor(Color.parseColor("#32ff51"));
                    break;
                case "Pending Validation" :
                    mValidated.setTextColor(Color.parseColor("#a0a0a0"));
            }
        }

        private void bindResults(String results) {
            mResults = results;
        }

        @Override
        public void onClick(View v) {
            String msg = mResults + "\n" + mFilePath;

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Details")
                    .setCancelable(true)
                    .setMessage(msg);
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private class ResultAdapter extends RecyclerView.Adapter<ResultHolder> {

        private HashMap<String, ArrayList<String>> mResults = new LinkedHashMap<>();
        private List<String> mItemUrls;
        private ArrayList<ArrayList<String>> mResult;

        private ResultAdapter(HashMap<String, ArrayList<String>> result){
            mResults = result;
            mItemUrls = new ArrayList<>(mResults.keySet());
            mResult = new ArrayList<>(mResults.values());
        }

        @Override
        public ResultHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent);
            View view = inflater.inflate(R.layout.list_item_history, viewGroup, false);
            return new ResultHolder(view);
        }

        @Override
        public void onBindViewHolder(ResultHolder holder, int position) {
            holder.bindBitmap(mItemUrls.get(position));
            ArrayList<String> results = mResult.get(position);
            holder.bindResults(results.get(0));
            holder.bindText(results.get(0));
            holder.bindValidated(results.get(1));
        }

        @Override
        public int getItemCount() {
            return mResults.size();
        }
    }

    /*
     * AsyncTask that pairs up the result file in dropbox with the image on the user's app storage
     *
     * Currently goes through each file in the user's image directory for this app, then for each image
     * it checks each result file in dropbox for a corresponding result. If it finds one, it will extract
     * the results and update the results file that is stored on the client side.
     *
     * This is pretty slow actually, it's currently O(n^2) but perhaps I can run the search for the
     * result file in dropbox's folder with binary search since all of the files are sorted in order of
     * timestamp so the search can be O(logn) for a total of O(nlogn).
     *
     * @param String - The dropbox directory that it will read from
     */
    private class FetchResultsTask extends AsyncTask<String, Void, HashMap<String, ArrayList<String>>> {

        @Override
        protected void onPreExecute(){
            mRecyclerView.setVisibility(View.GONE);
            mTextView.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected HashMap<String, ArrayList<String>> doInBackground(String... params) {
            try {
                File imageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "RecognitionClient");
                File fetchedFiles = new File(parent.getFilesDir() + "/files_fetched.csv");
                ListFolderResult dBoxResultsFolder = dbClient.files().listFolder(params[0]);
                ListFolderResult expertAccepted = dbClient.files().listFolder(params[1]);
                ListFolderResult expertRejected = dbClient.files().listFolder(params[2]);
                TreeMap<String, ArrayList<String>> results = new TreeMap<>(Collections.<String>reverseOrder());
                HashMap<String, ArrayList<String>> sortedResults = new LinkedHashMap<>();
                ArrayList<String> noResult = new ArrayList<>();
                boolean hasAllResults = false;

                for(Map.Entry<String, ArrayList<String>> entry : mDownloadedResults.entrySet()) {
                    if(entry.getValue().get(0).equals("Waiting for results")) {
                        hasAllResults = false;
                        break;
                    } else {
                        hasAllResults = true;
                    }
                }

                if(!imageDir.exists()) {
                    throw new Exception("No images to display");
                }

                if(hasAllResults) {
                    return mDownloadedResults;
                } else {
                    getResults(fetchedFiles, imageDir, dBoxResultsFolder, results, noResult, expertAccepted, expertRejected);
                    orderResults(noResult, results, sortedResults);
                }

                return sortedResults;

            } catch (Exception e) { Log.d("aaa", e.toString()); }

            return new LinkedHashMap<>();
        }

        @Override
        protected void onPostExecute(HashMap<String, ArrayList<String>> results){
            setResults(results);
        }

        /*
         * Method that does the actual comparisons between the image file and the result file in dropbox.
         * Also checks the validated folders to see whether the results have been validated
         *
         * @param fetchedFiles - The file that is saved on the client side, needed to update the file.
         * @param imageDir - The directory where the images are stored on the client side
         * @param dBoxResultsFolder - The results directory in the dropbox, holds all the result files
         * @param results - Treemap to store the images with results, sorted by newest to oldest dates
         * @param noResult - ArrayList to hold the images with no corresponding results file
         * @param expertAccepted - The directory with accepted validation results
         * @param expertRejected - The directory with rejected validation results
         */
        private void getResults(File fetchedFiles, File imageDir, ListFolderResult dBoxResultsFolder,
                                TreeMap<String, ArrayList<String>> results, ArrayList<String> noResult,
                                ListFolderResult expertAccepted, ListFolderResult expertRejected) {
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(fetchedFiles, true));
                for(File imgFile : imageDir.listFiles()) {
                    String timeStamp = getTimeStamp(imgFile.getName());

                    if(dBoxResultsFolder != null) {
                        for(Metadata dBoxFile : dBoxResultsFolder.getEntries()){
                            if(getTimeStamp(dBoxFile.getName()).equals(timeStamp)){
                                StringBuilder sb = new StringBuilder();
                                bw.write(imgFile.getName() + ",");
                                ArrayList<String> downloadedRes = downloadResultString(dBoxFile);
                                ArrayList<String> imgResults = new ArrayList<>();
                                for(int i = 0; i < downloadedRes.size(); i++) {
                                    bw.write(downloadedRes.get(i) + (i == downloadedRes.size()-1 ? "\n":","));
                                    String[] parts = downloadedRes.get(i).split(",");
                                    double percent = Double.parseDouble(parts[1]);
                                    sb.append(parts[0] + " : " + Math.round(percent * 10000.0) / 100.0 + "%" + (i == downloadedRes.size()-1 ? "":"\n"));
                                }
                                imgResults.add(sb.toString());
                                getValidation(imgFile, imgResults, expertAccepted, expertRejected);
                                results.put(imgFile.getPath(), imgResults);
                                break;
                            }
                        }
                        if(!results.containsKey(imgFile.getPath())){
                            noResult.add(imgFile.getPath());
                        }
                    }
                }
                bw.flush();
                bw.close();
            } catch (IOException ioe) { Log.d("aaa", ioe.toString()); }
        }
    }

    /*
     * AsyncTask that reads the results file stored on the client side and shows results. This allows
     * for the client to see their results without having internet.
     *
     * If the client does have internet, then it will display whether the results have been validated or not
     *
     * Structure for files_fetched.csv is imagefilepath,class01,0.91,class02,0.1,....
     *
     * @params - Folders for expert validation
     */
    private class CheckImagesTask extends AsyncTask<String, Void, HashMap<String, ArrayList<String>>> {

        @Override
        protected void onPreExecute(){
            mRecyclerView.setVisibility(View.GONE);
            mTextView.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected HashMap<String, ArrayList<String>> doInBackground(String... params) {
            try {
                File imageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "RecognitionClient");
                File fetchedFiles = new File(parent.getFilesDir() + "/files_fetched.csv");
                ArrayList<String> fetchedResults = new ArrayList<>();
                ListFolderResult expertAccepted = null;
                ListFolderResult expertRejected = null;
                TreeMap<String, ArrayList<String>> results = new TreeMap<>(Collections.<String>reverseOrder());
                HashMap<String, ArrayList<String>> sortedResults = new LinkedHashMap<>();
                ArrayList<String> noResult = new ArrayList<>();

                if(!fetchedFiles.exists()){
                    fetchedFiles.createNewFile();
                }

                if(Utility.isNetworkAvailable(parent)) {
                    expertAccepted = dbClient.files().listFolder(params[0]);
                    expertRejected = dbClient.files().listFolder(params[1]);
                }

                readFile(fetchedFiles, fetchedResults);

                if(!imageDir.exists()) {
                    if(fetchedResults.size() > 0) {
                        fetchedFiles.delete();
                        throw new IOException("No image folder but there were saved results so the results have no file to map to");
                    } else {
                        throw new IOException("No images to display");
                    }
                }

                getResults(fetchedResults, imageDir, results, noResult, expertAccepted, expertRejected);
                orderResults(noResult, results, sortedResults);
                return sortedResults;

            } catch(IOException | DbxException e){ Log.d("aaa", e.toString()); }
            return new LinkedHashMap<>();
        }

        @Override
        protected void onPostExecute(HashMap<String, ArrayList<String>> results){
            setResults(results);
        }

        /*
         * Method that extracts the results from the client side result file and stores it in a ArrayList
         *
         * @param fetchedFiles - The client side result file
         * @param fetchedResults - The ArrayList that will hold the extracted results
         */
        private void readFile(File fetchedFiles, ArrayList<String> fetchedResults) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(fetchedFiles));
                String line;
                while((line = br.readLine()) != null){
                    String[] lineParts = line.split(",");
                    StringBuilder sb = new StringBuilder();
                    for(int i = 0; i < lineParts.length; i++){
                        String res = lineParts[i] + (i == lineParts.length-1 ? "":",");
                        sb.append(res);
                    }
                    fetchedResults.add(sb.toString());
                }
                br.close();
            } catch(IOException ioe) { System.out.println(ioe.toString()); }
        }

        /*
         * Method that reads the image files in the client side result file and finds the image
         * associated with it.
         *
         * If internet is available, it will also check the validation results
         *
         * @param fetchedResults - The results that were extracted from the client side result file
         * @param imageDir - The directory where the images are stored on the client side
         * @param results - Treemap to store the images with results, sorted by newest to oldest dates
         * @param noResult - ArrayList to hold the images with no corresponding results file
         * @param expertAccepted - The directory with accepted validation results
         * @param expertRejected - The directory with rejected validation results
         */
        private void getResults(ArrayList<String> fetchedResults, File imageDir, TreeMap<String, ArrayList<String>> results,
                                  ArrayList<String> noResult, ListFolderResult expertAccepted, ListFolderResult expertRejected){
            if(fetchedResults.isEmpty()){
                ArrayList<String> temp = new ArrayList<>(1);
                temp.add("Waiting for results");
                temp.add("");
                for(File imgFile : imageDir.listFiles()){
                    results.put(imgFile.getPath(), temp);
                }
            } else {
                for(File imgFile : imageDir.listFiles()){
                    for(String res : fetchedResults){
                        String[] resParts = res.split(",");
                        if(imgFile.getName().equals(resParts[0]) && !results.containsKey(imgFile.getPath())){
                            ArrayList<String> imgResults = new ArrayList<>();
                            StringBuilder sb = new StringBuilder();
                            for(int i = 1; i < resParts.length; i+=2){
                                double percent = Double.parseDouble(resParts[i+1]);
                                sb.append(resParts[i] + " : " + Math.round(percent * 10000.0) / 100.0 + "%" + (i+1 == resParts.length-1 ? "":"\n"));
                            }
                            imgResults.add(sb.toString());
                            getValidation(imgFile, imgResults, expertAccepted, expertRejected);
                            results.put(imgFile.getPath(), imgResults);
                            break;
                        }
                    }

                    if(!results.containsKey(imgFile.getPath())){
                        noResult.add(imgFile.getPath());
                    }
                }
            }
        }
    }

    /*
     * Method that orders the results that will be displayed to the user.
     *
     * @param noResult - An ArrayList of all the images that do not have results, used to set those images
     * with "Waiting for results"
     * @param results - TreeMap that orders the images with results from newest to oldest timestamp
     * @param sortedResults - LinkedHashMap that takes in the images with no results first and then the
     * images with results so it will show the images with no results on top
     */
    private void orderResults(ArrayList<String> noResult, TreeMap<String, ArrayList<String>> results, HashMap<String,
            ArrayList<String>> sortedResults) {
        for(String path : noResult) {
            ArrayList<String> wait = new ArrayList<>();
            wait.add("Waiting for results");
            wait.add("");
            sortedResults.put(path, wait);
        }

        for(Map.Entry<String, ArrayList<String>> entry : results.entrySet()){
            sortedResults.put(entry.getKey(), entry.getValue());
        }
    }

    /*
     * Method that gets the HashMap from AsyncTasks that contains the items that will be shown on the
     * RecyclerView. Also changes view of the progressbar.
     *
     * @param results - HashMap of all the items to be displayed
     */
    private void setResults(HashMap<String, ArrayList<String>> results) {
        mDownloadedResults = results;
        mProgressBar.setVisibility(View.GONE);
        if(results.isEmpty()){
            mRecyclerView.setVisibility(View.GONE);
            mTextView.setVisibility(View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            mTextView.setVisibility(View.GONE);
            mRecyclerView.setAdapter(new ResultAdapter(mDownloadedResults));
        }
    }

    private void getValidation(File imgFile, ArrayList<String> imgResults,
                               ListFolderResult expertAccepted, ListFolderResult expertRejected) {
        if(Utility.isNetworkAvailable(parent) && expertAccepted != null && expertRejected != null) {
            for(Metadata file : expertAccepted.getEntries()) {
                if(isPNG(file.getName()) && getTimeStamp(file.getName()).equals(getTimeStamp(imgFile.getName()))) {
                    imgResults.add("Accepted");
                    break;
                }
            }

            if(imgResults.size() < 2) {
                for(Metadata file : expertRejected.getEntries()) {
                    if(isPNG(file.getName()) && getTimeStamp(file.getName()).equals(getTimeStamp(imgFile.getName()))) {
                        imgResults.add("Rejected");
                        break;
                    }
                }
            }

            if(imgResults.size() == 1) {
                imgResults.add("Pending Validation");
            }
        } else {
            imgResults.add("");
        }
    }

    /*
     * Gets the timestamp from a filename depending on whether it's from the phone or Dropbox
     *
     * @param fileName - The file's name
     */
    protected String getTimeStamp(String fileName) {
        String[] parts = fileName.split("[._]");
        return parts[1] + "_" + parts[2];
    }

    protected boolean isPNG(String fileName) {
        String[] parts = fileName.split("[._]");
        return parts[parts.length-1].equals("png");
    }

    /*
     * Downloads the .csv file from the Dropbox result folder and parses the information that will
     * be displayed in the RecyclerView
     *
     * @param file - The dropbox file that needs to be downloaded
     */
    protected ArrayList<String> downloadResultString(Metadata file) {
        File csvFile = new File(parent.getApplication().getFilesDir() + "/" + file.getName() + ".csv");

        try {
            OutputStream stream = new FileOutputStream(csvFile);
            dbClient.files().download(file.getPathLower()).download(stream);
            stream.close();
            BufferedReader br = new BufferedReader(new FileReader(csvFile));
            ArrayList<String> content = new ArrayList<>();

            String line;
            while((line = br.readLine()) != null){
                content.add(line);
            }

            csvFile.delete();
            return content;
        } catch (DbxException | IOException e) { System.out.println(e.toString()); }

        return null;
    }
}
