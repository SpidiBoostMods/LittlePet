package com.spidiboost.littlepet.model;

import com.spidiboost.littlepet.PetBrain;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;





 
public final class CrocodileModel {
    public static final Identifier TEXTURE = Identifier.of("spidiboostlittlepet", "textures/crocodile.png");
    
    public static final float SCALE = 2.4F * 16.0F / 119.0F;
    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart tail3;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart leftHand;
    private final ModelPart rightHand;
    private final ModelPart leftFoot;
    private final ModelPart rightFoot;
    
    private static final float FOOT_CLEARANCE_PIXELS = 0.5F;
    private final MatrixStack footTransforms = new MatrixStack();
    private final Vector3f footCorner = new Vector3f();
    private float lowestLimbY;
    private final ModelPart.CuboidConsumer measureLimb = this::measureLimbCorners;
    private float restProgress;
    private float previousAge = Float.NaN;

    public CrocodileModel() {
        ModelData data = new ModelData();
        ModelPartData rootData = data.getRoot().addChild("root", ModelPartBuilder.create(),
                ModelTransform.of(0.0F, 24.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        ModelPartData bodyData = rootData.addChild("body", ModelPartBuilder.create()
                .uv(0,0).mirrored(false)
                .cuboid(-8.0F, -7.0F, -13.0F, 16.0F, 12.0F, 27.0F, new Dilation(0.0F)),
                ModelTransform.of(0.0F, -9.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        ModelPartData left_legData = bodyData.addChild("left_leg", ModelPartBuilder.create()
                .uv(0,0).mirrored(false)
                .cuboid(-2.0F, -2.0F, -5.0F, 5.0F, 8.0F, 8.0F, new Dilation(0.0F)),
                ModelTransform.of(8.0F, 3.0F, 10.0F, 0.0F, 0.0F, 0.0F));

        ModelPartData left_footData = left_legData.addChild("left_foot", ModelPartBuilder.create()
                .uv(45,42).mirrored(false)
                .cuboid(-2.0F, -0.01F, -5.0F, 5.0F, 0.0F, 6.0F, new Dilation(0.0F)),
                ModelTransform.of(2.0F, 6.0F, -3.0F, 0.0F, 0.0F, 0.0F));

        ModelPartData right_legData = bodyData.addChild("right_leg", ModelPartBuilder.create()
                .uv(0,0).mirrored(true)
                .cuboid(-3.0F, -2.0F, -5.0F, 5.0F, 8.0F, 8.0F, new Dilation(0.0F)),
                ModelTransform.of(-8.0F, 3.0F, 10.0F, 0.0F, 0.0F, 0.0F));

        ModelPartData right_footData = right_legData.addChild("right_foot", ModelPartBuilder.create()
                .uv(45,42).mirrored(true)
                .cuboid(-3.0F, -0.01F, -5.0F, 5.0F, 0.0F, 6.0F, new Dilation(0.0F)),
                ModelTransform.of(-2.0F, 6.0F, -3.0F, 0.0F, 0.0F, 0.0F));

        ModelPartData left_armData = bodyData.addChild("left_arm", ModelPartBuilder.create()
                .uv(0,40).mirrored(false)
                .cuboid(-2.0F, -2.0F, -2.0F, 4.0F, 10.0F, 4.0F, new Dilation(0.0F)),
                ModelTransform.of(9.0F, 1.0F, -9.0F, 0.0F, 0.0F, 0.0F));

        ModelPartData left_handData = left_armData.addChild("left_hand", ModelPartBuilder.create()
                .uv(0,17).mirrored(false)
                .cuboid(-2.0F, -0.01F, -7.0F, 6.0F, 0.0F, 7.0F, new Dilation(0.0F)),
                ModelTransform.of(0.0F, 8.0F, 1.0F, 0.0F, 0.0F, 0.0F));

        ModelPartData right_armData = bodyData.addChild("right_arm", ModelPartBuilder.create()
                .uv(0,40).mirrored(true)
                .cuboid(-2.0F, -2.0F, -2.0F, 4.0F, 10.0F, 4.0F, new Dilation(0.0F)),
                ModelTransform.of(-9.0F, 1.0F, -9.0F, 0.0F, 0.0F, 0.0F));

        ModelPartData right_handData = right_armData.addChild("right_hand", ModelPartBuilder.create()
                .uv(0,17).mirrored(true)
                .cuboid(-4.0F, -0.01F, -7.0F, 6.0F, 0.0F, 7.0F, new Dilation(0.0F)),
                ModelTransform.of(0.0F, 8.0F, 1.0F, 0.0F, 0.0F, 0.0F));

        ModelPartData tail1Data = bodyData.addChild("tail1", ModelPartBuilder.create()
                .uv(0,40).mirrored(false)
                .cuboid(-5.0F, -5.0F, -2.0F, 10.0F, 10.0F, 24.0F, new Dilation(0.0F))
                .uv(45,51).mirrored(false)
                .cuboid(-5.0F, -7.0F, -2.0F, 10.0F, 2.0F, 24.0F, new Dilation(0.0F)),
                ModelTransform.of(0.0F, 0.0F, 15.0F, 0.0F, 0.0F, 0.0F));

        ModelPartData tail2Data = tail1Data.addChild("tail2", ModelPartBuilder.create()
                .uv(62,15).mirrored(false)
                .cuboid(-3.0F, -3.0F, -2.0F, 6.0F, 7.0F, 25.0F, new Dilation(0.0F))
                .uv(43,78).mirrored(false)
                .cuboid(-2.0F, -5.0F, -2.0F, 4.0F, 2.0F, 20.0F, new Dilation(0.0F)),
                ModelTransform.of(0.0F, 1.0F, 24.0F, 0.0F, 0.0F, 0.0F));

        ModelPartData tail3Data = tail2Data.addChild("tail3", ModelPartBuilder.create()
                .uv(0,75).mirrored(false)
                .cuboid(0.0F, -6.0F, 0.0F, 0.0F, 10.0F, 21.0F, new Dilation(0.0F)),
                ModelTransform.of(0.0F, 0.0F, 18.0F, 0.0F, 0.0F, 0.0F));

        ModelPartData neckData = bodyData.addChild("neck", ModelPartBuilder.create()
                .uv(80,89).mirrored(false)
                .cuboid(-6.0F, -5.0F, -10.0F, 12.0F, 10.0F, 12.0F, new Dilation(0.0F))
                .uv(60,0).mirrored(false)
                .cuboid(-4.0F, -6.0F, -10.0F, 8.0F, 1.0F, 12.0F, new Dilation(0.0F)),
                ModelTransform.of(0.0F, 0.0F, -14.0F, 0.0F, 0.0F, 0.0F));

        ModelPartData headData = neckData.addChild("head", ModelPartBuilder.create()
                .uv(72,78).mirrored(false)
                .cuboid(-5.0F, -4.0F, -5.0F, 10.0F, 4.0F, 6.0F, new Dilation(0.0F))
                .uv(60,14).mirrored(false)
                .cuboid(-4.0F, -5.0F, -5.0F, 8.0F, 1.0F, 5.0F, new Dilation(0.0F))
                .uv(22,78).mirrored(false)
                .cuboid(-3.0F, -4.0F, -17.0F, 6.0F, 4.0F, 12.0F, new Dilation(0.0F)),
                ModelTransform.of(0.0F, 1.0F, -10.0F, 0.0F, 0.0F, 0.0F));

        ModelPartData left_upperteethData = headData.addChild("left_upperteeth", ModelPartBuilder.create()
                .uv(104,23).mirrored(false)
                .cuboid(0.0F, 0.0F, -0.025F, 3.0F, 2.0F, 11.0F, new Dilation(0.0F)),
                ModelTransform.of(0.0F, 0.0F, -17.0F, 0.0F, 0.0F, -0.0873F));

        ModelPartData right_upperteethData = headData.addChild("right_upperteeth", ModelPartBuilder.create()
                .uv(104,23).mirrored(true)
                .cuboid(-3.0F, 0.0F, -0.025F, 3.0F, 2.0F, 11.0F, new Dilation(0.0F)),
                ModelTransform.of(0.0F, 0.0F, -17.0F, 0.0F, 0.0F, 0.0873F));

        ModelPartData jawData = headData.addChild("jaw", ModelPartBuilder.create()
                .uv(100,7).mirrored(false)
                .cuboid(-5.5F, -2.0F, -6.0F, 11.0F, 5.0F, 7.0F, new Dilation(0.001F))
                .uv(90,48).mirrored(false)
                .cuboid(-3.0F, 0.0F, -17.0F, 6.0F, 3.0F, 11.0F, new Dilation(0.0F)),
                ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));

        ModelPartData left_lowerteethData = jawData.addChild("left_lowerteeth", ModelPartBuilder.create()
                .uv(105,67).mirrored(false)
                .cuboid(0.0F, -2.0F, -0.025F, 3.0F, 2.0F, 11.0F, new Dilation(0.0F)),
                ModelTransform.of(0.0F, 0.0F, -17.0F, 0.0F, 0.0F, 0.0873F));

        ModelPartData right_lowerteethData = jawData.addChild("right_lowerteeth", ModelPartBuilder.create()
                .uv(105,67).mirrored(true)
                .cuboid(-3.0F, -2.0F, -0.025F, 3.0F, 2.0F, 11.0F, new Dilation(0.0F)),
                ModelTransform.of(0.0F, 0.0F, -17.0F, 0.0F, 0.0F, -0.0873F));
        ModelPart model = TexturedModelData.of(data, 256, 256).createModel();
        this.root = model.getChild("root");
        this.body = root.getChild("body");
        this.neck = body.getChild("neck");
        this.head = neck.getChild("head");
        this.jaw = head.getChild("jaw");
        this.tail1 = body.getChild("tail1");
        this.tail2 = tail1.getChild("tail2");
        this.tail3 = tail2.getChild("tail3");
        this.leftArm = body.getChild("left_arm");
        this.rightArm = body.getChild("right_arm");
        this.leftLeg = body.getChild("left_leg");
        this.rightLeg = body.getChild("right_leg");
        this.leftHand = leftArm.getChild("left_hand");
        this.rightHand = rightArm.getChild("right_hand");
        this.leftFoot = leftLeg.getChild("left_foot");
        this.rightFoot = rightLeg.getChild("right_foot");
    }

    public static RenderLayer getRenderLayer() {
        return RenderLayer.getEntityCutoutNoCull(TEXTURE);
    }

    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       PetBrain brain, float age) {
        float dt = Float.isNaN(previousAge) ? 1.0F : MathHelper.clamp(age - previousAge, 0.0F, 2.0F);
        previousAge = age;
        float targetRest = brain.isLay() ? 1.0F : 0.0F;
        restProgress = MathHelper.lerp(1.0F - (float) Math.exp(-dt * 0.16F), restProgress, targetRest);
        float movement = MathHelper.clamp(brain.limbSwingAmount, 0.0F, 1.0F) * (1.0F - restProgress);
        animate(brain.getBody(), brain.getYaw(), brain.getPitch(), brain.limbSwing, movement, age, restProgress);
        renderModel(matrices, vertices, light, overlay, brain.getBody());
    }

    
    public void animate(float bodyYaw, float headYaw, float headPitch, float limbSwing,
                        float movement, float age, float rest) {
        root.traverse().forEach(ModelPart::resetTransform);
        float phase = limbSwing * 0.6662F;
        float stride = (float) Math.cos(phase) * 0.55F * movement;
        leftArm.pitch = stride;
        rightLeg.pitch = stride;
        rightArm.pitch = -stride;
        leftLeg.pitch = -stride;
        
        body.pivotY += rest * 3.0F;
        float splay = rest * 0.5235988F;
        leftArm.roll = -splay;
        leftLeg.roll = -splay;
        rightArm.roll = splay;
        rightLeg.roll = splay;
        for (ModelPart leg : new ModelPart[]{leftArm, rightArm, leftLeg, rightLeg}) leg.pivotY -= rest * 3.0F;
        leftHand.roll = splay;
        leftFoot.roll = splay;
        rightHand.roll = -splay;
        rightFoot.roll = -splay;
        
        leftHand.pivotY = rightHand.pivotY = 8.0F / (float) Math.cos(splay);
        leftFoot.pivotY = rightFoot.pivotY = (6.0F + 2.0F * (float) Math.sin(splay)) / (float) Math.cos(splay);
        leftHand.pitch = -leftArm.pitch;
        rightHand.pitch = -rightArm.pitch;
        leftFoot.pitch = -leftLeg.pitch;
        rightFoot.pitch = -rightLeg.pitch;
        leftLeg.pivotY -= Math.max(0.0F, 6.0F * (float) Math.cos(leftLeg.pitch) + 3.0F * (float) Math.sin(leftLeg.pitch) - 6.0F);
        rightLeg.pivotY -= Math.max(0.0F, 6.0F * (float) Math.cos(rightLeg.pitch) + 3.0F * (float) Math.sin(rightLeg.pitch) - 6.0F);

        float lookYaw = MathHelper.clamp(MathHelper.wrapDegrees(headYaw), -28.0F, 28.0F) * MathHelper.RADIANS_PER_DEGREE;
        float lookPitch = MathHelper.clamp(headPitch, -12.0F, 12.0F) * MathHelper.RADIANS_PER_DEGREE;
        neck.yaw = lookYaw * 0.5F;
        head.yaw = lookYaw * 0.5F;
        neck.pitch = lookPitch * 0.35F;
        head.pitch = lookPitch * 0.65F;
        
        jaw.pitch = 0.12F - rest * 0.04F;
        float tailMotion = 0.02F + movement * 0.12F;
        tail1.yaw = (float) Math.sin(age * 0.08F) * tailMotion + rest * 0.12F;
        tail2.yaw = (float) Math.sin(age * 0.08F - 0.5F) * tailMotion * 1.2F + rest * 0.15F;
        tail3.yaw = (float) Math.sin(age * 0.08F - 1.0F) * tailMotion * 1.4F + rest * 0.12F;
        body.pivotY += (float) Math.sin(age * 0.07F) * rest * 0.06F;

        
        
        float stepLift = 0.6F * movement * (1.0F - rest);
        float liftLeft = Math.max(0.0F, (float) Math.sin(phase)) * stepLift;
        float liftRight = Math.max(0.0F, -(float) Math.sin(phase)) * stepLift;
        placeLimbAboveGround(leftArm, liftLeft);
        placeLimbAboveGround(rightArm, liftRight);
        placeLimbAboveGround(leftLeg, liftRight);
        placeLimbAboveGround(rightLeg, liftLeft);
    }

    private void placeLimbAboveGround(ModelPart leg, float liftPixels) {
        footTransforms.push();
        root.rotate(footTransforms);
        body.rotate(footTransforms);
        float parentVerticalScale = footTransforms.peek().getPositionMatrix().m11();
        lowestLimbY = Float.NEGATIVE_INFINITY;
        
        leg.forEachCuboid(footTransforms, measureLimb);
        footTransforms.pop();
        float groundModelY = (24.0F - FOOT_CLEARANCE_PIXELS - liftPixels) / 16.0F;
        leg.pivotY += (groundModelY - lowestLimbY) * 16.0F / parentVerticalScale;
    }

    private void measureLimbCorners(MatrixStack.Entry transform, String path, int index,
                                    ModelPart.Cuboid cuboid) {
        for (int corner = 0; corner < 8; corner++) {
            footCorner.set(((corner & 1) == 0 ? cuboid.minX : cuboid.maxX) / 16.0F,
                    ((corner & 2) == 0 ? cuboid.minY : cuboid.maxY) / 16.0F,
                    ((corner & 4) == 0 ? cuboid.minZ : cuboid.maxZ) / 16.0F);
            transform.getPositionMatrix().transformPosition(footCorner);
            
            lowestLimbY = Math.max(lowestLimbY, footCorner.y);
        }
    }

    public void renderModel(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float bodyYaw) {
        matrices.push();
        matrices.scale(SCALE, SCALE, SCALE);
        matrices.translate(0.0F, 1.5F, 0.0F);
        
        
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0F));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(bodyYaw));
        root.render(matrices, vertices, light, overlay);
        matrices.pop();
    }
}
