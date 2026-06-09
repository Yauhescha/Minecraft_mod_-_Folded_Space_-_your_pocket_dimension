package com.hescha.pocketspace;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeschaPocketSpace implements ModInitializer {

  public static final String MOD_ID = "heschapocketspace";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  @Override
  public void onInitialize() {
    LOGGER.info("Initialize HeschaPocketSpace mod");

    ModComponents.initialize();
    ModItems.initialize();
    ModBlocks.initialize();

    LOGGER.info("Adding pocket space items to creative mode tabs");
    CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS)
        .register((creativeTab) -> {
          creativeTab.accept(ModItems.ACTIVATION_ITEM);
          creativeTab.accept(ModItems.ACTIVATED_POCKET_SPACE);
        });

    LOGGER.info("Adding POCKET_SPACE_BORDER to creative mode tabs");
    CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS)
        .register((creativeTab) -> creativeTab.accept(ModBlocks.POCKET_SPACE_BORDER.asItem()));
  }
}
