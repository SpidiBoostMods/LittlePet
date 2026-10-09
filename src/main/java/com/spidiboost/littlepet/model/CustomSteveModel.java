package com.spidiboost.littlepet.model;

import com.spidiboost.littlepet.PetBrain;
import com.spidiboost.littlepet.custom.CustomFaceTexture;
import com.spidiboost.littlepet.custom.ImagePathSetting;
import com.spidiboost.littlepet.custom.SkinNameSetting;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;

 
public final class CustomSteveModel {
    public static final float SCALE=.9375F;
    private ModelPart root=TexturedModelData.of(PlayerEntityModel.getTexturedModelData(Dilation.NONE,false),64,64).createModel();
    private PlayerEntityModel model=new PlayerEntityModel(root,false);
    private final CustomFaceTexture face=new CustomFaceTexture();
    private final MatrixStack contact=new MatrixStack();
    private final Vector3f corner=new Vector3f();
    private float maximumY;

    private boolean slim;
    public CustomSteveModel() { this(false); }
    public CustomSteveModel(boolean narrow) {
        slim=narrow;
        root=TexturedModelData.of(PlayerEntityModel.getTexturedModelData(Dilation.NONE,slim),64,64).createModel();
        model=new PlayerEntityModel(root,slim);
    }
    public void updatePose(PetBrain brain,ImagePathSetting setting) { updatePose(brain,setting,null); }
    public void updatePose(PetBrain brain,ImagePathSetting setting,SkinNameSetting skin) {
        face.observe(setting,skin);
        if(slim!=face.isSlim()) {
            slim=face.isSlim();
            root=TexturedModelData.of(PlayerEntityModel.getTexturedModelData(Dilation.NONE,slim),64,64).createModel();
            model=new PlayerEntityModel(root,slim);
        }
        animate(brain.limbSwing,MathHelper.clamp(brain.limbSwingAmount,0,1),brain.getYaw(),brain.getPitch());
    }
    public void animate(float walk,float movement,float headYaw,float headPitch) {
        root.traverse().forEach(ModelPart::resetTransform);
        model.head.yaw=MathHelper.clamp(MathHelper.wrapDegrees(headYaw),-55,55)*MathHelper.RADIANS_PER_DEGREE;
        model.head.pitch=MathHelper.clamp(headPitch,-30,30)*MathHelper.RADIANS_PER_DEGREE;
        
        float phase=walk*.6662F;
        model.rightArm.pitch=(float)Math.cos(phase+Math.PI)*movement;
        model.leftArm.pitch=(float)Math.cos(phase)*movement;
        model.rightLeg.pitch=(float)Math.cos(phase)*1.4F*movement;
        model.leftLeg.pitch=(float)Math.cos(phase+Math.PI)*1.4F*movement;
        maximumY=Float.NEGATIVE_INFINITY;
        root.forEachCuboid(contact,(entry,path,index,cube) -> {
            for(ModelPart.Quad quad:cube.sides)for(ModelPart.Vertex vertex:quad.vertices()) {
                entry.getPositionMatrix().transformPosition(corner.set(vertex.pos()).mul(1F/16F));
                maximumY=Math.max(maximumY,corner.y);
            }
        });
        root.pivotY+=24F-.26F-maximumY*16F;
    }
    public RenderLayer getRenderLayer() { return face.getLayer(); }
    public void renderModel(MatrixStack matrices,VertexConsumer vertices,int light,int overlay,float bodyYaw) {
        matrices.push();matrices.scale(SCALE,SCALE,SCALE);matrices.translate(0,1.5,0);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(bodyYaw));
        root.render(matrices,vertices,light,overlay);matrices.pop();
    }
}
