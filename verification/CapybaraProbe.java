import com.spidiboost.littlepet.model.CapybaraModel;
import com.spidiboost.littlepet.PetBrain;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.util.math.MatrixStack;
import java.nio.file.*;
import java.util.*;
import com.google.gson.Gson;

 
public final class CapybaraProbe {
    static double minimum = Double.POSITIVE_INFINITY;
    static int samples;
    static ModelProbe check(CapybaraModel m, float yaw) {
        ModelProbe sink = new ModelProbe();
        m.renderModel(new MatrixStack(),sink,0xF000F0,0,yaw);
        if(sink.vertices.size()!=288) throw new AssertionError("Geometry missing: "+sink.vertices.size());
        for(float[] v : sink.vertices) {
            for(float c : v) if(!Float.isFinite(c)) throw new AssertionError("Nonfinite vertex");
            if(v[1]<.0123F) throw new AssertionError("Part went underground: Y="+v[1]);
            minimum=Math.min(minimum,v[1]);
        }
        samples++;
        return sink;
    }
    public static void main(String[] args) throws Exception {
        Path out=Path.of(args[0]);Files.createDirectories(out);
        CapybaraModel m = new CapybaraModel();
        
        for(int rest=0;rest<=10;rest++) for(int back=0;back<=10;back++)
            for(float movement : new float[]{0,.25F,.5F,1}) for(int phase=0;phase<48;phase++) for(int time=0;time<8;time++) {
                float swing=(float)(phase*Math.PI*2/48/.6662);
                float age=(float)(time*Math.PI*2/8/.09);
                m.animate(25,-15,swing,movement,age,rest/10F,back/10F);
                check(m,0);
            }
        System.out.printf("PASS: %d poses, 288 vertices each, minimum Y=%.6f blocks%n",samples,minimum);
        Map<String,float[]> poses = new LinkedHashMap<>();
        
        poses.put("standing",new float[]{0,0,0,0,10,0,0});
        poses.put("walking",new float[]{0,0,1.8F,.8F,12,0,0});
        poses.put("sitting",new float[]{0,0,0,0,20,1,0});
        poses.put("on_back",new float[]{0,0,0,0,25,1,1});
        poses.put("rolling",new float[]{0,0,0,0,25,1,.5F});
        for(var entry:poses.entrySet()) {
            float[] p=entry.getValue();m.animate(p[0],p[1],p[2],p[3],p[4],p[5],p[6]);
            ModelProbe sink=check(m,0);
            Files.writeString(out.resolve(entry.getKey()+".json"),new Gson().toJson(sink.vertices));
        }
        var rootField=CapybaraModel.class.getDeclaredField("root");rootField.setAccessible(true);
        ModelPart poseRoot=(ModelPart)rootField.get(m);
        for(float back : new float[]{0,1}) {
            m.animate(0,0,0,0,25,1,back);
            MatrixStack contact=new MatrixStack();
            contact.scale(CapybaraModel.SCALE,CapybaraModel.SCALE,CapybaraModel.SCALE);
            contact.translate(0,1.5,0);
            contact.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(180));
            poseRoot.rotate(contact);
            ModelProbe bodySink=new ModelProbe();
            poseRoot.getChild(EntityModelPartNames.BODY).render(contact,bodySink,0xF000F0,0);
            double bodyContact=bodySink.vertices.stream().mapToDouble(v -> v[1]).min().orElseThrow();
            if(bodyContact>.05) throw new AssertionError("Resting body floats: "+bodyContact);
            if(back==0) {
                for(String name : List.of(EntityModelPartNames.LEFT_FRONT_LEG,EntityModelPartNames.RIGHT_FRONT_LEG,
                        EntityModelPartNames.LEFT_HIND_LEG,EntityModelPartNames.RIGHT_HIND_LEG)) {
                    MatrixStack direction=new MatrixStack();poseRoot.rotate(direction);poseRoot.getChild(name).rotate(direction);
                    var dir=direction.peek().getPositionMatrix().transformDirection(new org.joml.Vector3f(0,1,0));
                    if(dir.z>-.95F) throw new AssertionError("Sitting paw not forwards: "+name);
                }
            }
            System.out.printf("PASS: %s body contacts ground at Y=%.6f%n",back==0?"sitting":"on back",bodyContact);
        }
        m.animate(0,0,0,0,10,0,0);
        for(int yaw:new int[]{0,90,180,270}) {
            ModelProbe sink=check(m,yaw);
            Files.writeString(out.resolve("direction_"+yaw+".json"),new Gson().toJson(sink.vertices));
        }
        var rf=CapybaraModel.class.getDeclaredField("root");rf.setAccessible(true);
        ModelPart root=(ModelPart)rf.get(m);
        PetBrain brain = new PetBrain();
        var lay=PetBrain.class.getDeclaredField("lay");lay.setAccessible(true);
        lay.setBoolean(brain,true);
        m.resetAnimation();
        for(int tick=0;tick<110;tick++) {m.updatePose(brain,tick);check(m,0);}
        if(root.pitch>-.95F || Math.abs(root.roll)>.01F) throw new AssertionError("AFK did not sit first");
        Files.writeString(out.resolve("afk_sit.json"),new Gson().toJson(check(m,0).vertices));
        for(int tick=110;tick<245;tick++) {m.updatePose(brain,tick);check(m,0);}
        if(root.roll<3.0F) throw new AssertionError("AFK did not play on back");
        Files.writeString(out.resolve("afk_back.json"),new Gson().toJson(check(m,0).vertices));
        ModelPart front=root.getChild(EntityModelPartNames.LEFT_FRONT_LEG);
        MatrixStack matrix=new MatrixStack();root.rotate(matrix);front.rotate(matrix);
        var pawDirection=matrix.peek().getPositionMatrix().transformDirection(new org.joml.Vector3f(0,1,0));
        if(pawDirection.y>-.8F) throw new AssertionError("Back paws do not point upwards");
        
        brain.limbSwingAmount=1;
        for(int tick=245;tick<285;tick++) {m.updatePose(brain,tick);check(m,0);}
        if(Math.abs(root.pitch)>.01F || Math.abs(root.roll)>.01F) throw new AssertionError("Movement did not wake pet");
        brain.limbSwingAmount=0;
        lay.setBoolean(brain,false);
        for(int tick=285;tick<345;tick++) {m.updatePose(brain,tick);check(m,0);}
        if(Math.abs(root.pitch)>.001F || Math.abs(root.roll)>.001F) throw new AssertionError("Standing did not recover");
        System.out.println("PASS: real PetBrain AFK -> sit -> back; movement interrupts; standing recovers");
    }
}
