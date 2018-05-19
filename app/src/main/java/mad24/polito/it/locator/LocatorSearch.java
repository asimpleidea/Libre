package mad24.polito.it.locator;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.LinearLayout;

public class LocatorSearch implements ActivityCompat.OnRequestPermissionsResultCallback
{
    private Context context = null;
    private Activity activity = null;
    private final int PERMISSION_REQUEST_LOCATION = 20;
    private LocationManager locationManager = null;
    private Criteria criteria = null;
    private LocatorEventsListener Listener = null;

    public LocatorSearch(Activity a, Context c, LocatorEventsListener locationListener)
    {
        //---------------------------------
        //  Init
        //---------------------------------

        if(c == null || a == null) return;

        context = c;
        activity = a;

        //---------------------------------
        //  Set up criteria
        //---------------------------------

        //  The criteria. This is probably not the most recommended method by Google, but we're short on time so...
        //  you know... done is better than perfect...
        criteria = new Criteria();

        //  To save up battery, we're using COARSE
        //  TODO: is this accurate enough? we must test this!
        criteria./*setAccuracy(Criteria.ACCURACY_FINE)*/setAccuracy(Criteria.ACCURACY_COARSE);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(true);
        criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);

        //---------------------------------
        //  Set up location manager
        //---------------------------------

        //  TODO: is it good to define it here? check with battery.
        locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);

        Listener = locationListener;
        //Log.d("debug", "Calling once()");
        once();
        //Log.d("debug", "Once() returned");
    }

    private Context getContext() { return context; }

    public Activity getActivity() { return activity; }

    private void askForPermission()
    {
        //---------------------------------
        //  Init
        //---------------------------------

        //  We should throw an exception, but we're short on time! Let's do it like this.
        if(activity == null || context == null) return;

        //Log.d("debug", "Activity and context not null");

        //  Ask
        //  NOTE: I left getContext() and getActivity() to provide some code reusability with google
        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Log.d("debug", "RequestPermission");
            //  No?
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode)
        {
            case PERMISSION_REQUEST_LOCATION:
            {
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    //Log.d("debug", "PERMISSION GRANTED");
                    once();
                }
                else
                {
                    //Log.d("debug", "PERMISSION NOT GRANTED");
                    if(Listener != null) Listener.onPermissionDenied();
                }
            }
            break;
        }
    }

    public void once()
    {
        //-------------------------------------
        //  Ask for permissions first
        //-------------------------------------

        askForPermission();

        //Log.d("debug", "PerformRequest");
        performRequest();
    }

    private void performRequest() {
        //-------------------------------------
        //  Get the location ONCE (no updates)
        //-------------------------------------

        try
        {
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener()
            {
                @Override
                public void onLocationChanged(Location location)
                {
                    //Log.d("debug", "onLocationChanged");
                    //if(Listener != null) {
                    if(location != null ) Listener.onSuccess(location.getLatitude(), location.getLongitude());
                    else Listener.onFailure();
                    //}
                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle) {
                    //Log.d("debug", "onStatusChanged");
                }

                @Override
                public void onProviderEnabled(String s) {
                    //Log.d("debug", "onProviderEnabled");
                }

                @Override
                public void onProviderDisabled(String s) {
                    //Log.d("debug", "onProviderDisabled");
                }

            }, null);
        }
        catch (SecurityException s)
        {
            if(Listener != null) Listener.onPermissionDenied();
        }
    }


    public LocatorSearch addListener(LocatorEventsListener locationListener)
    {
        if(locationListener != null) Listener = locationListener;
        return this;
    }

    public void removeListener() {
        locationManager = null;
    }
}
