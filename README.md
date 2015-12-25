# 介绍

## 干什么的

**CameraStreaming** 是一个Android(4.0以上)摄像头实时输出到RTMP服务器的直播演示

在网上找了一圈，关于Android 直播的实现，大都指向RTP/RTSP方式的直播。经过一番摸索，踩了不少坑。

1. [libstreaming](https://github.com/fyhertz/libstreaming) 这个库提供了一个很好的RTP/RTSP直播基础。然而它在Android 5.0上不能使用，原因是Android5.0对于LocalSocket的使用做了安全限制。解决的办法是改用ParcelFileDescriptor;
2. RTMP协议的实现，在网上找的一个开源的实现,基本可以使用。关于RTMP协议的详细信息可以阅读一下[rtmp_specification_1.0.pdf](https://github.com/dourgulf/CameraStreaming/blob/master/doc/rtmp_specification_1.0.pdf)，[这里有一个blog介绍这个协议](http://www.cnweblog.com/fly2700/archive/2008/04/09/281431.html)，也可以参考我另外开源的一个[C++实现的RTMP客户端]()(还没开放出来）
3. MediaRecorder得到的MP4格式的视频流其实并不适合直播的，因为，MP4文件会在文件技术的时候才写入SPS（序列参数值）和PPS（图片参数集），而SPS和PPS是解码的关键信息，我们直播的时候必须先发送SPS和PPS，后续的视频流才能被服务器正确的接受和处理。参考了libstreaming的做法：先录一个小视频存到sdcard，然后解析这个视频文件，就能取得SPS和PPS，然后把着两个参数保存到Setting里头，后面再次直播的时候就直接从Setting得到SPS和PPS。后续真正直播就直接使用这个SPS和PPS了（这里有个疑问：SPS和PPS是固定的吗？）
4. 视频流的封装格式：4字节的长度（注意是big endian）接NALU数据，NALU的第一个字节是头信息，它的语法如下:

		+---------------+
		|0|1|2|3|4|5|6|7|
		+-+-+-+-+-+-+-+-+
		|F|NRI|  Type  |
		+---------------+
		
5. 音频流格式：
6. RTMP的一些限制：目前测试的结果发现（没有找到相关的规范）：
	* 必须使用44.1K的采样率
	* 必须使用双通道立体声格式
	* 必须使用16位的编码深度。

# Licensing

This streaming stack is available under two licenses, the GPL and a commercial license. *If you are willing to integrate this project into a close source application, please contact me at dawenhing at gmail.com*. Thank you.
