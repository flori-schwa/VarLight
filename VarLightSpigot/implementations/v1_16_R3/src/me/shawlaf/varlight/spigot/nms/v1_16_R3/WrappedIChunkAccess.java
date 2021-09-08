package me.shawlaf.varlight.spigot.nms.v1_16_R3;

import me.shawlaf.varlight.spigot.VarLightPlugin;
import me.shawlaf.varlight.spigot.exceptions.VarLightNotActiveException;
import me.shawlaf.varlight.spigot.persistence.ICustomLightStorage;
import me.shawlaf.varlight.util.pos.IntPosition;
import net.minecraft.server.v1_16_R3.*;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.LongSet;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.shorts.ShortList;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class WrappedIChunkAccess implements IChunkAccess {

    private VarLightPlugin plugin;
    private org.bukkit.World world;
    private IChunkAccess wrapped;
    @Override
    protected void finalize() throws Throwable {
        System.out.printf("Wrapped IChunkAccess for Chunk %s garbage-collected%n", wrapped.getPos().toString());
    }

    public WrappedIChunkAccess(VarLightPlugin plugin, org.bukkit.World world, IChunkAccess wrapped) {
        this.plugin = plugin;
        this.world = world;
        this.wrapped = wrapped;
    }

    public void unloaded() {
        plugin = null;
        world = null;
        wrapped = null;
    }

    @Override
    public int g(BlockPosition blockposition) {
        int vanilla = wrapped.getType(blockposition).f();

        try {
            ICustomLightStorage cls = plugin.getApi().unsafe().requireVarLightEnabled(world);

            return cls.getCustomLuminance(new IntPosition(blockposition.getX(), blockposition.getY(), blockposition.getZ()), vanilla);
        } catch (VarLightNotActiveException e) {
            return vanilla;
        }
    }

    @Override
    public Stream<BlockPosition> m() {
        return StreamSupport.stream(BlockPosition.b(getPos().d(), 0, getPos().e(), getPos().f(), 255, getPos().g()).spliterator(), false).filter(pos -> g(pos) != 0);
    }

    // region Behaviour unchanged

    @Nullable
    @Override
    public IBlockData setType(BlockPosition blockPosition, IBlockData iBlockData, boolean b) {
        return wrapped.setType(blockPosition, iBlockData, b);
    }

    @Override
    public void setTileEntity(BlockPosition blockPosition, TileEntity tileEntity) {
        wrapped.setTileEntity(blockPosition, tileEntity);
    }

    @Override
    public void a(Entity entity) {
        wrapped.a(entity);
    }

    @Override
    public Set<BlockPosition> c() {
        return wrapped.c();
    }

    @Override
    public ChunkSection[] getSections() {
        return wrapped.getSections();
    }

    @Override
    public Collection<Map.Entry<HeightMap.Type, HeightMap>> f() {
        return wrapped.f();
    }

    @Override
    public void a(HeightMap.Type type, long[] longs) {
        wrapped.a(type, longs);
    }

    @Override
    public HeightMap a(HeightMap.Type type) {
        return wrapped.a(type);
    }

    @Override
    public int getHighestBlock(HeightMap.Type type, int i, int i1) {
        return wrapped.getHighestBlock(type, i, i1);
    }

    @Override
    public ChunkCoordIntPair getPos() {
        return wrapped.getPos();
    }

    @Override
    public void setLastSaved(long l) {
        wrapped.setLastSaved(l);
    }

    @Override
    public Map<StructureGenerator<?>, StructureStart<?>> h() {
        return wrapped.h();
    }

    @Override
    public void a(Map<StructureGenerator<?>, StructureStart<?>> map) {
        wrapped.a(map);
    }

    @Nullable
    @Override
    public BiomeStorage getBiomeIndex() {
        return wrapped.getBiomeIndex();
    }

    @Override
    public void setNeedsSaving(boolean b) {
        wrapped.setNeedsSaving(b);
    }

    @Override
    public boolean isNeedsSaving() {
        return wrapped.isNeedsSaving();
    }

    @Override
    public ChunkStatus getChunkStatus() {
        return wrapped.getChunkStatus();
    }

    @Override
    public void removeTileEntity(BlockPosition blockPosition) {
        wrapped.removeTileEntity(blockPosition);
    }

    @Override
    public ShortList[] l() {
        return wrapped.l();
    }

    @Nullable
    @Override
    public NBTTagCompound i(BlockPosition blockPosition) {
        return wrapped.i(blockPosition);
    }

    @Nullable
    @Override
    public NBTTagCompound j(BlockPosition blockPosition) {
        return wrapped.j(blockPosition);
    }

    @Override
    public TickList<Block> n() {
        return wrapped.n();
    }

    @Override
    public TickList<FluidType> o() {
        return wrapped.o();
    }

    @Override
    public ChunkConverter p() {
        return wrapped.p();
    }

    @Override
    public void setInhabitedTime(long l) {
        wrapped.setInhabitedTime(l);
    }

    @Override
    public long getInhabitedTime() {
        return wrapped.getInhabitedTime();
    }

    @Override
    public boolean r() {
        return wrapped.r();
    }

    @Override
    public void b(boolean b) {
        wrapped.b(b);
    }

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPosition blockPosition) {
        return wrapped.getTileEntity(blockPosition);
    }

    @Override
    public IBlockData getType(BlockPosition blockPosition) {
        return wrapped.getType(blockPosition);
    }

    @Override
    public Fluid getFluid(BlockPosition blockPosition) {
        return wrapped.getFluid(blockPosition);
    }

    @Nullable
    @Override
    public StructureStart<?> a(StructureGenerator<?> structureGenerator) {
        return wrapped.a(structureGenerator);
    }

    @Override
    public void a(StructureGenerator<?> structureGenerator, StructureStart<?> structureStart) {
        wrapped.a(structureGenerator, structureStart);
    }

    @Override
    public LongSet b(StructureGenerator<?> structureGenerator) {
        return wrapped.b(structureGenerator);
    }

    @Override
    public void a(StructureGenerator<?> structureGenerator, long l) {
        wrapped.a(structureGenerator, l);
    }

    @Override
    public Map<StructureGenerator<?>, LongSet> v() {
        return wrapped.v();
    }

    @Override
    public void b(Map<StructureGenerator<?>, LongSet> map) {
        wrapped.b(map);
    }

    // endregion
}
