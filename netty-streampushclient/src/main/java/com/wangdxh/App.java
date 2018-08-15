package com.wangdxh;

import com.wangdxh.handler.ClientHandler;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldPrepender;

import java.io.File;
import java.nio.ByteOrder;

/**
 *
 *
 */
public class App {
    //

    public static void main(String[] args) {
        System.out.println("commond params: filepath  devicename ip port");

        String filepath = "c:\\testmy.h264";
        if (args.length >= 1)
        {
            filepath = args[0];
        }
        File file = new File(filepath);
        if (false == file.exists())
        {
            System.out.printf("file %s not exits\n", filepath);
            return;
        }

        String strdeviceid = "123abcdef32153421";
        if (args.length >= 2)
        {
            strdeviceid = args[1];
        }

        String strip = "127.0.0.1";
        if (args.length >= 3)
        {
            strip = args[2];
        }

        short wport = 1985;
        if (args.length >= 4)
        {
            int nport = Integer.parseInt(args[3]);
            if (nport >= 65535 || nport < 1)
            {
                System.out.println("port number is error");
                return;
            }
            wport = (short)nport;
        }

        EventLoopGroup eloop = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(eloop);
            final String strmypath = filepath;
            final String strdev = strdeviceid;
            b.channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new LengthFieldPrepender(ByteOrder.LITTLE_ENDIAN,
                                    4, 0, false));
                            socketChannel.pipeline().addLast(new ClientHandler(strmypath, strdev));
                        }
                    });

            while (true)
            {
                Channel chn = b.connect(strip, wport).addListener((ChannelFutureListener) channelFuture -> {
                    if (channelFuture.isSuccess())
                    {
                        System.out.println("connect ok");
                    }
                    else
                    {
                        System.out.println("connect error");
                    }
                }).channel();

                chn.closeFuture().addListener((ChannelFutureListener) future ->
                {
                    System.out.println("socket error and will connect again");

                }).sync();
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            eloop.shutdownGracefully();
        }

    }
}
