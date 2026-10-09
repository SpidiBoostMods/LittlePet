import com.spidiboost.littlepet.model.CustomSteveModel;
import com.spidiboost.littlepet.custom.FaceImageComposer;
import com.spidiboost.littlepet.custom.ImagePathSetting;
import net.minecraft.client.util.math.MatrixStack;
import com.google.gson.Gson;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;

public final class CustomSteveProbe {
    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        Files.createDirectories(out);
        CustomSteveModel model = new CustomSteveModel();
        double minimum = Double.POSITIVE_INFINITY;
        int samples = 0;
        for(boolean slim:new boolean[]{false,true}) {
        model=new CustomSteveModel(slim);
        for (int phase = 0; phase < 96; phase++) for (float movement : new float[]{0, .25F, .5F, 1}) {
            model.animate(phase / 7F, movement, 37, -19);
            for (int yaw : new int[]{0, 90, 180, 270}) {
                ModelProbe sink = new ModelProbe();
                model.renderModel(new MatrixStack(), sink, 0xF000F0, 0, yaw);
                if (sink.vertices.size() < 144 || sink.vertices.size() % 4 != 0) throw new AssertionError("Broken Steve mesh");
                for (float[] vertex : sink.vertices) {
                    for (float value : vertex) if (!Float.isFinite(value)) throw new AssertionError("Nonfinite vertex");
                    if (vertex[1] < .015F) throw new AssertionError("Steve foot below ground");
                    minimum = Math.min(minimum, vertex[1]);
                }
                if (phase == 17 && movement == .5F) Files.writeString(out.resolve("steve-" + slim + "-" + yaw + ".json"), new Gson().toJson(sink.vertices));
                samples++;
            }
        }
        }
        BufferedImage original = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) original.setRGB(x, y, 0xFF000000 | x << 16 | y << 8 | 41);
        BufferedImage photo = new BufferedImage(17, 11, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 11; y++) for (int x = 0; x < 17; x++) photo.setRGB(x, y, 0xFFFF7733);
        BufferedImage skin = FaceImageComposer.compose(original, photo);
        long hash = 1;
        for (int y = 0; y < 1024; y++) for (int x = 0; x < 1024; x++) {
            boolean face = x >= 128 && x < 256 && y >= 128 && y < 256;
            boolean hat = x >= 640 && x < 768 && y >= 128 && y < 256;
            int expected = face ? 0xFFFF7733 : hat ? 0 : original.getRGB(x / 16, y / 16);
            if (skin.getRGB(x, y) != expected) throw new AssertionError("Changed unrelated skin texel " + x + "/" + y);
            hash = hash * 31 + skin.getRGB(x, y);
        }
        ImagePathSetting setting = new ImagePathSetting(() -> true);
        long initialRevision = setting.getRevision();
        setting.setValue("/tmp/Моё лицо.png");
        if (!setting.getValue().equals("/tmp/Моё лицо.png") || setting.getRevision() != initialRevision + 1) throw new AssertionError("Path setting changed");
        if (!ImagePathSetting.parsePath("\"/tmp/Моё лицо.png\"").toString().equals("/tmp/Моё лицо.png")) throw new AssertionError("Quoted Unicode path changed");
        setting.status("SpidiBoost", true);
        if (!setting.hasError() || !setting.getStatus().equals("SpidiBoost")) throw new AssertionError("Path status changed");
        Files.writeString(out.resolve("face-hash.txt"), Long.toHexString(hash));
        System.out.printf("PASS: %d Steve poses, minimum Y=%.6f; all 1048576 face-atlas texels and Unicode path setting correct%n", samples, minimum);
    }
}
