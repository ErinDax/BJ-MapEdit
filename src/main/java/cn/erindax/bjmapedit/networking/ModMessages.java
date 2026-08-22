package cn.erindax.bjmapedit.networking;

import cn.erindax.bjmapedit.networking.payload.DatapackOpPayload;
import cn.erindax.bjmapedit.networking.payload.DatapackOpResultPayload;
import cn.erindax.bjmapedit.networking.payload.ExecuteCommandsPayload;
import cn.erindax.bjmapedit.networking.payload.GiveItemPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModMessages {

	public static void registerPayloads() {
		PayloadTypeRegistry.playC2S().register(GiveItemPayload.TYPE, GiveItemPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ExecuteCommandsPayload.TYPE, ExecuteCommandsPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(DatapackOpPayload.TYPE, DatapackOpPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(DatapackOpResultPayload.TYPE, DatapackOpResultPayload.CODEC);
	}

	public static void registerServerReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(GiveItemPayload.TYPE, (payload, context) -> {
			if (!EditorPerms.canEdit(context.player())) return;
			context.player().getInventory().add(payload.stack());
		});

		ServerPlayNetworking.registerGlobalReceiver(ExecuteCommandsPayload.TYPE, (payload, context) -> {
			if (context.player().isSpectator() || context.player().isDeadOrDying()) return;
			if (!context.player().hasPermissions(2)) return;
			var server = context.player().getServer();
			if (server == null) return;
			var source = context.player().createCommandSourceStack().withPermission(4);
			for (String cmd : payload.commands()) {
				server.getCommands().performPrefixedCommand(source, cmd);
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(DatapackOpPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> DatapackOps.handle(context.player(), payload));
		});
	}
}
