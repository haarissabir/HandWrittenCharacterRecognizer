package com.example.varamach.simplepaintapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.example.varamach.simplepaintapp.controller.MainActivity;

public class splash_screen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        Thread splashThread = new Thread()
        {
            @Override
            public void run() {
                try
                {
                    sleep(2000);
                    Intent main_activityIntent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(main_activityIntent);
                    finish();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        splashThread.start();
    }
}
