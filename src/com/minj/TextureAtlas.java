package com.minj;

import java.util.Random;

public class TextureAtlas {
    public static final int TEXTURE_SIZE = 16;
    public static final int NUM_TEXTURES = 21;
    public static final int[][] TEXTURES = new int[NUM_TEXTURES][TEXTURE_SIZE * TEXTURE_SIZE];

    static {
        generateAllTextures();
    }

    private static void generateAllTextures() {
        Random rng = new Random(1337);

        // 0: Grass Top
        int[] grassTop = TEXTURES[0];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int r = 85 + rng.nextInt(25);
                int g = 155 + rng.nextInt(35);
                int b = 45 + rng.nextInt(20);
                grassTop[y * 16 + x] = (r << 16) | (g << 8) | b;
            }
        }

        // 2: Dirt
        int[] dirt = TEXTURES[2];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int base = 95 + rng.nextInt(30);
                int r = (int)(base * 1.05);
                int g = (int)(base * 0.72);
                int b = (int)(base * 0.45);
                dirt[y * 16 + x] = (r << 16) | (g << 8) | b;
            }
        }

        // 1: Grass Side
        int[] grassSide = TEXTURES[1];
        System.arraycopy(dirt, 0, grassSide, 0, 256);
        for (int x = 0; x < 16; x++) {
            int overhang = 2 + (x % 2 == 0 ? rng.nextInt(3) : 1);
            for (int y = 0; y < overhang; y++) {
                int r = 80 + rng.nextInt(25);
                int g = 145 + rng.nextInt(35);
                int b = 40 + rng.nextInt(20);
                grassSide[y * 16 + x] = (r << 16) | (g << 8) | b;
            }
        }

        // 3: Stone
        int[] stone = TEXTURES[3];
        for (int i = 0; i < 256; i++) {
            int v = 110 + rng.nextInt(40);
            stone[i] = (v << 16) | (v << 8) | v;
        }

        // 4: Cobblestone
        int[] cobble = TEXTURES[4];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                boolean border = (x % 4 == 0 && y % 4 != 0) || (y % 4 == 0) || rng.nextInt(12) == 0;
                int v = border ? 70 + rng.nextInt(25) : 120 + rng.nextInt(35);
                cobble[y * 16 + x] = (v << 16) | (v << 8) | v;
            }
        }

        // 5: Wood Planks
        int[] planks = TEXTURES[5];
        for (int y = 0; y < 16; y++) {
            boolean seam = (y % 4 == 0);
            for (int x = 0; x < 16; x++) {
                int plankCol = (x / 8 + y / 4) % 2;
                boolean nail = (y % 4 == 2) && (x % 8 == 2 || x % 8 == 6);
                int r, g, b;
                if (seam) {
                    r = 100; g = 75; b = 45;
                } else if (nail) {
                    r = 80; g = 60; b = 35;
                } else {
                    int base = 150 + plankCol * 15 + rng.nextInt(15);
                    r = base;
                    g = (int)(base * 0.75);
                    b = (int)(base * 0.45);
                }
                planks[y * 16 + x] = (r << 16) | (g << 8) | b;
            }
        }

        // 6: Log Top
        int[] logTop = TEXTURES[6];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double dist = Math.hypot(x - 7.5, y - 7.5);
                int r, g, b;
                if (dist > 7.0) { // Bark border
                    r = 85 + rng.nextInt(15);
                    g = 65 + rng.nextInt(12);
                    b = 40 + rng.nextInt(10);
                } else { // Rings
                    int ring = (int)(dist * 1.5) % 2;
                    int base = 160 + ring * 20 + rng.nextInt(10);
                    r = base;
                    g = (int)(base * 0.82);
                    b = (int)(base * 0.55);
                }
                logTop[y * 16 + x] = (r << 16) | (g << 8) | b;
            }
        }

        // 7: Log Side
        int[] logSide = TEXTURES[7];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int bark = 80 + ((x * 3) % 7) * 4 + rng.nextInt(15);
                int r = bark + 10;
                int g = (int)(bark * 0.75);
                int b = (int)(bark * 0.48);
                logSide[y * 16 + x] = (r << 16) | (g << 8) | b;
            }
        }

        // 8: Leaves
        int[] leaves = TEXTURES[8];
        for (int i = 0; i < 256; i++) {
            if (rng.nextInt(10) == 0) {
                leaves[i] = 0x00000000; // Transparent cutout
            } else {
                int g = 100 + rng.nextInt(60);
                int r = 35 + rng.nextInt(25);
                int b = 25 + rng.nextInt(20);
                leaves[i] = (0xFF << 24) | (r << 16) | (g << 8) | b;
            }
        }

        // 9: Glass
        int[] glass = TEXTURES[9];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                boolean border = (x == 0 || x == 15 || y == 0 || y == 15);
                boolean glint = (x == y && x >= 3 && x <= 6) || (x == y - 4 && x >= 8 && x <= 11);
                if (border) {
                    glass[y * 16 + x] = (0xCC << 24) | 0xCCDDEE;
                } else if (glint) {
                    glass[y * 16 + x] = (0x99 << 24) | 0xFFFFFF;
                } else {
                    glass[y * 16 + x] = (0x22 << 24) | 0xAACCFF; // Semi-transparent glass body
                }
            }
        }

        // 10: Sand
        int[] sand = TEXTURES[10];
        for (int i = 0; i < 256; i++) {
            int base = 205 + rng.nextInt(25);
            int r = base;
            int g = (int)(base * 0.92);
            int b = (int)(base * 0.68);
            sand[i] = (r << 16) | (g << 8) | b;
        }

        // 11: Bricks
        int[] brick = TEXTURES[11];
        for (int y = 0; y < 16; y++) {
            int row = y / 4;
            boolean mortarY = (y % 4 == 0);
            for (int x = 0; x < 16; x++) {
                int shift = (row % 2 == 0) ? 0 : 4;
                boolean mortarX = ((x + shift) % 8 == 0);
                if (mortarY || mortarX) {
                    brick[y * 16 + x] = 0xCCCCCC;
                } else {
                    int r = 160 + rng.nextInt(35);
                    int g = 65 + rng.nextInt(20);
                    int b = 55 + rng.nextInt(20);
                    brick[y * 16 + x] = (r << 16) | (g << 8) | b;
                }
            }
        }

        // 12: Bedrock
        int[] bedrock = TEXTURES[12];
        for (int i = 0; i < 256; i++) {
            int v = rng.nextInt(80);
            bedrock[i] = (v << 16) | (v << 8) | v;
        }

        // 13: Water
        int[] water = TEXTURES[13];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int r = 30 + rng.nextInt(15);
                int g = 80 + rng.nextInt(30);
                int b = 210 + rng.nextInt(35);
                water[y * 16 + x] = (0x99 << 24) | (r << 16) | (g << 8) | b;
            }
        }

        // 14: TNT Top
        int[] tntTop = TEXTURES[14];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double d = Math.hypot(x - 7.5, y - 7.5);
                if (d < 3.0) {
                    tntTop[y * 16 + x] = 0x444444; // primer fuse
                } else if (d < 5.0) {
                    tntTop[y * 16 + x] = 0xEEEEEE;
                } else {
                    int r = 180 + rng.nextInt(30);
                    tntTop[y * 16 + x] = (r << 16) | (35 << 8) | 30;
                }
            }
        }

        // 15: TNT Bottom
        int[] tntBottom = TEXTURES[15];
        for (int i = 0; i < 256; i++) {
            int r = 160 + rng.nextInt(30);
            tntBottom[i] = (r << 16) | (30 << 8) | 25;
        }

        // 16: TNT Side with "TNT" text
        int[] tntSide = TEXTURES[16];
        // 5x5 font for 'T', 'N', 'T'
        boolean[][] tntPattern = {
            // T   N   T
            {true,true,true, false, true,false,false,true, false, true,true,true},
            {false,true,false, false, true,true,false,true, false, false,true,false},
            {false,true,false, false, true,false,true,true, false, false,true,false},
            {false,true,false, false, true,false,false,true, false, false,true,false},
            {false,true,false, false, true,false,false,true, false, false,true,false}
        };
        for (int y = 0; y < 16; y++) {
            boolean inStripe = (y >= 5 && y <= 11);
            for (int x = 0; x < 16; x++) {
                if (inStripe) {
                    int py = y - 6;
                    int px = x - 2;
                    boolean textPixel = false;
                    if (py >= 0 && py < 5 && px >= 0 && px < 12) {
                        textPixel = tntPattern[py][px];
                    }
                    tntSide[y * 16 + x] = textPixel ? 0x000000 : 0xFFFFFF;
                } else {
                    int r = 190 + rng.nextInt(25);
                    tntSide[y * 16 + x] = (r << 16) | (35 << 8) | 30;
                }
            }
        }

        // 17: Gold Ore
        int[] gold = TEXTURES[17];
        System.arraycopy(stone, 0, gold, 0, 256);
        addOreSpots(gold, 0xEFC842, 0xFBE87A, rng);

        // 18: Iron Ore
        int[] iron = TEXTURES[18];
        System.arraycopy(stone, 0, iron, 0, 256);
        addOreSpots(iron, 0xCEAA8C, 0xE4C7AB, rng);

        // 19: Coal Ore
        int[] coal = TEXTURES[19];
        System.arraycopy(stone, 0, coal, 0, 256);
        addOreSpots(coal, 0x1E1E1E, 0x333333, rng);

        // 20: Bookshelf
        int[] book = TEXTURES[20];
        System.arraycopy(planks, 0, book, 0, 256);
        int[] bookColors = {0xAA2222, 0x2255AA, 0x228833, 0xAA6622, 0x882288};
        for (int shelf = 0; shelf < 2; shelf++) {
            int sy = shelf * 7 + 2;
            for (int bx = 2; bx < 14; bx++) {
                int col = bookColors[(bx + shelf * 3) % bookColors.length];
                for (int by = sy; by < sy + 5; by++) {
                    book[by * 16 + bx] = (by == sy + 4) ? 0xDDDDDD : col;
                }
            }
        }
    }

    private static void addOreSpots(int[] baseTexture, int color1, int color2, Random rng) {
        for (int spot = 0; spot < 6; spot++) {
            int ox = 2 + rng.nextInt(12);
            int oy = 2 + rng.nextInt(12);
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (rng.nextBoolean()) {
                        int idx = (oy + dy) * 16 + (ox + dx);
                        if (idx >= 0 && idx < 256) {
                            baseTexture[idx] = rng.nextBoolean() ? color1 : color2;
                        }
                    }
                }
            }
        }
    }

    public static int getPixel(int textureIndex, int u, int v) {
        if (textureIndex < 0 || textureIndex >= NUM_TEXTURES) return 0xFF00FF;
        int clampedU = (u & 15);
        int clampedV = (v & 15);
        return TEXTURES[textureIndex][clampedV * 16 + clampedU];
    }
}
