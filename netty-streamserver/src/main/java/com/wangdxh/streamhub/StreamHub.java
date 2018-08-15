package com.wangdxh.streamhub;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class StreamHub {
    private static Map<String, Map<StreamFrameSink, StreamFrameSink>> sinkmap = new ConcurrentHashMap<>();

    public static Map<StreamFrameSink, StreamFrameSink> GetStream(String name){
        if (sinkmap.containsKey(name)){
            return sinkmap.get(name);
        }
        Map<StreamFrameSink, StreamFrameSink> map = new ConcurrentHashMap<>();
        Map<StreamFrameSink, StreamFrameSink> retmap = sinkmap.putIfAbsent(name, map);
        return retmap == null ? map : retmap;
    }

    public static void EnterStream(String name, StreamFrameSink sink){
        GetStream(name).put(sink, sink);
    }
    public static void LeaveStream(String name, StreamFrameSink sink){
        GetStream(name).remove(sink);
    }

    public static void WriteFrame(Map<StreamFrameSink, StreamFrameSink> map, StreamFrame frame){
        //System.out.printf("input thread %s\n", Thread.currentThread().getName());
        for(StreamFrameSink key : map.keySet()){
            key.WriteFrame(frame);
        }
    }
    public static Set<String> GetStreams()
    {
        return sinkmap.keySet();
    }
}
