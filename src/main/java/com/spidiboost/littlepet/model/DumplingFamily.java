package com.spidiboost.littlepet.model;

import com.google.gson.Gson;
import com.spidiboost.littlepet.PetBrain;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;




 
public final class DumplingFamily {
    public static final float WORLD_SCALE = .96F;
    public static final Identifier TEXTURE = Identifier.of("spidiboostlittlepet", "textures/dumpling-family.png");
    private static final float CHILD_SCALE = .55F, CLEARANCE = .015F;
    private static final Asset FATHER = load("father"), SON = load("son");
    private final Rig father = new Rig(FATHER, 1F), son = new Rig(SON, CHILD_SCALE);
    private float previousAge = Float.NaN, idleTicks, approach, gesture, lift, bend;
    private int currentAction;
    private boolean showSon;

    public static RenderLayer getRenderLayer() { return RenderLayer.getEntityCutoutNoCull(TEXTURE); }
    public void resetAnimation() {
        previousAge = Float.NaN;
        idleTicks = approach = gesture = lift = bend = 0;
        currentAction = 0;
        showSon = false;
    }

    public void updatePose(PetBrain brain, float age, boolean family) {
        if (!Float.isNaN(previousAge) && (age < previousAge || age - previousAge > 20F)) resetAnimation();
        float dt = Float.isNaN(previousAge) ? 1F : MathHelper.clamp(age - previousAge, 0, 2);
        previousAge = age;
        boolean idle = family && brain.isLay() && brain.limbSwingAmount < .02F;
        idleTicks = idle ? idleTicks + dt : 0;
        float cycle = idleTicks % 960F;
        int action = 0;
        float t = 0;
        if (idle && cycle >= 70 && cycle < 250) { action = 1; t = (cycle - 70) / 180F; }
        else if (idle && cycle >= 320 && cycle < 530) { action = 2; t = (cycle - 320) / 210F; }
        else if (idle && cycle >= 610 && cycle < 860) { action = 3; t = (cycle - 610) / 250F; }
        float a = action == 0 ? 0 : envelope(t, .0F, .18F, .79F, 1F);
        float g = action == 0 ? 0 : envelope(t, .18F, .32F, .67F, .79F);
        float h = action == 3 ? envelope(t, .32F, .46F, .57F, .68F) * .56F : 0;
        
        
        if (action != 0) currentAction = action;
        float smoothing = 1F - (float)Math.exp(-dt * (idle ? .24F : .15F));
        approach = MathHelper.lerp(smoothing, approach, a);
        gesture = MathHelper.lerp(smoothing, gesture, g);
        lift = MathHelper.lerp(smoothing, lift, h);
        float targetBend = currentAction == 1 ? .55F : currentAction == 2 ? .65F : .68F * (1F - MathHelper.clamp(lift / .56F,0,1)) + .22F * MathHelper.clamp(lift / .56F,0,1);
        
        
        bend = targetBend * gesture;
        if (!family) { approach = gesture = lift = bend = 0; currentAction = 0; }
        pose(age, brain.limbSwing, MathHelper.clamp(brain.limbSwingAmount,0,1), brain.getYaw(), brain.getPitch(), family);
    }

     
    public void sample(int action, float t, float age, float movement, boolean family) {
        resetAnimation(); currentAction = action;
        approach = action == 0 ? 0 : envelope(t,0,.18F,.79F,1);
        gesture = action == 0 ? 0 : envelope(t,.18F,.32F,.67F,.79F);
        lift = action == 3 ? .56F * envelope(t,.32F,.46F,.57F,.68F) : 0;
        bend = (action == 1 ? .55F : action == 2 ? .65F : .68F + (.22F-.68F)*lift/.56F)*gesture;
        pose(age, age*.7F, movement, 0, 0, family);
    }

    private void pose(float age, float walk, float movement, float headYaw, float headPitch, boolean family) {
        showSon = family;
        float stride = movement * (1F - approach);
        father.build(new Vector3f(0,0,0),0,bend,walk,stride,age,headYaw + (currentAction==1 ? 25F*gesture : 0),headPitch + 10F*gesture);
        float targetZ = currentAction == 1 ? .66F : currentAction == 2 ? .43F : .59F;
        float targetX = currentAction == 1 ? .48F : 0;
        Vector3f childOrigin = new Vector3f(.78F + (targetX-.78F)*approach, lift + CLEARANCE*(1F-CHILD_SCALE), -.10F + (targetZ + .10F) * approach);
        son.build(childOrigin, ((float)Math.PI + (currentAction==1 ? .63F : 0))*approach, 0, walk + .6F,stride,age+17,-headYaw*.3F,headPitch*.2F-22F*approach);
        son.foldLegs(lift / .56F);
        if (gesture > .00001F && family) {
            if (currentAction == 1) {
                Vector3f palm = new Vector3f(.22F,.70F + (float)Math.sin(age*.6F)*.009F*gesture,.53F);
                father.reach(true,palm,gesture); son.reach(true,palm,gesture);
            } else if (currentAction == 2) {
                father.reach(false,new Vector3f(-.18F,.66F,.59F),gesture);
                father.reach(true,new Vector3f(.18F,.66F,.59F),gesture);
                son.reach(false,new Vector3f(.19F,.63F,.23F),gesture);
                son.reach(true,new Vector3f(-.19F,.63F,.23F),gesture);
            } else if (currentAction == 3) {
                
                father.reach(false,new Vector3f(-.16F,.61F + lift,.59F),gesture);
                father.reach(true,new Vector3f(.16F,.61F + lift,.59F),gesture);
                son.reach(false,new Vector3f(.24F,.64F + lift,.37F),gesture);
                son.reach(true,new Vector3f(-.24F,.64F + lift,.37F),gesture);
            }
        }
        father.ground();
        son.ground(); 
    }

