package com.example.mybattery;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.util.Timer;
import java.util.TimerTask;

public class alarm_service extends Service {
    int target_percent=100,current_percent,vol=0;
    SeekBar set_sound;
    Timer t = new Timer();
    TimerTask timerTask;
    boolean ring_state;
    Intent intent;
    BatteryManager bm;
    boolean volume_pos;
    SharedPreferences sharedPreferences;
    private static final int NOTIF_ID = 1;
    private static final String NOTIF_CHANNEL_ID = "Channel_Id";
    WindowManager.LayoutParams parameters;
    WindowManager wm;
    View customView;
    Ringtone ringtone;
    Vibrator v;
    AudioManager audioManager;
    public alarm_service() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public int onStartCommand(Intent intent2, int flags, int startId) {
        sharedPreferences= getApplicationContext().getSharedPreferences("settings",0);
        volume_pos=sharedPreferences.getBoolean("volume_right_or_left",false);
        target_percent=intent2.getIntExtra("target_percent",100);
        ring_state=intent2.getBooleanExtra("ring_state",sharedPreferences.getBoolean("ringtone_state",true));
        volume_pos=intent2.getBooleanExtra("volume_right_or_left",volume_pos);
        System.out.println("on bind");
        parameters = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        if(volume_pos) {
            parameters.gravity = Gravity.RIGHT;
        }
        else{
            parameters.gravity = Gravity.LEFT;
        }
        parameters.y=-550;
        wm.updateViewLayout(customView, parameters);

        return super.onStartCommand(intent2, flags, startId);
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onCreate(){
        v=(Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        audioManager = (AudioManager) alarm_service.this.getSystemService(Context.AUDIO_SERVICE);
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());

        final LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        @SuppressLint("InflateParams") final View
                customview = inflater.inflate(R.layout.sound_activity, null);
        customView=customview;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
         parameters = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        if(volume_pos) {
            parameters.gravity = Gravity.RIGHT;
        }
        else{
            parameters.gravity = Gravity.LEFT;
        }
        parameters.y=-550;

        if(Settings.canDrawOverlays(this)){
            wm.addView(customView, parameters);
        }

        set_sound=customView.findViewById(R.id.sound_line);
        set_sound.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @RequiresApi(api = Build.VERSION_CODES.P)
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_SAME,0 );
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,progress,0);
                System.out.println(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
//                int prog=audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//                System.out.println(prog);
//                if(progress>prog){
//                    while(progress>prog){
//                        new Thread(){
//                            @Override
//                            public void run() {
//                                super.run();
//                                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,0 );
//                            }
//                        }.start();
//                        //audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,0 );
//                        prog=audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//
//                    }
//                }
//                else if(prog>progress){
//                    while(prog>progress) {
//                        new Thread(){
//                            @Override
//                            public void run() {
//                                super.run();
//                                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,0 );
//                            }
//                        }.start();
//                        //audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,0 );
//                        prog=audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//
//                    }
//                }
                v.vibrate(20);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekBar.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                seekBar.getThumb().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                v.vibrate(120);
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBar.getProgressDrawable().setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.SRC_IN);
                seekBar.getThumb().setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.SRC_IN);
            }
        });
        timerTask=new TimerTask() {
            @Override
            public void run() {
                intent = getApplicationContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                bm = (BatteryManager) getApplicationContext().getSystemService(BATTERY_SERVICE);
                int state = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                int state2 = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                boolean p = BatteryManager.BATTERY_STATUS_CHARGING == state;
                boolean p2 = BatteryManager.BATTERY_PLUGGED_AC == state2;
                current_percent = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                if (p2 == true && current_percent>=target_percent) {
                    if(ring_state){
                        ringtone.play();}
                    v.vibrate(1000);
                }
            }
        };
        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), alarmUri);
        t.schedule(timerTask, 0, 3000);
        //startForeground();
    }

    private void startMyOwnForeground(){
        String NOTIFICATION_CHANNEL_ID = "com.example.mybattery";
        String channelName = "My Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(false)
                .setSmallIcon(R.drawable.ic_battery_notification)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }
}
