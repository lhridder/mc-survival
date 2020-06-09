package nl.lucasridder.survival;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.org.apache.commons.io.output.ByteArrayOutputStream;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class Main extends JavaPlugin implements Listener, PluginMessageListener {

    boolean lock;
    String lockreason;
    HashMap<Player, String> PlayerBoolean = new HashMap<Player, String>();

    //send server
    public void sendServer(String server, Player player) {
        player.sendMessage(ChatColor.DARK_GRAY + "Je wordt nu doorverbonden naar: " + ChatColor.GOLD + server);
        //BUNGEE
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("Connect");
            out.writeUTF(server);
        } catch (IOException e) {
            e.printStackTrace();
        }
        player.sendPluginMessage(this, "BungeeCord", b.toByteArray());
        PlayerBoolean.put(player, server);
    }

    //update scoreboard
    public void updateScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard b = manager.getNewScoreboard();

        String top = this.getConfig().getString("scoreboard.top");
        Objective o = b.registerNewObjective("Gold", "", ChatColor.BOLD + "" + ChatColor.BLUE + top);
        o.setDisplaySlot(DisplaySlot.SIDEBAR);

        Score score5 = o.getScore(ChatColor.YELLOW + "");
        score5.setScore(5);

        Score score4 = o.getScore(ChatColor.YELLOW + "Welkom, " + ChatColor.GRAY + player.getName());
        score4.setScore(4);

        Score score3 = o.getScore(ChatColor.BOLD + "");
        score3.setScore(3);

        int spelers = getServer().getOnlinePlayers().size();
        Score score2 = o.getScore(ChatColor.GOLD + "Aantal spelers online: " + ChatColor.RED + spelers);
        score2.setScore(2);

        Score score1 = o.getScore("");
        score1.setScore(1);

        String name = this.getConfig().getString("scoreboard.name");
        Score score0 = o.getScore(ChatColor.BOLD + "" + ChatColor.GREEN + name);
        score0.setScore(0);

        player.setScoreboard(b);
    }

    //clear chat
    public void clearChat(Player player) {
        int x = 0;
        while (x < 20){
            player.sendMessage("");
            x = x + 1;
        }
    }

    //motd
    public void motd(Player player) {
        String name = player.getName();
        clearChat(player);
        player.sendMessage(ChatColor.DARK_GRAY + "Welkom, " + ChatColor.GOLD + name);
        player.sendMessage(ChatColor.BLUE + "Beschikbare servers: ");
        player.sendMessage(ChatColor.GOLD + "/survival" + ChatColor.DARK_GRAY + " en " + ChatColor.GOLD + "/minigames");
        player.sendMessage("");
        player.sendMessage("");
    }

    //Start-up
    @Override
    public void onEnable() {
        //Bungeecord channel
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);

        //register events
        Bukkit.getServer().getPluginManager().registerEvents(this, this);

        //config
        this.getConfig().options().copyDefaults(true);
        this.saveDefaultConfig();

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
        //lock
        if (!player.isOp()) {
            if (this.lock) {
                player.kickPlayer(ChatColor.GRAY + "De server is momenteel in lockdown vanwege:" + "\n" +
                        ChatColor.BLUE + this.lockreason + "\n" +
                        ChatColor.YELLOW + "Zie actuele status via: " + ChatColor.AQUA + "https://www.discord.gg/AzVCaQE");
                return;
            }
        }

        //join message
        if (player.hasPermission("survival.admin")) {
            e.setJoinMessage(ChatColor.DARK_GRAY + "[" + ChatColor.DARK_GREEN + "+" + ChatColor.DARK_GRAY + "] " + ChatColor.RED + name);
            } else if (!player.hasPlayedBefore()) {
            e.setJoinMessage(ChatColor.DARK_GRAY + "Welkom " + ChatColor.RESET + name);
            } else {
            e.setJoinMessage(ChatColor.DARK_GRAY + "[" + ChatColor.DARK_GREEN + "+" + ChatColor.DARK_GRAY + "] " + ChatColor.RED + name);
        }

        //scoreboard
        new BukkitRunnable() {
            public void run() {
                if (!player.isOnline()) this.cancel();
                else {
                    updateScoreboard(player);
                }

            }
        }.runTaskTimer(this, 20, 20);

        //motd message
        motd(player);
    }

    //Leave
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        //player info
        Player player = e.getPlayer();
        String name = player.getName();

        //bungee check
        String server = PlayerBoolean.get(player);
        if (server != null) {
            if (player.hasPermission("survival.admin")) {
                e.setQuitMessage(ChatColor.RED + name + ChatColor.DARK_RED + " -> " + ChatColor.GRAY + server);
            } else {
                e.setQuitMessage(ChatColor.WHITE + name + ChatColor.DARK_RED + " -> " + ChatColor.GRAY + server);
            }
            PlayerBoolean.remove(player);
        } else {
            //leave message
            if (player.hasPermission("survival.admin")) {
                e.setQuitMessage(ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "-" + ChatColor.DARK_GRAY + "] " + ChatColor.RED + name);
            } else {
                e.setQuitMessage(ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "-" + ChatColor.DARK_GRAY + "] " + ChatColor.RESET + name);
            }
        }
    }

    //commands
    @SuppressWarnings("NullableProblems")
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        //gamemode
        if(cmd.getName().equalsIgnoreCase("gamemode")) {
            if(sender.hasPermission("survival.admin")) {
                //check of sender speler is
                if (!(sender instanceof Player)) {
                    //zeg het
                    sender.sendMessage(ChatColor.RED + "Je bent geen speler");
                    return true;
                } else {
                    //te weinig argumenten
                    if (args.length == 0) {
                        sender.sendMessage(ChatColor.RED + "/gamemode (creative/survival/spectator/adventure)/(0/1/2/3) (speler)");
                        return true;
                    }

                    //teveel argumenten
                    if (args.length > 2) {
                        sender.sendMessage(ChatColor.RED + "/gamemode (creative/survival/spectator/adventure)/(0/1/2/3) (speler)");
                        return true;
                    }

                    //goede aantal argumenten
                    if (args.length == 1) {
                        //pak speler
                        Player player = (Player) sender;

                        if (args[0].equalsIgnoreCase("creative") | args[0].equalsIgnoreCase("1")) {
                            player.setGameMode(GameMode.CREATIVE);
                            sender.sendMessage(ChatColor.GREEN + "Gedaan.");
                            return true;
                        } else if (args[0].equalsIgnoreCase("survival") | args[0].equalsIgnoreCase("0")) {
                            player.setGameMode(GameMode.SURVIVAL);
                            sender.sendMessage(ChatColor.GREEN + "Gedaan.");
                            return true;
                        } else if (args[0].equalsIgnoreCase("spectator") | args[0].equalsIgnoreCase("3")) {
                            player.setGameMode(GameMode.SPECTATOR);
                            sender.sendMessage(ChatColor.GREEN + "Gedaan.");
                            return true;
                        } else if (args[0].equalsIgnoreCase("adventure") | args[0].equalsIgnoreCase("2")) {
                            player.setGameMode(GameMode.ADVENTURE);
                            sender.sendMessage(ChatColor.GREEN + "Gedaan.");
                            return true;
                        } else
                            sender.sendMessage(ChatColor.RED + "/gamemode (creative/survival/spectator/adventure)/(0/1/2/3) (speler)");

                    }

                    //andere speler
                    if (args.length == 2) {
                        //pak speler
                        Player target = Bukkit.getServer().getPlayer(args[1]);
                        if (target == null) {
                            sender.sendMessage(ChatColor.RED + "Doel is niet online");
                        } else {

                            if (args[0].equalsIgnoreCase("creative") | args[0].equalsIgnoreCase("1")) {
                                target.setGameMode(GameMode.CREATIVE);
                                target.sendMessage(ChatColor.GREEN + "Gamemode aangepast.");
                                sender.sendMessage(ChatColor.GREEN + "Gedaan.");
                                return true;
                            } else if (args[0].equalsIgnoreCase("survival") | args[0].equalsIgnoreCase("0")) {
                                target.setGameMode(GameMode.SURVIVAL);
                                target.sendMessage(ChatColor.GREEN + "Gamemode aangepast.");
                                sender.sendMessage(ChatColor.GREEN + "Gedaan.");
                                return true;
                            } else if (args[0].equalsIgnoreCase("spectator") | args[0].equalsIgnoreCase("3")) {
                                target.setGameMode(GameMode.SPECTATOR);
                                target.sendMessage(ChatColor.GREEN + "Gamemode aangepast.");
                                sender.sendMessage(ChatColor.GREEN + "Gedaan.");
                                return true;
                            } else if (args[0].equalsIgnoreCase("adventure") | args[0].equalsIgnoreCase("2")) {
                                target.setGameMode(GameMode.ADVENTURE);
                                target.sendMessage(ChatColor.GREEN + "Gamemode aangepast.");
                                sender.sendMessage(ChatColor.GREEN + "Gedaan.");
                                return true;
                            } else {
                                sender.sendMessage(ChatColor.RED + "/gamemode (creative/survival/spectator/adventure)/(0/1/2/3) (speler)");
                            }
                        }
                    }
                }
            } else {
                sender.sendMessage(ChatColor.DARK_RED + "Geen toegang tot dit commando");
            }
        }

        //setspawn command
        if(cmd.getName().equalsIgnoreCase("setspawn")) {
            if(!(sender instanceof Player)) {
                //zeg het
                sender.sendMessage(ChatColor.RED + "Je bent geen speler");
                return true;
            } else {
                Player player = (Player) sender;
                int x = player.getLocation().getBlockX();
                int y = player.getLocation().getBlockY();
                int z = player.getLocation().getBlockZ();
                float yaw = player.getLocation().getYaw();
                float pitch = player.getLocation().getPitch();
                this.getConfig().set("spawn.x", x);
                this.getConfig().set("spawn.y", y);
                this.getConfig().set("spawn.z", z);
                this.getConfig().set("spawn.yaw", yaw);
                this.getConfig().set("spawn.pitch", pitch);
                this.saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Spawn set");
            }
        }

        //minigames
        if(cmd.getName().equalsIgnoreCase("minigames")) {
            if(!(sender instanceof Player)) {
                //zeg het
                sender.sendMessage(ChatColor.RED + "Je bent geen speler");
                return true;
            } else {
                sendServer("minigames", (Player) sender);
            }
        }

        //lobby
        if(cmd.getName().equalsIgnoreCase("lobby")) {
            if(!(sender instanceof Player)) {
                //zeg het
                sender.sendMessage(ChatColor.RED + "Je bent geen speler");
                return true;
            } else {
                sendServer("lobby", (Player) sender);
            }
        }

        //fly
        if(cmd.getName().equalsIgnoreCase("fly")) {
            if (sender.hasPermission("survival.admin")) {
                if (!(sender instanceof Player)) {
                    //zeg het
                    sender.sendMessage(ChatColor.RED + "Je bent geen speler");
                    return true;
                } else {
                    //zelf
                    if (args.length == 0) {
                        Player player = (Player) sender;

                        if (player.isFlying()) {
                            player.setFlying(false);
                            sender.sendMessage(ChatColor.GREEN + "Vliegen uitgeschakeld.");
                        } else {
                            player.setFlying(true);
                            sender.sendMessage(ChatColor.GREEN + "Vliegen ingeschakeld.");
                        }
                        sender.sendMessage(ChatColor.GREEN + "Vliegen ingeschakeld.");
                    }
                    //andere speler
                    if (args.length == 1) {
                        Player target = Bukkit.getServer().getPlayer(args[0]);
                        if (target == null) {
                            sender.sendMessage(ChatColor.RED + "Doel is niet online");
                        } else {
                            if (target.isFlying()) {
                                target.setFlying(false);
                                sender.sendMessage(ChatColor.GREEN + "Vliegen uitgeschakeld voor " + ChatColor.GOLD + args[0] + ChatColor.GREEN + ".");
                            } else {
                                target.setFlying(true);
                                sender.sendMessage(ChatColor.GREEN + "Vliegen ingeschakeld voor " + ChatColor.GOLD + args[0] + ChatColor.GREEN + ".");
                            }
                        }
                    }
                    //teveel argumenten
                    if(args.length >= 1) { sender.sendMessage(ChatColor.RED + "/fly (speler)"); }

                }
            }
        } else {
            sender.sendMessage(ChatColor.DARK_RED + "Geen toegang tot dit commando");
        }

        //spawn
        if(cmd.getName().equalsIgnoreCase("spawn")) {
            Player player = (Player) sender;
            if (!(sender instanceof Player)) {
                //zeg het
                sender.sendMessage(ChatColor.RED + "Je bent geen speler");
                return true;
            }
            //spawn loc
            sender.sendMessage(ChatColor.GREEN + "Aan het teleporteren...");
            try {
                int x = this.getConfig().getInt("spawn.x");
                int y = this.getConfig().getInt("spawn.y");
                int z = this.getConfig().getInt("spawn.z");
                Location loc = new Location(player.getWorld(), x, y, z);
                player.teleport(loc);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }

        //lock
        if(cmd.getName().equalsIgnoreCase("lock")) {
            if(sender.hasPermission("survival.admin")) {
                if (!this.lock) {
                    lock = true;
                    //geen reden
                    if (args.length == 0) {
                        this.lockreason = "onbekend";
                        sender.sendMessage(ChatColor.GRAY + "Lockdown: " + ChatColor.GOLD + lock);
                    }
                    //meer args
                    if (args.length > 0) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 1; i < args.length; i++) {
                            sb.append(args[i]).append(" ");
                        }
                        this.lockreason = sb.toString().trim();
                        sender.sendMessage(ChatColor.GRAY + "Lockdown: " + ChatColor.GOLD + lock);
                        sender.sendMessage(ChatColor.GRAY + "Reden: " + ChatColor.GOLD + this.lockreason);
                    }
                } else {
                    this.lock = false;
                    sender.sendMessage(ChatColor.GREEN + "Lockdown opgeheven");
                }
            } else {
                sender.sendMessage(ChatColor.DARK_RED + "Geen toegang tot dit commando");
            }
        }

        //motd
        if(cmd.getName().equalsIgnoreCase("motd")) {
            if(!(sender instanceof Player)) {
                //zeg het
                sender.sendMessage(ChatColor.RED + "Je bent geen speler");
                return true;
            } else {
                motd((Player) sender);
            }
        }

        //stop
        if(cmd.getName().equalsIgnoreCase("stop")) {
            sender.sendMessage(ChatColor.GREEN + "Kicking all players...");
            for(Player players : this.getServer().getOnlinePlayers()) {
                if(!players.equals(sender)) {
                    players.kickPlayer(ChatColor.GRAY + "De server wordt momenteel herstart" + "\n" +
                            ChatColor.BLUE + "wacht even met opnieuw joinen" + "\n" +
                            ChatColor.YELLOW + "Zie actuele status via: " + ChatColor.AQUA + "https://www.discord.gg/AzVCaQE");
                }
                sender.sendMessage(ChatColor.GREEN + "Stopping server...");
                System.out.println("[HUB]" + ChatColor.DARK_RED + " stopping server...");
                Bukkit.shutdown();
            }

        }
        return true;
    }

    //Chat
    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        String name = e.getPlayer().getName();
        String message = e.getMessage();
        if(player.isOp()) {
            e.setFormat(ChatColor.GOLD + name + ChatColor.DARK_GRAY + " » " + ChatColor.RESET + message);
        } else {
            e.setFormat(ChatColor.GRAY + name + ChatColor.DARK_GRAY + " » " + ChatColor.RESET + message);
        }
    }

    //Listener bungee
    @SuppressWarnings("NullableProblems")
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) {
            System.out.println(Arrays.toString(message));
        }
        }

}
