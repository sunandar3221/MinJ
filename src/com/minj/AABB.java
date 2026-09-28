package com.minj;

public class AABB {
    public double minX, minY, minZ;
    public double maxX, maxY, maxZ;

    public AABB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public AABB move(double dx, double dy, double dz) {
        return new AABB(minX + dx, minY + dy, minZ + dz, maxX + dx, maxY + dy, maxZ + dz);
    }

    public boolean intersects(AABB o) {
        return (maxX > o.minX && minX < o.maxX) &&
               (maxY > o.minY && minY < o.maxY) &&
               (maxZ > o.minZ && minZ < o.maxZ);
    }

    public double clipXCollide(AABB c, double xa) {
        if (c.maxY <= this.minY || c.minY >= this.maxY) return xa;
        if (c.maxZ <= this.minZ || c.minZ >= this.maxZ) return xa;

        if (xa > 0.0 && c.maxX <= this.minX) {
            double max = this.minX - c.maxX;
            if (max < xa) xa = max;
        }
        if (xa < 0.0 && c.minX >= this.maxX) {
            double max = this.maxX - c.minX;
            if (max > xa) xa = max;
        }
        return xa;
    }

    public double clipYCollide(AABB c, double ya) {
        if (c.maxX <= this.minX || c.minX >= this.maxX) return ya;
        if (c.maxZ <= this.minZ || c.minZ >= this.maxZ) return ya;

        if (ya > 0.0 && c.maxY <= this.minY) {
            double max = this.minY - c.maxY;
            if (max < ya) ya = max;
        }
        if (ya < 0.0 && c.minY >= this.maxY) {
            double max = this.maxY - c.minY;
            if (max > ya) ya = max;
        }
        return ya;
    }

    public double clipZCollide(AABB c, double za) {
        if (c.maxX <= this.minX || c.minX >= this.maxX) return za;
        if (c.maxY <= this.minY || c.minY >= this.maxY) return za;

        if (za > 0.0 && c.maxZ <= this.minZ) {
            double max = this.minZ - c.maxZ;
            if (max < za) za = max;
        }
        if (za < 0.0 && c.minZ >= this.maxZ) {
            double max = this.maxZ - c.minZ;
            if (max > za) za = max;
        }
        return za;
    }
}
