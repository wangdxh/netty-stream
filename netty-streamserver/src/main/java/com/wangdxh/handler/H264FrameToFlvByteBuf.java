package com.wangdxh.handler;

import com.wangdxh.streamhub.StreamFrame;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.ReferenceCountUtil;

import java.util.List;

public class H264FrameToFlvByteBuf extends MessageToMessageEncoder<StreamFrame> {
    private boolean bflvheadsended = false;
    private int dwtime = 0;

    @Override
    protected void encode(ChannelHandlerContext ctx, StreamFrame streamFrame, List<Object> list) throws Exception {
        //System.out.printf("encode thread %s\n", Thread.currentThread().getName());
        if (false == bflvheadsended && ProduceFlvHead(ctx, streamFrame, list) == false){
            return;
        }
        bflvheadsended = true;
        this.ProduceH264(ctx, streamFrame, list);
    }

    @Override
    public boolean acceptOutboundMessage(Object msg) throws Exception {
        return super.acceptOutboundMessage(msg);
    }

    protected Object WrappedFlvBuf(ByteBuf buf)
    {
        return buf;
    }

    private boolean ProduceFlvHead(ChannelHandlerContext ctx, StreamFrame streamFrame, List<Object> list){
        ByteBuf h24frame = streamFrame.content();

        ByteBuf buf = h24frame.slice();
        ByteBuf sps = null, pps = null;
        int dwindex = 0;
        while (buf.isReadable()){
            int nalsize = buf.readInt();
            dwindex += 4;
            byte type = buf.readByte();
            if ((type & 0x1f) == 7){
                sps = h24frame.slice(dwindex, nalsize);
            } else if ((type & 0x1f) == 8){
                pps = h24frame.slice(dwindex, nalsize);
            }
            dwindex += nalsize;

            if (null != sps && null != pps){ break;}
            //except the type byte
            buf.skipBytes(nalsize-1);

        }
        if (null == sps || null == pps){
            return false;
        }

        ByteBuf flvhead = ctx.alloc().directBuffer(1024 + sps.readableBytes() + pps.readableBytes());
        flvhead.writeBytes(new byte[]{0x46, 0x4c, 0x56, 0x01, 0x01, 0x00, 0x00, 0x00, 0x09, 0x00, 0x00, 0x00, 0x00});
        int taglen = 1 + 1 + 3 + (1 + 3 + 2 + 2 + sps.readableBytes() + 1 + 2 + pps.readableBytes());
        flvhead.writeByte(9).writeMedium(taglen).writeZero(7);// 1+3+7
        flvhead.writeByte(0x17).writeZero(4).writeByte(0x01);
        flvhead.writeBytes(sps, 1, 3).writeByte(0xff).writeByte(0xe1);

        flvhead.writeShort(sps.readableBytes()).writeBytes(sps);
        flvhead.writeByte(1).writeShort(pps.readableBytes()).writeBytes(pps);
        flvhead.writeInt(taglen + 11);

        list.add(WrappedFlvBuf(flvhead));

        return true;
    }

    protected boolean ProduceH264(ChannelHandlerContext ctx, StreamFrame streamFrame, List<Object> list){
        streamFrame.dwTime = this.dwtime;
        ByteBuf buf = streamFrame.content();
        byte type = buf.getByte(4);
        streamFrame.bIsKey = ((type&0x1f) == 5) || ((type&0x1f) == 7);

        int taglen = 5 + buf.readableBytes();
        ByteBuf buftag = ctx.alloc().directBuffer(16, 16);
        // 11 bytes
        buftag.writeByte(0x9).writeMedium(taglen).writeMedium(streamFrame.dwTime);
        buftag.writeByte((streamFrame.dwTime>>24)&0xff).writeZero(3);

        // 5 bytes
        buftag.writeByte(streamFrame.bIsKey ? 0x17 : 0x27);
        buftag.writeByte(1).writeZero(3);

        //list.add(buftag);
        list.add(WrappedFlvBuf(buftag));


        ReferenceCountUtil.retain(streamFrame.content());
        //list.add(streamFrame.content());
        list.add(WrappedFlvBuf(streamFrame.content()));

        ByteBuf bufend = ctx.alloc().directBuffer(4, 4).writeInt(taglen + 11);
        //list.add(bufend);
        list.add(WrappedFlvBuf(bufend));
        this.dwtime += 30;
        return true;
    }
}
