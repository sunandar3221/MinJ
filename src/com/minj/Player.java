package com.minj;

import java.util.List;

public class Player {
    public double x, y, z;
    public double vx, vy, vz;

    public final Camera camera = new Camera();

    public final double width = 0.6;
    public final double height = 1.8;
    public final double eyeOffset = 1.62;

    public boolean onGround = false;
    public boolean isFlying = false;

    // Water states
    public boolean inWater = false;
    public boolean headUnderwater = false;
    private boolean wasInWater = false;

    // Hotbar slots (9 items)
    public final Block[] hotbar = new Block[] {
        Block.GRASS,
        Block.DIRT,
        Block.STONE,
        Block.COBBLESTONE,
        Block.WOOD_PLANKS,
        Block.LOG,
        Block.LEAVES,
        Block.GLASS,
        Block.TNT
    };
    public int selectedSlot = 0;

    // Head bobbing & step sounds
    public double walkDistance = 0.0;
    public double bobX = 0.0;
    public double bobY = 0.0;
    private double stepSoundTimer = 0.0;

    public Player(double startX, double startY, double startZ) {
        this.x = startX;
        this.y = startY;
        this.z = startZ;
        updateCamera();
    }

    public AABB getBoundingBox() {
        double r = width / 2.0;
        return new AABB(x - r, y, z - r, x + r, y + height, z + r);
    }

    public Block getSelectedBlock() {
        if (selectedSlot >= 0 && selectedSlot < hotbar.length) {
            return hotbar[selectedSlot];
        }
        return Block.STONE;
    }

