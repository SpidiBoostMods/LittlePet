package com.spidiboost.littlepet;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.io.IOException;
import java.util.List;

public final class PetConfig {
    public static final List<String> PETS=List.of("Такса","Крокодил","Капибара","Такса 2","Пельмень","Кастом");
    @SerializedName("enabled") public boolean enabled=true;
    @SerializedName("pet") public String pet="Такса";
    @SerializedName("family") public boolean family;
    @SerializedName("face") public String face="";
    @SerializedName("skin") public String skin="";
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().create();
    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("littlepet-spidiboost.json"); }
    public static PetConfig load() {
        try {
            if(Files.isRegularFile(file())) {
                PetConfig result=GSON.fromJson(Files.readString(file()),PetConfig.class);
                if(result!=null) {result.validate();return result;}
            }
        } catch(IOException | RuntimeException ignored) { }
        return new PetConfig();
    }
    private void validate() {
        if(!PetCatalog.valid(pet))pet="Такса";
        if(face==null)face="";if(skin==null)skin="";
        if(face.length()>4096)face="";if(skin.length()>16)skin="";
    }
    public boolean save() {
        try {
            Path target=file();Files.createDirectories(target.getParent());
            Path temporary=Files.createTempFile(target.getParent(),"littlepet-",".tmp");
            Files.writeString(temporary,GSON.toJson(this));
            try {Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e) {Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);}
            return true;
        } catch(IOException e) {return false;}
    }
}
