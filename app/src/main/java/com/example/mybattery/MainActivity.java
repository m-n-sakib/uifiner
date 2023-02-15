package com.example.mybattery;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.media.MediaRecorder;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    int target_percent,current_percent;
    Button set_alarm,check_noise;
    Timer t = new Timer();
    TimerTask timerTask;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    boolean skip;
    Intent intent;
    Switch volume_button_pos;
    BatteryManager bm;
    Switch ring;
    boolean volume_pos,ring_state;
    Ringtone ringtone;
    TextView noise_text;
    MediaRecorder mRecorder;
    File f;
    private static double mEMA = 0.0;
    static final private double EMA_FILTER = 0.6;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        skip=true;

        set_alarm=findViewById(R.id.set_button);
        ring=findViewById(R.id.ring_or_vib);
        sharedPreferences= getApplicationContext().getSharedPreferences("settings",0);
        editor=sharedPreferences.edit();
        volume_pos=sharedPreferences.getBoolean("volume_right_or_left",false);
        ring_state=sharedPreferences.getBoolean("ringtone_state",false);
        target_percent=sharedPreferences.getInt("target_percent",100);
        volume_button_pos=(Switch)findViewById(R.id.volume_left_or_right);
        ring.setChecked(ring_state);
        volume_button_pos.setChecked(volume_pos);
        ((EditText) findViewById(R.id.percent_to_set)).setHint(String.valueOf(target_percent));
        f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Download/mybattery/");
        if(!f.exists())
            f.mkdirs();
        noise_text=findViewById(R.id.noise_text_1);
        check_noise=findViewById(R.id.noiose_button);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if(!Settings.canDrawOverlays(this)){
                Intent intent2 = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent2, 0);
            }
            else{
                open_app();
            }
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivityForResult(intent,2);

            }

        }
        System.out.println("error found");

        //noise_calculate();

    }

    private void open_app(){
        volume_button_pos.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setprogram();
                if(isChecked)
                    volume_button_pos.setText("Volume Button on Right");
                else if(!isChecked)
                    volume_button_pos.setText("Volume Button on Left");
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent= new Intent(this, alarm_service.class);
            intent.putExtra("target_percent",target_percent);
            startForegroundService(intent);
        } else {
            intent.putExtra("target_percent",target_percent);
            Intent intent= new Intent(this, alarm_service.class);
            startService(intent);
        }
        set_alarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String p=((EditText)findViewById(R.id.percent_to_set)).getText().toString();
                if(p.isEmpty()){
                    p=String.valueOf(target_percent);
                }
                target_percent=Integer.parseInt(p);
                if(target_percent>100){
                    target_percent=100;
                    ((EditText) findViewById(R.id.percent_to_set)).setText("100");
                }
                Toast.makeText(MainActivity.this,"Alarm is set",Toast.LENGTH_LONG).show();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent intent= new Intent(MainActivity.this, alarm_service.class);
                    intent.putExtra("target_percent",target_percent);
                    intent.putExtra("ring_state",ring.isChecked());
                    startForegroundService(intent);
                } else {
                    intent.putExtra("target_percent",target_percent);
                    Intent intent= new Intent(MainActivity.this, alarm_service.class);
                    intent.putExtra("target_percent",target_percent);
                    intent.putExtra("ring_state",ring.isChecked());
                    startService(intent);
                }

            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mRecorder!=null){
        mRecorder.stop();
        mRecorder.release();
        mRecorder=null;
        timerTask.cancel();}
        setprogram();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //noise_calculate();
    }

    private void setprogram(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent= new Intent(MainActivity.this, alarm_service.class);
            editor.putBoolean("volume_right_or_left",volume_button_pos.isChecked());
            editor.putBoolean("ringtone_state",ring.isChecked());
            editor.putInt("target_percent",target_percent);
            editor.apply();
            intent.putExtra("target_percent",target_percent);
            intent.putExtra("ring_state",ring.isChecked());
            intent.putExtra("volume_right_or_left",volume_button_pos.isChecked());
            startForegroundService(intent);
        } else {
            Intent intent= new Intent(MainActivity.this, alarm_service.class);
            editor.putBoolean("volume_right_or_left",volume_button_pos.isChecked());
            editor.putBoolean("ringtone_state",ring.isChecked());
            editor.putInt("target_percent",target_percent);
            editor.apply();
            intent.putExtra("target_percent",target_percent);
            intent.putExtra("ring_state",ring.isChecked());
            intent.putExtra("volume_right_or_left",volume_button_pos.isChecked());
            startService(intent);
        }
    }
    private void noise_calculate() throws IOException {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                   125);

        }


        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setOutputFile(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/mybattery/myreport");
        mRecorder.prepare();
        mRecorder.start();

        timerTask=new TimerTask() {
            @Override
            public void run() {
                if(mRecorder!=null){
                final double p=mRecorder.getMaxAmplitude();
                final double amplitude= 20 * Math.log10(p/ 2700.0);
                final int amp=(int) amplitude+70;
                System.out.println(amp);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        check_noise.setText(String.valueOf(p));
                        noise_text.setText(String.valueOf(amp));
                        //updateTv();
                    }
                });
                }
            }
        };
        new Timer().schedule(timerTask, 0, 300);


    }
    public void updateTv(){
        String k=Double.toString(soundDb(2700)) + " dB";
        noise_text.setText(k);
        System.out.println(k);

    }
    public double soundDb(double ampl){
        return  20 * Math.log10(getAmplitudeEMA() / ampl);
    }
    public double getAmplitude() {
        if (mRecorder != null)
            return  (mRecorder.getMaxAmplitude());
        else
            return 0;

    }
    public double getAmplitudeEMA() {

        double amp =  getAmplitude();
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA;
        return mEMA;
    }

}

