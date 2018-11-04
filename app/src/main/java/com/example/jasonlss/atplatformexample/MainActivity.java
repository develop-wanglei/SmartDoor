package com.example.jasonlss.atplatformexample;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.Pwm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity  {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String BUTTON_PIN_NAME = "BCM26"; // GPIO 4/5/6/12/16/17/22/23/24/25/26/27
    private static final String LED_PIN_NAME = "BCM5"; // GPIO 4/5/6/12/16/17/22/23/24/25/26/27
    private static final String PWM_NAME ="PWM1";//pwm1-13 PWM0-18
    private static final String InfraredSensor_NAME ="BCM16";
    private static final String Trig_NAME ="BCM6";
    private static final String Echo_NAME ="BCM12";
    private static final String Sensirion_NAME ="BCM25";

    private float Distance;
    private float Temperature;
    private float Humidity;
    private long TimeTicket=0;
    private boolean mState = false;
    private boolean humanState=false;

    private Gpio mGpio;//开关
    private Gpio LEDGpio;//LED
    private Gpio InfraredSensor;//红外传感器
    private Pwm mPwm;
    private Camera mCamera;//相机
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private Handler LoopHandler = new Handler();
    private AtomicBoolean mReady = new AtomicBoolean(false);
    private SensorManager mSensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Activity created.");
        PeripheralManager Manager = PeripheralManager.getInstance();
        try{
            //gpio init
            mGpio = Manager.openGpio(BUTTON_PIN_NAME);
            mGpio.setDirection(Gpio.DIRECTION_IN);
            mGpio.setEdgeTriggerType(Gpio.EDGE_FALLING);
            mGpio.registerGpioCallback(mGpioCallback);

            LEDGpio=Manager.openGpio(LED_PIN_NAME);
            LEDGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            LEDGpio.setActiveType(Gpio.ACTIVE_LOW);

            InfraredSensor = Manager.openGpio(InfraredSensor_NAME);
            InfraredSensor.setDirection(Gpio.DIRECTION_IN);
            InfraredSensor.setActiveType(Gpio.ACTIVE_HIGH);
            InfraredSensor.setEdgeTriggerType(Gpio.EDGE_BOTH);
            InfraredSensor.registerGpioCallback(InfraredSensorCallback);
            //sensor init
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            mSensorManager.registerDynamicSensorCallback(new mDynamicSensorCallback());
            Hcsr04UltrasonicDriver hcsr04UltrasonicDriver = new Hcsr04UltrasonicDriver(Trig_NAME, Echo_NAME);
            hcsr04UltrasonicDriver.register();
            DH11SensirionDriver dh11SensirionDriver = new DH11SensirionDriver(Sensirion_NAME);
            dh11SensirionDriver.register();
            //loop init
            LoopHandler.post(looper);
        }catch (IOException e) {
            Log.e(TAG, "Unable to on GPIO", e);
        }
        try {
            //PWM init
            mPwm = Manager.openPwm(PWM_NAME);
            initializePwm(mPwm);
        } catch (IOException e) {
            Log.w(TAG, "Unable to on PWM", e);
        }
        CameraInit();
    }

    private void CameraInit(){
        mBackgroundThread = new HandlerThread("BackgroundThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        mBackgroundHandler.post(mInitializeOnBackground);
    }

    private SensorEventListener mTemperatureListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            Temperature = event.values[0]/10000;
            Humidity=event.values[0]%10000;

        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(TAG, "accuracy changed: " + accuracy);
        }
    };

    private SensorEventListener mDistanceListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            Distance = event.values[0];
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(TAG, "accuracy changed: " + accuracy);
        }
    };

    private class mDynamicSensorCallback extends SensorManager.DynamicSensorCallback {
        @Override
        public void onDynamicSensorConnected(Sensor sensor) {
            if (sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE){
                mSensorManager.registerListener(mTemperatureListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
            else if (sensor.getType() == Sensor.TYPE_PROXIMITY)
            {
                mSensorManager.registerListener(mDistanceListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            }

        }

        @Override
        public void onDynamicSensorDisconnected(Sensor sensor) {
            super.onDynamicSensorDisconnected(sensor);
        }
    }


    private final GpioCallback InfraredSensorCallback =new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            try {
                if (gpio.getValue()) {
                    Log.e("有人来了", gpio.getValue()+ "");
                    humanState=true;
                } else {
                    Log.e("没有人", gpio.getValue() + "");
                    humanState=false;
                }
            } catch (IOException e) {
                Log.i(TAG, "InfraredSensor not in used");
            }
            return true;
        }

        @Override
        public void onGpioError(Gpio gpio, int error) {
            Log.w(TAG, gpio + ": Error event " + error);
        }
    };

    private final GpioCallback mGpioCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            //Log.i(TAG, "GPIO changed, button pressed");
          //  if (getgpioValue(mGpio)) {
               // setgpioValue(LEDGpio,true);


         //   } else {
                //setgpioValue(LEDGpio,false);

          //  }
            return true;
        }

        @Override
        public void onGpioError(Gpio gpio, int error) {
            Log.w(TAG, gpio + ": Error event " + error);
        }
    };

    private Runnable mInitializeOnBackground = new Runnable() {
        @Override
        public void run() {
            mCamera = Camera.getInstance();
            mCamera.initializeCamera(MainActivity.this,
                    mBackgroundHandler, mOnImageAvailableListener);
            setReady(true);
        }
    };

    private Runnable mBackgroundClickHandler = new Runnable() {
        @Override
        public void run() {
            mCamera.takePicture();
        }
    };

    private Runnable looper = new Runnable() {//主任务循环（1ms
        @Override
        public void run() {
            try {
                TimeTicket++;
                if(TimeTicket>9999)
                {
                    TimeTicket=0;
                }
                if(TimeTicket%1000==0)
                {
                    startImageCapture();
                }
               if(TimeTicket%200==0)
               {
                   mState = !mState;
                   LEDGpio.setValue(mState);
               }
               if(TimeTicket%2000==0){
                    if(Distance==0)
                    {
                        Log.e("距离：", "异常距离");
                    }else
                    {
                        Log.e("距离", Distance+"CM"+Temperature+Humidity);
                    }
                    if(humanState)
                    {
                        Log.e("人体红外传感器：", "有人");
                    }
                    else
                    {
                        Log.e("人体红外传感器：", "没人");
                    }

               }
                LoopHandler.postDelayed(looper, 1);
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    };

    /**
     * Verify and initiate a new image capture
     */
    private void startImageCapture() {
        boolean isReady = mReady.get();
        Log.d(TAG, "Ready for another capture? " + isReady);
        if (isReady) {
            setReady(false);
            mBackgroundHandler.post(mBackgroundClickHandler);
        } else {
            Log.i(TAG, "Sorry, processing hasn't finished. Try again in a few seconds");
        }
    }

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener(){
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();
            // get image bytes
            ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
            final byte[] imageBytes = new byte[imageBuf.remaining()];
            imageBuf.get(imageBytes);
            image.close();
            onPictureTaken(imageBytes);
        }
    };

    private void setReady(boolean ready) {
        mReady.set(ready);
    }


    private void setgpioValue(Gpio GPIOS,boolean value) {
        try {
            GPIOS.setValue(value);
        } catch (IOException e) {
            Log.e(TAG, "Error updating GPIO value", e);
        }
    }

    private boolean getgpioValue(Gpio gpios) {
        try {
            return gpios.getValue();
        } catch (IOException e) {
            Log.e(TAG, "Error updating GPIO value", e);
        }
        return false;
    }

    public void initializePwm(Pwm pwm) throws IOException {
        pwm.setPwmFrequencyHz(50);
        pwm.setPwmDutyCycle(1);
        pwm.setEnabled(true);
    }

    private void onPictureTaken(final byte[] imageBytes) {
        if (imageBytes != null) {
            Log.e(TAG, "image find");
            setReady(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LoopHandler.removeCallbacks(looper);
        mSensorManager.unregisterListener(mTemperatureListener);
        mSensorManager.unregisterListener(mDistanceListener);
        if (mGpio != null) {
            mGpio.unregisterGpioCallback(mGpioCallback);
            try {
                mGpio.close();
            } catch (IOException e) {
                Log.e(TAG, "Unable to close GPIO", e);
            }
        }
        if (mPwm != null) {
            try {
                mPwm.close();
                mPwm = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close PWM", e);
            }
        }
        if (LEDGpio != null) {
            try {
                LEDGpio.close();
                LEDGpio = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close LED", e);
            }
        }
        if (InfraredSensor != null) {
            InfraredSensor.unregisterGpioCallback(InfraredSensorCallback);
            try {
                InfraredSensor.close();
                InfraredSensor = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close InfraredSensor", e);
            }
        }
        try {
            if (mBackgroundThread != null) mBackgroundThread.quit();
        } catch (Throwable t) {
            // close quietly
        }
        mBackgroundThread = null;
        mBackgroundHandler = null;
        try {
            if (mCamera != null) mCamera.shutDown();
        } catch (Throwable t) {
            // close quietly
        }
    }

}
