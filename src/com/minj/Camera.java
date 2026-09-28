package com.minj;

public class Camera {
    public double x, y, z;
    public double yaw;   // In radians (horizontal rotation)
    public double pitch; // In radians (vertical look: -PI/2 to PI/2)

    public double cosYaw = 1.0, sinYaw = 0.0;
    public double cosPitch = 1.0, sinPitch = 0.0;

    public void updateTrig() {
        cosYaw = Math.cos(yaw);
        sinYaw = Math.sin(yaw);
        cosPitch = Math.cos(pitch);
        sinPitch = Math.sin(pitch);
    }

    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setRotation(double yaw, double pitch) {
        this.yaw = yaw;
        this.pitch = Math.max(-1.55, Math.min(1.55, pitch));
        updateTrig();
    }

    /**
     * Transforms world point (wx, wy, wz) into camera space (cx, cy, cz).
     * Output stored in out[0]=X (right), out[1]=Y (up), out[2]=Z (forward).
     */
    public void transformToCameraSpace(double wx, double wy, double wz, float[] out) {
        double dx = wx - x;
        double dy = wy - y;
        double dz = wz - z;

        // Yaw rotation around Y axis
        double x1 = dx * cosYaw + dz * sinYaw;
        double z1 = -dx * sinYaw + dz * cosYaw;

        // Pitch rotation around X axis
        double y2 = dy * cosPitch - z1 * sinPitch;
        double z2 = dy * sinPitch + z1 * cosPitch;

        out[0] = (float) x1;
        out[1] = (float) y2;
        out[2] = (float) z2;
    }

    // Forward direction unit vector
    public double getForwardX() {
        return -sinYaw * cosPitch;
    }

    public double getForwardY() {
        return sinPitch;
    }

    public double getForwardZ() {
        return cosYaw * cosPitch;
    }
}
