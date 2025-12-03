package com.minecraft.Generation;

import com.minecraft.core.Block;
import com.minecraft.core.Chunk;
import com.minecraft.graphics.Mesh;

import java.util.ArrayList;
import java.util.List;

public class Terrain {

    private static final int TERRAIN_WIDTH = 1;
    private static final int TERRAIN_DEPTH = 1;

    private final Chunk[][] chunks;

    public Terrain() {
        chunks = new Chunk[TERRAIN_WIDTH][TERRAIN_DEPTH];
        for (int x = 0; x < TERRAIN_WIDTH; x++) {
            for (int z = 0; z < TERRAIN_DEPTH; z++) {
                chunks[x][z] = new Chunk();
            }
        }
    }

    public void generateTerrain() {
        for (int x = 0; x < TERRAIN_WIDTH; x++) {
            for (int z = 0; z < TERRAIN_DEPTH; z++) {
                generateChunk(chunks[x][z]);
            }
        }
    }

    public Mesh generateMesh() {
        List<Float> positions = new ArrayList<>();
        List<Float> textCoords = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        for (int x = 0; x < TERRAIN_WIDTH; x++) {
            for (int z = 0; z < TERRAIN_DEPTH; z++) {
                generateChunkMesh(x, z, positions, textCoords, indices);
            }
        }

        float[] posArr = new float[positions.size()];
        for (int i = 0; i < positions.size(); i++) {
            posArr[i] = positions.get(i);
        }

        float[] textCoordsArr = new float[textCoords.size()];
        for (int i = 0; i < textCoords.size(); i++) {
            textCoordsArr[i] = textCoords.get(i);
        }

        int[] indicesArr = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            indicesArr[i] = indices.get(i);
        }

