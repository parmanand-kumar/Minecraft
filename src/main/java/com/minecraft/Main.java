package com.minecraft;

import com.minecraft.Generation.Terrain;
import com.minecraft.graphics.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
 
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
 
        AtomicReference<ShaderProgram> shaderProgramRef = new AtomicReference<>(null);
        try {
            ShaderProgram shaderProgram = new ShaderProgram("src/main/resources/shaders/terrain.vert",
                    "src/main/resources/shaders/terrain.frag");
            shaderProgram.start(); // Start the shader program to create uniforms
            shaderProgram.createUniform("projectionMatrix");
            shaderProgram.createUniform("viewMatrix");
            shaderProgram.createUniform("modelMatrix");
            shaderProgram.createUniform("texture_sampler");
            shaderProgram.stop(); // Stop the shader program after creating uniforms
            shaderProgramRef.set(shaderProgram);
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

        GLFW.glfwSetFramebufferSizeCallback(DisplayManager.getWindow(), (window, width, height) -> {
            glViewport(0, 0, width, height);
            ShaderProgram shaderProgram = shaderProgramRef.get();
            if (shaderProgram != null) {
                shaderProgram.start();
                shaderProgram.setUniform("projectionMatrix",
                        transformation.getProjectionMatrix((float) Math.toRadians(70.0f),
                                width, height, NEAR_PLANE, FAR_PLANE));
                shaderProgram.stop();
            }
        });
  
        glEnable(GL_DEPTH_TEST);
 
        DisplayManager.handleCursorState(cursorLocked);
 
        Terrain terrain = new Terrain();
        terrain.generateTerrain();
        Map<String, Mesh> terrainMeshes = terrain.generateMeshes();
        int spawnX = 64;
        int spawnZ = 64;
        int spawnY = terrain.getHeight(spawnX, spawnZ) + 2; // Spawn 2 blocks above the ground
        camera.setPosition(spawnX, spawnY, spawnZ);

        while (!DisplayManager.isCloseRequested()) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            ShaderProgram shaderProgram = shaderProgramRef.get();
            shaderProgram.start();
 
            shaderProgram.setUniform("projectionMatrix",
                    transformation.getProjectionMatrix((float) Math.toRadians(70.0f),
                            (float) DisplayManager.getWidth(), (float) DisplayManager.getHeight(), NEAR_PLANE, FAR_PLANE));
            shaderProgram.setUniform("viewMatrix", transformation.getViewMatrix(camera));
            shaderProgram.setUniform("modelMatrix", new org.joml.Matrix4f().identity()); // Placeholder, will be updated
            shaderProgram.setUniform("texture_sampler", 0);
            
            for (Mesh mesh : terrainMeshes.values()) {
                mesh.render();
            }
            
            camera.update(cursorLocked);
            
            shaderProgram.stop();
 
            DisplayManager.updateDisplay();
        }
        if (shaderProgramRef.get() != null) {
            shaderProgramRef.get().cleanUp();
        }
        DisplayManager.closeDisplay();
    }

}
