A demo for camera streaming H.264,AAC using RTMP on Android

# 介绍

## 干什么的

**CameraStreaming** 是一个Android(4.0以上)摄像头实时输出到RTMP服务器的直播演示

在网上找了一圈，关于Android 直播的实现，大都指向RTP/RTSP方式的直播。经过一番摸索，踩了不少坑。

1. libstreaming 这个库提供了一个很好的RTP/RTSP直播基础。然而它在Android 5.0上不能使用，原因是Android5.0对于LocalSocket的使用做了安全限制。
2. to be continue.

# Licensing

This streaming stack is available under two licenses, the GPL and a commercial license. *If you are willing to integrate this project into a close source application, please contact me at dawenhing at gmail.com*. Thank you.
