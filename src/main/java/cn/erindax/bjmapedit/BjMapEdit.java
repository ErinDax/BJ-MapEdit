package cn.erindax.bjmapedit;

import cn.erindax.bjmapedit.networking.ModMessages;
import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BjMapEdit implements ModInitializer {
	public static final String MOD_ID = "bj_mapedit";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModMessages.registerPayloads();
		ModMessages.registerServerReceivers();

		LOGGER.info("BJ-MapEdit initialized!");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
