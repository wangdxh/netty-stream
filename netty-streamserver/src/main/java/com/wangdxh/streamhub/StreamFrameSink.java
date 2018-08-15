package com.wangdxh.streamhub;

public interface StreamFrameSink {
    boolean WriteFrame(StreamFrame frame);
    void CloseThisClient();
}
