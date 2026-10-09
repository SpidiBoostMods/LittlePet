package com.spidiboost.littlepet.custom;

import com.google.gson.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

public final class OfficialSkinService {
    public record Skin(BufferedImage image,boolean slim,String name) { }
    public record Response(int status,byte[] body) { }
    public interface Transport { Response get(URI uri,int limit) throws Exception; }
    private record Cached(Skin skin,long expires) { }
    private static final HttpClient HTTP=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final OfficialSkinService SHARED=new OfficialSkinService(OfficialSkinService::download);
    private final Transport transport;
    private final Map<String,Cached> cache=new LinkedHashMap<>();
    public OfficialSkinService(Transport transport) { this.transport=transport; }
    public static OfficialSkinService shared() { return SHARED; }
    public static boolean validName(String name) { return name!=null && name.matches("[A-Za-z0-9_]{1,16}"); }
    public synchronized Skin fetch(String nickname) throws Exception {
        if(!validName(nickname))throw new IOException("Некорректный ник");
        String key=nickname.toLowerCase(Locale.ROOT);
        Cached saved=cache.get(key);
        if(saved!=null && saved.expires()>System.currentTimeMillis())return saved.skin();
        JsonObject profile=json(transport.get(URI.create("https://api.mojang.com/users/profiles/minecraft/"+nickname),65536));
        String id=profile.get("id").getAsString();
        if(!id.matches("[a-fA-F0-9]{32}"))throw new IOException("Некорректный профиль Mojang");
        JsonObject session=json(transport.get(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/"+id),262144));
        JsonObject textures=null;
        for(JsonElement e:session.getAsJsonArray("properties")) {
            JsonObject property=e.getAsJsonObject();
            if("textures".equals(property.get("name").getAsString())) {
                byte[] decoded=Base64.getDecoder().decode(property.get("value").getAsString());
                textures=JsonParser.parseString(new String(decoded,StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("textures");break;
            }
        }
        if(textures==null || !textures.has("SKIN"))throw new IOException("У игрока нет загруженного скина");
        JsonObject skin=textures.getAsJsonObject("SKIN");
        URI url=URI.create(skin.get("url").getAsString());
        if(!"textures.minecraft.net".equalsIgnoreCase(url.getHost()) || url.getRawUserInfo()!=null || url.getPort()!=-1 || !url.getPath().matches("/texture/[a-fA-F0-9]+"))throw new IOException("Некорректный адрес текстуры");
        Response response=transport.get(URI.create("https://textures.minecraft.net"+url.getPath()),1048576);
        ensureSuccess(response);
        BufferedImage image=ImageIO.read(new ByteArrayInputStream(response.body()));
        image=FaceImageComposer.normalizeSkin(image);
        boolean slim=skin.has("metadata") && skin.getAsJsonObject("metadata").has("model") && "slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString());
        Skin result=new Skin(image,slim,profile.has("name")?profile.get("name").getAsString():nickname);
        if(cache.size()>=64)cache.remove(cache.keySet().iterator().next());
        cache.put(key,new Cached(result,System.currentTimeMillis()+3600000));return result;
    }
    private static Response download(URI uri,int limit) throws Exception {
        HttpRequest request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).header("User-Agent","LittlePet-SpidiBoost/1.6").GET().build();
        HttpResponse<InputStream> result=HTTP.send(request,HttpResponse.BodyHandlers.ofInputStream());
        try(InputStream stream=result.body()) {
            byte[] bytes=stream.readNBytes(limit+1);
            if(bytes.length>limit)throw new IOException("Ответ сервиса слишком большой");
            return new Response(result.statusCode(),bytes);
        }
    }
    private static void ensureSuccess(Response response) throws IOException {
        if(response.status()==404 || response.status()==204)throw new IOException("Лицензионный игрок не найден");
        if(response.status()==429)throw new IOException("Mojang: слишком много запросов, попробуйте позже");
        if(response.status()!=200)throw new IOException("Сервис скинов недоступен ("+response.status()+")");
    }
    private static JsonObject json(Response response) throws IOException {
        ensureSuccess(response);
        try {return JsonParser.parseString(new String(response.body(),StandardCharsets.UTF_8)).getAsJsonObject();}
        catch(RuntimeException e) {throw new IOException("Некорректный ответ Mojang",e);}
    }
}
