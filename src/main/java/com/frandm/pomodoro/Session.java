package com.frandm.pomodoro;


public class Session {
    private final String date;
    private final String subject;
    private final String topic;
    private final String description;
    private final int duration;

    public Session(String date, String subject, String topic, String description, int duration) {
        this.date = date;
        this.subject = subject;
        this.topic = topic;
        this.description = description;
        this.duration = duration;
    }

    public String getDate() { return date; }
    public String getSubject() { return subject; }
    public String getTopic() { return topic; }
    public String getDescription() { return description; }
    public int getDuration() { return duration; }
}
