/*
 * Copyright (C) 2014 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.angelaq.miyomayo;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewDebug;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.HistoryApi;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.result.DailyTotalResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public class GooglePlayServicesActivity extends ActionBarActivity implements SensorEventListener {
    public static final String TAG = "BasicSensorsApi";
    // [START auth_variable_references]
    private static final int REQUEST_OAUTH = 1;

    TextView txt;
    private Sensor senAccelerometer;
    private Sensor senGyroscope;
    private long lastUpdate = 0;
    private float z;
    private float last_z;
    private float prev_z;
    private static final int DROP_THRESHOLD = 300;
    private static final int REST_THRESHOLD = 10;
    TextToSpeech t1;
    private SensorManager manager;
    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {}
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            z = sensorEvent.values[2];
            long curTime = System.currentTimeMillis();
            if ((curTime - lastUpdate) > 100) {
                Log.i("z", String.valueOf(z));
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;
                float last_speed = Math.abs(last_z - prev_z) / diffTime * 10000;
                float speed = Math.abs(z - last_z) / diffTime * 10000;
                if (last_speed > DROP_THRESHOLD && speed > REST_THRESHOLD) {
                    Log.i("X", "PHONE HAS DROPPED");
                    CharSequence i;
                    i = "OW";
//                    t1.speak(i, TextToSpeech.QUEUE_FLUSH, null, "x");
                    loseHP();
                    t1.speak(i, TextToSpeech.QUEUE_FLUSH, null, "x");
                    loseHP();
                }
            }
        }
        else if (mySensor.getType() == Sensor.TYPE_GYROSCOPE) {
            long curr = System.currentTimeMillis();
            if ((curr - lastUpdate) > 100) {
                lastUpdate = curr;
                float gZ = sensorEvent.values[2];
                Log.i("gZ",String.valueOf(gZ));
                if (gZ >= 3) {
                    Log.i("Z", "PHONE HAS TURNED");
                    CharSequence i;
                    i = "WHEEEEEEE";
                    t1.speak(i, TextToSpeech.QUEUE_FLUSH, null, "z");
                }
            }
        }
        prev_z = last_z;
        last_z = z;
    }

    /**
     *  Track whether an authorization activity is stacking over the current activity, i.e. when
     *  a known auth error is being resolved, such as showing the account chooser or presenting a
     *  consent dialog. This avoids common duplications as might happen on screen rotations, etc.
     */
    private static final String AUTH_PENDING = "auth_state_pending";
    private boolean authInProgress = false;

    private GoogleApiClient mClient = null;
    // [END auth_variable_references]

    // [START mListener_variable_reference]
    // Need to hold a reference to this listener, as it's passed into the "unregister"
    // method in order to stop all sensors from sending data to this listener.
    private OnDataPointListener mListener;
    // [END mListener_variable_reference]


    // [START auth_oncreate_setup_beginning]
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Put application specific code here.
        // [END auth_oncreate_setup_beginning]
        setContentView(R.layout.activity_main);
        // This method sets up our custom logger, which will print all log messages to the device
        // screen, as well as to adb logcat.
//        initializeLogging();

        txt = (TextView)findViewById(R.id.testtxt);

        // [START auth_oncreate_setup_ending]

        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }

        buildFitnessClient();
        manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senGyroscope = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        manager.registerListener(this, senAccelerometer,SensorManager.SENSOR_DELAY_NORMAL);
        manager.registerListener(this, senGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        t1=new TextToSpeech(this,new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    t1.setLanguage(Locale.UK);
                }
            }

        });
    }
    // [END auth_oncreate_setup_ending]

    // [START auth_build_googleapiclient_beginning]
    /**
     *  Build a {@link GoogleApiClient} that will authenticate the user and allow the application
     *  to connect to Fitness APIs. The scopes included should match the scopes your app needs
     *  (see documentation for details). Authentication will occasionally fail intentionally,
     *  and in those cases, there will be a known resolution, which the OnConnectionFailedListener()
     *  can address. Examples of this include the user never having signed in before, or having
     *  multiple accounts on the device and needing to specify which account to use, etc.
     */
    private void buildFitnessClient() {
        // Create the Google API Client
        mClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.HISTORY_API)
