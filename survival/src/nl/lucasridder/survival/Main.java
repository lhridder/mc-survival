package nl.lucasridder.survival;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.org.apache.commons.io.output.ByteArrayOutputStream;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class Main extends JavaPlugin implements Listener, PluginMessageListener {

    //variabelen
    int all;
    HashMap<Player, String> PlayerBoolean = new HashMap<>();
    HashMap<Player, Boolean> Staff = new HashMap<>();
    HashMap<Player, Boolean> Invis = new HashMap<>();

    //check if block is in spawn
    public boolean checkSpawn(Location location) {
        double x = location.getBlock().getX();
        double z = location.getBlock().getZ();
        return x >= 320 && z >= -831 && x <= 356 && z <= -795;
    }

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

    //playercount
    public void playerCount() {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress("vps3.lucasridder.nl", 25565), 1000);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            out.write(0xFE);

            StringBuilder str = new StringBuilder();

            int b;
            while ((b = in.read()) != -1) {
                if (b > 16 && b != 255 && b != 23 && b != 24) {
                    str.append((char) b);
                }
            }

            String[] data = str.toString().split("§");
            this.all = Integer.parseInt(data[1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //inventory staff
    public void setStaffInventory(Player player) {
        if(Staff.containsKey(player)) {
            if (Invis.containsKey(player)) {
                //invis on state item
                ItemStack stack5 = new ItemStack(Material.GREEN_DYE);
                ItemMeta meta5 = stack5.getItemMeta();
                meta5.setDisplayName(ChatColor.GOLD + "Disable invis mode.");
                stack5.setItemMeta(meta5);
                player.getInventory().setItem(7, stack5);
            } else {
                //invis off state item
                ItemStack stack4 = new ItemStack(Material.GRAY_DYE);
                ItemMeta meta4 = stack4.getItemMeta();
                meta4.setDisplayName(ChatColor.GOLD + "Enable invis mode.");
                stack4.setItemMeta(meta4);
                player.getInventory().setItem(7, stack4);
            }
            //Leave staffmode item
            ItemStack stack6 = new ItemStack(Material.RED_BED);
            ItemMeta meta6 = stack6.getItemMeta();
            meta6.setDisplayName(ChatColor.GOLD + "Leave staffmode.");
            stack6.setItemMeta(meta6);
            player.getInventory().setItem(8, stack6);
            //finish
            player.updateInventory();
        } else {
            player.getInventory().clear();
        }
    }

    //update scoreboard
    public void updateScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard b = manager.getNewScoreboard();

        Objective o = b.registerNewObjective("Gold", "", ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "Survival");
        o.setDisplaySlot(DisplaySlot.SIDEBAR);


        UUID uuid = player.getUniqueId();
        int kills = getConfig().getInt("player." + uuid + ".kills");
        int deaths = getConfig().getInt("player." + uuid + ".deaths");;

        //staff 4 t/m 6
        //check if player has staff permission
        if(player.isOp()) {
            //Kills 9
            o.getScore(ChatColor.YELLOW + "Kills: " + ChatColor.RED + kills).setScore(9);

            //Deaths 8
            o.getScore(ChatColor.YELLOW + "Deaths: " + ChatColor.RED + deaths).setScore(8);

            //spacer 7
            o.getScore(ChatColor.RED + "").setScore(7);

            //if staff mode enabled
            if(Staff.containsKey(player)) {
                //staff on 6
                o.getScore(ChatColor.DARK_GREEN + "Staffmode: " + ChatColor.GREEN + "✔").setScore(6);
                //check invis
                if(Invis.containsKey(player)) {
                    //invis on 5
                    o.getScore(ChatColor.BLUE + "  Invisability: " + ChatColor.GREEN + "✔").setScore(5);
                } else {
                    //invis off 5
                    o.getScore(ChatColor.BLUE + "  Invisability: " + ChatColor.RED + "✘").setScore(5);
                }
            } else {
                //staff off 6
                o.getScore(ChatColor.DARK_GREEN + "Staffmode: " + ChatColor.RED + "✘").setScore(6);
            }

        } else {
            //Kills 6
            o.getScore(ChatColor.YELLOW + "Kills: " + ChatColor.RED + kills).setScore(6);

            //Deaths 5
            o.getScore(ChatColor.YELLOW + "Deaths: " + ChatColor.RED + deaths).setScore(5);
        }

        //spacer 4
        o.getScore(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "").setScore(4);

        //totaal spelers proxy 3
        o.getScore(ChatColor.YELLOW + "Totaal spelers: " + ChatColor.RED + this.all).setScore(3);

        //totaal spelers hub 2
        int spelers = getServer().getOnlinePlayers().size();
        int invis = Invis.size();
        int hub = spelers - invis;
        o.getScore(ChatColor.BLUE + "  Survival: " + ChatColor.RED + hub).setScore(2);


        //spacer 1
        o.getScore("").setScore(1);

        //server footer 0
        o.getScore(ChatColor.GREEN + "LucasRidder.nl").setScore(0);

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
        player.sendMessage("  " + ChatColor.DARK_GRAY + "Welkom, " + ChatColor.GOLD + name + ChatColor.DARK_GRAY + " op de survival server!");
        player.sendMessage("  " + ChatColor.BLUE + "Beschikbare andere servers: ");
        player.sendMessage("  " + ChatColor.GOLD + "/hub" + ChatColor.DARK_GRAY + ", " + ChatColor.GOLD + "/minigames" + ChatColor.DARK_GRAY + " en " + ChatColor.GOLD + "/kitpvp");
        player.sendMessage("");
        player.sendMessage("");
    }

    //invisOn method
    public void invisOn(Player player) {
        if(!Staff.containsKey(player)) {
            //make player staff
            Staff.put(player, true);
            staffOn(player);
        } else {
            for (Player players : Bukkit.getOnlinePlayers()) {
                //check the players if they are staff
                if(!Invis.containsKey(players)) {
                    players.hidePlayer(this, player);
                }
            }
            Invis.put(player, true);
            player.sendMessage(ChatColor.GOLD + "Your invisability was: " + ChatColor.GREEN + "enabled.");
            setStaffInventory(player);
        }
    }

    //invisOff method
    public void invisOff(Player player) {
        for (Player players : Bukkit.getOnlinePlayers()) {
            players.showPlayer(this, player);
        }
        Invis.remove(player);
        player.sendMessage(ChatColor.GOLD + "Your invisability was: " + ChatColor.RED + "disabled.");
        setStaffInventory(player);
        UUID uuid = player.getUniqueId();
        getConfig().set("player." + uuid + ".invis", false);
        saveConfig();
    }

    //staff on method
    public void staffOn(Player player) {

        player.sendMessage(ChatColor.GOLD + "Staff mode has been: " + ChatColor.GREEN + "enabled!");
        Staff.put(player, true);
        setStaffInventory(player);
        UUID uuid = player.getUniqueId();
        if(getConfig().getBoolean("player." + uuid + ".invis")) {
            invisOn(player);
        }
    }

    //staff off method
    public void staffOff(Player player) {
        if(Invis.containsKey(player)) {
            invisOff(player);
        }

        player.sendMessage(ChatColor.GOLD + "Staff mode has been: " + ChatColor.RED + "disabled!");
        Staff.remove(player);
        setStaffInventory(player);
        UUID uuid = player.getUniqueId();
        getConfig().set("player." + uuid + ".staff", false);
        saveConfig();
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

        //start counter
        new BukkitRunnable() {
            public void run() {
                if(getServer().getOnlinePlayers().size() != 0) {
                    playerCount();
                }

            }
        }.runTaskTimer(this, 20, 100);

        //enable complete
        System.out.println(ChatColor.GRAY + "Survival plugin by LucasRidder " + ChatColor.GREEN + "Enabled");
    }

    //Power-down
    @Override
    public void onDisable() {
        // shut down plugin
        this.saveConfig();
        //stop scoreboard
        for (Player onlinePlayers : Bukkit.getOnlinePlayers()) {
            onlinePlayers.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        }
        //disable complete
        System.out.println(ChatColor.GRAY + "Survival plugin by LucasRidder " + ChatColor.GREEN + "Disabled");
    }

    //Join
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        //player info
        Player player = e.getPlayer();
        String name = player.getName();

        //check config for player invis info
        if(!Invis.containsKey(player)) {
            //join message
            if (player.isOp()) {
                //Join message
                e.setJoinMessage(ChatColor.DARK_GRAY + "[" + ChatColor.DARK_GREEN + "+" + ChatColor.DARK_GRAY + "] " + ChatColor.RED + name);
            } else if (!player.hasPlayedBefore()) {
                //Join message
                e.setJoinMessage(ChatColor.DARK_GRAY + "Welkom " + ChatColor.RESET + name);
            } else {
                //Join message
                e.setJoinMessage(ChatColor.DARK_GRAY + "[" + ChatColor.DARK_GREEN + "+" + ChatColor.DARK_GRAY + "] " + ChatColor.YELLOW + name);
            }
            motd(player);
        } else {
            motd(player);
            e.setJoinMessage(null);
        }

        //scoreboard
        new BukkitRunnable() {
            public void run() {
                if (!player.isOnline()) this.cancel();
                else {
                    updateScoreboard(player);
                }

            }
        }.runTaskTimer(this, 20, 100);

        //check which inv is needed
        UUID uuid = player.getUniqueId();
        if(getConfig().getBoolean("player." + uuid + ".staff")) {
            staffOn(player);
        }

        //tablist
        if(player.isOp()) {
            player.setPlayerListName(ChatColor.RED + player.getName());
        } else {
            player.setPlayerListName(ChatColor.YELLOW + player.getName());
        }

        //get invis staff and nametag
        for(Player players : this.getServer().getOnlinePlayers()) {
            if(Invis.containsKey(players)) {
                //check the players if they are staff
                if(!Invis.containsKey(players)) {
                    players.hidePlayer(this, player);
                }
            }

        }

        //register player in config
        getConfig().set("player." + uuid + ".name", player.getName());
        saveConfig();

        //set attack speed
        Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_ATTACK_SPEED)).setBaseValue(100);
    }

    //Leave
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        //player info
        Player player = e.getPlayer();
        String name = player.getName();

        if(!Invis.containsKey(player)) {
            //bungee check
            String server = PlayerBoolean.get(player);
            if (server != null) {
                if (player.isOp()) {
                    e.setQuitMessage(ChatColor.RED + name + ChatColor.DARK_RED + " -> " + ChatColor.GRAY + server);
                } else {
                    e.setQuitMessage(ChatColor.WHITE + name + ChatColor.DARK_RED + " -> " + ChatColor.GRAY + server);
                }
                PlayerBoolean.remove(player);
            } else {
                //leave message
                if (player.isOp()) {
                    e.setQuitMessage(ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "-" + ChatColor.DARK_GRAY + "] " + ChatColor.RED + name);
                } else {
                    e.setQuitMessage(ChatColor.DARK_GRAY + "[" + ChatColor.DARK_RED + "-" + ChatColor.DARK_GRAY + "] " + ChatColor.RESET + name);
                }
            }
        } else {
            //cancel quit message
            e.setQuitMessage(null);
            UUID uuid = player.getUniqueId();
            getConfig().set("player." + uuid + ".invis", true);
            saveConfig();
            Invis.remove(player);
        }
        if(Staff.containsKey(player)) {
            UUID uuid = player.getUniqueId();
            getConfig().set("player." + uuid + ".staff", true);
            saveConfig();
            Staff.remove(player);
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
                    //te weinig argumenten
                    if (args.length == 0 | args.length > 2) {
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
                            return true;
                    }

                    //andere speler
                    if (args.length == 2) {
                        //pak speler
                        Player target = Bukkit.getServer().getPlayer(args[1]);
                        if (target == null) {
                            sender.sendMessage(ChatColor.RED + "Doel is niet online");
                            return true;
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
                                return true;
                            }
                        }
                    }
            } else {
                sender.sendMessage(ChatColor.DARK_RED + "Geen toegang tot dit commando");
                return true;
            }
        }

        //setspawn command
        if(cmd.getName().equalsIgnoreCase("setspawn")) {
            if(!(sender instanceof Player)) {
                //zeg het
                sender.sendMessage(ChatColor.RED + "Je bent geen speler");
                return true;
            } else {
                if(sender.isOp()) {
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
                } else {
                    sender.sendMessage(ChatColor.DARK_RED + "Geen toegang tot dit commando");
                }
                return true;
            }
        }

        //survival
        if(cmd.getName().equalsIgnoreCase("lobby")) {
            sendServer("lobby", (Player) sender);
            return true;
        }

        //minigames
        if(cmd.getName().equalsIgnoreCase("minigames")) {
            sendServer("minigames", (Player) sender);
            return true;
        }

        //kitpvp
        if(cmd.getName().equalsIgnoreCase("kitpvp")) {
            sendServer("kitpvp", (Player) sender);
            return true;
        }

        //spawn
        if(cmd.getName().equalsIgnoreCase("spawn")) {
            Player player = (Player) sender;
            if (sender != null) {
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
            } else {
                sender.sendMessage(ChatColor.RED + "Je bent geen speler");
            }
            return true;
        }

        //stop
        if(cmd.getName().equalsIgnoreCase("stop")) {
            sender.sendMessage(ChatColor.GREEN + "Kicking all players...");
            if(this.getServer().getOnlinePlayers().size() != 0) {
                for (Player players : this.getServer().getOnlinePlayers()) {
                    if (!players.equals(sender)) {
                        players.kickPlayer(ChatColor.GRAY + "De server wordt momenteel herstart" + "\n" +
                                ChatColor.BLUE + "wacht even met opnieuw joinen" + "\n" +
                                ChatColor.YELLOW + "Zie actuele status via: " + ChatColor.AQUA + "https://www.discord.gg/AzVCaQE");
                    }
                }
            }
            sender.sendMessage(ChatColor.GREEN + "Stopping server...");
            System.out.println("[HUB]" + ChatColor.DARK_RED + " stopping server...");
            Bukkit.shutdown();
            return true;
        }

        //vanish
        if(cmd.getName().equalsIgnoreCase("vanish")) {
            if (Invis.containsKey((Player) sender)) {
                invisOff((Player) sender);
            } else {
                invisOn((Player) sender);
            }
        }

        //staff
        if(cmd.getName().equalsIgnoreCase("staff")) {
            if (Staff.containsKey((Player) sender)) {
                staffOff((Player) sender);
            } else {
                staffOn((Player) sender);
            }
        }

        //set
        if(cmd.getName().equalsIgnoreCase("set")) {
                //te weinig argumenten
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "/set (speler) (path) (waarde)");
                    return true;
                }

                //teveel argumenten
                if (args.length > 3) {
                    sender.sendMessage(ChatColor.RED + "/set (speler) (path) (waarde)");
                    return true;
                }

            //goed
            Player target = Bukkit.getPlayer(args[0]);
            if(target == null) {
                sender.sendMessage(ChatColor.RED + "Doel is niet online");
                return true;
            }

            //haal info
            UUID uuid = target.getUniqueId();
            String path = args[1];
            String value = args[2];

            //finish
            getConfig().set("player." + uuid + "." + path, value);
            sender.sendMessage(ChatColor.GREEN + "Gedaan.");
        }

        return true;
    }

    //Commandblock
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent e) {
        String message = e.getMessage();
        Player player = e.getPlayer();
        if (!player.isOp()) {
            //help
            if (message.startsWith("/hub") | message.startsWith("/minigames") | message.startsWith("/lobby") | message.startsWith("/spawn") | message.startsWith("/kitpvp")) {
                e.setCancelled(false);
            } else if (message.startsWith("/help")) {
                player.sendMessage(ChatColor.DARK_GRAY + "Zie hier de beschikbare commando's: ");
                player.sendMessage(ChatColor.DARK_GRAY + " - " + ChatColor.AQUA + "/hub" + ChatColor.DARK_GRAY + " : " + ChatColor.GOLD + "Ga naar de hub server!");
                player.sendMessage(ChatColor.DARK_GRAY + " - " + ChatColor.AQUA + "/minigames" + ChatColor.DARK_GRAY + " : " + ChatColor.GOLD + "Ga naar de minigames server!");
                player.sendMessage(ChatColor.DARK_GRAY + " - " + ChatColor.AQUA + "/spawn" + ChatColor.DARK_GRAY + " : " + ChatColor.GOLD + "Teleporteer naar spawn!");
                e.setCancelled(true);
            } else {
                e.setCancelled(true);
            }
        }
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

    //break
    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Location location = e.getBlock().getLocation();
        Player player = e.getPlayer();
        if(checkSpawn(location)) {
            if(!player.isOp()) {
                e.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Hey!, je mag in de spawn niks veranderen!");
            }
        }
    }

    //Block place
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        Location location = e.getBlock().getLocation();
        Player player = e.getPlayer();
        if(checkSpawn(location)) {
            if(!player.isOp()) {
                e.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Hey!, je mag in de spawn niks veranderen!");
            }
        }
    }

    //No Damage spawn
    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        //cancel any damage in spawn
        if(checkSpawn(e.getEntity().getLocation())) e.setCancelled(true);
    }

    //dood
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {

        Player player = e.getEntity();
        String name = player.getName();
        //register death
        UUID uuidd = player.getUniqueId();
        int deaths = getConfig().getInt("player." + uuidd + ".deaths");
        int newdeaths = deaths + 1;
        getConfig().set("player." + uuidd + ".deaths", newdeaths);

        EntityDamageEvent.DamageCause cause = Objects.requireNonNull(player.getLastDamageCause()).getCause();
        if (cause.equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK)) {
            Player killer = Objects.requireNonNull(player.getKiller());
            String killername = killer.getName();
            e.setDeathMessage(ChatColor.GRAY + name + ChatColor.RESET + " is gehutst door " + ChatColor.GRAY + killername + ChatColor.RED + "!");

            //register kill
            UUID uuidk = killer.getUniqueId();
            int kills = getConfig().getInt("player." + uuidk + ".kills");
            int newkills = kills + 1;
            getConfig().set("player." + uuidk + ".kills", newkills);

        } else if (cause.equals(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)) {
            e.setDeathMessage(ChatColor.GRAY + name + ChatColor.RESET + " is opgeblazen" + ChatColor.RED + "!");
        } else if (cause.equals(EntityDamageEvent.DamageCause.FALL)) {
            e.setDeathMessage(ChatColor.GRAY + name + ChatColor.RESET + " is gevallen" + ChatColor.RED + "!");
        } else {
            e.setDeathMessage(ChatColor.GRAY + name + ChatColor.RESET + " is gestorven" + ChatColor.RED + "!");
        }
    }

    //Interact
    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();

        if(e.getAction() == Action.RIGHT_CLICK_BLOCK | e.getAction() == Action.RIGHT_CLICK_AIR) {
            //staff items
            if(player.isOp()) {
                //invis on state item
                ItemStack stack5 = new ItemStack(Material.GREEN_DYE);
                ItemMeta meta5 = stack5.getItemMeta();
                meta5.setDisplayName(ChatColor.GOLD + "Disable invis mode.");
                stack5.setItemMeta(meta5);
                //invis off state item
                ItemStack stack4 = new ItemStack(Material.GRAY_DYE);
                ItemMeta meta4 = stack4.getItemMeta();
                meta4.setDisplayName(ChatColor.GOLD + "Enable invis mode.");
                stack4.setItemMeta(meta4);
                //Leave staffmode item
                ItemStack stack6 = new ItemStack(Material.RED_BED);
                ItemMeta meta6 = stack6.getItemMeta();
                meta6.setDisplayName(ChatColor.GOLD + "Leave staffmode.");
                stack6.setItemMeta(meta6);

                if(player.getInventory().getItemInMainHand().equals(stack4)) { invisOn(player); return;}
                if(player.getInventory().getItemInMainHand().equals(stack5)) { invisOff(player); return;}
                if(player.getInventory().getItemInMainHand().equals(stack6)) { staffOff(player); return;}
            }
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
