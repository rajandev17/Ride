package rajandev.com.rideit.UI;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import rajandev.com.rideit.R;
import rajandev.com.rideit.Utils.Util;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity implements LocationListener {

    Button rideIT;
    private Timer selfieTimer;
    private static String ride_folder_path = "";
    SurfaceView preview;
    SurfaceHolder holder;
    private double currentLatitude;
    private double currentLongitude;
    protected LocationManager locationManager;
    protected String locationProvider;
    private Camera camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rideIT = (Button) findViewById(R.id.btn_ride_txt);
        ((TextView) findViewById(R.id.tv_title_main)).setTypeface(Util.getFontWithName(this, "head"));

        Util.checkAndEnableGPS(this);

        //setup camera
        preview = (SurfaceView) findViewById(R.id.surface_view);
        holder = preview.getHolder();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Criteria crit = new Criteria();
        crit.setAccuracy(Criteria.ACCURACY_FINE);
        locationProvider = locationManager.getBestProvider(crit, false);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
        locationManager.requestLocationUpdates(locationProvider, 0, 1, this);


        Location loc = getLastBestLocation();
        if (loc != null) {
            currentLatitude = loc.getLatitude();
            currentLongitude = loc.getLongitude();
        }
        rideIT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getTag().toString().equals("ON")) {
                    v.setTag("OFF");
                    v.setBackgroundColor(getResources().getColor(R.color.start));
                    ((Button) v).setText(getResources().getString(R.string.ride_text));
                    Util.showLongToast(MainActivity.this, "Succesfully completed Ride.");
                    endRide();
                } else {
                    Util.showLongToast(MainActivity.this, "Started Ride");
                    ((Button) v).setText(getResources().getString(R.string.ride_text_off));
                    v.setBackgroundColor(getResources().getColor(R.color.stop));
                    v.setTag("ON");
                    startRide();
                }
            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();
        Log.d("latitude", currentLatitude + "");
        Log.d("longitude", currentLongitude + "");
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude", "disable");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude", "enable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Latitude", "status");
    }

    private Location getLastBestLocation() {
        Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        long GPSLocationTime = 0;
        if (null != locationGPS) {
            GPSLocationTime = locationGPS.getTime();
        }
        long NetLocationTime = 0;
        if (null != locationNet) {
            NetLocationTime = locationNet.getTime();
        }
        if (0 < GPSLocationTime - NetLocationTime) {
            return locationGPS;
        } else {
            return locationNet;
        }
    }


    public String createFolderForRide() {
        File root_directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/RideIT");
        if (!root_directory.exists()) {
            root_directory.mkdir();
        }
        String sub_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/RideIT/" + new SimpleDateFormat("yyyy-MMM-dd' at 'hh:mm a").format(new Date());
        File sub_directory = new File(sub_path);
        try {
            sub_directory.mkdir();
        } catch (Exception e) {

        }
        return sub_path;
    }


    public void createFileWithSource() {
        try {
            File ride_details = new File(ride_folder_path, "RideDetails.txt");
            FileWriter writer = new FileWriter(ride_details);
            String start = getAddress();
            if (currentLongitude == 0 && currentLatitude == 0) {
                Location loc = getLastBestLocation();
                if (loc != null) {
                    currentLatitude = loc.getLatitude();
                    currentLongitude = loc.getLongitude();
                }
            }
            String time = new SimpleDateFormat("hh:mm a' on 'dd-MMM-yyyy'.'").format(new Date());
            String file_content = "\n*****************************************************\n\n" +
                    "\t# Ride Started at " + time + "\n" +
                    "\t# Start Address\n" + start +
                    "\t# GeoLocation\n\t\tLatitude - " + currentLatitude + "\n\t\tLongitude - " + currentLongitude + "\n";
            writer.write(file_content);
            writer.flush();
            writer.close();
        } catch (IOException e) {

        }
    }

    public void updateFileWithDestination() {
        try {
            File ride_details = new File(ride_folder_path, "RideDetails.txt");
            FileWriter textFileWriter = new FileWriter(ride_details, true);
            BufferedWriter out = new BufferedWriter(textFileWriter);
            String end = getAddress();
            if (currentLatitude == 0 && currentLongitude == 0) {
                Location loc = getLastBestLocation();
                if (loc != null) {
                    currentLatitude = loc.getLatitude();
                    currentLongitude = loc.getLongitude();
                }
            }
            String time = new SimpleDateFormat("hh:mm a' on 'dd-MMM-yyyy'.'").format(new Date());
            String file_content = "\n*****************************************************\n\n" +
                    "\t# Ride Ended at " + time + "\n" +
                    "\t# End Address\n" + end +
                    "\t# GeoLocation\n\t\tLatitude - " + currentLatitude + "\n\t\tLongitude - " + currentLongitude + "\n" +
                    "\n*****************************************************";
            out.write(file_content);
            out.close();
        } catch (Exception e) {
        }
    }

    public void startRide() {
        ride_folder_path = createFolderForRide();
        createFileWithSource();
        takeSelfieEveryFiveMinutes();
    }

    public void endRide() {
        stopTakingSelfie();
        updateFileWithDestination();
    }

    public void takeSelfieEveryFiveMinutes() {
        selfieTimer = new Timer();
        selfieTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                CaptureSelfieAndSave();
            }
        }, 0, 5000);
    }

    public void stopTakingSelfie() {
        selfieTimer.cancel();
    }

    private Camera openFrontCamera() {
        int cameraCount = 0;
        Camera cam = null;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras();
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    cam = Camera.open(camIdx);
                } catch (RuntimeException e) {
                }
            }
        }
        return cam;
    }

    public void CaptureSelfieAndSave() {
        Log.d("rideit", "taking selfie");
        camera = null;
        try {
            camera = openFrontCamera();
            try {
                camera.setPreviewDisplay(holder);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            camera.startPreview();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            camera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    camera.release();
                    Log.d("rideit", "Taken selfie");
                    saveTakenSelfie(data);
                }
            });

        } catch (Exception e) {
            if (camera != null)
                camera.release();
        }
    }

    private void saveTakenSelfie(byte[] byteArray) {
        FileOutputStream stream = null;
        try {
            Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            String imageName = "default";
            if (currentLatitude == 0 && currentLongitude == 0) {
                Location loc = getLastBestLocation();
                if (loc != null) {
                    currentLatitude = loc.getLatitude();
                    currentLongitude = loc.getLongitude();
                }
            }
            imageName = getFormattedLocationInDegree(currentLatitude, currentLongitude);
            String path = ride_folder_path + "/" + imageName + ".jpg";
            stream = new FileOutputStream(path);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();
            Log.d("rideit", "saved selfie");
            stream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException ioException) {

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        locationManager.requestLocationUpdates(locationProvider, 10000, 1, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this);
    }

    public static String getFormattedLocationInDegree(double latitude, double longitude) {
        try {
            int latSeconds = (int) Math.round(latitude * 3600);
            int latDegrees = latSeconds / 3600;
            latSeconds = Math.abs(latSeconds % 3600);
            int latMinutes = latSeconds / 60;
            latSeconds %= 60;

            int longSeconds = (int) Math.round(longitude * 3600);
            int longDegrees = longSeconds / 3600;
            longSeconds = Math.abs(longSeconds % 3600);
            int longMinutes = longSeconds / 60;
            longSeconds %= 60;
            String latDegree = latDegrees >= 0 ? "N" : "S";
            String lonDegrees = latDegrees >= 0 ? "E" : "W";

            return Math.abs(latDegrees) + "°" + latMinutes + "'" + latSeconds
                    + "\"" + latDegree + " " + Math.abs(longDegrees) + "°" + longMinutes
                    + "'" + longSeconds + "\"" + lonDegrees;
        } catch (Exception e) {

            return "" + String.format("%8.5f", latitude) + "  "
                    + String.format("%8.5f", longitude);
        }
    }

    public String getAddress() {
        String completeAddress = "";
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());
        try {
            addresses = geocoder.getFromLocation(currentLatitude, currentLongitude, 1);
            if (addresses.size() > 0) {
                String address = addresses.get(0).getAddressLine(0).length() > 0 ? "\t\t\t" + addresses.get(0).getAddressLine(0) + ",\n" : "";
                String city = addresses.get(0).getLocality().length() > 0 ? "\t\t\t" + addresses.get(0).getLocality() + "," : "";
                String state = addresses.get(0).getAdminArea().length() > 0 ? addresses.get(0).getAdminArea() : "";
                String country = addresses.get(0).getCountryName().length() > 0 ? "\n\t\t\t" + addresses.get(0).getCountryName() : "";
                String postalCode = addresses.get(0).getPostalCode().length() > 0 ? " - " + addresses.get(0).getPostalCode() + "\n" : "";
                completeAddress = address + city + state + country + postalCode;
            }
        } catch (IOException e) {
        }
        return completeAddress;
    }

}
