package edu.uw.cyclife.cyclifeapp;
//
//import android.os.Bundle;
//import android.os.CountDownTimer;
//import android.view.View;
//import android.widget.Button;
//import android.widget.TextView;
//import android.view.View.OnClickListener;
//import android.app.Activity;
//
///**
// * Created by keegangriffee on 4/23/17.
// */
//
//public class KillswitchActivity extends Activity implements OnClickListener {
//    private CountDownTimer countDownTimer;
//    private boolean timerHasStarted = false;
//    private Button startB;
//    public TextView text;
//
//    private final long startTime = 30 * 1000;
//    private final long interval = 1 * 1000;
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.kill_switch_timer);
//        startB = (Button) this.findViewById(R.id.button);
//        startB.setOnClickListener(this);
//        text = (TextView) this.findViewById(R.id.timer);
//        countDownTimer = new MyCountDownTimer(startTime, interval);
//        text.setText(text.getText() + String.valueOf(startTime / 1000));
//    }
//
//    @Override
//    public void onClick(View v) {
//        if (!timerHasStarted) {
//            countDownTimer.start();
//            timerHasStarted = true;
//            startB.setText("STOP");
//        } else {
//            countDownTimer.cancel();
//            timerHasStarted = false;
//            startB.setText("RESTART");
//        }
//    }
//
//    public class MyCountDownTimer extends CountDownTimer {
//        public MyCountDownTimer(long startTime, long interval) {
//            super(startTime, interval);
//        }
//
//        @Override
//        public void onFinish() {
//            text.setText("Time's up!");
//        }
//
//        @Override
//        public void onTick(long millisUntilFinished) {
//            text.setText("" + millisUntilFinished / 1000);
//        }
//    }
//}

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;


public class KillswitchActivity extends Activity {
    TextView text1;

    private static final String FORMAT = "%02d:%02d:%02d";

    int seconds , minutes;


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.kill_switch_timer);

        text1=(TextView)findViewById(R.id.timer_countdown);

        final Button killbutton = (Button)findViewById(R.id.kill_button);
        killbutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO: Kill timer, go back to home screen
            }
        });

        new CountDownTimer(16069000, 1000) { // adjust the milli seconds here

            public void onTick(long millisUntilFinished) {

                text1.setText(""+String.format(FORMAT,
                        TimeUnit.MILLISECONDS.toHours(millisUntilFinished),
                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) - TimeUnit.HOURS.toMinutes(
                                TimeUnit.MILLISECONDS.toHours(millisUntilFinished)),
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(
                                TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished))));
            }

            public void onFinish() {
                text1.setText("done!");
            }
        }.start();


    }

}
