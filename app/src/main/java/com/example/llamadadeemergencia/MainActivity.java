package com.example.llamadadeemergencia;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    EditText ettelefono;
    Button btnguardar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ettelefono = (EditText)findViewById(R.id.E_Num);
        btnguardar = (Button)findViewById(R.id.B_Entrar);

        Intent serviceIntent = new Intent(this, MyBackgroundService.class);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String telefono = sharedPreferences.getString("telefono", "");

        btnguardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("telefono", ettelefono.getText().toString());
                editor.apply();

                Toast.makeText(MainActivity.this, "Número guardado con éxito", Toast.LENGTH_SHORT).show();

                if(isServiceRunning(MainActivity.this, MyBackgroundService.class)) {
                    stopService(serviceIntent);
                    Toast.makeText(MainActivity.this, "Reiniciando servicio", Toast.LENGTH_SHORT).show();
                }

                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        if (!telefono.equals("")) {
            ettelefono.setText(telefono);

            startService(serviceIntent);
        }
    }

    public boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo service : runningServices) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }
}