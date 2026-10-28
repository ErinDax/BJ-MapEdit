package cn.erindax.bjmapedit.forge;

import cn.erindax.bjmapedit.BjMapEdit;
import cn.erindax.bjmapedit.forge.client.BjMapEditForgeClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(BjMapEdit.MOD_ID)
public final class BjMapEditForge {
	public BjMapEditForge(FMLJavaModLoadingContext context) {
		PlatformNetworkImpl.register();
		if (FMLEnvironment.dist == Dist.CLIENT) {
			BjMapEditForgeClient.init(context.getModEventBus());
		}
	}
}
