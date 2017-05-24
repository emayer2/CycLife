package edu.uw.cyclife.cyclifeapp;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class KillswitchActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    TextView text1;
    TextView infoText;
    boolean outOfTime;
    Ringtone r;
    Vibrator vib;
    private List<String> contactNumbers;

    private static final String FORMAT = "%02d:%02d";

    // Start without a delay
    // Vibrate for 100 milliseconds
    // Sleep for 1000 milliseconds
    long[] pattern = {0, 100, 1000};

    SharedPreferences sharedPref = null;

    /**
     * Provides the entry point to Google Play services.
     */
    private GoogleApiClient mGoogleApiClient;

    /**
     * Represents a geographical location.
     */
    protected Location mLastLocation;

    protected String mLatitudeLabel;
    protected String mLongitudeLabel;
    protected TextView mLatitudeText;
    protected TextView mLongitudeText;
    protected String mLat;
    protected String mLong;
    //protected String emergencyMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.kill_switch_timer);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CALL_PHONE}, 1);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        mLatitudeLabel = getResources().getString(R.string.latitude_label);
        mLongitudeLabel = getResources().getString(R.string.longitude_label);
        // Build the location services request
        buildGoogleApiClient();

        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.SEND_SMS},1);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String killSwitchLength = sharedPref.getString("KillSwitchLength", "");
        int ksLength;
        final boolean emsEnabled;
        if (killSwitchLength.equals("")) {
            ksLength = 60;
        } else {
            ksLength = Integer.parseInt(killSwitchLength);
        }
        final String emergencyMessage = sharedPref.getString("EmergencyMessage", "");
        contactNumbers = getEmergencyNumbers();

        text1=(TextView)findViewById(R.id.timer_countdown);
        infoText=(TextView)findViewById(R.id.info_message);
        infoText.setText("Press the killswitch to disable the emergency message!");

        emsEnabled = sharedPref.getBoolean("911Enabled", false);
        final String emsMessage = sharedPref.getString("UserName", "") + "\n" +
                sharedPref.getString("EmergencyMessage", "Cyclist has crashed and is unresponsive. Please contact me to see if I'm alright, if not please call 911 and give them my location:");

        outOfTime = false;
        final Button killbutton = (Button)findViewById(R.id.kill_button);


        // Code to blink the kill switch on and off drawing attention until the user pushes it
        final Animation animation = new AlphaAnimation(1, 0); // Change alpha from fully visible to invisible
        animation.setDuration(500); // duration - half a second
        animation.setInterpolator(new LinearInterpolator()); // do not alter animation rate
        animation.setRepeatCount(Animation.INFINITE); // Repeat animation infinitely
        animation.setRepeatMode(Animation.REVERSE); // Reverse animation at the end so the button will fade back in
        final Button btn = (Button) findViewById(R.id.kill_button);
        btn.startAnimation(animation);

        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }

        vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        vib.vibrate(pattern, 0);

        final CountDownTimer ks = new CountDownTimer(ksLength * 1000, 1000) { // adjust the milli seconds here

            public void onTick(long millisUntilFinished) {

                text1.setText(""+String.format(FORMAT,
                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) - TimeUnit.HOURS.toMinutes(
                                TimeUnit.MILLISECONDS.toHours(millisUntilFinished)),
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(
                                TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))));
            }

            public void onFinish() {
                // TODO: Send messages to a list of contacts, currently only does one
                for (int i = 0; i < contactNumbers.size(); i++) {
                    try {
                        SmsManager smsManager = SmsManager.getDefault();
                        String finalMessage = emsMessage + "\n" + mLat + "\n" + mLong;
                        ArrayList<String> msgArray = smsManager.divideMessage(finalMessage);
                        // Send a message to each of the emergency contacts
                        smsManager.sendMultipartTextMessage(contactNumbers.get(i), null, msgArray, null, null);

                        Toast.makeText(getApplicationContext(), "Message Sent", Toast.LENGTH_LONG).show();
                    } catch (Exception ex) {
                        Toast.makeText(getApplicationContext(), ex.getMessage().toString(), Toast.LENGTH_LONG).show();
                        ex.printStackTrace();
                    }
                }
                // Send a message to 911 if EMS is enabled
                if (emsEnabled) {
                    try {
                        // Send a message to 911
                        // TODO: Replace with 911
                        SmsManager smsManager = SmsManager.getDefault();
                        String finalMessage = emsMessage + "\n" + mLat + "\n" + mLong;
                        ArrayList<String> msgArray = smsManager.divideMessage(finalMessage);

                        smsManager.sendMultipartTextMessage("411", null, msgArray, null, null);
                        Toast.makeText(getApplicationContext(), "Message Sent", Toast.LENGTH_LONG).show();
                    } catch (Exception ex) {
                        Toast.makeText(getApplicationContext(), ex.getMessage().toString(), Toast.LENGTH_LONG).show();
                        ex.printStackTrace();
                    }
                }
                findViewById(R.id.kill_button).setBackgroundResource(R.drawable.power_button_black);
                text1.setText("Emergency Message Sent");
                outOfTime = true;
            }
        }.start();

        killbutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                v.clearAnimation();
                r.stop();
                vib.cancel();
                if (!outOfTime) {
                    ks.cancel();
                    findViewById(R.id.kill_button).setBackgroundResource(R.drawable.power_button_green);
                    text1.setText("Emergency Message Cancelled");
                    (findViewById(R.id.main_button)).performClick();
                }
            }
        });
    }

    public List<String> getEmergencyNumbers() {
        List<String> contactNumbers = new ArrayList<String>();
        for (int i = 0; i < 5; i++) {
            String contact = sharedPref.getString("contact" + i, "");
            if (!contact.equals("") || !contact.equals(" ") || !contact.equals("555-2368")) {
                contactNumbers.add(contact);
            }
        }

        return contactNumbers;
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        // Provides a simple way of getting a device's location and is well suited for
        // applications that do not require a fine-grained location and that do not need location
        // updates. Gets the best and most recent location currently available, which may be null
        // in rare cases when a location is not available.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            mLat = (String.format("%s: %f", mLatitudeLabel,
                    mLastLocation.getLatitude()));
            mLong = (String.format("%s: %f", mLongitudeLabel,
                    mLastLocation.getLongitude()));
        } else {
            Toast.makeText(this, R.string.no_location_detected, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i("MainActivity", "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i("MainActivity", "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Builds a GoogleApiClient. Uses the addApi() method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }
}
