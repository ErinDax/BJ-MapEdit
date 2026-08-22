package cn.erindax.bjmapedit.client.screen;

import cn.erindax.bjmapedit.client.widget.RenameDeleteMenu;
import cn.erindax.bjmapedit.client.widget.UiTheme;
import cn.erindax.bjmapedit.client.DatapackNet;
import cn.erindax.bjmapedit.client.EditorAccess;
import cn.erindax.bjmapedit.networking.DatapackOps;
import cn.erindax.bjmapedit.networking.payload.DatapackOpResultPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

@Environment(EnvType.CLIENT)
public class DatapackEditorScreen extends Screen {

	private static final int LINE_NUM_W = 28;
	private static final int LINE_H = 10;
	private static final int FOOTER_H = 30;
	private static final int CMD_YELLOW = 0xFFFF55;
	private static final int CUSTOM_GREEN = 0x55FF55;
	private static final int BRACE_PURPLE = 0xFF55FF;
	private static final int BRACKET_BLUE = 0x5555FF;
	private static final int QUOTE_LIGHT_BLUE = 0x55FFFF;
	private static final int DEFAULT_WHITE = 0xE0E0E0;
	private static final int LINE_NUM_GRAY = 0x707070;
	private static final int KEYWORD_PURPLE = 0xFF55FF;
	private static final int NUMBER_GREEN = 0x55FF55;
	private static final int STRING_BLUE = 0x55AAFF;

	private Path datapacksPath;
	private boolean remoteMode;
	private Set<String> pendingExpanded;
	private String pendingSelectRel;
	private boolean pendingApplyReload;
	private boolean selectAfterOk;
	private boolean keepRemoteCursor;
	private List<FileNode> rootNodes = new ArrayList<>();
	private FileNode selectedNode;
	private FileNode editingFileNameNode;
	private EditBox fileNameEditBox;
	private final RenameDeleteMenu treeMenu = new RenameDeleteMenu();
	private FileNode treeMenuNode;
	private int ignoreTreeLeftUntil;

	private List<String> editorLines = new ArrayList<>();
	private int cursorLine, cursorCol;
	private int scrollLines;
	private int hScroll;
	private int selStartLine = -1, selStartCol = -1;
	private int tickCount;

	private boolean dirty;
	private String currentFilePath;

	private EditBox commandInput;
	private CommandSuggestions commandSuggestions;
	private Button saveBtn, newFileBtn, newFolderBtn, newDatapackBtn;

	private int WIDTH = 500;
	private int HEIGHT = 400;
	private int CONTENT_TOP = 30;
	private int CONTENT_BOTTOM;
	private int leftPos;
	private int topPos;
	private int templateRailX = 8;
	private int templateRailW = 148;
	private int leftPanelX, leftPanelY, leftPanelW, leftPanelH;
	private int editorX, editorY, editorW, editorH;
	private int visibleLines;
	private int refreshX, refreshY;
	private int editorMaxScroll;
	private int editorMaxHScroll;
	private boolean draggingTreeScroll;
	private boolean draggingEditorScroll;
	private boolean draggingEditorHScroll;
	private double treeScrollGrab;
	private double editorScrollGrab;
	private double editorHScrollGrab;
	private boolean opened;
	private final TreeDrag treeDrag = new TreeDrag();

	private List<FileNode> flatTree = new ArrayList<>();
	private static final int TREE_ITEM_H = 18;
	private static final int TREE_ICON = 8;
	private int treeScroll;
	private int treeMaxScroll;
	private static Session session;

	public DatapackEditorScreen() {
		super(Component.translatable("screen.bj_mapedit.datapack_title"));
	}

	private boolean resolveDatapacksPath() {
		Minecraft mc = Minecraft.getInstance();
		IntegratedServer server = mc.getSingleplayerServer();
		if (server != null) {
			remoteMode = false;
			datapacksPath = server.getWorldPath(LevelResource.DATAPACK_DIR);
			return Files.isDirectory(datapacksPath);
		}
		if (!EditorAccess.canOpen()) return false;
		remoteMode = true;
		datapacksPath = DatapackNet.virtualRoot();
		return true;
	}

