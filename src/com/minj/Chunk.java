package com.minj;

import java.util.ArrayList;
import java.util.List;

public class Chunk {
    public static final int SIZE_X = 16;
    public static final int SIZE_Y = 64;
    public static final int SIZE_Z = 16;

    public final int chunkX;
    public final int chunkZ;
    public final int worldStartX;
    public final int worldStartZ;

    private final byte[] blocks = new byte[SIZE_X * SIZE_Y * SIZE_Z];
    public boolean isDirty = true;

    private final List<FaceQuad> solidMesh = new ArrayList<>();
    private final List<FaceQuad> transparentMesh = new ArrayList<>();

    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.worldStartX = chunkX * SIZE_X;
        this.worldStartZ = chunkZ * SIZE_Z;
    }

    private static int index(int x, int y, int z) {
        return (y * SIZE_Z + z) * SIZE_X + x;
    }

    public byte getBlockLocal(int x, int y, int z) {
        if (x < 0 || x >= SIZE_X || y < 0 || y >= SIZE_Y || z < 0 || z >= SIZE_Z) {
            return 0;
        }
        return blocks[index(x, y, z)];
    }

    public void setBlockLocal(int x, int y, int z, byte id) {
        if (x < 0 || x >= SIZE_X || y < 0 || y >= SIZE_Y || z < 0 || z >= SIZE_Z) {
            return;
        }
        blocks[index(x, y, z)] = id;
        isDirty = true;
    }

    public List<FaceQuad> getSolidMesh() {
        return solidMesh;
    }

    public List<FaceQuad> getTransparentMesh() {
        return transparentMesh;
    }

    public void rebuildMesh(World world) {
        solidMesh.clear();
        transparentMesh.clear();

        for (int y = 0; y < SIZE_Y; y++) {
            for (int z = 0; z < SIZE_Z; z++) {
                for (int x = 0; x < SIZE_X; x++) {
                    byte id = blocks[index(x, y, z)];
                    if (id == 0) continue; // AIR

                    Block block = Block.getById(id);
                    int gx = worldStartX + x;
                    int gy = y;
                    int gz = worldStartZ + z;

                    // 6 Faces: 0=TOP, 1=BOTTOM, 2=NORTH(-Z), 3=SOUTH(+Z), 4=WEST(-X), 5=EAST(+X)
                    checkFace(world, block, gx, gy, gz, 0,  0, 1, 0, 1.0f);
                    checkFace(world, block, gx, gy, gz, 1,  0,-1, 0, 0.5f);
                    checkFace(world, block, gx, gy, gz, 2,  0, 0,-1, 0.8f);
                    checkFace(world, block, gx, gy, gz, 3,  0, 0, 1, 0.8f);
                    checkFace(world, block, gx, gy, gz, 4, -1, 0, 0, 0.7f);
                    checkFace(world, block, gx, gy, gz, 5,  1, 0, 0, 0.7f);
                }
            }
        }
        isDirty = false;
    }

    private void checkFace(World world, Block block, int gx, int gy, int gz, int face, int dx, int dy, int dz, float baseLight) {
        byte neighborId = world.getBlock(gx + dx, gy + dy, gz + dz);
        Block neighbor = Block.getById(neighborId);

        boolean drawFace;
        if (neighbor == Block.AIR) {
            drawFace = true;
        } else if (neighbor.isTransparent) {
            drawFace = (neighbor != block) || (block == Block.LEAVES);
        } else {
            drawFace = false;
        }

        if (!drawFace) return;

        FaceQuad quad = new FaceQuad();
        quad.textureId = block.textureIndices[face];
        quad.faceDirection = face;
        quad.blockX = gx;
        quad.blockY = gy;
        quad.blockZ = gz;

        setupQuadGeometry(quad, gx, gy, gz, face);
        computeAmbientOcclusion(world, quad, gx, gy, gz, face, baseLight);

        if (block.isTransparent) {
            transparentMesh.add(quad);
            if (block == Block.WATER && face == 0) {
                FaceQuad under = new FaceQuad();
                under.textureId = block.textureIndices[face];
                under.faceDirection = 1;
                under.blockX = gx;
                under.blockY = gy + 1;
                under.blockZ = gz;
                float x0 = gx, x1 = gx + 1.0f;
                float y1 = gy + 1.0f;
                float z0 = gz, z1 = gz + 1.0f;
                under.x[0] = x0; under.y[0] = y1; under.z[0] = z1;
                under.x[1] = x1; under.y[1] = y1; under.z[1] = z1;
                under.x[2] = x1; under.y[2] = y1; under.z[2] = z0;
                under.x[3] = x0; under.y[3] = y1; under.z[3] = z0;
                for (int i = 0; i < 4; i++) under.light[i] = 0.85f;
                transparentMesh.add(under);
            }
        } else {
            solidMesh.add(quad);
        }
    }

    private void setupQuadGeometry(FaceQuad q, int gx, int gy, int gz, int face) {
        float x0 = gx, x1 = gx + 1.0f;
        float y0 = gy, y1 = gy + 1.0f;
        float z0 = gz, z1 = gz + 1.0f;

        switch (face) {
            case 0: // TOP (+Y)
                q.x[0] = x0; q.y[0] = y1; q.z[0] = z0;
                q.x[1] = x1; q.y[1] = y1; q.z[1] = z0;
                q.x[2] = x1; q.y[2] = y1; q.z[2] = z1;
                q.x[3] = x0; q.y[3] = y1; q.z[3] = z1;
                break;
            case 1: // BOTTOM (-Y)
                q.x[0] = x0; q.y[0] = y0; q.z[0] = z1;
                q.x[1] = x1; q.y[1] = y0; q.z[1] = z1;
                q.x[2] = x1; q.y[2] = y0; q.z[2] = z0;
                q.x[3] = x0; q.y[3] = y0; q.z[3] = z0;
                break;
            case 2: // NORTH (-Z)
                q.x[0] = x1; q.y[0] = y1; q.z[0] = z0;
                q.x[1] = x0; q.y[1] = y1; q.z[1] = z0;
                q.x[2] = x0; q.y[2] = y0; q.z[2] = z0;
                q.x[3] = x1; q.y[3] = y0; q.z[3] = z0;
                break;
            case 3: // SOUTH (+Z)
                q.x[0] = x0; q.y[0] = y1; q.z[0] = z1;
                q.x[1] = x1; q.y[1] = y1; q.z[1] = z1;
                q.x[2] = x1; q.y[2] = y0; q.z[2] = z1;
                q.x[3] = x0; q.y[3] = y0; q.z[3] = z1;
                break;
            case 4: // WEST (-X)
                q.x[0] = x0; q.y[0] = y1; q.z[0] = z0;
                q.x[1] = x0; q.y[1] = y1; q.z[1] = z1;
                q.x[2] = x0; q.y[2] = y0; q.z[2] = z1;
                q.x[3] = x0; q.y[3] = y0; q.z[3] = z0;
                break;
            case 5: // EAST (+X)
                q.x[0] = x1; q.y[0] = y1; q.z[0] = z1;
                q.x[1] = x1; q.y[1] = y1; q.z[1] = z0;
                q.x[2] = x1; q.y[2] = y0; q.z[2] = z0;
                q.x[3] = x1; q.y[3] = y0; q.z[3] = z1;
                break;
        }
    }

    private void computeAmbientOcclusion(World world, FaceQuad q, int gx, int gy, int gz, int face, float baseLight) {
        for (int i = 0; i < 4; i++) {
            float ao = calculateVertexAO(world, gx, gy, gz, face, i);
            q.light[i] = baseLight * ao;
        }
    }

    private float calculateVertexAO(World world, int x, int y, int z, int face, int corner) {
        int side1 = 0, side2 = 0, cornerBlock = 0;

        // Calculate occlusion depending on face and corner
        switch (face) {
            case 0: // TOP (+Y)
                if (corner == 0) { // (-X, -Z)
                    side1 = isBlockOpaque(world, x - 1, y + 1, z);
                    side2 = isBlockOpaque(world, x, y + 1, z - 1);
                    cornerBlock = isBlockOpaque(world, x - 1, y + 1, z - 1);
                } else if (corner == 1) { // (+X, -Z)
                    side1 = isBlockOpaque(world, x + 1, y + 1, z);
                    side2 = isBlockOpaque(world, x, y + 1, z - 1);
                    cornerBlock = isBlockOpaque(world, x + 1, y + 1, z - 1);
                } else if (corner == 2) { // (+X, +Z)
                    side1 = isBlockOpaque(world, x + 1, y + 1, z);
                    side2 = isBlockOpaque(world, x, y + 1, z + 1);
                    cornerBlock = isBlockOpaque(world, x + 1, y + 1, z + 1);
                } else { // (-X, +Z)
                    side1 = isBlockOpaque(world, x - 1, y + 1, z);
                    side2 = isBlockOpaque(world, x, y + 1, z + 1);
                    cornerBlock = isBlockOpaque(world, x - 1, y + 1, z + 1);
                }
                break;
            case 1: // BOTTOM (-Y)
                if (corner == 0) { // (-X, +Z)
                    side1 = isBlockOpaque(world, x - 1, y - 1, z);
                    side2 = isBlockOpaque(world, x, y - 1, z + 1);
                    cornerBlock = isBlockOpaque(world, x - 1, y - 1, z + 1);
                } else if (corner == 1) { // (+X, +Z)
                    side1 = isBlockOpaque(world, x + 1, y - 1, z);
                    side2 = isBlockOpaque(world, x, y - 1, z + 1);
                    cornerBlock = isBlockOpaque(world, x + 1, y - 1, z + 1);
                } else if (corner == 2) { // (+X, -Z)
                    side1 = isBlockOpaque(world, x + 1, y - 1, z);
                    side2 = isBlockOpaque(world, x, y - 1, z - 1);
                    cornerBlock = isBlockOpaque(world, x + 1, y - 1, z - 1);
                } else { // (-X, -Z)
                    side1 = isBlockOpaque(world, x - 1, y - 1, z);
                    side2 = isBlockOpaque(world, x, y - 1, z - 1);
                    cornerBlock = isBlockOpaque(world, x - 1, y - 1, z - 1);
                }
                break;
            default: // Sides
                return 0.95f;
        }

        if (side1 == 1 && side2 == 1) {
            return 0.55f;
        }
        int sum = side1 + side2 + cornerBlock;
        if (sum == 0) return 1.0f;
        if (sum == 1) return 0.85f;
        if (sum == 2) return 0.70f;
        return 0.55f;
    }

    private int isBlockOpaque(World world, int x, int y, int z) {
        byte b = world.getBlock(x, y, z);
        if (b == 0) return 0;
        Block block = Block.getById(b);
        return (block.isSolid && !block.isTransparent) ? 1 : 0;
    }
}
