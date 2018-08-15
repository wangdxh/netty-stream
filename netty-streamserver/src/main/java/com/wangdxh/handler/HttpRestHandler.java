package com.wangdxh.handler;

import com.wangdxh.streamhub.StreamHub;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import net.sf.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class HttpRestHandler extends SimpleChannelInboundHandler<FullHttpRequest>
{
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception
    {
        if (!msg.decoderResult().isSuccess())
        {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }
        if (msg.method() != HttpMethod.GET)
        {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
            return;
        }

        QueryStringDecoder uri = new QueryStringDecoder(msg.uri());
        System.out.println(uri.path());

        ByteBuf buf = null;
        if (uri.path().equals("/"))
        {
            String localip = ((InetSocketAddress)ctx.channel().localAddress()).getHostString();
            Map<String, Map<String, String>> playmap = new HashMap<>();
            for(String key: StreamHub.GetStreams())
            {
                Map<String, String> urllist = new HashMap<>();
                urllist.put("rtsp", String.format("rtsp://%s/live/livestream?deviceid=%s", localip, key));
                urllist.put("httpflv", String.format("http://%s:1984/live/liveflv?deviceid=%s", localip, key));
                urllist.put("wsflv", String.format("ws://%s:1983/live/liveflv?deviceid=%s", localip, key));
                playmap.put(key, urllist);
            }
            JSONObject object = JSONObject.fromObject(playmap);
            buf = ByteBufUtil.writeUtf8(ctx.alloc(), object.toString(2));
        }
        else
        {
            buf = ByteBufUtil.writeUtf8(ctx.alloc(), "url error");
        }

        FullHttpResponse o = new DefaultFullHttpResponse(HttpVersion.HTTP_1_0, HttpResponseStatus.OK, buf);

        o.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/json");
        if (HttpUtil.isKeepAlive(msg))
        {
            ctx.writeAndFlush(o).addListener(ChannelFutureListener.CLOSE);
        }
        else
        {
            o.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(o);
        }
    }

    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status)
    {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        System.out.println("httprest exception caught");
        cause.printStackTrace();
        ctx.channel().close();
    }
}
