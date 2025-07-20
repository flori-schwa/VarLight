package me.shawlaf.varlight.spigot.command.old.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import me.shawlaf.command.result.CommandResult;
import me.shawlaf.varlight.spigot.command.old.VarLightCommand;
import me.shawlaf.varlight.spigot.command.old.VarLightSubCommand;
import me.shawlaf.varlight.spigot.messages.VarLightMessages;
import me.shawlaf.varlight.spigot.permissions.PermissionNode;
import me.shawlaf.varlight.spigot.permissions.tree.VarLightPermissionTree;
import me.shawlaf.varlight.spigot.persistence.ICustomLightStorage;
import me.shawlaf.varlight.util.Paginator;
import me.shawlaf.varlight.util.collections.CollectionUtil;
import me.shawlaf.varlight.util.collections.CountingIterator;
import me.shawlaf.varlight.util.pos.ChunkCoords;
import me.shawlaf.varlight.util.pos.IntPosition;
import me.shawlaf.varlight.util.pos.RegionCoords;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;

import static me.shawlaf.command.result.CommandResult.failure;
import static me.shawlaf.command.result.CommandResult.success;
import static me.shawlaf.varlight.spigot.command.old.VarLightCommand.FAILURE;
import static me.shawlaf.varlight.spigot.command.old.VarLightCommand.SUCCESS;


public class VarLightCommandDebug extends VarLightSubCommand {

    private static final int PAGE_SIZE = 10;

    private static final RequiredArgumentBuilder<CommandSender, Integer> ARG_REGION_X = integerArgument("regionX");
    private static final RequiredArgumentBuilder<CommandSender, Integer> ARG_REGION_Z = integerArgument("regionZ");

    private static final RequiredArgumentBuilder<CommandSender, Integer> ARG_CHUNK_X = integerArgument("chunkX");
    private static final RequiredArgumentBuilder<CommandSender, Integer> ARG_CHUNK_Z = integerArgument("chunkZ");

    public VarLightCommandDebug(VarLightCommand command) {
        super(command, "debug");
    }

    @Override
    public @Nullable PermissionNode getRequiredPermissionNode() {
        return VarLightPermissionTree.DEBUG;
    }

    @NotNull
    @Override
    public String getDescription() {
        return "List all custom Light sources in a region or chunk or get a debug stick.";
    }

    @NotNull
    @Override
    public LiteralArgumentBuilder<CommandSender> build(LiteralArgumentBuilder<CommandSender> literalArgumentBuilder) {

        suggestCoordinate(ARG_CHUNK_X, e -> e.getLocation().getBlockX() >> 4);
        suggestCoordinate(ARG_CHUNK_Z, e -> e.getLocation().getBlockZ() >> 4);

        suggestCoordinate(ARG_REGION_X, e -> (e.getLocation().getBlockX() >> 4) >> 5);
        suggestCoordinate(ARG_REGION_Z, e -> (e.getLocation().getBlockZ() >> 4) >> 5);

        literalArgumentBuilder.then(
                literalArgument("list")
                        .then(
                                literalArgument("region")
                                        .executes(this::regionImplicit)
                                        .then(
                                                ARG_REGION_X.then(ARG_REGION_Z.executes((c) -> regionExplicit(c, 1))
                                                        .then(
                                                                integerArgument("rpage", 1)
                                                                        .executes(
                                                                                c -> regionExplicit(c, c.getArgument("rpage", int.class))
                                                                        )
                                                        )
                                                )
                                        )
                        ).then(
                        literalArgument("chunk")
                                .executes(this::chunkImplicit)
                                .then(
                                        ARG_CHUNK_X.then(ARG_CHUNK_Z.executes(c -> chunkExplicit(c, 1))
                                                .then(
                                                        integerArgument("cpage", 1)
                                                                .executes(
                                                                        c -> chunkExplicit(c, c.getArgument("cpage", int.class))
                                                                )
                                                )
                                        )
                                )
                )
        );

        literalArgumentBuilder.then(
                literalArgument("stick").executes(context -> {
                    if (!(context.getSource() instanceof Player)) {
                        failure(this, context.getSource(), "You must be a player to use this command!");
                        return FAILURE;
                    }

                    ((Player) context.getSource()).getInventory().addItem(plugin.getNmsAdapter().makeVarLightDebugStick());
                    ;

                    return SUCCESS;
                })
        );

        return literalArgumentBuilder;
    }

    private int regionImplicit(CommandContext<CommandSender> context) {
        if (!(context.getSource() instanceof Player)) {
            failure(this, context.getSource(), "You must be a player to use this command!");
            return FAILURE;
        }

        Player player = (Player) context.getSource();

        int regionX = player.getLocation().getBlockX() >> 4 >> 5;
        int regionZ = player.getLocation().getBlockZ() >> 4 >> 5;

        listLightSourcesInRegion(player, regionX, regionZ, 1);

        return SUCCESS;
    }

