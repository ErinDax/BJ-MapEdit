package cn.erindax.bjmapedit;

public final class VillagerTradeLock {
	private static final ThreadLocal<Integer> ALLOW = ThreadLocal.withInitial(() -> 0);

	private VillagerTradeLock() {}

	public static void allowStop() {
		ALLOW.set(ALLOW.get() + 1);
	}

	public static void endAllow() {
		ALLOW.set(Math.max(0, ALLOW.get() - 1));
	}

	public static boolean isAllowed() {
		return ALLOW.get() > 0;
	}
}
