package com.minj;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.List;

public class Renderer {
    public final int width;
    public final int height;
    private final float centerX;
    private final float centerY;
    private final float fovScale;

    private final BufferedImage screenBuffer;
    public final int[] pixels;
    private final float[] depthBuffer;

    private static final int SKY_TOP = 0x5599FF;
    private static final int SKY_HORIZON = 0xA0CCFF;
    private static final int UNDERWATER_SKY_TOP = 0x061830;
    private static final int UNDERWATER_SKY_HORIZON = 0x0C2C55;
    private static final int UNDERWATER_FOG = 0x0B2A4E;
    private static final float NEAR_PLANE = 0.15f;
    private static final float FOG_START = 35.0f;
    private static final float FOG_END = 65.0f;

    private boolean isUnderwater = false;

    // Temporary clipping buffers
    private final float[] clipX = new float[12];
    private final float[] clipY = new float[12];
    private final float[] clipZ = new float[12];
    private final float[] clipU = new float[12];
    private final float[] clipV = new float[12];
    private final float[] clipL = new float[12];

    private final float[] polyX = new float[12];
    private final float[] polyY = new float[12];
    private final float[] polyZ = new float[12];
    private final float[] polyU = new float[12];
    private final float[] polyV = new float[12];
    private final float[] polyL = new float[12];

    // Held block swing animation
    public float swingProgress = 0.0f;

    public Renderer(int width, int height) {
        this.width = width;
        this.height = height;
        this.centerX = width / 2.0f;
        this.centerY = height / 2.0f;

        // 75 degree FOV
        float fovRad = (float) Math.toRadians(75.0);
        this.fovScale = (width / 2.0f) / (float) Math.tan(fovRad / 2.0);

        this.screenBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.pixels = ((DataBufferInt) screenBuffer.getRaster().getDataBuffer()).getData();
        this.depthBuffer = new float[width * height];
    }

    public BufferedImage getBuffer() {
        return screenBuffer;
    }

    public void render(World world, Player player, Raycast.HitResult targetHit, long gameTick) {
        isUnderwater = player.checkHeadUnderwater(world);
        clearBuffers();
        renderSkyAndClouds(player.camera, gameTick);

        Camera cam = player.camera;
        List<Chunk> chunks = world.getLoadedChunks();

        // 1. Render solid voxel faces
        for (Chunk chunk : chunks) {
            if (isChunkInView(chunk, cam)) {
                for (FaceQuad quad : chunk.getSolidMesh()) {
                    renderQuad(quad, cam);
                }
            }
        }

        // 2. Render transparent faces (leaves, glass, water)
        for (Chunk chunk : chunks) {
            if (isChunkInView(chunk, cam)) {
                for (FaceQuad quad : chunk.getTransparentMesh()) {
                    renderQuad(quad, cam);
                }
            }
        }

        // 3. Underwater screen tint & ripple
        if (isUnderwater) {
            applyUnderwaterOverlay(gameTick);
        }

        // 4. Render target block wireframe highlight
        if (targetHit != null && targetHit.hit) {
            renderBlockOutline(targetHit.x, targetHit.y, targetHit.z, cam);
        }

        // 5. Render held item in player hand
        renderHeldItem(player.getSelectedBlock(), cam);
    }

    private void clearBuffers() {
        Arrays.fill(depthBuffer, 0.0f); // 0.0 means 1/z is 0 (infinitely far away)
    }

    private void renderSkyAndClouds(Camera cam, long tick) {
        int skyTop = isUnderwater ? UNDERWATER_SKY_TOP : SKY_TOP;
        int skyHorizon = isUnderwater ? UNDERWATER_SKY_HORIZON : SKY_HORIZON;

        // Horizon pitch offset
        float pitchOffset = (float) (Math.tan(cam.pitch) * fovScale);

        for (int y = 0; y < height; y++) {
            float gradient = (y - centerY + pitchOffset) / (float) height + 0.5f;
            gradient = Math.max(0.0f, Math.min(1.0f, gradient));

            int r = (int) (((skyTop >> 16) & 0xFF) * (1.0f - gradient) + ((skyHorizon >> 16) & 0xFF) * gradient);
            int g = (int) (((skyTop >> 8) & 0xFF) * (1.0f - gradient) + ((skyHorizon >> 8) & 0xFF) * gradient);
            int b = (int) ((skyTop & 0xFF) * (1.0f - gradient) + (skyHorizon & 0xFF) * gradient);
            int color = (r << 16) | (g << 8) | b;

            int rowStart = y * width;
            for (int x = 0; x < width; x++) {
                pixels[rowStart + x] = color;
            }
        }

        if (!isUnderwater) {
            renderSun(cam);
        }
    }

