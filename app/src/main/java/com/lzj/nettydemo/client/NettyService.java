package com.lzj.nettydemo.client;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import com.lzj.nettydemo.LogUtils;
import com.lzj.nettydemo.MyApplication;

import org.json.JSONObject;


import java.util.UUID;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;

public class NettyService extends Service {

    public static String host = "192.168.11.237";

    public static int port = 8081;

    public static boolean isConnect = false;//是否连接

    private boolean isNeedReconnect = true;//是否需要重连


    public ChannelFuture channelFuture = null;

//    public Channel channel = null;
    public EventLoopGroup group;

    private NettyServiceBinder mBinder = new NettyServiceBinder();

    //用于Activity和service通讯
    public class NettyServiceBinder extends Binder {
        public NettyService getService() {
            return NettyService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        LogUtils.log("onStartCommand NettyService  "+NettyService.this);
        isNeedReconnect = true;
        isConnect = false;
        connectServer();
        return super.onStartCommand(intent, flags, startId);
     }


    /**
     * 发送消息
     * @param msg
     */
    public void sendMsg(String msg) {
        LogUtils.log( "sendMsg msg"+msg);
        if (channelFuture != null && channelFuture.channel().isActive()) {
            channelFuture.channel().writeAndFlush(msg + "\r\n").addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (channelFuture.isSuccess()) {
                        LogUtils.log(  "sendMsg successful");
                    } else {
                        LogUtils.log( "sendMsg error");
                    }
                }
            });
        }

    }

    //发送心跳文字消息
    public void sendHeartBeatMessage(String requestKey) {
        LogUtils.log("NettyService  sendHeartBeatMessage  ");
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("messageType", "");
            jsonObject.put("to", "123456");
//            jsonObject.put("to", Utils.getUserID(JWebSocketClientService.this));
            jsonObject.put("sourceType", "heartbeat");
            jsonObject.put("requestKey", requestKey);
            JSONObject jsonContent = new JSONObject();
            jsonContent.put("text", "");
            jsonObject.put("content", jsonContent);
            String message = jsonObject.toString();
            sendMsg(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭连接
     */
    public void disconnect() {
        LogUtils.log( "disconnect");
        isNeedReconnect = false;
        mHandler.removeCallbacks(heartBeatRunnable);//关闭心跳
        group.shutdownGracefully();
    }

    public boolean getConnectStatus(){
        return isConnect;
    }

    public void setConnectStatus(boolean status){
        this.isConnect = status;
    }


    /**
     * 重新连接
     */
    public void reconnect() {
        LogUtils.log( "reconnect");
        LogUtils.log( "isNeedReconnect  "+isNeedReconnect);
        LogUtils.log( "isConnect  "+isConnect);
        if ( isNeedReconnect && !isConnect) {
            if (null != channelFuture) {
                if (channelFuture.channel() != null && channelFuture.channel().isOpen()) {
                    channelFuture.channel().close();
                }
            }
            group.shutdownGracefully();
            connectServer(); //连接服务器
        }
    }


    //    -------------------------------------websocket心跳检测------------------------------------------------
    private static final long HEART_BEAT_RATE = 10 * 1000;//每隔10秒进行一次对长连接的心跳检测
    private Handler mHandler = new Handler();
    private Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            sendHeartBeatMessage(UUID.randomUUID() + "");

            //每隔一定的时间，对长连接进行一次心跳检测
            mHandler.postDelayed(this, HEART_BEAT_RATE);
        }
    };



    /**
     * 连接服务器
     */
    public void connectServer() {

        if (!isConnect) {
            try {
                //进行初始化
                group = new NioEventLoopGroup();
                Bootstrap bootstrap = new Bootstrap()
                        .group(group)
                        .option(ChannelOption.TCP_NODELAY, true) //无阻塞
                        .option(ChannelOption.SO_KEEPALIVE, true) //长连接
                        .option(ChannelOption.SO_TIMEOUT, 30 * 1000) //收发超时
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000)
                        .option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(5000, 5000, 8000)) //接收缓冲区 最小值太小时数据接收不全
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ch.pipeline()
                                        .addLast(new IdleStateHandler(30, 30, 0)) //心跳包  参数1：代表读套接字超时的时间，例如30秒没收到数据会触发读超时回调;参数2：代表写套接字超时时间，例如10秒没有进行写会触发写超时回调;参数3：将在未执行读取或写入时触发超时回调，0代表不处理；读超时尽量设置大于写超时代表多次写超时时写心跳包，多次写了心跳数据仍然读超时代表当前连接错误，即可断开连接重新连接
                                        .addLast(new StringDecoder(CharsetUtil.UTF_8))  //接收解码方式
                                        .addLast(new StringEncoder(CharsetUtil.UTF_8))  //发送编码方式
                                        .addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()))//黏包处理
                                        .addLast(new ClientHandler(NettyService.this)); //处理数据接收
                            }
                        });

                //开始建立连接并监听返回
                channelFuture = bootstrap.connect(host, port);
                channelFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            LogUtils.log("connect success");
                            isConnect = true;
                            mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);//开启心跳检测
                        } else {
                            LogUtils.log("connect failed");
                            isConnect = false;
                        }
                    }
                }).sync();

                //等待客户端链路关闭，当客户端连接关闭之后，客户端主函数退出，退出之前释放NIO线程组的资源
//              channelFuture.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
                if (null != channelFuture) {
                    if (channelFuture.channel() != null && channelFuture.channel().isOpen()) {
                        channelFuture.channel().close();
                    }
                }
                group.shutdownGracefully();
                reconnect();
            }

//            finally {
//                group.shutdownGracefully();
//            }


        }
    }
}
