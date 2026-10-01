package com.nexus.echoes.machines;

import com.nexus.echoes.registry.NexusRegistries;
import com.nexus.echoes.research.mc.ResearchGatekeeper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/** The NEXUS Processor block: dust → refined material, kinetic powered. */
public class ProcessorBlock extends AbstractOrientedMachineBlock {

    public ProcessorBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProcessorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, NexusRegistries.PROCESSOR_BE.get(),
                AbstractNexusMachineBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            // Phase 4 research gate: the machine block stays research-agnostic,
            // but operating it requires the technology unlock (ADR-010).
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
            if (id != null && !ResearchGatekeeper.checkUseMachine(serverPlayer, id)) {
                return InteractionResult.FAIL;
            }
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ProcessorBlockEntity processor) {
                NetworkHooks.openScreen(serverPlayer, processor, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
