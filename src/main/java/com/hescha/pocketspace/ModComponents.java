package com.hescha.pocketspace;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class ModComponents {

  public static final DataComponentType<String> POCKET_SPACE_ID = Registry.register(
      BuiltInRegistries.DATA_COMPONENT_TYPE,
      Identifier.fromNamespaceAndPath(HeschaPocketSpace.MOD_ID, "pocket_space_id"),
      DataComponentType.<String>builder()
          .persistent(Codec.STRING)
          .build()
  );

  public static void initialize() {
    // Нужен, чтобы компонент был зарегистрирован во время onInitialize().
  }
}
