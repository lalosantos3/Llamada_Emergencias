package com.example.llamadadeemergencia;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telecom.TelecomManager;
import android.telephony.PhoneStateListener;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

public class MyBackgroundService extends Service {

    private static final String TAG = "MyBackgroundService";
    private LocationManager locationManager;
    private LocationListener locationListener;

    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    private AudioManager audioManager;
    private TelecomManager telecomManager;

    double latitude = -1.0;
    double longitude = -1.0;

    String telefono;

    @Override
    public void onCreate() {
        super.onCreate();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MyBackgroundService.this);
        telefono = preferences.getString("telefono", "");

        Toast.makeText(this, "Servicio iniciado", Toast.LENGTH_SHORT).show();

        // Configuración para obtener las coordenadas del teléfono
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, locationListener);

        Context THIS = this;

        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                        primero = false;

                        // Llamada entrante
                        Toast.makeText(MyBackgroundService.this, "Llamada entrante", Toast.LENGTH_SHORT).show();

                        // Envío de mensajes con las coordenadas al recibir una llamada
                        Toast.makeText(THIS, "Mensaje", Toast.LENGTH_SHORT).show();
                        sendMessageWithCoordinates(latitude, longitude);

                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        // Llamada contestada
                        Toast.makeText(MyBackgroundService.this, "Llamada contestada", Toast.LENGTH_SHORT).show();

                        // Bajar el volumen al mínimo
                        lowerVolume();

                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (!primero) {
                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    // Aquí se ejecuta el código después del retraso de 5 segundos

                                    Toast.makeText(MyBackgroundService.this, "Llamada finalizada", Toast.LENGTH_SHORT).show();

                                    String phoneNumber = telefono; // Número de teléfono al que deseas llamar

                                    // Crear un Intent con la acción de realizar una llamada
                                    Intent intent = new Intent(Intent.ACTION_CALL);

                                    // Establecer el número de teléfono en el Intent
                                    intent.setData(Uri.parse("tel:" + phoneNumber));

                                    // Establecer el indicador FLAG_ACTIVITY_NEW_TASK
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                                    startActivity(intent);

                                    primero = true;
                                }
                            }, 3000); // Retraso de 3 segundos en milisegundos
                        }
                        break;
                }
            }
        };

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);

        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    boolean primero = true;

    private void sendMessageWithCoordinates(double latitude, double longitude) {
        SmsManager smsManager = SmsManager.getDefault();

        String phoneNumber = telefono;
        String message = "Coordenadas: " + latitude + ", " + longitude;

        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
    }

    private void lowerVolume() {
        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_LOWER, 0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Detener la escucha de llamadas telefónicas
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);

        // Detener la obtención de coordenadas
        locationManager.removeUpdates(locationListener);
    }
}
