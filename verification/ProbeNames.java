import com.google.gson.Gson;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

public final class ProbeNames {
    private static final Map<String, String> REVERSE = new HashMap<>();
    private static final Map<String, String> FIELDS = new HashMap<>();
    static {
        try {
            Map<?, ?> data = new Gson().fromJson(Files.readString(Path.of(System.getProperty("protection.map"))), Map.class);
            ((Map<?, ?>) data.get("classes")).forEach((original, mapped) -> REVERSE.put(mapped.toString(), original.toString()));
            ((Map<?, ?>) data.get("fields")).forEach((original, mapped) -> FIELDS.put(original.toString(), mapped.toString()));
        } catch (Exception error) { throw new ExceptionInInitializerError(error); }
    }
    public static Field field(Class<?> owner, String name) throws ReflectiveOperationException {
        String original = REVERSE.get(owner.getName().replace('.', '/'));
        if (original != null) {
            String prefix = original + "." + name + ":";
            for (var entry : FIELDS.entrySet()) {
                if (entry.getKey().startsWith(prefix)) return owner.getDeclaredField(entry.getValue());
            }
        }
        return owner.getDeclaredField(name);
    }
}
