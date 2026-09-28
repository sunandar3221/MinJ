package com.minj;

public class Raycast {
    public static class HitResult {
        public boolean hit = false;
        public int x, y, z;          // Voxel hit
        public int faceX, faceY, faceZ; // Normal of the face hit (-1, 0, or 1)
        public double distance;
    }

    public static HitResult cast(World world, double startX, double startY, double startZ,
                                 double dirX, double dirY, double dirZ, double maxDist) {
        HitResult result = new HitResult();

        // Normalize direction
        double len = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (len == 0.0) return result;
        dirX /= len; dirY /= len; dirZ /= len;

        int x = (int) Math.floor(startX);
        int y = (int) Math.floor(startY);
        int z = (int) Math.floor(startZ);

        int stepX = dirX >= 0 ? 1 : -1;
        int stepY = dirY >= 0 ? 1 : -1;
        int stepZ = dirZ >= 0 ? 1 : -1;

        double tDeltaX = Math.abs(1.0 / dirX);
        double tDeltaY = Math.abs(1.0 / dirY);
        double tDeltaZ = Math.abs(1.0 / dirZ);

        double tMaxX = (dirX >= 0 ? (x + 1.0 - startX) : (startX - x)) * tDeltaX;
        double tMaxY = (dirY >= 0 ? (y + 1.0 - startY) : (startY - y)) * tDeltaY;
        double tMaxZ = (dirZ >= 0 ? (z + 1.0 - startZ) : (startZ - z)) * tDeltaZ;

        int lastFaceX = 0, lastFaceY = 0, lastFaceZ = 0;
        double currentT = 0.0;

        while (currentT <= maxDist) {
            byte id = world.getBlock(x, y, z);
            Block b = Block.getById(id);
            if (b.isSolid) {
                result.hit = true;
                result.x = x;
                result.y = y;
                result.z = z;
                result.faceX = lastFaceX;
                result.faceY = lastFaceY;
                result.faceZ = lastFaceZ;
                result.distance = currentT;
                return result;
            }

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    currentT = tMaxX;
                    tMaxX += tDeltaX;
                    x += stepX;
                    lastFaceX = -stepX;
                    lastFaceY = 0;
                    lastFaceZ = 0;
                } else {
                    currentT = tMaxZ;
                    tMaxZ += tDeltaZ;
                    z += stepZ;
                    lastFaceX = 0;
                    lastFaceY = 0;
                    lastFaceZ = -stepZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    currentT = tMaxY;
                    tMaxY += tDeltaY;
                    y += stepY;
                    lastFaceX = 0;
                    lastFaceY = -stepY;
                    lastFaceZ = 0;
                } else {
                    currentT = tMaxZ;
                    tMaxZ += tDeltaZ;
                    z += stepZ;
                    lastFaceX = 0;
                    lastFaceY = 0;
                    lastFaceZ = -stepZ;
                }
            }
        }

        return result;
    }
}
