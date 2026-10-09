package com.spidiboost.littlepet;

import com.spidiboost.littlepet.custom.*;
import com.spidiboost.littlepet.model.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;
import org.lwjgl.glfw.GLFW;

public final class LittlePetClient implements ClientModInitializer {
    public static LittlePetClient INSTANCE;
    public final PetConfig config=PetConfig.load();
    public final ImagePathSetting face=new ImagePathSetting(() -> config.pet.equals("Кастом"));
    public final SkinNameSetting skin=new SkinNameSetting(() -> config.pet.equals("Кастом"));
    private final PetBrain brain=new PetBrain();
    private final TaksaModel taksa=new TaksaModel();
    private final CrocodileModel crocodile=new CrocodileModel();
    private final CapybaraModel capybara=new CapybaraModel();
    private final DachshundModel dachshund=new DachshundModel();
    private final DumplingFamily dumpling=new DumplingFamily();
    private final CustomSteveModel steve=new CustomSteveModel();
    private KeyBinding menu;
    private final VanillaPet vanilla=new VanillaPet();
    private final PlayerName playerName=new PlayerName();
    private String lastPet;
    private String savedFace;
    private net.minecraft.client.world.ClientWorld lastWorld;
    private boolean wasEnabled;
    private LivingEntity currentTarget;
    public boolean saved=true;
    @Override public void onInitializeClient() {
        INSTANCE=this;playerName.update(MinecraftClient.getInstance().getSession().getUsername(),null);face.setValue(config.face);skin.setValue(config.skin);if(!config.skin.isBlank())skin.apply();savedFace=config.face;
        menu=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.littlepet.menu",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_RIGHT_SHIFT,"category.littlepet"));
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        WorldRenderEvents.AFTER_ENTITIES.register(this::render);
        AttackEntityCallback.EVENT.register((player,world,hand,target,hit) -> {
            if(world.isClient && config.enabled && !config.pet.equals("Кастом") && player==MinecraftClient.getInstance().player && target instanceof LivingEntity living && target!=player) {
                currentTarget=living;brain.setAttackTarget(living);
            }
            return ActionResult.PASS;
        });
    }
    private void tick(MinecraftClient client) {
        while(menu.wasPressed()) {
            if(client.currentScreen instanceof PetScreen screen) {if(!screen.isEditing())client.setScreen(null);}
            else if(client.currentScreen==null)client.setScreen(new PetScreen(this));
        }
        if(!config.pet.equals(lastPet)) {
            capybara.resetAnimation();dachshund.resetAnimation();dumpling.resetAnimation();vanilla.reset();lastPet=config.pet;
        }
        if(!face.getValue().equals(savedFace)) {savedFace=face.getValue();config.face=savedFace;save();}
        if(client.world!=lastWorld || config.enabled!=wasEnabled) {
            brain.reset();currentTarget=null;lastWorld=client.world;wasEnabled=config.enabled;
            capybara.resetAnimation();dachshund.resetAnimation();dumpling.resetAnimation();vanilla.reset();
        }
        if(!config.enabled || client.world==null || client.player==null)return;
        if(config.pet.equals("Кастом")) {brain.clearAttackTarget();currentTarget=null;}
        if(currentTarget!=null && (currentTarget.isRemoved() || currentTarget.isDead() || currentTarget.getWorld()!=client.world)) {currentTarget=null;brain.clearAttackTarget();}
        if(currentTarget!=null && currentTarget.getPos().distanceTo(brain.getPos())>16) {currentTarget=null;brain.clearAttackTarget();}
        brain.setEntity(client.player);brain.tick();vanilla.tick(config.pet,brain);
    }
    public KeyBinding getMenuKey() {return menu;}
    public VanillaPet getVanillaPet() {return vanilla;}
    public String menuKeyLabel() {
        return menu.matchesKey(GLFW.GLFW_KEY_RIGHT_SHIFT,0)?"Right Shift":menu.getBoundKeyLocalizedText().getString();
    }
    public boolean matchesMenuKey(int key,int scan) {return menu!=null && menu.matchesKey(key,scan);}
    private boolean atoolsMenu(net.minecraft.client.gui.screen.Screen screen) {
        if(screen==null)return false;
        for(Class<?> type=screen.getClass();type!=null;type=type.getSuperclass()) {
            String name=type.getName();
            if(name.startsWith("com.rwstafftool.") && name.toLowerCase(java.util.Locale.ROOT).contains("clickgui"))return true;
        }
        return false;
    }
    private void drainMenuKey() {while(menu.wasPressed()) { }}
    private boolean beforeMenuInput(long window,boolean matches,int action) {
        MinecraftClient client=MinecraftClient.getInstance();
        if(action!=GLFW.GLFW_PRESS || !matches || window!=client.getWindow().getHandle() || client.player==null)return false;
        if(client.currentScreen instanceof PetScreen screen) {screen.close();drainMenuKey();return true;}
        if(atoolsMenu(client.currentScreen)) {client.setScreen(new PetScreen(this));drainMenuKey();return true;}
        if(client.currentScreen==null) {
            var screen=BindConflicts.atoolsMenu(menu);
            client.setScreen(screen==null?new PetScreen(this):screen);drainMenuKey();return true;
        }
        return false;
    }
    public boolean beforeMenuKey(long window,int key,int scan,int action) {
        if(MinecraftClient.getInstance().currentScreen instanceof PetScreen screen && !screen.menuCloseKey(key,scan))return false;
        return beforeMenuInput(window,matchesMenuKey(key,scan),action);
    }
    public boolean beforeMenuMouse(long window,int button,int action) {
        if(button<3 && MinecraftClient.getInstance().currentScreen instanceof PetScreen)return false;
        return beforeMenuInput(window,menu!=null && menu.matchesMouse(button),action);
    }
    public String getPlayerName() {
        MinecraftClient client=MinecraftClient.getInstance();
        return playerName.update(client.getSession().getUsername(),client.player==null?null:client.player.getName().getString());
    }
    public PetBrain getBrain() { return brain; }
    public void save() { saved=config.save(); }
    private void render(WorldRenderContext context) {
        MinecraftClient client=MinecraftClient.getInstance();
        if(!config.enabled || client.world==null || client.player==null || !brain.isSpawned())return;
        MatrixStack matrices=context.matrixStack();VertexConsumerProvider consumers=context.consumers();
        if(matrices==null || consumers==null)return;
        float delta=context.tickCounter().getTickDelta(true),age=client.player.age+delta;
        Vec3d pos=brain.getPos(),camera=context.camera().getPos();
        int light=WorldRenderer.getLightmapCoordinates(client.world,BlockPos.ofFloored(pos));
        int overlay=OverlayTexture.DEFAULT_UV;
        matrices.push();
        try {
            matrices.translate(pos.x-camera.x,pos.y-camera.y,pos.z-camera.z);
            if(PetCatalog.vanilla(config.pet))vanilla.render(matrices,consumers,light,brain,delta);
            else switch(config.pet) {
                case "Крокодил" -> crocodile.render(matrices,consumers.getBuffer(CrocodileModel.getRenderLayer()),light,overlay,brain,age);
                case "Капибара" -> {capybara.updatePose(brain,age);capybara.renderModel(matrices,consumers.getBuffer(CapybaraModel.getRenderLayer()),light,overlay,brain.getBody());capybara.renderModel(matrices,consumers.getBuffer(capybara.getEyeLayer(age)),light,overlay,brain.getBody());}
                case "Такса 2" -> {dachshund.updatePose(brain,age);dachshund.renderModel(matrices,consumers.getBuffer(DachshundModel.getRenderLayer()),light,overlay,brain.getBody());}
                case "Пельмень" -> {dumpling.updatePose(brain,age,config.family);dumpling.renderModel(matrices,consumers.getBuffer(DumplingFamily.getRenderLayer()),light,overlay,brain.getBody());}
                case "Кастом" -> {steve.updatePose(brain,face,skin);steve.renderModel(matrices,consumers.getBuffer(steve.getRenderLayer()),light,overlay,brain.getBody());}
                default -> taksa.render(matrices,consumers.getBuffer(TaksaModel.getRenderLayer()),light,overlay,brain,age);
            }
        } finally {matrices.pop();}
    }
}
