package org.ihtsdo.buildcloud.service.helper;

import java.util.ArrayList;
import java.util.List;

public class LogOutputMessageList {

    private List<LogOutputMessage> messages = new ArrayList<>();

    public LogOutputMessageList(List<LogOutputMessage> messages) {
        this.messages = messages;
    }

    public List<LogOutputMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<LogOutputMessage> messages) {
        this.messages = messages;
    }
}
