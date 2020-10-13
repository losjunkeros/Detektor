package com.example.silly_000.detektor;

import android.app.Activity;
import android.os.Bundle;

/**
 * Klasa dziedzicząca po klasie Activity.
 *
 * Odpowiada za wyświetlenie interfejsu opisanego w pliku układu activity_help.xml
 *
 * @author Tomasz Junker
 */
public class Help extends Activity {

    /**
     * Podczas tworzenia aktywności ustawia widok na podstawie odpowiedniego pliku xml.
     *
     * Wczytuje zapisany stan instancji ze zmiennej savedInstanceState.
     *
     * @param savedInstanceState Zapamiętuje stan instancji.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
    }
}
