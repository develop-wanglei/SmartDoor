package com.example.jasonlss.atplatformexample;

import android.hardware.Sensor;

import com.google.android.things.userdriver.UserDriverManager;
import com.google.android.things.userdriver.sensor.UserSensor;
import com.google.android.things.userdriver.sensor.UserSensorDriver;
import com.google.android.things.userdriver.sensor.UserSensorReading;

import java.io.IOException;

public class DH11SensirionDriver implements AutoCloseable {

    private static final String TAG = DH11SensirionDriver.class.getSimpleName();
    private static final int DRIVER_VERSION = 1;
    private static final String DRIVER_NAME = "DH-11 Sensirion Sensor";

    private UserSensor userSensor;
    private DH11Sensirion device;

    public DH11SensirionDriver(String mPin) throws IOException {
        device = new DH11Sensirion(mPin);
    }

    private static UserSensor build(final DH11Sensirion dh11) {
        return new UserSensor.Builder()
                .setName(DRIVER_NAME)
                .setVersion(DRIVER_VERSION)
                .setType(Sensor.TYPE_AMBIENT_TEMPERATURE)
                .setDriver(new UserSensorDriver() {
                    @Override
                    public UserSensorReading read() {
                        float[] str1 =  {dh11.getTemperature()};
                        return new UserSensorReading(str1);
                    }
                })
                .build();
    }

    @Override
    public void close() {
        unregister();
        if (device != null) {
            try {
                device.close();
            } finally {
                device = null;
            }
        }
    }

    public void register() {
        if (device == null) {
            throw new IllegalStateException("cannot registered closed driver");
        }
        if (userSensor == null) {
            userSensor = build(device);
            UserDriverManager.getInstance().registerSensor(userSensor);
        }
    }

    public void unregister() {
        if (userSensor != null) {
            UserDriverManager.getInstance().unregisterSensor(userSensor);
            userSensor = null;
        }
    }
}