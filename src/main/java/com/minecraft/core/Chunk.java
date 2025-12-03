package com.minecraft.core;

public class Chunk {
    public static final int CHUNK_WIDTH = 16;
    public static final int CHUNK_HEIGHT = 256;
    public static final int CHUNK_DEPTH = 16;

    private final Block[][][] blocks;

    public Chunk() {
        blocks = new Block[CHUNK_WIDTH][CHUNK_HEIGHT][CHUNK_DEPTH];
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
    }
}