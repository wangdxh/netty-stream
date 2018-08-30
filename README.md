# netty-stream
netty stream 

a media server by netty framework

h264 test file path
https://github.com/wangdxh/Desert-Eagle/blob/master/streampushclient/testokmy.h264
format is: 4byte-frame length(Little Endian) follow by h264 raw frame


client push is :
java -jar netty-streampushclient-1.0-SNAPSHOT.jar filepath streamname ip port
streamname deault is 123abcdef32153421
ip is 127.0.0.1 port is 1985

server :
java -jar netty-streamserver-1.0-SNAPSHOT.jar
after server is running, there r 5 tcp ports is listening
1985 accept client push raw h264 stream
1984 http flv
1983 websocket flv
554  rtsp just udp transport
80   list all the stream and the stream's url.

http://127.0.0.1

{"123abcdef32153421": {
  "wsflv": "ws://127.0.0.1:1983/live/liveflv?deviceid=123abcdef32153421",
  "httpflv": "http://127.0.0.1:1984/live/liveflv?deviceid=123abcdef32153421",
  "rtsp": "rtsp://127.0.0.1/live/livestream?deviceid=123abcdef32153421"
}}

when u get the flv url,u can test it in :
