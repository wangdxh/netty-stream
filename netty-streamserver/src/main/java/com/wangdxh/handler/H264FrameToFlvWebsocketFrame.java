package com.wangdxh.handler;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

public class H264FrameToFlvWebsocketFrame extends H264FrameToFlvByteBuf
{
    @Override
    protected Object WrappedFlvBuf(ByteBuf buf)
    {
        return new BinaryWebSocketFrame(buf);
    }
}
