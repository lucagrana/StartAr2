package com.example.startar2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            InputStream inputStream = getAssets().open("qrcodes.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line + "\n");
            }
            JSONObject qrcodes = new JSONObject(builder.toString());
            MyModel.qrcode(qrcodes);
            Intent intent = new Intent(getApplicationContext(), ArNav.class);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    public void onClickLoad(View v) {
        Intent intent = new Intent(getApplicationContext(), LoadPath.class);
        startActivity(intent);
    }

    public void arNav(View v) {
        Intent intent = new Intent(getApplicationContext(), ArNav.class);
        startActivity(intent);
    }
}
