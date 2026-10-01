package com.nexus.echoes.dimension.item;

import com.nexus.echoes.dimension.discovery.DiscoveryIds;
import com.nexus.echoes.dimension.mc.HollowDiscoveryData;
import com.nexus.echoes.research.ResearchSources;
import com.nexus.echoes.research.mc.ResearchManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Rare research artifact (ADR-011). Analyzing it (right-click) converts it
 * into research points through the existing source abstraction — the reward
 * loop for deep exploration. Repeatable: every fragment is worth analyzing.
 */
public class MemoryFragmentItem extends Item {

    public MemoryFragmentItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            HollowDiscoveryData.get(serverPlayer.getServer())
                    .grant(serverPlayer.getUUID(), DiscoveryIds.MEMORY_FRAGMENT);
            ResearchManager.grantFromSource(serverPlayer, ResearchSources.ANALYZE_MEMORY_FRAGMENT);
            if (!serverPlayer.isCreative()) {
                stack.shrink(1);
            }
            serverPlayer.getCooldowns().addCooldown(this, 20);
            level.playSound(null, serverPlayer.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.4F, 1.4F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
