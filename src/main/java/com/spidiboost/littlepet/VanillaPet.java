package com.spidiboost.littlepet;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.util.math.Vec3d;

public final class VanillaPet {
    private LivingEntity entity;
    private String selected;
    private Vec3d previous;
    private float speed;
    public void reset() {entity=null;selected=null;previous=null;speed=0;}
    public LivingEntity getEntity() {return entity;}
    public void tick(String id,PetBrain brain) {
        MinecraftClient client=MinecraftClient.getInstance();
        if(client.world==null || !PetCatalog.vanilla(id)) {reset();return;}
        if(entity==null || entity.getWorld()!=client.world || !id.equals(selected)) {
            EntityType<?> type=PetCatalog.type(id);
            Entity created=type==null?null:type.create(client.world,SpawnReason.LOAD);
            if(!(created instanceof LivingEntity living)) {reset();return;}
            entity=living;selected=id;previous=null;
            if(entity instanceof ParrotEntity parrot)parrot.setVariant(ParrotEntity.Variant.values()[Math.min(4,Math.max(0,PetCatalog.variant(id)))]);
            if(entity instanceof net.minecraft.entity.mob.SlimeEntity slime)slime.setSize(2,false);
            if(entity instanceof net.minecraft.entity.passive.BatEntity bat) {bat.setRoosting(false);bat.flyingAnimationState.start(0);}
            if(entity instanceof net.minecraft.entity.mob.BreezeEntity breeze)breeze.idleAnimationState.start(0);
            entity.setInvulnerable(true);entity.setSilent(true);entity.setNoGravity(true);
        }
        Vec3d pos=brain.getPos();
        double movement=previous==null?0:Math.min(1,Math.sqrt(pos.squaredDistanceTo(previous))*4);
        speed+=(movement-speed)*0.4F;previous=pos;
        entity.prevX=entity.getX();entity.prevY=entity.getY();entity.prevZ=entity.getZ();
        entity.setPosition(pos);entity.lastRenderX=pos.x;entity.lastRenderY=pos.y;entity.lastRenderZ=pos.z;
        entity.prevYaw=entity.getYaw();entity.prevPitch=entity.getPitch();
        entity.prevBodyYaw=entity.bodyYaw;entity.prevHeadYaw=entity.headYaw;
        entity.setYaw(brain.getBody());entity.setPitch(brain.getPitch());entity.bodyYaw=brain.getBody();entity.headYaw=brain.getBody()+brain.getYaw();
        entity.setOnGround(true);entity.setVelocity(Vec3d.ZERO);entity.age++;
        entity.limbAnimator.updateLimbs(speed,0.4F,1);
        if(entity instanceof net.minecraft.entity.passive.SquidEntity squid) {
            squid.prevThrustTimer=squid.thrustTimer;squid.thrustTimer=entity.age*0.15F;
            squid.prevTentacleAngle=squid.tentacleAngle;squid.tentacleAngle=(float)(Math.sin(squid.thrustTimer)*Math.sin(squid.thrustTimer)*Math.PI/2);
        }
        if(entity instanceof net.minecraft.entity.mob.BreezeEntity breeze) {
            if(speed>0.03F)breeze.slidingAnimationState.startIfNotRunning(entity.age);else breeze.slidingAnimationState.stop();
        }
        if(entity instanceof net.minecraft.entity.mob.SlimeEntity slime) {slime.lastStretch=slime.stretch;slime.stretch=speed>0.03F?(float)Math.sin(entity.age*0.4F)*0.15F:0;}
        if(entity instanceof EnderDragonEntity dragon) {dragon.frameTracker.tick(pos.y,brain.getBody());dragon.prevWingPosition=dragon.wingPosition;dragon.wingPosition+=0.1F;}
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public void render(MatrixStack matrices,VertexConsumerProvider consumers,int light,PetBrain brain,float delta) {
        if(entity==null)return;
        EntityRenderer renderer=MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        EntityRenderState state=renderer.getAndUpdateRenderState(entity,delta);
        state.displayName=null;state.leashData=null;state.onFire=false;state.positionOffset=null;
        if(state instanceof LivingEntityRenderState living) {
            living.bodyYaw=brain.getBody();living.yawDegrees=brain.getYaw();living.pitch=brain.getPitch();
            living.limbFrequency=entity.limbAnimator.getPos(delta);living.limbAmplitudeMultiplier=entity.limbAnimator.getSpeed(delta);
            living.touchingWater=entity instanceof net.minecraft.entity.mob.WaterCreatureEntity;living.hurt=false;living.deathTime=0;
        }
        float scale=Math.min(1F,2.8F/Math.max(entity.getWidth(),entity.getHeight()));
        Bounds bounds=new Bounds();MatrixStack measure=new MatrixStack();measure.scale(scale,scale,scale);
        renderer.render(state,measure,layer->bounds,light);
        float clearance=Float.isFinite(bounds.minimum)?Math.max(0,0.01F-bounds.minimum):0;
        matrices.push();
        try {matrices.translate(0,clearance,0);matrices.scale(scale,scale,scale);renderer.render(state,matrices,consumers,light);}
        finally {matrices.pop();}
    }
    private static final class Bounds implements VertexConsumer {
        private float minimum=Float.POSITIVE_INFINITY;
        @Override public VertexConsumer vertex(float x,float y,float z) {minimum=Math.min(minimum,y);return this;}
        @Override public VertexConsumer color(int r,int g,int b,int a) {return this;}
        @Override public VertexConsumer texture(float u,float v) {return this;}
        @Override public VertexConsumer overlay(int u,int v) {return this;}
        @Override public VertexConsumer light(int u,int v) {return this;}
        @Override public VertexConsumer normal(float x,float y,float z) {return this;}
    }
}
