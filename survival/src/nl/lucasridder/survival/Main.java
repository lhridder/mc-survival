package nl.lucasridder.survival;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    //Start-up
    @Override
    public void onEnable() {


        //enable complete
        System.out.println(ChatColor.GRAY + "Survival plugin by LucasRidder " + ChatColor.GREEN + "Enabled");
    }

    //Power-down
    @Override
    public void onDisable() {


        //disable complete
        System.out.println(ChatColor.GRAY + "Survival plugin by LucasRidder " + ChatColor.GREEN + "Disabled");
    }

    //Join
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        //player info
        Player player = e.getPlayer();
        String name = player.getName();


    }

    //Leave
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e ) {
        //player info
        Player player = e.getPlayer();
        String name = player.getName();

        //join message
        if(player.hasPermission("survival.admin")) {
            e.setQuitMessage(ChatColor.DARK_GRAY + "[" + ChatColor.DARK_GREEN + "+" + ChatColor.DARK_GRAY + "] " + ChatColor.RED + name);
        } else {

        }

    }
}
