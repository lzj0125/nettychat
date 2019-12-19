package com.lzj.nettydemo;

import android.util.Log;

public class LogUtils
{
  public static boolean isShow = true;
  
  public static void log(String msg)
  {
    if (isShow) {
      Log.i("netty_chat", msg);
    }
  }
}