    private void renderSun(Camera cam) {
        // Sun fixed position in sky (e.g. forward = 0, up = 1, z = 0.5)
        float wx = 0.0f, wy = 80.0f, wz = 100.0f;
        float[] c = new float[3];
        cam.transformToCameraSpace(cam.x + wx, cam.y + wy, cam.z + wz, c);
        if (c[2] > NEAR_PLANE) {
            float sx = centerX + (c[0] / c[2]) * fovScale;
            float sy = centerY - (c[1] / c[2]) * fovScale;
            int sunSize = (int)(24.0f * (fovScale / 300.0f));
            int minX = Math.max(0, (int) sx - sunSize);
            int maxX = Math.min(width - 1, (int) sx + sunSize);
            int minY = Math.max(0, (int) sy - sunSize);
            int maxY = Math.min(height - 1, (int) sy + sunSize);

            for (int py = minY; py <= maxY; py++) {
                for (int px = minX; px <= maxX; px++) {
                    pixels[py * width + px] = 0xFFFFAA;
                }
            }
        }
    }

    private boolean isChunkInView(Chunk chunk, Camera cam) {
        // Fast chunk bounding sphere / distance test
        double midX = chunk.worldStartX + Chunk.SIZE_X / 2.0;
        double midY = Chunk.SIZE_Y / 2.0;
        double midZ = chunk.worldStartZ + Chunk.SIZE_Z / 2.0;

        double dx = midX - cam.x;
        double dy = midY - cam.y;
        double dz = midZ - cam.z;
        double distSq = dx * dx + dy * dy + dz * dz;

        // Skip chunks further than 90 blocks
        return distSq < 90.0 * 90.0;
    }

    private void renderQuad(FaceQuad q, Camera cam) {
        // Fast exact voxel face culling based on camera position relative to face plane
        switch (q.faceDirection) {
            case 0: // TOP (+Y at blockY + 1.0)
                if (cam.y <= q.blockY + 1.0) return;
                break;
            case 1: // BOTTOM (-Y at blockY)
                if (cam.y >= q.blockY) return;
                break;
            case 2: // NORTH (-Z at blockZ)
                if (cam.z >= q.blockZ) return;
                break;
            case 3: // SOUTH (+Z at blockZ + 1.0)
                if (cam.z <= q.blockZ + 1.0) return;
                break;
            case 4: // WEST (-X at blockX)
                if (cam.x >= q.blockX) return;
                break;
            case 5: // EAST (+X at blockX + 1.0)
                if (cam.x <= q.blockX + 1.0) return;
                break;
        }

        // Transform 4 vertices to camera space
        float[] c0 = new float[3], c1 = new float[3], c2 = new float[3], c3 = new float[3];
        cam.transformToCameraSpace(q.x[0], q.y[0], q.z[0], c0);
        cam.transformToCameraSpace(q.x[1], q.y[1], q.z[1], c1);
        cam.transformToCameraSpace(q.x[2], q.y[2], q.z[2], c2);
        cam.transformToCameraSpace(q.x[3], q.y[3], q.z[3], c3);

        // Near-plane clipping against z = NEAR_PLANE
        polyX[0] = c0[0]; polyY[0] = c0[1]; polyZ[0] = c0[2]; polyU[0] = q.u[0]; polyV[0] = q.v[0]; polyL[0] = q.light[0];
        polyX[1] = c1[0]; polyY[1] = c1[1]; polyZ[1] = c1[2]; polyU[1] = q.u[1]; polyV[1] = q.v[1]; polyL[1] = q.light[1];
        polyX[2] = c2[0]; polyY[2] = c2[1]; polyZ[2] = c2[2]; polyU[2] = q.u[2]; polyV[2] = q.v[2]; polyL[2] = q.light[2];
        polyX[3] = c3[0]; polyY[3] = c3[1]; polyZ[3] = c3[2]; polyU[3] = q.u[3]; polyV[3] = q.v[3]; polyL[3] = q.light[3];

        int vertexCount = clipPolygonNear(4);
        if (vertexCount < 3) return;

        // Project clipped polygon vertices to screen space
        for (int i = 0; i < vertexCount; i++) {
            float z = polyZ[i];
            float invZ = 1.0f / z;
            polyX[i] = centerX + (polyX[i] * invZ) * fovScale;
            polyY[i] = centerY - (polyY[i] * invZ) * fovScale;
            // Store 1/z for perspective-correct interpolation
            polyZ[i] = invZ;
            polyU[i] = polyU[i] * invZ;
            polyV[i] = polyV[i] * invZ;
            polyL[i] = polyL[i] * invZ;
        }

        // Triangulate convex polygon as fan from vertex 0
        for (int i = 1; i < vertexCount - 1; i++) {
            rasterizeTriangle(
                polyX[0], polyY[0], polyZ[0], polyU[0], polyV[0], polyL[0],
                polyX[i], polyY[i], polyZ[i], polyU[i], polyV[i], polyL[i],
                polyX[i + 1], polyY[i + 1], polyZ[i + 1], polyU[i + 1], polyV[i + 1], polyL[i + 1],
                q.textureId
            );
        }
    }

