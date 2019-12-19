package com.lzj.nettydemo.client;

import android.os.SystemClock;
import android.util.Log;

import com.lzj.nettydemo.LogUtils;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class Client extends Thread{

    public static String host = "192.168.11.237";

    public static int port = 8081;

    private static int reconnectNum = 3;//重连次数

    private static boolean isConnect = false;//是否连接

//    public ChannelFuture channelFuture = null;
    public  Channel channel = null;
    public EventLoopGroup group;

    public void run(){
        super.run();
        reconnectNum = 3;
        connectServer();
    }





    public void sendMsgToServer(String msg) {
        LogUtils.log( "sendMsgToServer msg"+msg);
        if(isConnect){
            channel.writeAndFlush(msg);
        }
//            channel.writeAndFlush(data + System.getProperty("line.separator")).addListener(new ChannelFutureListener() {
//                @Override
//                public void operationComplete(ChannelFuture future) throws Exception {
//
//                }
//            });
    }

    public void disconnect() {
        LogUtils.log( "disconnect");
        group.shutdownGracefully();
    }



    public void reconnect() {
        LogUtils.log( "reconnect");
        if (reconnectNum>0) {
            reconnectNum--;
            if (!isConnect) {
                LogUtils.log("重新连接");
                connectServer();
            }
        }
    }


    private void connectServer(){
        group = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap  = new Bootstrap()
                    .group(group)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,5*1000)
                    .channel(NioSocketChannel.class);
//                    .handler(new ClientHandler());
             channel = bootstrap.connect(host, port).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        LogUtils.log("连接成功");
                        isConnect = true;
                    } else {
                        isConnect = false;
                        LogUtils.log("连接失败");
                    }
                }
            }).sync().channel();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            isConnect = false;
            if(channel!=null&&channel.isOpen()){
                channel.close();
            }
            group.shutdownGracefully();//关闭NioEventLoopGroup
            reconnect();//重连
        }
    }

}
