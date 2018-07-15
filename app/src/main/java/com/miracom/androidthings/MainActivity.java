package com.miracom.androidthings;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.pio.Gpio;

import java.io.IOException;
import java.util.*;

import com.google.android.things.pio.PeripheralManager;
import com.google.firebase.database.*;
/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Gpio mLedGpio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        initailize();

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("message");
        myRef.setValue("Hello, World!");

        DatabaseReference dr = FirebaseDatabase.getInstance().getReference();

        dr.child("led").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                //Post post = dataSnapshot.getValue(Post.class);
                // ...

                String switchLED = dataSnapshot.getValue().toString();
                Log.e("육심현만세", switchLED);
                if("on".equals(switchLED)){
                    setLedValue(true);
                }
                else{
                    setLedValue(false);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                //Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                // ...
            }
        });

/*        dr.child("ledBlink").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Get Post object and use the values to update the UI
                //Post post = dataSnapshot.getValue(Post.class);
                // ...

                String switchLED = dataSnapshot.getValue().toString();
                Log.e("육심현만세", switchLED);
                if("on".equals(switchLED)){
                    setLedValue(false);
                    blink();
                }
                else{
                    setLedValue(false);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Getting Post failed, log a message
                //Log.w(TAG, "loadPost:onCancelled", databaseError.toException());
                // ...
            }
        });*/



    }

    //초기화
    public void initailize(){

        PeripheralManager pioService = PeripheralManager.getInstance();

        try {
            /*  LED 설정 */
            Log.i(TAG, "Configuring GPIO pins");
            mLedGpio = pioService.openGpio("BCM6");
            mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);


        } catch (IOException e) {
            Log.e(TAG, "Error configuring GPIO pins", e);
        }

    }

    private void blink(){
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                try{

                    Log.e(TAG, "완료");

                    if(mLedGpio.getValue() == false){
                        setLedValue(true);
                    }
                    else {
                        setLedValue(false);
                    }
                }
                catch (Exception e){

                }
            }
        }, 0, 1000L);


    }

    /**
     * Update the value of the LED output.
     */
    private void setLedValue(boolean value) {
        try {
            mLedGpio.setValue(value);
        } catch (IOException e) {
            Log.e(TAG, "Error updating GPIO value", e);
        }
    }
}
