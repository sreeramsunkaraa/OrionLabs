package com.orionlabs.codingexercise;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    EditText edtSearch;

    Button btnSearch, btnReload;

    ArrayList<HashMap<String, String>> libList = new ArrayList<>();

    String BLOCK = "block", DATEOCCURED = "dateOccurred", CPDDISTRICT = "cpdDistrict", XCORDINATE = "xCoordinate", YCORDINATE = "yCoordinate", CPDDISTRICTCOUNT = "count", COLOR = "color";

    static ProgressDialog pdLoading;

    HashMap<String, String> mapping = new HashMap<>();
    HashMap<String, Integer> mappingCount = new HashMap<>();
    HashMap<String, String> mappingColor = new HashMap<>();

    ArrayList<Marker> arrayMarker=new ArrayList<>();
    GetCrimeReport getCrime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        edtSearch = (EditText) findViewById(R.id.search);
        btnSearch = (Button) findViewById(R.id.btnSearch);

        btnReload = (Button) findViewById(R.id.btnReload);

        getCrime = new GetCrimeReport(this);
        getCrime.execute();

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchString= edtSearch.getText().toString();

                for (int i = 0; i < libList.size(); i++) {

                    HashMap<String, String> map = libList.get(i);

                    if(!map.get(CPDDISTRICT).toString().equalsIgnoreCase(searchString))
                    {
                       Marker marker= arrayMarker.get(i);
                        marker.remove();

                    }
                }
            }
        });

        btnReload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addMarkers();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    void onMapAsync() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        addMarkers();


    }

    @Override
    protected void onResume() {
        super.onResume();


    }

    public void addMarkers() {
        for (int i = 0; i < libList.size(); i++) {

            HashMap<String, String> mapping = libList.get(i);

            LatLng position = new LatLng(Double.parseDouble(mapping.get(XCORDINATE)), Double.parseDouble(mapping.get(YCORDINATE)));
            String colorStr = getStringResourceByName(mapping.get(COLOR));
            if (mMap != null) {
                Marker marker=mMap.addMarker(new MarkerOptions().position(position).title(mapping.get(BLOCK)).icon(BitmapDescriptorFactory.fromBitmap(setMarker(colorStr))));
                arrayMarker.add(marker);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
            }
        }

    }

    private String getStringResourceByName(String aString) {
        String packageName = getPackageName();
        int resId = getResources().getIdentifier(aString, "color", packageName);
        return getString(resId);
    }

    Bitmap setMarker(String color) {
        Bitmap ob = BitmapFactory.decodeResource(this.getResources(), R.drawable.circle);
        Bitmap obm = Bitmap.createBitmap(ob.getWidth(), ob.getHeight(), ob.getConfig());
        Canvas canvas = new Canvas(obm);
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(Color.parseColor((color)), PorterDuff.Mode.SRC_ATOP));
        canvas.drawBitmap(ob, 0f, 0f, paint);
        return obm;

    }


    public class GetCrimeReport extends AsyncTask<Void, Void, ArrayList<HashMap<String, String>>> {

        Context context;
        MapsActivity mapsActivity;

        String errorMessage = "";


        public GetCrimeReport(Context context) {
            this.context = context;
            mapsActivity = new MapsActivity();


        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pdLoading = ProgressDialog.show(context, "", "Loading...");
        }

        @Override
        protected ArrayList<HashMap<String, String>> doInBackground(Void... params) {

            BufferedReader br = null;
            InputStream is = null;
            StringBuilder sb = null;
            try {
                URL url = new URL("https://api1.chicagopolice.org/clearpath/api/1.0/crimes/major?dateOccurredStart=01-01-2017&dateOccurredEnd=01-30-2017&max=100&sort=dateOccurred");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                is = new BufferedInputStream(connection.getInputStream());
                br = new BufferedReader(new InputStreamReader(is));
                sb = new StringBuilder();
                String temp;
                while ((temp = br.readLine()) != null) {
                    sb.append(temp);
                }

            } catch (ProtocolException e) {
                e.printStackTrace();
                errorMessage = e.getMessage();
            } catch (MalformedURLException e) {
                e.printStackTrace();
                errorMessage += e.getMessage();
            } catch (IOException e) {
                e.printStackTrace();
                errorMessage += e.getMessage();
            } finally {
                try {
                    if (br != null) {
                        br.close();
                    }
                    if (is != null) {
                        is.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    errorMessage += e.getMessage();
                }
            }

            JSONArray jsonAr = null;
            try {
                if (sb != null && sb.length() > 0) {
                    jsonAr = new JSONArray(sb.toString());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }


            Double latMinimum = Double.valueOf(37);
            Double latMaximum = Double.valueOf(43);

            Double longMinimum = Double.valueOf(-92);
            Double longMaximum = Double.valueOf(-87);
            libList = new ArrayList<>();
            if (jsonAr != null && jsonAr.length() > 0) {
                for (int i = 0; i <= jsonAr.length(); i++) {


                    mapping = new HashMap<>();
                    JSONObject jsonOb = null;
                    try {
                        jsonOb = jsonAr.getJSONObject(i);

                        String block = jsonOb.getString(BLOCK);
                        String dateoccured = jsonOb.getString(DATEOCCURED);
                        String cpdistrict = jsonOb.getString(CPDDISTRICT);
                        String xcordiante = jsonOb.getString(XCORDINATE);
                        String ycordinate = jsonOb.getString(YCORDINATE);


                        mapping.put(BLOCK, block);
                        mapping.put(DATEOCCURED, dateoccured);
                        mapping.put(CPDDISTRICT, cpdistrict);

                        mapping.put(XCORDINATE, "" + getRandomDouble(latMinimum, latMaximum));
                        mapping.put(YCORDINATE, "" + getRandomDouble(longMinimum, longMaximum));

                        System.out.println("x" + mapping.get(XCORDINATE));
                        System.out.println("y" + mapping.get(YCORDINATE));


                        mapping.put(COLOR, "color11");

                        if (mappingCount.get(mapping.get(CPDDISTRICT)) != null) {
                            int count = mappingCount.get(mapping.get(CPDDISTRICT)) + 1;
                            mappingCount.put(mapping.get(CPDDISTRICT), count);
                        } else {
                            mappingCount.put(mapping.get(CPDDISTRICT), 1);
                        }

                        libList.add(mapping);

                    } catch (JSONException e) {
                        e.printStackTrace();

                    }

                }
            }
            return libList;
        }

        public Double getRandomDouble(Double minimum, Double maximum) {
            return ((Double) (Math.random() * (maximum - minimum))) + minimum;
        }


        @Override
        protected void onPostExecute(ArrayList<HashMap<String, String>> hashMaps) {
            super.onPostExecute(hashMaps);
            colorMappingForDisctrict();

            pdLoading.dismiss();

            onMapAsync();


        }


        protected void colorMappingForDisctrict() {
            Set<Map.Entry<String, Integer>> set = mappingCount.entrySet();
            List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(set);
            Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
                public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                    return (o2.getValue()).compareTo(o1.getValue());
                }
            });
            int colorl = 1;
            for (Map.Entry<String, Integer> entry : list) {
                System.out.println(entry.getKey() + " ==== " + entry.getValue());
                mappingColor.put(entry.getKey(), "color" + colorl);
                if (colorl != 11) {
                    colorl++;
                }
            }

            for (Map.Entry<String, String> str : mappingColor.entrySet()
                    ) {
                System.out.println(str.getKey() + " ==== " + str.getValue());
            }

            for (int i = 0; i < libList.size(); i++) {
                HashMap<String, String> map = libList.get(i);
                map.put(COLOR, mappingColor.get(map.get(CPDDISTRICT)));


            }

            for (int i = 0; i < libList.size(); i++) {

                HashMap<String, String> map = libList.get(i);

                System.out.println("CPDDISTRICT" + map.get(CPDDISTRICT));

                System.out.println("COLOR" + map.get(COLOR));


            }
        }


    }

}