        return new Mesh(posArr, textCoordsArr, indicesArr);
    }

    private void generateChunk(Chunk chunk) {
        for (int x = 0; x < Chunk.CHUNK_WIDTH; x++) {
            for (int z = 0; z < Chunk.CHUNK_DEPTH; z++) {
                for (int y = 0; y < 1; y++) {
                    chunk.setBlock(x, y, z, new Block(1));
                }
            }
        }
    }

    private void generateChunkMesh(int chunkX, int chunkZ, List<Float> positions, List<Float> textCoords, List<Integer> indices) {
        Chunk chunk = chunks[chunkX][chunkZ];
        for (int x = 0; x < Chunk.CHUNK_WIDTH; x++) {
            for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                for (int z = 0; z < Chunk.CHUNK_DEPTH; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (block != null) {
                        generateBlockMesh(chunk, x, y, z, positions, textCoords, indices);
                    }
                }
            }
        }
    }

    private void generateBlockMesh(Chunk chunk, int x, int y, int z, List<Float> positions, List<Float> textCoords, List<Integer> indices) {
        // Front face
        if (chunk.getBlock(x, y, z + 1) == null) {
            positions.add((float) x);
            positions.add((float) y);
            positions.add((float) z + 1);
            textCoords.add(0.0f);
            textCoords.add(0.0f);
            indices.add(positions.size() / 3 - 1);

            positions.add((float) x + 1);
            positions.add((float) y);
            positions.add((float) z + 1);
            textCoords.add(1.0f);
            textCoords.add(0.0f);
            indices.add(positions.size() / 3 - 1);

            positions.add((float) x + 1);
            positions.add((float) y + 1);
            positions.add((float) z + 1);
            textCoords.add(1.0f);
            textCoords.add(1.0f);
            indices.add(positions.size() / 3 - 1);

            positions.add((float) x);
            positions.add((float) y + 1);
            positions.add((float) z + 1);
            textCoords.add(0.0f);
            textCoords.add(1.0f);
            indices.add(positions.size() / 3 - 1);

            indices.add(positions.size() / 3 - 4);
            indices.add(positions.size() / 3 - 3);
            indices.add(positions.size() / 3 - 2);
            indices.add(positions.size() / 3 - 4);
            indices.add(positions.size() / 3 - 2);
            indices.add(positions.size() / 3 - 1);
        }

        // Back face
        if (chunk.getBlock(x, y, z - 1) == null) {
            positions.add((float) x);
            positions.add((float) y);
            positions.add((float) z);
            textCoords.add(0.0f);
            textCoords.add(0.0f);
            indices.add(positions.size() / 3 - 1);

            positions.add((float) x);
            positions.add((float) y + 1);
            positions.add((float) z);
            textCoords.add(0.0f);
            textCoords.add(1.0f);
            indices.add(positions.size() / 3 - 1);

            positions.add((float) x + 1);
            positions.add((float) y + 1);
            positions.add((float) z);
            textCoords.add(1.0f);
            textCoords.add(1.0f);
            indices.add(positions.size() / 3 - 1);

            positions.add((float) x + 1);
            positions.add((float) y);
            positions.add((float) z);
            textCoords.add(1.0f);
            textCoords.add(0.0f);
            indices.add(positions.size() / 3 - 1);

            indices.add(positions.size() / 3 - 4);
            indices.add(positions.size() / 3 - 3);
            indices.add(positions.size() / 3 - 2);
            indices.add(positions.size() / 3 - 4);
            indices.add(positions.size() / 3 - 2);
            indices.add(positions.size() / 3 - 1);
        }

        // Top face
        if (chunk.getBlock(x, y + 1, z) == null) {
            positions.add((float) x);
            positions.add((float) y + 1);
            positions.add((float) z);
            textCoords.add(0.0f);
            textCoords.add(0.0f);
            indices.add(positions.size() / 3 - 1);

            positions.add((float) x);
            positions.add((float) y + 1);
            positions.add((float) z + 1);
            textCoords.add(0.0f);
            textCoords.add(1.0f);
            indices.add(positions.size() / 3 - 1);

            positions.add((float) x + 1);
            positions.add((float) y + 1);
            positions.add((float) z + 1);
            textCoords.add(1.0f);
            textCoords.add(1.0f);
            indices.add(positions.size() / 3 - 1);

            positions.add((float) x + 1);
            positions.add((float) y + 1);
            positions.add((float) z);
            textCoords.add(1.0f);
            textCoords.add(0.0f);
            indices.add(positions.size() / 3 - 1);

            indices.add(positions.size() / 3 - 4);
            indices.add(positions.size() / 3 - 3);
            indices.add(positions.size() / 3 - 2);
            indices.add(positions.size() / 3 - 4);
            indices.add(positions.size() / 3 - 2);
            indices.add(positions.size() / 3 - 1);
        }

        // Bottom face
        if (chunk.getBlock(x, y - 1, z) == null) {
            positions.add((float) x);
            positions.add((float) y);
            positions.add((float) z);
            textCoords.add(0.0f);
            textCoords.add(0.0f);
            indices.add(positions.size() / 3 - 1);

            positions.add((float) x + 1);
            positions.add((float) y);
            positions.add((float) z);
            textCoords.add(1.0f);
            textCoords.add(0.0f);
            indices.add(positions.size() / 3 - 1);

            positions.add((float) x + 1);
            positions.add((float) y);
            positions.add((float) z + 1);
            textCoords.add(1.0f);
            textCoords.add(1.0f);
            indices.add(positions.size() / 3 - 1);

            positions.add((float) x);
            positions.add((float) y);
            positions.add((float) z + 1);
            textCoords.add(0.0f);
            textCoords.add(1.0f);
            indices.add(positions.size() / 3 - 1);

            indices.add(positions.size() / 3 - 4);
            indices.add(positions.size() / 3 - 3);
            indices.add(positions.size() / 3 - 2);
            indices.add(positions.size() / 3 - 4);
            indices.add(positions.size() / 3 - 2);
            indices.add(positions.size() / 3 - 1);
        }

        // Left face
        if (chunk.getBlock(x - 1, y, z) == null) {
            positions.add((float) x);
            positions.add((float) y);
            positions.add((float) z);
            textCoords.add(0.0f);
            textCoords.add(0.0f);
            indices.add(positions.size() / 3 - 1);

            positions.add((float) x);
            positions.add((float) y);
            positions.add((float) z + 1);
            textCoords.add(1.0f);
            textCoords.add(0.0f);
            indices.add(positions.size() / 3 - 1);

            positions.add((float) x);
            positions.add((float) y + 1);
            positions.add((float) z + 1);
            textCoords.add(1.0f);
            textCoords.add(1.0f);
            indices.add(positions.size() / 3 - 1);

            positions.add((float) x);
            positions.add((float) y + 1);
            positions.add((float) z);
            textCoords.add(0.0f);
            textCoords.add(1.0f);
            indices.add(positions.size() / 3 - 1);

            indices.add(positions.size() / 3 - 4);
            indices.add(positions.size() / 3 - 3);
            indices.add(positions.size() / 3 - 2);
            indices.add(positions.size() / 3 - 4);
            indices.add(positions.size() / 3 - 2);
            indices.add(positions.size() / 3 - 1);
        }

        // Right face
        if (chunk.getBlock(x + 1, y, z) == null) {
            positions.add((float) x + 1);
            positions.add((float) y);
            positions.add((float) z);
            textCoords.add(0.0f);
            textCoords.add(0.0f);
            indices.add(positions.size() / 3 - 1);

            positions.add((float) x + 1);
            positions.add((float) y + 1);
            positions.add((float) z);
            textCoords.add(0.0f);
            textCoords.add(1.0f);
            indices.add(positions.size() / 3 - 1);

            positions.add((float) x + 1);
            positions.add((float) y + 1);
            positions.add((float) z + 1);
            textCoords.add(1.0f);
            textCoords.add(1.0f);
            indices.add(positions.size() / 3 - 1);

            positions.add((float) x + 1);
            positions.add((float) y);
            positions.add((float) z + 1);
            textCoords.add(1.0f);
            textCoords.add(0.0f);
            indices.add(positions.size() / 3 - 1);

            indices.add(positions.size() / 3 - 4);
            indices.add(positions.size() / 3 - 3);
            indices.add(positions.size() / 3 - 2);
            indices.add(positions.size() / 3 - 4);
            indices.add(positions.size() / 3 - 2);
            indices.add(positions.size() / 3 - 1);
        }
    }
}
