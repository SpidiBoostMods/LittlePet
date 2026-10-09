import com.spidiboost.littlepet.PlayerName;
import com.spidiboost.littlepet.PetConfig;
import com.google.gson.Gson;
import java.nio.file.*;

public final class PlayerNameProbe {
    private static void require(boolean valid,String detail) {if(!valid)throw new AssertionError(detail);}
    public static void main(String[] args) throws Exception {
        PlayerName name=new PlayerName();
        require(name.update("LaunchNick",null).equals("LaunchNick"),"No-world session");
        require(name.update("LaunchNick","ServerNick").equals("ServerNick"),"Player identity");
        require(name.update("LaunchNick","ChangedPlayer").equals("ChangedPlayer"),"Live profile change");
        require(name.update("ChangedSession","ChangedPlayer").equals("ChangedSession"),"Live session change");
        require(name.update("ChangedSession","ChangedPlayer").equals("ChangedSession"),"Stable override");
        require(name.update("ChangedSession","ChangedBoth").equals("ChangedBoth"),"Profile changed again");
        require(name.update("ChangedSession",null).equals("ChangedSession"),"Disconnect fallback");
        require(name.update(null,null).isEmpty(),"Null fallback");
        for(int i=0;i<4096;i++)require(name.update("S"+i,"P"+i).equals("P"+i),"Repeated changes "+i);
        Gson gson=new Gson();PetConfig migrated=gson.fromJson("{\"enabled\":true,\"pet\":\"Крокодил\",\"chase\":false}",PetConfig.class);
        String json=gson.toJson(migrated);
        require(!json.contains("chase") && migrated.enabled && migrated.pet.equals("Крокодил"),"Removed setting migration");
        Files.writeString(Path.of(args[0]).resolve("player-name.txt"),"PASS: 4096 live nickname changes, session/profile priority, disconnect, legacy setting removed\n");
        System.out.println("PASS: live nickname + old configuration migration");
    }
}