    private int clipPolygonNear(int n) {
        int outCount = 0;
        for (int i = 0; i < n; i++) {
            int next = (i + 1) % n;
            float z1 = polyZ[i];
            float z2 = polyZ[next];

            boolean in1 = z1 >= NEAR_PLANE;
            boolean in2 = z2 >= NEAR_PLANE;

            if (in1) {
                clipX[outCount] = polyX[i];
                clipY[outCount] = polyY[i];
                clipZ[outCount] = polyZ[i];
                clipU[outCount] = polyU[i];
                clipV[outCount] = polyV[i];
                clipL[outCount] = polyL[i];
                outCount++;
            }

            if (in1 != in2) {
                float t = (NEAR_PLANE - z1) / (z2 - z1);
                clipX[outCount] = polyX[i] + t * (polyX[next] - polyX[i]);
                clipY[outCount] = polyY[i] + t * (polyY[next] - polyY[i]);
                clipZ[outCount] = NEAR_PLANE;
                clipU[outCount] = polyU[i] + t * (polyU[next] - polyU[i]);
                clipV[outCount] = polyV[i] + t * (polyV[next] - polyV[i]);
                clipL[outCount] = polyL[i] + t * (polyL[next] - polyL[i]);
                outCount++;
            }
        }

        System.arraycopy(clipX, 0, polyX, 0, outCount);
        System.arraycopy(clipY, 0, polyY, 0, outCount);
        System.arraycopy(clipZ, 0, polyZ, 0, outCount);
        System.arraycopy(clipU, 0, polyU, 0, outCount);
        System.arraycopy(clipV, 0, polyV, 0, outCount);
        System.arraycopy(clipL, 0, polyL, 0, outCount);

        return outCount;
    }

