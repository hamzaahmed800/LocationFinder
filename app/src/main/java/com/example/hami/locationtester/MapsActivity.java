package com.example.hami.locationtester;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener {

    private GoogleMap mMap;
    Geocoder mGeoCoder;
    LatLng mLatlang;
    EditText mSearchText;
    Button mSearchButton,mShowButton;
    LocationManager mLocation;
    double mLatitude,mLongitude;
    GPSDatabase myDatabase;
    Cursor cursor;
    List pointList;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapReader);
        mapFragment.getMapAsync(this);



        mSearchButton = (Button) findViewById(R.id.searchButton);
        mSearchButton.setOnClickListener(this);

        mShowButton = (Button) findViewById(R.id.showRecord);
        mShowButton.setOnClickListener(this);
    }

    public void onSearch(View view) {

        mSearchText = (EditText) findViewById(R.id.searchText);
        mSearchButton = (Button) findViewById(R.id.searchButton);

        String location = mSearchText.getText().toString();

        if (location != null || !location.equals("")) {

            mGeoCoder = new Geocoder(this);
            try {
              List<Address>  mAddress = mGeoCoder.getFromLocationName(location, 1);
                mLatlang = new LatLng(mAddress.get(0).getLatitude(), mAddress.get(0).getLongitude());
                mMap.addMarker(new MarkerOptions().position(mLatlang)).setTitle(mAddress.get(0).getLocality()+","
                        +mAddress.get(0).getCountryName());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mLatlang, 15.2f));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    //Show visited Data from SQLite Databse
    private void onShow(View v) {
        mMap.clear();
        Context context = getApplicationContext();
        GPSDatabase myDatabase=new GPSDatabase(this);
        myDatabase.open();
        cursor = myDatabase.getAllRows();
        cursor.moveToFirst();
        Toast.makeText(MapsActivity.this,"HERE",Toast.LENGTH_LONG).show();

        PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
        pointList = new ArrayList();
        if(cursor!= null && cursor.getCount()>0){


            while (cursor.moveToNext()){
                createMarker(Double.valueOf(cursor.getString(cursor.getColumnIndex("latitude"))),
                        Double.valueOf(cursor.getString(cursor.getColumnIndex("longitude"))),
                        cursor.getString(cursor.getColumnIndex("locationName")));

                LatLng l1 = new LatLng(Double.valueOf(cursor.getString(cursor.getColumnIndex("latitude"))),
                        Double.valueOf(cursor.getString(cursor.getColumnIndex("longitude"))));


                pointList.add(l1);
            }

            for (int z = 0; z < pointList.size(); z++) {
                LatLng point = (LatLng) pointList.get(z);
                options.add(point);

                mMap.addPolyline(options);
            }


            mMap.animateCamera( CameraUpdateFactory.zoomTo( 11.0f ) );

        }else{
            Toast.makeText(MapsActivity.this,"No Record Exits",Toast.LENGTH_LONG).show();
        }


    }

    //Setting marker
    protected GoogleMap createMarker(double latitude, double longitude, String location) {

         mMap.addMarker(new MarkerOptions()
                .position(new LatLng(latitude,longitude)))
                .setTitle(location);
            return mMap;
    }



    //Updating Databse
    public  void updateDatabase(String myLocation){
        myDatabase=new GPSDatabase(this);
        myDatabase.open();
        myDatabase.insertRows(Double.toString(mLatitude),Double.toString(mLongitude),myLocation);
        myDatabase.close();
    }

    //Starting of MAP
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        mLocation = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        if(mLocation.isProviderEnabled(mLocation.NETWORK_PROVIDER)){

            mLocation.requestLocationUpdates(mLocation.NETWORK_PROVIDER, 1, 1, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {

                    mLatitude = location.getLatitude();
                    mLongitude = location.getLongitude();
                    mLatlang = new LatLng(mLatitude,mLongitude);

                    mGeoCoder  = new Geocoder(getApplicationContext());
                    try {
                       List<Address> mAddress = mGeoCoder.getFromLocation(mLatitude,mLongitude,1);
                        //for location checking

                        String myLocation = mAddress.get(0).getSubLocality()+","+
                                mAddress.get(0).getLocality()+","+mAddress.get(0).getCountryName();
                        updateDatabase(myLocation);
                        mMap.addMarker(new MarkerOptions().position(mLatlang)).setTitle(myLocation);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mLatlang,17.2f));


                        
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            });

        }else if(mLocation.isProviderEnabled(mLocation.GPS_PROVIDER)){

            mLocation.requestLocationUpdates(mLocation.GPS_PROVIDER, 1, 1, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {

                    mLatitude = location.getLatitude();
                    mLongitude = location.getLongitude();
                    mLatlang = new LatLng(mLatitude,mLongitude);

                    mGeoCoder  = new Geocoder(getApplicationContext());
                    try {
                        //for location checking
                      List<Address>  mAddress = mGeoCoder.getFromLocation(mLatitude,mLongitude,1);//

                        String myLocation = mAddress.get(0).getSubLocality() + "," + mAddress.get(0).getLocality()
                                + "," + mAddress.get(0).getCountryName();
                        updateDatabase(myLocation);


                        mMap.addMarker(new MarkerOptions().position(mLatlang)).setTitle(myLocation);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mLatlang,17.2f));



                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            });
        }



        }


    @Override
    public void onClick(View v) {
        switch (v.getId()){

            case R.id.searchButton:
                onSearch(v);
                break;

            case R.id.showRecord:
                onShow(v);
                break;


        }
    }



}


