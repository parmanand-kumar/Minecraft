package com.minecraft.Generation;

import com.minecraft.core.Block;
import com.minecraft.core.Chunk;
import com.minecraft.graphics.Mesh;
import com.minecraft.graphics.TextureHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Terrain {

    private final int renderDistance;
    private final Map<String, Chunk> chunks = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    
    // Terrain generation parameters
    private static final int WATER_LEVEL = 32;
    private static final int MAX_HEIGHT = 48;
    private static final double FREQUENCY = 0.05;  // Controls how "zoomed in" the noise is
    private static final int OCTAVES = 4;  // More octaves = more detail
    private static final double PERSISTENCE = 0.5;  // How much each octave contributes
    
    private final long seed;
    // enableCulling: 0 = disabled (no culling, render all faces), 1 = enabled (cull internal faces)
    // Default 0 => no culling so all blocks are fully rendered
    private int enableCulling = 0;

    public Terrain(long seed, int renderDistance) {
        this.seed = seed;
        this.renderDistance = renderDistance;
    }

    public void setEnableCulling(int enableCulling) {
        this.enableCulling = enableCulling;
    }

    public void update(int playerChunkX, int playerChunkZ) {
        // Unload chunks outside render distance
        chunks.keySet().removeIf(key -> {
            String[] parts = key.split("_");
            int chunkX = Integer.parseInt(parts[0]);
            int chunkZ = Integer.parseInt(parts[1]);
            boolean outOfRange = Math.abs(chunkX - playerChunkX) > renderDistance || Math.abs(chunkZ - playerChunkZ) > renderDistance;
            if (outOfRange) {
                Chunk chunk = chunks.get(key);
                if (chunk != null) {
                    chunk.getMeshes().values().forEach(Mesh::cleanup);
                }
            }
            return outOfRange;
        });

        // Load new chunks within render distance and rebuild meshes
        for (int x = -renderDistance; x <= renderDistance; x++) {
            for (int z = -renderDistance; z <= renderDistance; z++) {
                int chunkX = playerChunkX + x;
                int chunkZ = playerChunkZ + z;
                loadChunk(chunkX, chunkZ);
                Chunk chunk = chunks.get(chunkX + "_" + chunkZ);
                if (chunk != null && chunk.needsRebuild()) {
                    executor.submit(() -> buildChunkMesh(chunkX, chunkZ));
                }
            }
        }
    }

    private void loadChunk(int chunkX, int chunkZ) {
        String key = chunkX + "_" + chunkZ;
        if (!chunks.containsKey(key)) {
            chunks.put(key, new Chunk()); // Placeholder
            executor.submit(() -> {
                Chunk chunk = new Chunk();
                generateChunk(chunk, chunkX, chunkZ);
                chunks.put(key, chunk);
            });
        }
    }

    public void buildChunkMesh(int chunkX, int chunkZ) {
        Chunk chunk = chunks.get(chunkX + "_" + chunkZ);
        if (chunk == null) return;

        Map<String, List<Float>> positionsMap = new HashMap<>();
        Map<String, List<Float>> textCoordsMap = new HashMap<>();
        Map<String, List<Integer>> indicesMap = new HashMap<>();
        // Generate raw mesh data in background thread
        generateChunkMesh(chunkX, chunkZ, positionsMap, textCoordsMap, indicesMap);

        // Convert Lists to primitive arrays and store as pending data on the chunk
        Map<String, float[]> posArrMap = new HashMap<>();
        Map<String, float[]> textCoordsArrMap = new HashMap<>();
        Map<String, int[]> indicesArrMap = new HashMap<>();

        for (String texture : positionsMap.keySet()) {
            List<Float> positions = positionsMap.get(texture);
            List<Float> textCoords = textCoordsMap.get(texture);
            List<Integer> indices = indicesMap.get(texture);

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

            posArrMap.put(texture, posArr);
            textCoordsArrMap.put(texture, textCoordsArr);
            indicesArrMap.put(texture, indicesArr);
        }

        // Store pending data on the chunk for main thread (GPU) upload
        chunk.setPendingMeshData(posArrMap, textCoordsArrMap, indicesArrMap);
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

    public int getHeight(int worldX, int worldZ) {
        return generateHeight(worldX, worldZ);
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

    private void generateChunkMesh(int chunkX, int chunkZ, Map<String, List<Float>> positionsMap, Map<String, List<Float>> textCoordsMap, Map<String, List<Integer>> indicesMap) {
        Chunk chunk = chunks.get(chunkX + "_" + chunkZ);
        if (chunk == null) return;
        for (int x = 0; x < Chunk.CHUNK_WIDTH; x++) {
            for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                for (int z = 0; z < Chunk.CHUNK_DEPTH; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (block != null) {
                        generateBlockMesh(chunkX, chunkZ, x, y, z, positionsMap, textCoordsMap, indicesMap);
                    }
                }
            }
        }
    }

    private void generateBlockMesh(int chunkX, int chunkZ, int x, int y, int z, Map<String, List<Float>> positionsMap, Map<String, List<Float>> textCoordsMap, Map<String, List<Integer>> indicesMap) {
        float worldX = chunkX * Chunk.CHUNK_WIDTH + x;
        float worldZ = chunkZ * Chunk.CHUNK_DEPTH + z;
        Chunk chunk = chunks.get(chunkX + "_" + chunkZ);
        if (chunk == null) return;
        Block block = chunk.getBlock(x, y, z);
        if (block == null) return;
        int blockType = block.getType();
        // Only add faces that are exposed to air (or if neighboring chunk is missing)
        // Top face
        if (isFaceExposed(chunkX, chunkZ, x, y, z, 0, 1, 0)) {
            String topTexture = getTextureForFace(blockType, 0, 1, 0);
            addFace(worldX, y + 1, worldZ, worldX + 1, y + 1, worldZ + 1, 0, 1, 0, topTexture, positionsMap, textCoordsMap, indicesMap);
        }

        // Bottom face
        if (isFaceExposed(chunkX, chunkZ, x, y, z, 0, -1, 0)) {
            String bottomTexture = getTextureForFace(blockType, 0, -1, 0);
            addFace(worldX, y, worldZ + 1, worldX + 1, y, worldZ, 0, -1, 0, bottomTexture, positionsMap, textCoordsMap, indicesMap);
        }

        // Front face
        if (isFaceExposed(chunkX, chunkZ, x, y, z, 0, 0, 1)) {
            String frontTexture = getTextureForFace(blockType, 0, 0, 1);
            addFace(worldX, y, worldZ + 1, worldX + 1, y + 1, worldZ + 1, 0, 0, 1, frontTexture, positionsMap, textCoordsMap, indicesMap);
        }

        // Back face
        if (isFaceExposed(chunkX, chunkZ, x, y, z, 0, 0, -1)) {
            String backTexture = getTextureForFace(blockType, 0, 0, -1);
            addFace(worldX + 1, y, worldZ, worldX, y + 1, worldZ, 0, 0, -1, backTexture, positionsMap, textCoordsMap, indicesMap);
        }

        // Right face
        if (isFaceExposed(chunkX, chunkZ, x, y, z, 1, 0, 0)) {
            String rightTexture = getTextureForFace(blockType, 1, 0, 0);
            addFace(worldX + 1, y, worldZ + 1, worldX + 1, y + 1, worldZ, 1, 0, 0, rightTexture, positionsMap, textCoordsMap, indicesMap);
        }

        // Left face
        if (isFaceExposed(chunkX, chunkZ, x, y, z, -1, 0, 0)) {
            String leftTexture = getTextureForFace(blockType, -1, 0, 0);
            addFace(worldX, y, worldZ, worldX, y + 1, worldZ + 1, -1, 0, 0, leftTexture, positionsMap, textCoordsMap, indicesMap);
        }
    }

    // Returns true if the face in the given direction from (x,y,z) is exposed (neighbor is air or neighbor chunk missing)
    private boolean isFaceExposed(int chunkX, int chunkZ, int x, int y, int z, int dirX, int dirY, int dirZ) {
    // If culling is disabled (enableCulling == 0), consider every face exposed so all faces are generated
    if (enableCulling == 0) return true;
        int neighborWorldX = chunkX * Chunk.CHUNK_WIDTH + x + dirX;
        int neighborWorldZ = chunkZ * Chunk.CHUNK_DEPTH + z + dirZ;

        int neighborChunkX = Math.floorDiv(neighborWorldX, Chunk.CHUNK_WIDTH);
        int neighborChunkZ = Math.floorDiv(neighborWorldZ, Chunk.CHUNK_DEPTH);

        int localX = Math.floorMod(neighborWorldX, Chunk.CHUNK_WIDTH);
        int localZ = Math.floorMod(neighborWorldZ, Chunk.CHUNK_DEPTH);
        int localY = y + dirY;

        // If neighbor Y is outside world bounds, consider it exposed
        if (localY < 0 || localY >= Chunk.CHUNK_HEIGHT) return true;

        Chunk neighborChunk = chunks.get(neighborChunkX + "_" + neighborChunkZ);
        if (neighborChunk == null) return true; // treat missing chunk as air (not built yet)

        Block neighbor = neighborChunk.getBlock(localX, localY, localZ);
        return neighbor == null;
    }

    private void addFace(float x1, float y1, float z1, float x2, float y2, float z2, int normX, int normY, int normZ, String texture, Map<String, List<Float>> positionsMap, Map<String, List<Float>> textCoordsMap, Map<String, List<Integer>> indicesMap) {
        positionsMap.computeIfAbsent(texture, k -> new ArrayList<>());
        textCoordsMap.computeIfAbsent(texture, k -> new ArrayList<>());
        indicesMap.computeIfAbsent(texture, k -> new ArrayList<>());

        List<Float> positions = positionsMap.get(texture);
        List<Float> textCoords = textCoordsMap.get(texture);
        List<Integer> indices = indicesMap.get(texture);

        int vertexOffset = positions.size() / 3;

        // Determine face orientation and add vertices accordingly
        if (normY != 0) {
            // Horizontal face (top/bottom)
            positions.add(x1); positions.add(y1); positions.add(z1);
            textCoords.add(0.0f); textCoords.add(1.0f);

            positions.add(x1); positions.add(y1); positions.add(z2);
            textCoords.add(0.0f); textCoords.add(0.0f);

            positions.add(x2); positions.add(y2); positions.add(z2);
            textCoords.add(1.0f); textCoords.add(0.0f);

            positions.add(x2); positions.add(y2); positions.add(z1);
            textCoords.add(1.0f); textCoords.add(1.0f);
        } else {
            // Vertical face
            positions.add(x1); positions.add(y1); positions.add(z1);
            textCoords.add(0.0f); textCoords.add(1.0f);

            positions.add(x2); positions.add(y1); positions.add(z2);
            textCoords.add(1.0f); textCoords.add(1.0f);

            positions.add(x2); positions.add(y2); positions.add(z2);
            textCoords.add(1.0f); textCoords.add(0.0f);

            positions.add(x1); positions.add(y2); positions.add(z1);
            textCoords.add(0.0f); textCoords.add(0.0f);
        }

        // Add indices for two triangles
        indices.add(vertexOffset);
        indices.add(vertexOffset + 1);
        indices.add(vertexOffset + 2);

        indices.add(vertexOffset);
        indices.add(vertexOffset + 2);
        indices.add(vertexOffset + 3);
    }
    private String getTextureForFace(int blockType, int normX, int normY, int normZ) {
        switch (blockType) {
            case 1: // Grass
                if (normY == 1) return "grass_top";
                if (normY == -1) return "dirt";
                return "grass_side";
            case 2: // Dirt
                return "dirt";
            case 3: // Stone
                return "stone";
            case 4: // Sand
                return "sand";
            default:
                return "default";
        }
    }

    // Returns a combined map of all meshes from loaded chunks. Keys are prefixed with chunk coordinates
    public Map<String, Mesh> generateMeshes() {
        Map<String, Mesh> combined = new HashMap<>();
        for (Map.Entry<String, Chunk> entry : chunks.entrySet()) {
            String chunkKey = entry.getKey();
            Chunk chunk = entry.getValue();
            if (chunk == null) continue;
            // If the chunk has pending raw mesh data produced by a background thread,
            // upload that data to the GPU (create TextureHandler + Mesh) on the main thread.
            if (chunk.hasPendingMeshData()) {
                Map<String, float[]> posMap = chunk.getPendingPositions();
                Map<String, float[]> tcMap = chunk.getPendingTextCoords();
                Map<String, int[]> indMap = chunk.getPendingIndices();
                Map<String, Mesh> meshes = new HashMap<>();
                for (String texture : posMap.keySet()) {
                    float[] posArr = posMap.get(texture);
                    float[] tcArr = tcMap.get(texture);
                    int[] indArr = indMap.get(texture);
                    try {
                        TextureHandler textureHandler = new TextureHandler("src/main/resources/texture/blocks/" + texture + ".png");
                        meshes.put(texture, new Mesh(posArr, tcArr, indArr, textureHandler));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                chunk.setMeshes(meshes);
                chunk.clearPendingMeshData();
            }

            Map<String, Mesh> meshes = chunk.getMeshes();
            if (meshes == null) continue;
            for (Map.Entry<String, Mesh> m : meshes.entrySet()) {
                // Use chunkKey + texture to avoid collisions between chunks using same texture
                combined.put(chunkKey + "_" + m.getKey(), m.getValue());
            }
        }
        return combined;
    }

}