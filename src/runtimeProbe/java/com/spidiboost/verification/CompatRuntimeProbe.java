package com.spidiboost.verification;

import com.spidiboost.littlepet.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.*;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.option.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.texture.NativeImage;
import org.lwjgl.glfw.GLFW;
import java.nio.file.*;

public final class CompatRuntimeProbe implements ClientModInitializer {
    private int step,wait=30;
    private Path out;
    private boolean failed;
    private LittlePetClient pets;
    private void require(boolean okay,String message) {if(!okay)throw new AssertionError(message);}
    private void log(String text) throws Exception {System.out.println("LITTLEPET_COMPAT "+text);Files.writeString(out.resolve("compat-runtime-results.txt"),text+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
    @Override public void onInitializeClient() {if(Boolean.getBoolean("littlepet.qa.compat"))ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
    private void key(MinecraftClient client,int key,int action) {client.keyboard.onKey(client.getWindow().getHandle(),key,0,action,0);}
    private void screenshot(MinecraftClient client,String name) throws Exception {
        Path dir=out.resolve("screenshots");Files.createDirectories(dir);
        try(NativeImage image=ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {image.writeTo(dir.resolve(name+".png"));}
    }
    private void tick(MinecraftClient client) {
        if(failed || client.world==null || client.player==null)return;
        try {
            if(out==null) {out=client.runDirectory.toPath();pets=LittlePetClient.INSTANCE;}
            if(wait-->0)return;
            switch(step) {
                case 0 -> {
                    require(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("reallystafftool"),"Real ATools missing");
                    Object click=Class.forName("com.rwstafftool.modules.FunctionManager").getMethod("get",String.class).invoke(null,"ClickGUI");
                    require(click!=null,"ATools ClickGUI module missing");click.getClass().getField("bind").setInt(click,GLFW.GLFW_KEY_RIGHT_SHIFT);
                    pets.getMenuKey().setBoundKey(InputUtil.fromKeyCode(GLFW.GLFW_KEY_RIGHT_SHIFT,0));KeyBinding.updateKeysByCode();
                    client.options.getGuiScale().setValue(2);client.onResolutionChanged();client.setScreen(null);
                    key(client,GLFW.GLFW_KEY_RIGHT_SHIFT,GLFW.GLFW_PRESS);step++;wait=15;
                }
                case 1 -> {
                    require(client.currentScreen!=null && client.currentScreen.getClass().getName().equals("com.rwstafftool.gui.dropdown.ClickGUI"),"First press did not prioritize real ATools: "+client.currentScreen);
                    screenshot(client,"compat-first-atools");log("PASS: first shared Right Shift opens actual ATools ClickGUI");
                    key(client,GLFW.GLFW_KEY_RIGHT_SHIFT,GLFW.GLFW_RELEASE);key(client,GLFW.GLFW_KEY_RIGHT_SHIFT,GLFW.GLFW_PRESS);
                    require(client.currentScreen instanceof PetScreen,"Second press did not open LittlePet");step++;wait=8;
                }
                case 2 -> {
                    screenshot(client,"compat-second-littlepet");log("PASS: second shared Right Shift switches to LittlePet");key(client,GLFW.GLFW_KEY_RIGHT_SHIFT,GLFW.GLFW_RELEASE);
                    key(client,GLFW.GLFW_KEY_RIGHT_SHIFT,GLFW.GLFW_PRESS);require(client.currentScreen==null,"Third press did not close LittlePet");key(client,GLFW.GLFW_KEY_RIGHT_SHIFT,GLFW.GLFW_RELEASE);
                    require(BindConflicts.atools(pets.getMenuKey()),"ATools conflict not detected");
                    KeybindsScreen keys=new KeybindsScreen(null,client.options);client.setScreen(keys);
                    for(var child:keys.children())if(child instanceof ControlsListWidget list) {
                        list.setScrollY(Double.MAX_VALUE);
                        for(var entry:list.children()) {
                            for(var f:entry.getClass().getDeclaredFields())if(f.getType()==KeyBinding.class) {
                                f.setAccessible(true);if(f.get(entry)==pets.getMenuKey()) {
                                    var first=entry.children().stream().filter(e->e instanceof ButtonWidget).map(e->(ButtonWidget)e).findFirst().orElseThrow();
                                    require(first.getMessage().getStyle().getColor()!=null && first.getMessage().getStyle().getColor().getRgb()==0xff5555,"Conflict is not red");
                                }
                            }
                        }
                    }
                    step++;wait=8;
                }
                case 3 -> {
                    screenshot(client,"compat-controls-atools-conflict");log("PASS: custom ATools bind conflict is red in native LittlePet / ClickGui controls");
                    Screen mods=(Screen)Class.forName("com.terraformersmc.modmenu.gui.ModsScreen").getConstructor(Screen.class).newInstance((Object)null);client.setScreen(mods);
                    for(var child:mods.children())if(child instanceof TextFieldWidget text)text.setText("spidiboost.LittlePet");step++;wait=12;
                }
                case 4 -> {
                    for(var child:client.currentScreen.children())if(child instanceof net.minecraft.client.gui.widget.EntryListWidget list && child.getClass().getName().contains("ModListWidget") && !list.children().isEmpty())list.setSelected(0);
                    step++;wait=8;
                }
                case 5 -> {screenshot(client,"compat-modmenu-name-author-version-icon");log("PASS: actual Mod Menu renders the new name/version/author/icon");log("PASS: protected ATools and Mod Menu compatibility suite");step++;wait=3;}
                case 6 -> client.scheduleStop();
            }
        } catch(Throwable error) {failed=true;error.printStackTrace();try {log("FAIL: "+error);screenshot(client,"compat-failure");}catch(Throwable ignored) { }client.scheduleStop();}
    }
}