    public boolean checkInWater(World world) {
        int x0 = (int) Math.floor(x - width / 2.0);
        int x1 = (int) Math.floor(x + width / 2.0);
        int y0 = (int) Math.floor(y);
        int y1 = (int) Math.floor(y + height * 0.7);
        int z0 = (int) Math.floor(z - width / 2.0);
        int z1 = (int) Math.floor(z + width / 2.0);

        for (int bx = x0; bx <= x1; bx++) {
            for (int by = y0; by <= y1; by++) {
                for (int bz = z0; bz <= z1; bz++) {
                    if (world.getBlock(bx, by, bz) == Block.WATER.id) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean checkHeadUnderwater(World world) {
        int bx = (int) Math.floor(camera.x);
        int by = (int) Math.floor(camera.y);
        int bz = (int) Math.floor(camera.z);
        return world.getBlock(bx, by, bz) == Block.WATER.id;
    }

    public void update(World world, boolean forward, boolean backward, boolean left, boolean right,
                       boolean jump, boolean sneak, boolean sprint) {
        inWater = checkInWater(world);
        headUnderwater = checkHeadUnderwater(world);

        // Water entry splash
        if (inWater && !wasInWater) {
            SoundManager.playSplash();
        }
        wasInWater = inWater;

        double fMove = 0;
        double sMove = 0;
        if (forward) fMove += 1.0;
        if (backward) fMove -= 1.0;
        if (left) sMove -= 1.0;
        if (right) sMove += 1.0;

        double moveLen = Math.hypot(fMove, sMove);
        if (moveLen > 0.001) {
            fMove /= moveLen;
            sMove /= moveLen;
        }

        // Camera directional vectors on horizontal plane
        double sin = Math.sin(camera.yaw);
        double cos = Math.cos(camera.yaw);
        double forwardX = -sin;
        double forwardZ = cos;
        double strafeX = cos;
        double strafeZ = sin;

        if (isFlying) {
            double flySpeed = sprint ? 0.35 : 0.20;
            vx = (forwardX * fMove + strafeX * sMove) * flySpeed;
            vz = (forwardZ * fMove + strafeZ * sMove) * flySpeed;
            vy = 0;
            if (jump) vy += flySpeed;
            if (sneak) vy -= flySpeed;
            moveWithCollision(world, vx, vy, vz);
        } else if (inWater) {
            // === REALISTIC WATER PHYSICS ===
            double waterSpeed = 0.045;
            double accelX = (forwardX * fMove + strafeX * sMove) * waterSpeed;
            double accelZ = (forwardZ * fMove + strafeZ * sMove) * waterSpeed;
            vx += accelX;
            vz += accelZ;

            // Slower sinking gravity in water
            vy -= 0.012;
            if (vy < -0.14) vy = -0.14; // Cap water falling speed

            // Swimming controls
            if (jump) {
                vy += 0.038;
                if (vy > 0.16) vy = 0.16;
            }
            if (sneak) {
                vy -= 0.03;
                if (vy < -0.16) vy = -0.16;
            }

            // High fluid drag
            vx *= 0.82;
            vz *= 0.82;
            vy *= 0.82;

            moveWithCollision(world, vx, vy, vz);

            // Water bobbing
            bobX *= 0.8;
            bobY *= 0.8;
        } else {
            // === AUTHENTIC MINECRAFT GROUND & AIR PHYSICS ===
            double groundSpeed = sprint ? 0.13 : 0.098;
            double airSpeed = 0.02;

            if (onGround) {
                double accelX = (forwardX * fMove + strafeX * sMove) * groundSpeed;
                double accelZ = (forwardZ * fMove + strafeZ * sMove) * groundSpeed;
                vx += accelX;
                vz += accelZ;

                // Jump with standard Minecraft velocity (0.42)
                if (jump) {
                    vy = 0.42;
                    if (sprint) {
                        vx += forwardX * 0.12;
                        vz += forwardZ * 0.12;
                    }
                    SoundManager.playJump();
                    onGround = false;
                }

                // Ground friction
                vx *= 0.58;
                vz *= 0.58;

                // Step sounds and head bobbing
                double horizSpeed = Math.hypot(vx, vz);
                if (horizSpeed > 0.02) {
                    walkDistance += horizSpeed;
                    bobX = Math.sin(walkDistance * 1.6) * 0.035;
                    bobY = Math.abs(Math.cos(walkDistance * 3.2)) * 0.055;

                    stepSoundTimer += horizSpeed;
                    if (stepSoundTimer > 1.4) {
                        SoundManager.playStep();
                        stepSoundTimer = 0.0;
                    }
                } else {
                    bobX *= 0.8;
                    bobY *= 0.8;
                }
            } else {
                // In air: lower acceleration, higher drag
                double accelX = (forwardX * fMove + strafeX * sMove) * airSpeed;
                double accelZ = (forwardZ * fMove + strafeZ * sMove) * airSpeed;
                vx += accelX;
                vz += accelZ;

                // Gravity: authentic Minecraft 0.08
                vy = (vy - 0.08) * 0.98;

                vx *= 0.91;
                vz *= 0.91;

                bobX *= 0.8;
                bobY *= 0.8;
            }

            moveWithCollision(world, vx, vy, vz);
        }

        updateCamera();
    }

    private void moveWithCollision(World world, double dx, double dy, double dz) {
        AABB box = getBoundingBox();
        AABB expanded = new AABB(
            box.minX + Math.min(0, dx), box.minY + Math.min(0, dy), box.minZ + Math.min(0, dz),
            box.maxX + Math.max(0, dx), box.maxY + Math.max(0, dy), box.maxZ + Math.max(0, dz)
        );

        List<AABB> obstacles = world.getCubes(expanded);

        double origDy = dy;
        double origDx = dx;
        double origDz = dz;

        // Clip Y first
        for (AABB o : obstacles) {
            dy = o.clipYCollide(box, dy);
        }
        box = box.move(0, dy, 0);

        // Clip X
        for (AABB o : obstacles) {
            dx = o.clipXCollide(box, dx);
        }
        box = box.move(dx, 0, 0);

        // Clip Z
        for (AABB o : obstacles) {
            dz = o.clipZCollide(box, dz);
        }
        box = box.move(0, 0, dz);

        // Auto-step up (0.5 blocks) if on ground and hit an obstacle
        if (onGround && (dx != origDx || dz != origDz)) {
            double stepY = 0.5;
            AABB stepBox = getBoundingBox();
            AABB stepExpanded = new AABB(
                stepBox.minX + Math.min(0, origDx), stepBox.minY, stepBox.minZ + Math.min(0, origDz),
                stepBox.maxX + Math.max(0, origDx), stepBox.maxY + stepY, stepBox.maxZ + Math.max(0, origDz)
            );
            List<AABB> stepObstacles = world.getCubes(stepExpanded);

            // Move up
            AABB testBox = stepBox.move(0, stepY, 0);
            double sDx = origDx;
            double sDz = origDz;

            for (AABB o : stepObstacles) {
                sDx = o.clipXCollide(testBox, sDx);
            }
            testBox = testBox.move(sDx, 0, 0);

            for (AABB o : stepObstacles) {
                sDz = o.clipZCollide(testBox, sDz);
            }
            testBox = testBox.move(0, 0, sDz);

            // Move back down
            double sDy = -stepY;
            for (AABB o : stepObstacles) {
                sDy = o.clipYCollide(testBox, sDy);
            }
            testBox = testBox.move(0, sDy, 0);

            // If stepped higher and further, adopt it
            if (sDx * sDx + sDz * sDz > dx * dx + dz * dz) {
                box = testBox;
                dx = sDx;
                dz = sDz;
            }
        }

        onGround = (origDy < 0.0 && dy != origDy);

        if (origDy != dy) {
            vy = 0;
        }

        x = (box.minX + box.maxX) / 2.0;
        y = box.minY;
        z = (box.minZ + box.maxZ) / 2.0;
    }

    public void updateCamera() {
        camera.setPosition(x + bobX, y + eyeOffset + bobY, z);
    }
}
