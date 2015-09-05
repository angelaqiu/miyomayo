package com.example.angelaq.miyomayo;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import java.util.Locale;

public class GettingSensorData extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensSensorManager;
    private Sensor senAccelerometer;
    private long lastUpdate = 0;
    private float last_x,last_y,last_z;
    private float prev_x,prev_y,prev_z;
    private static final int DROP_THRESHOLD = 300;
    private static final int REST_THRESHOLD = 10;
    TextToSpeech t1;
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){}
    @Override
    public void onSensorChanged(SensorEvent sensorEvent){
        Sensor mySensor = sensorEvent.sensor;
        t1=new TextToSpeech(getApplicationContext(),new TextToSpeech.onInitListener() {
            @Override
            public void onInit(int status) {
                super.onInit();
                if (status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                }
            }
            @Override
            public void onInitListener() {
                super.onInitListener();
            }
        });
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            long curTime = System.currentTimeMillis();
 
            if ((curTime - lastUpdate) > 100) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;
                float last_speed = Math.abs(last_x + last_y + last_z - prev_x - prev_y - prev_z) / diffTime * 10000;;
                float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;
                if (last_speed > DROP_THRESHOLD && speed > REST_THRESHOLD) {
                    Log.i("X","PHONE HAS DROPPED");
                    CharSequence i;
                    i = "OW";
                    t1.speak(i,TextToSpeech.QUEUE_FLUSH,null,"x");
                }
            }
            prev_x = last_x;
            prev_y = last_y;
            prev_z = last_z;
            last_x = x;
            last_y = y;
            last_z = z;
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_getting_sensor_data);
        sensSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = sensSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensSensorManager.registerListener(this, senAccelerometer,SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensSensorManager.unregisterListener(this);
        if (t1 != null) {
            t1.stop();
            t1.shutdown();
        }
        super.onPause();
    }
    @Override
    protected void onResume() {
        super.onResume();
        sensSensorManager.registerListener(this,senAccelerometer,SensorManager.SENSOR_DELAY_NORMAL);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_getting_sensor_data, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
