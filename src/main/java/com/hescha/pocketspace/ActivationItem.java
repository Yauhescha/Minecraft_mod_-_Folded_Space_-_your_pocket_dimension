package com.hescha.pocketspace;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.hescha.pocketspace.HeschaPocketSpace.MOD_ID;

public class ActivationItem extends Item {
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  public ActivationItem(Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResult useOn(UseOnContext context) {
    ItemStack stack = context.getItemInHand();
    if (stack.isEmpty() || stack.getItem() != ModItems.ACTIVATION_ITEM) {
      return InteractionResult.PASS;
    }

    Level level = context.getLevel();
    if (level.isClientSide()) {
      return InteractionResult.SUCCESS;
    }

    if (!(level instanceof ServerLevel serverLevel)) {
      return InteractionResult.PASS;
    }

    BlockPos origin = context.getClickedPos();
    Player player = context.getPlayer();

    BlockState clickedState = level.getBlockState(origin);
    if (clickedState.getBlock() != ModBlocks.POCKET_SPACE_BORDER) {
      LOGGER.info("Activation item used on a different block");
      return InteractionResult.PASS;
    }

    //[0]=X, [1]=Y, [2]=Z
    BlockPos[] corners = findFarthestSameTypeInAxes(level, origin, ModBlocks.POCKET_SPACE_BORDER);
    for (BlockPos corner : corners) {
      if (corner == null) {
        LOGGER.info("Dont have all border blocks set");
        if (player != null) {
          player.sendSystemMessage(Component.literal("Set 3 border blocks on X/Y/Z axes from the clicked origin block."));
        }
        return InteractionResult.FAIL;
      }
    }

    PocketSpaceBounds bounds = PocketSpaceBounds.from(origin, corners[0], corners[1], corners[2]);
    if (!bounds.isWithinLimit(PocketSpaceStorage.MAX_SIZE)) {
      if (player != null) {
        player.sendSystemMessage(Component.literal("Pocket space is too large. Max: " + PocketSpaceStorage.MAX_SIZE + "x" + PocketSpaceStorage.MAX_SIZE + "x" + PocketSpaceStorage.MAX_SIZE));
      }
      return InteractionResult.FAIL;
    }

    try {
      String pocketSpaceId = PocketSpaceStorage.capture(serverLevel, bounds);
      ItemStack activatedPocketSpace = new ItemStack(ModItems.ACTIVATED_POCKET_SPACE);
      activatedPocketSpace.set(ModComponents.POCKET_SPACE_ID, pocketSpaceId);

      if (player instanceof ServerPlayer serverPlayer) {
        if (!serverPlayer.getInventory().add(activatedPocketSpace)) {
          serverPlayer.drop(activatedPocketSpace, false);
        }

        if (!serverPlayer.getAbilities().instabuild) {
          stack.shrink(1);
        }

        serverPlayer.sendSystemMessage(
            Component.literal("Pocket space captured: " + bounds.width() + "x" + bounds.height() + "x" + bounds.depth())
        );
      }

      return InteractionResult.SUCCESS;
    } catch (IOException e) {
      LOGGER.error("Failed to capture pocket space", e);
      if (player != null) {
        player.sendSystemMessage(Component.literal("Failed to save pocket space."));
      }
      return InteractionResult.FAIL;
    }
  }

  private BlockPos[] findFarthestSameTypeInAxes(Level level, BlockPos origin, Block targetBlock) {
    BlockPos[] result = new BlockPos[3]; // [0]=X, [1]=Y, [2]=Z

    Direction[][] axisDirs = new Direction[][]{
        {Direction.EAST, Direction.WEST},
        {Direction.UP, Direction.DOWN},
        {Direction.SOUTH, Direction.NORTH}
    };

    for (int axis = 0; axis < 3; axis++) {
      int bestDist = 0;
      BlockPos bestPos = null;
      for (Direction dir : axisDirs[axis]) {
        for (int d = 1; d <= PocketSpaceStorage.MAX_SIZE - 1; d++) {
          BlockPos p = origin.relative(dir, d);
          BlockState state = level.getBlockState(p);
          if (state.getBlock() == targetBlock) {
            if (d > bestDist) {
              bestDist = d;
              bestPos = p.immutable();
            }
          }
        }
      }
      result[axis] = bestPos;
    }
    return result;
  }
}
