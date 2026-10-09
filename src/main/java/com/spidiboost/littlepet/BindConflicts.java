package com.spidiboost.littlepet;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.gui.screen.Screen;
import java.util.Collection;

public final class BindConflicts {
    private static boolean matches(KeyBinding binding,int code) {
        if(code>0)return binding.matchesKey(code,0);
        if(code<=-2 && code>=-9)return binding.matchesMouse(-code-2);
        return code<=-90 && code>=-100 && binding.matchesMouse(code+100);
    }
    public static Screen atoolsMenu(KeyBinding binding) {
        if(binding==null || binding.isUnbound())return null;
        try {
            Class<?> type=Class.forName("com.rwstafftool.modules.FunctionManager",false,BindConflicts.class.getClassLoader());
            Object click=type.getMethod("get",String.class).invoke(null,"ClickGUI");
            if(click!=null && matches(binding,click.getClass().getField("bind").getInt(click))) {
                return (Screen)Class.forName("com.rwstafftool.gui.dropdown.ClickGUI").getConstructor().newInstance();
            }
        } catch(ReflectiveOperationException | LinkageError ignored) { }
        return null;
    }
    public static boolean atools(KeyBinding binding) {
        if(binding==null || binding.isUnbound())return false;
        try {
            Class<?> type=Class.forName("com.rwstafftool.modules.FunctionManager",false,BindConflicts.class.getClassLoader());
            Object modules=type.getField("functions").get(null);
            if(modules instanceof Collection<?> values)for(Object value:values) {
                int code=value.getClass().getField("bind").getInt(value);
                if(matches(binding,code))return true;
            }
        } catch(ReflectiveOperationException | LinkageError ignored) { }
        return false;
    }
}
