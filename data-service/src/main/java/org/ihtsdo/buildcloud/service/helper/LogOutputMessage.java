package org.ihtsdo.buildcloud.service.helper;


public class LogOutputMessage {

    private String level;
    private String message;
    private Long time;

    public LogOutputMessage() {
    }

    public LogOutputMessage(String level, String message, Long time) {
        this.level = level;
        this.message = message;
        this.time = time;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }
}