	@Override
	protected void init() {
		boolean first = !opened;
		opened = true;
		super.init();
		this.clearWidgets();

		if (!resolveDatapacksPath()) {
			Minecraft.getInstance().setScreen(null);
			return;
		}
		if (remoteMode && !EditorAccess.canSendDatapack()) {
			EditorAccess.needMod();
		}

		layoutPanel();

		commandInput = new EditBox(font, 0, 0, 200, 12, Component.empty());
		commandInput.setMaxLength(32500);
		commandInput.setBordered(false);
		commandInput.setTextColor(0xFFFFFF);
		commandInput.setVisible(false);
		commandSuggestions = new CommandSuggestions(this.minecraft, this, commandInput, font,
			false, false, 1, 10, false, 0xD0000000);
		commandSuggestions.setAllowSuggestions(true);
		commandSuggestions.setAllowHiding(false);
		commandInput.setResponder(s -> {
			try { commandSuggestions.updateCommandInfo(); } catch (Exception ignored) {}
		});

		saveBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.apply_now"), b -> saveCurrentFile(true)
		).bounds(0, 0, 70, 20).build());
		newFileBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.new_file"), b -> createNewFile(false)
		).bounds(0, 0, 70, 20).build());
		newFolderBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.new_folder"), b -> createNewFile(true)
		).bounds(0, 0, 70, 20).build());
		newDatapackBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("screen.bj_mapedit.new_datapack"), b -> createNewDatapack()
		).bounds(0, 0, 70, 20).build());

		fileNameEditBox = new EditBox(font, 0, -1000, 120, 16, Component.empty());
		fileNameEditBox.setMaxLength(128);
		fileNameEditBox.setBordered(false);
		fileNameEditBox.setTextColor(UiTheme.cInputText());
		fileNameEditBox.setVisible(false);
		this.addRenderableWidget(fileNameEditBox);

		Set<String> keepExpanded = first && session != null ? session.expanded : collectExpanded();
		String keepSelected = first && session != null ? session.selectedPath
			: (selectedNode != null ? pathKey(selectedNode.path) : null);
		loadFileTree(keepExpanded);
		if (keepSelected != null) {
			if (remoteMode) pendingSelectRel = DatapackNet.rel(datapacksPath, Path.of(keepSelected));
			else selectNodeByPath(Path.of(keepSelected));
		}

		if (first && session != null) {
			restoreSession(session);
		} else if (editorLines.isEmpty()) {
			editorLines.add("");
		}

		layoutButtons();
		setFocused(null);
		refreshCommandSuggestions();
	}

	private void layoutPanel() {
		int margin = 8;
		int gap = 8;
		int availW = Math.max(32, this.width - margin * 2);
		int availH = Math.max(32, this.height - margin * 2);
		templateRailW = Mth.clamp(availW * 20 / 100, 112, 220);
		if (availW < 420) {
			templateRailW = Mth.clamp(availW * 26 / 100, 88, 140);
		}
		WIDTH = Math.max(8, availW - templateRailW - gap);
		HEIGHT = availH;
		templateRailX = margin;
		leftPos = margin + templateRailW + gap;
		topPos = margin;
		CONTENT_TOP = UiTheme.HEADER_H + 8;
		CONTENT_BOTTOM = HEIGHT - FOOTER_H - 10;
		leftPanelX = templateRailX;
		leftPanelY = treeListTop();
		leftPanelW = templateRailW;
		leftPanelH = treeListH();
		editorX = leftPos + 8;
		editorY = topPos + CONTENT_TOP + 14;
		editorW = Math.max(8, WIDTH - 16);
		editorH = Math.max(8, topPos + CONTENT_BOTTOM - editorY);
		visibleLines = Math.max(1, editorH / LINE_H);
		updateTreeScroll();
		updateEditorScroll();
		layoutRefreshIcon();
	}

	private void layoutRefreshIcon() {
		refreshX = UiTheme.headerIconX(leftPos, WIDTH);
		refreshY = UiTheme.headerIconY(topPos);
	}

	private void layoutButtons() {
		if (saveBtn == null) return;
		int pad = 8;
		int gap = 6;
		int btnH = 20;
		int footerY = topPos + HEIGHT - FOOTER_H + (FOOTER_H - btnH) / 2;
		int saveW = Math.max(72, font.width(saveBtn.getMessage()) + 22);
		int packW = Math.max(72, font.width(newDatapackBtn.getMessage()) + 16);
		int fileW = Math.max(56, font.width(newFileBtn.getMessage()) + 16);
		int folderW = Math.max(56, font.width(newFolderBtn.getMessage()) + 16);
		int need = packW + fileW + folderW + saveW + pad * 2 + gap * 3;
		if (need > WIDTH) {
			int shrink = need - WIDTH;
			int share = Math.max(0, shrink / 3);
			packW = Math.max(52, packW - share);
			fileW = Math.max(44, fileW - share);
			folderW = Math.max(44, folderW - share);
		}
		newDatapackBtn.setPosition(leftPos + pad, footerY);
		newDatapackBtn.setWidth(packW);
		newDatapackBtn.setHeight(btnH);
		newFileBtn.setPosition(newDatapackBtn.getX() + packW + gap, footerY);
		newFileBtn.setWidth(fileW);
		newFileBtn.setHeight(btnH);
		newFolderBtn.setPosition(newFileBtn.getX() + fileW + gap, footerY);
		newFolderBtn.setWidth(folderW);
		newFolderBtn.setHeight(btnH);
		saveBtn.setPosition(leftPos + WIDTH - pad - saveW, footerY);
		saveBtn.setWidth(saveW);
		saveBtn.setHeight(btnH);
		syncRenameBox();
	}

	private void resetFields() {
		if (currentFilePath == null && selectedNode != null && !selectedNode.isDirectory) {
			loadSelectedFile();
			return;
		}
		if (currentFilePath == null) return;
		reloadCurrentFile();
		clampCursor();
		updateEditorScroll();
		refreshCommandSuggestions();
	}

	private void loadFileTree() {
		loadFileTree(collectExpanded());
	}

	private void loadFileTree(Set<String> expanded) {
		if (remoteMode) {
			pendingExpanded = expanded;
			DatapackNet.send(DatapackOps.LIST, "", "");
			return;
		}
		rootNodes.clear();
		if (datapacksPath == null) return;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(datapacksPath)) {
			Set<String> seen = new HashSet<>();
			for (Path p : stream) {
				String key = p.toAbsolutePath().normalize().toString().toLowerCase(Locale.ROOT);
				if (!seen.add(key)) continue;
				FileNode node = buildTree(p, 0);
				if (node != null) rootNodes.add(node);
			}
		} catch (IOException ignored) {}
		rootNodes.sort(Comparator.comparing((FileNode n) -> !n.isDirectory).thenComparing(n -> n.name));
		if (expanded != null && !expanded.isEmpty()) {
			applyExpanded(rootNodes, expanded);
		}
		flattenTree();
	}

	private FileNode buildTree(Path path, int depth) {
		try {
			BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
			FileNode node = new FileNode();
			node.name = path.getFileName().toString();
			node.path = path;
			node.depth = depth;
			node.isDirectory = attrs.isDirectory();
			if (node.isDirectory && depth < 10) {
				try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
					Set<String> seen = new HashSet<>();
					for (Path child : stream) {
						String key = child.toAbsolutePath().normalize().toString().toLowerCase(Locale.ROOT);
						if (!seen.add(key)) continue;
						FileNode childNode = buildTree(child, depth + 1);
						if (childNode != null) node.children.add(childNode);
					}
				}
				node.children.sort(Comparator.comparing((FileNode n) -> !n.isDirectory).thenComparing(n -> n.name));
			}
			return node;
		} catch (IOException e) {
			return null;
		}
	}

	private void flattenTree() {
		flatTree.clear();
		for (FileNode root : rootNodes) {
			flattenNode(root);
		}
		updateTreeScroll();
	}

	private void flattenNode(FileNode node) {
		flatTree.add(node);
		if (node.expanded) {
			for (FileNode child : node.children) {
				flattenNode(child);
			}
		}
	}

	private FileNode findNodeAt(int idx) {
		if (idx >= 0 && idx < flatTree.size()) return flatTree.get(idx);
		return null;
	}

	private int findNodeIndex(FileNode node) {
		return flatTree.indexOf(node);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.renderBackground(g, mouseX, mouseY, partialTick);
		UiTheme.drawPanel(g, leftPos, topPos, WIDTH, HEIGHT);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(g, mouseX, mouseY, partialTick);
		UiTheme.drawHeader(g, font, this.title, leftPos, topPos, WIDTH);
		layoutRefreshIcon();
		boolean refreshHover = UiTheme.hitIcon(refreshX, refreshY, mouseX, mouseY);
		UiTheme.drawRefreshIcon(g, refreshX, refreshY, refreshHover);
		drawFileRail(g, mouseX, mouseY);

		int fy = topPos + HEIGHT - FOOTER_H;
		g.fill(leftPos + 1, fy, leftPos + WIDTH - 1, topPos + HEIGHT - 1, 0xFF191A1B);
		g.fill(leftPos + 1, fy, leftPos + WIDTH - 1, fy + 1, 0xFF2B2C2D);

		if (currentFilePath != null) {
			UiTheme.clipLabel(g, font, relativize(currentFilePath), editorX, topPos + CONTENT_TOP,
				editorW, UiTheme.cMuted());
		}

		g.fill(editorX, editorY, editorX + editorW, editorY + editorH, UiTheme.cInputBg());
		renderEditor(g, mouseX, mouseY);
		UiTheme.drawThinScrollBar(g, editorScrollBarX(), editorY, editorH, scrollLines, editorMaxScroll);
		UiTheme.drawThinHScrollBar(g, editorHBarX(), editorHBarY(), editorHBarW(), hScroll, editorMaxHScroll);

		if (newDatapackBtn != null) newDatapackBtn.render(g, mouseX, mouseY, partialTick);
		if (newFileBtn != null) newFileBtn.render(g, mouseX, mouseY, partialTick);
		if (newFolderBtn != null) newFolderBtn.render(g, mouseX, mouseY, partialTick);
		if (saveBtn != null) saveBtn.render(g, mouseX, mouseY, partialTick);
		if (fileNameEditBox != null && fileNameEditBox.isVisible()) {
			fileNameEditBox.render(g, mouseX, mouseY, partialTick);
		}
		if (commandSuggestions != null && canSuggestCommands()) {
			UiTheme.pushOverlay(g);
			g.enableScissor(editorX, editorY, editorX + editorW, editorY + editorH);
			commandSuggestions.render(g, mouseX, mouseY);
			g.disableScissor();
			UiTheme.popOverlay(g);
		}
		drawTreeMenu(g, mouseX, mouseY);
		if (refreshHover) {
			g.renderTooltip(font, Component.translatable("screen.bj_mapedit.reload_from_disk"), mouseX, mouseY);
		}
	}

	private String relativize(String fullPath) {
		try {
			Path p = Path.of(fullPath);
			Path rel = datapacksPath.toAbsolutePath().relativize(p.toAbsolutePath());
			return rel.toString();
		} catch (Exception e) {
			return fullPath;
		}
	}

	private void drawFileRail(GuiGraphics g, int mouseX, int mouseY) {
		int hx = mouseX;
		int hy = mouseY;
		if (treeMenu.covers(mouseX, mouseY)) {
			hx = -10000;
			hy = -10000;
		}
		int x = templateRailX;
		int w = templateRailW;
		updateTreeScroll();
		UiTheme.drawPanel(g, x, topPos, w, HEIGHT);
		g.fill(x + 1, topPos + 1, x + w - 1, topPos + UiTheme.HEADER_H, UiTheme.cHeaderBg());
		UiTheme.clipLabel(g, font, Component.translatable("screen.bj_mapedit.datapack_files").getString(),
			x + 8, topPos + 7, Math.max(16, w - 16), UiTheme.cHeaderTitle());

		if (flatTree.isEmpty()) {
			UiTheme.muted(g, font, Component.translatable("screen.bj_mapedit.datapack_empty").getString(),
				x + 8, treeListTop() + 4);
			return;
		}

		int listTop = treeListTop();
		int listBottom = treeListBottom();
		g.enableScissor(x + 1, listTop, x + w - 1, listBottom);
		for (int i = 0; i < flatTree.size(); i++) {
			FileNode node = flatTree.get(i);
			int itemY = treeRowY(i);
			if (itemY + TREE_ITEM_H < listTop || itemY > listBottom) continue;
			boolean dragging = treeDrag.active && treeDrag.src == node;
			boolean drop = treeDrag.active && treeDrag.hoverHighlight == node;
			boolean sel = node == selectedNode;
			boolean hover = !treeDrag.active && hx >= x + 1 && hx < x + w - 1 && hy >= itemY && hy < itemY + TREE_ITEM_H
				&& hy >= listTop && hy < listBottom;
			if (drop) {
				g.fill(x + 1, itemY, x + w - 1, itemY + TREE_ITEM_H, 0xFF3A3420);
			} else if (sel) {
				g.fill(x + 1, itemY, x + w - 1, itemY + TREE_ITEM_H, 0xFF21262D);
			} else if (hover) {
				g.fill(x + 1, itemY, x + w - 1, itemY + TREE_ITEM_H, 0xFF191A1B);
			}

			int indent = 6 + node.depth * 10;
			int iconX = x + indent;
			int iconY = itemY + (TREE_ITEM_H - TREE_ICON) / 2;
			boolean hot = drop || sel || hover;
			drawTreeIcon(g, node, iconX, iconY, hot && !dragging);
			int nameX = iconX + TREE_ICON + 3;
			int nameW = Math.max(16, treeActionRight() - nameX - 4);
			int nameY = itemY + 5;
			if (editingFileNameNode == node && fileNameEditBox != null && fileNameEditBox.isVisible()) {
				fileNameEditBox.render(g, mouseX, mouseY, 0);
			} else {
				int color = dragging ? 0xFF6E7681 : (hot ? 0xFFE6EDF3 : UiTheme.cListText());
				UiTheme.clipLabel(g, font, node.name, nameX, nameY, nameW, color);
			}
		}
		g.disableScissor();
		UiTheme.drawThinScrollBar(g, treeScrollBarX(), treeListTop(), treeListH(), treeScroll, treeMaxScroll);
		if (treeDrag.active) {
			if (treeDrag.hoverDest != null && samePath(treeDrag.hoverDest, datapacksPath)) {
				g.fill(x + 1, topPos + 1, x + w - 1, topPos + UiTheme.HEADER_H, 0x662D333B);
			}
			if (treeDrag.src != null) {
				int gx = mouseX + 10;
				int gy = mouseY - 6;
				int gw = Math.min(132, font.width(treeDrag.src.name) + 8);
				g.fill(gx - 3, gy - 3, gx + gw, gy + 13, 0xF021262D);
				UiTheme.clipLabel(g, font, treeDrag.src.name, gx, gy, gw - 4, 0xFFE6EDF3);
			}
		}
	}

	private void renderEditor(GuiGraphics g, int mouseX, int mouseY) {
		int gutterRight = editorX + LINE_NUM_W;
		int ex = editorTextLeft() - hScroll;
		int ey = editorY + 2;

		int startLine = scrollLines;
		int endLine = Math.min(startLine + visibleLines, editorLines.size());

		g.enableScissor(editorX, editorY, gutterRight, editorY + editorH);
		for (int i = startLine; i < endLine; i++) {
			int drawY = ey + (i - startLine) * LINE_H;
			String lineNumStr = String.valueOf(i + 1);
			int lnX = gutterRight - font.width(lineNumStr) - 2;
			g.drawString(font, lineNumStr, lnX, drawY, LINE_NUM_GRAY);
		}
		g.disableScissor();

		g.enableScissor(gutterRight, editorY, editorX + editorW, editorY + editorH);
		for (int i = startLine; i < endLine; i++) {
			int drawY = ey + (i - startLine) * LINE_H;
			renderHighlightedLine(g, editorLines.get(i), ex, drawY);
		}
		renderCursorAndSelection(g, ex, ey, startLine, endLine);
		g.disableScissor();
	}

	private void renderHighlightedLine(GuiGraphics g, String rawLine, int x, int y) {
		if (rawLine.isEmpty()) return;

		if (isCommandSourceFile()) {
			renderMcFunctionLine(g, rawLine, x, y);
		} else {
			renderJsonLikeLine(g, rawLine, x, y);
		}
	}

	private void renderMcFunctionLine(GuiGraphics g, String rawLine, int x, int y) {
		if (rawLine.startsWith("#")) {
			g.drawString(font, rawLine, x, y, 0xFF808080);
			return;
		}

		int firstSpace = rawLine.indexOf(' ');
		String cmdName = firstSpace > 0 ? rawLine.substring(0, firstSpace) : rawLine.trim();
		if (cmdName.startsWith("/")) cmdName = cmdName.substring(1);
		boolean validCmd = isValidMcCommand(cmdName);
		int cmdColor = validCmd ? CMD_YELLOW : 0xFFFF5555;
		if (!validCmd) {
			g.drawString(font, rawLine, x, y, cmdColor);
			return;
		}

		if (firstSpace <= 0) {
			g.drawString(font, rawLine, x, y, cmdColor);
			return;
		}

		int jsonStart = findFirstJsonChar(rawLine, firstSpace);
		if (jsonStart < 0) {
			g.drawString(font, rawLine, x, y, cmdColor);
			return;
		}

		String cmdPart = rawLine.substring(0, jsonStart);
		int cx = x;
		g.drawString(font, cmdPart, cx, y, cmdColor);
		cx += font.width(cmdPart);

		String jsonPart = rawLine.substring(jsonStart);
		renderMcFunctionJsonHighlight(g, jsonPart, cx, y);
	}

	private int findFirstJsonChar(String line, int fromIndex) {
		for (int i = fromIndex; i < line.length(); i++) {
			char c = line.charAt(i);
			if (c == '{' || c == '[') return i;
		}
		return -1;
	}

	private void renderMcFunctionJsonHighlight(GuiGraphics g, String text, int x, int y) {
		int cx = x;
		int i = 0;
		while (i < text.length()) {
			char c = text.charAt(i);
			if (c == '{') {
				g.drawString(font, "{", cx, y, BRACE_PURPLE);
				cx += font.width("{");
				i++;
			} else if (c == '}') {
				g.drawString(font, "}", cx, y, BRACE_PURPLE);
				cx += font.width("}");
				i++;
			} else if (c == '[') {
				g.drawString(font, "[", cx, y, BRACKET_BLUE);
				cx += font.width("[");
				i++;
			} else if (c == ']') {
				g.drawString(font, "]", cx, y, BRACKET_BLUE);
				cx += font.width("]");
				i++;
			} else if (c == '"') {
				int end = text.indexOf('"', i + 1);
				String quoted;
				if (end < 0) {
					quoted = text.substring(i);
					i = text.length();
				} else {
					quoted = text.substring(i, end + 1);
					i = end + 1;
				}
				g.drawString(font, quoted, cx, y, QUOTE_LIGHT_BLUE);
				cx += font.width(quoted);
			} else if (c == ' ' || c == '\t') {
				g.drawString(font, String.valueOf(c), cx, y, CUSTOM_GREEN);
				cx += font.width(String.valueOf(c));
				i++;
			} else {
				int end = i + 1;
				while (end < text.length()) {
					char nc = text.charAt(end);
					if (nc == '{' || nc == '}' || nc == '[' || nc == ']' || nc == '"' || nc == ' ' || nc == '\t') break;
					end++;
				}
				String segment = text.substring(i, end);
				g.drawString(font, segment, cx, y, CUSTOM_GREEN);
				cx += font.width(segment);
				i = end;
			}
		}
	}

	private void renderJsonLikeLine(GuiGraphics g, String rawLine, int x, int y) {
		int cx = x;
		int i = 0;
		while (i < rawLine.length()) {
			char c = rawLine.charAt(i);
			if (c == '{') {
				g.drawString(font, "{", cx, y, BRACE_PURPLE);
				cx += font.width("{");
				i++;
			} else if (c == '}') {
				g.drawString(font, "}", cx, y, BRACE_PURPLE);
				cx += font.width("}");
				i++;
			} else if (c == '[') {
				g.drawString(font, "[", cx, y, BRACKET_BLUE);
				cx += font.width("[");
				i++;
			} else if (c == ']') {
				g.drawString(font, "]", cx, y, BRACKET_BLUE);
				cx += font.width("]");
				i++;
			} else if (c == '"') {
				int end = findStringEnd(rawLine, i);
				if (end < 0) end = rawLine.length();
				String quoted = rawLine.substring(i, Math.min(end + 1, rawLine.length()));
				g.drawString(font, quoted, cx, y, STRING_BLUE);
				cx += font.width(quoted);
				i = end + 1;
			} else {
				int end = i + 1;
				while (end < rawLine.length()) {
					char nc = rawLine.charAt(end);
					if (nc == '{' || nc == '}' || nc == '[' || nc == ']' || nc == '"' || nc == ':' || nc == ',') break;
					end++;
				}
				String segment = rawLine.substring(i, end);
				int color = DEFAULT_WHITE;
				if (segment.equals("true") || segment.equals("false") || segment.equals("null")) {
					color = KEYWORD_PURPLE;
				} else if (segment.matches("-?\\d+\\.?\\d*") || segment.matches("-?\\d+")) {
					color = NUMBER_GREEN;
				}
				g.drawString(font, segment, cx, y, color);
				cx += font.width(segment);
				i = end;
			}
		}
	}

	private int findStringEnd(String text, int start) {
		for (int i = start + 1; i < text.length(); i++) {
			if (text.charAt(i) == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
				return i;
			}
		}
		return -1;
	}

	private void renderCursorAndSelection(GuiGraphics g, int ex, int ey, int startLine, int endLine) {
		for (int lineIdx = startLine; lineIdx < endLine; lineIdx++) {
			int drawY = ey + (lineIdx - startLine) * LINE_H;
			String rawLine = lineIdx < editorLines.size() ? editorLines.get(lineIdx) : "";

			int selStartL, selStartC, selEndL2, selEndC2;
			if (selStartLine >= 0) {
				if (isBefore(selStartLine, selStartCol, cursorLine, cursorCol)) {
					selStartL = selStartLine; selStartC = selStartCol;
					selEndL2 = cursorLine; selEndC2 = cursorCol;
				} else {
					selStartL = cursorLine; selStartC = cursorCol;
					selEndL2 = selStartLine; selEndC2 = selStartCol;
				}
			} else {
				selStartL = cursorLine; selStartC = cursorCol;
				selEndL2 = cursorLine; selEndC2 = cursorCol;
			}

			if (lineIdx >= selStartL && lineIdx <= selEndL2) {
				int sx = ex;
				if (lineIdx == selStartL) sx += visibleColWidth(rawLine, selStartC);
				int sEnd = ex;
				if (lineIdx == selEndL2) {
					sEnd = ex + visibleColWidth(rawLine, selEndC2);
				} else {
					sEnd = ex + visibleColWidth(rawLine, rawLine.length());
				}
				if (sEnd > sx) {
					g.fill(sx, drawY, sEnd, drawY + LINE_H, 0x604488FF);
				}
			}
		}

		if (tickCount % 20 < 10 && cursorLine >= startLine && cursorLine < endLine) {
			int cY = ey + (cursorLine - startLine) * LINE_H;
			String cursorLineText = cursorLine < editorLines.size() ? editorLines.get(cursorLine) : "";
			int cX = ex + visibleColWidth(cursorLineText, cursorCol);
			g.fill(cX, cY, cX + 1, cY + LINE_H, 0xFFFFFFFF);
		}
	}

	private boolean isBefore(int l1, int c1, int l2, int c2) {
		return l1 < l2 || (l1 == l2 && c1 <= c2);
	}

	private int visibleColWidth(String rawLine, int col) {
		if (col <= 0) return 0;
		if (col >= rawLine.length()) {
			return font.width(rawLine);
		}
		String sub = rawLine.substring(0, col);
		return font.width(sub);
	}

	private int colFromX(String rawLine, int targetX) {
		int bestCol = 0;
		int bestDist = Math.abs(targetX);
		for (int c = 0; c <= rawLine.length(); c++) {
			int w = font.width(rawLine.substring(0, c));
			int dist = Math.abs(targetX - w);
			if (dist < bestDist) {
				bestDist = dist;
				bestCol = c;
			}
		}
		return bestCol;
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (commandSuggestions != null && commandSuggestions.isVisible()
			&& commandSuggestions.mouseClicked(mx, my, button)) {
			applyCommandInputToLine();
			refreshCommandSuggestions();
			return true;
		}

		if (button == 0 && ignoreTreeLeftUntil > 0 && tickCount <= ignoreTreeLeftUntil
			&& (treeMenu.isOpen() || inFileRail(mx, my))) {
			treeDrag.reset();
			return true;
		}
		if (button == 0 && UiTheme.consumeRefreshClick(refreshX, refreshY, mx, my, this::resetFields)) {
			return true;
		}
		if (handleTreeMenuClick(mx, my, button)) {
			return true;
		}
		if (button == 0) {
			UiTheme.ScrollClick tsc = UiTheme.clickBar(treeScrollBarX(), treeListTop(), treeListH(),
				treeScroll, treeMaxScroll, mx, my);
			if (tsc != null) {
				draggingTreeScroll = true;
				treeScrollGrab = tsc.grab;
				if (tsc.scroll != treeScroll) {
					treeScroll = tsc.scroll;
					syncRenameBox();
				}
				return true;
			}
			UiTheme.ScrollClick esc = UiTheme.clickBar(editorScrollBarX(), editorY, editorH,
				scrollLines, editorMaxScroll, mx, my);
			if (esc != null) {
				draggingEditorScroll = true;
				editorScrollGrab = esc.grab;
				if (esc.scroll != scrollLines) {
					scrollLines = esc.scroll;
					updateEditorScroll();
				}
				return true;
			}
			UiTheme.ScrollClick hsc = UiTheme.clickHBar(editorHBarX(), editorHBarY(), editorHBarW(),
				hScroll, editorMaxHScroll, mx, my);
			if (hsc != null) {
				draggingEditorHScroll = true;
				editorHScrollGrab = hsc.grab;
				if (hsc.scroll != hScroll) {
					hScroll = hsc.scroll;
					updateEditorScroll();
				}
				return true;
			}
		}
		if (handleFileTreeClick((int) mx, (int) my, button)) {
			return true;
		}
		if (editingFileNameNode != null
			&& (fileNameEditBox == null || !fileNameEditBox.isMouseOver(mx, my))) {
			finishRename();
		}

		if (super.mouseClicked(mx, my, button)) return true;

		if (mx >= editorX && mx <= editorX + editorW && my >= editorY && my < editorY + editorH) {
			setFocused(null);
			handleEditorClick((int) mx, (int) my, button);
			refreshCommandSuggestions();
			return true;
		}
		return false;
	}

	private boolean handleFileTreeClick(int mX, int mY, int button) {
		if (!inFileRail(mX, mY)) {
			return false;
		}
		if (UiTheme.hitScrollBar(treeScrollBarX(), treeListTop(), treeListH(), mX, mY)) {
			return false;
		}
		if (mY >= topPos && mY < topPos + UiTheme.HEADER_H) {
			if (editingFileNameNode != null) finishRename();
			return true;
		}
		if (editingFileNameNode != null && fileNameEditBox != null && fileNameEditBox.isVisible()
			&& fileNameEditBox.isMouseOver(mX, mY)) {
			return false;
		}
		int idx = getTreeItemAt(mY);
		if (idx < 0) {
			if (editingFileNameNode != null) finishRename();
			return true;
		}
		FileNode node = findNodeAt(idx);
		if (node == null) return true;
		if (editingFileNameNode != null && editingFileNameNode != node) {
			finishRename();
		}
		if (button == 1) {
			treeDrag.reset();
			selectedNode = node;
			openTreeMenu(mX, mY, node);
			ignoreTreeLeftUntil = tickCount + 6;
			return true;
		}
		if (button == 0) {
			treeDrag.press(node, mX, mY);
			if (node.isDirectory) {
				selectedNode = node;
			} else {
				if (selectedNode != node) {
					if (dirty) saveCurrentFile();
				}
				selectedNode = node;
				loadSelectedFile();
			}
		}
		return true;
	}

	private void openTreeMenu(int mx, int my, FileNode node) {
		if (editingFileNameNode != null) cancelRename();
		treeMenuNode = node;
		treeMenu.show(font, mx, my, this.width, this.height);
	}

	private void closeTreeMenu() {
		treeMenu.close();
		treeMenuNode = null;
	}

	private boolean handleTreeMenuClick(double mx, double my, int button) {
		if (button != 0) {
			return treeMenu.isOpen() && treeMenu.covers(mx, my);
		}
		RenameDeleteMenu.Action a = treeMenu.pick(mx, my);
		FileNode node = treeMenuNode;
		if (a == RenameDeleteMenu.Action.RENAME && node != null) {
			treeMenuNode = null;
			selectedNode = node;
			startRename();
			return true;
		}
		if (a == RenameDeleteMenu.Action.DELETE && node != null) {
			treeMenuNode = null;
			selectedNode = node;
			deleteSelected();
			return true;
		}
		if (a == RenameDeleteMenu.Action.DISMISS) treeMenuNode = null;
		return false;
	}

	private void drawTreeMenu(GuiGraphics g, int mouseX, int mouseY) {
		treeMenu.draw(g, font, mouseX, mouseY);
	}

	private int getTreeItemAt(int mY) {
		if (mY < treeListTop() || mY >= treeListBottom()) return -1;
		int idx = (mY - treeListTop() + treeScroll) / TREE_ITEM_H;
		if (idx >= 0 && idx < flatTree.size()) return idx;
		return -1;
	}

	private void handleEditorClick(int mX, int mY, int button) {
		int ey = editorY + 2;
		int relY = mY - ey;
		int clickedLine = scrollLines + relY / LINE_H;

		if (clickedLine < 0) clickedLine = 0;
		if (clickedLine >= editorLines.size()) clickedLine = Math.max(0, editorLines.size() - 1);

		String lineText = editorLines.isEmpty() ? "" : editorLines.get(clickedLine);
		int clickedCol = colFromX(lineText, mX - editorTextLeft() + hScroll);

		if (button == 0) {
			if (hasShiftDown()) {
				if (selStartLine < 0) {
					selStartLine = cursorLine;
					selStartCol = cursorCol;
				}
				cursorLine = clickedLine;
				cursorCol = clickedCol;
			} else {
				cursorLine = clickedLine;
				cursorCol = clickedCol;
				selStartLine = -1;
				selStartCol = -1;
			}
		} else if (button == 1) {
			if (selStartLine < 0) {
				selStartLine = cursorLine;
				selStartCol = cursorCol;
			}
			cursorLine = clickedLine;
			cursorCol = clickedCol;
		}
		clampCursor();
	}

	@Override
	public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
		if (draggingTreeScroll && button == 0) {
			int next = UiTheme.scrollAtGrab(treeListTop(), treeListH(), treeMaxScroll, my, treeScrollGrab);
			if (next != treeScroll) {
				treeScroll = next;
				syncRenameBox();
			}
			return true;
		}
		if (draggingEditorScroll && button == 0) {
			int next = UiTheme.scrollAtGrab(editorY, editorH, editorMaxScroll, my, editorScrollGrab);
			if (next != scrollLines) {
				scrollLines = next;
				updateEditorScroll();
			}
			return true;
		}
		if (draggingEditorHScroll && button == 0) {
			int next = UiTheme.scrollAtGrab(editorHBarX(), editorHBarW(), editorMaxHScroll, mx, editorHScrollGrab);
			if (next != hScroll) {
				hScroll = next;
				updateEditorScroll();
			}
			return true;
		}
		if (button == 0 && updateTreeDrag(mx, my)) return true;
		if (super.mouseDragged(mx, my, button, dx, dy)) return true;
		if (button == 0 && mx >= editorX && mx <= editorX + editorW && my >= editorY && my < editorY + editorH) {
			int ey = editorY + 2;
			int clickedLine = scrollLines + ((int) my - ey) / LINE_H;
			clickedLine = Math.max(0, Math.min(clickedLine, editorLines.size() - 1));
			String lineText = editorLines.isEmpty() ? "" : editorLines.get(clickedLine);
			int clickedCol = colFromX(lineText, (int) mx - editorTextLeft() + hScroll);
			if (selStartLine < 0) {
				selStartLine = cursorLine;
				selStartCol = cursorCol;
			}
			cursorLine = clickedLine;
			cursorCol = clickedCol;
			clampCursor();
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseReleased(double mx, double my, int button) {
		if (button != 0) {
			treeDrag.reset();
			return super.mouseReleased(mx, my, button);
		}
		boolean moved = finishTreeDrag();
		draggingTreeScroll = false;
		draggingEditorScroll = false;
		draggingEditorHScroll = false;
		if (moved) return true;
		return super.mouseReleased(mx, my, button);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
		if (commandSuggestions != null && commandSuggestions.isVisible()
			&& commandSuggestions.mouseScrolled(scrollY)) {
			return true;
		}
		if (inFileRail(mx, my)) {
			treeScroll = Mth.clamp(treeScroll - (int) (scrollY * 20), 0, treeMaxScroll);
			syncRenameBox();
			return true;
		}
		if (mx >= editorX && mx <= editorX + editorW && my >= editorY && my <= editorY + editorH + 12) {
			updateEditorScroll();
			boolean overH = editorMaxHScroll > 0
				&& UiTheme.hitHScrollBar(editorHBarX(), editorHBarY(), editorHBarW(), mx, my);
			boolean horiz = overH || hasShiftDown() || (Math.abs(scrollX) > Math.abs(scrollY) && scrollX != 0);
			if (horiz && editorMaxHScroll > 0) {
				double wheel = scrollX != 0 ? scrollX : scrollY;
				int delta = (int) (wheel * 24);
				if (delta == 0) delta = (int) Math.signum(wheel) * 12;
				hScroll = Mth.clamp(hScroll - delta, 0, editorMaxHScroll);
				return true;
			}
			scrollLines = Mth.clamp(scrollLines - (int) Math.signum(scrollY != 0 ? scrollY : scrollX), 0, editorMaxScroll);
			return true;
		}
		return super.mouseScrolled(mx, my, scrollX, scrollY);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (treeMenu.isOpen() && keyCode == 256) {
			closeTreeMenu();
			return true;
		}
		if (editingFileNameNode != null) {
			if (keyCode == 257 || keyCode == 335) {
				finishRename();
				return true;
			}
			if (keyCode == 256) {
				cancelRename();
				return true;
			}
			return fileNameEditBox.keyPressed(keyCode, scanCode, modifiers);
		}

		if (canSuggestCommands() && commandSuggestions != null) {
			if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB) {
				refreshCommandSuggestions();
				try {
					if (!commandSuggestions.keyPressed(keyCode, scanCode, modifiers)) {
						commandSuggestions.showSuggestions(true);
					}
				} catch (Exception ignored) {}
				applyCommandInputToLine();
				return true;
			}
			if (commandSuggestions.isVisible()) {
				try {
					if (commandSuggestions.keyPressed(keyCode, scanCode, modifiers)) {
						applyCommandInputToLine();
						return true;
					}
				} catch (Exception ignored) {}
				if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
					hideCommandSuggestions();
					return true;
				}
			}
		}

		boolean ctrl = hasControlDown();
		boolean shift = hasShiftDown();

		if (ctrl && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_S) {
			saveCurrentFile(true);
			return true;
		}

		if (ctrl && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_A) {
			selStartLine = 0;
			selStartCol = 0;
			cursorLine = editorLines.size() - 1;
			cursorCol = editorLines.isEmpty() ? 0 : editorLines.get(editorLines.size() - 1).length();
			clampCursor();
			return true;
		}

		if (ctrl && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_C) {
			copySelection();
			return true;
		}

		if (ctrl && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_V) {
			pasteFromClipboard();
			refreshCommandSuggestions();
			return true;
		}

		if (ctrl && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_X) {
			copySelection();
			deleteSelection();
			refreshCommandSuggestions();
			return true;
		}

		switch (keyCode) {
			case org.lwjgl.glfw.GLFW.GLFW_KEY_UP:
				moveCursor(0, -1, shift);
				refreshCommandSuggestions();
				return true;
			case org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN:
				moveCursor(0, 1, shift);
				refreshCommandSuggestions();
				return true;
			case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT:
				moveCursor(-1, 0, shift);
				refreshCommandSuggestions();
				return true;
			case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT:
				moveCursor(1, 0, shift);
				refreshCommandSuggestions();
				return true;
			case org.lwjgl.glfw.GLFW.GLFW_KEY_HOME:
				if (shift) {
					if (selStartLine < 0) { selStartLine = cursorLine; selStartCol = cursorCol; }
				} else { selStartLine = -1; selStartCol = -1; }
				cursorCol = 0;
				clampCursor();
				refreshCommandSuggestions();
				return true;
			case org.lwjgl.glfw.GLFW.GLFW_KEY_END:
				if (shift) {
					if (selStartLine < 0) { selStartLine = cursorLine; selStartCol = cursorCol; }
				} else { selStartLine = -1; selStartCol = -1; }
				cursorCol = editorLines.isEmpty() ? 0 : editorLines.get(cursorLine).length();
				clampCursor();
				refreshCommandSuggestions();
				return true;
			case org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE:
				deleteBefore();
				refreshCommandSuggestions();
				return true;
			case org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE:
				deleteAfter();
				refreshCommandSuggestions();
				return true;
			case org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER:
			case org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER:
				hideCommandSuggestions();
				insertNewline();
				refreshCommandSuggestions();
				return true;
		}

		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if (editingFileNameNode != null) {
			return fileNameEditBox.charTyped(codePoint, modifiers);
		}
		if (codePoint == '`' || codePoint == '\u00a7') return false;

		if (codePoint >= 32 && codePoint != 127) {
			insertChar(String.valueOf(codePoint));
			refreshCommandSuggestions();
			return true;
		}
		return super.charTyped(codePoint, modifiers);
	}

	@Override
	public void tick() {
		super.tick();
		tickCount++;
	}

	private void moveCursor(int dc, int dl, boolean shift) {
		if (!shift) {
			selStartLine = -1;
			selStartCol = -1;
		} else if (selStartLine < 0) {
			selStartLine = cursorLine;
			selStartCol = cursorCol;
		}

		cursorLine = Math.max(0, Math.min(editorLines.size() - 1, cursorLine + dl));
		cursorCol += dc;
		clampCursor();
	}

	private void clampCursor() {
		if (editorLines.isEmpty()) {
			if (editorLines.isEmpty()) {
				editorLines.add("");
			}
			cursorLine = 0;
			cursorCol = 0;
			return;
		}
		cursorLine = Math.max(0, Math.min(editorLines.size() - 1, cursorLine));
		cursorCol = Math.max(0, Math.min(editorLines.get(cursorLine).length(), cursorCol));

		if (cursorLine < scrollLines) scrollLines = cursorLine;
		if (cursorLine >= scrollLines + visibleLines) scrollLines = cursorLine - visibleLines + 1;
		updateEditorScroll();
		int cx = visibleColWidth(editorLines.get(cursorLine), cursorCol);
		int viewW = editorTextViewW();
		int pad = 8;
		if (cx < hScroll + pad) hScroll = Math.max(0, cx - pad);
		else if (cx > hScroll + viewW - pad) hScroll = cx - viewW + pad;
		updateEditorScroll();
	}

	private void insertChar(String ch) {
		deleteSelection();
		String line = editorLines.get(cursorLine);
		editorLines.set(cursorLine, line.substring(0, cursorCol) + ch + line.substring(cursorCol));
		cursorCol += ch.length();
		dirty = true;
		clampCursor();
	}

	private void insertNewline() {
		deleteSelection();
		String line = editorLines.get(cursorLine);
		String rest = line.substring(cursorCol);
		editorLines.set(cursorLine, line.substring(0, cursorCol));
		editorLines.add(cursorLine + 1, rest);
		cursorLine++;
		cursorCol = 0;
		dirty = true;
		clampCursor();
	}

	private void deleteBefore() {
		if (deleteSelection()) return;
		if (cursorCol > 0) {
			String line = editorLines.get(cursorLine);
			int removeLen = 1;
			if (cursorCol >= 2 && line.charAt(cursorCol - 1) == '\u00a7') {
				removeLen = 2;
			}
			editorLines.set(cursorLine,
				line.substring(0, Math.max(0, cursorCol - removeLen)) + line.substring(cursorCol));
			cursorCol = Math.max(0, cursorCol - removeLen);
			dirty = true;
		} else if (cursorLine > 0) {
			String prevLine = editorLines.get(cursorLine - 1);
			String curLine = editorLines.get(cursorLine);
			editorLines.set(cursorLine - 1, prevLine + curLine);
			editorLines.remove(cursorLine);
			cursorLine--;
			cursorCol = prevLine.length();
			dirty = true;
		}
		clampCursor();
	}

	private void deleteAfter() {
		if (deleteSelection()) return;
		String line = editorLines.get(cursorLine);
		if (cursorCol < line.length()) {
			int removeLen = 1;
			if (cursorCol + 1 < line.length() && line.charAt(cursorCol) == '\u00a7') {
				removeLen = 2;
			}
			editorLines.set(cursorLine, line.substring(0, cursorCol) + line.substring(cursorCol + removeLen));
			dirty = true;
		} else if (cursorLine < editorLines.size() - 1) {
			String curLine = editorLines.get(cursorLine);
			String nextLine = editorLines.get(cursorLine + 1);
			editorLines.set(cursorLine, curLine + nextLine);
			editorLines.remove(cursorLine + 1);
			dirty = true;
		}
		clampCursor();
	}

	private boolean deleteSelection() {
		if (selStartLine < 0) return false;

		int sL, sC, eL, eC;
		if (isBefore(selStartLine, selStartCol, cursorLine, cursorCol)) {
			sL = selStartLine; sC = selStartCol;
			eL = cursorLine; eC = cursorCol;
		} else {
			sL = cursorLine; sC = cursorCol;
			eL = selStartLine; eC = selStartCol;
		}

		if (sL == eL && sC == eC) {
			selStartLine = -1;
			selStartCol = -1;
			return false;
		}

		if (sL == eL) {
			String line = editorLines.get(sL);
			editorLines.set(sL, line.substring(0, sC) + line.substring(eC));
		} else {
			String firstPart = editorLines.get(sL).substring(0, sC);
			String lastPart = editorLines.get(eL).substring(eC);
			for (int i = eL; i >= sL; i--) {
				editorLines.remove(i);
			}
			editorLines.set(sL, firstPart + lastPart);
		}
		cursorLine = sL;
		cursorCol = sC;
		selStartLine = -1;
		selStartCol = -1;
		dirty = true;
		clampCursor();
		return true;
	}

	private void copySelection() {
		String selected = getSelectedText();
		if (selected != null && !selected.isEmpty()) {
			Minecraft.getInstance().keyboardHandler.setClipboard(selected);
		}
	}

	private String getSelectedText() {
		if (selStartLine < 0) return null;
		int sL, sC, eL, eC;
		if (isBefore(selStartLine, selStartCol, cursorLine, cursorCol)) {
			sL = selStartLine; sC = selStartCol;
			eL = cursorLine; eC = cursorCol;
		} else {
			sL = cursorLine; sC = cursorCol;
			eL = selStartLine; eC = selStartCol;
		}
		if (sL == eL && sC == eC) return "";

		if (sL == eL) {
			return editorLines.get(sL).substring(sC, eC);
		}
		StringBuilder sb = new StringBuilder();
		sb.append(editorLines.get(sL).substring(sC)).append("\n");
		for (int i = sL + 1; i < eL; i++) {
			sb.append(editorLines.get(i)).append("\n");
		}
		sb.append(editorLines.get(eL).substring(0, eC));
		return sb.toString();
	}

	private void pasteFromClipboard() {
		String text = Minecraft.getInstance().keyboardHandler.getClipboard();
		if (text == null || text.isEmpty()) return;
		deleteSelection();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\n') {
				insertNewline();
			} else if (c == '\r') {
				continue;
			} else if (c >= 32) {
				insertChar(String.valueOf(c));
			}
		}
	}

	private boolean canSuggestCommands() {
		if (!isCommandSourceFile()) return false;
		if (this.minecraft == null || this.minecraft.getConnection() == null) return false;
		if (editorLines.isEmpty()) return false;
		String line = editorLines.get(Mth.clamp(cursorLine, 0, editorLines.size() - 1));
		return !line.startsWith("#");
	}

	private void layoutCommandInput() {
		int lineIdx = Mth.clamp(cursorLine, 0, Math.max(0, editorLines.size() - 1));
		int cmdLineY = editorY + 2 + (lineIdx - scrollLines) * LINE_H;
		int left = editorTextLeft();
		String line = editorLines.isEmpty() ? "" : editorLines.get(lineIdx);
		boolean hadSlash = line.startsWith("/");
		int textW = font.width(hadSlash ? line : "/" + line) + 32;
		commandInput.setX(left - hScroll);
		commandInput.setY(cmdLineY);
		commandInput.setWidth(Math.max(40, textW));
		commandInput.setHeight(LINE_H + 2);
	}

	public int[] placeCommandSuggestions(int x, int w, int count) {
		int left = editorTextLeft();
		int right = editorX + editorW - 2;
		int top = editorY + 1;
		int bottom = editorY + editorH - 1;
		int listH = Math.max(12, Math.min(Math.max(1, count), 10) * 12);
		int lineY = commandInput != null ? commandInput.getY() : top;
		int lineH = commandInput != null ? Math.max(LINE_H, commandInput.getHeight()) : LINE_H;
		w = Mth.clamp(w, 20, Math.max(20, right - left));
		x = Mth.clamp(x, left, Math.max(left, right - w));
		int below = lineY + lineH + 1;
		int above = lineY - listH;
		int spaceBelow = bottom - below;
		int spaceAbove = lineY - top;
		int y;
		if (spaceBelow >= listH) y = below;
		else if (spaceAbove >= listH) y = above;
		else if (spaceBelow >= spaceAbove) y = below;
		else y = Math.max(top, above);
		if (y + listH > bottom) y = Math.max(top, bottom - listH);
		if (y < top) y = top;
		return new int[] {x, y, w};
	}

	public void renderCommandUsage(GuiGraphics g, List<FormattedCharSequence> lines,
			int x, int w, int color, Font usageFont, EditBox box) {
		if (lines == null || lines.isEmpty() || box == null) return;
		int n = lines.size();
		int boxH = n * 12;
		int left = editorTextLeft();
		int right = editorX + editorW - 2;
		int top = editorY + 1;
		int bottom = editorY + editorH - 1;
		w = Mth.clamp(w, 20, Math.max(20, right - left));
		x = Mth.clamp(x, left, Math.max(left, right - w));
		int lineY = box.getY();
		int y = lineY - boxH - 2;
		if (y < top) y = lineY + Math.max(LINE_H, box.getHeight()) + 1;
		if (y + boxH > bottom) y = Math.max(top, bottom - boxH);
		if (y < top) y = top;
		for (int i = 0; i < n; i++) {
			int yy = y + i * 12;
			g.fill(x - 1, yy, x + w, yy + 12, color);
			g.drawString(usageFont, lines.get(i), x, yy + 2, 0xFFE6EDF3);
		}
	}

	private void refreshCommandSuggestions() {
		if (commandSuggestions == null || commandInput == null) return;
		if (!canSuggestCommands()) {
			hideCommandSuggestions();
			return;
		}
		layoutCommandInput();
		int cmdLineY = commandInput.getY();
		if (cmdLineY + LINE_H < editorY || cmdLineY >= editorY + editorH) {
			hideCommandSuggestions();
			return;
		}
		String line = editorLines.get(Mth.clamp(cursorLine, 0, editorLines.size() - 1));
		boolean hadSlash = line.startsWith("/");
		String prefixed = hadSlash ? line : "/" + line;
		int col = Mth.clamp(cursorCol + (hadSlash ? 0 : 1), 0, prefixed.length());
		commandInput.setValue(prefixed);
		commandInput.setCursorPosition(col);
		commandInput.setHighlightPos(col);
		commandSuggestions.setAllowSuggestions(true);
		try {
			commandSuggestions.updateCommandInfo();
		} catch (Exception ignored) {}
	}

	private void applyCommandInputToLine() {
		if (commandInput == null || editorLines.isEmpty()) return;
		String raw = commandInput.getValue();
		boolean strip = raw.startsWith("/");
		String val = strip ? raw.substring(1) : raw;
		int cp = commandInput.getCursorPosition();
		if (strip) cp = Math.max(0, cp - 1);
		int lineIdx = Mth.clamp(cursorLine, 0, editorLines.size() - 1);
		if (!editorLines.get(lineIdx).equals(val)) {
			editorLines.set(lineIdx, val);
			dirty = true;
		}
		cursorCol = Math.min(cp, val.length());
		clampCursor();
	}

	private void hideCommandSuggestions() {
		if (commandSuggestions == null) return;
		try {
			commandSuggestions.hide();
		} catch (Exception ignored) {}
	}

	private void loadSelectedFile() {
		if (selectedNode == null || selectedNode.isDirectory) return;
		if (remoteMode) {
			keepRemoteCursor = false;
			currentFilePath = selectedNode.path.toAbsolutePath().toString();
			DatapackNet.send(DatapackOps.READ, DatapackNet.rel(datapacksPath, selectedNode.path), "");
			return;
		}
		try {
			String content = Files.readString(selectedNode.path);
			applyEditorContent(content, selectedNode.path.toAbsolutePath().toString());
		} catch (IOException e) {
			editorLines.clear();
			editorLines.add("");
			currentFilePath = null;
		}
		refreshCommandSuggestions();
	}

	private void applyEditorContent(String content, String filePath) {
		editorLines.clear();
		String[] parts = (content == null ? "" : content).split("\n", -1);
		editorLines.addAll(Arrays.asList(parts));
		if (editorLines.isEmpty()) editorLines.add("");
		cursorLine = 0;
		cursorCol = 0;
		scrollLines = 0;
		hScroll = 0;
		selStartLine = -1;
		selStartCol = -1;
		dirty = false;
		currentFilePath = filePath;
		updateEditorScroll();
		refreshCommandSuggestions();
	}

	private void reloadCurrentFile() {
		if (currentFilePath == null) {
			editorLines.clear();
			editorLines.add("");
			cursorLine = 0;
			cursorCol = 0;
			return;
		}
		if (remoteMode) {
			keepRemoteCursor = true;
			DatapackNet.send(DatapackOps.READ, DatapackNet.rel(datapacksPath, Path.of(currentFilePath)), "");
			return;
		}
		try {
			String content = Files.readString(Path.of(currentFilePath));
			editorLines.clear();
			String[] parts = content.split("\n", -1);
			editorLines.addAll(Arrays.asList(parts));
			if (editorLines.isEmpty()) editorLines.add("");
			dirty = false;
		} catch (IOException e) {
			editorLines.clear();
			editorLines.add("");
			dirty = false;
		}
	}

	private void saveCurrentFile() {
		saveCurrentFile(false);
	}

	private void saveCurrentFile(boolean apply) {
		if (currentFilePath == null) return;
		if (remoteMode) {
			pendingApplyReload = apply;
			DatapackNet.send(DatapackOps.WRITE, DatapackNet.rel(datapacksPath, Path.of(currentFilePath)), String.join("\n", editorLines));
			return;
		}
		try {
			Files.writeString(Path.of(currentFilePath), String.join("\n", editorLines), StandardCharsets.UTF_8);
			dirty = false;
			if (apply) {
				Minecraft mc = Minecraft.getInstance();
				if (mc.player != null) {
					mc.player.displayClientMessage(Component.translatable("screen.bj_mapedit.applied"), true);
				}
				reloadWorldDatapacks(null);
			}
		} catch (IOException ignored) {}
	}

	private void reloadWorldDatapacks(Component doneMsg) {
		Minecraft mc = Minecraft.getInstance();
		if (remoteMode) {
			DatapackNet.send(DatapackOps.RELOAD, "", "");
			return;
		}
		IntegratedServer server = mc.getSingleplayerServer();
		if (server == null) return;
		server.execute(() -> {
			var repo = server.getPackRepository();
			repo.reload();
			Collection<String> selected = new ArrayList<>(repo.getSelectedIds());
			if (datapacksPath != null) {
				selected.removeIf(id -> id.startsWith("file/")
					&& !Files.isDirectory(datapacksPath.resolve(id.substring("file/".length()))));
			}
			server.reloadResources(selected).thenRunAsync(() -> {
				if (doneMsg == null) return;
				Minecraft.getInstance().execute(() -> {
					var player = Minecraft.getInstance().player;
					if (player != null) {
						player.displayClientMessage(doneMsg, true);
					}
				});
			}, server);
		});
	}

	private void createNewFile(boolean isDirectory) {
		Path parentDir;
		FileNode parentNode = selectedNode;
		if (parentNode != null && parentNode.isDirectory) {
			parentDir = parentNode.path;
		} else if (parentNode != null) {
			parentDir = parentNode.path.getParent();
		} else {
			parentDir = datapacksPath;
		}
		if (parentDir == null || (!remoteMode && !Files.isDirectory(parentDir))) {
			parentDir = datapacksPath;
		}

		final Path baseDir = parentDir;
		String baseName = isDirectory ? "new_folder" : "new_file";
		String ext = "";
		if (!isDirectory && isFunctionDir(baseDir)) {
			ext = ".mcfunction";
		} else if (!isDirectory && isJsonDir(baseDir)) {
			ext = ".json";
		} else if (!isDirectory && samePath(baseDir, datapacksPath)) {
			baseName = "new_datapack";
		}
		Path newPath = findAvailablePath(baseDir, baseName, ext);
		if (remoteMode) {
			pendingSelectRel = DatapackNet.rel(datapacksPath, newPath);
			DatapackNet.send(isDirectory ? DatapackOps.MKDIR : DatapackOps.CREATE_FILE, pendingSelectRel, "");
			return;
		}
		try {
			if (isDirectory) {
				Files.createDirectories(newPath);
			} else {
				Files.createFile(newPath);
			}
		} catch (IOException ignored) {
			return;
		}
		loadFileTree();
		selectNodeByPath(newPath);
		if (!isDirectory) {
			loadSelectedFile();
		}
	}

	private void createNewDatapack() {
		if (remoteMode) {
			selectAfterOk = true;
			DatapackNet.send(DatapackOps.CREATE_PACK, "new_datapack", "");
			return;
		}
		Path parentDir = datapacksPath;
		Path newPath = findAvailablePath(parentDir, "new_datapack", "");
		try {
			Files.createDirectories(newPath);
			String namespace = newPath.getFileName().toString();
			String mcmeta = "{\n  \"pack\": {\n    \"pack_format\": 48,\n    \"description\": \"My Datapack\"\n  }\n}\n";
			Files.writeString(newPath.resolve("pack.mcmeta"), mcmeta, StandardCharsets.UTF_8);
			Path dataDir = newPath.resolve("data").resolve(namespace);
			Files.createDirectories(dataDir.resolve("function"));
			Files.createDirectories(dataDir.resolve("advancement"));
			Files.createDirectories(dataDir.resolve("recipe"));
			Files.createDirectories(dataDir.resolve("loot_table"));
			Path minecraftTagsFunc = newPath.resolve("data").resolve("minecraft").resolve("tags").resolve("function");
			Files.createDirectories(minecraftTagsFunc);
			String loadJson = "{\n  \"values\": [\n    \"" + namespace + ":load\"\n  ]\n}\n";
			String tickJson = "{\n  \"values\": [\n    \"" + namespace + ":tick\"\n  ]\n}\n";
			Files.writeString(minecraftTagsFunc.resolve("load.json"), loadJson, StandardCharsets.UTF_8);
			Files.writeString(minecraftTagsFunc.resolve("tick.json"), tickJson, StandardCharsets.UTF_8);
			Files.writeString(dataDir.resolve("function").resolve("load.mcfunction"), "# Load function\n", StandardCharsets.UTF_8);
			Files.writeString(dataDir.resolve("function").resolve("tick.mcfunction"), "# Tick function\n", StandardCharsets.UTF_8);
		} catch (IOException ignored) {
			return;
		}
		loadFileTree();
		selectNodeByPath(newPath);
	}

	private void deleteSelected() {
		if (selectedNode == null) return;
		closeTreeMenu();
		if (remoteMode) {
			DatapackNet.send(DatapackOps.DELETE, DatapackNet.rel(datapacksPath, selectedNode.path), "");
			if (currentFilePath != null && currentFilePath.startsWith(selectedNode.path.toAbsolutePath().toString())) {
				currentFilePath = null;
				editorLines.clear();
				editorLines.add("");
				dirty = false;
			}
			selectedNode = null;
			return;
		}
		try {
			if (selectedNode.isDirectory) {
				deleteRecursive(selectedNode.path);
			} else {
				Files.delete(selectedNode.path);
			}
			if (currentFilePath != null && currentFilePath.startsWith(selectedNode.path.toAbsolutePath().toString())) {
				currentFilePath = null;
				editorLines.clear();
				editorLines.add("");
				dirty = false;
			}
			selectedNode = null;
			loadFileTree();
			reloadWorldDatapacks(Component.translatable("screen.bj_mapedit.deleted"));
		} catch (IOException ignored) {}
	}

	private void deleteRecursive(Path path) throws IOException {
		if (Files.isDirectory(path)) {
			try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
				for (Path child : ds) {
					deleteRecursive(child);
				}
			}
		}
		Files.delete(path);
	}

	private void startRename() {
		if (selectedNode == null || fileNameEditBox == null) return;
		closeTreeMenu();
		editingFileNameNode = selectedNode;
		fileNameEditBox.setValue(selectedNode.name);
		fileNameEditBox.setVisible(true);
		syncRenameBox();
		setFocused(fileNameEditBox);
		fileNameEditBox.setFocused(true);
		fileNameEditBox.moveCursorToEnd(false);
	}

	private void syncRenameBox() {
		if (fileNameEditBox == null || editingFileNameNode == null) {
			if (fileNameEditBox != null) {
				fileNameEditBox.setVisible(false);
				fileNameEditBox.setY(-1000);
			}
			return;
		}
		int idx = findNodeIndex(editingFileNameNode);
		if (idx < 0) {
			fileNameEditBox.setVisible(false);
			fileNameEditBox.setY(-1000);
			return;
		}
		int x = templateRailX + 6 + editingFileNameNode.depth * 10 + TREE_ICON + 3;
		int y = treeRowY(idx) + 2;
		int w = Math.max(24, treeActionRight() - x - 4);
		fileNameEditBox.setX(x);
		fileNameEditBox.setY(y);
		fileNameEditBox.setWidth(w);
		fileNameEditBox.setHeight(14);
		fileNameEditBox.setVisible(true);
	}

	private void finishRename() {
		if (editingFileNameNode == null) return;
		String newName = fileNameEditBox.getValue().trim();
		if (!newName.isEmpty() && !newName.equals(editingFileNameNode.name)) {
			Path oldPath = editingFileNameNode.path.toAbsolutePath().normalize();
			Path newPath = editingFileNameNode.path.resolveSibling(newName).toAbsolutePath().normalize();
			if (remoteMode) {
				if (currentFilePath != null) {
					Path curPath = Path.of(currentFilePath).toAbsolutePath().normalize();
					if (curPath.startsWith(oldPath)) {
						Path relative = oldPath.relativize(curPath);
						currentFilePath = newPath.resolve(relative).toString();
					}
				}
				pendingSelectRel = DatapackNet.rel(datapacksPath, newPath);
				editingFileNameNode = null;
				fileNameEditBox.setVisible(false);
				fileNameEditBox.setFocused(false);
				DatapackNet.send(DatapackOps.RENAME, DatapackNet.rel(datapacksPath, oldPath), pendingSelectRel);
				return;
			}
			try {
				Files.move(editingFileNameNode.path, newPath);
				if (currentFilePath != null) {
					Path curPath = Path.of(currentFilePath).toAbsolutePath().normalize();
					if (curPath.startsWith(oldPath)) {
						Path relative = oldPath.relativize(curPath);
						currentFilePath = newPath.resolve(relative).toString();
					}
				}
				Path renamedPath = newPath;
				editingFileNameNode = null;
				fileNameEditBox.setVisible(false);
				fileNameEditBox.setFocused(false);
				loadFileTree();
				selectNodeByPath(renamedPath);
				if (selectedNode != null && !selectedNode.isDirectory) {
					loadSelectedFile();
				}
				reloadWorldDatapacks(null);
				return;
			} catch (IOException ignored) {}
		}
		editingFileNameNode = null;
		if (fileNameEditBox != null) {
			fileNameEditBox.setVisible(false);
			fileNameEditBox.setFocused(false);
			fileNameEditBox.setY(-1000);
			if (fileNameEditBox.isFocused()) setFocused(null);
		}
		loadFileTree();
	}

	private void cancelRename() {
		editingFileNameNode = null;
		if (fileNameEditBox != null) {
			fileNameEditBox.setVisible(false);
			fileNameEditBox.setFocused(false);
			fileNameEditBox.setY(-1000);
			if (fileNameEditBox.isFocused()) setFocused(null);
		}
	}

	private void selectNodeByPath(Path path) {
		if (path == null) return;
		expandTo(path);
		flattenTree();
		Path want = path.toAbsolutePath().normalize();
		for (FileNode node : flatTree) {
			if (samePath(node.path, want)) {
				selectedNode = node;
				return;
			}
		}
	}

	private Path findAvailablePath(Path dir, String baseName, String ext) {
		Path candidate = dir.resolve(baseName + ext);
		if (!pathOccupied(candidate)) return candidate;
		for (int i = 1; i < 1000; i++) {
			candidate = dir.resolve(baseName + "_" + i + ext);
			if (!pathOccupied(candidate)) return candidate;
		}
		return dir.resolve(baseName + "_" + System.currentTimeMillis() + ext);
	}

	private boolean pathOccupied(Path path) {
		if (remoteMode) return findNodeByPath(path) != null;
		return Files.exists(path);
	}

	private String getCurrentFileExt() {
		if (currentFilePath == null) return "";
		String name = Path.of(currentFilePath).getFileName().toString();
		int dot = name.lastIndexOf('.');
		if (dot < 0) return "";
		return name.substring(dot + 1);
	}

	private boolean isCommandSourceFile() {
		String ext = getCurrentFileExt();
		if (ext.equalsIgnoreCase("mcfunction") || ext.equalsIgnoreCase("function")) return true;
		if (currentFilePath == null) return false;
		Path parent = Path.of(currentFilePath).getParent();
		return parent != null && isFunctionDir(parent) && !ext.equalsIgnoreCase("json") && !ext.equalsIgnoreCase("mcmeta");
	}

	private boolean isFunctionDir(Path dir) {
		if (dir == null) return false;
		for (Path part : dir.toAbsolutePath().normalize()) {
			String n = part.toString();
			if (n.equals("function") || n.equals("functions")) return true;
		}
		return false;
	}

	private boolean isJsonDir(Path dir) {
		if (dir == null) return false;
		for (Path part : dir.toAbsolutePath().normalize()) {
			String n = part.toString();
			if (n.equals("recipe") || n.equals("recipes")
				|| n.equals("advancement") || n.equals("advancements")
				|| n.equals("loot_table") || n.equals("loot_tables")
				|| n.equals("tags") || n.equals("predicate") || n.equals("predicates")) {
				return true;
			}
		}
		return false;
	}

	private boolean isValidMcCommand(String cmd) {
		if (cmd.isEmpty()) return false;
		try {
			var conn = Minecraft.getInstance().getConnection();
			if (conn != null) {
				return conn.getCommands().getRoot().getChildren().stream()
					.anyMatch(node -> node.getName().equalsIgnoreCase(cmd));
			}
			IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
			if (server == null) return false;
			return server.getCommands().getDispatcher().getRoot().getChildren().stream()
				.anyMatch(node -> node.getName().equalsIgnoreCase(cmd));
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public void removed() {
		session = captureSession();
		super.removed();
	}

	@Override
	public void onClose() {
		saveCurrentFile();
		super.onClose();
	}

	@Override
	public void insertText(String text, boolean overwrite) {
		if (text == null || text.isEmpty()) return;
		if (editingFileNameNode != null) {
			fileNameEditBox.insertText(text);
			return;
		}
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\n') insertNewline();
			else if (c >= 32) insertChar(String.valueOf(c));
		}
		refreshCommandSuggestions();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void drawTreeIcon(GuiGraphics g, FileNode node, int x, int y, boolean hot) {
		if (node.isDirectory) {
			UiTheme.drawFolderIcon(g, x, y, node.expanded, hot);
			return;
		}
		String ext = "";
		int dot = node.name.lastIndexOf('.');
		if (dot >= 0) ext = node.name.substring(dot + 1);
		UiTheme.drawFileTypeIcon(g, x, y, ext, hot);
	}

	private boolean inFileRail(double mouseX, double mouseY) {
		return mouseX >= templateRailX && mouseX < templateRailX + templateRailW
			&& mouseY >= topPos && mouseY < topPos + HEIGHT;
	}

	private int treeListTop() {
		return topPos + UiTheme.HEADER_H + 6;
	}

	private int treeListBottom() {
		return topPos + HEIGHT - 8;
	}

	private int treeListH() {
		return Math.max(0, treeListBottom() - treeListTop());
	}

	private void updateTreeScroll() {
		leftPanelX = templateRailX;
		leftPanelY = treeListTop();
		leftPanelW = templateRailW;
		leftPanelH = treeListH();
		treeMaxScroll = Math.max(0, flatTree.size() * TREE_ITEM_H - treeListH());
		treeScroll = Mth.clamp(treeScroll, 0, treeMaxScroll);
	}

	private void updateEditorScroll() {
		editorMaxScroll = Math.max(0, editorLines.size() - visibleLines);
		scrollLines = Mth.clamp(scrollLines, 0, editorMaxScroll);
		int maxW = 0;
		if (font != null) {
			for (String line : editorLines) {
				int w = font.width(line);
				if (w > maxW) maxW = w;
			}
		}
		editorMaxHScroll = Math.max(0, maxW + 8 - editorTextViewW());
		hScroll = Mth.clamp(hScroll, 0, editorMaxHScroll);
	}

	private int editorTextLeft() {
		return editorX + LINE_NUM_W + 4;
	}

	private int editorTextViewW() {
		int bar = editorMaxScroll > 0 ? 6 : 0;
		return Math.max(1, editorX + editorW - editorTextLeft() - bar);
	}

	private int editorHBarX() {
		return editorX;
	}

	private int editorHBarY() {
		return editorY + editorH + 3;
	}

	private int editorHBarW() {
		return Math.max(1, editorW);
	}

	private int treeScrollBarX() {
		return templateRailX + templateRailW - 5;
	}

	private int editorScrollBarX() {
		return editorX + editorW - 5;
	}

	private int treeActionRight() {
		return templateRailX + templateRailW - 8 - (treeMaxScroll > 0 ? 6 : 0);
	}

	private int treeRowY(int index) {
		return treeListTop() + index * TREE_ITEM_H - treeScroll;
	}

	private static String pathKey(Path path) {
		return path.toAbsolutePath().normalize().toString();
	}

	private static boolean samePath(Path a, Path b) {
		try {
			return a.toAbsolutePath().normalize().equals(b.toAbsolutePath().normalize());
		} catch (Exception e) {
			return false;
		}
	}

	private Set<String> collectExpanded() {
		Set<String> out = new HashSet<>();
		collectExpanded(rootNodes, out);
		return out;
	}

	private void collectExpanded(List<FileNode> nodes, Set<String> out) {
		for (FileNode node : nodes) {
			if (node.isDirectory && node.expanded) {
				out.add(pathKey(node.path));
				collectExpanded(node.children, out);
			}
		}
	}

	private void applyExpanded(List<FileNode> nodes, Set<String> expanded) {
		for (FileNode node : nodes) {
			if (node.isDirectory) {
				node.expanded = expanded.contains(pathKey(node.path));
				applyExpanded(node.children, expanded);
			}
		}
	}

	private void expandTo(Path path) {
		Path want = path.toAbsolutePath().normalize();
		expandTo(rootNodes, want);
	}

	private boolean expandTo(List<FileNode> nodes, Path want) {
		for (FileNode node : nodes) {
			Path p = node.path.toAbsolutePath().normalize();
			if (want.equals(p)) return true;
			if (node.isDirectory && want.startsWith(p)) {
				node.expanded = true;
				if (expandTo(node.children, want)) return true;
			}
		}
		return false;
	}

	private Session captureSession() {
		Session s = new Session();
		s.currentFilePath = currentFilePath;
		s.editorLines = new ArrayList<>(editorLines);
		s.cursorLine = cursorLine;
		s.cursorCol = cursorCol;
		s.scrollLines = scrollLines;
		s.hScroll = hScroll;
		s.treeScroll = treeScroll;
		s.dirty = dirty;
		s.selectedPath = selectedNode != null ? pathKey(selectedNode.path) : null;
		s.expanded = collectExpanded();
		return s;
	}

	private void restoreSession(Session s) {
		if (s == null) return;
		currentFilePath = s.currentFilePath;
		editorLines.clear();
		if (s.editorLines != null && !s.editorLines.isEmpty()) {
			editorLines.addAll(s.editorLines);
		} else {
			editorLines.add("");
		}
		cursorLine = s.cursorLine;
		cursorCol = s.cursorCol;
		scrollLines = s.scrollLines;
		hScroll = s.hScroll;
		treeScroll = s.treeScroll;
		dirty = s.dirty;
		if (s.selectedPath != null) {
			selectNodeByPath(Path.of(s.selectedPath));
		}
		clampCursor();
		updateEditorScroll();
		updateTreeScroll();
	}

	private boolean updateTreeDrag(double mx, double my) {
		if (!treeDrag.move(mx, my)) return false;
		treeDrag.hoverDest = null;
		treeDrag.hoverHighlight = null;
		if (!inFileRail(mx, my)) return true;
		if (my < treeListTop() || getTreeItemAt((int) my) < 0) {
			treeDrag.hoverDest = datapacksPath;
			return true;
		}
		FileNode over = findNodeAt(getTreeItemAt((int) my));
		if (over == null) {
			treeDrag.hoverDest = datapacksPath;
			return true;
		}
		if (over == treeDrag.src) return true;
		if (over.isDirectory) {
			treeDrag.hoverDest = over.path;
			treeDrag.hoverHighlight = over;
		} else {
			Path parent = over.path.getParent();
			treeDrag.hoverDest = parent != null ? parent : datapacksPath;
			treeDrag.hoverHighlight = findVisibleDir(treeDrag.hoverDest);
			if (treeDrag.hoverHighlight == null) treeDrag.hoverHighlight = over;
		}
		return true;
	}

	public void handleNet(DatapackOpResultPayload payload) {
		if (payload == null) return;
		if (payload.kind() == DatapackOps.KIND_ERR) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player != null && payload.extra() != null && !payload.extra().isEmpty()) {
				mc.player.displayClientMessage(Component.translatable(payload.extra()), true);
			}
			return;
		}
		if (payload.kind() == DatapackOps.KIND_FILE) {
			Path want = DatapackNet.join(datapacksPath, payload.path());
			boolean keep = keepRemoteCursor;
			keepRemoteCursor = false;
			if (currentFilePath != null && samePath(Path.of(currentFilePath), want)) {
				int keepLine = cursorLine;
				int keepCol = cursorCol;
				int keepH = hScroll;
				applyEditorContent(payload.extra(), currentFilePath);
				if (keep) {
					cursorLine = keepLine;
					cursorCol = keepCol;
					hScroll = keepH;
					clampCursor();
				}
			} else if (selectedNode != null && samePath(selectedNode.path, want)) {
				applyEditorContent(payload.extra(), want.toAbsolutePath().toString());
			}
			return;
		}
		if (payload.kind() == DatapackOps.KIND_OK) {
			dirty = false;
			if (selectAfterOk) {
				selectAfterOk = false;
				if (payload.path() != null && !payload.path().isEmpty()) pendingSelectRel = payload.path();
			}
			if (pendingApplyReload) {
				pendingApplyReload = false;
				DatapackNet.send(DatapackOps.RELOAD, "", "");
			}
			if (payload.extra() != null && !payload.extra().isEmpty()) {
				Minecraft mc = Minecraft.getInstance();
				if (mc.player != null) {
					mc.player.displayClientMessage(Component.translatable(payload.extra()), true);
				}
			}
			return;
		}
		if (payload.kind() == DatapackOps.KIND_TREE) {
			applyTreeListing(payload.extra());
		}
	}

	private void applyTreeListing(String listing) {
		Set<String> expanded = pendingExpanded != null ? pendingExpanded : collectExpanded();
		pendingExpanded = null;
		rootNodes.clear();
		java.util.Map<String, FileNode> byRel = new java.util.LinkedHashMap<>();
		if (listing != null && !listing.isEmpty()) {
			for (String line : listing.split("\n")) {
				if (line.length() < 3 || line.charAt(1) != '\t') continue;
				boolean dir = line.charAt(0) == 'D';
				String rel = line.substring(2).replace('\\', '/');
				if (rel.isEmpty()) continue;
				ensureRemoteNode(byRel, rel, dir);
			}
		}
		sortNodes(rootNodes);
		if (expanded != null && !expanded.isEmpty()) applyExpanded(rootNodes, expanded);
		flattenTree();
		if (pendingSelectRel != null && !pendingSelectRel.isEmpty()) {
			selectNodeByPath(DatapackNet.join(datapacksPath, pendingSelectRel));
			if (selectedNode != null && !selectedNode.isDirectory) loadSelectedFile();
			pendingSelectRel = null;
		} else if (currentFilePath != null) {
			selectNodeByPath(Path.of(currentFilePath));
		} else if (selectedNode != null) {
			selectNodeByPath(selectedNode.path);
		}
	}

	private FileNode ensureRemoteNode(java.util.Map<String, FileNode> byRel, String rel, boolean dir) {
		FileNode existing = byRel.get(rel);
		if (existing != null) {
			if (dir) existing.isDirectory = true;
			return existing;
		}
		int slash = rel.lastIndexOf('/');
		String name = slash < 0 ? rel : rel.substring(slash + 1);
		String parentRel = slash < 0 ? "" : rel.substring(0, slash);
		FileNode node = new FileNode();
		node.name = name;
		node.path = DatapackNet.join(datapacksPath, rel);
		node.isDirectory = dir;
		if (parentRel.isEmpty()) {
			node.depth = 0;
			rootNodes.add(node);
		} else {
			FileNode parent = ensureRemoteNode(byRel, parentRel, true);
			node.depth = parent.depth + 1;
			parent.children.add(node);
		}
		byRel.put(rel, node);
		return node;
	}

	private void sortNodes(List<FileNode> nodes) {
		nodes.sort(Comparator.comparing((FileNode n) -> !n.isDirectory).thenComparing(n -> n.name));
		for (FileNode node : nodes) sortNodes(node.children);
	}

	private FileNode findNodeByPath(Path path) {
		return findNodeByPath(rootNodes, path);
	}

	private FileNode findNodeByPath(List<FileNode> nodes, Path path) {
		for (FileNode node : nodes) {
			if (samePath(node.path, path)) return node;
			FileNode child = findNodeByPath(node.children, path);
			if (child != null) return child;
		}
		return null;
	}

	private FileNode findVisibleDir(Path dir) {
		if (dir == null) return null;
		for (FileNode node : flatTree) {
			if (node.isDirectory && samePath(node.path, dir)) return node;
		}
		return null;
	}

	private boolean finishTreeDrag() {
		if (!treeDrag.busy()) return false;
		if (treeMenu.isOpen() || (ignoreTreeLeftUntil > 0 && tickCount <= ignoreTreeLeftUntil)) {
			treeDrag.reset();
			return false;
		}
		FileNode src = treeDrag.src;
		boolean toggle = treeDrag.waitToggle && !treeDrag.active;
		Path dest = treeDrag.hoverDest;
		treeDrag.reset();
		if (src != null && dest != null && !toggle) {
			moveNode(src, dest);
		} else if (src != null && toggle && src.isDirectory) {
			src.expanded = !src.expanded;
			flattenTree();
		}
		return true;
	}

	private void moveNode(FileNode src, Path destDir) {
		if (src == null || src.path == null || destDir == null || datapacksPath == null) return;
		Path root = datapacksPath.toAbsolutePath().normalize();
		Path from = src.path.toAbsolutePath().normalize();
		Path dest = destDir.toAbsolutePath().normalize();
		if (!from.startsWith(root) || !dest.startsWith(root)) return;
		if (!remoteMode && !Files.isDirectory(dest)) return;
		if (remoteMode && findNodeByPath(dest) == null && !samePath(dest, datapacksPath)) return;
		if (src.isDirectory && (dest.equals(from) || dest.startsWith(from))) return;
		Path parent = from.getParent();
		if (parent != null && parent.normalize().equals(dest)) return;
		String name = src.name;
		String base = name;
		String ext = "";
		if (!src.isDirectory) {
			int dot = name.lastIndexOf('.');
			if (dot > 0) {
				base = name.substring(0, dot);
				ext = name.substring(dot);
			}
		}
		Path target = dest.resolve(name);
		if (pathOccupied(target)) target = findAvailablePath(dest, base, ext);
		if (target.equals(from)) return;
		saveCurrentFile();
		Path oldOpen = currentFilePath != null ? Path.of(currentFilePath).toAbsolutePath().normalize() : null;
		if (remoteMode) {
			if (oldOpen != null && (oldOpen.equals(from) || oldOpen.startsWith(from))) {
				Path relative = from.relativize(oldOpen);
				currentFilePath = target.resolve(relative).toString();
			}
			pendingSelectRel = DatapackNet.rel(datapacksPath, target);
			DatapackNet.send(DatapackOps.MOVE, DatapackNet.rel(datapacksPath, from), DatapackNet.rel(datapacksPath, dest));
			return;
		}
		try {
			Files.move(from, target);
		} catch (IOException ignored) {
			return;
		}
		if (oldOpen != null && (oldOpen.equals(from) || oldOpen.startsWith(from))) {
			Path relative = from.relativize(oldOpen);
			currentFilePath = target.resolve(relative).toString();
		}
		Set<String> expanded = collectExpanded();
		expanded.add(pathKey(dest));
		loadFileTree(expanded);
		selectNodeByPath(target);
		reloadWorldDatapacks(null);
	}

	private static class TreeDrag {
		private static final double START = 12;
		FileNode src;
		boolean pending;
		boolean active;
		boolean waitToggle;
		double startX;
		double startY;
		Path hoverDest;
		FileNode hoverHighlight;

		void reset() {
			src = null;
			pending = false;
			active = false;
			waitToggle = false;
			startX = 0;
			startY = 0;
			hoverDest = null;
			hoverHighlight = null;
		}

		void press(FileNode node, double x, double y) {
			src = node;
			pending = true;
			active = false;
			waitToggle = node != null && node.isDirectory;
			startX = x;
			startY = y;
			hoverDest = null;
			hoverHighlight = null;
		}

		boolean move(double x, double y) {
			if (!pending && !active) return false;
			if (!active) {
				double dx = x - startX;
				double dy = y - startY;
				if (dx * dx + dy * dy < START * START) return false;
				active = true;
			}
			return true;
		}

		boolean busy() {
			return pending || active;
		}
	}

	private static class Session {
		String currentFilePath;
		List<String> editorLines;
		int cursorLine;
		int cursorCol;
		int scrollLines;
		int hScroll;
		int treeScroll;
		boolean dirty;
		String selectedPath;
		Set<String> expanded;
	}

	private static class FileNode {
		String name;
		Path path;
		int depth;
		boolean isDirectory;
		boolean expanded = true;
		List<FileNode> children = new ArrayList<>();
	}
}
