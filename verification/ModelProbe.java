import com.spidiboost.littlepet.model.CrocodileModel;
import com.spidiboost.littlepet.PetBrain;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import java.nio.file.*;
import java.util.*;
import com.google.gson.Gson;

 
public class ModelProbe implements VertexConsumer {
    final List<float[]> vertices = new ArrayList<>();
    float[] current;
    public VertexConsumer vertex(float x,float y,float z) { current=new float[]{x,y,z,0,0,0,0,0};vertices.add(current);return this; }
    public VertexConsumer color(int r,int g,int b,int a) {return this;}
    public VertexConsumer texture(float u,float v) {current[3]=u;current[4]=v;return this;}
    public VertexConsumer overlay(int u,int v) {return this;}
    public VertexConsumer light(int u,int v) {return this;}
    public VertexConsumer normal(float x,float y,float z) {current[5]=x;current[6]=y;current[7]=z;return this;}
    public static void main(String[] args) throws Exception {
        Path out=Path.of(args[0]);Files.createDirectories(out);
        CrocodileModel model=new CrocodileModel();
        Map<String,float[]> poses = new LinkedHashMap<>();
        poses.put("standing",new float[]{0,0,0,0,0,0,0});
        poses.put("walking",new float[]{0,12,-5,1.8f,0.8f,12,0});
        poses.put("resting",new float[]{0,0,0,0,0,30,1});
        poses.put("looking_left",new float[]{0,40,12,0,0,0,0});
        poses.put("looking_right",new float[]{0,-40,-12,0,0,0,0});
        for(int yaw:new int[]{0,90,180,270}) poses.put("direction_"+yaw,new float[]{yaw,0,0,0,0,0,0});
        
        PetBrain brain = new PetBrain();
        var layField = PetBrain.class.getDeclaredField("lay"); layField.setAccessible(true);
        var rootField = CrocodileModel.class.getDeclaredField("root"); rootField.setAccessible(true);
        ModelPart body = ((ModelPart)rootField.get(model)).getChild("body");
        layField.setBoolean(brain, true);
        for (int tick=0;tick<120;tick++) model.render(new MatrixStack(),new ModelProbe(),0xF000F0,0,brain,tick);
        if(body.pivotY < -6.1F) throw new AssertionError("AFK pose was not applied");
        ModelProbe resting = new ModelProbe(); model.render(new MatrixStack(),resting,0xF000F0,0,brain,120);
        Files.writeString(out.resolve("afk_from_brain.json"),new Gson().toJson(resting.vertices));
        layField.setBoolean(brain, false);
        for (int tick=121;tick<241;tick++) model.render(new MatrixStack(),new ModelProbe(),0xF000F0,0,brain,tick);
        if(Math.abs(body.pivotY + 9.0F) > 0.01F) throw new AssertionError("Standing pose did not return");
        System.out.println("PASS: real PetBrain.isLay drives rest and recovers standing pose");
        for(var entry:poses.entrySet()) {
            float[] p=entry.getValue();model.animate(p[0],p[1],p[2],p[3],p[4],p[5],p[6]);
            ModelProbe sink=new ModelProbe();model.renderModel(new MatrixStack(),sink,0xF000F0,0,p[0]);
            if(sink.vertices.size()%4!=0) throw new AssertionError("Incomplete quad");
            for(float[] v:sink.vertices) for(float c:v) if(!Float.isFinite(c)) throw new AssertionError("Nonfinite vertex");
            Files.writeString(out.resolve(entry.getKey()+".json"),new Gson().toJson(sink.vertices));
            double min=1e9,max=-1e9;for(float[] v:sink.vertices){min=Math.min(min,v[1]);max=Math.max(max,v[1]);}
            System.out.printf("%s: %d vertices, Y %.4f..%.4f%n",entry.getKey(),sink.vertices.size(),min,max);
        }
    }
}
