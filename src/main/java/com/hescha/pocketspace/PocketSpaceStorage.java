package com.hescha.pocketspace;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.hescha.pocketspace.HeschaPocketSpace.MOD_ID;

public class PocketSpaceStorage {
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  public static final int MAX_SIZE = 24;
  private static final int UPDATE_FLAGS = 3 | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;

  private static final String VERSION = "Version";
  private static final String WIDTH = "Width";
  private static final String HEIGHT = "Height";
  private static final String DEPTH = "Depth";
  private static final String BLOCKS = "Blocks";
  private static final String ENTITIES = "Entities";

  private static final String X = "X";
  private static final String Y = "Y";
  private static final String Z = "Z";
  private static final String STATE = "State";
  private static final String BLOCK_ENTITY = "BlockEntity";

  private static final String REL_X = "RelX";
  private static final String REL_Y = "RelY";
  private static final String REL_Z = "RelZ";
  private static final String ENTITY_DATA = "Data";

  private PocketSpaceStorage() {
  }

  public static String capture(ServerLevel level, PocketSpaceBounds bounds) throws IOException {
    String pocketSpaceId = UUID.randomUUID().toString();
    CompoundTag root = serialize(level, bounds);

    Path file = file(level, pocketSpaceId);
    Files.createDirectories(file.getParent());
    NbtIo.writeCompressed(root, file);

    clearOriginalArea(level, bounds);
    return pocketSpaceId;
  }

  public static boolean place(ServerLevel level, BlockPos origin, String pocketSpaceId) {
    Path file = file(level, pocketSpaceId);
    if (!Files.exists(file)) {
      LOGGER.warn("Pocket space file not found: {}", file);
      return false;
    }

    try {
      CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
      int height = root.getIntOr(HEIGHT, 0);
      if (!isInsideWorldHeight(level, origin, height)) {
        LOGGER.warn("Cannot place pocket space {} outside world height at origin {}", pocketSpaceId, origin);
        return false;
      }

      deserialize(level, origin, root);
      Files.deleteIfExists(file);
      return true;
    } catch (Exception e) {
      LOGGER.error("Failed to place pocket space {}", pocketSpaceId, e);
      return false;
    }
  }

  private static CompoundTag serialize(ServerLevel level, PocketSpaceBounds bounds) {
    CompoundTag root = new CompoundTag();
    root.putInt(VERSION, 1);
    root.putInt(WIDTH, bounds.width());
    root.putInt(HEIGHT, bounds.height());
    root.putInt(DEPTH, bounds.depth());

    ListTag blocks = new ListTag();
    for (int y = bounds.min().getY(); y <= bounds.max().getY(); y++) {
      for (int z = bounds.min().getZ(); z <= bounds.max().getZ(); z++) {
        for (int x = bounds.min().getX(); x <= bounds.max().getX(); x++) {
          BlockPos worldPos = new BlockPos(x, y, z);
          BlockState state = level.getBlockState(worldPos);

          if (state.isAir()) {
            continue;
          }

          CompoundTag blockTag = new CompoundTag();
          blockTag.putInt(X, x - bounds.min().getX());
          blockTag.putInt(Y, y - bounds.min().getY());
          blockTag.putInt(Z, z - bounds.min().getZ());
          blockTag.put(STATE, NbtUtils.writeBlockState(state));

          BlockEntity blockEntity = level.getBlockEntity(worldPos);
          if (blockEntity != null) {
            blockTag.put(BLOCK_ENTITY, blockEntity.saveWithFullMetadata(level.registryAccess()));
          }

          blocks.add(blockTag);
        }
      }
    }
    root.put(BLOCKS, blocks);

    ListTag entities = serializeEntities(level, bounds);
    root.put(ENTITIES, entities);

    return root;
  }

  private static ListTag serializeEntities(ServerLevel level, PocketSpaceBounds bounds) {
    ListTag result = new ListTag();
    AABB area = new AABB(
        bounds.min().getX(),
        bounds.min().getY(),
        bounds.min().getZ(),
        bounds.max().getX() + 1,
        bounds.max().getY() + 1,
        bounds.max().getZ() + 1
    );

    List<Entity> entities = level.getEntitiesOfClass(Entity.class, area, entity -> !(entity instanceof Player));
    for (Entity entity : entities) {
      if (entity.isPassenger()) {
        continue;
      }

      TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
      if (!entity.save(output)) {
        continue;
      }

      CompoundTag entityTag = new CompoundTag();
      entityTag.putDouble(REL_X, entity.getX() - bounds.min().getX());
      entityTag.putDouble(REL_Y, entity.getY() - bounds.min().getY());
      entityTag.putDouble(REL_Z, entity.getZ() - bounds.min().getZ());
      entityTag.put(ENTITY_DATA, output.buildResult());
      result.add(entityTag);
    }

    return result;
  }

  private static void clearOriginalArea(ServerLevel level, PocketSpaceBounds bounds) {
    AABB area = new AABB(
        bounds.min().getX(),
        bounds.min().getY(),
        bounds.min().getZ(),
        bounds.max().getX() + 1,
        bounds.max().getY() + 1,
        bounds.max().getZ() + 1
    );

    List<Entity> entities = level.getEntitiesOfClass(Entity.class, area, entity -> !(entity instanceof Player));
    for (Entity entity : entities) {
      entity.discard();
    }

    for (int y = bounds.min().getY(); y <= bounds.max().getY(); y++) {
      for (int z = bounds.min().getZ(); z <= bounds.max().getZ(); z++) {
        for (int x = bounds.min().getX(); x <= bounds.max().getX(); x++) {
          BlockPos pos = new BlockPos(x, y, z);
          if (level.getBlockEntity(pos) != null) {
            level.removeBlockEntity(pos);
          }
          level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
        }
      }
    }
  }

