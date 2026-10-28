package cn.erindax.bjmapedit.forge;

import cn.erindax.bjmapedit.BjMapEdit;
import cn.erindax.bjmapedit.client.DatapackNet;
import cn.erindax.bjmapedit.networking.ModMessages;
import cn.erindax.bjmapedit.networking.payload.DatapackOpPayload;
import cn.erindax.bjmapedit.networking.payload.DatapackOpResultPayload;
import cn.erindax.bjmapedit.networking.payload.ExecuteCommandsPayload;
import cn.erindax.bjmapedit.networking.payload.GiveItemPayload;
import cn.erindax.bjmapedit.networking.payload.ModPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class PlatformNetworkImpl {
	private static final String PROTOCOL = "1";
	private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
		BjMapEdit.id("main"),
		() -> PROTOCOL,
		NetworkRegistry.acceptMissingOr(PROTOCOL),
		NetworkRegistry.acceptMissingOr(PROTOCOL)
	);

	private PlatformNetworkImpl() {
	}

	static void register() {
		serverbound(0, GiveItemPayload.class, GiveItemPayload::read, ModMessages::giveItem);
		serverbound(1, ExecuteCommandsPayload.class, ExecuteCommandsPayload::read, ModMessages::executeCommands);
		serverbound(2, DatapackOpPayload.class, DatapackOpPayload::read, ModMessages::datapackOp);
		CHANNEL.registerMessage(3, DatapackOpResultPayload.class, DatapackOpResultPayload::write, DatapackOpResultPayload::read,
			(payload, context) -> {
				NetworkEvent.Context ctx = context.get();
				ctx.enqueueWork(() -> DatapackNet.dispatch(payload));
				ctx.setPacketHandled(true);
			}, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
	}

	private static <T extends ModPayload> void serverbound(int index, Class<T> type, Function<FriendlyByteBuf, T> reader,
			BiConsumer<ServerPlayer, T> handler) {
		CHANNEL.registerMessage(index, type, ModPayload::write, reader, (T payload, Supplier<NetworkEvent.Context> context) -> {
			NetworkEvent.Context ctx = context.get();
			ServerPlayer sender = ctx.getSender();
			if (sender != null) {
				ctx.enqueueWork(() -> handler.accept(sender, payload));
			}
			ctx.setPacketHandled(true);
		}, Optional.of(NetworkDirection.PLAY_TO_SERVER));
	}

	public static boolean canSendToServer(ResourceLocation id) {
		ClientPacketListener connection = Minecraft.getInstance().getConnection();
		return connection != null && CHANNEL.isRemotePresent(connection.getConnection());
	}

	public static void sendToServer(ModPayload payload) {
		if (canSendToServer(payload.id())) {
			CHANNEL.sendToServer(payload);
		}
	}

	public static void sendToPlayer(ServerPlayer player, ModPayload payload) {
		if (CHANNEL.isRemotePresent(player.connection.connection)) {
			CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
		}
	}
}
