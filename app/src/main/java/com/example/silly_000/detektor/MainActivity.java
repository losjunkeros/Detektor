package com.example.silly_000.detektor;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.view.KeyEvent;
import android.view.View;

/**
 * Klasa dziedzicząca po klasie Activity.
 *
 * Odpowiada za obsługę interfejsu opisanego w pliku układu activity_main.xml
 * Uruchamia odpowiednie aktywności bądź serwisy po naciśnięciu odpowiednich przycisków.
 *
 * @author Tomasz Junker
 */
public class MainActivity extends Activity {
    /**
     * Podczas tworzenia aktywności ustawia widok na podstawie odpowiedniego pliku xml.
     *
     * Wczytuje zapisany stan instancji ze zmiennej savedInstanceState.
     *
     * Ustawia startowe parametry elementów interfejsu (przycisków).
     *
     * @param savedInstanceState Zapamiętuje stan instancji.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setButtonHandlers();
        enableButtons(MyService.isRecording);
    }

    /**
     * Ustawia uchwyty do poszczególnych przycisków.
     */
    private void setButtonHandlers() {
        (findViewById(R.id.btnStart)).setOnClickListener(btnClick);
        (findViewById(R.id.btnStop)).setOnClickListener(btnClick);
        (findViewById(R. id.btnHelp)).setOnClickListener(help);
        (findViewById(R.id.btnBluetooth)).setOnClickListener(bluetooth);
    }

    /**
     * Przełącza stan aktywności przycisku o podanym id, zależnie od wartości logicznej podanej w parametrze isEnable.
     *
     * @param id Identyfikator przycisku.
     * @param isEnable Wartość true uaktywnia przycisk, wartość false dezaktywuje.
     */
    private void enableButton(int id, boolean isEnable) {
        (findViewById(id)).setEnabled(isEnable);
    }

    /**
     * Włącza lub wyłącza możliwość naciśnięcia przycisków START i STOP zależnie od tego, czy aplikacja pobiera w danej chwili dane z mikrofonu czy nie.
     *
     * @param isRecording Czy aplikacja jest w trakcie nagrywania?
     */
    private void enableButtons(boolean isRecording) {
        enableButton(R.id.btnStart, !isRecording);
        enableButton(R.id.btnStop, isRecording);
    }

    /**
     * Wyświetla powiadomienie o działaniu detektora na pasku powiadomień urządzenia.
     *
     * @param view Obiekt klasy View, opisującej podstawowy blok interfejsu użytkownika.
     */
    public void sendNotification(View view) {
        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("Detektor włączony")
                .setContentText("Kliknij, by otworzyć okno aplikacji")
                .setContentIntent(resultPendingIntent);
        /** Powiadomienie. */
        NotificationManager notification = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notification.notify(1, builder.build());
    }

    /**
     * Usuwa wcześniej wyświetlone powiadomienie z paska powiadomień urządzenia.
     *
     * @param view Obiekt klasy View, opisującej podstawowy blok interfejsu użytkownika.
     */
    public void cancelNotification(View view) {
        NotificationManager notification = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notification.cancel(1);
    }

    /**
     * Uruchamia serwis MyService.
     *
     * @see MyService
     */
    private void startMyService() {
        Intent serviceIntent = new Intent(this, MyService.class);
        startService(serviceIntent);
    }

    /**
     * Zatrzymuje działanie serwisu MyService.
     *
     * @see MyService
     */
    private void stopMyService() {
        Intent serviceIntent = new Intent(this, MyService.class);
        stopService(serviceIntent);
    }

    /**
     * Uruchamia aktywność Help.
     *
     * @see Help
     */
    private void showHelp() {
        Intent helpIntent = new Intent(this, Help.class);
        startActivity(helpIntent);
    }

    /**
     * Uruchamia aktywność Bluetooth.
     *
     * @see Bluetooth
     */
    private void startBluetooth() {
        Intent bluetoothIntent = new Intent(this, Bluetooth.class);
        startActivity(bluetoothIntent);
    }

    /**
     * "Słuchacz" przycisku btnHelp.
     */
    private View.OnClickListener help = new View.OnClickListener() {
        /**
         * Definiuje działanie aplikacji w przypadku naciśnięcia przycisku btnHelp.
         *
         * @param v Obiekt klasy View, opisującej podstawowy blok interfejsu użytkownika.
         */
        public void onClick(View v) {
            showHelp();
        }
    };

    /**
     * "Słuchacz" przycisku btnBluetooth.
     */
    private View.OnClickListener bluetooth = new View.OnClickListener() {
        /**
         * Definiuje działanie aplikacji w przypadku naciśnięcia przycisku btnBluetooth.
         *
         * @param v Obiekt klasy View, opisującej podstawowy blok interfejsu użytkownika.
         */
        public void onClick(View v) {
            startBluetooth();
        }
    };

    /**
     * "Słuchacz" przycisków btnStart lub btnStop.
     */
    private View.OnClickListener btnClick = new View.OnClickListener() {
        /**
         * Definiuje działanie aplikacji w przypadku naciśnięcia przycisków btnStart lub btnStop.
         *
         * @param v Obiekt klasy View, opisującej podstawowy blok interfejsu użytkownika.
         */
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnStart: {
                    enableButtons(true);
                    startMyService();
                    sendNotification(v);
                    break;
                }
                case R.id.btnStop: {
                    enableButtons(false);
                    stopMyService();
                    cancelNotification(v);
                    break;
                }
            }
        }
    };
}