  private static void deserialize(ServerLevel level, BlockPos origin, CompoundTag root) {
    int width = root.getIntOr(WIDTH, 0);
    int height = root.getIntOr(HEIGHT, 0);
    int depth = root.getIntOr(DEPTH, 0);

    clearTargetArea(level, origin, width, height, depth);

    List<BlockEntityPlacement> blockEntities = new ArrayList<>();
    ListTag blocks = root.getListOrEmpty(BLOCKS);

    for (Tag tag : blocks) {
      if (!(tag instanceof CompoundTag blockTag)) {
        continue;
      }

      int relX = blockTag.getIntOr(X, 0);
      int relY = blockTag.getIntOr(Y, 0);
      int relZ = blockTag.getIntOr(Z, 0);
      BlockPos targetPos = origin.offset(relX, relY, relZ);

      BlockState state = NbtUtils.readBlockState(
          level.registryAccess().lookupOrThrow(Registries.BLOCK),
          blockTag.getCompoundOrEmpty(STATE)
      );

      if (!state.isAir()) {
        level.setBlock(targetPos, state, UPDATE_FLAGS);
      }

      Optional<CompoundTag> blockEntityTag = blockTag.getCompound(BLOCK_ENTITY);
      blockEntityTag.ifPresent(compoundTag -> blockEntities.add(new BlockEntityPlacement(targetPos, compoundTag.copy())));
    }

    for (BlockEntityPlacement placement : blockEntities) {
      CompoundTag blockEntityTag = placement.tag();
      blockEntityTag.putInt("x", placement.pos().getX());
      blockEntityTag.putInt("y", placement.pos().getY());
      blockEntityTag.putInt("z", placement.pos().getZ());

      BlockState state = level.getBlockState(placement.pos());
      BlockEntity loadedBlockEntity = BlockEntity.loadStatic(
          placement.pos(),
          state,
          blockEntityTag,
          level.registryAccess()
      );

      if (loadedBlockEntity == null) {
        continue;
      }

      level.setBlockEntity(loadedBlockEntity);
      loadedBlockEntity.setChanged();
      level.sendBlockUpdated(placement.pos(), state, state, UPDATE_FLAGS);
    }

    deserializeEntities(level, origin, root.getListOrEmpty(ENTITIES));
  }

  private static void deserializeEntities(ServerLevel level, BlockPos origin, ListTag entities) {
    for (Tag tag : entities) {
      if (!(tag instanceof CompoundTag entityWrapper)) {
        continue;
      }

      Optional<CompoundTag> data = entityWrapper.getCompound(ENTITY_DATA);
      if (data.isEmpty()) {
        continue;
      }

      double x = origin.getX() + entityWrapper.getDoubleOr(REL_X, 0.0D);
      double y = origin.getY() + entityWrapper.getDoubleOr(REL_Y, 0.0D);
      double z = origin.getZ() + entityWrapper.getDoubleOr(REL_Z, 0.0D);

      CompoundTag entityData = data.get().copy();
      entityData.remove("UUID");

      Entity loadedEntity = EntityType.loadEntityRecursive(
          entityData,
          level,
          EntitySpawnReason.LOAD,
          entity -> {
            entity.snapTo(x, y, z, entity.getYRot(), entity.getXRot());
            return entity;
          }
      );

      if (loadedEntity != null) {
        level.addFreshEntity(loadedEntity);
      }
    }
  }

  private static void clearTargetArea(ServerLevel level, BlockPos origin, int width, int height, int depth) {
    AABB area = new AABB(
        origin.getX(),
        origin.getY(),
        origin.getZ(),
        origin.getX() + width,
        origin.getY() + height,
        origin.getZ() + depth
    );

    List<Entity> entities = level.getEntitiesOfClass(Entity.class, area, entity -> !(entity instanceof Player));
    for (Entity entity : entities) {
      entity.discard();
    }

    for (int y = 0; y < height; y++) {
      for (int z = 0; z < depth; z++) {
        for (int x = 0; x < width; x++) {
          BlockPos pos = origin.offset(x, y, z);
          if (level.getBlockEntity(pos) != null) {
            level.removeBlockEntity(pos);
          }
          level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
        }
      }
    }
  }

  private static boolean isInsideWorldHeight(ServerLevel level, BlockPos origin, int height) {
    // Minecraft 26.1.x removed the old ServerLevel#getMinBuildHeight/#getMaxBuildHeight accessors.
    // For the first implementation we target vanilla world height only.
    // If later you support custom dimensions/datapacks with custom height, replace these constants
    // with the corresponding 26.1.x dimension-height API after choosing that compatibility target.
    int minBuildHeight = -64;
    int maxBuildHeightExclusive = 320;

    return height > 0
        && origin.getY() >= minBuildHeight
        && origin.getY() + height - 1 < maxBuildHeightExclusive;
  }

  private static Path file(ServerLevel level, String pocketSpaceId) {
    return storageRoot(level).resolve(pocketSpaceId + ".nbt");
  }

  private static Path storageRoot(ServerLevel level) {
    return level.getServer()
        .getWorldPath(LevelResource.ROOT)
        .resolve("data")
        .resolve(HeschaPocketSpace.MOD_ID)
        .resolve("pocket_spaces");
  }

  private record BlockEntityPlacement(BlockPos pos, CompoundTag tag) {
  }
}
