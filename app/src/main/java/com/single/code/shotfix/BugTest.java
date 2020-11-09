package com.single.code.shotfix;

import android.util.Log;

/**
 * 创建时间：2020/10/25
 * 创建人：singleCode
 * 功能描述：
 **/
public class BugTest {
    public static void test(){
        System.out.println("bug");
        Log.d("BugTest", "test: bug ");
        throw new NullPointerException("哈哈出错啦");
    }
}
