package com.wangdxh;

import com.wangdxh.handler.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.rtsp.RtspDecoder;
import io.netty.handler.codec.rtsp.RtspEncoder;
import io.netty.util.ResourceLeakDetector;

import java.nio.ByteOrder;

/**
 *
 *
 */
public class App {
    private static Bootstrap udpstrap = new Bootstrap();
    public static void initUdp(EventLoopGroup group)
    {
        udpstrap.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_SNDBUF, 1024*1024)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel nioDatagramChannel) throws Exception {
                        nioDatagramChannel.pipeline().addLast(new UdpHandler());
                    }
                })
                .option(ChannelOption.SO_BROADCAST, false);
    }
    public static Channel createUdp(String strip, int port)
    {
        try
        {
            System.out.printf("start udp bind %s %d \n", strip, port);
            Channel n = udpstrap.bind(strip, port).sync().channel();
            System.out.printf("end udp bind %s %d \n", strip, port);
            System.out.println(n);
            return n;
        }
        catch (InterruptedException e)
        {
            return null;
        }
    }

    public static void main(String[] args) {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);

        EventLoopGroup listenGrp = new NioEventLoopGroup(1);
        EventLoopGroup workGrp = new NioEventLoopGroup(4);
        initUdp(workGrp);

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(listenGrp, workGrp)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_RCVBUF, 1 * 1024 * 1024)
                    .childOption(ChannelOption.SO_SNDBUF, 64 * 1024)
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(32 * 1024, 64 * 1024))
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new LengthFieldBasedFrameDecoder(
                                    ByteOrder.LITTLE_ENDIAN,
                                    1 * 1024 * 1024, 0,
                                    4, 0, 4, true));
                            socketChannel.pipeline().addLast(new ServerHandler());
                        }
                    });

            ServerBootstrap httpstrap = b.clone();
            httpstrap.childOption(ChannelOption.SO_RCVBUF, 64 * 1024)
                    .childOption(ChannelOption.SO_SNDBUF, 1024 * 1024)
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1024 * 1024 / 2, 1024 * 1024))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin().allowNullOrigin().allowCredentials().build();
                            socketChannel.pipeline()
                                    .addLast(new HttpResponseEncoder())
                                    .addLast(new HttpRequestDecoder())
                                    .addLast(new HttpObjectAggregator(64 * 1024))
                                    .addLast(new CorsHandler(corsConfig))//.addLast(new ChunkedWriteHandler())
                                    .addLast(new H264FrameToFlvByteBuf())
                                    .addLast(new HttpFlvHandler());
                        }
                    });

            ServerBootstrap wsstrap = b.clone();
            wsstrap.childOption(ChannelOption.SO_RCVBUF, 64 * 1024)
                    .childOption(ChannelOption.SO_SNDBUF, 1024 * 1024)
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1024 * 1024 / 2, 1024 * 1024))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline()
                                    .addLast(new HttpServerCodec())
                                    .addLast(new HttpObjectAggregator(64 * 1024))
                                    .addLast(new WebSocketServerProtocolHandler("/live/liveflv", null, false,
                                                                                512*1024, false, true))
                                    .addLast(new H264FrameToFlvWebsocketFrame())
                                    .addLast(new WebSocketHandler());
                        }
                    });

            ServerBootstrap rtspstrap = b.clone();
            rtspstrap.childOption(ChannelOption.SO_RCVBUF, 64 * 1024)
                    .childOption(ChannelOption.SO_SNDBUF, 64 * 1024)
                    .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(64 * 1024 / 2, 64 * 1024))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline()
                                    .addLast(new RtspDecoder())
                                    .addLast(new RtspEncoder())
                                    .addLast(new HttpObjectAggregator(64 * 1024))
                                    .addLast(new RtspHandler());
                        }
                    });

            ChannelFuture f = b.bind(1985).sync();
            httpstrap.bind(1984).sync();
            wsstrap.bind(1983).sync();
            rtspstrap.bind(554).sync();

            System.out.println("Moriturus te saluto!!!");

            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            listenGrp.shutdownGracefully();
            workGrp.shutdownGracefully();
        }
    }
}
