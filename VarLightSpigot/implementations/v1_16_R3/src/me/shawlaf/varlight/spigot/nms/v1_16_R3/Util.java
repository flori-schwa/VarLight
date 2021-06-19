package me.shawlaf.varlight.spigot.nms.v1_16_R3;

import lombok.experimental.UtilityClass;
import me.shawlaf.varlight.spigot.util.NamespacedID;
import me.shawlaf.varlight.util.IntPosition;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.joor.Reflect;

import java.util.concurrent.CompletableFuture;

@UtilityClass
public class Util {

    // region Keys

    public MinecraftKey toMinecraftKey(NamespacedID self) {
        return new MinecraftKey(self.getNamespace(), self.getName());
    }

    public NamespacedID toNamespacedId(NamespacedKey self) {
        return new NamespacedID(self.getNamespace(), self.getKey());
    }

    public NamespacedID toNamespacedId(MinecraftKey self) {
        return new NamespacedID(self.getNamespace(), self.getKey());
    }

    // endregion

    public BlockPosition toBlockPosition(IntPosition self) {
        return self.convert(BlockPosition::new);
    }

    public WorldServer toNmsWorld(World bukkitWorld) {
        return ((CraftWorld) bukkitWorld).getHandle();
    }

    public CompletableFuture<Void> runLightEngineSync(World bukkitWorld, Runnable task) {
        return runLightEngineSync(toNmsWorld(bukkitWorld), task);
    }

    public CompletableFuture<Void> runLightEngineSync(WorldServer world, Runnable task) {
        return runLightEngineSync(((LightEngineThreaded) world.e()), task);
    }

    public CompletableFuture<Void> runLightEngineSync(LightEngineThreaded lightengine, Runnable task) {
        ThreadedMailbox<Runnable> mailbox = Reflect.on(lightengine).get("b");
        CompletableFuture<Void> future = new CompletableFuture<>();

        mailbox.a(() -> {
            try {
                task.run();

                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    // region Deobfuscation ((Mostly) Yarn Names)

    public LightEngine getLightProvider(net.minecraft.server.v1_16_R3.World self) {
        return self.e();
    }

    public LightEngineLayerEventListener getLightingView(LightEngine self, EnumSkyBlock lightType) {
        return self.a(lightType);
    }

    public void checkBlock(LightEngineBlock self, IntPosition position) {
        checkBlock(self, toBlockPosition(position));
    }

    public void checkBlock(LightEngineBlock self, BlockPosition position) {
        self.a(position);
    }

    public CompletableFuture<IChunkAccess> lightChunk(LightEngineThreaded self, IChunkAccess chunk, boolean excludeBlocks) {
        return self.a(chunk, excludeBlocks);
    }

    // endregion

}
