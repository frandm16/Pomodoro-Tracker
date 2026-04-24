package com.frandm.studytracker.ui.views.planner;

import com.frandm.studytracker.client.ApiClient;
import com.frandm.studytracker.core.NotificationManager;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlannerHelpers {


    public static TagSelectionData loadTagData() {
        Map<String, List<String>> tagMap = new LinkedHashMap<>();
        Map<String, String> tagColors = new LinkedHashMap<>();

        try {
            ApiClient.getTags().forEach(tag -> {
                String tagName = String.valueOf(tag.get("name"));
                tagColors.put(tagName, String.valueOf(tag.getOrDefault("color", "")));
                try {
                    List<String> taskNames = ApiClient.getTasks(tagName)
                            .stream()
                            .map(task -> String.valueOf(task.get("name")))
                            .collect(Collectors.toList());
                    tagMap.put(tagName, taskNames);
                } catch (Exception ex) {
                    tagMap.put(tagName, new ArrayList<>());
                }
            });
        } catch (Exception ignored) {}

        return new TagSelectionData(tagMap, tagColors);
    }

    public static void preselectTask(Map<String, List<String>> tagMap, ComboBox<String> tags, ComboBox<String> tasks, String taskName) {
        tagMap.entrySet().stream()
                .filter(entry -> entry.getValue().contains(taskName))
                .findFirst()
                .ifPresent(entry -> {
                    tags.getSelectionModel().select(entry.getKey());
                    tasks.getItems().setAll(entry.getValue());
                    tasks.getSelectionModel().select(taskName);
                });

        if (tags.getValue() == null && !tags.getItems().isEmpty()) {
            tags.getSelectionModel().selectFirst();
            tasks.getItems().setAll(tagMap.getOrDefault(tags.getValue(), List.of()));
            if (!tasks.getItems().isEmpty()) {
                tasks.getSelectionModel().selectFirst();
            }
        }
    }

    public static TextField createTimeField(String initial, int max) {
        TextField field = new TextField(initial);
        field.setPrefWidth(65);
        field.setAlignment(Pos.CENTER);
        field.textProperty().addListener((_, _, newValue) -> {
            if (!newValue.matches("\\d*")) {
                field.setText(newValue.replaceAll("\\D", ""));
            }
            if (field.getText().length() > 2) {
                field.setText(field.getText().substring(0, 2));
            }
            if (!field.getText().isEmpty()) {
                int value = Integer.parseInt(field.getText());
                if (value > max) {
                    field.setText(String.valueOf(max));
                }
            }
        });
        return field;
    }

    public static void toggleTimeFields(TextField hourField, TextField minuteField, boolean disabled) {
        hourField.setDisable(disabled);
        minuteField.setDisable(disabled);
    }

    public static int parseInt(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return Integer.parseInt(value);
    }

    public static boolean requireDate(DatePicker datePicker, String label) {
        if (datePicker.getValue() != null) {
            return true;
        }
        NotificationManager.show("Missing data", label + " date is required", NotificationManager.NotificationType.ERROR);
        return false;
    }

    public static boolean requireSelection(ComboBox<String> comboBox, String label) {
        if (comboBox.getValue() != null && !comboBox.getValue().isBlank()) {
            return true;
        }
        NotificationManager.show("Missing data", label + " must be selected", NotificationManager.NotificationType.ERROR);
        return false;
    }

    public static boolean requireTime(TextField hourField, TextField minuteField, String label) {
        if (!hourField.getText().isBlank() && !minuteField.getText().isBlank()) {
            return true;
        }
        NotificationManager.show("Missing data", label + " time is required", NotificationManager.NotificationType.ERROR);
        return false;
    }

    public static boolean requireValue(String value, String label) {
        if (value != null && !value.isBlank()) {
            return true;
        }
        NotificationManager.show("Missing data", label + " must be selected", NotificationManager.NotificationType.ERROR);
        return false;
    }

    public static boolean requireChronologicalRange(LocalDateTime start, LocalDateTime end) {
        if (start.isBefore(end)) {
            return true;
        }
        NotificationManager.show("Invalid time range", "Start time must be earlier than end time", NotificationManager.NotificationType.ERROR);
        return false;
    }

    public record TagSelectionData(Map<String, List<String>> tagMap, Map<String, String> tagColors) {}
}
