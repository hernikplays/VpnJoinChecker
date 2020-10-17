package cz.hernikplays.vpnjoinchecker;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;

public final class VpnJoinChecker extends JavaPlugin implements Listener {
    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    @Override
    public void onEnable() {
        // Plugin startup logic
        System.out.println("Now starting");
        getServer().getPluginManager().registerEvents(this,this);
        getConfig().options().copyDefaults();
        saveDefaultConfig();
        getCommand("togglevpncheck").setExecutor(this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        System.out.println("Now disabling");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        System.out.println(player.getDisplayName()+" is joining");
        if(getConfig().getBoolean("Enabled")) {
            String playerIpwPort = player.getAddress().toString().replace("/","");
            String[] playerNoPort = playerIpwPort.split(":");

            System.out.println(playerNoPort[0]);

            HttpGet request = new HttpGet("http://v2.api.iphub.info/ip/"+playerNoPort[0]);
            String key = getConfig().getString("IPHub-Key");
            if(key.isEmpty()){
                System.out.println("API Key is missing");
            }
            else{
                request.addHeader("X-Key",key);

                try (CloseableHttpResponse response = httpClient.execute(request)) {

                    HttpEntity entity = response.getEntity();

                    if (entity != null) {
                        String result = EntityUtils.toString(entity);
                        Object resObj = new JSONParser().parse(result);
                        JSONObject jo = (JSONObject)resObj;
                        long blocked = (long)jo.get("block");
                        if(blocked == 1){
                            player.kickPlayer(ChatColor.translateAlternateColorCodes('&',getConfig().getString("KickMessage")));
                        }
                        else{
                            System.out.println("Player "+player.getDisplayName()+" is not using a VPN");
                        }
                    }

                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equals("togglevpncheck")){
            if(sender instanceof Player){
                Player player = (Player)sender;
                if(player.hasPermission("vpnjoinchecker.admin")){
                    boolean enabled = getConfig().getBoolean("Enabled");
                    System.out.println(enabled);
                    if(enabled){
                        getConfig().set("Enabled",false);
                        player.sendMessage(ChatColor.GOLD+"VPN checking "+ChatColor.RED+"DISABLED");
                    }
                    else{
                        getConfig().set("Enabled",true);
                        player.sendMessage(ChatColor.GOLD+"VPN checking "+ChatColor.GREEN+"ENABLED");
                    }
                }
                else{
                    player.sendMessage("You do not have permission to do this");
                }
            }
            else if (sender instanceof ConsoleCommandSender||sender instanceof RemoteConsoleCommandSender){
                getConfig().set("Enabled",!getConfig().getBoolean("Enabled"));
            }
        }
        return true;
    }
}
