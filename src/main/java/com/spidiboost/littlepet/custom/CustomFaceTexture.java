package com.spidiboost.littlepet.custom;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public final class CustomFaceTexture {
    public static final Identifier STEVE=Identifier.ofVanilla("textures/entity/player/wide/steve.png");
    private static final Identifier CUSTOM=Identifier.of("spidiboostlittlepet","littlepet/custom_steve_face");
    private static final ScheduledExecutorService DECODER=Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t=new Thread(r,"LittlePet skin decoder");t.setDaemon(true);return t;
    });
    private final AtomicLong generation=new AtomicLong();
    private String observed;
    private long observedRevision=-1,skinRevision=-1;
    private boolean skinInitialized,slim;
    private ScheduledFuture<?> pending;
    private RenderLayer layer=RenderLayer.getEntityCutoutNoCull(STEVE);
    public RenderLayer getLayer() { return layer; }
    public boolean isSlim() { return slim; }
    public void observe(ImagePathSetting image) { observe(image,null); }
    public void observe(ImagePathSetting image,SkinNameSetting skin) {
        if(skin!=null && !skinInitialized) {
            skinInitialized=true;
            if(!skin.isEditing() && !skin.getValue().isBlank())skin.apply();
        }
        long rev=skin==null?0:skin.getRevision();
        String value=image.getValue();
        if(Objects.equals(value,observed) && observedRevision==image.getRevision() && skinRevision==rev)return;
        observed=value;observedRevision=image.getRevision();skinRevision=rev;
        long token=generation.incrementAndGet();
        if(pending!=null)pending.cancel(false);
        String nickname=skin==null?"":skin.getApplied();
        image.status(value.isBlank()?"Без замены лица":"Загрузка изображения…",false);
        pending=DECODER.schedule(() -> decode(image,skin,value,nickname,token),value.isBlank()?0:350,TimeUnit.MILLISECONDS);
    }
    private void decode(ImagePathSetting imageSetting,SkinNameSetting skinSetting,String path,String nickname,long token) {
        MinecraftClient client=MinecraftClient.getInstance();
        try {
            BufferedImage base;boolean narrow=false;
            if(nickname.isBlank()) {
                try(InputStream input=client.getResourceManager().getResource(STEVE).orElseThrow().getInputStream()) {base=ImageIO.read(input);}
            } else {
                OfficialSkinService.Skin result=OfficialSkinService.shared().fetch(nickname);
                base=result.image();narrow=result.slim();
            }
            BufferedImage photo=path.isBlank()?null:FaceImageComposer.readPhoto(ImagePathSetting.parsePath(path));
            BufferedImage composed=FaceImageComposer.compose(base,photo);
            int[] argb=composed.getRGB(0,0,FaceImageComposer.SIZE,FaceImageComposer.SIZE,null,0,FaceImageComposer.SIZE);
            boolean finalNarrow=narrow;
            client.execute(() -> {
                if(token!=generation.get())return;
                NativeImage nativeImage=null;NativeImageBackedTexture texture=null;
                try {
                    nativeImage=new NativeImage(FaceImageComposer.SIZE,FaceImageComposer.SIZE,false);
                    for(int y=0;y<FaceImageComposer.SIZE;y++)for(int x=0;x<FaceImageComposer.SIZE;x++)nativeImage.setColorArgb(x,y,argb[y*FaceImageComposer.SIZE+x]);
                    texture=new NativeImageBackedTexture(nativeImage);nativeImage=null;texture.setFilter(false,false);
                    client.getTextureManager().registerTexture(CUSTOM,texture);texture=null;
                    layer=RenderLayer.getEntityCutoutNoCull(CUSTOM);slim=finalNarrow;
                    imageSetting.status(path.isBlank()?"Без замены лица":"Лицо обновлено",false);
                    if(skinSetting!=null)skinSetting.status(nickname.isBlank()?"Стандартный Стив":"Скин "+nickname+" установлен",false);
                } catch(RuntimeException e) {
                    if(texture!=null)texture.close();if(nativeImage!=null)nativeImage.close();
                    imageSetting.status("Не удалось загрузить текстуру",true);
                }
            });
        } catch(Exception e) {
            client.execute(() -> {
                if(token!=generation.get())return;
                String reason=e instanceof java.net.http.HttpTimeoutException?"Сервис скинов не ответил":e.getMessage();
                if(reason==null || reason.isBlank())reason="Не удалось загрузить скин или лицо";
                imageSetting.status(reason,true);if(skinSetting!=null)skinSetting.status(reason,true);
            });
        }
    }
}
