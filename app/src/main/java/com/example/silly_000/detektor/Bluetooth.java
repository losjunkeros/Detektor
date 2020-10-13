package com.example.silly_000.detektor;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Set;

/**
 * Klasa dziedzicząca po klasie Activity.
 *
 * Odpowiada za obsługę interfejsu opisanego w pliku układu activity_bluetooth.xml
 * Uruchamia odpowiednie aktywności bądź serwisy po naciśnięciu odpowiednich przycisków.
 *
 * @author Tomasz Junker
 */
public class Bluetooth extends Activity {
    /** Przechowuje listę urządzeń */
    private static ListView deviceList;

    /**
     * Podczas tworzenia aktywności ustawia widok na podstawie odpowiedniego pliku xml.
     *
     * Wczytuje zapisany stan instancji ze zmiennej savedInstanceState.
     *
     * Ustawia uchwyty do poszczególnych elementów interfejsu i ich startowe parametry.
     *
     * Tworzy obiekt klasy BluetoothAdapter i wyświetla prośbę o zezwolenie na uruchomienie usługi Bluetooth w urządzeniu.
     *
     * @param savedInstanceState Zapamiętuje stan instancji.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        (findViewById(R.id.btnPaired)).setOnClickListener(btnClick);
        (findViewById(R.id.btnDisconnect)).setOnClickListener(btnClick);
        deviceList = (ListView)findViewById(R.id.listView);
        if (MyService.address.equals("")) {
            (findViewById(R.id.btnDisconnect)).setEnabled(false);
        }

        BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
        if (!ba.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 1);
        }
    }

    /**
     * "Słuchacz" przycisków btnPaired i btnDisconnect.
     */
    private View.OnClickListener btnClick = new View.OnClickListener() {
        /**
         * Definiuje działanie aplikacji w przypadku naciśnięcia przycisków btnStart lub btnStop.
         *
         * @param v Obiekt klasy View, opisującej podstawowy blok interfejsu użytkownika.
         */
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnPaired: {
                    paired();
                    break;
                }
                case R.id.btnDisconnect: {
                    MyService.address = "";
                    (findViewById(R.id.btnDisconnect)).setEnabled(false);
                    break;
                }
            }
        }
    };

    /**
     * Pobiera informajce o sparowanych urządzeniach i dodaje je do listy deviceList.
     *
     * Jeśli brak sparowanych urządzeń wyświetla odpowiedni komunikat.
     *
     * Ustawia uchwyt do listy urządzeń deviceList.
     */
    private void paired(){
        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1);
        BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = ba.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                adapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "Nie wykryto sparowanych urządzeń.", Toast.LENGTH_LONG).show();
        }
        deviceList.setAdapter(adapter);
        deviceList.setOnItemClickListener(listClick);
    }

    /**
     * "Słuchacz" listy deviceList.
     */
    private AdapterView.OnItemClickListener listClick = new AdapterView.OnItemClickListener() {
        /**
         * Opisuje działanie aplikacji w przypadku naciśnięcia na któryś z elementów wyświetlonej listy deviceList.
         *
         * Pobiera informację o adresie wybranego urządzenia i przekazuje ją do obiektu address serwisu MyService.
         *
         * Uaktywnia przycisk btnDisconnect i kończy działanie aktywności Bluetooth, wracając do aktywności MainActivity.
         *
         * @param av Obiekt klasy AdapterView, informuje o miejscu zdarzenia (naciśnięcia listy).
         * @param v Obiekt klasy View, opisującej podstawowy blok interfejsu użytkownika.
         * @param arg3 Pozycja widoku w adapterze.
         * @param arg4 Id rzędu, w którym doszło do zdarzenia (naciśnięcia listy).
         * @see MyService
         * @see MainActivity
         */
        public void onItemClick(AdapterView av, View v, int arg3, long arg4)
        {
            String info = ((TextView) v).getText().toString();
            MyService.address = info.substring(info.length() - 17);
            (findViewById(R.id.btnDisconnect)).setEnabled(true);
            finish();
        }
    };
}

