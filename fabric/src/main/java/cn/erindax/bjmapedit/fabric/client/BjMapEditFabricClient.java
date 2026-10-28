package cn.erindax.bjmapedit.fabric.client;

import cn.erindax.bjmapedit.client.BjMapEditClient;
import cn.erindax.bjmapedit.client.DatapackNet;
import cn.erindax.bjmapedit.networking.payload.DatapackOpResultPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class BjMapEditFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BjMapEditClient.KEY_MAPPINGS.forEach(KeyBindingHelper::registerKeyBinding);
		ClientPlayNetworking.registerGlobalReceiver(DatapackOpResultPayload.TYPE, (payload, context) ->
			context.client().execute(() -> DatapackNet.dispatch(payload)));
		ClientTickEvents.END_CLIENT_TICK.register(client -> BjMapEditClient.tick());
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
			dispatcher.register(BjMapEditClient.command()));
	}
}
