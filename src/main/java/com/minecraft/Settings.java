package com.minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Settings {
    private int renderDistance = 8;
    private float nearClip = 0.1f;
    private float farClip = 1000.0f;
    private int enableCulling = 0; // 0 = disabled by default

    public static Settings load(String path) {
        Settings s = new Settings();
        try {
            String content = Files.readString(Path.of(path));
            // integer pattern
            Pattern intPat = Pattern.compile("\"renderDistance\"\s*:\s*(\\d+)");
            Matcher m = intPat.matcher(content);
            if (m.find()) s.renderDistance = Integer.parseInt(m.group(1));

            Pattern nearPat = Pattern.compile("\"nearClip\"\s*:\s*([-+]?[0-9]*\\.?[0-9]+)");
            m = nearPat.matcher(content);
            if (m.find()) s.nearClip = Float.parseFloat(m.group(1));

            Pattern farPat = Pattern.compile("\"farClip\"\s*:\s*([-+]?[0-9]*\\.?[0-9]+)");
            m = farPat.matcher(content);
            if (m.find()) s.farClip = Float.parseFloat(m.group(1));

            Pattern cullPat = Pattern.compile("\"enableCulling\"\s*:\s*(\\d+)");
            m = cullPat.matcher(content);
            if (m.find()) s.enableCulling = Integer.parseInt(m.group(1));
        } catch (IOException e) {
        }
        return s;
    }

    public int getRenderDistance() {
        return renderDistance;
    }

    public float getNearClip() {
        return nearClip;
    }

    public float getFarClip() {
        return farClip;
    }

    public int getEnableCulling() {
        return enableCulling;
    }
}
