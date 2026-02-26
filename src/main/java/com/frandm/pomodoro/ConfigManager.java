package com.frandm.pomodoro;

import java.io.*;
import java.util.Properties;

public class ConfigManager {

    // 1. Definimos el nombre de la carpeta y el archivo
    private static final String FOLDER_NAME = ".pomodoro_app";
    private static final String FILE_NAME = "settings.properties";

    /**
     * Obtiene la ruta absoluta del archivo de configuración en la carpeta de usuario.
     * Ejemplo: C:\Users\TuUsuario\.pomodoro_app\settings.properties
     */
    private static File getConfigFile() {
        String userHome = System.getProperty("user.home");
        File configDir = new File(userHome, FOLDER_NAME);

        // Si la carpeta no existe, la creamos
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        return new File(configDir, FILE_NAME);
    }


    public static void save(PomodoroEngine engine) {
        Properties props = new Properties();
        props.setProperty("workMins", String.valueOf(engine.getWorkMins()));
        props.setProperty("shortMins", String.valueOf(engine.getShortMins()));
        props.setProperty("longMins", String.valueOf(engine.getLongMins()));
        props.setProperty("interval", String.valueOf(engine.getInterval()));
        props.setProperty("autoBreak", String.valueOf(engine.isAutoStartBreaks()));
        props.setProperty("autoPomo", String.valueOf(engine.isAutoStartPomo()));
        props.setProperty("alarmSoundVolume", String.valueOf(engine.getAlarmSoundVolume()));

        File configFile = getConfigFile();

        try (OutputStream out = new FileOutputStream(configFile)) {
            props.store(out, "Pomodoro App User Settings");
        } catch (IOException e) {
            System.err.println("Error saving config file: " + e.getMessage());
        }
    }

    public static void load(PomodoroEngine engine) {
        File configFile = getConfigFile();

        if (!configFile.exists()) {
            System.out.println("Cant find config file. Using default configuration.");
            return;
        }

        Properties props = new Properties();
        try (InputStream in = new FileInputStream(configFile)) {
            props.load(in);

            engine.updateSettings(
                    Integer.parseInt(props.getProperty("workMins", String.valueOf(engine.getWorkMins()))),
                    Integer.parseInt(props.getProperty("shortMins", String.valueOf(engine.getShortMins()))),
                    Integer.parseInt(props.getProperty("longMins", String.valueOf(engine.getLongMins()))),
                    Integer.parseInt(props.getProperty("interval", String.valueOf(engine.getInterval()))),
                    Boolean.parseBoolean(props.getProperty("autoBreak", String.valueOf(engine.isAutoStartBreaks()))),
                    Boolean.parseBoolean(props.getProperty("autoPomo", String.valueOf(engine.isAutoStartPomo()))),
                    Integer.parseInt(props.getProperty("alarmSoundVolume", String.valueOf(engine.getAlarmSoundVolume())))
            );
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading config file: " + e.getMessage());
        }
    }
}