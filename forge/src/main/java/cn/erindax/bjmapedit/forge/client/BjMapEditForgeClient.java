package cn.erindax.bjmapedit.forge.client;

import cn.erindax.bjmapedit.client.BjMapEditClient;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class BjMapEditForgeClient {
	private BjMapEditForgeClient() {
	}

	public static void init(IEventBus modBus) {
		modBus.addListener((RegisterKeyMappingsEvent event) -> BjMapEditClient.KEY_MAPPINGS.forEach(event::register));
		MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent event) -> {
			if (event.phase == TickEvent.Phase.END) {
				BjMapEditClient.tick();
			}
		});
		MinecraftForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) ->
			event.getDispatcher().register(BjMapEditClient.command()));
	}
}
