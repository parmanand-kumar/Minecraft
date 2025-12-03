package com.minecraft.Generation;

import com.minecraft.core.Block;
import com.minecraft.core.Chunk;
import com.minecraft.graphics.Mesh;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Terrain {

    private static final int TERRAIN_WIDTH = 8;  // 8x8 chunks = 128x128 blocks
    private static final int TERRAIN_DEPTH = 8;
    
    // Terrain generation parameters
    private static final int WATER_LEVEL = 32;
    private static final int MAX_HEIGHT = 48;
    private static final double FREQUENCY = 0.05;  // Controls how "zoomed in" the noise is
    private static final int OCTAVES = 4;  // More octaves = more detail
    private static final double PERSISTENCE = 0.5;  // How much each octave contributes
    
    private final Chunk[][] chunks;
    private final long seed;

    public Terrain() {
        this(System.currentTimeMillis());
    }
    
    public Terrain(long seed) {
        this.seed = seed;
        chunks = new Chunk[TERRAIN_WIDTH][TERRAIN_DEPTH];
        for (int x = 0; x < TERRAIN_WIDTH; x++) {
            for (int z = 0; z < TERRAIN_DEPTH; z++) {
                chunks[x][z] = new Chunk();
            }
        }
    }

    public void generateTerrain() {
        for (int chunkX = 0; chunkX < TERRAIN_WIDTH; chunkX++) {
            for (int chunkZ = 0; chunkZ < TERRAIN_DEPTH; chunkZ++) {
                generateChunk(chunks[chunkX][chunkZ], chunkX, chunkZ);
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

    private void generateChunk(Chunk chunk, int chunkX, int chunkZ) {
        for (int x = 0; x < Chunk.CHUNK_WIDTH; x++) {
            for (int z = 0; z < Chunk.CHUNK_DEPTH; z++) {
                // Calculate world position
                int worldX = chunkX * Chunk.CHUNK_WIDTH + x;
                int worldZ = chunkZ * Chunk.CHUNK_DEPTH + z;
                
                // Generate height using multi-octave Perlin-like noise
                int height = generateHeight(worldX, worldZ);
                
                // Fill ALL blocks from y=0 (bedrock) to the surface height
                for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                    if (y <= height) {
                        int blockType = getBlockType(y, height);
                        chunk.setBlock(x, y, z, new Block(blockType));
                    }
                    // Leave air above the terrain (y > height means no block)
                }
            }
        }
    }
    
    private int generateHeight(int worldX, int worldZ) {
        double noise = 0;
        double amplitude = 1;
        double frequency = FREQUENCY;
        double maxValue = 0;
        
        // Multi-octave noise for more natural terrain
        for (int i = 0; i < OCTAVES; i++) {
            noise += perlinNoise(worldX * frequency, worldZ * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= PERSISTENCE;
            frequency *= 2;
        }
        
        // Normalize and scale to height range
        noise = noise / maxValue;
        int height = (int) ((noise + 1) / 2 * (MAX_HEIGHT - WATER_LEVEL)) + WATER_LEVEL;
        
        return Math.min(height, Chunk.CHUNK_HEIGHT - 1);
    }
    
    private int getBlockType(int y, int terrainHeight) {
        // Grass on top layer
        if (y == terrainHeight) {
            if (terrainHeight >= WATER_LEVEL) {
                return 1;  // Grass block
            } else {
                return 4;  // Sand/gravel underwater
            }
        }
        // Dirt layer (3-5 blocks deep from surface)
        else if (y > terrainHeight - 5 && y < terrainHeight) {
            return 2;  // Dirt block
        }
        // Everything below is stone
        else {
            return 3;  // Stone block
        }
    }
    
    // Simple Perlin-like noise implementation
    private double perlinNoise(double x, double z) {
        // Get integer parts
        int xi = (int) Math.floor(x);
        int zi = (int) Math.floor(z);
        
        // Get fractional parts
        double xf = x - xi;
        double zf = z - zi;
        
        // Smooth interpolation
        double u = fade(xf);
        double v = fade(zf);
        
        // Hash coordinates of the 4 cube corners
        double n00 = dotGridGradient(xi, zi, x, z);
        double n10 = dotGridGradient(xi + 1, zi, x, z);
        double n01 = dotGridGradient(xi, zi + 1, x, z);
        double n11 = dotGridGradient(xi + 1, zi + 1, x, z);
        
        // Interpolate
        double x1 = lerp(n00, n10, u);
        double x2 = lerp(n01, n11, u);
        
        return lerp(x1, x2, v);
    }
    
    private double dotGridGradient(int ix, int iz, double x, double z) {
        // Get gradient from hash
        Random rand = new Random(hash(ix, iz, seed));
        double angle = rand.nextDouble() * 2 * Math.PI;
        double gradX = Math.cos(angle);
        double gradZ = Math.sin(angle);
        
        // Distance vector
        double dx = x - ix;
        double dz = z - iz;
        
        // Dot product
        return dx * gradX + dz * gradZ;
    }
    
    private long hash(int x, int z, long seed) {
        long hash = seed;
        hash = hash * 31 + x;
        hash = hash * 31 + z;
        return hash;
    }
    
    private double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }
    
    private double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    private void generateChunkMesh(int chunkX, int chunkZ, List<Float> positions, List<Float> textCoords, List<Integer> indices) {
        Chunk chunk = chunks[chunkX][chunkZ];
        for (int x = 0; x < Chunk.CHUNK_WIDTH; x++) {
            for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                for (int z = 0; z < Chunk.CHUNK_DEPTH; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (block != null) {
                        generateBlockMesh(chunkX, chunkZ, x, y, z, positions, textCoords, indices);
                    }
                }
            }
        }
    }

    private void generateBlockMesh(int chunkX, int chunkZ, int x, int y, int z, List<Float> positions, List<Float> textCoords, List<Integer> indices) {
        // Calculate world position for proper rendering
        float worldX = chunkX * Chunk.CHUNK_WIDTH + x;
        float worldZ = chunkZ * Chunk.CHUNK_DEPTH + z;
        
        // Front face (z+1)
        addFace(worldX, y, worldZ + 1, worldX + 1, y + 1, worldZ + 1, 
               0, 0, 1, positions, textCoords, indices);

        // Back face (z-1)
        addFace(worldX + 1, y, worldZ, worldX, y + 1, worldZ, 
               0, 0, -1, positions, textCoords, indices);

        // Top face (y+1)
        addFace(worldX, y + 1, worldZ, worldX + 1, y + 1, worldZ + 1, 
               0, 1, 0, positions, textCoords, indices);

        // Bottom face (y-1)
        addFace(worldX, y, worldZ + 1, worldX + 1, y, worldZ, 
               0, -1, 0, positions, textCoords, indices);

        // Left face (x-1)
        addFace(worldX, y, worldZ, worldX, y + 1, worldZ + 1, 
               -1, 0, 0, positions, textCoords, indices);

        // Right face (x+1)
        addFace(worldX + 1, y, worldZ + 1, worldX + 1, y + 1, worldZ, 
               1, 0, 0, positions, textCoords, indices);
    }

    private void addFace(float x1, float y1, float z1, float x2, float y2, float z2,
                        int normX, int normY, int normZ,
                        List<Float> positions, List<Float> textCoords, List<Integer> indices) {
        int vertexOffset = positions.size() / 3;
        
        // Determine face orientation and add vertices accordingly
        if (normY != 0) {
            // Horizontal face (top/bottom)
            positions.add(x1); positions.add(y1); positions.add(z1);
            textCoords.add(0.0f); textCoords.add(0.0f);
            
            positions.add(x1); positions.add(y1); positions.add(z2);
            textCoords.add(0.0f); textCoords.add(1.0f);
            
            positions.add(x2); positions.add(y2); positions.add(z2);
            textCoords.add(1.0f); textCoords.add(1.0f);
            
            positions.add(x2); positions.add(y2); positions.add(z1);
            textCoords.add(1.0f); textCoords.add(0.0f);
        } else {
            // Vertical face
            positions.add(x1); positions.add(y1); positions.add(z1);
            textCoords.add(0.0f); textCoords.add(0.0f);
            
            positions.add(x2); positions.add(y1); positions.add(z2);
            textCoords.add(1.0f); textCoords.add(0.0f);
            
            positions.add(x2); positions.add(y2); positions.add(z2);
            textCoords.add(1.0f); textCoords.add(1.0f);
            
            positions.add(x1); positions.add(y2); positions.add(z1);
            textCoords.add(0.0f); textCoords.add(1.0f);
        }
        
        // Add indices for two triangles
        indices.add(vertexOffset);
        indices.add(vertexOffset + 1);
        indices.add(vertexOffset + 2);
        
        indices.add(vertexOffset);
        indices.add(vertexOffset + 2);
        indices.add(vertexOffset + 3);
    }
}