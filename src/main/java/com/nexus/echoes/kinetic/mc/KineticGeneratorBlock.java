package com.nexus.echoes.kinetic.mc;

import com.nexus.echoes.registry.NexusRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * The kinetic generator block. Right-click opens the stats GUI.
 */
public class KineticGeneratorBlock extends KineticBlock {

    public KineticGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KineticGeneratorBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof KineticGeneratorBlockEntity be
                && player instanceof ServerPlayer serverPlayer) {
            // Pass the BlockPos through the open-screen buffer (client menu reads it).
            NetworkHooks.openScreen(serverPlayer, be, buf -> buf.writeBlockPos(pos));
            return InteractionResult.sidedSuccess(false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
