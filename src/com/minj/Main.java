package com.minj;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferStrategy;
import javax.swing.JFrame;

public class Main implements Runnable {
    public static final String TITLE = "MinJ - Minecraft Java Edition";
    public static final int WINDOW_WIDTH = 1024;
    public static final int WINDOW_HEIGHT = 640;

    // Internal rendering resolution (crisp retro pixel aesthetic)
    public static final int RENDER_WIDTH = 512;
    public static final int RENDER_HEIGHT = 320;

    private JFrame frame;
    private Canvas canvas;
    private boolean running = false;
    private Thread gameThread;

    private static World world;
    private static Player player;
    private static Renderer renderer;
    private static InputHandler input;

    private int currentFps = 0;
    private Raycast.HitResult currentHit = null;

    public Main() {
        initWindow();
        initGame();
    }

    private void initWindow() {
        frame = new JFrame(TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(true);

        canvas = new Canvas();
        canvas.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        canvas.setFocusable(true);

        frame.add(canvas);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        canvas.createBufferStrategy(3);
    }

    private void initGame() {
        input = new InputHandler(canvas);
        world = new World(12345L);

        // Search for a suitable dry grassy hill with clear open air above for spawn
        int spawnX = 0, spawnZ = 0, spawnY = 0;
        boolean found = false;
        for (int r = 0; r <= 32 && !found; r += 2) {
            for (int dx = -r; dx <= r && !found; dx += 2) {
                for (int dz = -r; dz <= r && !found; dz += 2) {
                    int ty = world.getTopBlockY(dx, dz);
                    if (ty > World.WATER_LEVEL + 2 && world.getBlock(dx, ty, dz) == Block.GRASS.id) {
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
        if (!found) {
            spawnY = world.getTopBlockY(spawnX, spawnZ) + 1;
        }

        player = new Player(spawnX + 0.5, spawnY, spawnZ + 0.5);
        player.camera.setRotation(0.5, -0.1);

        renderer = new Renderer(RENDER_WIDTH, RENDER_HEIGHT);
    }

    public static void onMouseWheel(int notches) {
        if (player == null) return;
        player.selectedSlot = (player.selectedSlot + notches) % 9;
        if (player.selectedSlot < 0) {
            player.selectedSlot += 9;
        }
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        gameThread = new Thread(this, "GameLoop");
        gameThread.start();
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double nsPerTick = 1_000_000_000.0 / 60.0;
        double delta = 0;

        long lastFpsTimer = System.currentTimeMillis();
        int frames = 0;
        long gameTick = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerTick;
            lastTime = now;

            while (delta >= 1.0) {
                tick(gameTick++);
                delta -= 1.0;
            }

            render(gameTick);
            frames++;

            if (System.currentTimeMillis() - lastFpsTimer >= 1000) {
                currentFps = frames;
                frames = 0;
                lastFpsTimer += 1000;
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException ignored) {}
        }
    }

    private void tick(long tick) {
        input.update(player);

        if (input.toggleFlyRequested) {
            player.isFlying = !player.isFlying;
            input.toggleFlyRequested = false;
        }

        if (input.mouseCaptured) {
            input.handleMouseMotion(player.camera);
        }

        player.update(world, input.forward, input.backward, input.left, input.right,
                      input.jump, input.sneak, input.sprint);

        // Raycasting for block selection
        double fx = player.camera.getForwardX();
        double fy = player.camera.getForwardY();
        double fz = player.camera.getForwardZ();

        currentHit = Raycast.cast(world, player.camera.x, player.camera.y, player.camera.z,
                                  fx, fy, fz, 5.2);

        // Handle block destruction (Left Click)
        if (input.leftClick) {
            if (currentHit.hit) {
                world.setBlock(currentHit.x, currentHit.y, currentHit.z, Block.AIR.id);
                SoundManager.playBlockBreak();
                renderer.swingProgress = 1.0f;
            }
            input.leftClick = false;
        }

        // Handle block placement (Right Click)
        if (input.rightClick) {
            if (currentHit.hit) {
                int px = currentHit.x + currentHit.faceX;
                int py = currentHit.y + currentHit.faceY;
                int pz = currentHit.z + currentHit.faceZ;

                // Don't place inside player body
                AABB placeBox = new AABB(px, py, pz, px + 1, py + 1, pz + 1);
                if (!player.getBoundingBox().intersects(placeBox)) {
                    Block toPlace = player.getSelectedBlock();
                    if (toPlace != Block.AIR) {
                        world.setBlock(px, py, pz, toPlace.id);
                        SoundManager.playBlockPlace();
                        renderer.swingProgress = 1.0f;
                    }
                }
            }
            input.rightClick = false;
        }
    }

    private void render(long tick) {
        BufferStrategy bs = canvas.getBufferStrategy();
        if (bs == null) {
            canvas.createBufferStrategy(3);
            return;
        }

        renderer.render(world, player, currentHit, tick);

        Graphics2D g = (Graphics2D) bs.getDrawGraphics();
        int canvasW = canvas.getWidth();
        int canvasH = canvas.getHeight();

        // Draw 3D scene scaled up with nearest neighbor interpolation for sharp pixels
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(renderer.getBuffer(), 0, 0, canvasW, canvasH, null);

        // Enable antialiasing for text & HUD
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        renderer.drawHUD(g, canvasW, canvasH, player, currentFps, currentHit);

        // Pause / Start Overlay when mouse not captured
        if (!input.mouseCaptured) {
            drawPauseOverlay(g, canvasW, canvasH);
        }

        g.dispose();
        bs.show();
    }

    private void drawPauseOverlay(Graphics2D g, int w, int h) {
        g.setColor(new Color(0, 0, 0, 185));
        g.fillRect(0, 0, w, h);

        g.setFont(new Font("SansSerif", Font.BOLD, 32));
        g.setColor(Color.WHITE);
        String title = "MINJ - MINECRAFT JAVA";
        int tw = g.getFontMetrics().stringWidth(title);
        g.drawString(title, (w - tw) / 2, h / 2 - 80);

        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(Color.YELLOW);
        String prompt = ">> KLIK LAYAR UNTUK MULAI BERMAIN <<";
        int pw = g.getFontMetrics().stringWidth(prompt);
        g.drawString(prompt, (w - pw) / 2, h / 2 - 30);

        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(Color.LIGHT_GRAY);
        String[] controls = {
            "W, A, S, D : Berjalan / Bergerak",
            "SPASI : Melompat (atau Terbang ke atas)",
            "SHIFT : Jongkok (atau Terbang ke bawah)",
            "KLIK KIRI : Menghancurkan Balok (Break Block)",
            "KLIK KANAN : Menaruh Balok (Place Block)",
            "1 - 9 / Scroll Mouse : Memilih Balok di Hotbar",
            "F : Toggle Terbang (Creative Flight Mode)",
            "ESC : Lepaskan Kursor Mouse"
        };

        int startY = h / 2 + 20;
        for (int i = 0; i < controls.length; i++) {
            int cw = g.getFontMetrics().stringWidth(controls[i]);
            g.drawString(controls[i], (w - cw) / 2, startY + i * 22);
        }
    }

    public static void main(String[] args) {
        Main game = new Main();
        game.start();
    }
}
