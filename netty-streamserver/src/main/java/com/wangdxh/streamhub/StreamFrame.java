package com.wangdxh.streamhub;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;

public class StreamFrame extends DefaultByteBufHolder
{
    public int dwTime;
    public boolean bIsKey;
    public int streamType;

    public StreamFrame(ByteBuf buf)
    {
        super(buf);
    }
}
