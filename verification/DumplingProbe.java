import com.spidiboost.littlepet.model.DumplingFamily;
import com.spidiboost.littlepet.PetBrain;
import net.minecraft.client.util.math.MatrixStack;
import com.google.gson.Gson;
import java.nio.file.*;
import java.util.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Quaternionf;

public class DumplingProbe {
    static double minY = 1e9;
    static void check(DumplingFamily model) {
        ModelProbe sink = new ModelProbe();
        model.renderModel(new MatrixStack(),sink,0xF000F0,0,0);
        if(sink.vertices.size()%4!=0 || sink.vertices.size()<20000)throw new AssertionError("Broken mesh");
        for(float[] v:sink.vertices) {
            for(float f:v)if(!Float.isFinite(f))throw new AssertionError("Nonfinite vertex");
            if(v[1]<.01438)throw new AssertionError("Underground vertex: "+v[1]);
            if(v[3]<0 || v[3]>1 || v[4]<0 || v[4]>1)throw new AssertionError("Bad UV");
            double norm=Math.sqrt(v[5]*v[5]+v[6]*v[6]+v[7]*v[7]);
            if(Math.abs(norm-1)>.0001)throw new AssertionError("Bad normal");
            minY=Math.min(minY,v[1]);
        }
    }
    public static void main(String[] args) throws Exception {
        Path out=Path.of(args[0]);Files.createDirectories(out);
        com.spidiboost.littlepet.PetConfig config=new com.spidiboost.littlepet.PetConfig();
        if(config.family || !config.enabled)throw new AssertionError("Invalid standalone defaults");
        config.family=true;config.pet="Пельмень";config.skin="Notch";
        String json=new Gson().toJson(config);
        var restored=new Gson().fromJson(json,com.spidiboost.littlepet.PetConfig.class);
        if(!restored.family || !restored.pet.equals("Пельмень") || !restored.skin.equals("Notch"))throw new AssertionError("Protected config persistence failed");
        System.out.println("PASS: standalone config JSON round trip");
        DumplingFamily model=new DumplingFamily();
        for(String name:new String[]{"standing","walking","handshake","hug","lift","pickup","lowering","alone"}) {
            int action=name.equals("handshake")?1:name.equals("hug")?2:name.equals("lift")||name.equals("pickup")||name.equals("lowering")?3:0;
            float t=name.equals("pickup")?.32F:name.equals("lowering")?.65F:.5F;
            model.sample(action,t,30,name.equals("walking")?.8F:0,!name.equals("alone"));
            check(model);ModelProbe sink=new ModelProbe();model.renderModel(new MatrixStack(),sink,0xF000F0,0,0);
            Files.writeString(out.resolve(name+".json"),new Gson().toJson(sink.vertices));
            if(name.equals("handshake"))System.out.println("Palm gap = "+model.palmDistance());
            if(name.equals("lift")||name.equals("pickup")||name.equals("lowering"))System.out.printf("%s support gaps %.5f/%.5f height %.3f%n",name,model.supportDistance(false),model.supportDistance(true),model.getLiftHeight());
            double maxY=sink.vertices.stream().mapToDouble(v->v[1]).max().orElseThrow();
            System.out.printf("%s: %d vertices; height %.4f%n",name,sink.vertices.size(),maxY);
        }
        
        model.sample(0,0,30,0,true);
        ModelProbe base = new ModelProbe(); model.renderModel(new MatrixStack(),base,0xF000F0,0,0);
        for(int yaw:new int[]{90,180,270}) {
            ModelProbe turned=new ModelProbe();model.renderModel(new MatrixStack(),turned,0xF000F0,0,yaw);
            double a=Math.toRadians(yaw),c=Math.cos(a),si=Math.sin(a);
            for(int i=0;i<base.vertices.size();i++) {
                float[] v=base.vertices.get(i),w=turned.vertices.get(i);
                if(Math.abs(w[0]-(v[0]*c-v[2]*si))>1e-5 || Math.abs(w[2]-(v[0]*si+v[2]*c))>1e-5)throw new AssertionError("Wrong forward direction at "+yaw);
            }
        }
        
        Map<String,List<Map<String,Object>>> clips = new LinkedHashMap<>();
        for(int action=0;action<=3;action++) {
            List<Map<String,Object>> frames = new ArrayList<>();
            int count=action==0?1:61;
            for(int frame=0;frame<count;frame++) {
                model.sample(action,frame/60F,30+frame,0,true);
                Map<String,Object> data = new LinkedHashMap<>();
                data.put("time",frame/60F*(action==1?9:action==2?10.5F:12.5F));
                Map<String,Object> nodes=new LinkedHashMap<>();
                for(String who:new String[]{"father","son"}) {
                    var rf=DumplingFamily.class.getDeclaredField(who);rf.setAccessible(true);Object rig=rf.get(model);
                    var bf=rig.getClass().getDeclaredField("bones");bf.setAccessible(true);
                    @SuppressWarnings("unchecked") Map<String,Matrix4f> bones=(Map<String,Matrix4f>)bf.get(rig);
                    for(var b:bones.entrySet()) {
                        Matrix4f matrix=b.getValue();Vector3f pos=matrix.getTranslation(new Vector3f()),scale=matrix.getScale(new Vector3f());
                        Quaternionf q=matrix.getUnnormalizedRotation(new Quaternionf()).normalize();
                        nodes.put(who+"/"+b.getKey(),new float[][]{{pos.x,pos.y,pos.z},{q.x,q.y,q.z,q.w},{scale.x,scale.y,scale.z}});
                    }
                }
                data.put("nodes",nodes);frames.add(data);
            }
            clips.put(action==0?"Standing":action==1?"Handshake":action==2?"Hug":"Lift",frames);
        }
        Files.writeString(out.resolve("rig-clips.json"),new Gson().toJson(clips));
        int poses=0;
        for(int action=0;action<=3;action++)for(int tick=0;tick<=200;tick++) {
            model.sample(action,tick/200F,tick,action==0?(tick%9)/8F:0,true);check(model);poses++;
            if(action==1&&tick>=64&&tick<=134&&model.palmDistance()>.015)throw new AssertionError("Handshake disconnected "+model.palmDistance());
            if(action==3&&tick>=64&&tick<=134 && Math.max(model.supportDistance(true),model.supportDistance(false))>.001)throw new AssertionError("Unsupported child at "+tick+": "+model.supportDistance(true));
        }
        
        var lay=PetBrain.class.getDeclaredField("lay");lay.setAccessible(true);
        for(int interruption:new int[]{170,440,740}) {
            PetBrain brain=new PetBrain();lay.setBoolean(brain,true);model.resetAnimation();
            boolean[] seen=new boolean[4];float previousLift=0;
            for(int tick=0;tick<interruption+160;tick++) {
                if(tick==interruption){lay.setBoolean(brain,false);brain.limbSwingAmount=.8F;}
                model.updatePose(brain,tick,true);check(model);poses++;
                if(model.getGesture()>.95)seen[model.getAction()]=true;
                if(model.getGesture()>.999F && model.getAction()==1 && model.palmDistance()>.005)throw new AssertionError("Live handshake missed");
                if(model.getGesture()>.999F && model.getAction()==3 && Math.max(model.supportDistance(false),model.supportDistance(true))>.005)throw new AssertionError("Live lift support missed: "+model.supportDistance(true));
                if(Math.abs(model.getLiftHeight()-previousLift)>.09)throw new AssertionError("Abrupt lift");
                previousLift=model.getLiftHeight();
            }
            if(model.getGesture()>.001 || model.getLiftHeight()>.001)throw new AssertionError("Did not recover walking");
            for(int a=1;a<=(interruption==170?1:interruption==440?2:3);a++)if(!seen[a])throw new AssertionError("Missing AFK gesture "+a);
        }
        System.out.printf("PASS: %d actual rendered family poses, minY %.6f; three AFK gestures and interruption recovery%n",poses,minY);
    }
}
