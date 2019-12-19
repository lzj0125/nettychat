package com.lzj.nettydemo.server;


import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

public class ServerHandler extends SimpleChannelInboundHandler<String> {

    /**
     * A thread-safe Set  Using ChannelGroup, you can categorize Channels into a meaningful group.
     * A closed Channel is automatically removed from the collection,
     */
    private static ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // TODO Auto-generated method stub
        Channel channel = ctx.channel();
        System.out.println("Client ："+channel.remoteAddress()+"  在线\n");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // TODO Auto-generated method stub
        Channel channel = ctx.channel();
        System.out.println("Client ："+channel.remoteAddress()+"  离线\n");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // TODO Auto-generated method stub
        Channel channel = ctx.channel();
        System.out.println("Client ："+channel.remoteAddress()+"  异常\n");
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        // TODO Auto-generated method stub
        Channel channel = ctx.channel();
        System.out.println("-------handlerAdded");
        channels.writeAndFlush("[Server]: "+channel.remoteAddress()+" 加入\n");
        channels.add(channel);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        // TODO Auto-generated method stub
        Channel channel = ctx.channel();
        System.out.println("-------handlerRemoved");
        channels.writeAndFlush("[Server]: "+channel.remoteAddress()+" 离开\n");
        // A closed Channel is automatically removed from ChannelGroup,
        // so there is no need to do "channels.remove(ctx.channel());"
        channels.remove(channel);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        // TODO Auto-generated method stub
        System.out.println("---channelRead0 hava received");
        System.out.println("---channelRead0  msg "+msg);
        Channel inComing = ctx.channel();
        for(Channel channel:channels) {
            if(channel!=inComing) {
                channel.writeAndFlush("["+inComing.remoteAddress()+"]:  "+msg+"\n");
            }else {
                channel.writeAndFlush("[localhost]:  "+msg+"\n");
            }
        }
    }

}
