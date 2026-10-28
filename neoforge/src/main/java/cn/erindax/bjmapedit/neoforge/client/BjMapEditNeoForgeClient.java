package cn.erindax.bjmapedit.neoforge.client;

import cn.erindax.bjmapedit.BjMapEdit;
import cn.erindax.bjmapedit.client.BjMapEditClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = BjMapEdit.MOD_ID, dist = Dist.CLIENT)
public final class BjMapEditNeoForgeClient {
	public BjMapEditNeoForgeClient(IEventBus modBus) {
		modBus.addListener((RegisterKeyMappingsEvent event) -> {
			event.registerCategory(BjMapEditClient.CATEGORY);
			BjMapEditClient.KEY_MAPPINGS.forEach(event::register);
		});
		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> BjMapEditClient.tick());
		NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) ->
			event.getDispatcher().register(BjMapEditClient.command()));
	}
}
