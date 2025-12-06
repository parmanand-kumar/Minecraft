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
    
    // Terrain generation parameters - tuned for Minecraft-like terrain
    private static final int WATER_LEVEL = 32;
    private static final int MAX_HEIGHT = 64;
    
    // Base terrain (rolling hills)
    private static final double BASE_FREQUENCY = 0.008;
    private static final int BASE_OCTAVES = 4;
    private static final double BASE_PERSISTENCE = 0.5;
    private static final double BASE_AMPLITUDE = 20.0;
    
    // Detail layer (adds small variations)
    private static final double DETAIL_FREQUENCY = 0.04;
    private static final int DETAIL_OCTAVES = 3;
    private static final double DETAIL_PERSISTENCE = 0.4;
    private static final double DETAIL_AMPLITUDE = 5.0;
    
    // Mountain layer (creates dramatic peaks)
    private static final double MOUNTAIN_FREQUENCY = 0.003;
    private static final int MOUNTAIN_OCTAVES = 2;
    private static final double MOUNTAIN_PERSISTENCE = 0.55;
    private static final double MOUNTAIN_AMPLITUDE = 25.0;

    private final long seed;
    private final int[] p = new int[512];
    private final double[][] G = {
            {1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
            {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
            {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1},
            {1, 1, 0}, {-1, 1, 0}, {0, -1, 1}, {0, -1, -1}
    };

    private int enableCulling = 0;

    public Terrain(long seed, int renderDistance) {
        this.seed = seed;
        this.renderDistance = renderDistance;
        initPerlin();
    }

    public void setEnableCulling(int enableCulling) {
        this.enableCulling = enableCulling;
    }

    public void update(int playerChunkX, int playerChunkZ) {
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
            chunks.put(key, new Chunk());
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
        generateChunkMesh(chunkX, chunkZ, positionsMap, textCoordsMap, indicesMap);

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

        chunk.setPendingMeshData(posArrMap, textCoordsArrMap, indicesArrMap);
    }

    private void generateChunk(Chunk chunk, int chunkX, int chunkZ) {
        for (int x = 0; x < Chunk.CHUNK_WIDTH; x++) {
            for (int z = 0; z < Chunk.CHUNK_DEPTH; z++) {
                int worldX = chunkX * Chunk.CHUNK_WIDTH + x;
                int worldZ = chunkZ * Chunk.CHUNK_DEPTH + z;
                
                int height = generateHeight(worldX, worldZ);
                
                for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                    if (y <= height) {
                        int blockType = getBlockType(y, height);
                        chunk.setBlock(x, y, z, new Block(blockType));
                    }
                }
            }
        }
    }
    
    private int generateHeight(int worldX, int worldZ) {
        // Base terrain - rolling hills
        double baseNoise = octaveNoise(worldX, worldZ, BASE_FREQUENCY, BASE_OCTAVES, BASE_PERSISTENCE);
        double baseHeight = baseNoise * BASE_AMPLITUDE;
        
        // Detail layer - small variations
        double detailNoise = octaveNoise(worldX + 1000, worldZ + 1000, DETAIL_FREQUENCY, DETAIL_OCTAVES, DETAIL_PERSISTENCE);
        double detailHeight = detailNoise * DETAIL_AMPLITUDE;
        
        // Mountain layer - dramatic peaks
        double mountainNoise = octaveNoise(worldX + 2000, worldZ + 2000, MOUNTAIN_FREQUENCY, MOUNTAIN_OCTAVES, MOUNTAIN_PERSISTENCE);
        // Make mountains sparse - only where noise is high
        mountainNoise = Math.max(0, (mountainNoise - 0.3) * 2.5);
        double mountainHeight = mountainNoise * MOUNTAIN_AMPLITUDE;
        
        // Combine all layers
        int finalHeight = WATER_LEVEL + (int)(baseHeight + detailHeight + mountainHeight);
        
        return Math.max(0, Math.min(finalHeight, Chunk.CHUNK_HEIGHT - 1));
    }
    
    private double octaveNoise(double x, double z, double frequency, int octaves, double persistence) {
        double noise = 0;
        double amplitude = 1;
        double freq = frequency;
        double maxValue = 0;
        
        for (int i = 0; i < octaves; i++) {
            noise += perlinNoise(x * freq, z * freq) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            freq *= 2;
        }
        
        return noise / maxValue;
    }

    public int getHeight(int worldX, int worldZ) {
        return generateHeight(worldX, worldZ);
    }
    
    private int getBlockType(int y, int terrainHeight) {
        if (y == terrainHeight) {
            if (terrainHeight >= WATER_LEVEL) {
                return 1;  // Grass block
            } else {
                return 4;  // Sand underwater
            }
        }
        else if (y > terrainHeight - 5 && y < terrainHeight) {
            return 2;  // Dirt block
        }
        else {
            return 3;  // Stone block
        }
    }
    
    private double perlinNoise(double x, double z) {
        int xi = (int) Math.floor(x) & 255;
        int zi = (int) Math.floor(z) & 255;
        
        double xf = x - Math.floor(x);
        double zf = z - Math.floor(z);
        
        double u = fade(xf);
        double v = fade(zf);
        
        // Get gradient indices for the 4 corners
        int aa = p[p[xi] + zi];
        int ab = p[p[xi] + zi + 1];
        int ba = p[p[xi + 1] + zi];
        int bb = p[p[xi + 1] + zi + 1];
        
        // Calculate dot products with gradients
        double x1 = lerp(grad(aa, xf, zf), grad(ba, xf - 1, zf), u);
        double x2 = lerp(grad(ab, xf, zf - 1), grad(bb, xf - 1, zf - 1), u);
        
        return lerp(x1, x2, v);
    }
    
    private double grad(int hash, double x, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : z;
        double v = h < 4 ? z : (h == 12 || h == 14 ? x : 0);
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    private void initPerlin() {
        Random rand = new Random(seed);
        for (int i = 0; i < 256; i++) {
            p[i] = i;
        }
        for (int i = 0; i < 256; i++) {
            int r = rand.nextInt(256 - i) + i;
            int temp = p[i];
            p[i] = p[r];
            p[r] = temp;
        }
        for (int i = 0; i < 256; i++) {
            p[i + 256] = p[i];
        }
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
        
        if (isFaceExposed(chunkX, chunkZ, x, y, z, 0, 1, 0)) {
            String topTexture = getTextureForFace(blockType, 0, 1, 0);
            addFace(worldX, y + 1, worldZ, worldX + 1, y + 1, worldZ + 1, 0, 1, 0, topTexture, positionsMap, textCoordsMap, indicesMap);
        }

        if (isFaceExposed(chunkX, chunkZ, x, y, z, 0, -1, 0)) {
            String bottomTexture = getTextureForFace(blockType, 0, -1, 0);
            addFace(worldX, y, worldZ + 1, worldX + 1, y, worldZ, 0, -1, 0, bottomTexture, positionsMap, textCoordsMap, indicesMap);
        }

        if (isFaceExposed(chunkX, chunkZ, x, y, z, 0, 0, 1)) {
            String frontTexture = getTextureForFace(blockType, 0, 0, 1);
            addFace(worldX, y, worldZ + 1, worldX + 1, y + 1, worldZ + 1, 0, 0, 1, frontTexture, positionsMap, textCoordsMap, indicesMap);
        }

        if (isFaceExposed(chunkX, chunkZ, x, y, z, 0, 0, -1)) {
            String backTexture = getTextureForFace(blockType, 0, 0, -1);
            addFace(worldX + 1, y, worldZ, worldX, y + 1, worldZ, 0, 0, -1, backTexture, positionsMap, textCoordsMap, indicesMap);
        }

        if (isFaceExposed(chunkX, chunkZ, x, y, z, 1, 0, 0)) {
            String rightTexture = getTextureForFace(blockType, 1, 0, 0);
            addFace(worldX + 1, y, worldZ + 1, worldX + 1, y + 1, worldZ, 1, 0, 0, rightTexture, positionsMap, textCoordsMap, indicesMap);
        }

        if (isFaceExposed(chunkX, chunkZ, x, y, z, -1, 0, 0)) {
            String leftTexture = getTextureForFace(blockType, -1, 0, 0);
            addFace(worldX, y, worldZ, worldX, y + 1, worldZ + 1, -1, 0, 0, leftTexture, positionsMap, textCoordsMap, indicesMap);
        }
    }

    private boolean isFaceExposed(int chunkX, int chunkZ, int x, int y, int z, int dirX, int dirY, int dirZ) {
        if (enableCulling == 0) return true;
        
        int neighborWorldX = chunkX * Chunk.CHUNK_WIDTH + x + dirX;
        int neighborWorldZ = chunkZ * Chunk.CHUNK_DEPTH + z + dirZ;

        int neighborChunkX = Math.floorDiv(neighborWorldX, Chunk.CHUNK_WIDTH);
        int neighborChunkZ = Math.floorDiv(neighborWorldZ, Chunk.CHUNK_DEPTH);

        int localX = Math.floorMod(neighborWorldX, Chunk.CHUNK_WIDTH);
        int localZ = Math.floorMod(neighborWorldZ, Chunk.CHUNK_DEPTH);
        int localY = y + dirY;

        if (localY < 0 || localY >= Chunk.CHUNK_HEIGHT) return true;

        Chunk neighborChunk = chunks.get(neighborChunkX + "_" + neighborChunkZ);
        if (neighborChunk == null) return true;

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

        if (normY != 0) {
            positions.add(x1); positions.add(y1); positions.add(z1);
            textCoords.add(0.0f); textCoords.add(1.0f);

            positions.add(x1); positions.add(y1); positions.add(z2);
            textCoords.add(0.0f); textCoords.add(0.0f);

            positions.add(x2); positions.add(y2); positions.add(z2);
            textCoords.add(1.0f); textCoords.add(0.0f);

            positions.add(x2); positions.add(y2); positions.add(z1);
            textCoords.add(1.0f); textCoords.add(1.0f);
        } else {
            positions.add(x1); positions.add(y1); positions.add(z1);
            textCoords.add(0.0f); textCoords.add(1.0f);

            positions.add(x2); positions.add(y1); positions.add(z2);
            textCoords.add(1.0f); textCoords.add(1.0f);

            positions.add(x2); positions.add(y2); positions.add(z2);
            textCoords.add(1.0f); textCoords.add(0.0f);

            positions.add(x1); positions.add(y2); positions.add(z1);
            textCoords.add(0.0f); textCoords.add(0.0f);
        }

        indices.add(vertexOffset);
        indices.add(vertexOffset + 1);
        indices.add(vertexOffset + 2);

        indices.add(vertexOffset);
        indices.add(vertexOffset + 2);
        indices.add(vertexOffset + 3);
    }
    
    private String getTextureForFace(int blockType, int normX, int normY, int normZ) {
        switch (blockType) {
            case 1:
                if (normY == 1) return "grass_top";
                if (normY == -1) return "dirt";
                return "grass_side";
            case 2:
                return "dirt";
            case 3:
                return "stone";
            case 4:
                return "sand";
            default:
                return "default";
        }
    }

    public Map<String, Mesh> generateMeshes() {
        Map<String, Mesh> combined = new HashMap<>();
        for (Map.Entry<String, Chunk> entry : chunks.entrySet()) {
            String chunkKey = entry.getKey();
            Chunk chunk = entry.getValue();
            if (chunk == null) continue;
            
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
                combined.put(chunkKey + "_" + m.getKey(), m.getValue());
            }
        }
        return combined;
    }

    public void cleanup() {
        executor.shutdown();
    }
}