    public void renderModel(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float bodyYaw) {
        matrices.push();
        
        
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw));
        matrices.scale(WORLD_SCALE,WORLD_SCALE,WORLD_SCALE);
        father.render(matrices.peek(),vertices,light,overlay);
        if (showSon) son.render(matrices.peek(),vertices,light,overlay);
        matrices.pop();
    }

    public float getLiftHeight() { return lift; }
    public int getAction() { return currentAction; }
    public float getGesture() { return gesture; }
    public float palmDistance() { return father.rightPalm.distance(son.rightPalm); }
    public float supportDistance(boolean right) {
        Vector3f support = new Vector3f(right ? .16F : -.16F,.61F + lift,.59F);
        return support.distance(right ? father.rightPalm : father.leftPalm);
    }

    private static float smooth(float t) { t = MathHelper.clamp(t,0,1); return t*t*(3-2*t); }
    private static float envelope(float t,float in0,float in1,float out0,float out1) {
        return smooth((t-in0)/(in1-in0)) * (1-smooth((t-out0)/(out1-out0)));
    }
    private static Asset load(String name) {
        String path = "/assets/spidiboostlittlepet/models/dumpling-" + name + ".json";
        try (InputStream stream = DumplingFamily.class.getResourceAsStream(path)) {
            if (stream == null) throw new IllegalStateException("Missing original LittlePet mesh: " + path);
            Asset asset = new Gson().fromJson(new InputStreamReader(stream,StandardCharsets.UTF_8),Asset.class);
            for (Part p:asset.parts) if (p.vertices.length % 4 != 0) throw new IllegalStateException("Incomplete quad in " + p.bone);
            return asset;
        } catch (IOException e) { throw new IllegalStateException("Cannot read LittlePet mesh",e); }
    }
    private static final class Asset { Part[] parts; }
    private static final class Part { String bone; float[][] vertices; }

    private static final class Rig {
        final Asset asset;
        final float scale;
        final Map<String,Matrix4f> bones = new HashMap<>();
        final Vector3f rightShoulder = new Vector3f(), leftShoulder = new Vector3f();
        final Vector3f rightPalm = new Vector3f(), leftPalm = new Vector3f();
        final Vector3f temp = new Vector3f(), normal = new Vector3f();
        Matrix4f torso, base;
        float baseYaw;
        Rig(Asset asset,float scale) { this.asset=asset;this.scale=scale; }
        void build(Vector3f origin,float yaw,float lean,float walk,float movement,float age,float headYaw,float headPitch) {
            bones.clear();baseYaw=yaw;
            base=new Matrix4f().translation(origin).rotateY(yaw).scale(scale);
            torso=new Matrix4f(base).translate(0,.59F,0).rotateX(lean);
            bones.put("torso",torso);
            bones.put("pelvis",new Matrix4f(base).translate(0,.59F,0));
            Matrix4f head=new Matrix4f(torso).translate(0,.91F,0).rotateX(-lean*.82F)
                    .rotateY(MathHelper.clamp(headYaw,-30,30)*MathHelper.RADIANS_PER_DEGREE)
                    .rotateX(MathHelper.clamp(headPitch,-12,12)*MathHelper.RADIANS_PER_DEGREE);
            bones.put("head",head);
            boolean blink = (age % 117) < 3;
            bones.put("eyes",new Matrix4f(head).translate(0,.052F,0).scale(1,blink?.12F:1,1).translate(0,-.052F,0));
            float phase = walk*.6662F;
            for (boolean right : new boolean[]{false,true}) {
                String side=right?"right":"left";float sign=right?1:-1;
                Vector3f shoulder=new Vector3f(sign*.32F,.56F,0);
                torso.transformPosition(shoulder);
                (right?rightShoulder:leftShoulder).set(shoulder);
                float swing=(float)Math.cos(phase + (right?0:Math.PI)) * .27F * movement;
                Vector3f hand = new Vector3f(sign*.36F,.09F,(float)Math.sin(swing)*.48F);
                torso.transformPosition(hand);
                setArm(right,hand);
                Vector3f hip=new Vector3f(sign*.135F,.59F,0),knee=new Vector3f(sign*.135F,.33F,0),ankle=new Vector3f(sign*.135F,.097F,0);
                float step=(float)Math.cos(phase + (right?Math.PI:0)) * .16F * movement;
                float footLift=Math.max(0,(float)Math.sin(phase+(right?Math.PI:0)))*.067F*movement;
                knee.z=step*.48F;knee.y+=footLift*.3F;
                ankle.z=step;ankle.y+=footLift;
                base.transformPosition(hip);base.transformPosition(knee);base.transformPosition(ankle);
                bones.put(side+"Thigh",segment(hip,knee,.26F));bones.put(side+"Shin",segment(knee,ankle,.233F));
                bones.put(side+"Foot",new Matrix4f().translation(ankle).rotateY(yaw).scale(scale));
            }
        }
        void foldLegs(float amount) {
            amount = MathHelper.clamp(amount,0,1);
            if(amount < .00001F)return;
            for(boolean right:new boolean[]{false,true}) {
                String side=right?"right":"left";float x=(right?1:-1)*.135F;
                Vector3f hip=new Vector3f(x,.59F,0), knee=new Vector3f(x,.33F+.06F*amount,.15F*amount);
                Vector3f ankle=new Vector3f(x,.097F+.153F*amount,.23F*amount);
                base.transformPosition(hip);base.transformPosition(knee);base.transformPosition(ankle);
                bones.put(side+"Thigh",segment(hip,knee,.26F));bones.put(side+"Shin",segment(knee,ankle,.233F));
                bones.put(side+"Foot",new Matrix4f().translation(ankle).rotateY(baseYaw).rotateX(-.4F*amount).scale(scale));
            }
        }
        void reach(boolean right,Vector3f target,float weight) {
            Vector3f hand=new Vector3f(right?rightPalm:leftPalm).lerp(target,weight);
            setArm(right,hand);
        }
        void setArm(boolean right,Vector3f requested) {
            Vector3f shoulder=right?rightShoulder:leftShoulder;
            Vector3f delta=new Vector3f(requested).sub(shoulder);
            float upper=.26F*scale, lower=.25F*scale;
            float distance=Math.max(.001F,delta.length());
            Vector3f direction=delta.div(distance);
            float d=MathHelper.clamp(distance,Math.abs(upper-lower)+.001F,upper+lower-.0001F);
            Vector3f hand=new Vector3f(direction).mul(d).add(shoulder);
            Vector3f bendDir=new Vector3f(right?.7F:-.7F,0,.75F).rotateY(baseYaw);
            bendDir.sub(new Vector3f(direction).mul(bendDir.dot(direction)));
            if(bendDir.lengthSquared()<.00001F)bendDir.set(0,0,1).sub(new Vector3f(direction).mul(direction.z));
            bendDir.normalize();
            float along=(upper*upper-lower*lower+d*d)/(2*d);
            float perpendicular=(float)Math.sqrt(Math.max(0,upper*upper-along*along));
            Vector3f elbow=new Vector3f(shoulder).add(new Vector3f(direction).mul(along)).add(bendDir.mul(perpendicular));
            String side=right?"right":"left";
            bones.put(side+"UpperArm",segment(shoulder,elbow,.26F));bones.put(side+"Forearm",segment(elbow,hand,.25F));
            
            bones.put(side+"Hand",new Matrix4f().translation(hand).rotateY(baseYaw).scale(scale));
            (right?rightPalm:leftPalm).set(hand);
        }
        Matrix4f segment(Vector3f a,Vector3f b,float restLength) {
            Vector3f direction=new Vector3f(b).sub(a);
            float length=direction.length();direction.normalize();
            return new Matrix4f().translation(a).rotate(new Quaternionf().rotationTo(new Vector3f(0,1,0),direction)).scale(scale,length/restLength,scale);
        }
        void ground() {
            float min=Float.POSITIVE_INFINITY;
            for(Part part:asset.parts) {
                Matrix4f transform=bones.get(part.bone);
                for(float[] v:part.vertices) {transform.transformPosition(temp.set(v[0],v[1],v[2]));min=Math.min(min,temp.y);}
            }
            float correction=Math.max(0,CLEARANCE-min);
            if(correction>0) {
                
                
                for(Matrix4f transform:bones.values())transform.m31(transform.m31()+correction);
                rightPalm.y+=correction;leftPalm.y+=correction;
            }
        }
        void render(MatrixStack.Entry entry,VertexConsumer sink,int light,int overlay) {
            for(Part part:asset.parts) {
                Matrix4f bone=bones.get(part.bone);
                Matrix4f position=new Matrix4f(entry.getPositionMatrix()).mul(bone);
                Matrix3f normals=new Matrix3f(position).invert().transpose();
                for(float[] v:part.vertices) {
                    position.transformPosition(temp.set(v[0],v[1],v[2]));
                    normals.transform(normal.set(v[5],v[6],v[7])).normalize();
                    sink.vertex(temp.x,temp.y,temp.z,0xFFFFFFFF,v[3],v[4],overlay,light,normal.x,normal.y,normal.z);
                }
            }
        }
    }
}
