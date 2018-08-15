package com.miracom.androidthings;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.Pwm;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.nio.ByteBuffer;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    // Pin Name
    private static final String SERVO_MOTOR_PIN = "PWM1";
    private static final String LED_PIN = "BCM6";

    // Device Class
    private Gpio mLedGpio;
    private Pwm mPwm;
    private DoorbellCamera mCamera;

    // Parameters of the servo PWM
    private static final double MIN_ACTIVE_PULSE_DURATION_MS = 1;
    private static final double MAX_ACTIVE_PULSE_DURATION_MS = 2;
    private static final double PULSE_PERIOD_MS = 20;  // Frequency of 50Hz (1000/20)

    private FirebaseDatabase database;
    private FirebaseStorage mStorage;
    private DatabaseReference dr;

    private PeripheralManager service;

    // for running Camera tasks in the background.
    private Handler mCameraHandler;
    // An additional thread for running Camera tasks that shouldn't block the UI.
    private HandlerThread mCameraThread;

    /**
     * Listener for new camera images.
     */
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = reader.acquireLatestImage();
                    // get image bytes
                    ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
                    final byte[] imageBytes = new byte[imageBuf.remaining()];
                    imageBuf.get(imageBytes);
                    image.close();

                    onPictureTaken(imageBytes);
                }
            };

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

            // Always set frequency and initial duty cycle before enabling PWM
            mPwm.setPwmFrequencyHz(1000 / PULSE_PERIOD_MS);
            mPwm.setPwmDutyCycle(100 * MAX_ACTIVE_PULSE_DURATION_MS / PULSE_PERIOD_MS);
            mPwm.setEnabled(true);

            // We need permission to access the camera
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // A problem occurred auto-granting the permission
                Log.e(TAG, "No permission");
            }

            mCamera = DoorbellCamera.getInstance();
            mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener);

            database = FirebaseDatabase.getInstance();
            mStorage = FirebaseStorage.getInstance();

            dr = FirebaseDatabase.getInstance().getReference();
            dr.child("led").addValueEventListener(new ValueEventListener() {

                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    String sLedCd = getValue(dataSnapshot, "power");

                    try {
                        if ("on".equals(sLedCd)) {
                            mLedGpio.setValue(true);
                        } else {
                            mLedGpio.setValue(false);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "LED Error!", e);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });

            dr.child("door").addValueEventListener(new ValueEventListener() {

                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    String sPercent = getValue(dataSnapshot, "percent");
                    double offset;

                    if ("".equals(sPercent)) {
                        offset = 0;
                    } else {
                        offset = 1.0 * (Double.parseDouble(sPercent) / 100);
                    }

                    try {
                        mPwm.setPwmDutyCycle(100 * (MAX_ACTIVE_PULSE_DURATION_MS - offset) / PULSE_PERIOD_MS);
                    } catch (Exception e) {
                        Log.e(TAG, "Door Error!", e);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });

            dr.child("camera").addValueEventListener(new ValueEventListener() {

                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {



                    String sValue = getValue(dataSnapshot, "take");

                    Log.i(TAG, "Camera -> " + sValue);

                    if ("on".equals(sValue)) {
                        mCamera.takePicture();
                        dr.child("camera").child("take").child("value").setValue("off");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error!", e);
        }
    }

    /**
     * Upload image data to Firebase as a doorbell event.
     */
    private void onPictureTaken(final byte[] imageBytes) {
        if (imageBytes != null) {
            final DatabaseReference log = database.getReference("logs").push();
            final StorageReference imageRef = mStorage.getReference().child("capture");

            // upload image to storage
            UploadTask task = imageRef.putBytes(imageBytes);
            task.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    // mark image in the database
                    Log.i(TAG, "Image upload successful");
                    log.child("timestamp").setValue(ServerValue.TIMESTAMP);
                    log.child("image").setValue(downloadUrl.toString());

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // clean up this entry
                    Log.w(TAG, "Unable to upload image to Firebase");
                    log.removeValue();
                }
            });
        }
    }

    private String getValue(DataSnapshot dataSnapshot, String itemNm) {

        for (DataSnapshot item : dataSnapshot.getChildren()) {

            String name = item.getKey().toString();

            if (itemNm.equals(name)) {

                for (DataSnapshot value : item.getChildren()) {

                    return value.getValue().toString();
                }
            }
        }
        return "";
    }
}
