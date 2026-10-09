import com.spidiboost.littlepet.model.CrocodileModel;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.util.math.MatrixStack;
import java.util.*;

 
public final class FootContactProbe {
    public static void main(String[] args) throws Exception {
        CrocodileModel model = new CrocodileModel();
        var rootField = CrocodileModel.class.getDeclaredField("root"); rootField.setAccessible(true);
        ModelPart root = (ModelPart) rootField.get(model);
        ModelPart body = root.getChild("body");
        List<ModelPart> feet = List.of(body.getChild("left_arm").getChild("left_hand"),
                body.getChild("right_arm").getChild("right_hand"),
                body.getChild("left_leg").getChild("left_foot"),
                body.getChild("right_leg").getChild("right_foot"));
        root.traverse().forEach(part -> part.hidden = true);
        feet.forEach(part -> part.hidden = false);
        for (String name : List.of("left_arm", "right_arm", "left_leg", "right_leg"))
            body.getChild(name).hidden = false;
        double minimum = Double.POSITIVE_INFINITY;
        int violations = 0, samples = 0;
        for (int r=0;r<=10;r++) for (float amount : new float[]{0,.25F,.5F,1})
            for(int phase=0;phase<96;phase++) for(int time=0;time<8;time++) {
                float swing=(float)(phase*Math.PI*2/96/.6662);
                float age=(float)(time*Math.PI*2/8/.07);
                model.animate(0,0,0,swing,amount,age,r/10.0F);
                ModelProbe sink = new ModelProbe();
                model.renderModel(new MatrixStack(),sink,0xF000F0,0,0);
                if(sink.vertices.size()!=192) throw new AssertionError("A foot disappeared from emitted geometry");
                double lowest=sink.vertices.stream().mapToDouble(v -> v[1]).min().orElseThrow();
                minimum=Math.min(minimum,lowest);samples++;
                if(lowest<.009) {
                    violations++;
                    if(violations<=3) System.out.printf("Foot below clearance: rest=%.1f movement=%.2f phase=%d time=%d lowestY=%.6f%n",r/10.0,amount,phase,time,lowest);
                }
            }
        System.out.printf("Samples=%d limbVerticesPerSample=192 lowestY=%.6f clearanceViolations=%d%n",samples,minimum,violations);
        if(violations!=0) throw new AssertionError("Feet intersect the floor / have insufficient depth clearance");
    }
}
