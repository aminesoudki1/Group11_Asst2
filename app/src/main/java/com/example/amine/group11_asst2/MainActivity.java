package com.example.amine.group11_asst2;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.stetho.Stetho;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.UniqueLegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.Series;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity  {

    public static String TAG = "MainActivity";
    public static String TABLE_NAME = "TABLE_NAME";
    public static String TIMESTAMP_TO_SERVICE = "TIMESTAMP_TO_SERVICE";
    @BindView(R.id.et_patient_name)
    EditText et_patient_name;

    @BindView(R.id.et_age)
    EditText et_age;

    @BindView(R.id.tv_progress)
    TextView tv_progress;

    @BindView(R.id.et_patient_id)
    EditText et_patient_id;

    @BindView(R.id.bt_run)
    Button bt_run;

    @BindView(R.id.bt_stop)
    Button bt_stop;

    @BindView(R.id.bt_download)
    Button bt_download;

    @BindView(R.id.bt_upload)
    Button bt_upload;

    @BindView(R.id.rg_sex)
    RadioGroup rg_sex;

    @BindView(R.id.rb_male)
    RadioButton rb_male;

    @BindView(R.id.rb_female)
    RadioButton rb_female;

    @BindView(R.id.gv_graph)
    GraphView graph;

    //accelerometer x,y,z values stored in lineseries
    LineGraphSeries xvalues;
    LineGraphSeries yvalues;
    LineGraphSeries zvalues;
    //timestamp
    int timestamp = 0;
    boolean running = false;
    boolean userChanged = false;
    Intent t;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this); //bind xml to activity
        Stetho.initializeWithDefaults(this); //so we can check databases in the browser

        initGraph();
        //detecting if any of the inputs have been changed, then we detected a user change
        TextWatcher textWatcher = new TextWatcher() {
            CharSequence charSequence;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                charSequence = s;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(!s.equals(charSequence)) {
                    userChanged = true;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
        rg_sex.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton checkedRadioButton = (RadioButton) group.findViewById(checkedId);
                // This puts the value (true/false) into the variable
                boolean isChecked = checkedRadioButton.isChecked();
                // If the radiobutton that has changed in check state is now checked...
                if (isChecked)
                {
                    userChanged = true;
                }
            }
        });
        et_age.addTextChangedListener(textWatcher);
        et_patient_id.addTextChangedListener(textWatcher);
        et_patient_name.addTextChangedListener(textWatcher);
    }
    public void initGraph() {
        //initialize the graph by setting the legend and colors of the line series
        graph.getViewport().setScalable(true); // enables horizontal zooming and scrolling
        graph.getViewport().setScalableY(true); // enables vertical zooming and scrolling

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);


        xvalues = new LineGraphSeries();
        yvalues = new LineGraphSeries();
        zvalues = new LineGraphSeries();

        xvalues.setTitle("XVAL");
        yvalues.setTitle("YVAL");
        zvalues.setTitle("ZVAL");
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        xvalues.setColor(Color.RED);
        yvalues.setColor(Color.YELLOW);
        graph.addSeries(xvalues);
        graph.addSeries(yvalues);
        graph.addSeries(zvalues);
    }
    @Override
    protected void onResume() {
        //registering local broadcast receiver to receive updates from the accelerometer service
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("UPDATEUI"));
        super.onResume();
    }

    @Override
    protected void onPause() {
        //unregistering local broadcast receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
                mMessageReceiver);
        super.onPause();

    }

    @OnClick(R.id.bt_run)
    public void run() {

        //if no input is detected, prompt the user to input the 4 fields
        if(((et_age.getText().toString().isEmpty() || et_patient_id.getText().toString().isEmpty() ||
                et_patient_name.getText().toString().isEmpty()) || (!rb_female.isChecked() &&
                !rb_male.isChecked())) && !running) {

            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("ALERT");
            alertDialog.setMessage("Please Enter Patient Information!");
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }

        else if(!running) {
            et_age.setEnabled(false);
            et_patient_name.setEnabled(false);
            et_patient_id.setEnabled(false);
            rb_male.setEnabled(false);
            rb_female.setEnabled(false);
            bt_download.setEnabled(false);
            bt_upload.setEnabled(false);
            if(userChanged) {
                graph.removeAllSeries();
                initGraph();
                userChanged = false;
                timestamp = 0;
            }
            if(graph.getSeries().size()==0) {

                graph.addSeries(xvalues);
                graph.addSeries(yvalues);
                graph.addSeries(zvalues);
            }
            String table_name = et_patient_name.getText().toString()+""+et_patient_id.getText().toString()+""+et_age.getText().toString();
            if(rb_male.isChecked()) {
                table_name = table_name+"male";
            } else if(rb_female.isChecked()) {
                table_name = table_name +"female";
            }
            //send the table name and timestamp to the acceleromter service to start recording accelerometer data
            t = new Intent(this,AccelerometerService.class);
            t.putExtra(TABLE_NAME,table_name);
            t.putExtra(TIMESTAMP_TO_SERVICE,timestamp);
            this.startService(t);
        }
    }
    @OnClick(R.id.bt_stop)
    public void stop() {
        if(t!=null) {
            Log.d("MainActivity Stop", stopService(t) + "");

        }
        graph.removeAllSeries();
        et_age.setEnabled(true);
        et_patient_name.setEnabled(true);
        et_patient_id.setEnabled(true);
        rb_male.setEnabled(true);
        rb_female.setEnabled(true);
        bt_download.setEnabled(true);
        bt_upload.setEnabled(true);

    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            //update UI
            float x =0,y=0,z=0;

            if(intent.hasExtra("X")) {
                x = intent.getFloatExtra("X",0);
            }

            if(intent.hasExtra("Y")) {
                y = intent.getFloatExtra("Y",0);
            }

            if(intent.hasExtra("Z")) {
                z = intent.getFloatExtra("Z",0);
            }
            if(intent.hasExtra("TIMESTAMP")) {
                timestamp = intent.getIntExtra("TIMESTAMP",0);
            }
            Log.d("receiver", "Got message: " +timestamp+ " " + x +" " + y + " " + z );

            xvalues.appendData(new DataPoint(timestamp,x),true,1000);
            yvalues.appendData(new DataPoint(timestamp,y),true,1000);
            zvalues.appendData(new DataPoint(timestamp,z),true,1000);

        }
    };

    @OnClick(R.id.bt_upload)
    public void uploadToServer() {
        //Uploading database
        UploadFile uploadFile = new UploadFile();
        uploadFile.execute(this.getDatabasePath("CSE535_ASSIGNMENT2").getPath());
    }

    @OnClick(R.id.bt_download)
    public void downloadFromServer() {
        //downloading database
        DownloadFile downloadFile = new DownloadFile();
        downloadFile.execute("http://impact.asu.edu/CSE535Spring18Folder/group11_asst2.db"
                ,this.getDatabasePath("CSE535_ASSIGNMENT2").getParent() + "/CSE535_ASSIGNMENT2_DOWN.db");


    }
    private class DownloadFile extends AsyncTask<String,Integer,String> {
        int serverResponseCode;
        @Override
        protected String doInBackground(String... strings) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            //trust manager is not used because impact lab has not implemented any https connection
            //http connection will be used instead of https
            //-----------------------------------------------------------------------
            /*TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    // Not implemented
                }

                @Override
                public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    // Not implemented
                }
            } };

            try {
                SSLContext sc = SSLContext.getInstance("TLS");

                sc.init(null, trustAllCerts, new java.security.SecureRandom());

                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (KeyManagementException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }*/
            //-------------------------------------------------------------------------------
            try {
                URL url = new URL(strings[0]); //connect to impact lab
                connection = (HttpURLConnection) url.openConnection();

                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                    Log.d("download",connection.getResponseCode() + " " +connection.getResponseMessage());
                }
                serverResponseCode = connection.getResponseCode();
                int fileLength = connection.getContentLength(); //get file to download

                input = connection.getInputStream();
                Log.d("download",strings[1] + " " +fileLength);
                output = new FileOutputStream(strings[1]);
                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) { //while there is more content to read continue reading

                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0)
                        publishProgress((int) total,fileLength);
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }

            return null;
        }

        @Override
        protected void onPreExecute() {
            //before execution UI update
            tv_progress.setText("Preparing to Download");
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            //during execution
            tv_progress.setText("Downloading " + values[0] +" out of " + values[1]);
        }

        @Override
        protected void onPostExecute(String s) {

            //after execution
            //if the response is 200 meaning everything was OK
            if(serverResponseCode==200) {
                Toast.makeText(MainActivity.this, "Download Complete", Toast.LENGTH_LONG).show();
                tv_progress.setText("");
                //open newly downloaded database
                DBHelper db = new DBHelper(getApplicationContext(),"CSE535_ASSIGNMENT2_DOWN.db","");

                SQLiteDatabase sqLiteDatabase = db.getReadableDatabase();
                //get all tables from that database
                Cursor c = sqLiteDatabase.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name!= 'android_metadata'",null);
                Log.e("table",c.getPosition()+"");
                if (c.moveToFirst()) {
                    //get the first table and plot the first 10 sec
                    String table_name = c.getString(c.getColumnIndex("name"));

                    Cursor d = sqLiteDatabase.rawQuery("SELECT * FROM" + " " + table_name +" LIMIT 10",null );

                    graph.removeAllSeries();
                    initGraph(); //reset graph
                    userChanged = true;
                    if(d.moveToFirst()) {
                        //iterate over the values queried from the database and plot them in the graph
                        while(!d.isAfterLast()) {
                            int timestamp = d.getInt(d.getColumnIndex("Timestamp"));
                            float x = d.getFloat(d.getColumnIndex("XVAL"));
                            float y = d.getFloat(d.getColumnIndex("YVAL"));
                            float z = d.getFloat(d.getColumnIndex("ZVAL"));
                            xvalues.appendData(new DataPoint(timestamp,x),true,1000);
                            yvalues.appendData(new DataPoint(timestamp,y),true,1000);
                            zvalues.appendData(new DataPoint(timestamp,z),true,1000);
                            Log.e("table",x+" "+y+" "+ z);
                            d.moveToNext();
                        }
                    }
                }
            }

            //if the file is not found, alert the user
            else if(serverResponseCode==404){
                tv_progress.setText("File Not Found");
            }
        }
    }
    private class UploadFile extends AsyncTask<String,Integer,String> {

        int serverResponseCode;
        @Override
        protected String doInBackground(String... strings) {
            String fileName = strings[0];
            HttpURLConnection conn = null;
            DataOutputStream dos = null;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1024*1024 ;
            File sourceFile = new File(fileName);
            Log.d("PATH",sourceFile.getPath());
            try {
                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL("http://impact.asu.edu/CSE535Spring18Folder/UploadToServer.php");

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                //connection properties set as per POST operation
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                //uploaded file required by UploadtoServer.php script
                conn.setRequestProperty("uploaded_file", fileName);

                dos = new DataOutputStream(conn.getOutputStream());


                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"group11_asst2.db" + "\"" + lineEnd);

                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();
                int filesize = bytesAvailable;
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {

                    //while file has been fully read and sent, read data and send to impact lab
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    publishProgress(bytesAvailable,filesize);
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                //show progress to user (x out of y bytes uploaded)
                publishProgress(0,filesize);
                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();


                Log.i("uploadFile", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);


                //cleanup
                //close the streams and the connection//
                fileInputStream.close();
                dos.flush();
                dos.close();
                conn.disconnect();

            } catch (MalformedURLException ex) {
                ex.printStackTrace();
                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Upload file to server", "error" + serverResponseCode);
                tv_progress.setText("Create Database First");

            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            //progress text change (UI update)
            tv_progress.setText("Uploading...");
        }
        @Override
        protected void onPostExecute(String s) {
           //if upload is successful a toast msg is diplayed to the user
            if(serverResponseCode==200) {
                Toast.makeText(MainActivity.this, "Uploaded Successfully", Toast.LENGTH_LONG).show();
                tv_progress.setText("");
            } else {
                Toast.makeText(getApplicationContext(),"Something Went Wrong!",Toast.LENGTH_SHORT);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            tv_progress.setText("Uploading " + values[0] +" out of " + values[1]);
        }

        @Override
        protected void onCancelled() {
            Toast.makeText(MainActivity.this, "UPLOAD CANCELLED", Toast.LENGTH_LONG).show();
            tv_progress.setText("");
        }
    }
}
