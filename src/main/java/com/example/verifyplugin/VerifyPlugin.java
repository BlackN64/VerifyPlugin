package com.example.verifyplugin;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class VerifyPlugin extends JavaPlugin implements Listener {

    private HashMap<UUID, Boolean> verifiedPlayers = new HashMap<>();
    private FileConfiguration config;

    @Override
    public void onEnable() {
        config = getConfig();
        loadVerificationData(); // Load saved verification data from file
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("VerifyPlugin has been enabled!");
    }

    @Override
    public void onDisable() {
        saveVerificationData(); // Save verification data to file
        getLogger().info("VerifyPlugin has been disabled!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!config.contains("verification.codes." + uuid.toString())) {
            // First-time join, prompt to set code
            event.setJoinMessage(null);
            player.teleport(player.getWorld().getSpawnLocation());
            player.sendMessage(formatMessage(config.getString("verification.messages.prompt_set_code")));
            makePlayerInvulnerable(player);
        } else {
            // Subsequent join, prompt to verify
            event.setJoinMessage(null);
            player.teleport(player.getWorld().getSpawnLocation());
            player.sendMessage(formatMessage(config.getString("verification.messages.prompt_set_code")));
            makePlayerInvulnerable(player);
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            UUID uuid = player.getUniqueId();

            if (command.getName().equalsIgnoreCase("verify")) {
                if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
                    // Register a new code
                    if (args[1].length() == 4 && args[1].matches("\\d+")) {
                        config.set("verification.codes." + uuid.toString(), args[1]);
                        verifiedPlayers.put(uuid, true);
                        saveConfig();
                        player.sendMessage(formatMessage(config.getString("verification.messages.set_code_success").replace("%code%", args[1])));
                        makePlayerVulnerable(player);
                    } else {
                        player.sendMessage(formatMessage(config.getString("verification.messages.usage_verify_set")));
                    }
                } else if (args.length == 1) {
                    // Verify with existing code
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
                    sender.sendMessage(formatMessage(config.getString("verification.messages.usage_verify")));
                }
            } else {
                sender.sendMessage(formatMessage(config.getString("verification.messages.no_permission")));
            }
            return true;
        }

        return false;
    }

    private void loadVerificationData() {
        // Load verification codes directly from the config
    }

    private void saveVerificationData() {
        // Save verification codes directly to the config
        saveConfig();
    }

    private String formatMessage(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private void makePlayerInvulnerable(Player player) {
        player.setInvulnerable(true);
        player.setGameMode(GameMode.SPECTATOR);
    }

    private void makePlayerVulnerable(Player player) {
        player.setInvulnerable(false);
        player.setGameMode(GameMode.SURVIVAL);
    }
}
