package edu.uw.cyclife.cyclifeapp;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.audiofx.BassBoost;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.R.id.message;


public class KillswitchActivity extends AppCompatActivity {
    TextView text1;
    TextView infoText;
    boolean outOfTime;
    Ringtone r;
    Vibrator vib;
    private List<String> contactNumbers;

    private static final String FORMAT = "%02d:%02d";

    int seconds , minutes;
    // Start without a delay
    // Vibrate for 100 milliseconds
    // Sleep for 1000 milliseconds
    long[] pattern = {0, 100, 1000};

    SharedPreferences sharedPref = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.kill_switch_timer);

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
        String emergencyMessage = sharedPref.getString("EmergencyMessage", "");
        contactNumbers = getEmergencyNumbers();

        text1=(TextView)findViewById(R.id.timer_countdown);
        infoText=(TextView)findViewById(R.id.info_message);
        infoText.setText("Press the killswitch to disable the emergency message!");

        emsEnabled = sharedPref.getBoolean("911Enabled", false);
        final String emsMessage = sharedPref.getString("UserName", "") + " " +
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
                        ArrayList<String> msgArray = smsManager.divideMessage(emsMessage);
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
                        SmsManager smsManager = SmsManager.getDefault();
                        ArrayList<String> msgArray = smsManager.divideMessage(emsMessage);
                        // Send a message to 911
                        // TODO: Replace with 911
                        smsManager.sendMultipartTextMessage("9999999999", null, msgArray, null, null);
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
                }
            }
        });
    }

    public List<String> getEmergencyNumbers() {
        List<String> contactNumbers = new ArrayList<String>();
        for (int i = 0; i < 5; i++) {
            String contact = sharedPref.getString("contact" + i, "");
            if (!contact.equals("") || !contact.equals(" ") || !contact.equals("5552368")) {
                contactNumbers.add(contact);
            }
        }

        return contactNumbers;
    }
}
