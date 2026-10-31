package cn.erindax.bjmapedit.client.widget;

public final class SignData {
	public static final int LINES = 4;

	public String wood = "";
	public boolean waxed;
	public Side front = new Side();
	public Side back = new Side();

	public Side side(int index) {
		if (index == 1) {
			if (back == null) back = new Side();
			return back;
		}
		if (front == null) front = new Side();
		return front;
	}

	public SignData copy() {
		SignData d = new SignData();
		d.wood = wood == null ? "" : wood;
		d.waxed = waxed;
		d.front = side(0).copy();
		d.back = side(1).copy();
		return d;
	}

	public static final class Side {
		public String color = "black";
		public boolean glow;
		public String[] lines = blank();
		public String[] commands = blank();

		public String line(int i) {
			return lines != null && i >= 0 && i < lines.length && lines[i] != null ? lines[i] : "";
		}

		public String command(int i) {
			return commands != null && i >= 0 && i < commands.length && commands[i] != null ? commands[i] : "";
		}

		public void set(int i, String line, String command) {
			if (lines == null || lines.length < LINES) lines = resize(lines);
			if (commands == null || commands.length < LINES) commands = resize(commands);
			lines[i] = line == null ? "" : line;
			commands[i] = command == null ? "" : command;
		}

		public Side copy() {
			Side s = new Side();
			s.color = color == null ? "black" : color;
			s.glow = glow;
			for (int i = 0; i < LINES; i++) s.set(i, line(i), command(i));
			return s;
		}

		private static String[] resize(String[] old) {
			String[] out = blank();
			if (old != null) {
				for (int i = 0; i < Math.min(old.length, LINES); i++) out[i] = old[i] == null ? "" : old[i];
			}
			return out;
		}
	}

	private static String[] blank() {
		return new String[]{"", "", "", ""};
	}
}
