package com.minj;

public class FaceQuad {
    // 4 vertices (x, y, z) in world space
    public final float[] x = new float[4];
    public final float[] y = new float[4];
    public final float[] z = new float[4];

    // Texture UVs for 4 corners (0=top-left, 1=top-right, 2=bottom-right, 3=bottom-left)
    public final float[] u = new float[4];
    public final float[] v = new float[4];

    // Lighting multipliers for 4 vertices (Ambient Occlusion + Directional Shading)
    public final float[] light = new float[4];

    public int textureId;
    public int faceDirection; // 0=TOP, 1=BOTTOM, 2=NORTH, 3=SOUTH, 4=WEST, 5=EAST
    public int blockX, blockY, blockZ; // Coordinates of the block this quad belongs to

    public FaceQuad() {
        u[0] = 0.0f; v[0] = 0.0f;
        u[1] = 1.0f; v[1] = 0.0f;
        u[2] = 1.0f; v[2] = 1.0f;
        u[3] = 0.0f; v[3] = 1.0f;
    }
}
