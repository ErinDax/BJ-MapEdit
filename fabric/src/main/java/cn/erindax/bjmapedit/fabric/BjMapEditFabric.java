package cn.erindax.bjmapedit.fabric;

import cn.erindax.bjmapedit.networking.ModMessages;
import cn.erindax.bjmapedit.networking.payload.DatapackOpPayload;
import cn.erindax.bjmapedit.networking.payload.DatapackOpResultPayload;
import cn.erindax.bjmapedit.networking.payload.ExecuteCommandsPayload;
import cn.erindax.bjmapedit.networking.payload.GiveItemPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class BjMapEditFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playC2S().register(GiveItemPayload.TYPE, GiveItemPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ExecuteCommandsPayload.TYPE, ExecuteCommandsPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(DatapackOpPayload.TYPE, DatapackOpPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(DatapackOpResultPayload.TYPE, DatapackOpResultPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(GiveItemPayload.TYPE, (payload, context) ->
			ModMessages.giveItem(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(ExecuteCommandsPayload.TYPE, (payload, context) ->
			ModMessages.executeCommands(context.player(), payload));
		ServerPlayNetworking.registerGlobalReceiver(DatapackOpPayload.TYPE, (payload, context) ->
			ModMessages.datapackOp(context.player(), payload));
	}
}
