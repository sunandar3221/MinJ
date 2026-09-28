package com.minj;

import java.awt.Component;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

public class InputHandler implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {
    private final boolean[] keys = new boolean[65536];

    public boolean forward, backward, left, right;
    public boolean jump, sneak, sprint;
    public boolean toggleFlyRequested = false;

    public boolean leftClick = false;
    public boolean rightClick = false;

    private final Component component;
    private Robot robot;
    public boolean mouseCaptured = false;
    private boolean ignoreNextMotion = false;

    public static final double SENSITIVITY = 0.0035;

    public InputHandler(Component component) {
        this.component = component;
        try {
            this.robot = new Robot();
        } catch (Exception e) {
            this.robot = null;
        }

        component.addKeyListener(this);
        component.addMouseListener(this);
        component.addMouseMotionListener(this);
        component.addMouseWheelListener(this);
    }

    public void setMouseCaptured(boolean captured) {
        this.mouseCaptured = captured;
        if (captured) {
            centerCursor();
            ignoreNextMotion = true;
        }
    }

    private void centerCursor() {
        if (robot == null || !component.isShowing()) return;
        Point loc = component.getLocationOnScreen();
        int cx = loc.x + component.getWidth() / 2;
        int cy = loc.y + component.getHeight() / 2;
        robot.mouseMove(cx, cy);
    }

    public void update(Player player) {
        forward = keys[KeyEvent.VK_W] || keys[KeyEvent.VK_UP];
        backward = keys[KeyEvent.VK_S] || keys[KeyEvent.VK_DOWN];
        left = keys[KeyEvent.VK_A] || keys[KeyEvent.VK_LEFT];
        right = keys[KeyEvent.VK_D] || keys[KeyEvent.VK_RIGHT];
        jump = keys[KeyEvent.VK_SPACE];
        sneak = keys[KeyEvent.VK_SHIFT];
        sprint = keys[KeyEvent.VK_CONTROL];

        // Number keys 1-9
        for (int i = 0; i < 9; i++) {
            if (keys[KeyEvent.VK_1 + i]) {
                player.selectedSlot = i;
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code >= 0 && code < keys.length) {
            keys[code] = true;
        }

        if (code == KeyEvent.VK_F) {
            toggleFlyRequested = true;
        } else if (code == KeyEvent.VK_ESCAPE) {
            setMouseCaptured(false);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code >= 0 && code < keys.length) {
            keys[code] = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {
        if (!mouseCaptured) {
            setMouseCaptured(true);
            return;
        }

        if (e.getButton() == MouseEvent.BUTTON1) {
            leftClick = true;
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            rightClick = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    public void handleMouseMotion(Camera camera) {
        if (!mouseCaptured || robot == null || !component.isShowing()) return;

        Point loc = component.getLocationOnScreen();
        int cx = loc.x + component.getWidth() / 2;
        int cy = loc.y + component.getHeight() / 2;

        Point mouseLoc = java.awt.MouseInfo.getPointerInfo().getLocation();
        int dx = mouseLoc.x - cx;
        int dy = mouseLoc.y - cy;

        if (ignoreNextMotion) {
            ignoreNextMotion = false;
            centerCursor();
            return;
        }

        if (dx != 0 || dy != 0) {
            camera.yaw += dx * SENSITIVITY;
            camera.pitch -= dy * SENSITIVITY;
            camera.pitch = Math.max(-1.55, Math.min(1.55, camera.pitch));
            camera.updateTrig();
            centerCursor();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {}

    @Override
    public void mouseDragged(MouseEvent e) {}

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int notches = e.getWheelRotation();
        Main.onMouseWheel(notches);
    }
}
