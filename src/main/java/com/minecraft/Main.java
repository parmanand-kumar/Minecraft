package com.minecraft;

import com.minecraft.graphics.Camera;
import com.minecraft.graphics.DisplayManager;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;

public class Main {
    public static void main(String[] args) {
        DisplayManager.createDisplay();
        Camera camera = new Camera(DisplayManager.getWindow());

        
        glEnable(GL_DEPTH_TEST);

        
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        float aspectRatio = 1280.0f / 720.0f;
        glFrustum(-aspectRatio, aspectRatio, -1, 1, 1.0, 100.0);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        while (!DisplayManager.isCloseRequested()) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            glLoadIdentity();
            camera.update();

            
            drawCube();

            DisplayManager.updateDisplay();
        }
        DisplayManager.closeDisplay();
    }

    private static void drawCube() {
        glBegin(GL_QUADS);
        glColor3f(1.0f, 0.0f, 0.0f);

        
        glVertex3f(-1.0f, -1.0f, 1.0f);
        glVertex3f(1.0f, -1.0f, 1.0f);
        glVertex3f(1.0f, 1.0f, 1.0f);
        glVertex3f(-1.0f, 1.0f, 1.0f);

        
        glVertex3f(-1.0f, -1.0f, -1.0f);
        glVertex3f(-1.0f, 1.0f, -1.0f);
        glVertex3f(1.0f, 1.0f, -1.0f);
        glVertex3f(1.0f, -1.0f, -1.0f);

        
        glVertex3f(-1.0f, 1.0f, -1.0f);
        glVertex3f(-1.0f, 1.0f, 1.0f);
        glVertex3f(1.0f, 1.0f, 1.0f);
        glVertex3f(1.0f, 1.0f, -1.0f);

        
        glVertex3f(-1.0f, -1.0f, -1.0f);
        glVertex3f(1.0f, -1.0f, -1.0f);
        glVertex3f(1.0f, -1.0f, 1.0f);
        glVertex3f(-1.0f, -1.0f, 1.0f);

        
        glVertex3f(1.0f, -1.0f, -1.0f);
        glVertex3f(1.0f, 1.0f, -1.0f);
        glVertex3f(1.0f, 1.0f, 1.0f);
        glVertex3f(1.0f, -1.0f, 1.0f);

        
        glVertex3f(-1.0f, -1.0f, -1.0f);
        glVertex3f(-1.0f, -1.0f, 1.0f);
        glVertex3f(-1.0f, 1.0f, 1.0f);
        glVertex3f(-1.0f, 1.0f, -1.0f);

        glEnd();
    }
}