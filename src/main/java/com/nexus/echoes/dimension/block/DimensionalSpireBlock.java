package com.nexus.echoes.dimension.block;

import com.nexus.echoes.dimension.travel.HollowTravelService;
import com.nexus.echoes.dimension.mc.HollowTravel;
import com.nexus.echoes.registry.NexusRegistries;
import com.nexus.echoes.research.mc.ResearchGatekeeper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * First-generation dimensional access device (ADR-011).
 *
 * <p>Not a Nether-portal clone: a kinetic machine. The spire is a kinetic
 * consumer (240 RPM / 25 N·m — it genuinely needs a 2:1 gearbox off the
 * standard generator) that accumulates dimensional charge while powered.
 * Right-click at full charge crosses into The Hollow; right-click while
 * charging reports progress. Crafting, placing and operating are gated on
 * the {@code hollow_access} technology (Phase 4 research).
 */
public class DimensionalSpireBlock extends BaseEntityBlock {

    public DimensionalSpireBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return NexusRegistries.SPIRE_BE.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, NexusRegistries.SPIRE_BE.get(),
                SpireBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id != null && !ResearchGatekeeper.checkUseMachine(serverPlayer, id)) {
            return InteractionResult.FAIL;
        }
        if (!HollowTravelService.canTravel(
                com.nexus.echoes.research.mc.ResearchManager.graph(serverPlayer.getServer()),
                com.nexus.echoes.research.mc.ResearchManager.stateOf(serverPlayer))) {
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.nexus_echoes.spire_no_access"), true);
            return InteractionResult.FAIL;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof SpireBlockEntity spire)) {
            return InteractionResult.FAIL;
        }
        if (!spire.isCharged()) {
            int pct = (int) Math.round(spire.getCharge() * 100.0);
            serverPlayer.displayClientMessage(Component.translatable(
                    "message.nexus_echoes.spire_charging", pct,
                    (int) SpireBlockEntity.REQUIRED_RPM), true);
            return InteractionResult.sidedSuccess(false);
        }
        if (HollowTravel.travelTo(serverPlayer, pos)) {
            spire.discharge();
        }
        return InteractionResult.sidedSuccess(false);
    }
}
