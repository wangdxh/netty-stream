package com.wangdxh.handler;

import com.wangdxh.streamhub.StreamFrame;
import com.wangdxh.streamhub.StreamFrameSink;
import com.wangdxh.streamhub.StreamHub;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

import java.util.Map;

public class ServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private String strId = "unsetted";
    private String strDevice = "not named";
    private int first = 0;
    private Map<StreamFrameSink, StreamFrameSink> map;
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf){
        //System.out.printf("%s %d \n", strId, byteBuf.readableBytes());

        if (0 == first) {
            first = 1;
            strDevice = byteBuf.toString(CharsetUtil.UTF_8);
            System.out.println(strDevice);
            map = StreamHub.GetStream(strDevice);
        } else {
            //read bytes
            StreamFrame frame = new StreamFrame(byteBuf);
            StreamHub.WriteFrame(map, frame);
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
        System.out.printf("%s: writeable change : %b\n", strId, ctx.channel().isWritable());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        strId = ctx.channel().id().asShortText();
        super.channelActive(ctx);
        System.out.printf("%s:new connection %s\n", strId, Thread.currentThread().getName());
        System.out.printf("%s %s\n", strId, ctx.channel().config().getWriteBufferWaterMark().toString());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        System.out.printf("%s:i am dead\n", strId);
        ctx.channel().close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("exception caught");

        cause.printStackTrace();
        ctx.channel().close();
    }
}
