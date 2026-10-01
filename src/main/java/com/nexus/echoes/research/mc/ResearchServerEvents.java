package com.nexus.echoes.research.mc;

import com.nexus.echoes.NexusEchoes;
import com.nexus.echoes.research.ResearchSources;
import com.nexus.echoes.research.ResearchTechnologies;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Forge-bus wiring for research (ADR-010): datapack reload listeners,
 * commands, login sync, point sources and technology gating.
 *
 * <p>Everything here is event-driven — there is no per-tick research scan.
 * All mutations are server-side; the client only receives snapshots.
 */
@Mod.EventBusSubscriber(modid = NexusEchoes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ResearchServerEvents {

    private static final ResourceLocation NEXUS_ORE_ID =
            new ResourceLocation(NexusEchoes.MOD_ID, "nexus_ore");
    private static final ResourceLocation GEAR_ID =
            new ResourceLocation(NexusEchoes.MOD_ID, "gear");

    private ResearchServerEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ResearchDefinitionLoader());
        event.addListener(new CodexLoader());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ResearchCommand.register(event.getDispatcher());
    }

    // ------------------------------------------------------- sync on login

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResearchManager.syncToClient(player);
        }
    }

    // ------------------------------------------------------- point sources

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()
                || event.isCanceled()
                || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(event.getState().getBlock());
        if (NEXUS_ORE_ID.equals(blockId)) {
            int before = ResearchManager.stateOf(player).points();
            ResearchManager.grantFromSource(player, ResearchSources.DISCOVER_NEXUS_ORE);
            ResearchManager.grantFromSource(player, ResearchSources.MINE_NEXUS_ORE);
            int gained = ResearchManager.stateOf(player).points() - before;
            if (gained > 0) {
                player.displayClientMessage(Component.translatable(
                        "message.nexus_echoes.research_points", gained), true);
            }
        }
    }

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return; // server-side only; client mirrors must not double-grant
        }
        ItemStack crafted = event.getCrafting();
        if (crafted.isEmpty()) {
            return;
        }
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(crafted.getItem());

        // source: first gear crafted
        if (GEAR_ID.equals(itemId)) {
            ResearchManager.grantFromSource(player, ResearchSources.CRAFT_GEAR);
        }

        // gate: void the result when the technology is locked
        if (!ResearchGatekeeper.checkCraft(player, itemId)) {
            crafted.setCount(0);
        }
    }

    // ------------------------------------------------------- placement gate

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()
                || event.isCanceled()
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(event.getPlacedBlock().getBlock());
        if (blockId != null && ResearchTechnologies.PLACE_GATE.containsKey(blockId)
                && !ResearchGatekeeper.checkPlace(player, blockId)) {
            event.setCanceled(true);
        }
    }
}
