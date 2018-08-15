package handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    public ClientHandler(String strpath, String strdeviceid)
    {
        this.deviceid = strdeviceid;
        this.filepath = strpath;
    }
    private String filepath;
    private String deviceid;

    private ScheduledFuture<?> myfuture;
    private RandomAccessFile file;
    private byte[] myarray = new byte[1*1024*1024];
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
        System.out.println(byteBuf.readableBytes());
    }

    public void changeH264Nal(byte[] buf)
    {

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        System.out.println("connection active");
        ByteBuf buf = ctx.alloc().directBuffer(256);
        buf.writeCharSequence(deviceid, CharsetUtil.UTF_8);
        ctx.writeAndFlush(buf);

        file = new RandomAccessFile(filepath, "r");

        myfuture = ctx.channel().eventLoop().scheduleWithFixedDelay(() ->
        {
            try
            {
                int nread = file.read(myarray, 0, 4);
                if (-1 == nread)
                {
                    file.seek(0);
                    return;
                }

                int b0 = myarray[0] & 0xFF;
                int b1 = myarray[1] & 0xFF;
                int b2 = myarray[2] & 0xFF;
                int b3 = myarray[3] & 0xFF;
                int nbytes = (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
                System.out.println(nbytes);

                int nbytereads = file.read(myarray, 0, nbytes);
                H264FrameInfo frame = new H264FrameInfo(myarray, nbytes);
                ByteBuf bufdata = ctx.channel().alloc().directBuffer(nbytes);
                frame.toBytebuf(bufdata);
                ctx.writeAndFlush(bufdata);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

        }, 30, 30, TimeUnit.MILLISECONDS);

    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        if (null != myfuture)
        {
            myfuture.cancel(false);
        }
        if (null != file)
        {
            file.close();
        }
        System.out.println("i am dead");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("exception cautht");
        cause.printStackTrace();
        ctx.channel().close();
    }
}
