package com.example.verifyplugin;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public class VerifyPlugin extends JavaPlugin implements Listener {

    private HashMap<UUID, Boolean> verifiedPlayers = new HashMap<>();
    private HashMap<UUID, Location> lastLocations = new HashMap<>();
    private FileConfiguration config;
    private VersionChecker versionChecker;

    @Override
    public void onEnable() {
        config = getConfig();
        versionChecker = new VersionChecker(this);
        versionChecker.checkVersion();
        loadVerificationData();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("VerifyPlugin has been enabled!");
    }

    @Override
    public void onDisable() {
        saveVerificationData();
        getLogger().info("VerifyPlugin has been disabled!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!config.contains("verification.codes." + uuid.toString())) {
            player.sendMessage(formatMessage(config.getString("verification.messages.prompt_set_code")));
            makePlayerInvulnerable(player);
            startPromptTask(player, true);
        } else {
            player.sendMessage(formatMessage(config.getString("verification.messages.prompt_verify_code")));
            makePlayerInvulnerable(player);
            startPromptTask(player, false);
            if (lastLocations.containsKey(uuid)) {
                player.teleport(lastLocations.get(uuid));
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!verifiedPlayers.containsKey(uuid)) {
            event.setCancelled(true);
            player.sendMessage(formatMessage(config.getString("verification.messages.cannot_move")));
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (!verifiedPlayers.containsKey(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        lastLocations.put(uuid, player.getLocation());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            UUID uuid = player.getUniqueId();

            if (command.getName().equalsIgnoreCase("verify")) {
                if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
                    if (!config.contains("verification.codes." + uuid.toString())) {
                        if (args[1].length() == 4 && args[1].matches("\\d+")) {
                            config.set("verification.codes." + uuid.toString(), args[1]);
                            verifiedPlayers.put(uuid, true);
                            saveConfig();
                            player.sendMessage(formatMessage(config.getString("verification.messages.set_code_success").replace("%code%", args[1])));
                            makePlayerVulnerable(player);
                        } else {
                            player.sendMessage(formatMessage(config.getString("verification.messages.usage_verify_set")));
                        }
                    } else {
                        player.sendMessage(formatMessage(config.getString("verification.messages.already_set_code")));
                    }
                } else if (args.length == 1) {
                    if (config.getString("verification.codes." + uuid.toString()).equals(args[0])) {
                        verifiedPlayers.put(uuid, true);
                        player.sendMessage(formatMessage(config.getString("verification.messages.verify_success")));
                        makePlayerVulnerable(player);
                    } else {
                        player.sendMessage(formatMessage(config.getString("verification.messages.incorrect_code")));
                    }
                } else {
                    player.sendMessage(formatMessage(config.getString("verification.messages.usage_verify")));
                }
                return true;
            }
        } else {
            sender.sendMessage(formatMessage(config.getString("verification.messages.only_players")));
        }

        if (command.getName().equalsIgnoreCase("getcode")) {
            if (sender.isOp()) {
                if (args.length == 1) {
                    Player target = getServer().getPlayer(args[0]);
                    if (target != null && config.contains("verification.codes." + target.getUniqueId().toString())) {
                        sender.sendMessage(formatMessage(config.getString("verification.messages.get_code_success")
                                .replace("%player%", target.getName())
                                .replace("%code%", config.getString("verification.codes." + target.getUniqueId().toString()))));
                    } else {
                        sender.sendMessage(formatMessage(config.getString("verification.messages.player_not_found")));
                    }
                } else {
                    sender.sendMessage(formatMessage(config.getString("verification.messages.usage_getcode")));
                }
            } else {
                sender.sendMessage(formatMessage(config.getString("verification.messages.no_permission")));
            }
            return true;
        }

        return false;
    }

    private void loadVerificationData() {
        // Load verification codes and last locations directly from the config
    }

    private void saveVerificationData() {
        // Save verification codes and last locations directly to the config
        saveConfig();
    }

    private String formatMessage(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private void makePlayerInvulnerable(Player player) {
        player.setInvulnerable(true);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setSprinting(false);
    }

    private void makePlayerVulnerable(Player player) {
        player.setInvulnerable(false);
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setSprinting(true);
        player.setWalkSpeed(0.2F);
    }

    private void startPromptTask(final Player player, final boolean isFirstTime) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!verifiedPlayers.containsKey(player.getUniqueId())) {
                    if (isFirstTime) {
                        player.sendMessage(formatMessage(config.getString("verification.messages.prompt_set_code")));
                    } else {
                        player.sendMessage(formatMessage(config.getString("verification.messages.prompt_verify_code")));
                    }
                } else {
                    cancel();
                }
            }
        }.runTaskTimer(this, 0, 100); // Send the message every 5 seconds (100 ticks)
    }
}