//                .addScope(new Scope(Scopes.FITNESS_LOCATION_READ))
                .addConnectionCallbacks(
                        new GoogleApiClient.ConnectionCallbacks() {

                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.i(TAG, "Connected!!!");

                                PendingResult<DailyTotalResult> result = Fitness.HistoryApi.readDailyTotal(mClient, DataType.TYPE_STEP_COUNT_DELTA);
                                result.setResultCallback(new ResultCallback<DailyTotalResult>() {
                                    @Override
                                    public void onResult(DailyTotalResult dailyTotalResult) {
                                        DataSet totalSet = dailyTotalResult.getTotal();
                                        long total = totalSet.isEmpty()
                                                ? 0
                                                : totalSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                                        txt.setText("# of steps:" + total);
                                    }
                                });
                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                // If your connection to the sensor gets lost at some point,
                                // you'll be able to determine the reason and react to it here.
                                if (i == ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                    Log.i(TAG, "Connection lost.  Cause: Network Lost.");
                                } else if (i == ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                    Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
                                }
                            }
                        }
                )
                .addOnConnectionFailedListener(
                        new GoogleApiClient.OnConnectionFailedListener() {
                            // Called whenever the API client fails to connect.
                            @Override
                            public void onConnectionFailed(ConnectionResult result) {
                                Log.i(TAG, "Connection failed. Cause: " + result.toString());
                                if (!result.hasResolution()) {
                                    // Show the localized error dialog
                                    GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(),
                                            GooglePlayServicesActivity.this, 0).show();
                                    return;
                                }
                                // The failure has a resolution. Resolve it.
                                // Called typically when the app is not yet authorized, and an
                                // authorization dialog is displayed to the user.
                                if (!authInProgress) {
                                    try {
                                        Log.i(TAG, "Attempting to resolve failed connection");
                                        authInProgress = true;
                                        result.startResolutionForResult(GooglePlayServicesActivity.this,
                                                REQUEST_OAUTH);
                                    } catch (IntentSender.SendIntentException e) {
                                        Log.e(TAG,
                                                "Exception while starting resolution activity", e);
                                    }
                                }
                            }
                        }
                )
                .build();
    }
    // [END auth_build_googleapiclient_ending]

    // [START auth_connection_flow_in_activity_lifecycle_methods]
    @Override
    protected void onStart() {
        super.onStart();
        // Connect to the Fitness API
        Log.i(TAG, "Connecting...");
        mClient.connect();
        manager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        manager.registerListener(this, senGyroscope, SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mClient.isConnected()) {
            mClient.disconnect();
        }
        if (t1 != null) {
            t1.stop();
        }
        t1.shutdown();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OAUTH) {
            authInProgress = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mClient.isConnecting() && !mClient.isConnected()) {
                    mClient.connect();
                }
            }
        }
    }

    public void addHP() {
        Log.v(TAG, "adding HP");
        RelativeLayout lay = (RelativeLayout)findViewById(R.id.healthbar);
        if (lay.getLayoutParams().width <= 300) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(lay.getLayoutParams().width + 20,
                    lay.getLayoutParams().height);
            params.setMargins(371, 1222, 0, 0);
            lay.setLayoutParams(params);
        }
        if (lay.getLayoutParams().width > 300) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(300, lay.getLayoutParams().height);
            params.setMargins(371, 1222, 0, 0);
            lay.setLayoutParams(params);
        }

        if (lay.getLayoutParams().width > 60) {
            ImageView img= (ImageView) findViewById(R.id.imageView);
            img.setImageResource(R.drawable.mm_sprite_01x);
        }
    }

    public void loseHP() {
        RelativeLayout lay = (RelativeLayout)findViewById(R.id.healthbar);
        if (lay.getLayoutParams().width > 0) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(lay.getLayoutParams().width - 10,
                    lay.getLayoutParams().height);
            params.setMargins(371, 1222, 0, 0);
            lay.setLayoutParams(params);
        }

        if (lay.getLayoutParams().width < 0) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(0, lay.getLayoutParams().height);
            params.setMargins(371, 1222, 0, 0);
            lay.setLayoutParams(params);
        }

        if (lay.getLayoutParams().width <= 60) {
            ImageView img= (ImageView) findViewById(R.id.imageView);
            img.setImageResource(R.drawable.mm_sprite_01xsadd);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(AUTH_PENDING, authInProgress);
    }
    // [END auth_connection_flow_in_activity_lifecycle_methods]

    public void sendOw(View v) {
        loseHP();
        Log.v(TAG, "ow!!!!!!");

    }

    public void sendFit(View v) {
        addHP();
        Log.v(TAG, "fit!!!!!!");
    }

    public void buttonUpdate(View v) {
        PendingResult<DailyTotalResult> result = Fitness.HistoryApi.readDailyTotal(mClient, DataType.TYPE_STEP_COUNT_DELTA);
        result.setResultCallback(new ResultCallback<DailyTotalResult>() {
            @Override
            public void onResult(DailyTotalResult dailyTotalResult) {
                DataSet totalSet = dailyTotalResult.getTotal();
                long total = totalSet.isEmpty()
                        ? 0
                        : totalSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();
                txt.setText("# of steps:" + total);
                if (total > 5000) {
                    Toast.makeText(GooglePlayServicesActivity.this, "Congratulations on hitting your goal! +20HP", Toast.LENGTH_LONG).show();
                    Button butt = (Button) findViewById(R.id.refreshButton);
                    butt.setEnabled(false);
                    addHP();
                }
            }
        });
        Log.v(TAG, "updaaate");
    }
}