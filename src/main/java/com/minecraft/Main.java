package com.minecraft;

import com.minecraft.Generation.Terrain;
import com.minecraft.Settings;
import com.minecraft.core.Chunk;
import com.minecraft.graphics.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
 
import static org.lwjgl.opengl.GL11.*;
 
public class Main {
    private static boolean cursorLocked = true;
 
    public static void main(String[] args) {
        DisplayManager.createDisplay();

        // Load settings (path relative to project root). If missing, defaults are used.
        Settings settings = Settings.load("src/main/java/com/minecraft/settings.json");
        final float NEAR_PLANE = settings.getNearClip();
        final float FAR_PLANE = settings.getFarClip();
        final int RENDER_DISTANCE = settings.getRenderDistance();
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
 
    Terrain terrain = new Terrain(System.currentTimeMillis(), RENDER_DISTANCE);
    terrain.setEnableCulling(settings.getEnableCulling());
        int spawnX = 0;
        int spawnZ = 0;
        int spawnY = terrain.getHeight(spawnX, spawnZ) + 2;
        camera.setPosition(spawnX, spawnY, spawnZ);

        while (!DisplayManager.isCloseRequested()) {
            int playerChunkX = (int) Math.floor(camera.getPosition().x / Chunk.CHUNK_WIDTH);
            int playerChunkZ = (int) Math.floor(camera.getPosition().z / Chunk.CHUNK_DEPTH);
            terrain.update(playerChunkX, playerChunkZ);

            Map<String, Mesh> terrainMeshes = terrain.generateMeshes();

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            ShaderProgram shaderProgram = shaderProgramRef.get();
            shaderProgram.start();

            shaderProgram.setUniform("projectionMatrix",
                    transformation.getProjectionMatrix((float) Math.toRadians(70.0f),
                            (float) DisplayManager.getWidth(), (float) DisplayManager.getHeight(), NEAR_PLANE, FAR_PLANE));
            shaderProgram.setUniform("viewMatrix", transformation.getViewMatrix(camera));
            shaderProgram.setUniform("modelMatrix", new org.joml.Matrix4f().identity());
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
        terrain.cleanup();
        DisplayManager.closeDisplay();
    }

}
