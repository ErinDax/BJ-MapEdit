package cn.erindax.bjmapedit.networking;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ClockOps {

	public static final String PACK = "bjclock";
	public static final String INSTALLED = "screen.bj_mapedit.clock_installed";
	public static final String INSTALL_FAILED = "screen.bj_mapedit.clock_install_failed";
	public static final List<String> TIMES = List.of("dawn", "morning", "noon", "afternoon", "dusk", "night", "midnight", "predawn");
	public static final int MAX_DAY = 999999;

	private static final String BUNDLE = "bj_mapedit_bundled/clock.zip";
	private static final String ALREADY = "screen.bj_mapedit.clock_already";
	private static final String NOT_READY = "screen.bj_mapedit.clock_not_ready";
	private static final String BAD = "screen.bj_mapedit.clock_bad_value";
	private static final String KEEP_ONE = "screen.bj_mapedit.clock_keep_one";
	private static final String DATA = "clock.data";
	private static final String ON = "clock.on";
	private static final String OFF = "clock.off";

	private ClockOps() {}

	public static boolean installed(MinecraftServer server) {
		return server.getFunctions().get(id("hud/update")).isPresent();
	}

	public static String state(MinecraftServer server, ServerPlayer player) {
		Scoreboard sb = server.getScoreboard();
		Objective obj = sb.getObjective(DATA);
		List<ServerPlayer> players = server.getPlayerList().getPlayers();
		int on = 0;
		for (ServerPlayer p : players) {
			if (p.getTags().contains(ON)) on++;
		}
		return "{\"installed\":" + installed(server)
			+ ",\"ord\":" + get(sb, obj, "#ord", 1)
			+ ",\"showd\":" + get(sb, obj, "#showd", 1)
			+ ",\"showt\":" + get(sb, obj, "#showt", 1)
			+ ",\"shows\":" + get(sb, obj, "#shows", 1)
			+ ",\"dayoff\":" + get(sb, obj, "#dayoff", 0)
			+ ",\"speed\":" + get(sb, obj, "#speed", 1)
			+ ",\"slow\":" + get(sb, obj, "#slowdiv", 1)
			+ ",\"cycle\":" + server.overworld().getGameRules().get(GameRules.ADVANCE_TIME)
			+ ",\"me\":" + player.getTags().contains(ON)
			+ ",\"on\":" + on
			+ ",\"online\":" + players.size()
			+ "}";
	}

	public static String install(MinecraftServer server, Path root) throws IOException {
		if (installed(server)) return ALREADY;
		Path dir = DatapackOps.resolveIn(root, PACK);
		if (dir == null) return INSTALL_FAILED;
		if (!Files.isRegularFile(dir.resolve("pack.mcmeta"))) extract(dir);
		return "";
	}

	private static void extract(Path dir) throws IOException {
		InputStream in = ClockOps.class.getResourceAsStream("/" + BUNDLE);
		if (in == null) in = ClockOps.class.getClassLoader().getResourceAsStream(BUNDLE);
		if (in == null) throw new IOException(BUNDLE);
		byte[] meta = null;
		try (ZipInputStream zip = new ZipInputStream(in)) {
			Files.createDirectories(dir);
			for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
				String name = entry.getName().replace('\\', '/');
				if (entry.isDirectory() || name.endsWith("/")) continue;
				if (name.equals("pack.mcmeta")) {
					meta = zip.readAllBytes();
					continue;
				}
				Path out = DatapackOps.resolveIn(dir, name);
				if (out == null) throw new IOException(name);
				Files.createDirectories(out.getParent());
				Files.write(out, zip.readAllBytes());
			}
		}
		if (meta == null) throw new IOException(BUNDLE);
		Files.write(dir.resolve("pack.mcmeta"), meta);
	}

	public static String apply(MinecraftServer server, ServerPlayer player, String action, String value) {
		Scoreboard sb = server.getScoreboard();
		Objective obj = sb.getObjective(DATA);
		if (obj == null || !installed(server)) return NOT_READY;
		String v = value == null ? "" : value.trim();
		switch (action == null ? "" : action) {
			case "ord" -> {
				int n = number(v);
				if (n < 1 || n > 6) return BAD;
				set(sb, obj, "#ord", n);
			}
			case "showd", "showt", "shows" -> {
				int n = number(v);
				if (n != 0 && n != 1) return BAD;
				String name = "#" + action;
				int others = get(sb, obj, "#showd", 1) + get(sb, obj, "#showt", 1) + get(sb, obj, "#shows", 1) - get(sb, obj, name, 1);
				if (n == 0 && others < 1) return KEEP_ONE;
				set(sb, obj, name, n);
			}
			case "day" -> {
				int n = number(v);
				if (n < 1 || n > MAX_DAY) return BAD;
				if (!run(server, "hud/calc")) return NOT_READY;
				set(sb, obj, "#daytar", n);
				set(sb, obj, "#dayoff", n - get(sb, obj, "#dayraw", 1));
			}
			case "dayadd" -> {
				int n = number(v);
				if (n != 1 && n != -1) return BAD;
				set(sb, obj, "#dayoff", get(sb, obj, "#dayoff", 0) + n);
			}
			case "dayreset" -> set(sb, obj, "#dayoff", 0);
			case "nextday" -> {
				if (!run(server, "set/dawn")) return NOT_READY;
				set(sb, obj, "#dayoff", get(sb, obj, "#dayoff", 0) + 1);
			}
			case "time" -> {
				if (!TIMES.contains(v)) return BAD;
				if (!run(server, "set/" + v)) return NOT_READY;
			}
			case "flow" -> {
				int n = number(v);
				if (n != 0 && n != 1 && n != 2 && n != 4 && n != 8 && n != -2 && n != -4 && n != -8) return BAD;
				server.overworld().getGameRules().set(GameRules.ADVANCE_TIME, n == 1, server);
				set(sb, obj, "#speed", n < 0 ? 1 : n);
				set(sb, obj, "#slowdiv", n < 0 ? -n : 1);
				if (n < 0) set(sb, obj, "#stag", 0);
			}
			case "hud" -> {
				if (!v.equals("on") && !v.equals("off")) return BAD;
				for (ServerPlayer p : server.getPlayerList().getPlayers()) tag(p, v.equals("on"));
			}
			case "me" -> {
				if (!v.equals("on") && !v.equals("off")) return BAD;
				tag(player, v.equals("on"));
			}
			default -> {
				return BAD;
			}
		}
		run(server, "hud/update");
		return "";
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("clock", path);
	}

	private static boolean run(MinecraftServer server, String path) {
		ServerFunctionManager functions = server.getFunctions();
		var function = functions.get(id(path));
		if (function.isEmpty()) return false;
		functions.execute(function.get(), functions.getGameLoopSender());
		return true;
	}

	private static int get(Scoreboard sb, Objective obj, String name, int fallback) {
		if (obj == null) return fallback;
		ReadOnlyScoreInfo info = sb.getPlayerScoreInfo(ScoreHolder.forNameOnly(name), obj);
		return info == null ? fallback : info.value();
	}

	private static void set(Scoreboard sb, Objective obj, String name, int value) {
		sb.getOrCreatePlayerScore(ScoreHolder.forNameOnly(name), obj).set(value);
	}

	private static void tag(ServerPlayer player, boolean on) {
		player.addTag(on ? ON : OFF);
		player.removeTag(on ? OFF : ON);
	}

	private static int number(String text) {
		try {
			return Integer.parseInt(text);
		} catch (NumberFormatException e) {
			return Integer.MIN_VALUE;
		}
	}
}
