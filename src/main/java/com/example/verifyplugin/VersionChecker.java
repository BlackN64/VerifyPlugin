package com.example.verifyplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class VersionChecker {

    private static final String VERSION_URL = "https://www.spigotmc.org/threads/verifyplugin.655732/";
    private Plugin plugin;
    private String latestVersion;

    public VersionChecker(Plugin plugin) {
        this.plugin = plugin;
    }

    public void checkVersion() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL(VERSION_URL).openConnection();
                    connection.setRequestMethod("GET");
                    connection.setReadTimeout(5000);
                    connection.setConnectTimeout(5000);

                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }

                    in.close();
                    connection.disconnect();

                    // Extract version from content
                    // Assuming the version is mentioned in the format: "Current Version: x.y.z"
                    String versionLine = content.toString().split("Current Version:")[1].split("<")[0].trim();
                    latestVersion = versionLine;

                    String currentVersion = plugin.getDescription().getVersion();
                    if (!currentVersion.equalsIgnoreCase(latestVersion)) {
                        notifyPlayers();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 432000L); // Check every 6 hours (432000 ticks)
    }

    private void notifyPlayers() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                        "&aA new version of VerifyPlugin is available: &e" + latestVersion + " &a. Please visit " +
                                VERSION_URL + " to download the latest version."));
            }
        }.runTask(plugin);
    }
}
