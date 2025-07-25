package com.shanebeestudios.stress.api.bot;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles movement for bots
 */
public class BotMovement extends BukkitRunnable {
    private final Bot bot;
    private final Random random;
    private int movementCooldown = 0;
    private double lastX, lastZ;
    private double targetX, targetZ;
    private boolean isMoving = false;
    private long lastMoveTime = 0;
    private static final long MOVE_INTERVAL = 50; // 50ms between movement packets
    private static final double MOVE_SPEED = 0.15; // Movement speed per tick

    public BotMovement(Bot bot) {
        this.bot = bot;
        this.random = ThreadLocalRandom.current();
        this.lastX = 0;
        this.lastZ = 0;
    }

    @Override
    public void run() {
        if (!bot.isConnected()) {
            cancel();
            return;
        }

        if (movementCooldown > 0) {
            movementCooldown--;
            if (isMoving) {
                // Continue moving towards target
                moveTowardsTarget();
            }
            return;
        }

        // 50% chance to start moving if not already moving
        if (!isMoving && random.nextBoolean()) {
            startNewMovement();
        } else if (isMoving) {
            // Stop current movement
            isMoving = false;
            movementCooldown = 20 + random.nextInt(40); // 1-3 seconds cooldown
        }
    }

    private void startNewMovement() {
        // Set random target within 5 blocks
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = 2 + random.nextDouble() * 3; // 2-5 blocks
        
        this.targetX = lastX + Math.cos(angle) * distance;
        this.targetZ = lastZ + Math.sin(angle) * distance;
        
        this.isMoving = true;
        this.movementCooldown = 20 + random.nextInt(40); // 1-3 seconds to reach target
    }

    private void moveTowardsTarget() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMoveTime < MOVE_INTERVAL) {
            return; // Don't send movement packets too frequently
        }
        lastMoveTime = currentTime;
        
        double dx = targetX - lastX;
        double dz = targetZ - lastZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        
        if (distance < 0.2) {
            // Reached target
            isMoving = false;
            movementCooldown = 20 + random.nextInt(40); // 1-3 seconds cooldown
            return;
        }
        
        // Normalize direction and move
        double vx = (dx / distance) * MOVE_SPEED;
        double vz = (dz / distance) * MOVE_SPEED;
        
        // Calculate new position
        double newX = lastX + vx;
        double newZ = lastZ + vz;
        
        // Make sure we don't overshoot the target
        if (Math.signum(dx) != Math.signum(targetX - newX)) newX = targetX;
        if (Math.signum(dz) != Math.signum(targetZ - newZ)) newZ = targetZ;
        
        // Update position
        lastX = newX;
        lastZ = newZ;
        
        // Calculate yaw for looking in movement direction
        float yaw = (float) Math.toDegrees(Math.atan2(vz, vx)) - 90;
        
        // Move the bot
        bot.moveTo(lastX, bot.getLastY(), lastZ, yaw, 0);
    }

    public void updatePosition(double x, double z) {
        this.lastX = x;
        this.lastZ = z;
    }

    public double getLastX() {
        return lastX;
    }

    public double getLastZ() {
        return lastZ;
    }
}