    private void rasterizeTriangle(
            float x0, float y0, float iz0, float u0, float v0, float l0,
            float x1, float y1, float iz1, float u1, float v1, float l1,
            float x2, float y2, float iz2, float u2, float v2, float l2,
            int textureId) {

        // Edge vectors
        float area = (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0);
        if (Math.abs(area) < 0.001f) return;

        // If clockwise (negative area), swap vertices 1 and 2 to make counter-clockwise
        if (area < 0) {
            float tx = x1; x1 = x2; x2 = tx;
            float ty = y1; y1 = y2; y2 = ty;
            float tiz = iz1; iz1 = iz2; iz2 = tiz;
            float tu = u1; u1 = u2; u2 = tu;
            float tv = v1; v1 = v2; v2 = tv;
            float tl = l1; l1 = l2; l2 = tl;
            area = -area;
        }

        float invArea = 1.0f / area;

        // Bounding box clamped to screen
        int minX = Math.max(0, (int) Math.floor(Math.min(x0, Math.min(x1, x2))));
        int maxX = Math.min(width - 1, (int) Math.ceil(Math.max(x0, Math.max(x1, x2))));
        int minY = Math.max(0, (int) Math.floor(Math.min(y0, Math.min(y1, y2))));
        int maxY = Math.min(height - 1, (int) Math.ceil(Math.max(y0, Math.max(y1, y2))));

        if (minX > maxX || minY > maxY) return;

        int[] texture = TextureAtlas.TEXTURES[textureId];

        for (int y = minY; y <= maxY; y++) {
            float py = y + 0.5f;
            int pixelRow = y * width;

            for (int x = minX; x <= maxX; x++) {
                float px = x + 0.5f;

                // Positive barycentric weights
                float w0 = (py - y1) * (x2 - x1) - (px - x1) * (y2 - y1);
                float w1 = (py - y2) * (x0 - x2) - (px - x2) * (y0 - y2);
                float w2 = (py - y0) * (x1 - x0) - (px - x0) * (y1 - y0);

                if (w0 < 0 || w1 < 0 || w2 < 0) continue;

                w0 *= invArea;
                w1 *= invArea;
                w2 *= invArea;

                float invZ = w0 * iz0 + w1 * iz1 + w2 * iz2;
                int pIdx = pixelRow + x;

                if (invZ > depthBuffer[pIdx]) {
                    float z = 1.0f / invZ;

                    float u = (w0 * u0 + w1 * u1 + w2 * u2) * z;
                    float v = (w0 * v0 + w1 * v1 + w2 * v2) * z;
                    float light = (w0 * l0 + w1 * l1 + w2 * l2) * z;

                    int texU = ((int)(u * 16.0f)) & 15;
                    int texV = ((int)(v * 16.0f)) & 15;

                    int texColor = texture[texV * 16 + texU];
                    // Leaves (textureId 8) cutout: skip fully transparent leaf pixels
                    if (textureId == 8 && (texColor == 0 || ((texColor >> 24) & 0xFF) == 0)) {
                        continue;
                    }

                    // Water (13) and Glass (9) are semi-transparent; all other blocks are 100% solid opaque
                    int alpha = (textureId == 13) ? 175 : ((textureId == 9) ? 150 : 255);

                    // Apply directional + ambient occlusion lighting
                    int r = (int) (((texColor >> 16) & 0xFF) * light);
                    int g = (int) (((texColor >> 8) & 0xFF) * light);
                    int b = (int) ((texColor & 0xFF) * light);

                    // Dynamic fog
                    float fogStart = isUnderwater ? 2.0f : FOG_START;
                    float fogEnd = isUnderwater ? 18.0f : FOG_END;
                    int curFogColor = isUnderwater ? UNDERWATER_FOG : SKY_HORIZON;

                    if (z > fogStart) {
                        float fog = (z - fogStart) / (fogEnd - fogStart);
                        if (fog > 1.0f) fog = 1.0f;
                        int fogR = (curFogColor >> 16) & 0xFF;
                        int fogG = (curFogColor >> 8) & 0xFF;
                        int fogB = curFogColor & 0xFF;

                        r = (int) (r * (1.0f - fog) + fogR * fog);
                        g = (int) (g * (1.0f - fog) + fogG * fog);
                        b = (int) (b * (1.0f - fog) + fogB * fog);
                    }

                    r = Math.min(255, Math.max(0, r));
                    g = Math.min(255, Math.max(0, g));
                    b = Math.min(255, Math.max(0, b));

                    if (alpha < 255) {
                        int prevColor = pixels[pIdx];
                        int prevR = (prevColor >> 16) & 0xFF;
                        int prevG = (prevColor >> 8) & 0xFF;
                        int prevB = prevColor & 0xFF;

                        float a = alpha / 255.0f;
                        r = (int) (r * a + prevR * (1.0f - a));
                        g = (int) (g * a + prevG * (1.0f - a));
                        b = (int) (b * a + prevB * (1.0f - a));
                        depthBuffer[pIdx] = invZ;
                    } else {
                        depthBuffer[pIdx] = invZ;
                    }

                    pixels[pIdx] = (r << 16) | (g << 8) | b;
                }
            }
        }
    }

