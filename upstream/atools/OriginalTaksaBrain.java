package com.rwstafftool.modules.visuals.taksa;

import com.rwstafftool.manager.IMinecraft;
import com.rwstafftool.modules.visuals.taksa.SmoothValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class TaksaBrain
implements IMinecraft {
    private Vec3d pos = null;
    private Vec3d motion = Vec3d.ZERO;
    private float direction = (float)(Math.random() * 360.0);
    private SmoothValue sx = new SmoothValue(0.0f, 150);
    private SmoothValue sy = new SmoothValue(0.0f, 150);
    private SmoothValue sz = new SmoothValue(0.0f, 150);
    private float yaw;
    private float body;
    private SmoothValue bodySmooth = new SmoothValue(0.0f, 150);
    private SmoothValue yawSmooth = new SmoothValue(0.0f, 150);
    private SmoothValue pitchSmooth = new SmoothValue(0.0f, 150);
    private boolean lay;
    private long stayingStart = System.currentTimeMillis();
    public float prevLimbSwingAmount;
    public float limbSwingAmount;
    public float limbSwing;
    private PlayerEntity entity;
    private LivingEntity attackTarget;
    private long attackTargetExpiry = 0L;
    private static final long ATTACK_TARGET_TIMEOUT = 3000L;

    public void setEntity(PlayerEntity player) {
        this.entity = player;
    }

    public void setAttackTarget(LivingEntity target) {
        this.attackTarget = target;
        this.attackTargetExpiry = System.currentTimeMillis() + 3000L;
    }

    public void clearAttackTarget() {
        this.attackTarget = null;
        this.attackTargetExpiry = 0L;
    }

    public void reset() {
        this.pos = null;
        this.entity = null;
        this.attackTarget = null;
        this.attackTargetExpiry = 0L;
        this.motion = Vec3d.ZERO;
        this.direction = (float)(Math.random() * 360.0);
        this.sx = new SmoothValue(0.0f, 150);
        this.sy = new SmoothValue(0.0f, 150);
        this.sz = new SmoothValue(0.0f, 150);
        this.bodySmooth = new SmoothValue(0.0f, 150);
        this.yawSmooth = new SmoothValue(0.0f, 150);
        this.pitchSmooth = new SmoothValue(0.0f, 150);
        this.stayingStart = System.currentTimeMillis();
        this.lay = false;
        this.limbSwing = 0.0f;
        this.limbSwingAmount = 0.0f;
        this.prevLimbSwingAmount = 0.0f;
        this.yaw = 0.0f;
        this.body = 0.0f;
    }

    public void tick() {
        boolean moving;
        Vec3d toOwner;
        Vec3d flat;
        double verticalGap;
        boolean ownerNearGround;
        double groundY;
        if (this.entity == null) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        Vec3d ownerPos = this.entity.getPos();
        if (this.pos == null || this.pos.distanceTo(ownerPos) > 10.0) {
            double spawnY;
            double gy = this.findGroundY(ownerPos.getX(), ownerPos.getY(), ownerPos.getZ(), 256);
            if (!Double.isNaN(gy)) {
                spawnY = gy;
            } else {
                spawnY = ownerPos.getY();
                if (mc.world != null) {
                    spawnY = Math.max(spawnY, (double)(mc.world.getBottomY() + 1));
                }
            }
            this.pos = new Vec3d(ownerPos.getX(), spawnY, ownerPos.getZ());
            this.sx = new SmoothValue((float)this.pos.getX(), 80);
            this.sy = new SmoothValue((float)this.pos.getY(), 80);
            this.sz = new SmoothValue((float)this.pos.getZ(), 80);
            this.motion = Vec3d.ZERO;
        }
        this.motion = this.motion.add(0.0, -0.2, 0.0);
        Vec3d newPos = this.pos.add(this.motion);
        if (mc.world != null && !Double.isNaN(groundY = this.findGroundY(newPos.getX(), newPos.getY() + 0.5, newPos.getZ(), 64))) {
            boolean jumping;
            boolean bl = jumping = this.motion.getY() > 0.15;
            if (!jumping) {
                newPos = new Vec3d(newPos.getX(), groundY, newPos.getZ());
                this.motion = new Vec3d(this.motion.getX(), 0.0, this.motion.getZ());
            } else if (newPos.getY() < groundY) {
                newPos = new Vec3d(newPos.getX(), groundY, newPos.getZ());
                this.motion = new Vec3d(this.motion.getX(), 0.0, this.motion.getZ());
            }
        }
        if (this.attackTarget != null && System.currentTimeMillis() > this.attackTargetExpiry) {
            this.attackTarget = null;
        }
        LivingEntity target = this.attackTarget;
        double distToOwner = newPos.distanceTo(ownerPos);
        if (target != null && (target.isRemoved() || target.isDead())) {
            target = null;
            this.attackTarget = null;
        }
        boolean bl = ownerNearGround = (verticalGap = Math.abs(ownerPos.getY() - newPos.getY())) < 3.5;
        if (target != null) {
            Box targetBox;
            Box selfBox;
            Vec3d toTarget;
            Vec3d flatTarget;
            Vec3d targetPos = target.getPos();
            double targetGap = Math.abs(targetPos.getY() - newPos.getY());
            if (mc.world != null && targetGap < 3.5 && (flatTarget = new Vec3d((toTarget = targetPos.subtract(newPos)).getX(), 0.0, toTarget.getZ())).lengthSquared() > 1.0E-4) {
                Vec3d dir = flatTarget.normalize();
                BlockPos jumpCheck = BlockPos.ofFloored((double)(newPos.getX() + dir.getX() * 0.5), (double)(newPos.getY() + 0.5), (double)(newPos.getZ() + dir.getZ() * 0.5));
                if (!mc.world.getBlockState(jumpCheck).isAir()) {
                    this.motion = new Vec3d(this.motion.getX(), 0.62, this.motion.getZ());
                }
                this.motion = this.motion.add(dir.multiply(0.45));
            }
            if ((selfBox = new Box(newPos.add(-0.4, 0.0, -0.4), newPos.add(0.4, 0.4, 0.4))).intersects(targetBox = target.getBoundingBox().expand(-0.1, 0.0, -0.1))) {
                this.motion = new Vec3d(-this.motion.getX(), this.motion.getY(), -this.motion.getZ());
            }
        } else if (distToOwner > 2.0 && ownerNearGround && (flat = new Vec3d((toOwner = ownerPos.subtract(newPos)).getX(), 0.0, toOwner.getZ())).lengthSquared() > 1.0E-4) {
            this.motion = this.motion.add(flat.normalize().multiply(0.4));
        }
        this.handleRotation(newPos, target);
        this.pos = newPos;
        if (distToOwner < 0.1 && target == null) {
            this.direction = (float)(Math.random() * 360.0);
            double wanderX = -Math.sin(Math.toRadians(this.direction)) * 0.1;
            double wanderZ = Math.cos(Math.toRadians(this.direction)) * 0.1;
            this.motion = this.motion.add(wanderX, 0.0, wanderZ);
        }
        this.motion = this.motion.multiply(0.5);
        this.sx.set((float)this.pos.getX());
        this.sy.set((float)this.pos.getY());
        this.sz.set((float)this.pos.getZ());
        this.limbTick();
        float smoothDx = this.sx.get() - (float)this.pos.getX();
        float smoothDz = this.sz.get() - (float)this.pos.getZ();
        boolean bl2 = moving = Math.abs(smoothDx) > 0.1f || Math.abs(smoothDz) > 0.1f;
        if (moving) {
            this.stayingStart = System.currentTimeMillis();
        }
        this.lay = System.currentTimeMillis() - this.stayingStart > 1000L;
    }

    private double findGroundY(double x, double y, double z, int maxDepth) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) {
            return Double.NaN;
        }
        int startX = MathHelper.floor((double)x);
        int startZ = MathHelper.floor((double)z);
        int startY = MathHelper.floor((double)y);
        int bottom = mc.world.getBottomY();
        int limit = Math.max(bottom, startY - maxDepth);
        for (int by = startY; by >= limit; --by) {
            if (mc.world.getBlockState(new BlockPos(startX, by, startZ)).isAir()) continue;
            return (double)by + 1.0;
        }
        return Double.NaN;
    }

    private void handleRotation(Vec3d currentPos, LivingEntity target) {
        float pitchClamp;
        Vec3d lookAt;
        double hSpeed = Math.sqrt(this.motion.getX() * this.motion.getX() + this.motion.getZ() * this.motion.getZ());
        if (hSpeed > 0.01) {
            double angle = Math.atan2(this.motion.getZ(), this.motion.getX());
            this.yaw = (float)Math.toDegrees(angle) - 90.0f;
            this.yaw %= 360.0f;
            if (this.yaw < 0.0f) {
                this.yaw += 360.0f;
            }
        }
        if (target != null) {
            lookAt = target.getPos();
        } else if (this.entity != null) {
            Vec3d eye = this.entity.getEyePos();
            Vec3d feet = this.entity.getPos();
            double gap = Math.abs(eye.getY() - currentPos.getY());
            lookAt = gap > 4.0 ? feet : eye;
        } else {
            lookAt = currentPos;
        }
        float[] rot = this.getRotation(currentPos, lookAt);
        float targetYaw = rot[0];
        float targetPitch = rot[1];
        float yawSnap = this.lay ? 200.0f : 150.0f;
        float f = pitchClamp = this.lay ? 100.0f : 40.0f;
        if (targetYaw - this.yaw < -yawSnap || targetYaw - this.yaw > yawSnap) {
            this.yaw = targetYaw;
        }
        float bodyDelta = ((this.yaw - this.body) % 360.0f + 540.0f) % 360.0f - 180.0f;
        if (this.lay) {
            bodyDelta = 0.0f;
        } else if (hSpeed < 0.01 && Math.abs(bodyDelta) < 30.0f) {
            bodyDelta = 0.0f;
        } else if (hSpeed < 0.01) {
            bodyDelta *= 0.15f;
        }
        if (!this.lay) {
            this.bodySmooth.set(this.body + bodyDelta);
        }
        this.yawSmooth.set(MathHelper.clamp((float)(targetYaw - this.yaw), (float)(-pitchClamp), (float)pitchClamp));
        this.pitchSmooth.set(MathHelper.clamp((float)targetPitch, (float)(-pitchClamp), (float)pitchClamp));
        this.body += bodyDelta;
    }

    private float[] getRotation(Vec3d from, Vec3d to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yawDeg = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        float pitchDeg = (float)(-Math.toDegrees(Math.atan2(dy, horizontal)));
        return new float[]{yawDeg, pitchDeg};
    }

    private void limbTick() {
        double dz;
        this.prevLimbSwingAmount = this.limbSwingAmount;
        double dx = (double)this.sx.get() - this.pos.getX();
        float speed = MathHelper.sqrt((float)((float)(dx * dx + (dz = (double)this.sz.get() - this.pos.getZ()) * dz))) * 4.0f;
        if (speed > 1.0f) {
            speed = 1.0f;
        }
        this.limbSwingAmount += (speed - this.limbSwingAmount) * 0.4f;
        this.limbSwing += this.limbSwingAmount;
    }

    public Vec3d getPos() {
        if (this.pos == null) {
            return Vec3d.ZERO;
        }
        return new Vec3d((double)this.sx.get(), (double)this.sy.get(), (double)this.sz.get());
    }

    public boolean isSpawned() {
        return this.pos != null;
    }

    public float getBody() {
        return this.bodySmooth.get();
    }

    public float getYaw() {
        return this.yawSmooth.get();
    }

    public float getPitch() {
        return this.pitchSmooth.get();
    }

    public boolean isLay() {
        return this.lay;
    }
}
