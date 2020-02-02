package com.example.myapplication;

import android.content.SharedPreferences;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

public class Settings extends AppCompatActivity {
    TextView tServer;
    TextView tPort;

    SharedPreferences sPref;
    public static final String APP_PREFERENCES = "settings";
    public static final String APP_PREFERENCES_SERVER = "server";
    public static final String APP_PREFERENCES_PORT = "port";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        tServer = (TextView) findViewById(R.id.etIpaddr);
        tPort = (TextView) findViewById(R.id.etPort);

        sPref = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE);

        if (sPref.contains(APP_PREFERENCES_SERVER)) {
            tServer.setText(sPref.getString(APP_PREFERENCES_SERVER, ""));
        }
        if (sPref.contains(APP_PREFERENCES_PORT)) {
            tPort.setText("" + sPref.getInt(APP_PREFERENCES_PORT, 0));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                SharedPreferences.Editor editor = sPref.edit();
                editor.putString(APP_PREFERENCES_SERVER, tServer.getText().toString());
                if ( !tPort.getText().toString().isEmpty() )
                    editor.putInt(APP_PREFERENCES_PORT, Integer.parseInt(tPort.getText().toString()));
                else
                    editor.putInt(APP_PREFERENCES_PORT, 0);
                editor.apply();
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}