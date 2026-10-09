package com.spidiboost.littlepet.custom;
import java.util.function.Supplier;
public class TextValue {
    private String value="";
    private final Supplier<Boolean> visible;
    private boolean focused;
    public TextValue(String name,String initial,Supplier<Boolean> visible) {this.visible=visible;setValue(initial);}
    public String getValue() { return value; }
    public void setValue(String text) { value=text==null?"":text; }
    public boolean isVisible() { return visible.get(); }
    public void setFocused(boolean focus) { focused=focus; }
    public boolean isFocused() { return focused; }
}
