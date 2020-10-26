package com.single.code.shotfix;

import android.app.Application;
import android.content.Context;

import com.single.patch.hotfix.PatchHotFix;

import java.io.File;

/**
 * 创建时间：2020/10/12
 * 创建人：singleCode
 * 功能描述：
 **/
public class UIApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        PatchHotFix.installPatch(this, new File("/sdcard/patch.jar"));
    }
}
