package com.minj;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class World {
    public static final int WATER_LEVEL = 26;
    private final Map<Long, Chunk> chunks = new HashMap<>();
    private final PerlinNoise noise;
    private final PerlinNoise detailNoise;

    public World(long seed) {
        this.noise = new PerlinNoise(seed);
        this.detailNoise = new PerlinNoise(seed + 9999);
        generateInitialWorld(-3, -3, 3, 3);
    }

    public static long chunkKey(int cx, int cz) {
        return (((long) cx) << 32) | (cz & 0xFFFFFFFFL);
    }

    public Chunk getChunk(int cx, int cz) {
        return chunks.get(chunkKey(cx, cz));
    }

    public Chunk getOrCreateChunk(int cx, int cz) {
        long key = chunkKey(cx, cz);
        Chunk c = chunks.get(key);
        if (c == null) {
            c = new Chunk(cx, cz);
            generateChunkTerrain(c);
            chunks.put(key, c);
        }
        return c;
    }

    public void generateInitialWorld(int minCx, int minCz, int maxCx, int maxCz) {
        // Generate chunk blocks
        for (int cz = minCz; cz <= maxCz; cz++) {
            for (int cx = minCx; cx <= maxCx; cx++) {
                getOrCreateChunk(cx, cz);
            }
        }
        // Place features (trees) after basic terrain is established
        for (int cz = minCz; cz <= maxCz; cz++) {
            for (int cx = minCx; cx <= maxCx; cx++) {
                decorateChunk(getChunk(cx, cz));
            }
        }
        // Build initial meshes
        for (Chunk c : chunks.values()) {
            c.rebuildMesh(this);
        }
    }

    private void generateChunkTerrain(Chunk chunk) {
        Random oreRng = new Random(chunk.worldStartX * 31337L ^ chunk.worldStartZ * 1337L);

        for (int lz = 0; lz < Chunk.SIZE_Z; lz++) {
            for (int lx = 0; lx < Chunk.SIZE_X; lx++) {
                int gx = chunk.worldStartX + lx;
                int gz = chunk.worldStartZ + lz;

                double continental = noise.octaveNoise2D(gx * 0.015, gz * 0.015, 3, 0.5);
                double hills = detailNoise.octaveNoise2D(gx * 0.04, gz * 0.04, 2, 0.5);
                int height = 28 + (int)(continental * 16.0 + hills * 6.0);
                if (height < 6) height = 6;
                if (height >= Chunk.SIZE_Y - 8) height = Chunk.SIZE_Y - 9;

                for (int y = 0; y < Chunk.SIZE_Y; y++) {
                    if (y == 0) {
                        chunk.setBlockLocal(lx, y, lz, Block.BEDROCK.id);
                    } else if (y < height - 3) {
                        // Stone layer with ores
                        byte blockId = Block.STONE.id;
                        int roll = oreRng.nextInt(100);
                        if (roll < 2 && y < 20) {
                            blockId = Block.GOLD_ORE.id;
                        } else if (roll < 7 && y < 35) {
                            blockId = Block.IRON_ORE.id;
                        } else if (roll < 14 && y < 45) {
                            blockId = Block.COAL_ORE.id;
                        }
                        chunk.setBlockLocal(lx, y, lz, blockId);
                    } else if (y < height) {
                        if (height <= WATER_LEVEL + 1) {
                            chunk.setBlockLocal(lx, y, lz, Block.SAND.id);
                        } else {
                            chunk.setBlockLocal(lx, y, lz, Block.DIRT.id);
                        }
                    } else if (y == height) {
                        if (height <= WATER_LEVEL + 1) {
                            chunk.setBlockLocal(lx, y, lz, Block.SAND.id);
                        } else {
                            chunk.setBlockLocal(lx, y, lz, Block.GRASS.id);
                        }
                    } else if (y <= WATER_LEVEL) {
                        chunk.setBlockLocal(lx, y, lz, Block.WATER.id);
                    }
                }
            }
        }
    }

    private void decorateChunk(Chunk chunk) {
        Random treeRng = new Random(chunk.worldStartX * 49999L ^ chunk.worldStartZ * 7919L);
        for (int lx = 2; lx < Chunk.SIZE_X - 2; lx++) {
            for (int lz = 2; lz < Chunk.SIZE_Z - 2; lz++) {
                int gx = chunk.worldStartX + lx;
                int gz = chunk.worldStartZ + lz;

                int topY = getTopBlockY(gx, gz);
                if (topY > WATER_LEVEL + 1 && getBlock(gx, topY, gz) == Block.GRASS.id) {
                    if (treeRng.nextInt(45) == 0) {
                        generateTree(gx, topY + 1, gz, treeRng);
                    }
                }
            }
        }
    }

    public void generateTree(int x, int y, int z, Random rng) {
        int trunkHeight = 4 + rng.nextInt(3);
        // Trunk
        for (int dy = 0; dy < trunkHeight; dy++) {
            setBlockInternal(x, y + dy, z, Block.LOG.id);
        }
        // Leaves crown
        int crownBottom = y + trunkHeight - 2;
        int crownTop = y + trunkHeight + 1;
        for (int cy = crownBottom; cy <= crownTop; cy++) {
            int radius = (cy == crownTop) ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && (rng.nextInt(2) == 0 || cy == crownTop)) {
                        continue;
                    }
                    if (getBlock(x + dx, cy, z + dz) == Block.AIR.id) {
                        setBlockInternal(x + dx, cy, z + dz, Block.LEAVES.id);
                    }
                }
            }
        }
    }

    public int getTopBlockY(int x, int z) {
        for (int y = Chunk.SIZE_Y - 1; y >= 0; y--) {
            byte id = getBlock(x, y, z);
            if (id != Block.AIR.id && id != Block.WATER.id) {
                return y;
            }
        }
        return 0;
    }

    public byte getBlock(int x, int y, int z) {
        if (y < 0 || y >= Chunk.SIZE_Y) return Block.AIR.id;
        int cx = Math.floorDiv(x, Chunk.SIZE_X);
        int cz = Math.floorDiv(z, Chunk.SIZE_Z);
        Chunk c = chunks.get(chunkKey(cx, cz));
        if (c == null) return Block.AIR.id;

        int lx = x - c.worldStartX;
        int lz = z - c.worldStartZ;
        return c.getBlockLocal(lx, y, lz);
    }

    private void setBlockInternal(int x, int y, int z, byte id) {
        if (y < 0 || y >= Chunk.SIZE_Y) return;
        int cx = Math.floorDiv(x, Chunk.SIZE_X);
        int cz = Math.floorDiv(z, Chunk.SIZE_Z);
        Chunk c = chunks.get(chunkKey(cx, cz));
        if (c != null) {
            int lx = x - c.worldStartX;
            int lz = z - c.worldStartZ;
            c.setBlockLocal(lx, y, lz, id);
        }
    }

    public void setBlock(int x, int y, int z, byte id) {
        if (y < 0 || y >= Chunk.SIZE_Y) return;
        int cx = Math.floorDiv(x, Chunk.SIZE_X);
        int cz = Math.floorDiv(z, Chunk.SIZE_Z);
        Chunk c = chunks.get(chunkKey(cx, cz));
        if (c != null) {
            int lx = x - c.worldStartX;
            int lz = z - c.worldStartZ;
            c.setBlockLocal(lx, y, lz, id);
            c.rebuildMesh(this);

            // If on chunk border, mark neighbor dirty and rebuild
            if (lx == 0) updateNeighborChunk(cx - 1, cz);
            if (lx == Chunk.SIZE_X - 1) updateNeighborChunk(cx + 1, cz);
            if (lz == 0) updateNeighborChunk(cx, cz - 1);
            if (lz == Chunk.SIZE_Z - 1) updateNeighborChunk(cx, cz + 1);
        }
    }

    private void updateNeighborChunk(int cx, int cz) {
        Chunk n = chunks.get(chunkKey(cx, cz));
        if (n != null) {
            n.rebuildMesh(this);
        }
    }

    public List<Chunk> getLoadedChunks() {
        return new ArrayList<>(chunks.values());
    }

    public List<AABB> getCubes(AABB box) {
        List<AABB> cubes = new ArrayList<>();
        int x0 = (int) Math.floor(box.minX);
        int x1 = (int) Math.floor(box.maxX + 1.0);
        int y0 = (int) Math.floor(box.minY);
        int y1 = (int) Math.floor(box.maxY + 1.0);
        int z0 = (int) Math.floor(box.minZ);
        int z1 = (int) Math.floor(box.maxZ + 1.0);

        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                for (int z = z0; z < z1; z++) {
                    byte id = getBlock(x, y, z);
                    Block block = Block.getById(id);
                    if (block.isSolid) {
                        cubes.add(new AABB(x, y, z, x + 1, y + 1, z + 1));
                    }
                }
            }
        }
        return cubes;
    }
}
