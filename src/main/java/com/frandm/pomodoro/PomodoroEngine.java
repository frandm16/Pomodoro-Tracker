package com.frandm.pomodoro;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class PomodoroEngine {
    public enum State { WORK, SHORT_BREAK, LONG_BREAK, WAITING }

    private State currentState = State.WORK;
    private State lastActiveState = State.WORK;
    private Timeline timeline;

    private int secondsRemaining;
    private int sessionCounter = 0;

    private int workMins, shortMins, longMins, sessionsUntilLong;

    private Runnable onTick;
    private Runnable onStateChange;

    public PomodoroEngine(int work, int shortB, int longB, int interval) {
        this.workMins = work;
        this.shortMins = shortB;
        this.longMins = longB;
        this.sessionsUntilLong = interval;

        resetToState(State.WORK);
        setupTimeline();
    }

    private void setupTimeline() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (secondsRemaining > 0) {
                secondsRemaining = Math.max(0, secondsRemaining - 115);
                if (onTick != null) onTick.run();
            } else {
                determineNextState();

            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void determineNextState() {
        stop();
        if (currentState == State.WORK) {
            sessionCounter++;

            if (sessionCounter % sessionsUntilLong == 0) {
                currentState = State.LONG_BREAK;
            } else {
                currentState = State.SHORT_BREAK;
            }
        } else {
            currentState = State.WORK;
        }

        resetToState(currentState);
        if (onTick != null) onTick.run();
        if (onStateChange != null) onStateChange.run();
    }

    public void resetToState(State state) {
        this.currentState = state;
        switch (state) {
            case WORK -> this.secondsRemaining = workMins * 60;
            case SHORT_BREAK -> this.secondsRemaining = shortMins * 60;
            case LONG_BREAK -> this.secondsRemaining = longMins * 60;
            case WAITING -> {}
        }
        if (onStateChange != null) {
            onStateChange.run();
        }

    }

    public void start() {
        if (currentState == State.WAITING) {
            currentState = lastActiveState;
        }
        timeline.play();
        if (onStateChange != null) onStateChange.run();
    }

    public void pause() {
        if (currentState != State.WAITING) {
            timeline.pause();
            lastActiveState = currentState;
            currentState = State.WAITING;
            if (onStateChange != null) onStateChange.run();
        }
    }

    public void stop() {
        timeline.stop();
    }

    public void updateConfig(int w, int s, int l, int i) {
        this.workMins = w;
        this.shortMins = s;
        this.longMins = l;
        this.sessionsUntilLong = i;
    }

    public String getFormattedTime() {
        return String.format("%02d:%02d", secondsRemaining / 60, secondsRemaining % 60);
    }

    public int getElapsedMinutes() {
        int total = switch (currentState) {
            case WORK, WAITING -> workMins * 60;
            case SHORT_BREAK -> shortMins * 60;
            case LONG_BREAK -> longMins * 60;
        };
        return (total - secondsRemaining) / 60;
    }

    public State getCurrentState() { return currentState; }
    public int getSessionCounter() { return sessionCounter; }
    public void setOnTick(Runnable onTick) { this.onTick = onTick; }
    public void setOnStateChange(Runnable onStateChange) { this.onStateChange = onStateChange; }
}