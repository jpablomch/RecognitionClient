package zika.edu.recognitionclient;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/*
 * Constants and methods that the entire app can have access to
 *
 * TODO : Still need to implement the offline uploading soon
 */
public class Utility {

    //Checks internet connection
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    //Checks if there is Wifi
    public static boolean isWifiAvailable(Context context){
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return activeWifi.isConnected();
    }

}
