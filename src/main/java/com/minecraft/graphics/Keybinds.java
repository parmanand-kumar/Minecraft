package com.minecraft.graphics;

import org.lwjgl.glfw.GLFW;

public class Keybinds {
    private static final float MOVEMENT_SPEED = 0.1f;

    public static boolean isKeyDown(long window, int key) {
        return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }

    public static float[] handleKeyboardInput(long window, float x, float y, float z, float yaw) {
        float dx = 0, dz = 0, dy = 0;

        if (isKeyDown(window, GLFW.GLFW_KEY_W)) {
            dz = -MOVEMENT_SPEED;
        }
        if (isKeyDown(window, GLFW.GLFW_KEY_S)) {
            dz = MOVEMENT_SPEED;
        }
        if (isKeyDown(window, GLFW.GLFW_KEY_A)) {
            dx = -MOVEMENT_SPEED;
        }
        if (isKeyDown(window, GLFW.GLFW_KEY_D)) {
            dx = MOVEMENT_SPEED;
        }
        if (isKeyDown(window, GLFW.GLFW_KEY_SPACE)) {
            dy = MOVEMENT_SPEED;
        }
        if (isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)) {
            dy = -MOVEMENT_SPEED;
        }

        float yawRad = (float) Math.toRadians(yaw);
        x += dx * Math.cos(yawRad) - dz * Math.sin(yawRad);
        z += dx * Math.sin(yawRad) + dz * Math.cos(yawRad);
        y += dy;

        return new float[]{x, y, z};
    }
}
