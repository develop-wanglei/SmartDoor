package com.example.jasonlss.atplatformexample;

import android.os.Handler;
import android.util.Log;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.GetCallback;
import com.avos.avoscloud.SaveCallback;



public class ScanDatabase {
    private Handler LoopHandler = new Handler();
    private AVObject Led_Data;
    private AVObject temp;
    public boolean Led_state = false;

    public ScanDatabase(String DATACLASS, String ID) {
        Led_Data = AVObject.createWithoutData(DATACLASS, ID);
        Led_Data.put("LED_STATE",false);
        Led_Data.saveInBackground(new SaveCallback() {
            @Override
            public void done(AVException e) {
                Log.d("saved", "success!");
            }
        });
        temp=AVObject.createWithoutData(DATACLASS,ID);
        LoopHandler.post(looper);
    }

    public void Turn_ON_Local ()
    {
        Led_Data.put("LED_STATE",false);
        Led_Data.saveInBackground(new SaveCallback() {
            @Override
            public void done(AVException e) {
                Log.d("saved","local on");
            }
        });
    }
    public void Turn_OFF_Local()
    {
        Led_Data.put("LED_STATE",true);
        Led_Data.saveInBackground();
    }


    private Runnable looper = new Runnable() {
        //    private   AVObject temp=AVObject.createWithoutData(dc,id);

        @Override
        public void run() {

            temp.fetchInBackground(new GetCallback<AVObject>() {
                @Override
                public void done(AVObject avObject, AVException e) {
                    Led_state = avObject.getBoolean("LED_STATE");
                    if (Led_state==true)
                    {
                        Log.d("get","true");
                    }
                    else if (Led_state==false)
                    {
                        Log.d("get","false");
                    }
                }
            });
            LoopHandler.postDelayed(looper, 1000);
        }
    };




}
