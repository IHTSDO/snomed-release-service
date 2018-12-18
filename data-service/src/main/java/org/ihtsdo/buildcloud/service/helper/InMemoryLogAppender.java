package org.ihtsdo.buildcloud.service.helper;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class InMemoryLogAppender extends AppenderSkeleton{

    private List<LogOutputMessage> messages;
    private String trackerId;

    public InMemoryLogAppender(String trackerId) {
        this.trackerId = trackerId;
        messages = new ArrayList<>();
    }


    @Override
    protected void append(LoggingEvent loggingEvent) {
        Object mdcValue = loggingEvent.getMDC("trackerId");
        if(mdcValue != null && trackerId.equalsIgnoreCase(mdcValue.toString())) {
            LogOutputMessage message = new LogOutputMessage(loggingEvent.getLevel().toString(), loggingEvent.getRenderedMessage(), new Date().getTime());
            messages.add(message);
        }
    }

    public List<LogOutputMessage> getMessages() {
        return messages;
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    @Override
    public void close() {

    }
}
