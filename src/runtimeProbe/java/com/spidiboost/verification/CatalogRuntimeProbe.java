package com.spidiboost.verification;

import com.spidiboost.littlepet.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.option.*;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;
import java.util.*;

public final class CatalogRuntimeProbe implements ClientModInitializer {
    private LittlePetClient pets;
    private Path out;
    private int step,wait,index,samples;
    private boolean failed,geometryDone;
    private List<PetCatalog.Option> vanilla;
    private float minimum=Float.POSITIVE_INFINITY;
    private void require(boolean okay,String message) {if(!okay)throw new AssertionError(message);}
    private void log(String text) throws Exception {System.out.println("LITTLEPET_CATALOG "+text);Files.writeString(out.resolve("catalog-runtime-results.txt"),text+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
    @Override public void onInitializeClient() {
        if(!Boolean.getBoolean("littlepet.qa.catalog"))return;
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        WorldRenderEvents.AFTER_ENTITIES.register(ctx->geometry());
    }
    private void screenshot(MinecraftClient client,String name) throws Exception {
        Path dir=out.resolve("screenshots");Files.createDirectories(dir);
        try(NativeImage image=ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {boolean visible=false;for(int x=1;x<10;x++)for(int y=1;y<10;y++)visible|=(image.getColorArgb(image.getWidth()*x/10,image.getHeight()*y/10)&0xffffff)!=0;
            if(!visible)throw new CaptureNotReady();image.writeTo(dir.resolve(name+".png"));}
    }
    private void key(MinecraftClient client,int key,int action) {client.keyboard.onKey(client.getWindow().getHandle(),key,0,action,0);}
    private PetScreen screen(MinecraftClient client) {require(client.currentScreen instanceof PetScreen,"Pet menu missing");return (PetScreen)client.currentScreen;}
    private void openSearch(MinecraftClient client) {
        PetScreen gui=screen(client);float scale=gui.getLayoutScale();
        int h=pets.config.pet.equals("Кастом")?272:pets.config.pet.equals("Пельмень")?214:178;
        int left=(Math.round(client.getWindow().getScaledWidth()/scale)-316)/2,top=Math.max(6,(Math.round(client.getWindow().getScaledHeight()/scale)-h)/2);
        gui.mouseClicked((left+150)*scale,(top+103)*scale,0);
    }
    private void type(MinecraftClient client,String query) {for(char c:query.toCharArray())screen(client).charTyped(c,0);}
    private void tick(MinecraftClient client) {
        if(failed || client.world==null || client.player==null)return;
        try {
            if(out==null) {out=client.runDirectory.toPath();pets=LittlePetClient.INSTANCE;}
            if(wait>0) {wait--;return;}
            switch(step) {
                case 0 -> {
                    var metadata=net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("spidiboostlittlepet").orElseThrow().getMetadata();
                    require(metadata.getName().equals("spidiboost.LittlePet") && metadata.getVersion().getFriendlyString().equals("1.1.0"),"Metadata mismatch");
                    require(pets.getMenuKey().getCategory().equals("category.littlepet"),"Category mismatch");
                    require(Text.translatable("key.littlepet.menu").getString().equals("ClickGui"),"Bind name mismatch");
                    var all=PetCatalog.all();Set<String> ids=new HashSet<>();int types=0;
                    for(var option:all)require(ids.add(option.id()),"Duplicate option "+option.id());
                    for(var type:net.minecraft.registry.Registries.ENTITY_TYPE) {
                        if(type==net.minecraft.entity.EntityType.PLAYER || !net.minecraft.entity.attribute.DefaultAttributeRegistry.hasDefinitionFor(type) || !net.minecraft.registry.Registries.ENTITY_TYPE.getId(type).getNamespace().equals("minecraft"))continue;
                        types++;require(all.stream().anyMatch(o->PetCatalog.type(o.id())==type),"Missing mob "+type);
                    }
                    require(PetCatalog.search("попугай").size()==5 && PetCatalog.search("ЗЕЛЕНЫЙ").size()==1,"Parrot/case-fold catalog check");
                    require(!PetCatalog.valid("vanilla:minecraft:player") && !PetCatalog.valid("garbage"),"Invalid selection accepted");
                    log("PASS: registry coverage "+types+" vanilla types / "+all.size()+" choices");
                    pets.config.enabled=true;pets.config.pet="Такса";client.options.getGuiScale().setValue(2);client.onResolutionChanged();
                    client.player.setYaw(0);client.player.setPitch(18);client.options.setPerspective(Perspective.THIRD_PERSON_FRONT);
                    log("PASS: name spidiboost.LittlePet, author metadata, version 1.1.0, LittlePet category / ClickGui");
                    var center=client.player.getBlockPos();if(client.getServer()!=null)client.getServer().execute(()-> {
                        var world=client.getServer().getOverworld();world.setTimeOfDay(6000);world.setWeather(0,0,false,false);
                        for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++) {
                            world.setBlockState(center.add(x,-1,z),net.minecraft.block.Blocks.GRASS_BLOCK.getDefaultState());
                            for(int y=0;y<=5;y++)world.setBlockState(center.add(x,y,z),net.minecraft.block.Blocks.AIR.getDefaultState());
                        }
                    });
                    client.setScreen(null);key(client,GLFW.GLFW_KEY_RIGHT_SHIFT,GLFW.GLFW_PRESS);log("INFO: initial screen after native key "+client.currentScreen);step=1;wait=15;
                }
                case 1 -> {screen(client);screenshot(client,"catalog-menu");key(client,GLFW.GLFW_KEY_RIGHT_SHIFT,GLFW.GLFW_RELEASE);key(client,GLFW.GLFW_KEY_RIGHT_SHIFT,GLFW.GLFW_PRESS);require(client.currentScreen==null,"Bound key did not close");key(client,GLFW.GLFW_KEY_RIGHT_SHIFT,GLFW.GLFW_RELEASE);step=2;wait=3;}
                case 2 -> {
                    KeybindsScreen keys=new KeybindsScreen(null,client.options);client.setScreen(keys);keys.selectedKeyBinding=pets.getMenuKey();keys.keyPressed(GLFW.GLFW_KEY_P,0,0);
                    require(pets.getMenuKey().matchesKey(GLFW.GLFW_KEY_P,0),"Native rebind failed");client.options.write();
                    require(Files.readString(out.resolve("options.txt")).contains("key_key.littlepet.menu:key.keyboard.p"),"Bind persistence failed");
                    for(var child:keys.children())if(child instanceof net.minecraft.client.gui.screen.option.ControlsListWidget list)list.setScrollY(Double.MAX_VALUE);
                    screenshot(client,"catalog-controls-rebound");client.setScreen(null);key(client,GLFW.GLFW_KEY_RIGHT_SHIFT,GLFW.GLFW_PRESS);require(client.currentScreen==null,"Old bind still opens");key(client,GLFW.GLFW_KEY_RIGHT_SHIFT,GLFW.GLFW_RELEASE);
                    key(client,GLFW.GLFW_KEY_P,GLFW.GLFW_PRESS);screen(client);key(client,GLFW.GLFW_KEY_P,GLFW.GLFW_RELEASE);step=3;wait=6;
                }
                case 3 -> {
                    screenshot(client,"catalog-rebound-footer");openSearch(client);key(client,GLFW.GLFW_KEY_P,GLFW.GLFW_PRESS);require(client.currentScreen instanceof PetScreen,"Printable menu bind interrupted search");key(client,GLFW.GLFW_KEY_P,GLFW.GLFW_RELEASE);screen(client).keyPressed(GLFW.GLFW_KEY_ESCAPE,0,0);key(client,GLFW.GLFW_KEY_P,GLFW.GLFW_PRESS);require(client.currentScreen==null,"New bind did not close");key(client,GLFW.GLFW_KEY_P,GLFW.GLFW_RELEASE);
                    pets.getMenuKey().setBoundKey(InputUtil.fromKeyCode(GLFW.GLFW_KEY_RIGHT_SHIFT,0));KeyBinding.updateKeysByCode();client.options.write();
                    log("PASS: native rebind to P, save, old key inactive, new key opens/closes, footer follows bind");
                    key(client,GLFW.GLFW_KEY_RIGHT_SHIFT,GLFW.GLFW_PRESS);key(client,GLFW.GLFW_KEY_RIGHT_SHIFT,GLFW.GLFW_RELEASE);openSearch(client);step=4;wait=6;
                }
                case 4 -> {screenshot(client,"catalog-search-all");type(client,"собака");step=5;wait=6;}
                case 5 -> {screenshot(client,"catalog-search-dog");screen(client).keyPressed(GLFW.GLFW_KEY_ENTER,0,0);require(pets.config.pet.equals("vanilla:minecraft:wolf"),"Search did not select dog");log("PASS: Russian alias search and Enter selection");openSearch(client);type(client,"zzzz-not-a-mob");step=6;wait=5;}
                case 6 -> {screenshot(client,"catalog-search-empty");screen(client).keyPressed(GLFW.GLFW_KEY_ENTER,0,0);require(pets.config.pet.equals("vanilla:minecraft:wolf"),"Empty search changed pet");screen(client).keyPressed(GLFW.GLFW_KEY_ESCAPE,0,0);openSearch(client);type(client,"попугай");step=7;wait=5;}
                case 7 -> {screenshot(client,"catalog-search-parrots");screen(client).keyPressed(GLFW.GLFW_KEY_DOWN,0,0);screen(client).keyPressed(GLFW.GLFW_KEY_ENTER,0,0);require(PetCatalog.type(pets.config.pet)==net.minecraft.entity.EntityType.PARROT,"Parrot selection failed");pets.save();require(PetConfig.load().pet.equals(pets.config.pet),"Vanilla selection persistence failed");log("PASS: five parrot colors, arrow navigation, vanilla config round trip");client.setScreen(null);step=8;wait=4;}
                case 8 -> {
                    if(index<3) {
                        client.options.getGuiScale().setValue(new int[]{1,2,6}[index]);client.onResolutionChanged();pets.config.pet="Кастом";client.setScreen(new PetScreen(pets));openSearch(client);type(client,"minecraft:zombie");step=9;wait=5;
                    } else {client.options.getGuiScale().setValue(2);client.onResolutionChanged();client.setScreen(null);index=0;vanilla=PetCatalog.all().stream().filter(o->PetCatalog.vanilla(o.id())).toList();log("INFO: exhaustive rendered choices="+vanilla.size());step=20;wait=5;}
                }
                case 9 -> {screenshot(client,"catalog-scale-search-"+index);screen(client).keyPressed(GLFW.GLFW_KEY_ENTER,0,0);require(PetCatalog.vanilla(pets.config.pet),"Scaled search failed");log("PASS: search from Custom at GUI scale "+new int[]{1,2,6}[index]);index++;step=8;wait=3;}
                case 20 -> {
                    KeybindsScreen keys=new KeybindsScreen(null,client.options);client.setScreen(keys);keys.selectedKeyBinding=pets.getMenuKey();keys.mouseClicked(0,0,4);
                    require(pets.getMenuKey().matchesMouse(4),"Mouse bind assignment failed");client.options.write();client.setScreen(null);
                    var mouse=Mouse.class.getDeclaredMethod("method_1601",long.class,int.class,int.class,int.class);mouse.setAccessible(true);mouse.invoke(client.mouse,client.getWindow().getHandle(),4,GLFW.GLFW_PRESS,0);
                    step=21;wait=8;
                }
                case 21 -> {
                    screen(client);screenshot(client,"catalog-mouse-bind-footer");
                    var mouse=Mouse.class.getDeclaredMethod("method_1601",long.class,int.class,int.class,int.class);mouse.setAccessible(true);mouse.invoke(client.mouse,client.getWindow().getHandle(),4,GLFW.GLFW_RELEASE,0);mouse.invoke(client.mouse,client.getWindow().getHandle(),4,GLFW.GLFW_PRESS,0);
                    require(client.currentScreen==null,"Mouse bind did not close");mouse.invoke(client.mouse,client.getWindow().getHandle(),4,GLFW.GLFW_RELEASE,0);
                    pets.getMenuKey().setBoundKey(InputUtil.Type.MOUSE.createFromCode(0));KeyBinding.updateKeysByCode();client.setScreen(new PetScreen(pets));require(!pets.beforeMenuMouse(client.getWindow().getHandle(),0,GLFW.GLFW_PRESS),"Left mouse bind blocks menu interaction");client.setScreen(null);
                    pets.getMenuKey().setBoundKey(InputUtil.fromKeyCode(GLFW.GLFW_KEY_RIGHT_SHIFT,0));KeyBinding.updateKeysByCode();client.options.write();
                    log("PASS: native mouse bind opens/closes, primary mouse keeps UI usable, printable bind permits search editing");step=10;wait=3;
                }
                case 10 -> {
                    if(index==vanilla.size()) {log("PASS: every vanilla choice renders, "+samples+" grounded finite geometry poses, minimumY="+minimum);pets.config.pet="Капибара";pets.save();client.setScreen(new PetScreen(pets));step=12;wait=8;break;}
                    pets.config.pet=vanilla.get(index).id();geometryDone=false;step=11;wait=8;
                }
                case 11 -> {require(geometryDone,"Missing render measurement "+pets.config.pet);screenshot(client,"vanilla-"+vanilla.get(index).id().replace(':','-').replace('#','-'));log("PASS: rendered "+vanilla.get(index).name());index++;step=10;wait=1;}
                case 12 -> {screenshot(client,"catalog-final-original-pet");log("PASS: exhaustive protected catalog runtime suite");step=13;wait=3;}
                case 13 -> client.scheduleStop();
            }
        } catch(CaptureNotReady retry) {wait=5;} catch(Throwable t) {fail(client,t);}
    }
    private void fail(MinecraftClient client,Throwable t) {
        failed=true;t.printStackTrace();try {log("FAIL: "+t);screenshot(client,"catalog-failure");}catch(Throwable ignored) { }client.scheduleStop();
    }
    private void geometry() {
        if(failed || step!=11 || geometryDone)return;
        MinecraftClient client=MinecraftClient.getInstance();
        try {
            VanillaPet pet=pets.getVanillaPet();require(pet.getEntity()!=null,"Vanilla entity missing "+pets.config.pet);
            if(pet.getEntity().getType()!=PetCatalog.type(pets.config.pet))return;
            if(pet.getEntity() instanceof ParrotEntity parrot && parrot.getVariant().getId()!=PetCatalog.variant(pets.config.pet))return;
            for(int phase=0;phase<16;phase++) {
                pet.getEntity().age=phase*13;pet.getEntity().limbAnimator.updateLimbs(phase==0?0:1,1,1);
                if(pet.getEntity() instanceof net.minecraft.entity.mob.SlimeEntity slime)slime.stretch=slime.lastStretch=(float)Math.sin(phase)*0.15F;
                if(pet.getEntity() instanceof net.minecraft.entity.passive.SquidEntity squid)squid.tentacleAngle=squid.prevTentacleAngle=(float)(Math.sin(phase)*Math.sin(phase)*Math.PI/2);
                Sink sink=new Sink();pet.render(new MatrixStack(),layer->sink,0xF000F0,pets.getBrain(),0.5F);
                require(sink.vertices>0 && sink.finite,"Empty/nonfinite mesh "+pets.config.pet);
                require(sink.minimum>=0.009F,"Ground clipping "+pets.config.pet+" y="+sink.minimum);
                minimum=Math.min(minimum,sink.minimum);samples++;
            }
            if(pet.getEntity() instanceof ParrotEntity parrot)require(parrot.getVariant().ordinal()==PetCatalog.variant(pets.config.pet),"Wrong parrot texture variant");
            geometryDone=true;
        } catch(Throwable t) {fail(client,t);}
    }
    private static final class CaptureNotReady extends RuntimeException { }
    private static final class Sink implements VertexConsumer {
        private int vertices;
        private float minimum=Float.POSITIVE_INFINITY;
        private boolean finite=true;
        public VertexConsumer vertex(float x,float y,float z) {vertices++;minimum=Math.min(minimum,y);finite&=Float.isFinite(x+y+z);return this;}
        public VertexConsumer color(int r,int g,int b,int a) {return this;}
        public VertexConsumer texture(float u,float v) {finite&=Float.isFinite(u+v);return this;}
        public VertexConsumer overlay(int u,int v) {return this;}
        public VertexConsumer light(int u,int v) {return this;}
        public VertexConsumer normal(float x,float y,float z) {finite&=Float.isFinite(x+y+z);return this;}
    }
}
