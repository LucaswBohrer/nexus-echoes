package com.nexus.echoes.dimension.block;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Anomaly Ward: suppresses anomaly effects within {@link WardBlockEntity#RADIUS}
 * blocks. The first Hollow-derived technology with an immediate, concrete use
 * (counterplay against static fields, echo bursts and dimensional exposure).
 *
 * <p>Crafting/placing/operating is gated on the {@code anomaly_ward}
 * technology (unlocked by the {@code hollow_exploration} research).
 */
public class AnomalyWardBlock extends BaseEntityBlock {

    public AnomalyWardBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return NexusRegistries.WARD_BE.get().create(pos, state);
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
        serverPlayer.displayClientMessage(Component.translatable(
                "message.nexus_echoes.ward_status", WardBlockEntity.RADIUS), true);
        return InteractionResult.sidedSuccess(false);
    }
}
