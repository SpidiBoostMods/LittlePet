package com.spidiboost.littlepet.custom;

import com.spidiboost.littlepet.custom.TextValue;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import java.nio.file.*;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

 
public final class ImagePathSetting extends TextValue {
    private static final ExecutorService PICKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r,"LittlePet file picker");t.setDaemon(true);return t;
    });
    private final AtomicBoolean selecting = new AtomicBoolean();
    private volatile String status = "Выберите изображение";
    private volatile boolean error;
    private volatile long revision;

    public ImagePathSetting(Supplier<Boolean> visible) { super("Лицо","",visible); }
    @Override public void setValue(String value) { super.setValue(value==null?"":value);revision++; }
    public long getRevision() { return revision; }
    public String getStatus() { return status; }
    public boolean hasError() { return error; }
    public boolean isSelecting() { return selecting.get(); }
    public void status(String value,boolean failed) { status=value;error=failed; }

    public void browse() {
        if (!isVisible() || !selecting.compareAndSet(false,true)) return;
        String current = getValue();
        PICKER.execute(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                String initial = System.getProperty("user.home") + java.io.File.separator;
                try {
                    Path p = parsePath(current);
                    if (p != null && p.getParent() != null) initial=p.toString();
                } catch (RuntimeException ignored) { }
                boolean mac = System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("mac");
                PointerBuffer patterns = null;
                
                
                if (!mac) {
                    String[] formats={"*.png","*.jpg","*.jpeg","*.bmp","*.gif","*.tif","*.tiff","*.PNG","*.JPG","*.JPEG","*.BMP","*.GIF","*.TIF","*.TIFF"};
                    patterns=stack.mallocPointer(formats.length);
                    for(String format:formats)patterns.put(stack.UTF8(format));
                    patterns.flip();
                }
                boolean windows=System.getProperty("os.name","").toLowerCase(Locale.ROOT).startsWith("windows");
                if(windows)TinyFileDialogs.tinyfd_setGlobalInt(TinyFileDialogs.tinyfd_winUtf8,1);
                String selected=TinyFileDialogs.tinyfd_openFileDialog("LittlePet - Choose face image",initial,patterns,
                        mac ? null : "Images (PNG, JPG, JPEG, BMP, GIF, TIFF)",false);
                if(selected!=null)MinecraftClient.getInstance().execute(() -> {
                    setValue(Path.of(selected).toAbsolutePath().normalize().toString());
                    setFocused(false);status("Загрузка изображения…",false);
                });
            } catch (RuntimeException | LinkageError e) {
                MinecraftClient.getInstance().execute(() -> status("Не удалось открыть окно. Вставьте путь вручную.",true));
            } finally { selecting.set(false); }
        });
    }

    public static Path parsePath(String value) {
        String raw=value==null?"":value.strip();
        if(raw.length()>=2 && ((raw.startsWith("\"")&&raw.endsWith("\""))||(raw.startsWith("'")&&raw.endsWith("'"))))raw=raw.substring(1,raw.length()-1);
        if(raw.isEmpty())return null;
        if(raw.startsWith("file:"))return Path.of(java.net.URI.create(raw)).toAbsolutePath().normalize();
        if(raw.equals("~"))raw=System.getProperty("user.home");
        else if(raw.startsWith("~/")||raw.startsWith("~\\"))raw=System.getProperty("user.home")+raw.substring(1);
        return Path.of(raw).toAbsolutePath().normalize();
    }
}
