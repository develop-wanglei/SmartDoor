package com.example.jasonlss.atplatformexample;

import android.os.Handler;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;

public class DH11Sensirion implements AutoCloseable {
    private static final String TAG = DH11Sensirion.class.getSimpleName();
    private Gpio mGpio;
    private Handler handler = new Handler();
    private float Temperature=0;
    private float Humidity=0;
    private int j = 0;
    private int k = 0;
    private int data[]=new int[40];
    private Runnable startTrigger = new Runnable() {
        @Override
        public void run() {
            try {
                mGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            } catch (IOException e) {
                e.printStackTrace();
            }
            busyWaitMicros(20);
            try {
                mGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
                busyWaitMicros(20);
                mGpio.setDirection(Gpio.DIRECTION_IN);
                mGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);

                handler.postDelayed(startTrigger, 100);
            } catch (IOException e) {
                e.printStackTrace();
            }





        }
    };
    public DH11Sensirion(String Pin) throws IOException {
        try {
            PeripheralManager Manager = PeripheralManager.getInstance();
            mGpio = Manager.openGpio(Pin);
            configureGpio(mGpio);
        } catch (IOException e) {
            throw e;
        }
    }

    public static void busyWaitMicros(long micros) {
        long waitUntil = System.nanoTime() + (micros * 1_000);
        while (waitUntil > System.nanoTime()) {
        }
    }

    public void close() {
        handler.removeCallbacks(startTrigger);
        try {
            mGpio.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configureGpio(Gpio MGpio) {
        try {
            MGpio.setDirection(Gpio.DIRECTION_IN);
            MGpio.setActiveType(Gpio.ACTIVE_HIGH);
            MGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            handler.post(startTrigger);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public float getTemperature() {
        return Temperature*10000+Humidity;
    }


}
