package com.example.map.activity;
//E6:DD:71:85:60:98:F0:05:32:76:9F:54:8F:7D:88:8C:A2:9B:40:E2 (Windows)

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.map.R;
import com.example.map.customInterface.IBaseGpsListener;
import com.example.map.fragment.MapsFragment;
import com.example.map.service.CLocation;
import com.example.map.service.GpsTracker;
import com.example.map.service.UploadAws;
import com.example.map.service.Util;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Timer;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks, IBaseGpsListener, SensorEventListener {

    private TextView lat, lon;
    private TextView txtCurrentSpeed;
    private CheckBox chkUseMetricUnits;
    private Button openMap;
    private ToggleButton tripToggle;

    private Runnable runnable;
    private static Boolean tripCheck = true;
    private String filePath = null;
    private static final String FILENAME = "values4.txt";
    FileOutputStream fos = null;

    private FirebaseAuth auth;
    private FirebaseUser user;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        init();

        openMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(v.getContext(), MapActivity.class));
            }
        });

        tripToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                tripCheck = isChecked;
                if (!isChecked && filePath != null) {
                    UploadAws.INSTANCE.s3Upload(MainActivity.this, filePath, user.getEmail());
                }
            }
        });

        Fragment fragment = new MapsFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_view_map, fragment, fragment.getClass().getSimpleName()).addToBackStack(null).commit();


        SensorManager mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor sensorA = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(MainActivity.this, sensorA, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener((SensorEventListener) MainActivity.this, sensor, SensorManager.SENSOR_DELAY_NORMAL);


        //sensor data
        Timer time = new Timer();
        //x = findViewById(R.id.X);
        //y = findViewById(R.id.Y);
        //z = findViewById(R.id.Z);
        //text = findViewById(R.id.text1);
        //time.schedule(new exit(), 5,2000);
        try {
            fos = openFileOutput(FILENAME, MODE_APPEND);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


    }

    private void init() {
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();


        lat = findViewById(R.id.main_lat);
        lon = findViewById(R.id.main_lon);
        openMap = findViewById(R.id.main_open_map);
        tripToggle = findViewById(R.id.main_trip_toggle);

        txtCurrentSpeed = findViewById(R.id.txtCurrentSpeed);
        chkUseMetricUnits = findViewById(R.id.main_chk_metric_units);
    }

    private String updateSpeed(CLocation location) {
        // TODO Auto-generated method stub
        float nCurrentSpeed = 0;

        if (location != null) {
            location.setUseMetricunits(this.useMetricUnits());
            nCurrentSpeed = location.getSpeed();
        }

        Formatter fmt = new Formatter(new StringBuilder());
        fmt.format(Locale.US, "%5.1f", nCurrentSpeed);
        String strCurrentSpeed = fmt.toString();
        strCurrentSpeed = strCurrentSpeed.replace(' ', '0');

        String strUnits = "miles/hour";
        if (this.useMetricUnits()) {
            strUnits = "meters/second";
        }

        txtCurrentSpeed.setText(strCurrentSpeed + " " + strUnits);
        return strCurrentSpeed + " " + strUnits;
    }

    private boolean useMetricUnits() {
        // TODO Auto-generated method stub
        return chkUseMetricUnits.isChecked();
    }


    private void getCurrentLatLon() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);


        GpsTracker gpsTracker = new GpsTracker(this);
        lat.setText("" + gpsTracker.getLatitude());
        lon.setText("" + gpsTracker.getLongitude());
        String currentDateTimeString = Util.utcCurrentTime();


        if (tripCheck) {
            writeToFile(currentDateTimeString +
                    "\nLatitude: " + gpsTracker.getLatitude() +
                    "\nLongitude: " + gpsTracker.getLongitude() +
                    "\n" + "Speed: " + txtCurrentSpeed.getText().toString() +
                    "\n\n\n\n");
        }
    }

    private void writeToFile(String data) {
        try {
            File parentFile = new File(Environment.getExternalStorageDirectory() + "/MapPoc");
            if (!parentFile.exists()) {
                parentFile.mkdirs();
            }
            File childFile = new File(parentFile, user.getEmail() + ".txt");
            childFile.createNewFile();
            filePath = childFile.toString();
            FileOutputStream stream = new FileOutputStream(childFile, true);
            stream.write(data.getBytes());
            stream.close();

        } catch (Exception e) {
            Util.toast(this, e.getMessage());
        }
    }


    @Override
    protected void onResume() {
        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(this, perms)) {
            //start handler as activity become visible
            Handler handler = new Handler();
            int delay = 1000;

            handler.postDelayed(runnable = new Runnable() {
                public void run() {
                    //do something
                    getCurrentLatLon();


                    handler.postDelayed(runnable, delay);
                }
            }, delay);
        } else {
            EasyPermissions.requestPermissions(this, "We need permissions implement feature", 123, perms);
        }

        super.onResume();
    }


    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }


    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            CLocation myLocation = new CLocation(location, this.useMetricUnits());
            this.updateSpeed(myLocation);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onGpsStatusChanged(int event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (fos != null)
            try {
                //fos.write((event.values[0] + ", ").getBytes());
                //fos.write((event.values[1] + ", ").getBytes());
                //fos.write((event.values[2] + ", ").getBytes());
                //Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                //fos.write((timestamp.toString() + "\n").getBytes());
                // Log.e("Values",""+s );

                String data = null;
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    data = "TYPE_ACCELEROMETER: " + event.values[0] + "," + event.values[1] + "," + event.values[2] + Util.utcCurrentTime() + "\n";
                }

                if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    data = "TYPE_GYROSCOPE: " + event.values[0] + "," + event.values[1] + "," + event.values[2] + Util.utcCurrentTime() + "\n";
                }

                try {
                    File parentFile = new File(Environment.getExternalStorageDirectory() + "/MapPoc");
                    if (!parentFile.exists()) {
                        parentFile.mkdirs();
                    }
                    File childFile = new File(parentFile, user.getEmail() + "_sensor.txt");
                    childFile.createNewFile();
                    filePath = childFile.toString();
                    FileOutputStream stream = new FileOutputStream(childFile, true);
                    stream.write(data.getBytes());
                    stream.close();

                } catch (Exception e) {
                    Util.toast(this, e.getMessage());
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        //x.setText("" + event.values[0]);
        //y.setText("" + event.values[1]);
        //z.setText("" + event.values[2]);
    }

}