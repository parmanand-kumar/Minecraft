package com.minecraft.graphics;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.nio.DoubleBuffer;

public class Camera {
    private float x, y, z;
    private float yaw, pitch;
    private static final float MOUSE_SENSITIVITY = 0.1f;

    private long window;
    private boolean cursorLocked = true;
    private double lastMouseX, lastMouseY;

    public Camera(long window) {
        this.window = window;
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        GLFW.glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                cursorLocked = false;
                GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
            }
        });
        GLFW.glfwSetMouseButtonCallback(window, (win, button, action, mods) -> {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
                cursorLocked = true;
                GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
            }
        });

        DoubleBuffer xpos = BufferUtils.createDoubleBuffer(1);
        DoubleBuffer ypos = BufferUtils.createDoubleBuffer(1);
        GLFW.glfwGetCursorPos(window, xpos, ypos);
        lastMouseX = xpos.get(0);
        lastMouseY = ypos.get(0);
    }

    public void update() {
        handleMouseInput();
        handleKeyboardInput();

        GL11.glRotatef(pitch, 1, 0, 0);
        GL11.glRotatef(yaw, 0, 1, 0);
        GL11.glTranslatef(-x, -y, -z);
    }

    private void handleMouseInput() {
        DoubleBuffer xpos = BufferUtils.createDoubleBuffer(1);
        DoubleBuffer ypos = BufferUtils.createDoubleBuffer(1);
        GLFW.glfwGetCursorPos(window, xpos, ypos);

        float dx = (float) (xpos.get(0) - lastMouseX) * MOUSE_SENSITIVITY;
        float dy = (float) (ypos.get(0) - lastMouseY) * MOUSE_SENSITIVITY;

        lastMouseX = xpos.get(0);
        lastMouseY = ypos.get(0);

        yaw += dx;
        pitch += dy;
        
        if (pitch > 90.0f) {
            pitch = 90.0f;
        } else if (pitch < -90.0f) {
            pitch = -90.0f;
        }
    }

    private void handleKeyboardInput() {
        if (cursorLocked) {
            float[] newPos = Keybinds.handleKeyboardInput(window, x, y, z, yaw);
            x = newPos[0];
            y = newPos[1];
            z = newPos[2];
        }
    }
}