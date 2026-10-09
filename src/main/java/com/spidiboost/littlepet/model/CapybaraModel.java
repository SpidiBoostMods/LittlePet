package com.spidiboost.littlepet.model;

import com.spidiboost.littlepet.PetBrain;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;





 
public final class CapybaraModel {
    public static final Identifier TEXTURE = Identifier.of("spidiboostlittlepet", "textures/capybara.png");
    public static final Identifier EYES = Identifier.of("spidiboostlittlepet", "textures/capybara-eyes.png");
    public static final Identifier CLOSED_EYES = Identifier.of("spidiboostlittlepet", "textures/capybara-eyes-closed.png");
    public static final float SCALE = 0.8F;
    private static final String LOWER_TEETH = "lower_teeth", UPPER_TEETH = "upper_teeth";
    private static final float GROUND_CLEARANCE = 0.25F; 
    private final ModelPart root, body, head, jaw, leftFront, rightFront, leftHind, rightHind;
    private final MatrixStack contactTransforms = new MatrixStack();
    private final Vector3f corner = new Vector3f();
    private final ModelPart.CuboidConsumer measure = this::measureCorners;
    private float lowestModelY;
    private float sitProgress, backProgress, idleTicks;
    private float previousAge = Float.NaN;

    public CapybaraModel() {
        root = getTexturedModelData().createModel().getChild(EntityModelPartNames.ROOT);
        body = root.getChild(EntityModelPartNames.BODY);
        head = root.getChild(EntityModelPartNames.HEAD);
        jaw = head.getChild(EntityModelPartNames.JAW);
        leftFront = root.getChild(EntityModelPartNames.LEFT_FRONT_LEG);
        rightFront = root.getChild(EntityModelPartNames.RIGHT_FRONT_LEG);
        leftHind = root.getChild(EntityModelPartNames.LEFT_HIND_LEG);
        rightHind = root.getChild(EntityModelPartNames.RIGHT_HIND_LEG);
    }

    public static RenderLayer getRenderLayer() { return RenderLayer.getEntityCutoutNoCull(TEXTURE); }
    public RenderLayer getEyeLayer(float age) {
        
        boolean blink = age % 93.0F < 2.5F;
        return RenderLayer.getEntityCutoutNoCull(blink ? CLOSED_EYES : EYES);
    }

    public void resetAnimation() {
        sitProgress = backProgress = idleTicks = 0.0F;
        previousAge = Float.NaN;
    }

    public void updatePose(PetBrain brain, float age) {
        float dt = Float.isNaN(previousAge) ? 1.0F : MathHelper.clamp(age - previousAge, 0.0F, 2.0F);
        previousAge = age;
        boolean rest = brain.isLay() && brain.limbSwingAmount < 0.02F;
        idleTicks = rest ? idleTicks + dt : 0.0F;
        
        float cycle = idleTicks % 400.0F;
        boolean play = rest && cycle >= 160.0F && cycle < 280.0F;
        float smoothing = rest ? 0.10F : 0.22F;
        float blend = 1.0F - (float) Math.exp(-dt * smoothing);
        sitProgress = MathHelper.lerp(blend, sitProgress, rest ? 1.0F : 0.0F);
        backProgress = MathHelper.lerp(blend, backProgress, play ? 1.0F : 0.0F);
        animate(brain.getYaw(), brain.getPitch(), brain.limbSwing,
                MathHelper.clamp(brain.limbSwingAmount, 0.0F, 1.0F), age, sitProgress, backProgress);
    }

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData root = modelPartData.addChild(EntityModelPartNames.ROOT, ModelPartBuilder.create(), ModelTransform.pivot(0.5F, 14.5F, 0.5F));

		root.addChild(EntityModelPartNames.RIGHT_HIND_LEG, ModelPartBuilder.create().uv(31, 0).cuboid(-1.5F, 0.0F, -0.5F, 3.0F, 7.0F, 3.0F), ModelTransform.pivot(1.5F, 2.5F, 5.5F));
		root.addChild(EntityModelPartNames.LEFT_HIND_LEG, ModelPartBuilder.create().uv(0, 0).cuboid(-1.5F, 0.0F, -0.5F, 3.0F, 7.0F, 3.0F), ModelTransform.pivot(-2.5F, 2.5F, 5.5F));
		root.addChild(EntityModelPartNames.RIGHT_FRONT_LEG, ModelPartBuilder.create().uv(0, 36).cuboid(-1.5F, -1.0F, -2.0F, 3.0F, 6.0F, 3.0F), ModelTransform.pivot(1.5F, 4.5F, -4.5F));
		root.addChild(EntityModelPartNames.LEFT_FRONT_LEG, ModelPartBuilder.create().uv(12, 36).cuboid(-1.5F, -1.0F, -2.0F, 3.0F, 6.0F, 3.0F), ModelTransform.pivot(-2.5F, 4.5F, -4.5F));

