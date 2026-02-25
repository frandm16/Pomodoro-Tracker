package com.frandm.pomodoro;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class PomodoroEngine {
    private Timeline timeline;
    private final int initialSeconds; // initial timer
    private int secondsRemaining;
    private Runnable onTick;

    public PomodoroEngine(int minutes) {
        this.initialSeconds = minutes * 60;
        this.secondsRemaining = initialSeconds;
        setupTimeline();
    }

    private void setupTimeline() {
        // updates every 1 sec
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (secondsRemaining > 0) {
                secondsRemaining--;
                if (onTick != null) onTick.run();
            } else {
                stop();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    public void start() { timeline.play(); } // called from HandleStart() from PomodoroController
    public void pause() { timeline.pause(); } // called from HandlePause() from PomodoroController
    public void stop() { timeline.stop(); } // called from HandleStop() from PomodoroController

    public void reset() {
        stop();
        this.secondsRemaining = initialSeconds;
    } // called from HandleReset() from PomodoroController

    public int getElapsedMinutes() {
        int elapsedSeconds = initialSeconds - secondsRemaining;
        return elapsedSeconds / 60;
    }

    public String getFormattedTime() {
        return String.format("%02d:%02d", secondsRemaining / 60, secondsRemaining % 60);
    }

    public void setOnTick(Runnable onTick) { this.onTick = onTick; }
}