package com.example.speedysafe.ui.home;

import static android.content.Context.LOCATION_SERVICE;
import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.speedysafe.databinding.FragmentHomeBinding;
import com.example.speedysafe.databinding.ItemInputSpeedBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.Locale;

public class HomeFragment extends Fragment implements LocationListener {

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor sharedPreferenceEditor;
    FusedLocationProviderClient fusedLocationClient;
    long start, finish, time;//variables that will help me count time in milliseconds
    double lat1, lon1, lat2, lon2, time1, distance, speed;//variables to save the coordinates, time, distance and speed
    int counts, radius, maxspeed;
    LocationManager locationManager;//initialize variable, type LocationManager which is a class that provides access to system location services
    private FragmentHomeBinding binding;
    private Context context;
    TextToSpeech textToSpeech;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        setUI();
    }

    private void setUI() {
        context = requireContext();
        sharedPreferences = context.getSharedPreferences("SpeedySafe", MODE_PRIVATE);
        sharedPreferenceEditor = sharedPreferences.edit();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        setSpeedLimit();
        locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        distance = 0;
        start = 0;
        counts = 0;
        radius = 20;

        textToSpeech = new TextToSpeech(requireContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {

                // if No error is found then only it will run
                if(i!=TextToSpeech.ERROR){
                    // To Choose language of speech
                    textToSpeech.setLanguage(Locale.UK);
                }
            }
        });

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)//if the permission for location access is given
            binding.startMonitoringButton.setText("Start Monitoring");
        else
            binding.startMonitoringButton.setText("Give Permission");

        binding.editSpeedLimitIcon.setOnClickListener(v -> {
            displayInputSpeedLimit();
        });

        binding.startMonitoringButton.setOnClickListener(v -> {
            start();
        });
    }

    void setSpeedLimit() {
        int res = getSpeedFromSharedPreference();
        if (res == -1) {
            displayInputSpeedLimit();
            binding.speedLimitDisplayText.setText("NA Km/h");
            maxspeed = 40;
        } else {
            binding.speedLimitDisplayText.setText(res + " Km/h");
            binding.pointerSpeedometer.setMaxSpeed(res);
            maxspeed = res;
        }
    }

    void displayInputSpeedLimit() {
        // dialog to add an emergency contact
        ItemInputSpeedBinding binding = ItemInputSpeedBinding.inflate(LayoutInflater.from(context));
        //initialize alert builder.
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        //set our custom alert dialog to tha alertdialog builder
        alert.setView(binding.getRoot());

        final AlertDialog dialog = alert.create();

        binding.buttonAdd.setOnClickListener(view1 -> {
            String speed = binding.inputSpeedLimit.getText().toString();

            setSpeedToSharedPreference(Integer.parseInt(speed));
            setSpeedLimit();

            dialog.dismiss();
        });

        dialog.show();
    }

    void setSpeedToSharedPreference(int value) {
        sharedPreferenceEditor.putInt("speedLimit", value);
        sharedPreferenceEditor.commit();
        sharedPreferenceEditor.apply();
    }

    int getSpeedFromSharedPreference() {
        return sharedPreferences.getInt("speedLimit", -1);
    }

    public void start() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && binding.startMonitoringButton.getText() == "Start Monitoring") {//if the permission for location access is given
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this::onLocationChanged);//we request location updates
            binding.startMonitoringButton.setText("Exit");
            binding.infoCardView.setVisibility(View.GONE);
            binding.displayLayout.setVisibility(View.VISIBLE);
        } else if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && binding.startMonitoringButton.getText() == "Exit") {//if we want to exit the app
            //re run
        } else if (binding.startMonitoringButton.getText() == "Give Permission") {//if we haven't given permission yet
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 6);//we request the permission
            binding.startMonitoringButton.setText("Start Monitoring");
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (lat1 == 0 && lon1 == 0) {//If the counter is 0 which means it is the first time that the location has changed
            start = System.nanoTime();//a timer starts
            lat1 = location.getLatitude();//the coordinates are saved
            lon1 = location.getLongitude();
            distance = 0;
        } else {

            finish = System.nanoTime();//I finish the timer
            lat2 = location.getLatitude();//the coordinates are saved
            lon2 = location.getLongitude();

            if(lat1 == lat2 && lon1 == lon2){
                return;
            }

            time = finish - start;//I save the time in nanoseconds
            time1 = (double) time / 1_000_000_000.0;//I convert time in seconds
            distance = measureDistance(lat1, lat2, lon1, lon2);//I calculate the distance between two points through my custom method measureDistance
            speed = distance / time1;//calculate the speed in meters/second
            speed = speed * 3.6;//convert speed from m/s to km/h
            speed = (int) speed;//i get rid of the decimal numbers
            binding.pointerSpeedometer.speedTo((float) speed);//the speed is shown
            start = System.nanoTime();//restart time and counter
            finish = 0;
            boolean res = checkProximity(lat1, lon1, lat2, lon2);
            if (!res) {
                checkSpeed(speed);
            }

            addIntoSharedPreference("lat", lat2 + "");
            addIntoSharedPreference("long", lon2 + "");
            lat1 = lat2;
            lon1 = lon2;
        }
    }

    public void addIntoSharedPreference(String key, String value){
        sharedPreferenceEditor.putString(key, value);
        sharedPreferenceEditor.apply();
        sharedPreferenceEditor.commit();
    }

    public boolean checkProximity(Double latitude1, Double longitude1, Double latitude2, Double longitude2) {
        double distance = measureDistance(latitude1, latitude2, longitude1, longitude2);
        if (distance < radius) {
            binding.displayLayout.setVisibility(View.VISIBLE);
            binding.pointerSpeedometer.setVisibility(View.VISIBLE);
            binding.pointerSpeedometer.speedTo(0);
            binding.overSpeedLimitCardView.setVisibility(View.GONE);
            binding.locationProximityCardView.setVisibility(View.VISIBLE);
            return true;
        }
        return false;
    }

    public void checkSpeed(Double speed) {

        binding.locationProximityCardView.setVisibility(View.GONE);
        if (speed >= maxspeed) {
            binding.displayLayout.setVisibility(View.VISIBLE);
            binding.pointerSpeedometer.speedTo(0);
            binding.overSpeedLimitCardView.setVisibility(View.VISIBLE);
            textToSpeech.speak("Kindly Slow Down",TextToSpeech.QUEUE_FLUSH,null);
        } else if (speed < maxspeed) {
            binding.pointerSpeedometer.speedTo(speed.floatValue());
            binding.overSpeedLimitCardView.setVisibility(View.GONE);
            binding.locationProximityCardView.setVisibility(View.GONE);
        }
    }

    public double measureDistance(Double lat1, Double lat2, Double lon1, Double lon2) {//custom method that calculates distance between two points
        //formula found on the web
        final int R = 6371; // Radius of the earth
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters
        return distance;
    }
}