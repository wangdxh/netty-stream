package com.wangdxh.handler;

import com.wangdxh.App;
import com.wangdxh.streamhub.StreamFrame;
import com.wangdxh.streamhub.StreamHub;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.rtsp.*;
import io.netty.util.CharsetUtil;
import io.netty.util.internal.StringUtil;

import java.net.InetSocketAddress;

public class RtspHandler extends HttpFlvHandler
{
    private Channel rtpchn;
    private Channel rtcpchn;
    private static int localrtpport = 54000;
    private int remotertpport = 0;
    private int remotertcpport = 0;
    private String strremoteip;

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        super.channelInactive(ctx);
        if (null != rtpchn && rtpchn.isActive())
        {
            rtpchn.close();
            rtcpchn.close();
            System.out.println("close rtp rtcp channel");
        }
    }


    private boolean checkUrl(FullHttpRequest r)
    {
        if (!StringUtil.isNullOrEmpty(strDeviceid))
        {
            return true;
        }

        QueryStringDecoder uri = new QueryStringDecoder(r.uri());

        if (!uri.path().endsWith("/live/livestream") || !uri.parameters().containsKey("deviceid"))
        {
            return false;
        }
        strDeviceid = uri.parameters().get("deviceid").get(0);
        if (StringUtil.isNullOrEmpty(strDeviceid))
        {
            return false;
        }
        return true;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest r) throws Exception
    {
        if (!r.decoderResult().isSuccess())
        {
            System.out.println("decode error");
            CloseThisClient();
            return;
        }
        if (false == checkUrl(r))
        {
            System.out.println("check url error");
            CloseThisClient();
            return;
        }

        FullHttpResponse o = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0, RtspResponseStatuses.OK);
        if (r.method() == RtspMethods.OPTIONS)
        {
            o.headers().add(RtspHeaderValues.PUBLIC, "DESCRIBE, SETUP, PLAY, TEARDOWN");
        } else if (r.method() == RtspMethods.DESCRIBE)
        {
            InetSocketAddress addr = (InetSocketAddress) ctx.channel().localAddress();
            String sdp = String.format("c=IN IP4 %s \nm=video 0 RTP/AVP 96\na=rtpmap:96 H264/90000\n", addr.getHostString());
            o.headers().add(RtspHeaderNames.CONTENT_TYPE, "application/sdp");
            o.content().writeCharSequence(sdp, CharsetUtil.UTF_8);
            o.headers().add(RtspHeaderNames.CONTENT_LENGTH, o.content().writerIndex());

        } else if (r.method() == RtspMethods.SETUP)
        {
            System.out.println(r.headers().get(RtspHeaderNames.TRANSPORT));
            String transport = r.headers().get(RtspHeaderNames.TRANSPORT);
            transport = transport.toLowerCase();

            String[] strlist = transport.split(";");
            if (strlist.length > 0 && strlist[0].equals("rtp/avp"))
            {
                for(String i : strlist)
                {
                    if (i.startsWith("client_port"))
                    {
                        String[] strclientport = i.split("=|-");

                        remotertpport = Integer.parseInt(strclientport[1]);
                        remotertcpport = Integer.parseInt(strclientport[2]);
                        strremoteip = ((InetSocketAddress) ctx.channel().remoteAddress()).getHostString();

                        System.out.println(remotertpport);
                        System.out.println(remotertcpport);
                        System.out.println(strremoteip);
                        if (null == dstaddr)
                        {
                            dstaddr = new InetSocketAddress(strremoteip, remotertpport);
                        }
                        break;
                    }
                }

                o.headers().add(RtspHeaderNames.TRANSPORT,
                        r.headers().get(RtspHeaderNames.TRANSPORT)+String.format(";server_port=%d-%d", localrtpport, localrtpport+1));

                rtpchn = App.createUdp(((InetSocketAddress) ctx.channel().localAddress()).getHostString(), localrtpport);
                rtcpchn = App.createUdp(((InetSocketAddress) ctx.channel().localAddress()).getHostString(), localrtpport+1);
                localrtpport += 2;

            } else{
                System.out.println("error transport exit");
                CloseThisClient();
                return;
            }

            String session = String.format("%08x", (int) (Math.random() * 65536));
            o.headers().add(RtspHeaderNames.SESSION, session);
            System.out.println("setup over");

        } else if (r.method() == RtspMethods.PLAY)
        {
            // send rtp and rtcp to client
            System.out.println("play");
            StreamHub.EnterStream(strDeviceid, this);

        } else if (r.method() == RtspMethods.TEARDOWN)
        {
            System.out.println("teardown");
            CloseThisClient();
            return;
        } else
        {
            System.out.println("unknown message");
            o.setStatus(RtspResponseStatuses.NOT_FOUND);
        }
        sendAnswer(ctx, r, o);
    }
    private int timestamp = 0;
    private int sequence = 0;
    private int rtpssrc = 0x13;
    private InetSocketAddress dstaddr = null;

    private void writeRtpHeader(ByteBuf header, byte bmarker)
    {
        header.writeByte(0x80);
        header.writeByte(96 | (bmarker << 7) );
        header.writeShort(this.sequence++);
        header.writeInt(this.timestamp);
        header.writeInt(this.rtpssrc);
    }

    @Override
    public boolean WriteFrame(StreamFrame frame)
    {
        final int MAX_RTP_PKT_LENGTH = 1400;
        int nretains = 0;
        if (this.chn.isActive() && null != this.dstaddr && rtpchn.isActive())
        {
            ByteBuf h24frame = frame.content();

            ByteBuf buf = h24frame.slice();
            int dwtotallen = buf.readableBytes();
            int dwindex = 0;
            byte bmarker = 0;

            while (buf.isReadable())
            {
                final int nalsize = buf.readInt();
                dwindex += 4;
                if (dwindex + nalsize == dwtotallen) { bmarker = 1; }

                if ((nalsize-1) <= MAX_RTP_PKT_LENGTH)
                {
                    // new datagrampacket header 12
                    // body
                    ByteBuf header = chn.alloc().directBuffer(12);
                    writeRtpHeader(header, bmarker);

                    ByteBuf data = h24frame.retainedSlice(dwindex, nalsize);
                    CompositeByteBuf combuf = chn.alloc().compositeBuffer(2)
                            .addComponent(true, header)
                            .addComponent(true, data);
                    System.out.println(combuf.readableBytes());

                    rtpchn.write(new DatagramPacket(combuf, this.dstaddr));
                    nretains += 1;

                }
                else {
                    final byte bynaltype = buf.getByte(dwindex);
                    byte blastfu = 0;
                    int npacket = (nalsize - 1 + MAX_RTP_PKT_LENGTH - 1) / MAX_RTP_PKT_LENGTH;

                    for(int inxpack = 0; inxpack < npacket; inxpack++)
                    {
                        int npacketsize = MAX_RTP_PKT_LENGTH;
                        int packetinx = dwindex + 1 + inxpack * MAX_RTP_PKT_LENGTH;
                        if (inxpack == npacket - 1)
                        {
                            npacketsize = (nalsize - 1) - inxpack * MAX_RTP_PKT_LENGTH; // 2801 nallen
                            blastfu = 1;
                        }
                        ByteBuf header = chn.alloc().directBuffer(14);
                        writeRtpHeader(header, (byte)(bmarker & blastfu));

                        header.writeByte((bynaltype & 0xe0) | 28);
                        if (inxpack == 0)
                        {
                            header.writeByte((bynaltype & 0x1f) | 0x80);//S
                        }
                        // last pack
                        else if (inxpack == npacket - 1)
                        {
                            header.writeByte((bynaltype & 0x1f) | 0x40);//E
                        } else
                        {
                            header.writeByte(bynaltype & 0x1f);
                        }
                        ByteBuf data = h24frame.retainedSlice(packetinx, npacketsize);
                        CompositeByteBuf combuf = chn.alloc().compositeBuffer(2)
                                .addComponent(true, header)
                                .addComponent(true, data);

                        rtpchn.write(new DatagramPacket(combuf, this.dstaddr));

                        nretains += 1;
                    }
                }
                dwindex += nalsize;
                //except the type byte
                buf.skipBytes(nalsize);
            }
            rtpchn.flush();
            this.timestamp += 30*90;
        }
        return true;
    }

    private void sendAnswer(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse rep)
    {
        final String cseq = req.headers().get(RtspHeaderNames.CSEQ);
        if (cseq != null)
        {
            rep.headers().add(RtspHeaderNames.CSEQ, cseq);
        }
        final String session = req.headers().get(RtspHeaderNames.SESSION);
        if (session != null)
        {
            rep.headers().add(RtspHeaderNames.SESSION, session);
        }
        if (!HttpUtil.isKeepAlive(req))
        {
            ctx.writeAndFlush(rep).addListener(ChannelFutureListener.CLOSE);
        } else
        {
            rep.headers().set(RtspHeaderNames.CONNECTION, RtspHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(rep);
        }
    }
}