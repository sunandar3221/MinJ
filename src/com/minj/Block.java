package com.minj;

public enum Block {
    AIR(0, "Air", false, true, 0, 0, 0),
    GRASS(1, "Grass Block", true, false, 0, 2, 1),
    DIRT(2, "Dirt", true, false, 2, 2, 2),
    STONE(3, "Stone", true, false, 3, 3, 3),
    COBBLESTONE(4, "Cobblestone", true, false, 4, 4, 4),
    WOOD_PLANKS(5, "Oak Planks", true, false, 5, 5, 5),
    LOG(6, "Oak Log", true, false, 6, 6, 7),
    LEAVES(7, "Oak Leaves", true, true, 8, 8, 8),
    GLASS(8, "Glass", true, true, 9, 9, 9),
    SAND(9, "Sand", true, false, 10, 10, 10),
    BRICK(10, "Bricks", true, false, 11, 11, 11),
    BEDROCK(11, "Bedrock", true, false, 12, 12, 12),
    WATER(12, "Water", false, true, 13, 13, 13),
    TNT(13, "TNT", true, false, 14, 15, 16),
    GOLD_ORE(14, "Gold Ore", true, false, 17, 17, 17),
    IRON_ORE(15, "Iron Ore", true, false, 18, 18, 18),
    COAL_ORE(16, "Coal Ore", true, false, 19, 19, 19),
    BOOKSHELF(17, "Bookshelf", true, false, 5, 5, 20);

    public final byte id;
    public final String name;
    public final boolean isSolid;
    public final boolean isTransparent;

    // Texture IDs for 6 faces: 0=TOP, 1=BOTTOM, 2=NORTH(-Z), 3=SOUTH(+Z), 4=WEST(-X), 5=EAST(+X)
    public final int[] textureIndices;

    Block(int id, String name, boolean isSolid, boolean isTransparent, int topTex, int bottomTex, int sideTex) {
        this.id = (byte) id;
        this.name = name;
        this.isSolid = isSolid;
        this.isTransparent = isTransparent;
        this.textureIndices = new int[6];
        this.textureIndices[0] = topTex;
        this.textureIndices[1] = bottomTex;
        for (int i = 2; i < 6; i++) {
            this.textureIndices[i] = sideTex;
        }
    }

    private static final Block[] LOOKUP = new Block[256];
    static {
        for (Block b : values()) {
            LOOKUP[b.id & 0xFF] = b;
        }
    }

    public static Block getById(byte id) {
        Block b = LOOKUP[id & 0xFF];
        return b == null ? AIR : b;
    }
}
