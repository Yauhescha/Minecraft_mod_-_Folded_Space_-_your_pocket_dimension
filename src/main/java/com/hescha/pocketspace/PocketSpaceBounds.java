package com.hescha.pocketspace;

import net.minecraft.core.BlockPos;

public record PocketSpaceBounds(BlockPos min, BlockPos max) {

  public static PocketSpaceBounds from(BlockPos origin, BlockPos xCorner, BlockPos yCorner, BlockPos zCorner) {
    int minX = Math.min(origin.getX(), xCorner.getX());
    int maxX = Math.max(origin.getX(), xCorner.getX());

    int minY = Math.min(origin.getY(), yCorner.getY());
    int maxY = Math.max(origin.getY(), yCorner.getY());

    int minZ = Math.min(origin.getZ(), zCorner.getZ());
    int maxZ = Math.max(origin.getZ(), zCorner.getZ());

    return new PocketSpaceBounds(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ));
  }

  public int width() {
    return max.getX() - min.getX() + 1;
  }

  public int height() {
    return max.getY() - min.getY() + 1;
  }

  public int depth() {
    return max.getZ() - min.getZ() + 1;
  }

  public boolean isWithinLimit(int maxSize) {
    return width() <= maxSize && height() <= maxSize && depth() <= maxSize;
  }
}
