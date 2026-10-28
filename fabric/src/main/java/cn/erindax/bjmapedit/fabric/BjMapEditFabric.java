package cn.erindax.bjmapedit.fabric;

import cn.erindax.bjmapedit.networking.ModMessages;
import cn.erindax.bjmapedit.networking.payload.DatapackOpPayload;
import cn.erindax.bjmapedit.networking.payload.ExecuteCommandsPayload;
import cn.erindax.bjmapedit.networking.payload.GiveItemPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class BjMapEditFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		ServerPlayNetworking.registerGlobalReceiver(GiveItemPayload.ID, (server, player, handler, buf, sender) -> {
			GiveItemPayload payload = GiveItemPayload.read(buf);
			server.execute(() -> ModMessages.giveItem(player, payload));
		});
		ServerPlayNetworking.registerGlobalReceiver(ExecuteCommandsPayload.ID, (server, player, handler, buf, sender) -> {
			ExecuteCommandsPayload payload = ExecuteCommandsPayload.read(buf);
			server.execute(() -> ModMessages.executeCommands(player, payload));
		});
		ServerPlayNetworking.registerGlobalReceiver(DatapackOpPayload.ID, (server, player, handler, buf, sender) -> {
			DatapackOpPayload payload = DatapackOpPayload.read(buf);
			ModMessages.datapackOp(player, payload);
		});
	}
}
