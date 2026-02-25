package com.frandm.pomodoro;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class PomodoroEngine {
    public enum State { MENU, WORK, SHORT_BREAK, LONG_BREAK, WAITING }

    private State currentState = State.MENU;
    private State lastActiveState = State.WORK;
    private Timeline timeline;

    private int workMins = 45, shortMins = 15, longMins = 25, interval = 4;
    private boolean autoStartBreaks = false;
    private boolean autoStartPomodoros = false;

    private int secondsRemaining;
    private int secondsElapsed = 0;
    private int sessionCounter = 0;

    private Runnable onTick;
    private Runnable onStateChange;

    public PomodoroEngine() {
        this.secondsRemaining = workMins * 60;
        setupTimeline();
    }

    private void setupTimeline() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (secondsRemaining > 0) {
                secondsRemaining--;
                if (currentState == State.WORK) {
                    secondsElapsed++;
                }
                if (onTick != null) onTick.run();
            } else {
                next();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    public void start() {
        if (currentState == State.MENU || currentState == State.WAITING) {
            if (currentState == State.MENU) {
                currentState = State.WORK;
                secondsElapsed = 0;
            } else {
                currentState = lastActiveState;
            }
        }
        timeline.play();
        if (onStateChange != null) onStateChange.run();
    }

    public void pause() {
        if (currentState != State.WAITING && currentState != State.MENU) {
            timeline.pause();
            lastActiveState = currentState;
            currentState = State.WAITING;
            if (onStateChange != null) onStateChange.run();
        }
    }

    public void next() {
        stop();

        if (currentState == State.WORK || (currentState == State.WAITING && lastActiveState == State.WORK)) {
            sessionCounter++;
            currentState = (sessionCounter % interval == 0) ? State.LONG_BREAK : State.SHORT_BREAK;
        } else {
            currentState = State.WORK;
        }

        resetTimeForState(currentState);

        boolean shouldAutoStart = (currentState == State.WORK && autoStartPomodoros) ||
                ((currentState == State.SHORT_BREAK || currentState == State.LONG_BREAK) && autoStartBreaks);

        if (shouldAutoStart) {
            start();
        } else {
            lastActiveState = currentState;
            currentState = State.WAITING;
            if (onStateChange != null) onStateChange.run();
        }
    }

    public void skip() {
        next();
    }

    public void stop() {
        timeline.stop();
    }

    public void resetTimeForState(State state) {
        this.currentState = state;

        switch (state) {
            case WORK, MENU -> secondsRemaining = workMins * 60;
            case SHORT_BREAK -> secondsRemaining = shortMins * 60;
            case LONG_BREAK -> secondsRemaining = longMins * 60;
            case WAITING -> {}
        }
        if (onTick != null) onTick.run();
        if (onStateChange != null) onStateChange.run();
    }

    public void updateSettings(int w, int s, int l, int i, boolean aBreak, boolean aPomo) {
        this.workMins = w;
        this.shortMins = s;
        this.longMins = l;
        this.interval = i;
        this.autoStartBreaks = aBreak;
        this.autoStartPomodoros = aPomo;

        if (currentState == State.MENU) {
            resetTimeForState(State.MENU);
        }
    }


    public void setWorkMins(int mins) { this.workMins = mins; if(currentState == State.MENU) resetTimeForState(State.MENU); }
    public void setShortMins(int mins) { this.shortMins = mins; }
    public void setLongMins(int mins) { this.longMins = mins; }
    public void setInterval(int interval) { this.interval = interval; }
    public void setAutoStartBreaks(boolean value) { this.autoStartBreaks = value; }
    public void setAutoStartPomo(boolean value) { this.autoStartPomodoros = value; }

    public boolean isAutoStartBreaks() { return autoStartBreaks; }
    public boolean isAutoStartPomo() { return autoStartPomodoros; }

    public boolean isRunning() {
        return currentState != State.WAITING && currentState != State.MENU;
    }

    public State getLogicalState() {
        if (currentState == State.WAITING) {
            return lastActiveState;
        }
        return currentState;
    }

    public int getRealMinutesElapsed() {
        return secondsElapsed / 60;
    }

    public String getFormattedTime() {
        return String.format("%02d:%02d", secondsRemaining / 60, secondsRemaining % 60);
    }

    public void fullReset() {
        this.secondsElapsed = 0;
        this.sessionCounter = 0;
    }

    public State getCurrentState() { return currentState; }
    public State getLastActiveState() { return lastActiveState; }
    public int getSessionCounter() {return sessionCounter;}
    public void setOnTick(Runnable r) { this.onTick = r; }
    public void setOnStateChange(Runnable r) { this.onStateChange = r; }

    public int getWorkMins() { return workMins; }
    public int getShortMins() { return shortMins; }
    public int getLongMins() { return longMins; }
    public int getInterval() { return interval; }
}