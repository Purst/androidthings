package com.miracom.androidthings;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.Pwm;
import com.google.firebase.database.*;

public class MainActivity extends Activity{

    private static final String TAG = MainActivity.class.getSimpleName();

    // Pin Name
    private static final String SERVO_MOTOR_PIN = "PWM1";
    private static final String LED_PIN = "BCM6";

    private Gpio mLedGpio;

    // Parameters of the servo PWM
    private static final double MIN_ACTIVE_PULSE_DURATION_MS = 1;
    private static final double MAX_ACTIVE_PULSE_DURATION_MS = 2;
    private static final double PULSE_PERIOD_MS = 20;  // Frequency of 50Hz (1000/20)

    // Parameters for the servo movement over time
    private static final double PULSE_CHANGE_PER_STEP_MS = 0.2;
    private static final int INTERVAL_BETWEEN_STEPS_MS = 1000;

    private Handler mHandler = new Handler();
    private Pwm mPwm;
    private boolean mIsPulseIncreasing = true;
    private double mActivePulseDuration;

    private FirebaseDatabase database;
    private DatabaseReference dr;

    private PeripheralManager service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        service = PeripheralManager.getInstance();

        try {

            // LED Control
            mLedGpio = service.openGpio(LED_PIN);
            mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            // Servo Motor control
            mPwm = service.openPwm(SERVO_MOTOR_PIN);

            //mActivePulseDuration = MIN_ACTIVE_PULSE_DURATION_MS;

            // Always set frequency and initial duty cycle before enabling PWM
            mPwm.setPwmFrequencyHz(1000 / PULSE_PERIOD_MS);
            mPwm.setPwmDutyCycle(100 * MAX_ACTIVE_PULSE_DURATION_MS / PULSE_PERIOD_MS);
            mPwm.setEnabled(true);

            database = FirebaseDatabase.getInstance();
            dr = FirebaseDatabase.getInstance().getReference();
            dr.child("led").addValueEventListener(new ValueEventListener() {

                @Override
                public void onDataChange(DataSnapshot dataSnapshot){

                    String sLedCd = (String) dataSnapshot.getValue();

                    try {
                        if("on".equals(sLedCd)){
                            mLedGpio.setValue(true);
                        }
                        else{
                            mLedGpio.setValue(false);
                        }
                    } catch ( Exception e) {
                        Log.e(TAG, "LED Error!", e);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });

            dr.child("door").addValueEventListener(new ValueEventListener() {

                @Override
                public void onDataChange(DataSnapshot dataSnapshot){

                    String sDoorCd = (String) dataSnapshot.getValue();

                    try {
                        if("open".equals(sDoorCd)){
                            mPwm.setPwmDutyCycle(100 * MIN_ACTIVE_PULSE_DURATION_MS/ PULSE_PERIOD_MS);
                        }
                        else{
                            mPwm.setPwmDutyCycle(100 * MAX_ACTIVE_PULSE_DURATION_MS / PULSE_PERIOD_MS);
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Door Error!", e);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });

        } catch (Exception e) {
            Log.e(TAG, "Error!", e);
        }
    }
}
