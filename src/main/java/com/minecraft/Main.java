package com.minecraft;

import com.minecraft.graphics.Camera;
import com.minecraft.graphics.DisplayManager;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;

public class Main {
    private static boolean cursorLocked = true;

    public static void main(String[] args) {
        DisplayManager.createDisplay();
        Camera camera = new Camera(DisplayManager.getWindow());

        GLFW.glfwSetKeyCallback(DisplayManager.getWindow(), (window, key, scancode, action, mods) -> {
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
                cursorLocked = !cursorLocked;
                DisplayManager.handleCursorState(cursorLocked);
            }
        });

        GLFW.glfwSetMouseButtonCallback(DisplayManager.getWindow(), (window, button, action, mods) -> {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
                cursorLocked = true;
                DisplayManager.handleCursorState(cursorLocked);
            }
        });

        GLFW.glfwSetFramebufferSizeCallback(DisplayManager.getWindow(), (window, width, height) -> {
            glViewport(0, 0, width, height);
            glMatrixMode(GL_PROJECTION);
            glLoadIdentity();
            float aspectRatio = (float) width / (float) height;
            glFrustum(-aspectRatio, aspectRatio, -1, 1, 1.0, 100.0);
            glMatrixMode(GL_MODELVIEW);
            glLoadIdentity();
        });

        glEnable(GL_DEPTH_TEST);

        DisplayManager.handleCursorState(cursorLocked);

        while (!DisplayManager.isCloseRequested()) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            glLoadIdentity();
            camera.update(cursorLocked);

            drawCube();

            DisplayManager.updateDisplay();
        }
        DisplayManager.closeDisplay();
    }

    private static void drawCube() {
        glBegin(GL_QUADS);
        glColor3f(1.0f, 0.0f, 0.0f);

        // Front face
        glVertex3f(-1.0f, -1.0f, 1.0f);
        glVertex3f(1.0f, -1.0f, 1.0f);
        glVertex3f(1.0f, 1.0f, 1.0f);
        glVertex3f(-1.0f, 1.0f, 1.0f);

        // Back face
        glVertex3f(-1.0f, -1.0f, -1.0f);
        glVertex3f(-1.0f, 1.0f, -1.0f);
        glVertex3f(1.0f, 1.0f, -1.0f);
        glVertex3f(1.0f, -1.0f, -1.0f);

        // Top face
        glVertex3f(-1.0f, 1.0f, -1.0f);
        glVertex3f(-1.0f, 1.0f, 1.0f);
        glVertex3f(1.0f, 1.0f, 1.0f);
        glVertex3f(1.0f, 1.0f, -1.0f);

        // Bottom face
        glVertex3f(-1.0f, -1.0f, -1.0f);
        glVertex3f(1.0f, -1.0f, -1.0f);
        glVertex3f(1.0f, -1.0f, 1.0f);
        glVertex3f(-1.0f, -1.0f, 1.0f);

        // Right face
        glVertex3f(1.0f, -1.0f, -1.0f);
        glVertex3f(1.0f, 1.0f, -1.0f);
        glVertex3f(1.0f, 1.0f, 1.0f);
        glVertex3f(1.0f, -1.0f, 1.0f);

        // Left face
        glVertex3f(-1.0f, -1.0f, -1.0f);
        glVertex3f(-1.0f, -1.0f, 1.0f);
        glVertex3f(-1.0f, 1.0f, 1.0f);
        glVertex3f(-1.0f, 1.0f, -1.0f);

        glEnd();
    }
}


// test 2