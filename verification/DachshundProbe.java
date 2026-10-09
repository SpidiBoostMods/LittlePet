import com.spidiboost.littlepet.model.DachshundModel;
import com.spidiboost.littlepet.PetBrain;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.util.math.MatrixStack;
import java.nio.file.*;
import java.util.*;
import com.google.gson.Gson;

public final class DachshundProbe {
    static double minimum=Double.POSITIVE_INFINITY;
    static int samples;
    static ModelProbe check(DachshundModel model,float yaw) {
        ModelProbe sink=new ModelProbe();model.renderModel(new MatrixStack(),sink,0xF000F0,0,yaw);
        if(sink.vertices.size()!=1248) throw new AssertionError("Missing parts: "+sink.vertices.size());
        for(float[] v:sink.vertices) {
            for(float c:v) if(!Float.isFinite(c)) throw new AssertionError("Invalid vertex");
            if(v[1]<.0120F) throw new AssertionError("Part below ground: "+v[1]);
            if(v[3]<0 || v[3]>1 || v[4]<0 || v[4]>1) throw new AssertionError("UV outside texture");
            minimum=Math.min(minimum,v[1]);
        }
        samples++;return sink;
    }
    public static void main(String[] args) throws Exception {
        Path out=Path.of(args[0]);Files.createDirectories(out);
        DachshundModel m=new DachshundModel();
        Map<String,float[]> poses=new LinkedHashMap<>();
        poses.put("standing",new float[]{0,0,0,0,10,0});
        poses.put("walking",new float[]{0,0,1.8F,.8F,12,0});
        poses.put("play_bow",new float[]{0,0,0,0,25,1});
        poses.put("transition",new float[]{0,0,0,0,25,.5F});
        poses.put("look_left",new float[]{24,16,0,0,20,0});
        for(var entry:poses.entrySet()) {
            float[] p=entry.getValue();m.animate(p[0],p[1],p[2],p[3],p[4],p[5]);
            Files.writeString(out.resolve(entry.getKey()+".json"),new Gson().toJson(check(m,0).vertices));
        }
        m.animate(0,0,0,0,10,0);
        for(int yaw:new int[]{0,90,180,270}) Files.writeString(out.resolve("direction_"+yaw+".json"),new Gson().toJson(check(m,yaw).vertices));
        
        for(int bow=0;bow<=10;bow++) for(float move:new float[]{0,.25F,.5F,1})
            for(int phase=0;phase<48;phase++) for(int time=0;time<8;time++) for(int look=0;look<4;look++) {
                float swing=(float)(phase*Math.PI*2/48/.6662);
                m.animate((look&1)==0?-24:24,(look&2)==0?-12:16,swing,move,time*11.5F,bow/10F);
                check(m,0);
            }
        System.out.printf("PASS: %d poses, all 1248 vertices, minimum Y=%.6f%n",samples,minimum);
        PetBrain brain=new PetBrain();
        var lay=PetBrain.class.getDeclaredField("lay");lay.setAccessible(true);lay.setBoolean(brain,true);
        var rf=DachshundModel.class.getDeclaredField("root");rf.setAccessible(true);
        ModelPart torso=((ModelPart)rf.get(m)).getChild("torso");
        m.resetAnimation();
        for(int tick=0;tick<125;tick++){m.updatePose(brain,tick);check(m,0);}
        if(torso.pitch>-.22F) throw new AssertionError("No AFK bow");
        Files.writeString(out.resolve("afk_from_brain.json"),new Gson().toJson(check(m,0).vertices));
        brain.limbSwingAmount=1;
        for(int tick=125;tick<165;tick++){m.updatePose(brain,tick);check(m,0);}
        if(Math.abs(torso.pitch)>.001F) throw new AssertionError("Moving did not cancel bow");
        System.out.println("PASS: actual PetBrain AFK play bow and movement recovery");
    }
}
