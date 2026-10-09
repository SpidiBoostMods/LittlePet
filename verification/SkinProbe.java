import com.spidiboost.littlepet.custom.*;
import com.google.gson.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class SkinProbe {
    static byte[] bytes(String text) {return text.getBytes(StandardCharsets.UTF_8);}
    static void require(boolean condition,String message) {if(!condition)throw new AssertionError(message);}
    public static void main(String[] args) throws Exception {
        Path out=Path.of(args[0]);Files.createDirectories(out);
        BufferedImage base=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<64;y++)for(int x=0;x<64;x++)base.setRGB(x,y,0xff000000 | x<<16 | y<<8 | 77);
        ByteArrayOutputStream stream=new ByteArrayOutputStream();ImageIO.write(base,"png",stream);
        byte[] png=stream.toByteArray();AtomicInteger calls=new AtomicInteger();
        String payload=Base64.getEncoder().encodeToString(bytes("{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/abcdef\",\"metadata\":{\"model\":\"slim\"}}}}"));
        OfficialSkinService service=new OfficialSkinService((uri,limit) -> {
            calls.incrementAndGet();require(uri.getScheme().equals("https"),"HTTP not upgraded");
            if(uri.getHost().equals("api.mojang.com"))return new OfficialSkinService.Response(200,bytes("{\"id\":\"0123456789abcdef0123456789abcdef\",\"name\":\"Alex\"}"));
            if(uri.getHost().equals("sessionserver.mojang.com"))return new OfficialSkinService.Response(200,bytes("{\"properties\":[{\"name\":\"textures\",\"value\":\""+payload+"\"}]}"));
            require(uri.getHost().equals("textures.minecraft.net"),"Unexpected CDN");return new OfficialSkinService.Response(200,png);
        });
        var fetched=service.fetch("Alex");require(fetched.slim() && fetched.name().equals("Alex"),"Slim or nickname lost");
        service.fetch("aLEX");require(calls.get()==3,"Cache missed");
        SkinNameSetting setting=new SkinNameSetting(() -> true);long initial=setting.getRevision();
        setting.setValue("Alex");require(setting.getRevision()==initial && setting.getApplied().isEmpty(),"Typing committed skin");
        require(setting.apply() && setting.getApplied().equals("Alex") && setting.getRevision()==initial+1,"Enter did not apply");
        setting.setValue("Bad nickname!");require(!setting.apply() && setting.getApplied().equals("Alex") && setting.hasError(),"Invalid nickname replaced skin");
        setting.setValue("");require(setting.apply() && setting.getApplied().isEmpty(),"Clearing nickname failed");
        var alone=FaceImageComposer.compose(fetched.image(),null);
        BufferedImage photo=new BufferedImage(19,13,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<13;y++)for(int x=0;x<19;x++)photo.setRGB(x,y,0xffee9944);
        var combined=FaceImageComposer.compose(fetched.image(),photo);long hash=1;
        for(int y=0;y<1024;y++)for(int x=0;x<1024;x++) {
            int original=base.getRGB(x/16,y/16);require(alone.getRGB(x,y)==original,"Skin-only changed texel");
            boolean face=x>=128 && x<256 && y>=128 && y<256,hat=x>=640 && x<768 && y>=128 && y<256;
            int expected=face?0xffee9944:hat?0:original;require(combined.getRGB(x,y)==expected,"Skin+face modified side/back/body");hash=31*hash+combined.getRGB(x,y);
        }
        BufferedImage legacy=new BufferedImage(64,32,BufferedImage.TYPE_INT_ARGB);
        legacy.setRGB(4,20,0xff123456);var modern=FaceImageComposer.normalizeSkin(legacy);
        require(modern.getHeight()==64 && modern.getRGB(23,52)==0xff123456,"Legacy mirrored limb wrong");
        for(int status:new int[]{204,404,429,503}) {
            OfficialSkinService failed=new OfficialSkinService((uri,limit) -> new OfficialSkinService.Response(status,new byte[0]));
            try {failed.fetch("Unknown");throw new AssertionError("HTTP error accepted");}catch(IOException expected) { }
        }
        String[] names={"","a b","../foo","abcdefghijklmnopq","кириллица"};
        for(String name:names)require(!OfficialSkinService.validName(name),"Bad username accepted");
        Files.writeString(out.resolve("skin-checks.txt"),Long.toHexString(hash)+"; cache=3; legacy=PASS; enter=PASS; failures=PASS\n");
        if(args.length>1 && args[1].equals("live")) {
            var live=OfficialSkinService.shared().fetch("Notch");
            require(live.image().getWidth()==64 && live.name().equalsIgnoreCase("Notch"),"Official live profile failed");
            System.out.println("PASS: live official Notch profile + texture, slim="+live.slim());
        }
        System.out.println("PASS: skin-only + skin/face texels, slim, legacy, Enter, clear, errors, cache");
    }
}
