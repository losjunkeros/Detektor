package com.example.silly_000.detektor;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;
import java.io.IOException;
import java.util.UUID;

/**
 * Klasa dziedzicząca po klasie Service.
 *
 * Odpowiada za cały proces pobierania i analizy danych z mikrofonu urządzenia.
 * Opisuje sposób klasyfikowania sygnałów i reagowania na ich wykrycie.
 *
 * Nie jest powiazana z żadnym plikiem xml, nie ma przypisanego interfejsu (jest to serwis, nie aktywność).
 * Może pracować w tle nawet po wyjściu z aplikacji.
 *
 * @author Tomasz Junker
 */
public class MyService extends Service {
    /** Częstotliwość próbkowania danych z mikrofonu. */
    private static final int RECORDER_SAMPLE_RATE = 44100;
    /** Ilość nagrywanych kanałów (mono/stereo). */
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    /** Format enkodowania danych z mikrofonu (16-bitowy PCM). */
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    /** Służy do wyświetlania komunikatów logcat */
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    /** Kod UUID (universally unique identifier). */
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    /** Obiekt klasy AudioRecord. */
    private static AudioRecord recorder = null;
    /** Wątek do obsługi pobierania danych z mikrofonu i ich analizy. */
    private static Thread recordingThread = null;
    /** Obiekt klasy FrequencyScanner. */
    private static FrequencyScanner scanner = null;
    /** Obiekt klasy BluetoothAdapter. */
    private static BluetoothAdapter ba = null;
    /** Obiekt klasy BluetoothDevice. */
    private static BluetoothDevice device = null;
    /** Obiekt klasy BluetoothSocket do obsługi połączeń bluetooth. */
    private static BluetoothSocket socket = null;
    /** Adres urządzenia, z którym ma nastąpić połączenie. */
    public static String address = "";
    /** Przechowuje informację o stanie alarmu (false - wyłączony, true - jest alarm). */
    private static boolean alarm = false;
    /** Przechowuje informację o przerwaniu zliczania próbek w algorytmie sprawdzającym sygnał typu Wilk (true - przerwano). */
    private static boolean check5 = false;
    /** Czy trwa nagrywanie dźwięku z mikrofonu? */
    public static boolean isRecording = false;
    /** Zapamiętują ostatnią, przed- i przedprzedostatnią pobraną częstotliwość. */
    private static double last_freq = 0, prev_freq = 0, prev_prev_freq = 0;
    /** Liczniki stosowane w poszczególnych algorytmach sprawdzania typu sygnału. */
    private static int count = 0, count2 = 0, count3 = 0, count4 = 0, count5 = 0, count6 = 0, rec1 = 0, rec2 = 0, master = 0;
    /** Rozmiar buffora, do którego trafiają dane. */
    private static int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLE_RATE,
            RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

    /**
     * Wywoływana przy tworzeniu serwisu.
     */
    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     * Odpowiada za wszystkie działania, jakie wykonuje serwis od momentu jego wywołania przez daną intencję.
     *
     * Ustawia flagę isRecording. Próbuje się połączyć poprzez Bluetooth z urządzeniem o podanym adresie. Tworzy nowy wątek i rozpoczyna pobieranie danych z mikrofonu urządzenia.
     * Odfiltrowuje zbędne częstotliwości, zlicza poprawne do zmiennej master. Poddaje kolejne częstotliwości analizie i w przypadku dopasowania ich do któregoś ze schematu ustawia flagę alarm na wartoś true.
     * Jeśli wcześniej doszło do poprawnego połączenia z urządzeniem Bluetooth zostaje wysłana zmienna znakowa a, jeśli nie, urządzenie zawibruje.
     *
     * @param intent Intencja uruchamiająca serwis.
     * @param flags Flagi.
     * @param startId Startowe id.
     * @see MyService
     * @return onStartCommand.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRecording = true;
        /** Obiekt klasy Vibrator odpowiedzialny za sterowanie wibracjami urządzenia. */
        final Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        try
        {
            if (socket == null && !(address.equals(""))) {
                ba = BluetoothAdapter.getDefaultAdapter();
                device = ba.getRemoteDevice(address);
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                ba.cancelDiscovery();
                socket.connect();
                Log.v(LOG_TAG, "connected");
            }
        }
        catch (IOException e)
        {
            Log.v(LOG_TAG, "SOCKET ERROR");
        }

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

