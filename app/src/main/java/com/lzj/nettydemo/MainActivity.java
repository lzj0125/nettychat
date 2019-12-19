package com.lzj.nettydemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;

import com.lzj.nettydemo.client.Client;
import com.lzj.nettydemo.client.NettyService;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindService();

        TextView tv =  (TextView)findViewById(R.id.tv);
        TextView tv_connnect =  (TextView)findViewById(R.id.tv_connnect);
        TextView tv_disconnnect =  (TextView)findViewById(R.id.tv_disconnnect);

        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogUtils.log("MainActivity  nettyService  "+nettyService);
                nettyService.sendMsg("我在马路边捡到一分钱");
            }
        });

        tv_connnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nettyService.connectServer();
            }
        });

        tv_disconnnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nettyService.disconnect();
            }
        });




//        Client client = new Client();
//        client.start();

    }




    /**
     * 绑定服务
     */
    private void bindService() {
        if(!mBound){
            Intent bindIntent = new Intent(this, NettyService.class);
            bindService(bindIntent, serviceConnection, BIND_AUTO_CREATE);
        }

    }

    private NettyService.NettyServiceBinder binder;
    private NettyService nettyService;
    boolean mBound = false;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            LogUtils.log("NettyService MainActivity,服务与活动成功绑定");

            binder = (NettyService.NettyServiceBinder) iBinder;
            nettyService = binder.getService();
            mBound = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            LogUtils.log("NettyService MainActivity,服务与活动成功断开");
            nettyService = null;
            mBound = false;
        }
    };



    /**
     * 关闭服务（websocket客户端服务）
     */
    private void closeJWebSClientService() {
        Intent intent = new Intent(this, NettyService.class);
        stopService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(serviceConnection);
            mBound = false;
        }
        closeJWebSClientService();

    }
}
