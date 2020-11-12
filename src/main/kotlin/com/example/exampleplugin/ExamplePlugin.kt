package com.example.exampleplugin

import kr.entree.spigradle.annotations.PluginMain
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.CommandExecutor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.TabCompleteEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

private val Player.teamName: String
    get() {
        val team = scoreboard.getEntryTeam(name) ?: return name
        return "${team.color}" + team.prefix + name
    }

private val Player.teamNameWithOutColors: String
    get() {
        return ChatColor.stripColor(teamName) ?: name
    }

@PluginMain // important for plugin.yml generation
class ExamplePlugin : JavaPlugin(), Listener {

    private val vanishedPlayers = mutableListOf<String>()

    override fun onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this)
        getCommand("vanish")?.setExecutor(CommandExecutor { sender, command, label, args ->

            if (!sender.hasPermission("vanish")) {
                sender.sendMessage("Unknown command. Type \"/help\" for help.")
                return@CommandExecutor true
            }

            if (sender !is Player) {
                return@CommandExecutor true
            }

            if (sender.name in vanishedPlayers) {
                vanishedPlayers.remove(sender.name)
                Bukkit.broadcastMessage("§e${sender.teamNameWithOutColors} joined the game")
                showForAll(sender)
            } else {
                vanishedPlayers.add(sender.name)
                Bukkit.broadcastMessage("§e${sender.teamNameWithOutColors} left the game")
                hideForAll(sender)
            }

            return@CommandExecutor true
        })

        getCommand("invsee")?.setExecutor { sender, command, label, args ->
            if (sender !is Player) return@setExecutor true

            if (args.size == 1) {
                val player = Bukkit.getPlayer(args[0])
                if (player?.name in vanishedPlayers) return@setExecutor true
                player?.inventory?.let { sender.openInventory(it) }
            }

            return@setExecutor true
        }
    }

    @EventHandler
    fun on(event: PlayerCommandPreprocessEvent) {
        if (event.message.toLowerCase().trim() in arrayOf("/minecraft:list", "/list")) {
            event.isCancelled = true
            event.player.sendMessage("There are ${Bukkit.getOnlinePlayers().size - Bukkit.getOnlinePlayers().filter { it.name in vanishedPlayers }.size} of a max of ${Bukkit.getMaxPlayers()} players online: ${
                Bukkit.getOnlinePlayers().filter { it.name !in vanishedPlayers }.joinToString(separator = "§7,§r " ) { it.teamName }
            }")
        } else if (event.message.toLowerCase().trim() in arrayOf("/bukkit:plugins", "/plugins", "/pl")) {
            event.isCancelled = true
            event.player.sendMessage("Plugins (${Bukkit.getPluginManager().plugins.size - 1}): ${
                Bukkit.getPluginManager().plugins.filter { it !is ExamplePlugin }
                    .joinToString("§f, §r") { "§a${it.name}" }
            }")
        }
    }

    @EventHandler
    fun on(event: TabCompleteEvent) {
        event.completions.removeIf { it in vanishedPlayers }
    }

    @EventHandler
    fun on(event: PlayerJoinEvent) {
        event.joinMessage = null
        if (event.player.name in vanishedPlayers) {
            event.player.sendMessage("§eDu hast den Server im Vanish betreten!")
        } else {
            Bukkit.broadcastMessage("§e${event.player.teamNameWithOutColors} joined the game")
        }
    }

    @EventHandler
    fun on(event: PlayerQuitEvent) {
        event.quitMessage = null
        if (event.player.name !in vanishedPlayers)
            Bukkit.broadcastMessage("§e${event.player.teamNameWithOutColors} left the game")
    }

    private fun hideForAll(player: Player) {
        Bukkit.getOnlinePlayers().forEach {
            if (it != player) {
                it.hidePlayer(this, player)
            }
        }
    }

    private fun showForAll(player: Player) {
        Bukkit.getOnlinePlayers().forEach {
            if (it != player) {
                it.showPlayer(this, player)
            }
        }
    }

}
