package com.minecraft.core;

import com.minecraft.graphics.Mesh;
import java.util.Map;
import java.util.HashMap;

public class Chunk {
    public static final int CHUNK_WIDTH = 16;
    public static final int CHUNK_HEIGHT = 256;
    public static final int CHUNK_DEPTH = 16;

    private final Block[][][] blocks;
    private Map<String, Mesh> meshes;
    private boolean needsRebuild = true;
    // Pending mesh data produced by background threads (not yet uploaded to GPU)
    private Map<String, float[]> pendingPositions = new HashMap<>();
    private Map<String, float[]> pendingTextCoords = new HashMap<>();
    private Map<String, int[]> pendingIndices = new HashMap<>();

    public Chunk() {
        blocks = new Block[CHUNK_WIDTH][CHUNK_HEIGHT][CHUNK_DEPTH];
        meshes = new HashMap<>();
    }

    public Block getBlock(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_WIDTH || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_DEPTH) {
            return null;
        }
        return blocks[x][y][z];
    }

    public void setBlock(int x, int y, int z, Block block) {
        if (x < 0 || x >= CHUNK_WIDTH || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_DEPTH) {
            return;
        }
        blocks[x][y][z] = block;
        needsRebuild = true;
    }

    public Map<String, Mesh> getMeshes() {
        return meshes;
    }

    public void setMeshes(Map<String, Mesh> meshes) {
        this.meshes = meshes;
        needsRebuild = false;
    }

    public synchronized void setPendingMeshData(Map<String, float[]> positions, Map<String, float[]> textCoords, Map<String, int[]> indices) {
        this.pendingPositions = positions;
        this.pendingTextCoords = textCoords;
        this.pendingIndices = indices;
        // mark as not needing rebuild; the GPU upload will happen on the main thread
        this.needsRebuild = false;
    }

    public synchronized boolean hasPendingMeshData() {
        return pendingPositions != null && !pendingPositions.isEmpty();
    }

    public synchronized Map<String, float[]> getPendingPositions() {
        return pendingPositions;
    }

    public synchronized Map<String, float[]> getPendingTextCoords() {
        return pendingTextCoords;
    }

    public synchronized Map<String, int[]> getPendingIndices() {
        return pendingIndices;
    }

    public synchronized void clearPendingMeshData() {
        pendingPositions.clear();
        pendingTextCoords.clear();
        pendingIndices.clear();
    }

    public boolean needsRebuild() {
        return needsRebuild;
    }
}