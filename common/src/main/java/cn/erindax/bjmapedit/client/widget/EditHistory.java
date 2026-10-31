package cn.erindax.bjmapedit.client.widget;

import net.minecraft.util.Util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

public final class EditHistory<T> {

	private static final int LIMIT = 100;
	private static final long MERGE_MS = 1000L;

	private final Deque<T> undo = new ArrayDeque<>();
	private final Deque<T> redo = new ArrayDeque<>();
	private String lastKind;
	private long lastTime;

	public void record(T before, String kind) {
		long now = Util.getMillis();
		boolean merge = kind != null && kind.equals(lastKind) && now - lastTime < MERGE_MS && !undo.isEmpty();
		lastKind = kind;
		lastTime = now;
		redo.clear();
		if (merge) return;
		undo.push(before);
		while (undo.size() > LIMIT) undo.removeLast();
	}

	public T undo(T current) {
		lastKind = null;
		while (!undo.isEmpty()) {
			T prev = undo.pop();
			if (Objects.equals(prev, current)) continue;
			redo.push(current);
			return prev;
		}
		return null;
	}

	public T redo(T current) {
		lastKind = null;
		while (!redo.isEmpty()) {
			T next = redo.pop();
			if (Objects.equals(next, current)) continue;
			undo.push(current);
			return next;
		}
		return null;
	}

	public void cut() {
		lastKind = null;
	}

	public void clear() {
		undo.clear();
		redo.clear();
		lastKind = null;
	}
}
