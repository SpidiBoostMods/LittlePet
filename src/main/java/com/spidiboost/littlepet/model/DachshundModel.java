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
import java.util.Map;



 
public final class DachshundModel {
    public static final Identifier TEXTURE = Identifier.of("spidiboostlittlepet", "textures/dachshund2.png");
    public static final float SCALE = 1.5F;
    private static final float CLEARANCE = 0.13F; 
    private final ModelPart root, torso, head, tail, leftFront, rightFront, leftHind, rightHind;
    private final ModelPart leftFrontPaw, rightFrontPaw, leftHindPaw, rightHindPaw;
    private final ModelPart leftEar, rightEar;
    private final MatrixStack contact = new MatrixStack();
    private final Vector3f corner = new Vector3f();
    private final ModelPart.CuboidConsumer measure = this::measureVertices;
    private float lowestY;
    private float idleTicks, bowProgress, previousAge = Float.NaN;

    public DachshundModel() {
        root = createGeometry().createModel().getChild("root");
        torso=root.getChild("torso");head=torso.getChild("head_joint");
        tail=root.getChild("bunda").getChild("tail");
        leftFront=torso.getChild("front_leg_1");rightFront=torso.getChild("front_leg_2");
        leftHind=root.getChild("back_leg_3");rightHind=root.getChild("back_leg_2");
        leftFrontPaw=leftFront.getChild("front_leg_1_paw");rightFrontPaw=rightFront.getChild("front_leg_2_paw");
        leftHindPaw=leftHind.getChild("back_leg_3_paw");rightHindPaw=rightHind.getChild("back_leg_2_paw");
        leftEar=head.getChild("ear2");rightEar=head.getChild("ear1");
        
        root.forEachCuboid(new MatrixStack(), (matrix,path,index,cube) -> {
            String name=path.substring(path.lastIndexOf('/')+1);
            float[] uv=BOX_UVS.get(name);
            if(uv!=null) bakeBedrockUvs(cube,uv);
            if(name.equals("torso__cube_1")) {
                
                float rear=cube.minZ-uv[5];
                float cx=(cube.minX+cube.maxX)*.5F,cy=(cube.minY+cube.maxY)*.5F;
                var adjusted=new java.util.IdentityHashMap<Vector3f,Boolean>();
                for(ModelPart.Quad q:cube.sides) for(ModelPart.Vertex v:q.vertices()) {
                    Vector3f point=v.pos();
                    if(adjusted.put(point,Boolean.TRUE)!=null) continue;
                    if(Math.abs(point.z-rear)<.00001F) point.z=rear-.5F;
                    
                    point.x+=point.x<cx?.008F:-.008F;
                    point.y+=point.y<cy?.008F:-.008F;
                }
            }
        });
    }
    public static RenderLayer getRenderLayer() {return RenderLayer.getEntityCutoutNoCull(TEXTURE);}
    public void resetAnimation() {idleTicks=bowProgress=0;previousAge=Float.NaN;}
    public void updatePose(PetBrain brain,float age) {
        float dt=Float.isNaN(previousAge)?1:MathHelper.clamp(age-previousAge,0,2);
        previousAge=age;
        boolean afk=brain.isLay() && brain.limbSwingAmount<.02F;
        idleTicks=afk?idleTicks+dt:0;
        
        float phase=idleTicks%320;
        float target=afk && phase>=45 && phase<145 ? 1 : 0;
        bowProgress=MathHelper.lerp(1-(float)Math.exp(-dt*(afk?.11F:.24F)),bowProgress,target);
        animate(brain.getYaw(),brain.getPitch(),brain.limbSwing,
                MathHelper.clamp(brain.limbSwingAmount,0,1),age,bowProgress);
    }
    public void animate(float headYaw,float headPitch,float swing,float movement,float age,float bow) {
        root.traverse().forEach(ModelPart::resetTransform);
        bow=MathHelper.clamp(bow,0,1);
        movement=MathHelper.clamp(movement,0,1)*(1-bow);
        float phase=swing*.6662F;
        float stride=(float)Math.cos(phase)*.35F*movement;
        torso.pitch=-13*MathHelper.RADIANS_PER_DEGREE*bow;
        float foreStretch=73*MathHelper.RADIANS_PER_DEGREE*bow;
        leftFront.pitch=stride+foreStretch;
        rightFront.pitch=-stride+foreStretch;
        rightHind.pitch=stride;
        leftHind.pitch=-stride;
        float liftA=Math.max(0,(float)Math.sin(phase))*.22F*movement;
        float liftB=Math.max(0,-(float)Math.sin(phase))*.22F*movement;
        leftFront.pivotY-=liftA;rightHind.pivotY-=liftA;
        rightFront.pivotY-=liftB;leftHind.pivotY-=liftB;
        
        leftFrontPaw.pitch=-torso.pitch-leftFront.pitch;
        rightFrontPaw.pitch=-torso.pitch-rightFront.pitch;
        leftHindPaw.pitch=-leftHind.pitch;
        rightHindPaw.pitch=-rightHind.pitch;
        head.yaw=MathHelper.clamp(MathHelper.wrapDegrees(headYaw),-24,24)*MathHelper.RADIANS_PER_DEGREE;
        head.pitch=-torso.pitch+MathHelper.clamp(headPitch,-12,16)*MathHelper.RADIANS_PER_DEGREE*(1-bow);
        head.roll=(float)Math.sin(age*.045F)*.025F+(float)Math.sin(age*.08F)*.04F*bow;
        tail.yaw=(float)Math.sin(age*(.16F+bow*.14F))*(.12F+.30F*bow);
        tail.pitch=.08F*bow;
        leftEar.roll=(float)Math.sin(age*.11F)*.05F+stride*.09F;
        rightEar.roll=-(float)Math.sin(age*.11F-.4F)*.05F-stride*.09F;
        placeAboveGround();
    }
    private void placeAboveGround() {
        lowestY=Float.NEGATIVE_INFINITY;
        root.forEachCuboid(contact,measure);
        root.pivotY+=24-CLEARANCE-lowestY*16;
    }
    private void measureVertices(MatrixStack.Entry matrix,String path,int index,ModelPart.Cuboid cube) {
        
        for(ModelPart.Quad quad:cube.sides) for(ModelPart.Vertex vertex:quad.vertices()) {
            corner.set(vertex.pos()).mul(1F/16);
            matrix.getPositionMatrix().transformPosition(corner);
            lowestY=Math.max(lowestY,corner.y);
        }
    }
    public void renderModel(MatrixStack matrices,VertexConsumer vertices,int light,int overlay,float bodyYaw) {
        matrices.push();matrices.scale(SCALE,SCALE,SCALE);matrices.translate(0,1.5,0);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(bodyYaw));
        
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
        root.render(matrices,vertices,light,overlay);matrices.pop();
    }
    private static void bakeBedrockUvs(ModelPart.Cuboid cube,float[] box) {
        float u=box[0],v=box[1],w=box[2],h=box[3],d=box[4],inflate=box[5];
        float x0=cube.minX-inflate,x1=cube.maxX+inflate;
        float y0=cube.minY-inflate,y1=cube.maxY+inflate;
        float z0=cube.minZ-inflate,z1=cube.maxZ+inflate;
        Vector3f[] points={new Vector3f(x0,y1,z0),new Vector3f(x0,y1,z1),
                new Vector3f(x0,y0,z0),new Vector3f(x0,y0,z1),new Vector3f(x1,y0,z0),
                new Vector3f(x1,y0,z1),new Vector3f(x1,y1,z0),new Vector3f(x1,y1,z1)};
        int[][] faces={{3,2,0,1},{4,5,7,6},{2,4,6,0},{5,3,1,7},{3,5,4,2},{0,6,7,1}};
        float[][] atlas={{u+d+w,v+d,d,h},{u,v+d,d,h},{u+d,v+d,w,h},
                {u+2*d+w,v+d,w,h},{u+d,v,w,d},{u+d+w,v+d,w,-d}};
        Vector3f[] normals={new Vector3f(-1,0,0),new Vector3f(1,0,0),new Vector3f(0,0,-1),
                new Vector3f(0,0,1),new Vector3f(0,-1,0),new Vector3f(0,1,0)};
        for(int face=0;face<6;face++) {
            float[] a=atlas[face];float u0=a[0]/128,u1=(a[0]+a[2])/128,v0=a[1]/128,v1=(a[1]+a[3])/128;
            ModelPart.Vertex[] vs={new ModelPart.Vertex(points[faces[face][0]],u1,v0),
                    new ModelPart.Vertex(points[faces[face][1]],u0,v0),
                    new ModelPart.Vertex(points[faces[face][2]],u0,v1),
                    new ModelPart.Vertex(points[faces[face][3]],u1,v1)};
            cube.sides[face]=new ModelPart.Quad(vs,normals[face]);
        }
    }
    private static TexturedModelData createGeometry() {
        ModelData data = new ModelData();
        ModelPartData rootData = data.getRoot().addChild("root", ModelPartBuilder.create(), ModelTransform.pivot(0,24,0));
        ModelPartData b1 = rootData.addChild("torso", ModelPartBuilder.create(), ModelTransform.of(0.0F, -2.2F, -2.2F, 0.0F, 0.0F, 0.0F));
        b1.addChild("torso__cube_0", ModelPartBuilder.create().uv(0, 24).cuboid(-5F, -4.8F, -0.6F, 10F, 10F, 11F, new Dilation(-4F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b1.addChild("torso__cube_1", ModelPartBuilder.create().uv(0, 0).cuboid(-5F, -5.1F, -3.8F, 10F, 10F, 14F, new Dilation(-4.1F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        ModelPartData b2 = b1.addChild("bone4", ModelPartBuilder.create(), ModelTransform.of(0.0F, 0.0F, 5.6F, 0.0F, 0.0F, 0.0F));
        b2.addChild("bone4__cube_0", ModelPartBuilder.create().uv(32, 65).cuboid(-1F, -2.4F, -1F, 2F, 2.4F, 2F, new Dilation(-0.2F)), ModelTransform.of(0.0F, 0.0F, 0.0F, -0.12217305F, 0.0F, 0.0F));
        ModelPartData b3 = rootData.addChild("bunda", ModelPartBuilder.create(), ModelTransform.of(0.0F, -1.7F, -2.8F, 0.0F, 0.0F, 0.0F));
        b3.addChild("bunda__cube_0", ModelPartBuilder.create().uv(42, 24).cuboid(-5F, -5.6F, -5F, 10F, 10F, 10F, new Dilation(-4.1F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        ModelPartData b4 = b3.addChild("tail", ModelPartBuilder.create(), ModelTransform.of(0.0F, -1.3F, -0.85F, 0.0F, 0.0F, 0.0F));
        b4.addChild("tail__cube_0", ModelPartBuilder.create().uv(82, 34).cuboid(-0.5F, -0.5F, -1.75F, 1F, 1F, 2F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b4.addChild("tail__cube_1", ModelPartBuilder.create().uv(72, 22).cuboid(-0.5F, -0.7F, -2.15F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b4.addChild("tail__cube_2", ModelPartBuilder.create().uv(76, 22).cuboid(-0.5F, -1F, -2.55F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b4.addChild("tail__cube_3", ModelPartBuilder.create().uv(82, 37).cuboid(-0.5F, -1.2F, -2.95F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b4.addChild("tail__cube_4", ModelPartBuilder.create().uv(82, 39).cuboid(-0.5F, -1.4F, -3.25F, 1F, 1F, 1F, new Dilation(-0.4F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        ModelPartData b5 = b3.addChild("bone3", ModelPartBuilder.create(), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        ModelPartData b6 = rootData.addChild("back_leg_2", ModelPartBuilder.create(), ModelTransform.of(-0.7F, -2.2F, -3.1F, 0.0F, 0.0F, 0.0F));
        b6.addChild("back_leg_2__cube_0", ModelPartBuilder.create().uv(48, 0).cuboid(-1F, -8F, -2F, 8F, 9F, 9F, new Dilation(-3.6F)), ModelTransform.of(-3F, 3.6F, -2.5F, -0.017453293F, 0.0F, 0.0F));
        b6.addChild("back_leg_2__cube_1", ModelPartBuilder.create().uv(72, 18).cuboid(-0.75F, -1F, -2F, 2F, 2F, 2F, new Dilation(-0.7F)), ModelTransform.of(-0.2F, 1.7F, 1F, -0.6981317F, 0.0F, 0.0F));
        b6.addChild("back_leg_2__cube_2", ModelPartBuilder.create().uv(82, 10).cuboid(-1F, -3F, -2F, 4F, 4F, 5F, new Dilation(-1.7F)), ModelTransform.of(-1F, 0.3F, 0.0F, 3.1241394F, 0.0F, 0.0F));
        b6.addChild("back_leg_2__cube_3", ModelPartBuilder.create().uv(42, 44).cuboid(-1F, -9F, -2F, 10F, 10F, 10F, new Dilation(-4.6F)), ModelTransform.of(-4F, -1.55F, -5.15F, -1.5707963F, 0.0F, 0.0F));
        ModelPartData b7 = rootData.addChild("back_leg_3", ModelPartBuilder.create(), ModelTransform.of(0.7F, -2.2F, -3.1F, 0.0F, 0.0F, 0.0F));
        b7.addChild("back_leg_3__cube_0", ModelPartBuilder.create().uv(40, 64).cuboid(-7F, -8F, -2F, 8F, 9F, 9F, new Dilation(-3.6F)), ModelTransform.of(3F, 3.6F, -2.5F, -0.017453293F, 0.0F, 0.0F));
        b7.addChild("back_leg_3__cube_1", ModelPartBuilder.create().uv(32, 73).cuboid(-1.25F, -1F, -2F, 2F, 2F, 2F, new Dilation(-0.7F)), ModelTransform.of(0.2F, 1.7F, 1F, -0.6981317F, 0.0F, 0.0F));
        b7.addChild("back_leg_3__cube_2", ModelPartBuilder.create().uv(82, 19).cuboid(-3F, -3F, -2F, 4F, 4F, 5F, new Dilation(-1.7F)), ModelTransform.of(1F, 0.3F, 0.0F, 3.1241394F, 0.0F, 0.0F));
        b7.addChild("back_leg_3__cube_3", ModelPartBuilder.create().uv(0, 45).cuboid(-9F, -9F, -2F, 10F, 10F, 10F, new Dilation(-4.6F)), ModelTransform.of(4F, -1.55F, -5.15F, -1.5707963F, 0.0F, 0.0F));
        ModelPartData b8 = b1.addChild("front_leg_2", ModelPartBuilder.create(), ModelTransform.of(-0.8F, -0.1F, 4.9F, 0.0F, 0.0F, 0.0F));
        b8.addChild("front_leg_2__cube_0", ModelPartBuilder.create().uv(74, 64).cuboid(-4F, -4.3F, -4F, 8F, 10F, 8F, new Dilation(-3.6F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        ModelPartData b9 = b1.addChild("front_leg_1", ModelPartBuilder.create(), ModelTransform.of(0.8F, -0.1F, 4.9F, 0.0F, 0.0F, 0.0F));
        b9.addChild("front_leg_1__cube_0", ModelPartBuilder.create().uv(0, 65).cuboid(-4F, -4.3F, -4F, 8F, 10F, 8F, new Dilation(-3.6F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        ModelPartData b10 = b1.addChild("head_joint", ModelPartBuilder.create(), ModelTransform.of(0.0F, -1.6F, 5.2F, 0.0F, 0.0F, 0.0F));
        ModelPartData b11 = b10.addChild("head", ModelPartBuilder.create(), ModelTransform.of(0.0F, 3.4F, -3F, 0.0F, 0.0F, 0.0F));
        b11.addChild("head__cube_0", ModelPartBuilder.create().uv(82, 0).cuboid(-2.5F, -7F, 1.3F, 5F, 5F, 5F, new Dilation(-1.5F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        ModelPartData b12 = b11.addChild("stout", ModelPartBuilder.create(), ModelTransform.of(-0.05F, -3.1F, 4.3F, 0.0F, 0.0F, 0.0F));
        b12.addChild("stout__cube_0", ModelPartBuilder.create().uv(48, 18).cuboid(-1F, -2F, -2F, 2F, 2F, 4F, new Dilation(-0.5F)), ModelTransform.of(0.1F, -0.1F, 0.2F, -0.087266463F, 0.0F, 0.0F));
        ModelPartData b13 = b12.addChild("mouth", ModelPartBuilder.create(), ModelTransform.of(0.1F, 0.0F, 0.4F, 0.0F, 0.0F, 0.0F));
        b13.addChild("mouth__cube_0", ModelPartBuilder.create().uv(60, 18).cuboid(-1F, -2F, -2F, 2F, 2F, 4F, new Dilation(-0.6F)), ModelTransform.of(0.0F, 0.2F, -0.2F, 0.043633231F, 0.0F, 0.0F));
        ModelPartData b14 = b13.addChild("bone", ModelPartBuilder.create(), ModelTransform.of(0.0F, 0.2F, -0.2F, 0.0F, 0.0F, 0.0F));
        ModelPartData b15 = b10.addChild("nose", ModelPartBuilder.create(), ModelTransform.of(0.0F, 3.2F, -3.2F, 0.0F, 0.0F, 0.0F));
        b15.addChild("nose__cube_0", ModelPartBuilder.create().uv(32, 69).cuboid(-1F, -2F, 0.0F, 2F, 2F, 2F, new Dilation(-0.7F)), ModelTransform.of(0.1F, -3.3F, 5F, -0.087266463F, 0.0F, 0.0F));
        ModelPartData b16 = b10.addChild("ear1", ModelPartBuilder.create(), ModelTransform.of(-1F, -1.5F, 0.7F, 0.0F, 0.0F, 0.0F));
        b16.addChild("ear1__cube_0", ModelPartBuilder.create().uv(40, 82).cuboid(-0.7F, -0.8F, -0.6F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b16.addChild("ear1__cube_1", ModelPartBuilder.create().uv(82, 41).cuboid(-0.8F, -0.4F, -0.9F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b16.addChild("ear1__cube_2", ModelPartBuilder.create().uv(82, 43).cuboid(-0.9F, 0.0F, -0.6F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b16.addChild("ear1__cube_3", ModelPartBuilder.create().uv(44, 82).cuboid(-0.8F, 0.4F, -0.4F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b16.addChild("ear1__cube_4", ModelPartBuilder.create().uv(82, 45).cuboid(-0.7F, -0.8F, -0.2F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b16.addChild("ear1__cube_5", ModelPartBuilder.create().uv(48, 82).cuboid(-0.8F, 0.8F, 0.0F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b16.addChild("ear1__cube_6", ModelPartBuilder.create().uv(82, 55).cuboid(-0.8F, 0.7F, 0.1F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b16.addChild("ear1__cube_7", ModelPartBuilder.create().uv(82, 47).cuboid(-0.8F, 0.4F, 0.0F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b16.addChild("ear1__cube_8", ModelPartBuilder.create().uv(82, 49).cuboid(-0.9F, -0.4F, -0.6F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b16.addChild("ear1__cube_9", ModelPartBuilder.create().uv(82, 51).cuboid(-0.7F, -0.4F, -0.2F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b16.addChild("ear1__cube_10", ModelPartBuilder.create().uv(52, 82).cuboid(-0.8F, 0.0F, -0.2F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b16.addChild("ear1__cube_11", ModelPartBuilder.create().uv(82, 53).cuboid(-0.8F, 0.0F, -0.1F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        ModelPartData b17 = b10.addChild("ear2", ModelPartBuilder.create(), ModelTransform.of(1F, -1.5F, 0.7F, 0.0F, 0.0F, 0.0F));
        b17.addChild("ear2__cube_0", ModelPartBuilder.create().uv(56, 82).cuboid(-0.3F, -0.8F, -0.6F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b17.addChild("ear2__cube_1", ModelPartBuilder.create().uv(82, 57).cuboid(-0.1F, -0.4F, -0.9F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b17.addChild("ear2__cube_2", ModelPartBuilder.create().uv(82, 59).cuboid(-0.1F, 0.0F, -0.6F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b17.addChild("ear2__cube_3", ModelPartBuilder.create().uv(60, 82).cuboid(-0.2F, 0.4F, -0.4F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b17.addChild("ear2__cube_4", ModelPartBuilder.create().uv(82, 61).cuboid(-0.3F, -0.8F, -0.2F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b17.addChild("ear2__cube_5", ModelPartBuilder.create().uv(64, 82).cuboid(-0.2F, 0.8F, 0.0F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b17.addChild("ear2__cube_6", ModelPartBuilder.create().uv(68, 82).cuboid(-0.2F, 0.7F, 0.1F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b17.addChild("ear2__cube_7", ModelPartBuilder.create().uv(72, 82).cuboid(-0.2F, 0.4F, 0.0F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b17.addChild("ear2__cube_8", ModelPartBuilder.create().uv(76, 82).cuboid(-0.1F, -0.4F, -0.6F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b17.addChild("ear2__cube_9", ModelPartBuilder.create().uv(80, 82).cuboid(-0.3F, -0.4F, -0.2F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b17.addChild("ear2__cube_10", ModelPartBuilder.create().uv(0, 83).cuboid(-0.2F, 0.0F, -0.2F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        b17.addChild("ear2__cube_11", ModelPartBuilder.create().uv(4, 83).cuboid(-0.2F, 0.0F, -0.1F, 1F, 1F, 1F, new Dilation(-0.3F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        ModelPartData b18 = b10.addChild("eyelid", ModelPartBuilder.create(), ModelTransform.of(0.0F, 3.8F, -3.3F, 0.0F, 0.0F, 0.0F));
        b18.addChild("eyelid__cube_0", ModelPartBuilder.create().uv(94, 36).cuboid(-2.5F, -7.4F, 1.4F, 5F, 5F, 5F, new Dilation(-1.75F)), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        ModelPartData b19 = b9.addChild("front_leg_1_paw", ModelPartBuilder.create(), ModelTransform.of(0.1F, 2.1F, -0.3F, 0.0F, 0.0F, 0.0F));
        b19.addChild("front_leg_1_paw__cube_0", ModelPartBuilder.create().uv(82, 28).cuboid(-1F, 0.0F, -2F, 2F, 1.6F, 2.25F, new Dilation(-0.6F)), ModelTransform.of(0.0F, 0.8F, -0.4F, 3.1415927F, 0.0F, 0.0F));
        ModelPartData b20 = b8.addChild("front_leg_2_paw", ModelPartBuilder.create(), ModelTransform.of(-0.1F, 2.1F, -0.3F, 0.0F, 0.0F, 0.0F));
        b20.addChild("front_leg_2_paw__cube_0", ModelPartBuilder.create().uv(32, 80).cuboid(-1F, 0.0F, -2F, 2F, 1.6F, 2.25F, new Dilation(-0.6F)), ModelTransform.of(0.0F, 0.8F, -0.4F, 3.1415927F, 0.0F, 0.0F));
        ModelPartData b21 = b6.addChild("back_leg_2_paw", ModelPartBuilder.create(), ModelTransform.of(0.0F, 2F, -1F, 0.0F, 0.0F, 0.0F));
        b21.addChild("back_leg_2_paw__cube_0", ModelPartBuilder.create().uv(32, 77).cuboid(-1F, 0.0F, -2F, 2F, 1.6F, 2.25F, new Dilation(-0.6F)), ModelTransform.of(0.0F, 0.8F, -0.9F, 3.1415927F, 0.0F, 0.0F));
        ModelPartData b22 = b7.addChild("back_leg_3_paw", ModelPartBuilder.create(), ModelTransform.of(0.0F, 2F, -1F, 0.0F, 0.0F, 0.0F));
        b22.addChild("back_leg_3_paw__cube_0", ModelPartBuilder.create().uv(82, 31).cuboid(-1F, 0.0F, -2F, 2F, 1.6F, 2.25F, new Dilation(-0.6F)), ModelTransform.of(0.0F, 0.8F, -0.9F, 3.1415927F, 0.0F, 0.0F));
        return TexturedModelData.of(data,128,128);
    }

    private static final Map<String, float[]> BOX_UVS = Map.ofEntries(
            Map.entry("torso__cube_0", new float[]{0.0F, 24F, 10F, 10F, 11F, -4F}),
            Map.entry("torso__cube_1", new float[]{0.0F, 0.0F, 10F, 10F, 14F, -4.1F}),
            Map.entry("bone4__cube_0", new float[]{32F, 65F, 2F, 2.4F, 2F, -0.2F}),
            Map.entry("bunda__cube_0", new float[]{42F, 24F, 10F, 10F, 10F, -4.1F}),
            Map.entry("tail__cube_0", new float[]{82F, 34F, 1F, 1F, 2F, -0.3F}),
            Map.entry("tail__cube_1", new float[]{72F, 22F, 1F, 1F, 1F, -0.3F}),
            Map.entry("tail__cube_2", new float[]{76F, 22F, 1F, 1F, 1F, -0.3F}),
            Map.entry("tail__cube_3", new float[]{82F, 37F, 1F, 1F, 1F, -0.3F}),
            Map.entry("tail__cube_4", new float[]{82F, 39F, 1F, 1F, 1F, -0.4F}),
            Map.entry("back_leg_2__cube_0", new float[]{48F, 0.0F, 8F, 9F, 9F, -3.6F}),
            Map.entry("back_leg_2__cube_1", new float[]{72F, 18F, 2F, 2F, 2F, -0.7F}),
            Map.entry("back_leg_2__cube_2", new float[]{82F, 10F, 4F, 4F, 5F, -1.7F}),
            Map.entry("back_leg_2__cube_3", new float[]{42F, 44F, 10F, 10F, 10F, -4.6F}),
            Map.entry("back_leg_3__cube_0", new float[]{40F, 64F, 8F, 9F, 9F, -3.6F}),
            Map.entry("back_leg_3__cube_1", new float[]{32F, 73F, 2F, 2F, 2F, -0.7F}),
            Map.entry("back_leg_3__cube_2", new float[]{82F, 19F, 4F, 4F, 5F, -1.7F}),
            Map.entry("back_leg_3__cube_3", new float[]{0.0F, 45F, 10F, 10F, 10F, -4.6F}),
            Map.entry("front_leg_2__cube_0", new float[]{74F, 64F, 8F, 10F, 8F, -3.6F}),
            Map.entry("front_leg_1__cube_0", new float[]{0.0F, 65F, 8F, 10F, 8F, -3.6F}),
            Map.entry("head__cube_0", new float[]{82F, 0.0F, 5F, 5F, 5F, -1.5F}),
            Map.entry("stout__cube_0", new float[]{48F, 18F, 2F, 2F, 4F, -0.5F}),
            Map.entry("mouth__cube_0", new float[]{60F, 18F, 2F, 2F, 4F, -0.6F}),
            Map.entry("nose__cube_0", new float[]{32F, 69F, 2F, 2F, 2F, -0.7F}),
            Map.entry("ear1__cube_0", new float[]{40F, 82F, 1F, 1F, 1F, -0.3F}),
            Map.entry("ear1__cube_1", new float[]{82F, 41F, 1F, 1F, 1F, -0.3F}),
            Map.entry("ear1__cube_2", new float[]{82F, 43F, 1F, 1F, 1F, -0.3F}),
            Map.entry("ear1__cube_3", new float[]{44F, 82F, 1F, 1F, 1F, -0.3F}),
            Map.entry("ear1__cube_4", new float[]{82F, 45F, 1F, 1F, 1F, -0.3F}),
            Map.entry("ear1__cube_5", new float[]{48F, 82F, 1F, 1F, 1F, -0.3F}),
            Map.entry("ear1__cube_6", new float[]{82F, 55F, 1F, 1F, 1F, -0.3F}),
            Map.entry("ear1__cube_7", new float[]{82F, 47F, 1F, 1F, 1F, -0.3F}),
            Map.entry("ear1__cube_8", new float[]{82F, 49F, 1F, 1F, 1F, -0.3F}),
            Map.entry("ear1__cube_9", new float[]{82F, 51F, 1F, 1F, 1F, -0.3F}),
            Map.entry("ear1__cube_10", new float[]{52F, 82F, 1F, 1F, 1F, -0.3F}),
            Map.entry("ear1__cube_11", new float[]{82F, 53F, 1F, 1F, 1F, -0.3F}),
            Map.entry("ear2__cube_0", new float[]{56F, 82F, 1F, 1F, 1F, -0.3F}),
            Map.entry("ear2__cube_1", new float[]{82F, 57F, 1F, 1F, 1F, -0.3F}),
            Map.entry("ear2__cube_2", new float[]{82F, 59F, 1F, 1F, 1F, -0.3F}),
            Map.entry("ear2__cube_3", new float[]{60F, 82F, 1F, 1F, 1F, -0.3F}),
            Map.entry("ear2__cube_4", new float[]{82F, 61F, 1F, 1F, 1F, -0.3F}),
            Map.entry("ear2__cube_5", new float[]{64F, 82F, 1F, 1F, 1F, -0.3F}),
            Map.entry("ear2__cube_6", new float[]{68F, 82F, 1F, 1F, 1F, -0.3F}),
            Map.entry("ear2__cube_7", new float[]{72F, 82F, 1F, 1F, 1F, -0.3F}),
            Map.entry("ear2__cube_8", new float[]{76F, 82F, 1F, 1F, 1F, -0.3F}),
            Map.entry("ear2__cube_9", new float[]{80F, 82F, 1F, 1F, 1F, -0.3F}),
            Map.entry("ear2__cube_10", new float[]{0.0F, 83F, 1F, 1F, 1F, -0.3F}),
            Map.entry("ear2__cube_11", new float[]{4F, 83F, 1F, 1F, 1F, -0.3F}),
            Map.entry("eyelid__cube_0", new float[]{94F, 36F, 5F, 5F, 5F, -1.75F}),
            Map.entry("front_leg_1_paw__cube_0", new float[]{82F, 28F, 2F, 1.6F, 2.25F, -0.6F}),
            Map.entry("front_leg_2_paw__cube_0", new float[]{32F, 80F, 2F, 1.6F, 2.25F, -0.6F}),
            Map.entry("back_leg_2_paw__cube_0", new float[]{32F, 77F, 2F, 1.6F, 2.25F, -0.6F}),
            Map.entry("back_leg_3_paw__cube_0", new float[]{82F, 31F, 2F, 1.6F, 2.25F, -0.6F})
    );

}
