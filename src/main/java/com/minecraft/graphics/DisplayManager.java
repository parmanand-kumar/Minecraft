package com.minecraft.graphics;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

public class DisplayManager {
    private static long window;
    private static int width;
    private static int height;

    public static void createDisplay() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);

        GLFWVidMode vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        width = vidMode.width();
        height = vidMode.height();

        window = GLFW.glfwCreateWindow(width, height, "Minecraft", 0, 0);
        if (window == 0) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        GLFW.glfwSetFramebufferSizeCallback(window, (window, w, h) -> {
            width = w;
            height = h;
            GL11.glViewport(0, 0, w, h);
        });

        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();
    }

    public static void handleCursorState(boolean locked) {
        if (locked) {
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        } else {
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
        }
    }

    public static void updateDisplay() {
        GLFW.glfwPollEvents();
        GL11.glClearColor(0.5f, 0.8f, 1.0f, 1.0f);
        GLFW.glfwSwapBuffers(window);
    }

    public static void closeDisplay() {
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public static boolean isCloseRequested() {
        return GLFW.glfwWindowShouldClose(window);
    }

    public static long getWindow() {
        return window;
    }

    public static int getWidth() {
        return width;
    }

    public static int getHeight() {
        return height;
    }
}