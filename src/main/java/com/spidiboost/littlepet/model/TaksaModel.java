package com.spidiboost.littlepet.model;

import com.spidiboost.littlepet.PetBrain;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class TaksaModel {
    public static final Identifier TEXTURE = Identifier.of((String)"spidiboostlittlepet", (String)"textures/taksa.png");
    private final ModelPart head;
    private final ModelPart neck;
    private final ModelPart body;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontRightLeg;
    private final ModelPart leftBackLeg;
    private final ModelPart rightBackLeg;
    private final ModelPart tail;

    public TaksaModel() {
        ModelData modelData = TaksaModel.getModelData();
        ModelPartData root = modelData.getRoot();
        ModelPart main = root.createPart(60, 36);
        this.head = main.getChild("head");
        this.neck = main.getChild("neck");
        this.body = main.getChild("body");
        this.frontLeftLeg = main.getChild("frontLeftLeg");
        this.frontRightLeg = main.getChild("frontRightLeg");
        this.leftBackLeg = main.getChild("leftBackLeg");
        this.rightBackLeg = main.getChild("rightBackLeg");
        this.tail = main.getChild("tail");
    }

    public static ModelData getModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();
        ModelPartData headData = root.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-3.0f, -3.0f, -4.0f, 6.0f, 6.0f, 4.0f).uv(21, 0).cuboid(-1.5f, 0.0f, -7.0f, 3.0f, 3.0f, 3.0f), ModelTransform.pivot((float)0.0f, (float)10.5f, (float)-6.8f));
        headData.addChild("leftEar", ModelPartBuilder.create().uv(32, 4).cuboid(0.0f, -5.0f, -1.5f, 1.0f, 3.0f, 3.0f).uv(34, 1).cuboid(0.0f, -5.5f, -0.75f, 1.0f, 1.0f, 1.0f), ModelTransform.pivot((float)3.0f, (float)3.0f, (float)-2.0f));
        headData.addChild("rightEar", ModelPartBuilder.create().uv(32, 4).cuboid(-1.0f, -5.0f, -1.5f, 1.0f, 3.0f, 3.0f).uv(34, 1).cuboid(-1.0f, -5.5f, -0.75f, 1.0f, 1.0f, 1.0f), ModelTransform.pivot((float)-3.0f, (float)3.0f, (float)-2.0f));
        root.addChild("neck", ModelPartBuilder.create().uv(15, 7).cuboid(-2.95f, -1.0f, -4.0f, 5.9f, 5.0f, 6.0f), ModelTransform.of((float)0.0f, (float)10.5f, (float)-5.0f, (float)-0.43633232f, (float)0.0f, (float)0.0f));
        ModelPartData bodyData = root.addChild("body", ModelPartBuilder.create(), ModelTransform.pivot((float)0.0f, (float)13.5f, (float)-5.0f));
        bodyData.addChild("chest", ModelPartBuilder.create().uv(32, 13).cuboid(-4.0f, -3.5f, -3.0f, 8.0f, 7.0f, 6.0f), ModelTransform.pivot((float)0.0f, (float)0.0f, (float)3.0f));
        bodyData.addChild("back", ModelPartBuilder.create().uv(3, 19).cuboid(-3.0f, -3.0f, -0.5f, 6.0f, 6.0f, 11.0f), ModelTransform.pivot((float)0.0f, (float)-0.5f, (float)5.5f));
        root.addChild("frontLeftLeg", ModelPartBuilder.create().uv(42, 0).cuboid(-1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f), ModelTransform.pivot((float)1.5f, (float)16.0f, (float)-3.0f));
        root.addChild("frontRightLeg", ModelPartBuilder.create().uv(42, 0).cuboid(-1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f), ModelTransform.pivot((float)-1.5f, (float)16.0f, (float)-3.0f));
        root.addChild("leftBackLeg", ModelPartBuilder.create().uv(52, 0).cuboid(-1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f), ModelTransform.pivot((float)1.5f, (float)16.0f, (float)9.0f));
        root.addChild("rightBackLeg", ModelPartBuilder.create().uv(52, 0).cuboid(-1.0f, 0.0f, -1.0f, 2.0f, 5.0f, 2.0f), ModelTransform.pivot((float)-1.5f, (float)16.0f, (float)9.0f));
        root.addChild("tail", ModelPartBuilder.create().uv(2, 12).cuboid(-1.0f, 2.0f, -1.0f, 2.0f, 8.0f, 2.0f), ModelTransform.of((float)0.0f, (float)9.0f, (float)10.0f, (float)0.3926991f, (float)0.0f, (float)0.0f));
        return modelData;
    }

    public void setAngles(float age, PetBrain brain) {
        this.head.yaw = brain.getYaw() * ((float)Math.PI / 180);
        this.head.pitch = brain.getPitch() * ((float)Math.PI / 180);
        float swing = brain.limbSwing;
        float swingAmount = brain.limbSwingAmount;
        this.frontLeftLeg.pitch = (float)Math.cos(swing * 0.6662f) * 1.4f * swingAmount;
        this.frontRightLeg.pitch = (float)Math.cos((double)(swing * 0.6662f) + Math.PI) * 1.4f * swingAmount;
        this.leftBackLeg.pitch = (float)Math.cos((double)(swing * 0.6662f) + Math.PI) * 1.4f * swingAmount;
        this.rightBackLeg.pitch = (float)Math.cos(swing * 0.6662f) * 1.4f * swingAmount;
        if (brain.isLay()) {
            this.frontLeftLeg.pitch = (float)Math.toRadians(-90.0);
            this.frontRightLeg.pitch = (float)Math.toRadians(-90.0);
            this.leftBackLeg.pitch = (float)Math.toRadians(90.0);
            this.rightBackLeg.pitch = (float)Math.toRadians(90.0);
            this.frontLeftLeg.yaw = (float)Math.toRadians(-22.0);
            this.frontRightLeg.yaw = (float)Math.toRadians(22.0);
            this.leftBackLeg.yaw = (float)Math.toRadians(22.0);
            this.rightBackLeg.yaw = (float)Math.toRadians(-22.0);
        } else {
            this.frontLeftLeg.yaw = 0.0f;
            this.frontRightLeg.yaw = 0.0f;
            this.leftBackLeg.yaw = 0.0f;
            this.rightBackLeg.yaw = 0.0f;
        }
        this.tail.pitch = (float)Math.toRadians(brain.isLay() ? 45.0 : 22.0);
        this.tail.roll = (float)(Math.toRadians(-22.5) + 0.39269908169872414 + Math.cos(age * 0.15f) * (double)0.3f);
    }

    public static RenderLayer getRenderLayer() {
        return RenderLayer.getEntityTranslucent((Identifier)TEXTURE);
    }

    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, PetBrain brain, float age) {
        matrices.push();
        matrices.scale(0.75f, 0.75f, 0.75f);
        this.setAngles(age, brain);
        net.minecraft.client.util.math.MatrixStack contact=new net.minecraft.client.util.math.MatrixStack();
        org.joml.Vector3f corner=new org.joml.Vector3f();
        float[] maximum={Float.NEGATIVE_INFINITY};
        for(ModelPart part:new ModelPart[]{head,neck,body,frontLeftLeg,frontRightLeg,leftBackLeg,rightBackLeg,tail})part.forEachCuboid(contact,(entry,path,index,cube) -> {
            for(ModelPart.Quad quad:cube.sides)for(ModelPart.Vertex vertex:quad.vertices()) {
                entry.getPositionMatrix().transformPosition(corner.set(vertex.pos()).mul(1F/16F));maximum[0]=Math.max(maximum[0],corner.y);
            }
        });
        float lift = maximum[0]+.025f;
        matrices.translate(0.0f, lift, 0.0f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(brain.getBody()));
        this.setAngles(age, brain);
        this.head.render(matrices, vertices, light, overlay);
        this.neck.render(matrices, vertices, light, overlay);
        this.body.render(matrices, vertices, light, overlay);
        this.frontLeftLeg.render(matrices, vertices, light, overlay);
        this.frontRightLeg.render(matrices, vertices, light, overlay);
        this.leftBackLeg.render(matrices, vertices, light, overlay);
        this.rightBackLeg.render(matrices, vertices, light, overlay);
        this.tail.render(matrices, vertices, light, overlay);
        matrices.pop();
    }
}
