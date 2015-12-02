package tv.inhand.streaming.rtmp;

import org.red5.io.utils.ObjectMap;
import org.red5.server.messaging.IMessage;
import org.red5.server.net.rtmp.INetStreamEventHandler;
import org.red5.server.net.rtmp.RTMPClient;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.service.IPendingServiceCall;
import org.red5.server.service.IPendingServiceCallback;
import org.red5.server.stream.message.RTMPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by jinchudarwin on 15/12/2.
 */
public class RTMPPublisher implements INetStreamEventHandler, IPendingServiceCallback {
    private static Logger log = LoggerFactory.getLogger(RTMPPublisher.class);

    private List<IMessage> frameBuffer = new ArrayList<IMessage>();

    public static final int STOPPED = 0;

    public static final int CONNECTING = 1;

    public static final int STREAM_CREATING = 2;

    public static final int PUBLISHING = 3;

    public static final int PUBLISHED = 4;

    private String host;

    private int port;

    private String app;

    private int state;

    private String publishName;

    private int streamId;

    private String publishMode;

    private RTMPClient rtmpClient;

    public int getState() {
        return state;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public synchronized void start(String publishName, String publishMode, Object[] params) {
        state = CONNECTING;
        this.publishName = publishName;
        this.publishMode = publishMode;
        rtmpClient = new RTMPClient();

        Map<String, Object> defParams = rtmpClient.makeDefaultConnectionParams(host, port, app);
        rtmpClient.connect(host, port, defParams, this, params);
    }

    public synchronized void stop() {
        if (state >= STREAM_CREATING) {
            rtmpClient.disconnect();
        }
        state = STOPPED;
    }

    synchronized public void pushMessage(IMessage message) throws IOException {
        if (state >= PUBLISHED && message instanceof RTMPMessage) {
            RTMPMessage rtmpMsg = (RTMPMessage) message;
            rtmpClient.publishStreamData(streamId, rtmpMsg);
        } else {
            frameBuffer.add(message);
        }
    }

    public synchronized void onStreamEvent(Notify notify) {
        log.debug("onStreamEvent: {}", notify);
        ObjectMap<?, ?> map = (ObjectMap<?, ?>) notify.getCall().getArguments()[0];
        String code = (String) map.get("code");
        log.debug("<:{}", code);
        if (StatusCodes.NS_PUBLISH_START.equals(code)) {
            state = PUBLISHED;
            while (frameBuffer.size() > 0) {
                rtmpClient.publishStreamData(streamId, frameBuffer.remove(0));
            }
        }
    }

    public synchronized void resultReceived(IPendingServiceCall call) {
        log.debug("resultReceived:> {}", call.getServiceMethodName());
        if ("connect".equals(call.getServiceMethodName())) {
            state = STREAM_CREATING;
            rtmpClient.createStream(this);
        } else if ("createStream".equals(call.getServiceMethodName())) {
            state = PUBLISHING;
            Object result = call.getResult();
            if (result instanceof Integer) {
                Integer streamIdInt = (Integer) result;
                streamId = streamIdInt.intValue();
                rtmpClient.publish(streamIdInt.intValue(), publishName, publishMode, this);
            } else {
                rtmpClient.disconnect();
                state = STOPPED;
            }
        }
    }
}