    private void applyUnderwaterOverlay(long tick) {
        float waveTime = tick * 0.08f;
        for (int y = 0; y < height; y++) {
            int row = y * width;
            float wave = (float) Math.sin(y * 0.12f + waveTime) * 0.04f;
            for (int x = 0; x < width; x++) {
                int p = pixels[row + x];
                int r = (p >> 16) & 0xFF;
                int g = (p >> 8) & 0xFF;
                int b = p & 0xFF;

                float tint = 0.32f + wave;
                r = (int) (r * (1.0f - tint) + 10 * tint);
                g = (int) (g * (1.0f - tint) + 60 * tint);
                b = (int) (b * (1.0f - tint) + 135 * tint);

                pixels[row + x] = (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
            }
        }
    }

    private void renderBlockOutline(int bx, int by, int bz, Camera cam) {
        float x0 = bx - 0.002f, x1 = bx + 1.002f;
        float y0 = by - 0.002f, y1 = by + 1.002f;
        float z0 = bz - 0.002f, z1 = bz + 1.002f;

        float[][] corners = {
            {x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0},
            {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}
        };

        float[][] proj = new float[8][3];
        boolean[] valid = new boolean[8];

        for (int i = 0; i < 8; i++) {
            float[] c = new float[3];
            cam.transformToCameraSpace(corners[i][0], corners[i][1], corners[i][2], c);
            if (c[2] > NEAR_PLANE) {
                proj[i][0] = centerX + (c[0] / c[2]) * fovScale;
                proj[i][1] = centerY - (c[1] / c[2]) * fovScale;
                proj[i][2] = c[2];
                valid[i] = true;
            }
        }

        int[][] edges = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0}, // Front face
            {4, 5}, {5, 6}, {6, 7}, {7, 4}, // Back face
            {0, 4}, {1, 5}, {2, 6}, {3, 7}  // Connecting edges
        };

