package com.spidiboost.verification;

import com.spidiboost.littlepet.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.option.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.texture.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.block.Blocks;
import org.lwjgl.glfw.GLFW;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.util.*;

public final class RuntimeProbe implements ClientModInitializer {
    private int step,ticks,waiting,petIndex;
    private boolean failed;
    private Path out;
    private LittlePetClient pets;
    private int faceSample;
    private PetBrain cameraBrain;
    private net.minecraft.client.network.OtherClientPlayerEntity actor;
    private Vec3d settled;
    private int rotationTicks,guiScaleIndex;
    private com.mojang.authlib.GameProfile originalProfile;
    private net.minecraft.client.session.Session originalSession;
    private java.lang.reflect.Field profileField,sessionField;
    private int initialGuiScale;
    private void installSkinFixture() throws Exception {
        BufferedImage skin=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<64;y++)for(int x=0;x<64;x++)skin.setRGB(x,y,0xff000000 | x<<16 | y<<8 | 77);
        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();ImageIO.write(skin,"png",bytes);
        String payload=Base64.getEncoder().encodeToString("{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/abcdef\"}}}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        com.spidiboost.littlepet.custom.OfficialSkinService.Transport fixture=(uri,limit)-> {
            byte[] body=uri.getHost().equals("api.mojang.com")?"{\"id\":\"0123456789abcdef0123456789abcdef\",\"name\":\"Notch\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8):uri.getHost().equals("sessionserver.mojang.com")?("{\"properties\":[{\"name\":\"textures\",\"value\":\""+payload+"\"}]}" ).getBytes(java.nio.charset.StandardCharsets.UTF_8):bytes.toByteArray();
            return new com.spidiboost.littlepet.custom.OfficialSkinService.Response(200,body);
        };
        var service=com.spidiboost.littlepet.custom.OfficialSkinService.shared();typedField(service,com.spidiboost.littlepet.custom.OfficialSkinService.Transport.class).set(service,fixture);
        log("INFO: deterministic skin transport fixture installed in test harness; production JAR unchanged");
    }
    private void require(boolean okay,String message) {if(!okay)throw new AssertionError(message);}
    private void log(String line) throws Exception {
        System.out.println("LITTLEPET_TEST "+line);Files.writeString(out.resolve("runtime-results.txt"),line+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    }
    @Override public void onInitializeClient() {ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
    private TextFieldWidget field(PetScreen screen,int index) {return (TextFieldWidget)screen.children().stream().filter(c -> c instanceof TextFieldWidget).toList().get(index);}
    private PetScreen screen(MinecraftClient client) {require(client.currentScreen instanceof PetScreen,"Pet GUI not open");return (PetScreen)client.currentScreen;}
    private void snapshot(MinecraftClient client,String filename) throws Exception {
        Path directory=out.resolve("screenshots");Files.createDirectories(directory);
        try(NativeImage image=ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
            boolean visible=false;
            for(int x=1;x<10;x++)for(int y=1;y<10;y++)visible|=(image.getColorArgb(image.getWidth()*x/10,image.getHeight()*y/10)&0xffffff)!=0;
            if(!visible)throw new CaptureNotReady();
            image.writeTo(directory.resolve(filename+".png"));
        }
        System.out.println("LITTLEPET_SCREEN "+filename);
    }
    private void open(MinecraftClient client) {client.setScreen(null);KeyBinding.onKeyPressed(InputUtil.fromKeyCode(GLFW.GLFW_KEY_RIGHT_SHIFT,0));}
    private void choose(MinecraftClient client,String pet) {
        PetScreen screen=screen(client);int left=(client.getWindow().getScaledWidth()-316)/2;
        int height=pets.config.pet.equals("Кастом")?272:pets.config.pet.equals("Пельмень")?214:178;
        int top=Math.max(6,(client.getWindow().getScaledHeight()-height)/2);
        screen.mouseClicked(left+160,top+103,0);
        for(char chr:pet.toCharArray())screen.charTyped(chr,0);
        screen.keyPressed(GLFW.GLFW_KEY_ENTER,0,0);
        require(pets.config.pet.equals(pet),"Dropdown did not choose "+pet);
    }
    private NativeImage texture(MinecraftClient client) {
        var texture=client.getTextureManager().getTexture(Identifier.of("spidiboostlittlepet","littlepet/custom_steve_face"));
        require(texture instanceof NativeImageBackedTexture,"Combined GPU texture not registered");
        return ((NativeImageBackedTexture)texture).getImage();
    }
    private java.lang.reflect.Field typedField(Object target,Class<?> type) throws Exception {
        for(Class<?> owner=target.getClass();owner!=null;owner=owner.getSuperclass())for(var field:owner.getDeclaredFields())if(field.getType()==type && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {field.setAccessible(true);return field;}
        throw new AssertionError("Missing field type "+type);
    }
    private void tick(MinecraftClient client) {
        if(failed || (Boolean.getBoolean("littlepet.qa.catalog") || Boolean.getBoolean("littlepet.qa.compat")))return;
        try {
            if(out==null) {out=client.runDirectory.toPath();pets=LittlePetClient.INSTANCE;}
            if(client.world==null || client.player==null)return;
            ticks++;
            if(step==0 && Boolean.getBoolean("littlepet.qa.visualOnly")) {pets.config.enabled=true;step=500;waiting=20;return;}
            if(step==0 && Boolean.getBoolean("littlepet.qa.scalesOnly")) {initialGuiScale=client.options.getGuiScale().getValue();pets.config.enabled=true;step=300;waiting=20;return;}
            if(waiting>0) {waiting--;return;}
            switch(step) {
                case 0 -> {
                    if(Boolean.getBoolean("littlepet.qa.skinFixture"))installSkinFixture();
                    require(net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("reallystafftool").isEmpty(),"ATools loaded in standalone test");
                    pets.config.pet="Такса";pets.config.enabled=true;pets.config.family=false;
                    pets.save();require(!Files.readString(out.resolve("config/littlepet-spidiboost.json")).contains("chase"),"Removed setting survived configuration migration");
                    log("PASS: legacy disabled setting ignored and removed on save");
                    initialGuiScale=client.options.getGuiScale().getValue();client.options.getGuiScale().setValue(2);client.onResolutionChanged();
                    if(client.getServer()!=null)client.getServer().execute(() -> {
                        var world=client.getServer().getOverworld();world.setTimeOfDay(6000);world.setWeather(0,0,false,false);
                    });
                    log("PASS: production protected mod initialized without AdminTools");
                    client.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
                    open(client);step++;waiting=20;
                }
                case 1 -> {
                    screen(client);snapshot(client,"gui-default");log("PASS: right Shift key binding opens compact GUI");
                    PetScreen gui=screen(client);int left=(client.getWindow().getScaledWidth()-316)/2,top=Math.max(6,(client.getWindow().getScaledHeight()-178)/2);
                    gui.mouseClicked(left+160,top+103,0);gui.keyPressed(GLFW.GLFW_KEY_ESCAPE,0,0);
                    require(pets.config.pet.equals("Такса"),"Cancel changed pet");step=101;waiting=4;
                }
                case 101 -> {snapshot(client,"gui-after-cancel");log("PASS: dropdown cancel keeps compact panel");step=200;waiting=4;}
                case 200 -> {
                    originalProfile=client.player.getGameProfile();originalSession=client.getSession();
                    require(pets.getPlayerName().equals(originalProfile.getName()),"Header does not match live player");
                    profileField=typedField(client.player,com.mojang.authlib.GameProfile.class);
                    profileField.set(client.player,new com.mojang.authlib.GameProfile(originalProfile.getId(),"LivePetName"));
                    require(pets.getPlayerName().equals("LivePetName"),"Profile rename not reflected");
                    log("PASS: open GUI immediately tracks live player rename");step=201;waiting=4;
                }
                case 201 -> {
                    snapshot(client,"gui-player-renamed");sessionField=typedField(client,net.minecraft.client.session.Session.class);
                    sessionField.set(client,new net.minecraft.client.session.Session("SessionPetName",originalSession.getUuidOrNull(),"0",Optional.empty(),Optional.empty(),net.minecraft.client.session.Session.AccountType.LEGACY));
                    require(pets.getPlayerName().equals("SessionPetName"),"Session rename not reflected");
                    log("PASS: open GUI immediately tracks live session rename");step=202;waiting=4;
                }
                case 202 -> {
                    snapshot(client,"gui-session-renamed");sessionField.set(client,originalSession);profileField.set(client.player,originalProfile);
                    require(pets.getPlayerName().equals(originalProfile.getName()),"Nickname restore failed");
                    step=203;waiting=4;
                }
                case 203 -> {
                    snapshot(client,"gui-nickname-restored");
                    int left=(client.getWindow().getScaledWidth()-316)/2,top=Math.max(6,(client.getWindow().getScaledHeight()-178)/2);
                    screen(client).mouseClicked(left+272,top+66,0);require(!pets.config.enabled,"Enable switch missed after compact layout");step=204;waiting=4;
                }
                case 204 -> {
                    snapshot(client,"gui-disabled");int left=(client.getWindow().getScaledWidth()-316)/2,top=Math.max(6,(client.getWindow().getScaledHeight()-178)/2);
                    screen(client).mouseClicked(left+272,top+66,0);require(pets.config.enabled,"Enable switch restore failed");
                    log("PASS: compact menu toggle hitboxes and persistence");pets.config.pet="Кастом";step=205;waiting=4;
                }
                case 205 -> {
                    require(screen(client).children().stream().filter(c -> c instanceof TextFieldWidget).count()==2,"External pet change left stale GUI fields");
                    snapshot(client,"gui-live-pet-change");log("PASS: open GUI rebuilds layout after live pet change");step=2;waiting=4;
                }
                case 2 -> {
                    PetScreen gui=screen(client);require(gui.children().stream().filter(c -> c instanceof TextFieldWidget).count()==2,"Custom fields missing");
                    TextFieldWidget skin=field(gui,1);gui.setFocused(skin);skin.setFocused(true);skin.setText("Notch");
                    require(pets.skin.getApplied().isEmpty(),"Typing applied skin before Enter");
                    int left=(client.getWindow().getScaledWidth()-316)/2,top=Math.max(6,(client.getWindow().getScaledHeight()-272)/2);
                    gui.mouseClicked(left+160,top+103,0);step=102;waiting=4;
                }
                case 102 -> {snapshot(client,"gui-custom-dropdown-empty");screen(client).keyPressed(GLFW.GLFW_KEY_ESCAPE,0,0);require(pets.skin.getValue().equals("Notch") && pets.skin.getApplied().isEmpty(),"Dropdown damaged draft nickname");step=103;waiting=4;}
                case 103 -> {PetScreen gui=screen(client);var input=field(gui,1);gui.setFocused(input);input.setFocused(true);gui.keyPressed(GLFW.GLFW_KEY_ENTER,0,0);require(pets.skin.getApplied().equals("Notch"),"Enter did not commit skin");step=3;waiting=10;}
                case 3 -> {
                    if(!pets.skin.getStatus().contains("установлен")) {require(ticks<1000,"Official skin timed out: "+pets.skin.getStatus());waiting=10;return;}
                    log(Boolean.getBoolean("littlepet.qa.skinFixture")?"PASS: GUI Enter loaded skin service fixture to GPU":"PASS: GUI Enter loaded official Notch skin to GPU");snapshot(client,"gui-skin");
                    BufferedImage photo=new BufferedImage(17,13,BufferedImage.TYPE_INT_ARGB);
                    for(int y=0;y<13;y++)for(int x=0;x<17;x++)photo.setRGB(x,y,0xffee9944);
                    ImageIO.write(photo,"png",out.resolve("face-fixture.png").toFile());
                    field(screen(client),0).setText(out.resolve("face-fixture.png").toString());step++;waiting=20;
                }
                case 4 -> {
                    if(!pets.face.getStatus().contains("обновлено")) {require(!pets.face.hasError() && ticks<2200,"Image not applied: "+pets.face.getStatus());waiting=10;return;}
                    NativeImage image=texture(client);faceSample=image.getColorArgb(170,170);
                    require(faceSample==0xffee9944,"Photo did not replace front face on GPU");
                    require(image.getColorArgb(650,170)==0,"Front hat not cleared");
                    log("PASS: skin + face combined on real GPU texture");snapshot(client,"gui-skin-face");
                    PetScreen gui=screen(client);int left=(client.getWindow().getScaledWidth()-316)/2,top=Math.max(6,(client.getWindow().getScaledHeight()-272)/2);gui.mouseClicked(left+160,top+103,0);step=104;waiting=4;
                }
                case 104 -> {
                    snapshot(client,"gui-custom-dropdown-filled");screen(client).keyPressed(GLFW.GLFW_KEY_ESCAPE,0,0);
                    PetScreen gui=screen(client);var skin=field(gui,1);gui.setFocused(skin);skin.setFocused(true);skin.setText("bad name!");gui.keyPressed(GLFW.GLFW_KEY_ENTER,0,0);
                    require(pets.skin.hasError() && pets.skin.getApplied().equals("Notch") && texture(client).getColorArgb(170,170)==faceSample,"Invalid input damaged applied skin");
                    log("PASS: invalid nickname preserves applied skin + face");
                    field(gui,0).setText("");step=5;waiting=20;
                }
                case 5 -> {
                    if(texture(client).getColorArgb(170,170)==faceSample) {require(ticks<2200,"Clearing face did not restore skin");waiting=10;return;}
                    log("PASS: clearing photo retains official skin");
                    PetScreen gui=screen(client);var skin=field(gui,1);gui.setFocused(skin);skin.setFocused(true);skin.setText("");gui.keyPressed(GLFW.GLFW_KEY_ENTER,0,0);step++;waiting=20;
                }
                case 6 -> {
                    require(pets.skin.getApplied().isEmpty() && !pets.skin.hasError(),"Clearing skin failed");
                    log("PASS: clearing nickname restores Steve");
                    pets.config.pet="Крокодил";pets.save();client.setScreen(null);step++;waiting=30;
                }
                case 7 -> {
                    client.setScreen(null);String pet=PetConfig.PETS.get(petIndex);pets.config.pet=pet;pets.config.family=pet.equals("Пельмень");pets.save();
                    step++;waiting=30;
                }
                case 8 -> {
                    if(client.currentScreen!=null) {client.setScreen(null);waiting=10;return;}
                    require(pets.getBrain().isSpawned(),"Standalone brain failed");
                    Vec3d pos=pets.getBrain().getPos();require(Double.isFinite(pos.x+pos.y+pos.z),"Invalid follower coordinates");
                    snapshot(client,"pet-"+petIndex);log("PASS: real world rendering "+pets.config.pet);
                    if(++petIndex<PetConfig.PETS.size())step=7;else {step=9;open(client);waiting=5;}
                }
                case 9 -> {
                    choose(client,"Пельмень");PetScreen gui=screen(client);require(gui.children().isEmpty(),"Custom inputs leaked into other pet");
                    int left=(client.getWindow().getScaledWidth()-316)/2,top=Math.max(6,(client.getWindow().getScaledHeight()-214)/2);
                    boolean family=pets.config.family;gui.mouseClicked(left+272,top+142,0);require(pets.config.family!=family,"Family toggle failed");
                    pets.config.skin="Notch";pets.config.face=out.resolve("face-fixture.png").toString();pets.save();
                    PetConfig restored=PetConfig.load();require(restored.skin.equals("Notch") && restored.face.equals(pets.config.face),"Protected persistence failed");
                    log("PASS: conditional fields, family toggle, protected config persistence");
                    gui.keyPressed(GLFW.GLFW_KEY_RIGHT_SHIFT,0,0);require(client.currentScreen==null,"Right Shift did not close GUI");
                    log("PASS: GUI closes on right Shift");step++;waiting=5;
                }
                case 10 -> {
                    cameraBrain=new PetBrain();
                    actor=new net.minecraft.client.network.OtherClientPlayerEntity(client.world,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PetLogicTest"));
                    actor.setPosition(client.player.getPos());cameraBrain.setEntity(actor);cameraBrain.tick();
                    actor.setPosition(actor.getPos().add(2.5,0,0));step++;waiting=0;
                }
                case 11 -> {
                    cameraBrain.tick();
                    if(++rotationTicks<80)return;
                    settled=cameraBrain.getPos();rotationTicks=0;step++;
                }
                case 12 -> {
                    actor.setYaw(rotationTicks*31F);actor.setPitch((rotationTicks%5)*14F);cameraBrain.tick();
                    require(cameraBrain.getPos().subtract(settled).horizontalLength()<.025,"Camera rotation caused follower movement");
                    if(++rotationTicks<40)return;
                    log("PASS: camera-only rotation causes no follower movement");step++;
                }
                case 13 -> {
                    actor.setPosition(actor.getPos().add(35,64,0));cameraBrain.tick();
                    require(cameraBrain.getPos().y<actor.getY()-10,"Flying owner teleported pet into air");
                    log("PASS: flying owner respawns follower on ground, like ATools");
                    step=300;waiting=4;
                }
                case 300 -> {
                    int scale=new int[]{1,2,6,6,6}[guiScaleIndex];String pet=new String[]{"Такса","Кастом","Кастом","Пельмень","Капибара"}[guiScaleIndex];
                    client.options.getGuiScale().setValue(scale);client.onResolutionChanged();pets.config.pet=pet;client.setScreen(new PetScreen(pets));step=301;waiting=4;
                }
                case 301 -> {
                    snapshot(client,"gui-scale-"+guiScaleIndex);PetScreen gui=screen(client);float scale=gui.getLayoutScale();
                    int height=pets.config.pet.equals("Кастом")?272:pets.config.pet.equals("Пельмень")?214:178;
                    int virtualW=Math.round(client.getWindow().getScaledWidth()/scale),virtualH=Math.round(client.getWindow().getScaledHeight()/scale),left=(virtualW-316)/2,top=Math.max(6,(virtualH-height)/2);
                    require((top+height)*scale<=client.getWindow().getScaledHeight()+1,"Card clipped at GUI scale "+guiScaleIndex);
                    gui.mouseClicked((left+150)*scale,(top+103)*scale,0);for(char chr:"Крокодил".toCharArray())gui.charTyped(chr,0);step=302;waiting=4;
                }
                case 302 -> {
                    snapshot(client,"gui-scale-dropdown-"+guiScaleIndex);PetScreen gui=screen(client);float scale=gui.getLayoutScale();
                    int height=pets.config.pet.equals("Кастом")?272:pets.config.pet.equals("Пельмень")?214:178;
                    int virtualW=Math.round(client.getWindow().getScaledWidth()/scale),virtualH=Math.round(client.getWindow().getScaledHeight()/scale),left=(virtualW-316)/2,top=Math.max(6,(virtualH-height)/2),popup=Math.max(8,Math.min(top+120,virtualH-Math.min(226,virtualH-16)-8));
                    gui.mouseClicked((left+150)*scale,(popup+36+12)*scale,0);
                    require(pets.config.pet.equals("Крокодил"),"Scaled dropdown hitbox failed "+guiScaleIndex);
                    log("PASS: GUI scale "+guiScaleIndex+", complete card visible + dropdown selection");
                    if(++guiScaleIndex<5)step=300;else {client.options.getGuiScale().setValue(initialGuiScale);client.onResolutionChanged();step=400;}waiting=4;
                }
                case 400 -> {
                    client.setScreen(null);pets.config.pet="Крокодил";
                    actor=new net.minecraft.client.network.OtherClientPlayerEntity(client.world,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"AttackTargetTest"));actor.setPosition(client.player.getPos().add(2,0,0));
                    net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.invoker().interact(client.player,client.world,net.minecraft.util.Hand.MAIN_HAND,actor,null);
                    var target=typedField(pets,net.minecraft.entity.LivingEntity.class);require(target.get(pets)==actor,"Always-enabled attack target missing");
                    pets.config.pet="Кастом";target.set(pets,null);
                    net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.invoker().interact(client.player,client.world,net.minecraft.util.Hand.MAIN_HAND,actor,null);
                    require(target.get(pets)==null,"Custom attack exclusion lost");
                    log("PASS: always-enabled behavior + Custom attack exclusion");log(Boolean.getBoolean("littlepet.qa.scalesOnly")?"PASS: maximum-scale runtime suite":"PASS: complete runtime suite");step=401;waiting=5;
                }
                case 401 -> client.scheduleStop();
                case 500 -> {client.options.getGuiScale().setValue(2);client.onResolutionChanged();pets.config.pet="Такса";pets.config.enabled=true;client.setScreen(new PetScreen(pets));step=501;waiting=20;}
                case 501 -> {snapshot(client,"gui-normal-confirmed");pets.config.enabled=false;step=502;waiting=20;}
                case 502 -> {snapshot(client,"gui-disabled-confirmed");pets.config.pet="Кастом";step=503;waiting=20;}
                case 503 -> {
                    require(screen(client).children().stream().filter(c -> c instanceof TextFieldWidget).count()==2,"Live layout failed");snapshot(client,"gui-live-change-confirmed");
                    log("PASS: synchronous visual capture: normal, disabled, live custom layout");step=504;waiting=5;
                }
                case 504 -> client.scheduleStop();
            }
        } catch(CaptureNotReady retry) {waiting=5;} catch(Throwable error) {
            failed=true;error.printStackTrace();
            try {log("FAIL: "+error);}catch(Exception ignored) { }
            client.scheduleStop();
        }
    }
    private static final class CaptureNotReady extends RuntimeException { }
}
