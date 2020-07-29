package org.ihtsdo.buildcloud.service.helper;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Date;
import java.util.List;

public class WebSocketLogAppender extends AppenderSkeleton{

    private SimpMessagingTemplate messagingTemplate;
    private String trackerId;

    public WebSocketLogAppender(SimpMessagingTemplate messagingTemplate, String trackerId) {
        this.messagingTemplate = messagingTemplate;
        this.trackerId = trackerId;
    }

    @Override
    protected void append(LoggingEvent loggingEvent) {
        Object mdcValue = loggingEvent.getMDC("trackerId");
        if(mdcValue != null && trackerId.equalsIgnoreCase(mdcValue.toString())) {
            LogOutputMessage message = new LogOutputMessage(loggingEvent.getLevel().toString(), loggingEvent.getRenderedMessage(), new Date().getTime());
            messagingTemplate.convertAndSend("/queue/messages." + trackerId, message);
        }
    }

    @Override
    public void close() {

    }

    @Override
    public boolean requiresLayout() {
        return false;
    }
}
