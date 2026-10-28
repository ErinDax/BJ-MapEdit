package cn.erindax.bjmapedit.neoforge;

import cn.erindax.bjmapedit.BjMapEdit;
import cn.erindax.bjmapedit.client.DatapackNet;
import cn.erindax.bjmapedit.networking.ModMessages;
import cn.erindax.bjmapedit.networking.payload.DatapackOpPayload;
import cn.erindax.bjmapedit.networking.payload.DatapackOpResultPayload;
import cn.erindax.bjmapedit.networking.payload.ExecuteCommandsPayload;
import cn.erindax.bjmapedit.networking.payload.GiveItemPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.HandlerThread;

import java.util.function.BiConsumer;

@Mod(BjMapEdit.MOD_ID)
public final class BjMapEditNeoForge {
	private static final String PROTOCOL = "1";

	public BjMapEditNeoForge(IEventBus modBus) {
		modBus.addListener(BjMapEditNeoForge::registerPayloads);
	}

	private static void registerPayloads(RegisterPayloadHandlersEvent event) {
		event.registrar(PROTOCOL)
			.optional()
			.executesOn(HandlerThread.MAIN)
			.playToServer(GiveItemPayload.TYPE, GiveItemPayload.CODEC, server(ModMessages::giveItem))
			.playToServer(ExecuteCommandsPayload.TYPE, ExecuteCommandsPayload.CODEC, server(ModMessages::executeCommands))
			.playToServer(DatapackOpPayload.TYPE, DatapackOpPayload.CODEC, server(ModMessages::datapackOp))
			.playToClient(DatapackOpResultPayload.TYPE, DatapackOpResultPayload.CODEC, (payload, context) ->
				DatapackNet.dispatch(payload));
	}

	private static <T extends CustomPacketPayload> IPayloadHandler<T> server(BiConsumer<ServerPlayer, T> handler) {
		return (payload, context) -> {
			if (context.player() instanceof ServerPlayer player) {
				handler.accept(player, payload);
			}
		};
	}
}
