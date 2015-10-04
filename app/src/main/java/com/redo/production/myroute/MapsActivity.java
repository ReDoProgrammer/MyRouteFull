package com.redo.production.myroute;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
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

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
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





        //*******************************************************
        //checking GPS and Network available
        //*******************************************************
        ConnectivityManager conMan = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        //mobile
        NetworkInfo.State mobile = conMan.getNetworkInfo(1).getState();
        //wifi
        NetworkInfo.State wifi = conMan.getNetworkInfo(1).getState();


        if (mobile == NetworkInfo.State.CONNECTED || mobile == NetworkInfo.State.CONNECTING)
        {
            Toast.makeText(this,"Network connected",Toast.LENGTH_LONG).show();
        }
        else if (wifi == NetworkInfo.State.CONNECTED || wifi == NetworkInfo.State.CONNECTING)
        {
            Toast.makeText(this,"Wifi connected",Toast.LENGTH_LONG).show();
        }



        //Time counting

        Timer T=new Timer();
        T.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        txvTimeLeft.setText(setClock(count));
                        count++;
                    }
                });
            }
        }, 1000, 1000);


    }



    private void showDisabledGPSAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("GPS is disable. Would you like to enable it?")
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
                return;
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



    //capture screen
    public void onSave(View v){

        Date now = new Date();
        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);

        try {
            // image naming and path  to include sd card  appending name you choose for file
            String mPath = Environment.getExternalStorageDirectory().toString() + "/" + now + ".jpg";

            // create bitmap screen capture
            View v1 = getWindow().getDecorView().getRootView();
            v1.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(v1.getDrawingCache());
            v1.setDrawingCacheEnabled(false);

            File imageFile = new File(mPath);

            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();
            Toast.makeText(this,"Successful",Toast.LENGTH_LONG).show();
            openScreenshot(imageFile);
        } catch (Throwable e) {
            // Several error may come out with file handling or OOM

            Toast.makeText(this,"Fail",Toast.LENGTH_LONG).show();
        }

    }

    private void openScreenshot(File imageFile) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(imageFile);
        intent.setDataAndType(uri, "image/*");
        startActivity(intent);
    }

    public String setClock(long n){
        int second = (int)n%60;
        n = n/60;
        int minute = (int)n%60;

        int hour = (int)n/60;
        String strSecond = String.valueOf(second);
        String strMinute = String.valueOf(minute);
        String strHour = String.valueOf(hour);
        if(second<10) strSecond = "0" + strSecond;
        if(minute<10) strMinute = "0" + strMinute;
        if(hour<10) strHour = "0"+strHour;
        return strHour+":"+strMinute+":"+strSecond;
    }
    public  void onReset(View v)    {
        mMap.clear();
        distance = 0;
        count = 0;
        txvDistance.setText(Long.toString(distance));

        currentPoint = null;

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

}
