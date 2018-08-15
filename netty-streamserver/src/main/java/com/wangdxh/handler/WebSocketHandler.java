package com.wangdxh.handler;

import com.wangdxh.streamhub.StreamFrame;
import com.wangdxh.streamhub.StreamFrameSink;
import com.wangdxh.streamhub.StreamHub;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.StringUtil;

public class WebSocketHandler extends SimpleChannelInboundHandler<WebSocketFrame> implements StreamFrameSink
{
    protected String strDeviceid = "";
    protected Channel chn;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) throws Exception
    {
        System.out.println("websocket chnnel read0");
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
    {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete)
        {
            WebSocketServerProtocolHandler.HandshakeComplete complete = (WebSocketServerProtocolHandler.HandshakeComplete)evt;
            QueryStringDecoder uri = new QueryStringDecoder(complete.requestUri());
            System.out.println(uri.path());
            if (!uri.path().equals("/live/liveflv") || !uri.parameters().containsKey("deviceid"))
            {
                this.CloseThisClient();
                return;
            }
            strDeviceid = uri.parameters().get("deviceid").get(0);
            if (StringUtil.isNullOrEmpty(strDeviceid))
            {
                this.CloseThisClient();
                return;
            }

            StreamHub.EnterStream(strDeviceid, this);
            System.out.printf("%s enter stream %s from websocket\n", chn.id(), strDeviceid);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
        super.channelActive(ctx);
        chn = ctx.channel();
        System.out.printf("%s new connection %s\n", chn.id(), Thread.currentThread().getName());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        super.channelInactive(ctx);
        System.out.printf("%s i am dead\n", ctx.channel().id());
        if (!StringUtil.isNullOrEmpty(strDeviceid))
        {
            // from stream hub clear this info
            System.out.printf("%s will leave stream %s \n", chn.id(), strDeviceid);
            StreamHub.LeaveStream(strDeviceid, this);
            strDeviceid = "";
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        System.out.println("exception caught");
        cause.printStackTrace();
        ctx.channel().close();
    }

    @Override
    public void CloseThisClient()
    {
        if (this.chn.isActive())
        {
            System.out.println("i kill myself");
            this.chn.close();
        }
    }

    @Override
    public boolean WriteFrame(StreamFrame frame)
    {
        //System.out.printf("%s writeframe active %b writeable %b \n", chn.id(), chn.isActive(), chn.isWritable());
        if (this.chn.isActive() && this.chn.isWritable())
        {
            ReferenceCountUtil.retain(frame);
            chn.writeAndFlush(frame);
            return true;
        }
        return false;
    }
}
