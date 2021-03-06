package com.feed_the_beast.ftbu.handlers;

import com.feed_the_beast.ftbl.FTBLibStats;
import com.feed_the_beast.ftbl.api.FTBLibAPI;
import com.feed_the_beast.ftbl.api.IForgePlayer;
import com.feed_the_beast.ftbl.util.FTBLib;
import com.feed_the_beast.ftbu.FTBU;
import com.feed_the_beast.ftbu.FTBUFinals;
import com.feed_the_beast.ftbu.FTBUPermissions;
import com.feed_the_beast.ftbu.api.IClaimedChunk;
import com.feed_the_beast.ftbu.config.FTBUConfigWorld;
import com.feed_the_beast.ftbu.world.chunks.ChunkloaderType;
import com.feed_the_beast.ftbu.world.data.FTBUWorldDataMP;
import com.google.common.collect.MapMaker;
import com.latmod.lib.util.LMDimUtils;
import com.latmod.lib.util.LMStringUtils;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public enum FTBUChunkEventHandler implements ForgeChunkManager.LoadingCallback, ForgeChunkManager.OrderedLoadingCallback
{
    INSTANCE;

    private static final String PLAYER_ID_TAG = "PID";
    private final Map<World, Map<UUID, ForgeChunkManager.Ticket>> table = new MapMaker().weakKeys().makeMap();

    public void init()
    {
        if(!ForgeChunkManager.getConfig().hasCategory(FTBUFinals.MOD_ID))
        {
            ForgeChunkManager.getConfig().get(FTBUFinals.MOD_ID, "maximumTicketCount", 100).setMinValue(0);
            ForgeChunkManager.getConfig().get(FTBUFinals.MOD_ID, "maximumChunksPerTicket", 1000000).setMinValue(0);
            ForgeChunkManager.getConfig().save();
        }

        ForgeChunkManager.setForcedChunkLoadingCallback(FTBU.inst, this);
    }

    private ForgeChunkManager.Ticket request(World w, IForgePlayer player)
    {
        if(w == null || player == null)
        {
            return null;
        }

        UUID playerID = player.getProfile().getId();

        Map<UUID, ForgeChunkManager.Ticket> map = table.get(w);
        ForgeChunkManager.Ticket t = (map == null) ? null : map.get(playerID);

        if(t == null)
        {
            t = ForgeChunkManager.requestTicket(FTBU.inst, w, ForgeChunkManager.Type.NORMAL);
            if(t == null)
            {
                return null;
            }
            else
            {
                t.getModData().setString(PLAYER_ID_TAG, LMStringUtils.fromUUID(playerID));

                if(map == null)
                {
                    map = new HashMap<>();
                    table.put(w, map);
                }

                map.put(playerID, t);
            }
        }

        return t;
    }

    @Override
    public List<ForgeChunkManager.Ticket> ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world, int maxTicketCount)
    {
        table.remove(world);
        List<ForgeChunkManager.Ticket> tickets1 = new ArrayList<>();
        if(tickets.isEmpty() || !FTBUConfigWorld.chunk_loading.getAsBoolean())
        {
            return tickets1;
        }
        Map<UUID, ForgeChunkManager.Ticket> map = new HashMap<>();

        for(ForgeChunkManager.Ticket t : tickets)
        {
            if(t.getModData().getTagId(PLAYER_ID_TAG) == Constants.NBT.TAG_STRING)
            {
                UUID playerID = LMStringUtils.fromString(t.getModData().getString(PLAYER_ID_TAG));

                if(playerID != null)
                {
                    map.put(playerID, t);
                    tickets1.add(t);
                }
            }
        }

        table.put(world, map);
        return tickets1;
    }

    @Override
    public void ticketsLoaded(List<ForgeChunkManager.Ticket> tickets, World world)
    {
        for(ForgeChunkManager.Ticket t : tickets)
        {
            UUID playerID = LMStringUtils.fromString(t.getModData().getString(PLAYER_ID_TAG));

            if(playerID != null)
            {
                Collection<IClaimedChunk> chunks = FTBUWorldDataMP.chunks.getChunks(playerID);

                if(!chunks.isEmpty())
                {
                    int dim = world.provider.getDimension();

                    for(IClaimedChunk c : chunks)
                    {
                        if(c.isLoaded() && c.getPos().dim == dim)
                        {
                            ForgeChunkManager.forceChunk(t, c.getPos().getChunkPos());
                        }
                    }
                }
            }
        }

        // force chunks //
        markDirty(world);
    }

    public void markDirty(World w)
    {
        if(FTBLibAPI.get().getWorld() == null || FTBLib.getServerWorld() == null)
        {
            return;
        }
        if(w != null)
        {
            markDirty0(w);
        }

        if(!table.isEmpty())
        {
            World[] worlds = table.keySet().toArray(new World[table.size()]);
            for(World w1 : worlds)
            {
                markDirty0(w1);
            }
        }
    }

    private void markDirty0(World w)
    {
        /*int total = 0;
        int totalLoaded = 0;
		int markedLoaded = 0;
		int loaded = 0;
		int unloaded = 0;*/

        int dim = w.provider.getDimension();

        for(IClaimedChunk c : FTBUWorldDataMP.chunks.getAllChunks())
        {
            if(c.getPos().dim == dim)
            {
                //total++;

                boolean isLoaded = c.isLoaded();

                if(isLoaded)
                {
                    if(c.getOwner() == null)
                    {
                        isLoaded = false;
                    }
                    else
                    {
                        ChunkloaderType type = FTBUPermissions.CHUNKLOADER_TYPE.get(c.getOwner().getProfile());

                        if(type == ChunkloaderType.DISABLED)
                        {
                            isLoaded = false;
                        }
                        else if(type == ChunkloaderType.ONLINE)
                        {
                            isLoaded = c.getOwner().isOnline();
                        }
                        else if(type == ChunkloaderType.OFFLINE)
                        {
                            if(!c.getOwner().isOnline())
                            {
                                double max = FTBUPermissions.CHUNKLOADER_OFFLINE_TIMER.get(c.getOwner().getProfile());

                                if(max > 0D && FTBLibStats.getLastSeenDeltaInHours(c.getOwner().stats(), false) > max)
                                {
                                    isLoaded = false;

                                    if(c.isForced())
                                    {
                                        FTBU.logger.info("Unloading " + c.getOwner().getProfile().getName() + " chunks for being offline for too long");
                                    }
                                }
                            }
                        }
                    }
                }

                //if(isLoaded) totalLoaded++;
                //if(c.isLoaded()) markedLoaded++;

                if(c.isForced() != isLoaded)
                {
                    ForgeChunkManager.Ticket ticket = request(LMDimUtils.getWorld(c.getPos().dim), c.getOwner());

                    if(ticket != null)
                    {
                        if(isLoaded)
                        {
                            ForgeChunkManager.forceChunk(ticket, c.getPos().getChunkPos());
                            //loaded++;
                        }
                        else
                        {
                            ForgeChunkManager.unforceChunk(ticket, c.getPos().getChunkPos());
                            //unloaded++;
                        }

                        c.setForced(isLoaded);
                    }
                }
            }
        }

        //FTBLib.dev_logger.info("Total: " + total + ", Loaded: " + totalLoaded + "/" + markedLoaded + ", DLoaded: " + loaded + ", DUnloaded: " + unloaded);
    }

    /*
    private void releaseTicket(ForgeChunkManager.Ticket t)
    {
        if(t.getModData().hasKey(PLAYER_ID_TAG))
        {
            Map<UUID, ForgeChunkManager.Ticket> map = table.get(t.world);

            if(map != null)
            {
                map.remove(LMUtils.fromString(t.getModData().getString(PLAYER_ID_TAG)));

                if(map.isEmpty())
                {
                    table.remove(t.world);
                }
            }
        }

        ForgeChunkManager.releaseTicket(t);
    }
    */

    public void clear()
    {
        table.clear();
    }
}