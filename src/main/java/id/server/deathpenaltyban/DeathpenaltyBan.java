
package id.server.deathpenaltyban;

import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Date;
import java.util.UUID;

public class DeathpenaltyBan extends JavaPlugin implements Listener {

    private static final long BAN_TIME = 15 * 60 * 1000L;
    private static final String BAN_MESSAGE =
            "kacung bet main ginian aja mati, silahkan tunggu 15 menit";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("DeathpenaltyBan aktif!");
    }

    private String key(UUID uuid) {
        return "bans." + uuid;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        long expiry = System.currentTimeMillis() + BAN_TIME;

        getConfig().set(key(uuid), expiry);
        saveConfig();

        Bukkit.getScheduler().runTask(this, () -> {
            Bukkit.getBanList(BanList.Type.NAME).addBan(
                    player.getName(),
                    BAN_MESSAGE,
                    new Date(expiry),
                    "DeathpenaltyBan"
            );
            player.kickPlayer(BAN_MESSAGE);
        });
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        long expiry = getConfig().getLong(key(uuid), 0L);

        if (expiry <= 0L) {
            return;
        }

        if (expiry <= System.currentTimeMillis()) {
            getConfig().set(key(uuid), null);
            saveConfig();
            return;
        }

        long seconds = (expiry - System.currentTimeMillis() + 999) / 1000;
        long minutes = seconds / 60;
        seconds %= 60;

        event.disallow(
                PlayerLoginEvent.Result.KICK_BANNED,
                BAN_MESSAGE + "\nSisa waktu: "
                        + minutes + " menit " + seconds + " detik"
        );
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!command.getName().equalsIgnoreCase("penalty")) {
            return false;
        }

        if (!sender.hasPermission("deathpenaltyban.admin")) {
            sender.sendMessage("§cKamu tidak punya izin.");
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage("§e/penalty check <player>");
            sender.sendMessage("§e/penalty clear <player>");
            return true;
        }

        String action = args[0].toLowerCase();
        Player online = Bukkit.getPlayerExact(args[1]);
        OfflinePlayer target = online != null
                ? online : Bukkit.getOfflinePlayer(args[1]);
        UUID uuid = target.getUniqueId();

        if (action.equals("check")) {
            long remaining = getConfig().getLong(key(uuid), 0L)
                    - System.currentTimeMillis();

            if (remaining <= 0L) {
                sender.sendMessage("§aPemain tidak memiliki ban aktif.");
            } else {
                long seconds = (remaining + 999) / 1000;
                long minutes = seconds / 60;
                seconds %= 60;
                sender.sendMessage("§eSisa ban " + args[1] + ": "
                        + minutes + " menit " + seconds + " detik.");
            }
            return true;
        }

        if (action.equals("clear")) {
            getConfig().set(key(uuid), null);
            saveConfig();

            String name = target.getName();
            if (name != null) {
                Bukkit.getBanList(BanList.Type.NAME).pardon(name);
            }

            sender.sendMessage("§aBan " + args[1] + " berhasil dihapus.");
            return true;
        }

        sender.sendMessage("§cGunakan check atau clear.");
        return true;
    }
}
