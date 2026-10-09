package com.spidiboost.littlepet;

import com.google.gson.Gson;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class PetCatalog {
    private static final Map<String,String> NAMES=names();
    public record Option(String id,String name,String search) { }
    private static List<Option> entries;
    private static Map<String,String> names() {
        try(var stream=PetCatalog.class.getResourceAsStream("/assets/spidiboostlittlepet/mob-names.json")) {
            if(stream!=null) {
                Map<?,?> data=new Gson().fromJson(new InputStreamReader(stream,StandardCharsets.UTF_8),Map.class);
                Map<String,String> result=new HashMap<>();data.forEach((k,v)->result.put(k.toString(),v.toString()));return result;
            }
        } catch(Exception ignored) { }
        return Map.of();
    }
    public static List<Option> all() {
        if(entries==null) {
            List<Option> result=new ArrayList<>();
            for(String name:PetConfig.PETS)result.add(new Option(name,name,name.toLowerCase(Locale.ROOT)));
            List<Option> vanilla=new ArrayList<>();
            for(EntityType<?> type:Registries.ENTITY_TYPE) {
                Identifier id=Registries.ENTITY_TYPE.getId(type);
                if(!id.getNamespace().equals("minecraft") || type==EntityType.PLAYER || !DefaultAttributeRegistry.hasDefinitionFor(type))continue;
                String name=NAMES.getOrDefault(id.getPath(),Text.translatable(type.getTranslationKey()).getString());
                String aliases=switch(id.getPath()) {case "wolf"->" собака dog wolf";case "cat"->" кот кошка cat";default->"";};
                if(type==EntityType.PARROT) {
                    String[] colors={"Красный","Синий","Зелёный","Голубой","Серый"};
                    for(int i=0;i<colors.length;i++)vanilla.add(new Option("vanilla:"+id+"#"+i,name+" · "+colors[i],(name+" "+colors[i]+" "+id).toLowerCase(Locale.ROOT)));
                } else vanilla.add(new Option("vanilla:"+id,name,(name+" "+id+aliases).toLowerCase(Locale.ROOT)));
            }
            vanilla.sort(Comparator.comparing(Option::name));result.addAll(vanilla);entries=List.copyOf(result);
        }
        return entries;
    }
    public static List<Option> search(String query) {
        String value=query.strip().toLowerCase(Locale.ROOT).replace('ё','е');
        return all().stream().filter(o->o.search().replace('ё','е').contains(value)).toList();
    }
    public static boolean valid(String id) {return id!=null && all().stream().anyMatch(o->o.id().equals(id));}
    public static String label(String id) {return all().stream().filter(o->o.id().equals(id)).findFirst().map(Option::name).orElse("Такса");}
    public static boolean vanilla(String id) {return id!=null && id.startsWith("vanilla:");}
    public static EntityType<?> type(String id) {
        if(!vanilla(id))return null;
        String value=id.substring(8).split("#",2)[0];Identifier key=Identifier.tryParse(value);
        return key==null?null:Registries.ENTITY_TYPE.get(key);
    }
    public static int variant(String id) {
        try {int hash=id.indexOf('#');return hash<0?0:Integer.parseInt(id.substring(hash+1));}
        catch(RuntimeException ignored) {return 0;}
    }
}
