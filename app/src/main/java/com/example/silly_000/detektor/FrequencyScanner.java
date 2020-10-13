package com.example.silly_000.detektor;

import org.jtransforms.fft.DoubleFFT_1D;

/**
 * Klasa odpowiedzialna za analizę częstotliwościową sygnału.
 *
 * Wykonuje szybką transformatę Fouriera (wykorzystując bibliotekę JTransforms) i oblicza główną częstotliwość sygnału.
 *
 * @author Tomasz Junker
 */
public class FrequencyScanner {
    /**
     * Okno czasowe.
     */
    private double[] window;

    /**
     * Zeruje okno czasowe.
     */
    public FrequencyScanner() {
        window = null;
    }

    /**
     * Okienkuje dane wejściowe i oblicza FFT.
     *
     * Oblicza główną częstotliwość występującą w sygnale.
     *
     * @param sampleData Dane wejściowe poddawane analizie.
     * @param sampleRate Częstotliwość próbkowania.
     * @return Wartość częstotliwości głównej sygnału.
     */
    public double extractFrequency(short[] sampleData, int sampleRate) {
        DoubleFFT_1D fft = new DoubleFFT_1D(sampleData.length + 24 * sampleData.length);
        double[] a = new double[(sampleData.length + 24 * sampleData.length) * 2];

        System.arraycopy(applyWindow(sampleData), 0, a, 0, sampleData.length);
        fft.realForward(a);

        double maxMag = Double.NEGATIVE_INFINITY;
        int maxInd = -1;

        for(int i = 0; i < a.length / 2; ++i) {
            double re  = a[2*i];
            double im  = a[2*i+1];
            double mag = Math.sqrt(re * re + im * im);

            if(mag > maxMag) {
                maxMag = mag;
                maxInd = i;
            }
        }

        return (double)sampleRate * maxInd / (a.length / 2);
    }

    /**
     * Tworzy okno Hamminga o podanym rozmiarze (szerokości).
     *
     * @param size Rozmiar okna.
     */
    private void hammingWindow(int size) {
        if(window != null && window.length == size) {
            return;
        }
        window = new double[size];
        for(int i = 0; i < size; ++i) {
            window[i] = .54 - .46 * Math.cos(2 * Math.PI * i / (size - 1.0));
        }
    }

    /**
     * Nakłada okno na dane wejściowe.
     *
     * @param input Dane wejściowe.
     * @return Dane po okienkowaniu.
     */
    private double[] applyWindow(short[] input) {
        double[] res = new double[input.length];

        hammingWindow(input.length);
        for(int i = 0; i < input.length; ++i) {
            res[i] = (double)input[i] * window[i];
        }
        return res;
    }
}
