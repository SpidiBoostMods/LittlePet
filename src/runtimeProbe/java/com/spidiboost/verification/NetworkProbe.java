package com.spidiboost.verification;

import com.spidiboost.littlepet.custom.OfficialSkinService;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.TitleScreen;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

public final class NetworkProbe implements ClientModInitializer {
    private boolean started;
    @Override public void onInitializeClient() {
        if (!Boolean.getBoolean("littlepet.qa.network")) return;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (started || !(client.currentScreen instanceof TitleScreen) || client.getOverlay()!=null) return;
            started=true;
            CompletableFuture.runAsync(() -> {
                String result;
                try {
                    var skin=OfficialSkinService.shared().fetch("Notch");
                    if(skin.image().getWidth()!=64 || !skin.name().equalsIgnoreCase("Notch"))throw new AssertionError("Invalid live skin");
                    result="PASS native Minecraft initialization and live profile, session, texture downloads\n";
                } catch(Throwable error) { result="FAIL "+error+"\n"; }
                try { Files.writeString(client.runDirectory.toPath().resolve("network-results.txt"),result); }
                catch(Exception error) { throw new RuntimeException(error); }
                finally { client.execute(client::scheduleStop); }
            });
        });
    }
}
