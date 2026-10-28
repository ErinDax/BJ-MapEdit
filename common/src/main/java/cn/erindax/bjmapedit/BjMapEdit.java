package cn.erindax.bjmapedit;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BjMapEdit {
	public static final String MOD_ID = "bj_mapedit";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private BjMapEdit() {
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
