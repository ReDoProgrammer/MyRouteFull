package com.redo.production.myroute;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.acos;
import static java.lang.Math.toRadians;


@TargetApi(Build.VERSION_CODES.GINGERBREAD)
@SuppressLint("NewApi")



public class MapsActivity extends FragmentActivity{

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private LatLng startPoint=null,currentPoint;
    TextView txvDistance,txvSpeed,txvTimeLeft;
    long distance=0;
    long count=0;
    double speed=0;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        txvDistance=(TextView) findViewById(R.id.txvDistance);
        txvSpeed=(TextView) findViewById(R.id.txvSpeed);
        txvTimeLeft =(TextView) findViewById(R.id.txvTimeLeft);


        final CounterClass timer = new CounterClass(120000,1000);
        timer.start();


        //Thread thread = new Thread(new Timer());
        //thread.start();

        //check GPS is enable or not
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Toast.makeText(this,"GPS is enabled",Toast.LENGTH_SHORT).show();
        }
        else {
            showDisabledGPSAlert();
        }


    }

    private void showDisabledGPSAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("GPS is disable. Would you like to eable it?")
                .setCancelable(false)
                .setPositiveButton("Go to setting",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent settingGPSIntent = new Intent(
                                        Settings.ACTION_LOCATION_SOURCE_SETTINGS
                                );
                                startActivity(settingGPSIntent);
                            }
                        });
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog alert = alertDialog.create();
        alert.show();
    }


    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }


    public  void onReset(View v)    {
        mMap.clear();
        distance = 0;
        txvDistance.setText(Long.toString(distance));

        currentPoint = null;
        txvSpeed.setText("0asf");
        txvTimeLeft.setText("aba");
    }


    public double getDistance(LatLng fP, LatLng lP)    {
        double l1 = toRadians(fP.latitude);
        double l2 = toRadians(lP.latitude);
        double g1 = toRadians(fP.longitude);
        double g2 = toRadians(lP.longitude);

        double dist = acos(Math.sin(l1) * Math.sin(l2) +  Math.cos(l1) *  Math.cos(l2) *  Math.cos(g1 - g2));
        if(dist < 0) {
            dist = dist + Math.PI;
        }
        return Math.round(dist * 6378100);
    }



    private void setUpMapIfNeeded() {


        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            mMap.setMyLocationEnabled(true);
        }

        if (mMap != null) {
            mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
                @Override
                public void onMyLocationChange(Location arg0) {


                    //check startPoint is set or not?
                    if(startPoint==null)
                    {
                        startPoint = new LatLng(arg0.getLatitude(), arg0.getLongitude());
                        mMap.addMarker(new MarkerOptions().position(startPoint).title("Started point"));
                    }

                    LatLng prePoint;
                    if(currentPoint!=null)
                        prePoint=currentPoint;
                    else
                        prePoint=startPoint;


                    //updating current Position
                    currentPoint = new LatLng(arg0.getLatitude(),arg0.getLongitude());

                    //Draw route
                    PolylineOptions polylineOptions = new PolylineOptions()
                                                        .add(prePoint)
                                                        .add(currentPoint)
                                                        .width(10).color(Color.BLUE).geodesic(true);
                    mMap.addPolyline(polylineOptions);


                    //set camera
                    currentPoint = new LatLng(arg0.getLatitude(), arg0.getLongitude());
                    CameraPosition currentPos = new CameraPosition.Builder()
                            .target(currentPoint).zoom(17).bearing(90).tilt(30).build();
                    mMap.animateCamera(
                            CameraUpdateFactory.newCameraPosition(currentPos));


                    //update distance
                    distance+=getDistance(prePoint, currentPoint);
                    txvDistance.setText(Double.toString(distance) + "m");


                    //get current speed
                    speed = arg0.getSpeed();
                    txvSpeed.setText(Double.toString(speed)+ "km/h");

                }
            });

        }


    }


    public class Timer implements Runnable
    {

        @Override
        public void run() {
            count++;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            txvTimeLeft.setText(Long.toString(count));
        }
    }



    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @SuppressLint("NewApi")
    public class CounterClass extends CountDownTimer    {

        public CounterClass(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }


        @SuppressLint("NewApi")
        @TargetApi(Build.VERSION_CODES.GINGERBREAD)

        @Override
        public void onTick(long millis) {
           String hms = String.format("%02d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(millis),
                    TimeUnit.MILLISECONDS.toMinutes(millis) -
                    TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)),
                    TimeUnit.MILLISECONDS.toSeconds(millis) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
            txvTimeLeft.setText(hms);
        }

        @Override
        public void onFinish() {

        }
    }


}
