package com.spidiboost.littlepet.custom;

import com.spidiboost.littlepet.custom.TextValue;
import java.util.function.Supplier;

public final class SkinNameSetting extends TextValue {
    private String applied="";
    private long revision;
    private boolean editing;
    private volatile String status="Ник лицензии → Enter";
    private volatile boolean error;
    public SkinNameSetting(Supplier<Boolean> visible) { super("Скин","",visible); }
    public void beginEdit() { editing=true; }
    public boolean isEditing() { return editing; }
    public boolean apply() {
        String nickname=getValue().strip();
        if(!nickname.isEmpty() && !OfficialSkinService.validName(nickname)) {
            status("Ник: 1–16 латинских букв, цифр или _",true);return false;
        }
        setValue(nickname);applied=nickname;revision++;
        status(nickname.isEmpty()?"Стандартный Стив":"Загрузка скина…",false);return true;
    }
    public String getApplied() { return applied; }
    public long getRevision() { return revision; }
    public String getStatus() { return status; }
    public boolean hasError() { return error; }
    public void status(String text,boolean failed) { status=text;error=failed; }
}
