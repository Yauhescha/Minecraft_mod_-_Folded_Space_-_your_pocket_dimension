package com.hescha.pocketspace;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class ActivatedPocketSpaceItem extends Item {

  public ActivatedPocketSpaceItem(Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResult useOn(UseOnContext context) {
    Level level = context.getLevel();
    if (level.isClientSide()) {
      return InteractionResult.SUCCESS;
    }

    if (!(level instanceof ServerLevel serverLevel)) {
      return InteractionResult.PASS;
    }

    ItemStack stack = context.getItemInHand();
    String pocketSpaceId = stack.get(ModComponents.POCKET_SPACE_ID);
    if (pocketSpaceId == null || pocketSpaceId.isBlank()) {
      Player player = context.getPlayer();
      if (player != null) {
        player.sendSystemMessage(Component.literal("This pocket space item has no saved space id."));
      }
      return InteractionResult.FAIL;
    }

    BlockPos targetOrigin = context.getClickedPos().relative(context.getClickedFace());
    boolean placed = PocketSpaceStorage.place(serverLevel, targetOrigin, pocketSpaceId);
    if (!placed) {
      Player player = context.getPlayer();
      if (player != null) {
        player.sendSystemMessage(Component.literal("Failed to restore pocket space."));
      }
      return InteractionResult.FAIL;
    }

    Player player = context.getPlayer();
    if (player != null && !player.getAbilities().instabuild) {
      stack.shrink(1);
    }

    if (player != null) {
      player.sendSystemMessage(Component.literal("Pocket space restored."));
    }

    return InteractionResult.SUCCESS;
  }

  @Override
  public void appendHoverText(
      ItemStack stack,
      TooltipContext context,
      TooltipDisplay displayComponent,
      Consumer<Component> textConsumer,
      TooltipFlag flag
  ) {
    String pocketSpaceId = stack.get(ModComponents.POCKET_SPACE_ID);
    if (pocketSpaceId != null && !pocketSpaceId.isBlank()) {
      textConsumer.accept(Component.literal("Stored id: " + pocketSpaceId));
    }
  }
}