    private int regionExplicit(CommandContext<CommandSender> context, int page) {
        if (!(context.getSource() instanceof Player)) {
            failure(this, context.getSource(), "You must be a player to use this command!");
            return FAILURE;
        }

        Player player = (Player) context.getSource();

        int regionX = context.getArgument(ARG_REGION_X.getName(), int.class);
        int regionZ = context.getArgument(ARG_REGION_Z.getName(), int.class);

        listLightSourcesInRegion(player, regionX, regionZ, page);

        return SUCCESS;
    }

    private int chunkImplicit(CommandContext<CommandSender> context) {
        if (!(context.getSource() instanceof Player)) {
            failure(this, context.getSource(), "You must be a player to use this command!");
            return FAILURE;
        }

        Player player = (Player) context.getSource();

        int chunkX = player.getLocation().getBlockX() >> 4;
        int chunkZ = player.getLocation().getBlockZ() >> 4;

        listLightSourcesInChunk(player, chunkX, chunkZ, 1);

        return SUCCESS;
    }

    private int chunkExplicit(CommandContext<CommandSender> context, int page) {
        if (!(context.getSource() instanceof Player)) {
            failure(this, context.getSource(), "You must be a player to use this command!");
            return FAILURE;
        }

        Player player = (Player) context.getSource();

        int chunkX = context.getArgument(ARG_CHUNK_X.getName(), int.class);
        int chunkZ = context.getArgument(ARG_CHUNK_Z.getName(), int.class);

        listLightSourcesInChunk(player, chunkX, chunkZ, page);

        return SUCCESS;
    }

    private void listLightSourcesInRegion(Player player, int regionX, int regionZ, int page) {

        ICustomLightStorage cls;

        if ((cls = plugin.getApi().unsafe().getLightStorage(player.getWorld())) == null) {
            success(this, player, VarLightMessages.varLightNotActiveInWorld(player.getWorld()));
            return;
        }

        RegionCoords regionCoords = new RegionCoords(regionX, regionZ);

        CountingIterator<IntPosition> all = CollectionUtil.createCountingIterator(cls.iterateAllLightSources(regionCoords.getRegionStart(), regionCoords.getRegionEnd()));

        try {
            Iterator<IntPosition> pageList = Paginator.paginateEntriesIterator(all, PAGE_SIZE, page);

            int totalCount = all.countToEnd();

            int pages = Paginator.getAmountPages(totalCount, PAGE_SIZE);
            page = Math.min(page, pages);

            player.sendMessage(String.format("Light sources in region (%d | %d): %d (Showing Page %d / %d)", regionX, regionZ, totalCount, page, pages));
            listInternal(player, cls, pageList);
        } catch (IndexOutOfBoundsException e) {
            CommandResult.failure(this, player, String.format("The specified page %d (out of %d) is out of bounds!", page, Paginator.getAmountPages(all.countToEnd(), PAGE_SIZE)));
        }
    }

    private void listLightSourcesInChunk(Player player, int chunkX, int chunkZ, int page) {
        ICustomLightStorage cls;

        if ((cls = plugin.getApi().unsafe().getLightStorage(player.getWorld())) == null) {
            success(this, player, VarLightMessages.varLightNotActiveInWorld(player.getWorld()));
            return;
        }

        ChunkCoords chunkCoords = new ChunkCoords(chunkX, chunkZ);

        CountingIterator<IntPosition> all = CollectionUtil.createCountingIterator(cls.iterateAllLightSources(chunkCoords.getChunkStart(), chunkCoords.getChunkEnd()));

        try {
            Iterator<IntPosition> pageList = Paginator.paginateEntriesIterator(all, PAGE_SIZE, page);

            int totalCount = all.countToEnd();

            int pages = Paginator.getAmountPages(totalCount, PAGE_SIZE);
            page = Math.min(page, pages);

            player.sendMessage(String.format("Light sources in chunk (%d | %d): %d (Showing Page %d / %d)", chunkX, chunkZ, totalCount, page, pages));
            listInternal(player, cls, pageList);
        } catch (IndexOutOfBoundsException e) {
            CommandResult.failure(this, player, String.format("The specified page %d (out of %d) is out of bounds!", page, Paginator.getAmountPages(all.countToEnd(), PAGE_SIZE)));
        }
    }

    @SuppressWarnings("ConstantConditions")
    // All Methods that call this Method already assert that the WorldLightSourceManager exists!
    private void listInternal(Player player, ICustomLightStorage manager, Iterator<IntPosition> iterator) {
        while (iterator.hasNext()) {
            IntPosition lightSource = iterator.next();

            TextComponent textComponent = new TextComponent(String.format(
                    "%s = %d (%s)",
                    lightSource.toShortString(),
                    manager.getCustomLuminance(lightSource),
                    plugin.getNmsAdapter().getKey(player.getWorld().getBlockAt(lightSource.x, lightSource.y, lightSource.z).getType()))
            );

            textComponent.setColor(ChatColor.GREEN);

            textComponent.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    String.format("/tp %d %d %d", lightSource.x, lightSource.y, lightSource.z)
            ));

            textComponent.setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new BaseComponent[]{
                            new TextComponent("Click to teleport")
                    }
            ));

            player.spigot().sendMessage(textComponent);
        }
    }
}
