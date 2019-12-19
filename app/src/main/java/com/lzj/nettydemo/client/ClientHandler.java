package com.lzj.nettydemo.client;


import android.provider.SyncStateContract;

import com.lzj.nettydemo.LogUtils;

import java.util.UUID;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

class ClientHandler extends SimpleChannelInboundHandler<String> {

    private NettyService nettyService;
    private int reconnectNum = 5;//重连次数

    public ClientHandler(NettyService nettyService) {
        this.nettyService = nettyService;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        LogUtils.log( "channelRead0  收到消息 "+msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        Channel channel = ctx.channel();
        reconnectNum = 5;
        LogUtils.log( "channelActive  "+channel.remoteAddress()+" =====连接成功回调=====");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        Channel channel = ctx.channel();
        LogUtils.log( "channelInactive  "+channel.remoteAddress()+"  ===== 连接失败=====");
        nettyService.setConnectStatus(false);

        if (nettyService != null) {
            if(reconnectNum>0){
                reconnectNum--;
                nettyService.reconnect();//重新连接
            }else{
                LogUtils.log( "channelInactive  连接5次失败=====");
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);

        Channel channel = ctx.channel();
        LogUtils.log( "exceptionCaught channel.remoteAddress() "+channel.remoteAddress()+"  =====连接异常=====");
        LogUtils.log( "exceptionCaught  cause getCause "+cause.getCause());
        LogUtils.log( "exceptionCaught  cause getMessage "+cause.getMessage());
        cause.printStackTrace();
        ctx.close(); //关闭连接后回调channelInactive会重新调用  nettyService.connectServer();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);

        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            if (idleStateEvent.state().equals(IdleState.WRITER_IDLE)) {
                //写超时，此时可以发送心跳数据给服务器
                LogUtils.log( "userEventTriggered WRITER_IDLE");
//                if(nettyService!=null){
//                    nettyService.sendHeartBeatMessage(UUID.randomUUID() + "");
//                }
            }else if (idleStateEvent.state().equals(IdleState.READER_IDLE)){
                //读超时，此时代表没有收到心跳返回可以关闭当前连接进行重连
                LogUtils.log("userEventTriggered READER_IDLE");
                ctx.close();
            }else if (idleStateEvent.state().equals(IdleState.ALL_IDLE)){
                //读写超时
                LogUtils.log("userEventTriggered ALL_IDLE ");

            }
        }
    }

//
//    @Override
//    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
//        // TODO Auto-generated method stub
//        Channel channel = ctx.channel();
//        System.out.println("-------handlerAdded");
//        System.out.println("Client: "+channel.remoteAddress()+" 加入\n");
////        channels.writeAndFlush("[Server]: "+channel.remoteAddress()+" 加入\n");
////        channels.add(channel);
//    }
//
//    @Override
//    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
//        // TODO Auto-generated method stub
//        Channel channel = ctx.channel();
//        System.out.println("-------handlerRemoved");
//        System.out.println("Client: "+channel.remoteAddress()+" 离开\n");
////        channels.writeAndFlush("[Server]: "+channel.remoteAddress()+" 离开\n");
////        // A closed Channel is automatically removed from ChannelGroup,
////        // so there is no need to do "channels.remove(ctx.channel());"
////        channels.remove(channel);
//    }


}
