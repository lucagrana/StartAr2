package com.example.startar2;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LoadPath extends AppCompatActivity {


    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_path);
    }

    public void onClickAddPath(View v) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, 10);
        Log.d("fileee", "" + intent);
    }

    //@RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            //TODO: gestire meglio l'eccezione, fatto così è una merda
            Log.d("fileee", "onActivityResult. With request code " + requestCode);
            if (requestCode == 10 && resultCode == RESULT_OK) {
                //OK, this is good
                Log.d("fileee", "result seems to be OK");
                Uri uri = data.getData();
                InputStream inputStream = getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder builder = new StringBuilder();
                String line = null;
                while ((line = reader.readLine()) != null) {
                    builder.append(line + "\n");
                }
                Log.d("fileee", "data is: " + builder.toString());
                JSONObject punti = new JSONObject(builder.toString());
                Log.d("filee", "JSON parsed" + punti.toString());
                MyModel.coordinate(punti);

            } else {
                Log.d("fileee", "result IS NOT OK");
                //TODO: mange this case
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            //TODO: gestire l'eccezione
        }
    }

}
