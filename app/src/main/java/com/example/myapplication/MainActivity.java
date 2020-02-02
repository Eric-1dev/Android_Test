package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.ToggleButton;

import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.sqrt;

public class MainActivity extends AppCompatActivity {
    String server;
    int port;

    SocketClient client;

    byte[] data = new byte[4];

    SeekBar sTopPWM;
    Switch sBottomSwitch;
    SharedPreferences sPref;
    ToggleButton tClientStatus;
    ImageView iColorRing;
    ImageView iAutoButton;
    Bitmap bitmap;

    Timer mTimer = new Timer();

    public static final String APP_PREFERENCES = "settings";
    public static final String APP_PREFERENCES_SERVER = "server";
    public static final String APP_PREFERENCES_PORT = "port";

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        sPref = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE);
        sTopPWM = findViewById(R.id.topPWM);
        sBottomSwitch = findViewById(R.id.bottomSwitch);
        tClientStatus = findViewById(R.id.clientStatus);
        iColorRing = findViewById(R.id.colorRing);
        iAutoButton = findViewById(R.id.autoButton);

        sBottomSwitch.setOnCheckedChangeListener(switchChecker);
        sTopPWM.setOnSeekBarChangeListener(seekChecker);

        iColorRing.setDrawingCacheEnabled(true);
        iColorRing.buildDrawingCache(true);

        iColorRing.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE ) {
                    int regX = (int) (iColorRing.getWidth()/2 - event.getX());
                    int regY = (int) (iColorRing.getHeight()/2 - event.getY());

                    int rad = (int) sqrt( regX*regX + regY*regY );
                    if ( rad < iColorRing.getWidth()/2 - 2 && rad > iAutoButton.getWidth()/2 + 2 ) {
                        bitmap = iColorRing.getDrawingCache();
                        int pixel = bitmap.getPixel((int) event.getX(), (int) event.getY());

                        data[1] = (byte) Color.red(pixel);
                        data[2] = (byte) Color.green(pixel);
                        data[3] = (byte) Color.blue(pixel);

                        sendToESP(data);
                    }
                    if (event.getAction() == MotionEvent.ACTION_UP ) {
                        iColorRing.performClick();
                    }
                }
                return true;
            }
        });

        iAutoButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN ) {
                    int regX = (int) (iAutoButton.getWidth() / 2 - event.getX());
                    int regY = (int) (iAutoButton.getHeight() / 2 - event.getY());

                    int rad = (int) sqrt( regX*regX + regY*regY );

                    if ( rad < iAutoButton.getWidth()/2 ) {
                        data[1] = data[2] = data[3] = (byte)255;
                        sendToESP(data);
                    }
                }
                if (event.getAction() == MotionEvent.ACTION_UP ) {
                    iAutoButton.performClick();
                }
                return true;
            }
        });

        sTopPWM.setEnabled(false);
        sBottomSwitch.setEnabled(false);
        iColorRing.setEnabled(false);
        iAutoButton.setEnabled(false);

        if (sPref.contains(APP_PREFERENCES_SERVER)) {
            server = sPref.getString(APP_PREFERENCES_SERVER, "");
        } else {
            server = null;
        }
        if (sPref.contains(APP_PREFERENCES_PORT)) {
            port = sPref.getInt(APP_PREFERENCES_PORT, 0);
        } else {
            port = 0;
        }

        client = new SocketClient(server, port);

        if ( port != 0 && server != null ) {
            connect();
            mTimer.schedule(myTimerTask, 2000, 2000);
        } else {
            mTimer.cancel();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTimer.cancel();
        if ( client.isConnected() )
            client.closeConnection();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mTimer.cancel();
        if ( client.isConnected() )
            client.closeConnection();
    }

    TimerTask myTimerTask = new TimerTask() {
        @Override
        public void run() {
            sendToESP(data);
        }
    };

    private void connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if ( client.openConnection() ) {
                    data = client.getData();
                    uiSetEnable(true);
                } else {
                    uiSetEnable(false);
                }
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void uiSetEnable(final boolean state) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tClientStatus.setChecked(state);
                sTopPWM.setEnabled(state);
                sBottomSwitch.setEnabled(state);

                if ( state ) {
                    sTopPWM.setProgress(data[0]);
                    if (data[1] != 0 || data[2] != 0 || data[3] != 0)
                        sBottomSwitch.setChecked(true);
                }
                checkRGBSwitch();
            }
        });
    }

    private void checkRGBSwitch() {
        if ( sBottomSwitch.isChecked() ) {
            iColorRing.setEnabled(true);
            iAutoButton.setEnabled(true);
        }
        else {
            data[1] = data[2] = data[3] = 0;
            iColorRing.setEnabled(false);
            iAutoButton.setEnabled(false);
        }
    }

    private void sendToESP(final byte[] data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if ( !client.sendData(data) ) {
                    uiSetEnable(false);
                    connect();
                }
            }
        }).start();
    }

    CompoundButton.OnCheckedChangeListener switchChecker = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            checkRGBSwitch();
            sendToESP(data);
        }
    };

    SeekBar.OnSeekBarChangeListener seekChecker = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            data[0] = (byte) sTopPWM.getProgress();
            sendToESP(data);
        }
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                Intent intent = new Intent(MainActivity.this, Settings.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
