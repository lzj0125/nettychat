package com.lzj.nettydemo;

import android.app.Application;
import android.content.Intent;

import com.lzj.nettydemo.client.NettyService;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        NettyService();
    }

    /**
     * 启动服务（websocket客户端服务）
     */
    private void NettyService() {
        Intent intent = new Intent(this, NettyService.class);
        startService(intent);
    }


}
