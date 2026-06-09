package com.hescha.pocketspace;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public class ModItems {

  public static final Item ACTIVATION_ITEM = registerItem(
      "pocket_space_activation_item",
      ActivationItem::new,
      new Item.Properties()
  );

  public static final Item ACTIVATED_POCKET_SPACE = registerItem(
      "pocket_space_item",
      ActivatedPocketSpaceItem::new,
      new Item.Properties().stacksTo(1)
  );

  public static <T extends Item> T registerItem(String name, Function<Item.Properties, T> itemFactory, Item.Properties settings) {
    ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(HeschaPocketSpace.MOD_ID, name));
    T item = itemFactory.apply(settings.setId(itemKey));
    Registry.register(BuiltInRegistries.ITEM, itemKey, item);
    return item;
  }

  public static void initialize() {
    // Нужен, чтобы класс ModItems был загружен во время onInitialize().
  }
}
