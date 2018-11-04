package com.example.jasonlss.atplatformexample;

import android.app.Application;

import com.avos.avoscloud.AVOSCloud;


public class MyLeanCloudApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();


        AVOSCloud.initialize(this,"N69U8zk6ueVrpVEwcDaa7hix-gzGzoHsz","y2dKoDHSMyJ1eGlg6tFTAIT6");

        // 放在 SDK 初始化语句 AVOSCloud.initialize() 后面，只需要调用一次即可
        AVOSCloud.setDebugLogEnabled(true);//app发布后关闭

    }

}