		root.addChild(EntityModelPartNames.BODY, ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -4.0F, -7.5F, 8.0F, 8.0F, 15.0F), ModelTransform.pivot(-0.5F, 0.5F, 0.0F));
		ModelPartData head = root.addChild(EntityModelPartNames.HEAD, ModelPartBuilder.create()
						.uv(20, 23).cuboid(-3.0F, 1.0F, -5.0F, 6.0F, 1.0F, 5.0F)
						.uv(0, 23).cuboid(-3.0F, -4.0F, -8.0F, 6.0F, 5.0F, 8.0F),
				ModelTransform.of(-0.5F, -2.5F, -6.5F, -0.0436F, 0.0F, 0.0F));
		ModelPartData jaw = head.addChild(EntityModelPartNames.JAW, ModelPartBuilder.create().uv(28, 29).cuboid(-3.0F, 0.0F, -3.0F, 6.0F, 1.0F, 3.0F), ModelTransform.pivot(0.0F, 1.0F, -5.0F));

		head.addChild(EntityModelPartNames.LEFT_EAR, ModelPartBuilder.create().uv(0, 10).cuboid(0.0F, -2.0F, 0.0F, 1.0F, 2.0F, 2.0F), ModelTransform.of(2.0F, -3.0F, -1.0F, -0.2F, 0.2F, 0.0F));
		head.addChild(EntityModelPartNames.RIGHT_EAR, ModelPartBuilder.create().uv(6, 10).cuboid(-1.0F, -2.0F, 0.0F, 1.0F, 2.0F, 2.0F), ModelTransform.of(-2.0F, -3.0F, -1.0F, -0.2F, -0.2F, 0.0F));
		head.addChild(UPPER_TEETH, ModelPartBuilder.create().uv(9, 0).cuboid(-1.0F, -0.25F, 0.0F, 2.0F, 1.0F, 0.0F), ModelTransform.pivot(0.0F, 1.0F, -7.0F));
		jaw.addChild(LOWER_TEETH, ModelPartBuilder.create().uv(9, 1).cuboid(-1.0F, -0.75F, 0.0F, 2.0F, 1.0F, 0.0F), ModelTransform.pivot(0.0F, 0.0F, -2.0F));

		return TexturedModelData.of(modelData, 64, 64);
	}

     
    public void animate(float headYaw, float headPitch, float limbSwing, float movement,
                        float age, float rest, float back) {
        root.traverse().forEach(ModelPart::resetTransform);
        rest = MathHelper.clamp(rest, 0.0F, 1.0F);
        back = MathHelper.clamp(back, 0.0F, 1.0F) * rest;
        float sitting = rest * (1.0F - back);
        float walking = MathHelper.clamp(movement, 0.0F, 1.0F) * (1.0F - rest);
        float phase = limbSwing * 0.6662F;
        float stride = (float) Math.cos(phase) * 0.45F * walking;
        float lean = -MathHelper.RADIANS_PER_DEGREE * 60.0F * sitting;
        root.pitch = lean;
        root.roll = back * (MathHelper.PI + (float) Math.sin(age * 0.11F) * 0.045F);
        
        float forwardPaws = -MathHelper.RADIANS_PER_DEGREE * 30.0F * sitting;
        float wiggle = (float) Math.sin(age * 0.23F) * 0.24F * back;
        leftFront.pitch = forwardPaws + stride + wiggle;
        rightHind.pitch = forwardPaws + stride - wiggle;
        rightFront.pitch = forwardPaws - stride - wiggle;
        leftHind.pitch = forwardPaws - stride + wiggle;
        leftFront.roll = leftHind.roll = -0.18F * back;
        rightFront.roll = rightHind.roll = 0.18F * back;
        float liftA = Math.max(0.0F, (float) Math.sin(phase)) * 0.65F * walking;
        float liftB = Math.max(0.0F, -(float) Math.sin(phase)) * 0.65F * walking;
        leftFront.pivotY -= liftA;
        rightHind.pivotY -= liftA;
        rightFront.pivotY -= liftB;
        leftHind.pivotY -= liftB;
        
        head.pitch = -0.0436F - lean * 0.85F
                + MathHelper.clamp(headPitch, -15.0F, 20.0F) * MathHelper.RADIANS_PER_DEGREE * (1.0F - rest)
                + (float) Math.sin(age * 0.07F) * 0.025F * rest;
        head.yaw = MathHelper.clamp(MathHelper.wrapDegrees(headYaw), -25.0F, 25.0F)
                * MathHelper.RADIANS_PER_DEGREE * (1.0F - back);
        head.pivotY += 4.1F * back;
        jaw.pitch = 0.035F + back * 0.035F;
        body.yScale = 1.0F + (float) Math.sin(age * 0.09F) * 0.008F * rest;
        head.getChild(EntityModelPartNames.LEFT_EAR).roll = (float) Math.sin(age * 0.065F) * 0.025F;
        head.getChild(EntityModelPartNames.RIGHT_EAR).roll = -(float) Math.sin(age * 0.065F) * 0.025F;
        placePoseAboveGround();
    }

    private void placePoseAboveGround() {
        lowestModelY = Float.NEGATIVE_INFINITY;
        root.forEachCuboid(contactTransforms, measure);
        
        
        root.pivotY += 24.0F - GROUND_CLEARANCE - lowestModelY * 16.0F;
    }

    private void measureCorners(MatrixStack.Entry transform, String path, int index, ModelPart.Cuboid cube) {
        for (int c = 0; c < 8; c++) {
            corner.set(((c & 1) == 0 ? cube.minX : cube.maxX) / 16.0F,
                    ((c & 2) == 0 ? cube.minY : cube.maxY) / 16.0F,
                    ((c & 4) == 0 ? cube.minZ : cube.maxZ) / 16.0F);
            transform.getPositionMatrix().transformPosition(corner);
            lowestModelY = Math.max(lowestModelY, corner.y);
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
