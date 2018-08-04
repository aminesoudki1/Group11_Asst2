package com.example.amine.group11_asst2;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;

import static com.example.amine.group11_asst2.MainActivity.TABLE_NAME;
import static com.example.amine.group11_asst2.MainActivity.TIMESTAMP_TO_SERVICE;

public class AccelerometerService extends Service implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mSensor;
    DBHelper dbHelper;
    SQLiteDatabase db;
    long startTime = 0;
    String table_name;
    int timestamp;
    @Override
    public void onCreate() {
        super.onCreate();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Registering...
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);

        // Get default sensor type
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Gets the data repository in write mode

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Get sensor manager on starting the service.
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Registering...
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);

        // Get default sensor type
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if(intent.hasExtra(TABLE_NAME)) {
            table_name = intent.getStringExtra(TABLE_NAME);
        }

        dbHelper = new DBHelper(this,"CSE535_ASSIGNMENT2",table_name);

        db = dbHelper.getWritableDatabase();
        //delete table if it exists and craete new instance
        //this is because
        if(intent.hasExtra(TIMESTAMP_TO_SERVICE)) {
            timestamp = intent.getIntExtra(TIMESTAMP_TO_SERVICE,0);
        }
        //timestamp is 0 so we are running a new instance of the app
        if(timestamp == 0) {
            db.execSQL("DROP TABLE IF EXISTS " + table_name);
        }
//id INTEGER PRIMARY KEY AUTOINCREMENT,
        db.execSQL("CREATE TABLE IF NOT EXISTS "+ table_name + "(Timestamp INTEGER," +
                "XVAL REAL," +
                "YVAL REAL," +
                "ZVAL REAL )");
        return START_NOT_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long millis = System.currentTimeMillis() - startTime;

        if(millis>1000) {

            Log.d("time",millis+"");
            startTime  = System.currentTimeMillis();

            ContentValues values = new ContentValues();

            float x= event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            values.put("XVAL", x);
            values.put("YVAL", y);
            values.put("ZVAL", z);
            values.put("TIMESTAMP", timestamp);


// Insert the new row, returning the primary key value of the new row
            long newRowId = db.insert(table_name, null, values);
            Log.e("NEWINSERT", newRowId+" " + timestamp + " ("+ x + " " + y + " " +z +")");
            sendMessage(timestamp,x,y,z);
            timestamp = timestamp + 1;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void sendMessage(int timestamp , float x ,float y ,float z) {
        Log.d("sender", "Broadcasting message");
        Intent intent = new Intent("UPDATEUI");
        // You can also include some extra data.
        intent.putExtra("TIMESTAMP", timestamp);
        intent.putExtra("X", x);
        intent.putExtra("Y", y);
        intent.putExtra("Z", z);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this,mSensor);
        super.onDestroy();
    }

    @Override
    public boolean stopService(Intent name) {
        return super.stopService(name);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


}
