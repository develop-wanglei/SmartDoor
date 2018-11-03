package com.example.jasonlss.atplatformexample;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
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
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity{
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String BUTTON_PIN_NAME = "BCM26"; // GPIO 4/5/6/12/16/17/22/23/24/25/26/27
    private static final String LED_PIN_NAME = "BCM5"; // GPIO 4/5/6/12/16/17/22/23/24/25/26/27
    private static final String PWM_NAME ="PWM1";//pwm1-13 PWM0-18
    private static final String InfraredSensor_NAME ="BCM16";
    private static final String Trig_NAME ="BCM6";
    private static final String Echo_NAME ="BCM12";
    private final GpioCallback InfraredSensorCallback =new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            try {
                if (gpio.getValue()) {
                    Log.e("有人来了", gpio.getValue() + ":1111111111111");
                } else {
                    Log.e("没有人", gpio.getValue() + ":222222222222");
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
    private int NumberTime = 0;
    private long StartTime, EndTime;
    private float Distance;
    private boolean mState = false;
    private boolean needMeasure;
    private long TimeTicket=0;
    private Gpio mGpio;//开关
    private Gpio LEDGpio;//LED
    private Gpio InfraredSensor;//红外传感器
    private Gpio trigGpio;
    private Gpio echoGpio;//超声波传感器
    private Pwm mPwm;
    private Camera mCamera;//相机
    private HandlerThread mCameraThread;
    private Handler LoopHandler = new Handler();
    private GpioCallback UltrasonicCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            Log.e("状态：", NumberTime+" 改变");
                if (getgpioValue(echoGpio)) {
                    needMeasure=true;
                    StartTime = System.nanoTime();
                }
                if(!getgpioValue(echoGpio)&&needMeasure) {
                    needMeasure = false;
                    EndTime = System.nanoTime() - StartTime;
                    EndTime = TimeUnit.NANOSECONDS.toMicros(EndTime);
                    Distance = EndTime / 58;
                    Log.e("距离为：", " " + Distance + " CM");
                    NumberTime++;

                }
            return true;
        }

        @Override
        public void onGpioError(Gpio gpio, int error) {
            Log.w("123", gpio + ": Error event " + error);
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
                if(TimeTicket%10000==0)
                {
                    mCamera.takePicture();
                }
               if(TimeTicket%2000==0)
               {
                   mState = !mState;
                   LEDGpio.setValue(mState);
               }
                if(TimeTicket%100==0)
                {
                    try {
                        Log.e("状态：", NumberTime+" 输出");
                        trigGpio.setValue(true);//输出高电平
                        Thread.sleep((long) 0.020);//高电平输出15US
                        trigGpio.setValue(false);//恢复低电平
                    } catch (IOException e) {
                        Log.e("123", "Error on PeripheralIO API", e);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                LoopHandler.postDelayed(looper, 1);
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    };
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
            trigGpio = Manager.openGpio(Trig_NAME);
            echoGpio = Manager.openGpio(Echo_NAME);
            trigGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            echoGpio.setDirection(Gpio.DIRECTION_IN);
            trigGpio.setActiveType(Gpio.ACTIVE_HIGH);
            echoGpio.setActiveType(Gpio.ACTIVE_HIGH);
            echoGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            echoGpio.registerGpioCallback(UltrasonicCallback);
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
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // A problem occurred auto-granting the permission
            Log.e(TAG, "No permission");
            return;
        }
        mCameraThread = new HandlerThread("CameraBackground");
        mCameraThread.start();
        Handler mCameraHandler = new Handler(mCameraThread.getLooper());
        mCamera = Camera.getInstance();
        mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener);
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
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LoopHandler.removeCallbacks(looper);
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
        if (trigGpio != null) {
            try {
                trigGpio.close();
                trigGpio = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close trig", e);
            }
        }
        if (echoGpio != null) {
            echoGpio.unregisterGpioCallback(UltrasonicCallback);
            try {
                echoGpio.close();
                echoGpio = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close Ultrasonic", e);
            }
        }

        mCamera.shutDown();
        mCameraThread.quitSafely();
    }



}
