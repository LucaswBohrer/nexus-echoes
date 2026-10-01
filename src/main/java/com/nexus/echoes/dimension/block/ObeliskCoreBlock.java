package com.nexus.echoes.dimension.block;

import com.nexus.echoes.dimension.discovery.DiscoveryIds;
import com.nexus.echoes.dimension.mc.HollowDiscoveryData;
import com.nexus.echoes.dimension.mc.HollowTravel;
import com.nexus.echoes.research.ResearchSources;
import com.nexus.echoes.research.mc.ResearchManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Hollow's return landmark (ADR-011): a pre-generated obelisk whose core
 * carries a recorded return link. Right-click returns the player to the
 * Overworld spire they arrived from — the single return mechanic.
 *
 * <p>First use grants the {@code obelisk} discovery and research points
 * through the existing source abstraction.
 */
public class ObeliskCoreBlock extends Block {

    public ObeliskCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }
        boolean discovered = HollowDiscoveryData.get(serverLevel.getServer())
                .grant(serverPlayer.getUUID(), DiscoveryIds.OBELISK);
        if (discovered) {
            ResearchManager.grantFromSource(serverPlayer, ResearchSources.DISCOVER_OBELISK);
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.nexus_echoes.obelisk_discovery"), false);
        }
        if (!HollowTravel.travelBack(serverPlayer)) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.nexus_echoes.obelisk_no_link"), true);
            return InteractionResult.FAIL;
        }
        return InteractionResult.sidedSuccess(false);
    }
}
