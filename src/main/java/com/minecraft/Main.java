package com.minecraft;

import com.minecraft.graphics.Camera;
import com.minecraft.graphics.DisplayManager;
import com.minecraft.graphics.ShaderProgram;
import com.minecraft.graphics.Transformation;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
 
import static org.lwjgl.opengl.GL11.*;
 
public class Main {
    private static boolean cursorLocked = true;
    private static final int RENDER_DISTANCE = 3; // Render distance in chunks
    private static final float NEAR_PLANE = 0.1f; // Near clipping plane
    private static final float FAR_PLANE = 1000.0f; // Far clipping plane, changed to float
 
    public static void main(String[] args) {
        DisplayManager.createDisplay();
        GL.createCapabilities();
 
        Camera camera = new Camera(DisplayManager.getWindow());
        Transformation transformation = new Transformation();
 
        ShaderProgram shaderProgram = null;
        try {
            shaderProgram = new ShaderProgram("src/main/resources/shaders/basic.vert",
                    "src/main/resources/shaders/basic.frag");
            shaderProgram.start(); // Start the shader program to create uniforms
            shaderProgram.createUniform("projectionMatrix");
            shaderProgram.createUniform("viewMatrix");
            shaderProgram.createUniform("modelMatrix");
            shaderProgram.stop(); // Stop the shader program after creating uniforms
        } catch (Exception e) {
            e.printStackTrace();
        }
 
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
 
        glEnable(GL_DEPTH_TEST);
 
        DisplayManager.handleCursorState(cursorLocked);
 
        while (!DisplayManager.isCloseRequested()) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
 
            shaderProgram.start();
 
            shaderProgram.setUniform("projectionMatrix",
                    transformation.getProjectionMatrix((float) Math.toRadians(70.0f),
                            DisplayManager.getWidth(), DisplayManager.getHeight(), NEAR_PLANE, FAR_PLANE));
            shaderProgram.setUniform("viewMatrix", transformation.getViewMatrix(camera));
            shaderProgram.setUniform("modelMatrix", new org.joml.Matrix4f().identity()); // Placeholder, will be updated
 
            camera.update(cursorLocked);
 
            drawCube();
 
            shaderProgram.stop();
 
            DisplayManager.updateDisplay();
        }
        if (shaderProgram != null) {
            shaderProgram.cleanUp();
        }
        DisplayManager.closeDisplay();
    }

    private static void drawCube() {
        glBegin(GL_QUADS);
        glColor3f(1.0f, 0.0f, 0.0f);
 
        // Front face
        glVertex3f(-0.5f, -0.5f, 0.5f);
        glVertex3f(0.5f, -0.5f, 0.5f);
        glVertex3f(0.5f, 0.5f, 0.5f);
        glVertex3f(-0.5f, 0.5f, 0.5f);
 
        // Back face
        glVertex3f(-0.5f, -0.5f, -0.5f);
        glVertex3f(-0.5f, 0.5f, -0.5f);
        glVertex3f(0.5f, 0.5f, -0.5f);
        glVertex3f(0.5f, -0.5f, -0.5f);
 
        // Top face
        glVertex3f(-0.5f, 0.5f, -0.5f);
        glVertex3f(-0.5f, 0.5f, 0.5f);
        glVertex3f(0.5f, 0.5f, 0.5f);
        glVertex3f(0.5f, 0.5f, -0.5f);
 
        // Bottom face
        glVertex3f(-0.5f, -0.5f, -0.5f);
        glVertex3f(0.5f, -0.5f, -0.5f);
        glVertex3f(0.5f, -0.5f, 0.5f);
        glVertex3f(-0.5f, -0.5f, 0.5f);
 
        // Right face
        glVertex3f(0.5f, -0.5f, -0.5f);
        glVertex3f(0.5f, 0.5f, -0.5f);
        glVertex3f(0.5f, 0.5f, 0.5f);
        glVertex3f(0.5f, -0.5f, 0.5f);
 
        // Left face
        glVertex3f(-0.5f, -0.5f, -0.5f);
        glVertex3f(-0.5f, -0.5f, 0.5f);
        glVertex3f(-0.5f, 0.5f, 0.5f);
        glVertex3f(-0.5f, 0.5f, -0.5f);
 
        glEnd();
    }
}
