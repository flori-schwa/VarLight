package me.shawlaf.varlight.spigot.adapters;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.commands.CommandUtils;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import lombok.Getter;
import me.shawlaf.varlight.spigot.event.CustomLuminanceUpdateEvent;
import me.shawlaf.varlight.spigot.event.LightUpdateCause;
import me.shawlaf.varlight.spigot.util.collections.EntityToObjectMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class WorldGuardAdapter implements Listener {

    private final WorldGuard worldGuard;

    public WorldGuardAdapter() {
        this.worldGuard = WorldGuard.getInstance();
    }

    public boolean hasBypass(Player player, org.bukkit.World world) {
        return worldGuard.getPlatform().getSessionManager().hasBypass(CachedPlayer.getWorldGuardPlayer(player), BukkitAdapter.adapt(world));
    }

    public boolean canModifyAt(Player player, org.bukkit.Location location) {
        return hasBypass(player, location.getWorld()) || canBuildInRegion(player, queryLocation(location));
    }

    @EventHandler
    private void onLightSourceEdit(CustomLuminanceUpdateEvent lightUpdateEvent) {
        if (lightUpdateEvent.getCause().getCause() != LightUpdateCause.Type.PLAYER) {
            return;
        }

        CommandSender cause = lightUpdateEvent.getCause().getPlayerCause();

        if (!(cause instanceof Player)) {
            return;
        }

        Player playerCause = ((Player) cause);

        try (CachedPlayer cachedPlayer = new CachedPlayer(playerCause)) {
            if (hasBypass(playerCause, lightUpdateEvent.getBlock().getWorld())) {
                return;
            }

            ApplicableRegionSet lightModifyAt = queryLocation(lightUpdateEvent.getBlock().getLocation());

            if (!canBuildInRegion(playerCause, lightModifyAt)) {
                lightUpdateEvent.setCancelled(true);

                String message = lightModifyAt.queryValue(cachedPlayer.getWorldGuardPlayer(), Flags.DENY_MESSAGE);
                message = worldGuard.getPlatform().getMatcher().replaceMacros(cachedPlayer.getWorldGuardPlayer(), message);
                message = CommandUtils.replaceColorMacros(message);

                cachedPlayer.getWorldGuardPlayer().printRaw(message);
            }
        }
    }

    private ApplicableRegionSet queryLocation(org.bukkit.Location location) {
        return worldGuard.getPlatform().getRegionContainer().createQuery().getApplicableRegions(BukkitAdapter.adapt(location));
    }

    private boolean canBuildInRegion(Player player, ApplicableRegionSet regionSet) {
        return regionSet.testState(CachedPlayer.getWorldGuardPlayer(player), Flags.BUILD);
    }

    private static class CachedPlayer implements AutoCloseable {

        private static EntityToObjectMap<LocalPlayer> PLAYER_CACHE = new EntityToObjectMap<>();

        public static LocalPlayer getWorldGuardPlayer(Player player) {
            if (PLAYER_CACHE.containsKey(player)) {
                return PLAYER_CACHE.get(player);
            }

            return WorldGuardPlugin.inst().wrapPlayer(player);
        }

        private final Player player;
        @Getter
        private final LocalPlayer worldGuardPlayer;
        private final boolean shallRemove;

        CachedPlayer(Player player) {
            this.player = player;

            if (!PLAYER_CACHE.containsKey(this.player)) {
                shallRemove = true;

                PLAYER_CACHE.put(this.player, getWorldGuardPlayer(this.player));
            } else {
                shallRemove = false;
            }

            this.worldGuardPlayer = getWorldGuardPlayer(this.player);
        }

        @Override
        public void close() {
            if (shallRemove) {
                PLAYER_CACHE.remove(this.player);
            }
        }
    }

}
