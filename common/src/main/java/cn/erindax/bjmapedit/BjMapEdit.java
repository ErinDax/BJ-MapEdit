package cn.erindax.bjmapedit;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BjMapEdit {
	public static final String MOD_ID = "bj_mapedit";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private BjMapEdit() {
	}

	public static ResourceLocation id(String path) {
		return new ResourceLocation(MOD_ID, path);
	}
}
