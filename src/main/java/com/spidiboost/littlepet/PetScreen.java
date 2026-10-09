package com.spidiboost.littlepet;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.List;

public final class PetScreen extends Screen {
    private final LittlePetClient pets;
    private int left,top,cardHeight,scroll,selected;
    private float layoutScale=1;
    private TextFieldWidget face,skin,search;
    private boolean choosing;
    private String layoutPet;
    private List<PetCatalog.Option> results=List.of();
    private static final int WIDTH=316,WHITE=0xfff4f2ff,MUTED=0xffa2a0b5,ACCENT=0xffbca1ff;
    public PetScreen(LittlePetClient pets) {super(Text.literal("LittlePet"));this.pets=pets;}
    @Override protected void init() {
        layoutPet=pets.config.pet;cardHeight=pets.config.pet.equals("Кастом")?272:pets.config.pet.equals("Пельмень")?214:178;
        layoutScale=Math.min(1F,Math.min((width-12F)/WIDTH,(height-12F)/cardHeight));
        left=(Math.round(width/layoutScale)-WIDTH)/2;top=Math.max(6,(Math.round(height/layoutScale)-cardHeight)/2);
        face=new TextFieldWidget(textRenderer,left+22,top+140,WIDTH-80,20,Text.literal("Лицо"));
        face.setMaxLength(4096);face.setText(pets.face.getValue());face.setPlaceholder(Text.literal("Путь к изображению"));
        face.setChangedListener(value -> {pets.face.setValue(value);pets.config.face=value;});
        skin=new TextFieldWidget(textRenderer,left+22,top+192,WIDTH-44,20,Text.literal("Скин"));
        skin.setMaxLength(16);skin.setText(pets.skin.getValue());skin.setPlaceholder(Text.literal("Ник игрока → Enter"));skin.setChangedListener(value -> {pets.skin.beginEdit();pets.skin.setValue(value);});
        if(pets.config.pet.equals("Кастом")) {addDrawableChild(face);addDrawableChild(skin);}
        search=new TextFieldWidget(textRenderer,left+26,dropdownY()+8,WIDTH-52,20,Text.literal("Поиск питомца"));
        search.setMaxLength(96);search.setPlaceholder(Text.literal("Поиск · название или ID"));search.setChangedListener(value -> updateSearch(value));
        updateSearch("");
    }
    private int popupHeight() {return Math.min(226,Math.round(height/layoutScale)-16);}
    private int rows() {return Math.max(1,(popupHeight()-58)/24);}
    private int dropdownY() {return Math.max(8,Math.min(top+120,Math.round(height/layoutScale)-popupHeight()-8));}
    private int listY() {return dropdownY()+36;}
    private void updateSearch(String query) {results=PetCatalog.search(query);scroll=0;selected=0;}
    private void select(String pet) {pets.config.pet=pet;choosing=false;pets.save();clearAndInit();}
    private void openSearch() {choosing=true;setFocused(null);face.setFocused(false);skin.setFocused(false);search.setText("");search.setFocused(true);updateSearch("");}
    private void moveSelection(int amount) {
        selected=Math.max(0,Math.min(results.size()-1,selected+amount));
        if(selected<scroll)scroll=selected;if(selected>=scroll+rows())scroll=selected-rows()+1;
    }
    @Override public void tick() {if(!pets.config.pet.equals(layoutPet)) {choosing=false;clearAndInit();return;}if(face!=null && !face.isFocused() && !face.getText().equals(pets.face.getValue()))face.setText(pets.face.getValue());}
    @Override public boolean shouldPause() {return false;}
    @Override public void close() {pets.save();client.setScreen(null);}
    public boolean isEditing() {return choosing || pets.config.pet.equals("Кастом") && (face.isFocused() || skin.isFocused());}
    public boolean menuCloseKey(int key,int scan) {
        if(!pets.matchesMenuKey(key,scan))return false;
        return !isEditing() || key>=GLFW.GLFW_KEY_F1 && key<=GLFW.GLFW_KEY_F25 || key>=GLFW.GLFW_KEY_LEFT_SHIFT && key<=GLFW.GLFW_KEY_MENU;
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(menuCloseKey(key,scan)) {close();return true;}
        if(choosing) {
            if(key==GLFW.GLFW_KEY_ESCAPE) {choosing=false;clearAndInit();return true;}
            if(key==GLFW.GLFW_KEY_DOWN || key==GLFW.GLFW_KEY_UP) {moveSelection(key==GLFW.GLFW_KEY_DOWN?1:-1);return true;}
            if(key==GLFW.GLFW_KEY_PAGE_DOWN || key==GLFW.GLFW_KEY_PAGE_UP) {moveSelection(key==GLFW.GLFW_KEY_PAGE_DOWN?rows():-rows());return true;}
            if(key==GLFW.GLFW_KEY_ENTER || key==GLFW.GLFW_KEY_KP_ENTER) {if(!results.isEmpty())select(results.get(selected).id());return true;}
            search.keyPressed(key,scan,modifiers);return true;
        }
        if((key==GLFW.GLFW_KEY_ENTER || key==GLFW.GLFW_KEY_KP_ENTER) && pets.config.pet.equals("Кастом")) {
            if(skin.isFocused()) {if(pets.skin.apply()) {pets.config.skin=pets.skin.getApplied();pets.save();}return true;}
            if(face.isFocused()) {face.setFocused(false);pets.save();return true;}
        }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public void renderBackground(DrawContext ctx,int mouseX,int mouseY,float delta) { }
    @Override public boolean charTyped(char chr,int modifiers) {return choosing?search.charTyped(chr,modifiers):super.charTyped(chr,modifiers);}
    private boolean hit(double x,double y,int a,int b,int w,int h) {return x>=a && y>=b && x<a+w && y<b+h;}
    public float getLayoutScale() {return layoutScale;}
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy) {return choosing?search.mouseDragged(mx/layoutScale,my/layoutScale,button,dx/layoutScale,dy/layoutScale):super.mouseDragged(mx/layoutScale,my/layoutScale,button,dx/layoutScale,dy/layoutScale);}
    @Override public boolean mouseReleased(double mx,double my,int button) {return choosing?search.mouseReleased(mx/layoutScale,my/layoutScale,button):super.mouseReleased(mx/layoutScale,my/layoutScale,button);}
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        if(choosing) {scroll=Math.max(0,Math.min(Math.max(0,results.size()-rows()),scroll-(int)Math.signum(vertical)*3));selected=Math.max(scroll,Math.min(selected,scroll+rows()-1));return true;}
        return super.mouseScrolled(mx,my,horizontal,vertical);
    }
    @Override public boolean mouseClicked(double mx,double my,int button) {
        mx/=layoutScale;my/=layoutScale;
        if(button!=0)return choosing || super.mouseClicked(mx,my,button);
        if(choosing) {
            if(search.mouseClicked(mx,my,button))return true;
            if(hit(mx,my,left+18,listY(),WIDTH-36,rows()*24)) {
                int index=scroll+(int)((my-listY())/24);if(index<results.size())select(results.get(index).id());return true;
            }
            if(hit(mx,my,left+16,dropdownY()-2,WIDTH-32,popupHeight()+4)) {search.setFocused(true);return true;}
            choosing=false;clearAndInit();return true;
        }
        if(hit(mx,my,left+WIDTH-32,top+14,18,18)) {close();return true;}
        if(hit(mx,my,left+WIDTH-68,top+56,48,22)) {pets.config.enabled=!pets.config.enabled;pets.save();return true;}
        if(hit(mx,my,left+18,top+92,WIDTH-36,24)) {openSearch();return true;}
        if(pets.config.pet.equals("Пельмень") && hit(mx,my,left+WIDTH-68,top+131,48,22)) {pets.config.family=!pets.config.family;pets.save();return true;}
        if(pets.config.pet.equals("Кастом") && hit(mx,my,left+WIDTH-48,top+140,26,20)) {face.setFocused(false);pets.face.browse();return true;}
        return super.mouseClicked(mx,my,button);
    }
    @Override public void render(DrawContext ctx,int mx,int my,float delta) {
        ctx.getMatrices().push();ctx.getMatrices().scale(layoutScale,layoutScale,1);mx=Math.round(mx/layoutScale);my=Math.round(my/layoutScale);
        ctx.fillGradient(0,0,Math.round(width/layoutScale),Math.round(height/layoutScale),0x6b080911,0x96080911);round(ctx,left-2,top-2,WIDTH+4,cardHeight+4,12,0xff373044);round(ctx,left,top,WIDTH,cardHeight,10,0xf51a1924);
        ctx.fill(left+20,top+46,left+WIDTH-20,top+47,0xff34303f);
        ctx.drawText(textRenderer,"LittlePet",left+20,top+17,WHITE,false);ctx.drawText(textRenderer,textRenderer.trimToWidth(pets.getPlayerName(),WIDTH-134),left+91,top+18,MUTED,false);ctx.drawText(textRenderer,"×",left+WIDTH-28,top+17,MUTED,false);
        ctx.drawText(textRenderer,"LittlePet",left+20,top+62,WHITE,false);toggle(ctx,left+WIDTH-68,top+56,pets.config.enabled);
        round(ctx,left+18,top+92,WIDTH-36,24,5,0xff2a2639);ctx.drawText(textRenderer,"Питомец",left+28,top+100,MUTED,false);ctx.drawText(textRenderer,textRenderer.trimToWidth(PetCatalog.label(pets.config.pet),WIDTH-178),left+130,top+100,WHITE,false);ctx.drawText(textRenderer,choosing?"−":"⌄",left+WIDTH-37,top+99,ACCENT,false);
        if(!choosing) {
            if(pets.config.pet.equals("Пельмень")) {ctx.drawText(textRenderer,"Отец и сын",left+22,top+138,WHITE,false);toggle(ctx,left+WIDTH-68,top+131,pets.config.family);}
            else if(pets.config.pet.equals("Кастом")) {
                ctx.drawText(textRenderer,"Лицо · изображение",left+22,top+125,MUTED,false);round(ctx,left+WIDTH-48,top+140,26,20,4,0xff54406e);ctx.drawText(textRenderer,"…",left+WIDTH-39,top+145,WHITE,false);
                ctx.drawText(textRenderer,"Скин · ник лицензии",left+22,top+177,MUTED,false);smallStatus(ctx,pets.skin.getStatus(),top+217,pets.skin.hasError());
                if(pets.face.hasError() || !pets.face.getValue().isBlank())smallStatus(ctx,pets.face.getStatus(),top+232,pets.face.hasError());
            } else ctx.drawText(textRenderer,"Следует за вами и отдыхает рядом",left+22,top+131,MUTED,false);
            ctx.drawText(textRenderer,pets.saved?pets.menuKeyLabel()+" · закрыть":"Не удалось сохранить настройки",left+22,top+cardHeight-18,pets.saved?0xff777388:0xffff9c9c,false);super.render(ctx,mx,my,delta);
        } else {
            ctx.getMatrices().push();ctx.getMatrices().translate(0,0,400);round(ctx,left+16,dropdownY()-2,WIDTH-32,popupHeight()+4,6,0xff40334f);round(ctx,left+18,dropdownY(),WIDTH-36,popupHeight(),5,0xff211d2b);
            search.render(ctx,mx,my,delta);
            for(int i=0;i<rows() && scroll+i<results.size();i++) {
                int index=scroll+i,y=listY()+i*24;PetCatalog.Option option=results.get(index);
                if(index==selected || hit(mx,my,left+18,y,WIDTH-36,24))ctx.fill(left+20,y,left+WIDTH-20,y+24,0xff443355);
                ctx.drawText(textRenderer,textRenderer.trimToWidth(option.name(),WIDTH-64),left+29,y+8,option.id().equals(pets.config.pet)?ACCENT:WHITE,false);
            }
            if(results.isEmpty())ctx.drawText(textRenderer,"Ничего не найдено",left+29,listY()+8,MUTED,false);
            String count=results.isEmpty()?"0":(scroll+1)+"–"+Math.min(results.size(),scroll+rows())+" / "+results.size();
            ctx.drawText(textRenderer,count,left+29,dropdownY()+popupHeight()-15,MUTED,false);
            if(results.size()>rows()) {int track=rows()*24,thumb=Math.max(12,track*rows()/results.size()),y=listY()+(track-thumb)*scroll/(results.size()-rows());ctx.fill(left+WIDTH-24,y,left+WIDTH-22,y+thumb,0xff8d6dac);}
            ctx.getMatrices().pop();
        }
        ctx.getMatrices().pop();
    }
    private void smallStatus(DrawContext ctx,String value,int y,boolean error) {ctx.drawText(textRenderer,textRenderer.trimToWidth(value,WIDTH-44),left+22,y,error?0xffff9c9c:MUTED,false);}
    private void toggle(DrawContext ctx,int x,int y,boolean enabled) {round(ctx,x,y,48,22,7,enabled?0xff7556a6:0xff35313e);round(ctx,x+(enabled?29:3),y+3,16,16,5,enabled?WHITE:MUTED);}
    private static void round(DrawContext ctx,int x,int y,int w,int h,int radius,int color) {for(int i=0;i<h;i++) {int edge=Math.min(i,h-1-i),inset=edge>=radius?0:radius-(int)Math.sqrt(radius*radius-(radius-edge-1)*(radius-edge-1));ctx.fill(x+inset,y+i,x+w-inset,y+i+1,color);}}
}
