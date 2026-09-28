package com.minj;

import java.io.File;
import javax.imageio.ImageIO;

public class EngineTest {
    public static void main(String[] args) throws Exception {
        World world = new World(12345L);

        // Find a nice high ground spawn spot with no tree directly on top
        int spawnX = 0, spawnZ = 0, spawnY = 0;
        boolean found = false;
        for (int r = 0; r <= 32 && !found; r += 2) {
            for (int dx = -r; dx <= r && !found; dx += 2) {
                for (int dz = -r; dz <= r && !found; dz += 2) {
                    int ty = world.getTopBlockY(dx, dz);
                    if (ty > World.WATER_LEVEL + 2 && world.getBlock(dx, ty, dz) == Block.GRASS.id) {
                        // Check if air above
                        if (world.getBlock(dx, ty + 1, dz) == Block.AIR.id && world.getBlock(dx, ty + 2, dz) == Block.AIR.id) {
                            spawnX = dx;
                            spawnZ = dz;
                            spawnY = ty + 1;
                            found = true;
                        }
                    }
                }
            }
        }

        System.out.printf("Clean grassy spawn found at: %d, %d, %d\n", spawnX, spawnY, spawnZ);
        Player player = new Player(spawnX + 0.5, spawnY, spawnZ + 0.5);
        player.camera.setRotation(0.5, -0.2); // Look slightly down towards landscape
        player.updateCamera();

        Renderer renderer = new Renderer(Main.RENDER_WIDTH, Main.RENDER_HEIGHT);
        renderer.render(world, player, null, 0);

        int nonSkyPixels = 0;
        for (int p : renderer.pixels) {
            int r = (p >> 16) & 0xFF;
            int g = (p >> 8) & 0xFF;
            int b = p & 0xFF;
            if (b < 150 || r > 180 || (g > 90 && r < 120)) {
                nonSkyPixels++;
            }
        }
        System.out.println("Non-sky block pixels rendered: " + nonSkyPixels);

        ImageIO.write(renderer.getBuffer(), "PNG", new File("screenshot.png"));
        System.out.println("Screenshot written to screenshot.png!");
    }
}
