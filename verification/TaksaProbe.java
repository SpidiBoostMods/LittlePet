import com.spidiboost.littlepet.*;
import com.spidiboost.littlepet.model.TaksaModel;
import net.minecraft.client.util.math.MatrixStack;
import com.google.gson.Gson;
import java.nio.file.*;

public final class TaksaProbe {
    public static void main(String[] args) throws Exception {
        Path out=Path.of(args[0]);Files.createDirectories(out);TaksaModel model=new TaksaModel();PetBrain brain=new PetBrain();
        var lay=PetBrain.class.getDeclaredField("lay");lay.setAccessible(true);
        var body=PetBrain.class.getDeclaredField("bodySmooth");body.setAccessible(true);
        var yaw=PetBrain.class.getDeclaredField("yawSmooth");yaw.setAccessible(true);yaw.set(brain,new SmoothValue(35,150));
        var pitch=PetBrain.class.getDeclaredField("pitchSmooth");pitch.setAccessible(true);pitch.set(brain,new SmoothValue(-18,150));
        float minimum=Float.POSITIVE_INFINITY;int count=0;
        for(boolean sitting:new boolean[]{false,true})for(int phase=0;phase<96;phase++)for(float movement:new float[]{0,.4F,1})for(int heading:new int[]{0,90,180,270}) {
            lay.setBoolean(brain,sitting);body.set(brain,new SmoothValue(heading,150));brain.limbSwing=phase/7F;brain.limbSwingAmount=movement;
            ModelProbe sink=new ModelProbe();model.render(new MatrixStack(),sink,0xF000F0,0,brain,phase/3F);
            if(sink.vertices.size()<300)throw new AssertionError("Original Taksa mesh incomplete");
            for(float[] v:sink.vertices) {if(!Float.isFinite(v[0]+v[1]+v[2]) || v[1]<.018F)throw new AssertionError("Original Taksa clips ground");minimum=Math.min(minimum,v[1]);}
            if(phase==13 && movement==.4F)Files.writeString(out.resolve("taksa-"+sitting+"-"+heading+".json"),new Gson().toJson(sink.vertices));
            count++;
        }
        System.out.println("PASS: original Taksa "+count+" walking/sitting poses, minimum Y="+minimum);
    }
}