        for (int[] e : edges) {
            if (valid[e[0]] && valid[e[1]]) {
                drawLine((int) proj[e[0]][0], (int) proj[e[0]][1],
                         (int) proj[e[1]][0], (int) proj[e[1]][1], 0x111111);
            }
        }
    }

    private void drawLine(int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            if (x0 >= 0 && x0 < width && y0 >= 0 && y0 < height) {
                pixels[y0 * width + x0] = color;
            }
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    private void renderHeldItem(Block block, Camera cam) {
        if (block == Block.AIR) return;

        // Animate swing
        if (swingProgress > 0.0f) {
            swingProgress -= 0.15f;
            if (swingProgress < 0.0f) swingProgress = 0.0f;
        }

        float swingAngle = (float) Math.sin(swingProgress * Math.PI) * 0.4f;

        // Draw a miniature 3D block in lower right corner
        int itemSize = (int)(width * 0.16f);
        int ix = width - itemSize - (int)(width * 0.04f);
        int iy = height - itemSize - (int)(height * 0.04f) + (int)(swingAngle * 50.0f);

        // Simple 3D isometric projection of the block
        int half = itemSize / 2;
        int quarter = itemSize / 4;

        // Top Face (diamond)
        drawIsoFace(ix + half, iy, ix + itemSize, iy + quarter, ix + half, iy + half, ix, iy + quarter,
                    block.textureIndices[0], 1.0f);
        // Left Face
        drawIsoFace(ix, iy + quarter, ix + half, iy + half, ix + half, iy + itemSize, ix, iy + itemSize - quarter,
                    block.textureIndices[4], 0.7f);
        // Right Face
        drawIsoFace(ix + half, iy + half, ix + itemSize, iy + quarter, ix + itemSize, iy + itemSize - quarter, ix + half, iy + itemSize,
                    block.textureIndices[3], 0.85f);
    }

    private void drawIsoFace(int x0, int y0, int x1, int y1, int x2, int y2, int x3, int y3, int texId, float light) {
        int[] tex = TextureAtlas.TEXTURES[texId];
        // Render 2 triangles for the mini isometric face
        draw2DTriangle(x0, y0, x1, y1, x2, y2, tex, light);
        draw2DTriangle(x0, y0, x2, y2, x3, y3, tex, light);
    }

    private void draw2DTriangle(int x0, int y0, int x1, int y1, int x2, int y2, int[] tex, float light) {
        int minX = Math.max(0, Math.min(x0, Math.min(x1, x2)));
        int maxX = Math.min(width - 1, Math.max(x0, Math.max(x1, x2)));
        int minY = Math.max(0, Math.min(y0, Math.min(y1, y2)));
        int maxY = Math.min(height - 1, Math.max(y0, Math.max(y1, y2)));

        float area = (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0);
        if (Math.abs(area) < 0.001f) return;
        float invArea = 1.0f / area;

        for (int y = minY; y <= maxY; y++) {
            int row = y * width;
            for (int x = minX; x <= maxX; x++) {
                float w0 = (x - x1) * (y2 - y1) - (y - y1) * (x2 - x1);
                float w1 = (x - x2) * (y0 - y2) - (y - y2) * (x0 - x2);
                float w2 = (x - x0) * (y1 - y0) - (y - y0) * (x1 - x0);

                if (area < 0) {
                    if (w0 > 0 || w1 > 0 || w2 > 0) continue;
                } else {
                    if (w0 < 0 || w1 < 0 || w2 < 0) continue;
                }

                w0 *= invArea;
                w1 *= invArea;
                w2 *= invArea;

                int u = (int) ((w1 + w2) * 15.0f) & 15;
                int v = (int) (w2 * 15.0f) & 15;

                int c = tex[v * 16 + u];
                if (((c >> 24) & 0xFF) == 0) continue;

                int r = (int) (((c >> 16) & 0xFF) * light);
                int g = (int) (((c >> 8) & 0xFF) * light);
                int b = (int) ((c & 0xFF) * light);

                pixels[row + x] = (r << 16) | (g << 8) | b;
            }
        }
    }

    public void drawHUD(Graphics2D g2, int windowW, int windowH, Player player, int fps, Raycast.HitResult hit) {
        // Crosshair (+)
        int cx = windowW / 2;
        int cy = windowH / 2;
        g2.setColor(new Color(255, 255, 255, 180));
        g2.fillRect(cx - 8, cy - 1, 16, 2);
        g2.fillRect(cx - 1, cy - 8, 2, 16);

        // Hotbar
        int slotSize = 44;
        int hotbarW = slotSize * 9;
        int startX = (windowW - hotbarW) / 2;
        int startY = windowH - slotSize - 16;

        for (int i = 0; i < 9; i++) {
            int sx = startX + i * slotSize;
            // Slot background
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRect(sx, startY, slotSize, slotSize);
            g2.setColor(new Color(180, 180, 180, 180));
            g2.drawRect(sx, startY, slotSize, slotSize);

            // Selected slot highlight
            if (i == player.selectedSlot) {
                g2.setColor(Color.WHITE);
                g2.drawRect(sx - 2, startY - 2, slotSize + 4, slotSize + 4);
                g2.drawRect(sx - 1, startY - 1, slotSize + 2, slotSize + 2);
            }

            // Draw block preview in slot
            Block b = player.hotbar[i];
            if (b != null && b != Block.AIR) {
                int[] tex = TextureAtlas.TEXTURES[b.textureIndices[0]];
                for (int ty = 0; ty < 16; ty++) {
                    for (int tx = 0; tx < 16; tx++) {
                        int rgb = tex[ty * 16 + tx];
                        g2.setColor(new Color(rgb));
                        g2.fillRect(sx + 6 + tx * 2, startY + 6 + ty * 2, 2, 2);
                    }
                }
            }

            // Number 1-9 label
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawString(String.valueOf(i + 1), sx + 4, startY + 14);
        }

        // Selected block name above hotbar
        Block cur = player.getSelectedBlock();
        if (cur != null) {
            String name = cur.name;
            int nameW = g2.getFontMetrics().stringWidth(name);
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect((windowW - nameW) / 2 - 8, startY - 26, nameW + 16, 20);
            g2.setColor(Color.YELLOW);
            g2.drawString(name, (windowW - nameW) / 2, startY - 12);
        }

        // F3 Debug info overlay in top-left
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRect(8, 8, 260, 100);
        g2.setColor(Color.WHITE);
        g2.drawString("MinJ - Minecraft Java Edition", 16, 24);
        g2.drawString(String.format("FPS: %d", fps), 16, 40);
        g2.drawString(String.format("XYZ: %.2f / %.2f / %.2f", player.x, player.y, player.z), 16, 56);
        g2.drawString(String.format("Facing: %s (Yaw: %.1f)", getFacing(player.camera.yaw), Math.toDegrees(player.camera.yaw)), 16, 72);
        g2.drawString(String.format("Flight Mode [F]: %s", player.isFlying ? "ON" : "OFF"), 16, 88);
        if (hit != null && hit.hit) {
            Block tb = Block.getById((byte) 0); // Placeholder
            g2.drawString(String.format("Target: [%d, %d, %d]", hit.x, hit.y, hit.z), 16, 102);
        }
    }

    private String getFacing(double yaw) {
        double deg = Math.toDegrees(yaw) % 360;
        if (deg < 0) deg += 360;
        if (deg >= 45 && deg < 135) return "East (+X)";
        if (deg >= 135 && deg < 225) return "South (+Z)";
        if (deg >= 225 && deg < 315) return "West (-X)";
        return "North (-Z)";
    }
}
