package me.shawlaf.varlight.spigot.adapters;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldEditAdapter {

    private final WorldEditPlugin worldEditPlugin;

    public WorldEditAdapter() {
        if (Bukkit.getPluginManager().getPlugin("WorldEdit") == null) {
            throw new RuntimeException("WorldEdit not installed");
        }

        this.worldEditPlugin = JavaPlugin.getPlugin(WorldEditPlugin.class);
    }

    public Location[] getSelection(Player player, World world) {
        LocalSession session = worldEditPlugin.getSession(player);
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);

        Region region;

        try {
            region = session.getSelection(weWorld);
        } catch (IncompleteRegionException e) {
            return null;
        }

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        Location a = new Location(player.getWorld(), min.x(), min.y(), min.z());
        Location b = new Location(player.getWorld(), max.x(), max.y(), max.z());

        return new Location[]{a, b};
    }
}