                if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                    bufferSize = RECORDER_SAMPLE_RATE * 2;
                }

                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        RECORDER_SAMPLE_RATE, RECORDER_CHANNELS,
                        RECORDER_AUDIO_ENCODING, bufferSize);

                if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e(LOG_TAG, "Audio Record can't initialize!");
                    return;
                }
                recorder.startRecording();
                Log.v(LOG_TAG, "Start recording");

                scanner = new FrequencyScanner();
                /** Buffor przechowujący dane wejściowe. */
                short buffer[] = new short[bufferSize / 2];
                /** Zmienna wysyłana w przypadku uruchomienia alarmu. */
                char a = 'A';

                while (isRecording) {
                    recorder.read(buffer, 0, buffer.length);
                    /** Częstotliwość główna sygnału. */
                    int freq = (int) scanner.extractFrequency(buffer, 44100);
                    Log.v(LOG_TAG, String.format("freq: %d", freq));
                    if (freq > 400 && freq < 2500) {
                        master++;
                        Log.v(LOG_TAG, String.format("master: %d", master));
                        alarm = checkingSignal((double) freq);
                    }
                    else {
                        master = 0;
                    }
                    if (alarm) {
                        if (socket == null) {
                            vibe.vibrate(500);
                        }
                        else {
                            try
                            {
                                socket.getOutputStream().write((byte)a);
                                Log.v(LOG_TAG, "SENDING VIA BLUETOOTH");
                            }
                            catch (IOException e)
                            {
                                Log.v(LOG_TAG, "SENDING ERROR");
                                vibe.vibrate(500);
                            }
                        }
                        zeroCheck();
                        alarm = false;
                        Log.v(LOG_TAG, "Signal recognized");
                    }
                }
            }
        }, "AudioRecorder Thread");
        recordingThread.start();

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Zeruje odpowiednie liczniki.
     */
    private void zeroCount() {
        count = 0;
        count2 = 0;
        count3 = 0;
        count4 = 0;
        count5 = 0;
        count6 = 0;
    }

    /**
     * Zeruje odpowiednie liczniki i ustawia zmienną check5 na false.
     */
    private void zeroCheck() {
        check5 = false;
        rec1 = 0;
        rec2 = 0;
        master = 0;
    }

    /**
     * Odpowiada za sprawdzanie ciągu kolejnych częstotliwości i ich pokrywania się z założonymi schematami dla każdego typu sygnałów pojazdów uprzywilejowanych.
     *
     * @param freq Częstotliwość poddawana analizie.
     * @return true, jeśli wykryto jakiś sygnał, false, jeśli częstotliwości nie pokryły się z żadnym schematem.
     */
    private boolean checkingSignal(double freq) {
        //CHECKING LE-ON SIGNAL
        if (Math.sqrt(Math.pow(freq - last_freq, 2)) <= 100) {
            count++;
            count2 = 0;
            Log.v(LOG_TAG, String.format("count: %d, count2: %d", count, count2));
        }
        else {
            count = 0;
            count2++;
            Log.v(LOG_TAG, String.format("count: %d, count2: %d", count, count2));
        }

        if (count2 > 2 || count > 15) {
            rec1 = 0;
        }

        if (count == 5) {
            rec1++;
        }
        Log.v(LOG_TAG, String.format("rec1: %d", rec1));

        if (rec1 > 7 && master >= 20) {
            zeroCount();
            return true;
        }

        //CHECKING DOG SIGNAL
        if (freq < (last_freq + 20)) {
            count3++;
            count4 = 0;
            Log.v(LOG_TAG, String.format("count3: %d, count4: %d", count3, count4));
        }
        else {
            count3 = 0;
            count4++;
            Log.v(LOG_TAG, String.format("count3: %d, count4: %d", count3, count4));
        }

        if (count4 > 2 || count3 > 6) {
            rec2 = 0;
        }

        if (count3 == 3) {
            rec2++;
        }
        Log.v(LOG_TAG, String.format("rec2: %d", rec2));

        if (rec2 > 9 && master >= 20) {
            zeroCount();
            return true;
        }

        //CHECKING WOLF SIGNAL
        if ((freq >= last_freq || freq >= prev_freq || freq >= prev_prev_freq) && !check5) {
            count5++;
            Log.v(LOG_TAG, String.format("count5: %d", count5));
        }
        else {
            check5 = true;
        }

        if (check5 && count5 < 25) {
            count5 = 0;
            check5 = false;
        }

        if (check5 && count5 >= 20) {
            if (freq <= last_freq || freq <= prev_freq || freq <= prev_prev_freq) {
                count6++;
                Log.v(LOG_TAG, String.format("count6: %d", count6));
            }
            else {
                if (count6 >= 25 && master >= 20) {
                    zeroCount();
                    return true;
                }
                else {
                    check5 = false;
                    count5 = 0;
                    count6 = 0;
                }
            }
        }

        //CHECKING MASTER
        if (master == 120) {
            zeroCount();
            return true;
        }

        prev_prev_freq = prev_freq;
        prev_freq = last_freq;
        last_freq = freq;

        return false;
    }

    /**
     * Wykonuje się w momencie zatrzymania działania serwisu.
     *
     * Zeruje wszystkie zmienne wykorzystywane przez serwis, zatrzymuje pobieranie danych z mikrofonu urządzenia. Rozłącza się z urządzeniem Bluetooth. Ustawia flagę isRecording na wartość false.
     * @see MyService
     */
    @Override
    public void onDestroy() {
        if (recorder != null) {
            isRecording = false;

            if (socket != null) {
                try {
                    socket.close();
                }
                catch (IOException e) {
                    Log.v(LOG_TAG, "CLOSING ERROR");
                }
            }

            ba = null;
            device = null;
            socket = null;
            zeroCheck();
            zeroCount();
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
            scanner = null;
            last_freq = 0;
            prev_freq = 0;
            prev_prev_freq = 0;
            Log.v(LOG_TAG, "Recording stopped");
        }
        super.onDestroy();
    }

    /**
     * Podstawowy interfejs zdalnego obiektu.
     *
     * @param intent Intencja wywołująca serwis MyService.
     * @see MyService
     * @return null
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
