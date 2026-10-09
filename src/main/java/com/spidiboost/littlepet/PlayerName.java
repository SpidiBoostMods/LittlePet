package com.spidiboost.littlepet;

import java.util.Objects;

public final class PlayerName {
    private String session,player,display="";
    public String update(String sessionName,String playerName) {
        if(!Objects.equals(session,sessionName))display=sessionName;
        if(playerName!=null && !Objects.equals(player,playerName))display=playerName;
        if(playerName==null)display=sessionName;
        session=sessionName;player=playerName;
        return display==null?"":display;
    }
}
