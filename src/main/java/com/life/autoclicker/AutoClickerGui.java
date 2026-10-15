package com.life.autoclicker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;

@SideOnly(Side.CLIENT)
public class AutoClickerGui extends GuiScreen {

    private final AutoClickerMod mod;
    private static final ResourceLocation LOGO = new ResourceLocation("saicoautoclicker", "textures/gui/saico_logo.png");
    private static final int LOGO_TEX_W = 886;
    private static final int LOGO_TEX_H = 657;

    private static final int PANEL_W = 360;
    private static final int PAD = 16;
    private static final int TOP = 12;
    private static final int ROW_H = 22;
    private static final int ROW_GAP = 6;

    private int panelX;
    private int panelY;
    private int panelH;
    private int logoDrawW;
    private int logoDrawH;

    private CPSSlider cpsSlider;
    private CustomGuiButton modeButton;
    private CustomGuiButton actionBarButton;
    private CustomGuiButton bindToggleKeyButton;
    private CustomGuiButton bindGuiKeyButton;
    private CustomGuiButton bindModeKeyButton;
    private CustomGuiButton keybindsToggle;
    private CustomGuiButton statusButton;
    private CustomGuiButton muteButton;
    private CustomGuiButton backButton;
    private CustomGuiButton soundMuteButton;
    private CustomGuiButton customButton;
    private CustomGuiButton customAddButton;
    private CustomGuiButton[] soundRows;
    private CustomGuiButton[] soundDeleteButtons;
    private CustomGuiButton closeButton;

    private enum Page { MAIN, SOUND }
    private Page page = Page.MAIN;
    private int soundScroll;
    private float soundScrollVisual;
    private int[] soundOrder;
    private int soundWindow = 3;
    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private int soundLabelX;
    private int soundLabelY;
    private int clickLabelX;
    private int clickLabelY;
    private boolean scrollDrag;
    private boolean pickingSound;
    private Page pendingPage;
    private float pageBlend;
    private boolean closing;

    private enum KeybindTarget { TOGGLE, GUI, MODE }
    private KeybindTarget waitingForBind;
    private boolean keybindsOpen = true;
    private float fold;
    private float rowPhase;
    private int headerH;
    private int keybindLabelX;
    private int keybindLabelY;
    private int titleX;
    private int titleY;

    public AutoClickerGui(AutoClickerMod mod) {
        this.mod = mod;
        this.keybindsOpen = !mod.isKeybindsHidden();
        this.fold = this.keybindsOpen ? 0f : 1f;
        this.rowPhase = this.keybindsOpen ? 1f : 0f;
        CustomGuiButton.setUiMuted(mod.uiSoundsMuted);
    }

    @Override
    public void initGui() {
        buttonList.clear();
        SaicoGuiTheme.beginIntro();
        waitingForBind = null;
        mod.setKeybindsSuspended(false);

        int control = Math.max(18, SaicoFont.height() + 6);
        logoDrawH = 50;
        logoDrawW = logoDrawH * LOGO_TEX_W / LOGO_TEX_H;
        headerH = TOP + Math.max(logoDrawH, SaicoFont.height() + 4 + control) + 10;
        int openBody = labelStride() + ROW_H + ROW_GAP + ROW_H + ROW_GAP + ROW_H
                + 16 + labelStride() + 14
                + 16 + ROW_H
                + ROW_GAP + ROW_H
                + 16;
        if (headerH + openBody > height - 8) {
            logoDrawH = Math.max(36, logoDrawH - ((headerH + openBody) - (height - 8)));
            logoDrawW = logoDrawH * LOGO_TEX_W / LOGO_TEX_H;
            headerH = TOP + Math.max(logoDrawH, SaicoFont.height() + 4 + control) + 10;
        }

        int contentW = PANEL_W - PAD * 2;
        closeButton = new CustomGuiButton(102, 0, 0, control, control, "");
        closeButton.setCloseIcon(true);
        closeButton.setCornerRadius(5);
        buttonList.add(closeButton);

        muteButton = new CustomGuiButton(9, 0, 0, control, control, "");
        muteButton.setMuteIcon(true);
        muteButton.setCornerRadius(5);
        buttonList.add(muteButton);

        int statusW = SaicoFont.width("Inactive") + 26;
        statusButton = new CustomGuiButton(8, 0, 0, statusW, control, "Inactive");
        statusButton.setCornerRadius(-1);
        buttonList.add(statusButton);

        float hideScale = 0.84f;
        int hideW = Math.round(Math.max(SaicoFont.widthF("Hide"), SaicoFont.widthF("Show")) * hideScale) + 8;
        int hideH = Math.round(SaicoFont.height() * hideScale) + 2;
        keybindsToggle = new CustomGuiButton(7, 0, 0, hideW, hideH, "Hide");
        keybindsToggle.setTextScale(hideScale);
        keybindsToggle.setCornerRadius(-1);
        keybindsToggle.setTextColor(SaicoGuiTheme.TEXT_DIM);
        buttonList.add(keybindsToggle);

        bindGuiKeyButton = new CustomGuiButton(5, 0, 0, contentW, ROW_H, "Open Menu");
        bindGuiKeyButton.setValueText(keyName(mod.guiKey));
        buttonList.add(bindGuiKeyButton);

        bindToggleKeyButton = new CustomGuiButton(4, 0, 0, contentW, ROW_H, "Toggle Clicker");
        bindToggleKeyButton.setValueText(keyName(mod.toggleKey));
        buttonList.add(bindToggleKeyButton);

        bindModeKeyButton = new CustomGuiButton(6, 0, 0, contentW, ROW_H, "Click Mode");
        bindModeKeyButton.setValueText(keyName(mod.modeKey));
        buttonList.add(bindModeKeyButton);

        int currentCPS = Math.max(1, Math.min(mod.leftClickCPS, 10));
        mod.leftClickCPS = currentCPS;
        cpsSlider = new CPSSlider(1, 0, 0, contentW, 14, 1, 10, currentCPS);
        buttonList.add(cpsSlider);

        modeButton = new CustomGuiButton(2, 0, 0, contentW, ROW_H, "Click Mode");
        modeButton.setValueText(modeLabel());
        buttonList.add(modeButton);

        actionBarButton = new CustomGuiButton(3, 0, 0, contentW, ROW_H, "Action Bar");
        actionBarButton.setValueText(actionBarLabel());
        buttonList.add(actionBarButton);

        backButton = new CustomGuiButton(29, 0, 0, control, control, "");
        backButton.setBackIcon(true);
        backButton.setCornerRadius(5);
        buttonList.add(backButton);

        soundMuteButton = new CustomGuiButton(26, 0, 0, contentW, ROW_H, "Mute");
        soundMuteButton.setValueText("Off");
        buttonList.add(soundMuteButton);

        soundRows = new CustomGuiButton[4];
        for (int i = 0; i < soundRows.length; i++) {
            CustomGuiButton row = new CustomGuiButton(20 + i, 0, 0, contentW, ROW_H, "");
            row.setLeftAlign(true);
            row.setSilentPress(true);
            soundRows[i] = row;
            buttonList.add(row);
        }

        customButton = new CustomGuiButton(27, 0, 0, contentW, ROW_H, "Custom");
        customButton.setValueText("None");
        customButton.setSilentPress(true);
        buttonList.add(customButton);

        int deleteW = SaicoFont.width("Delete") + 16;
        soundDeleteButtons = new CustomGuiButton[soundRows.length];
        for (int i = 0; i < soundDeleteButtons.length; i++) {
            CustomGuiButton delete = new CustomGuiButton(30 + i, 0, 0, deleteW, ROW_H, "Delete");
            delete.setTextColor(SaicoGuiTheme.RED);
            delete.setSilentPress(true);
            soundDeleteButtons[i] = delete;
            buttonList.add(delete);
        }

        int addW = Math.max(SaicoFont.width("Add"), SaicoFont.width("Change")) + 16;
        customAddButton = new CustomGuiButton(28, 0, 0, addW, ROW_H, "Add");
        customAddButton.setSilentPress(true);
        buttonList.add(customAddButton);

        layout();
        refreshSwitches();
        updateKeybindConflictStyles();
    }

    private void layout() {
        int mainH = mainPanelHeight();
        int soundH = soundPanelHeight();
        float resize = pageResize();
        panelH = Math.round(mainH + (soundH - mainH) * resize);
        panelX = (width - PANEL_W) / 2;
        panelY = (height - panelH) / 2;
        layoutMain();
        layoutSound();
        applyPageFade();
    }

    private float pageResize() {
        float t = pageBlend;
        if (t <= 0.32f) {
            return 0f;
        }
        if (t >= 0.68f) {
            return 1f;
        }
        float u = (t - 0.32f) / 0.36f;
        return u * u * (3f - 2f * u);
    }

    private float mainText() {
        if (pageBlend >= 0.32f) {
            return 0f;
        }
        float u = pageBlend / 0.32f;
        u = u * u * (3f - 2f * u);
        return 1f - u;
    }

    private float soundText() {
        if (pageBlend <= 0.68f) {
            return 0f;
        }
        float u = (pageBlend - 0.68f) / 0.32f;
        return u * u * (3f - 2f * u);
    }

    private void requestClose() {
        if (closing) {
            return;
        }
        closing = true;
        if (page == Page.SOUND) {
            SaicoGuiTheme.beginOutro(mainPageCloseSpan(), visibleSlots());
            return;
        }
        float span = 420f;
        for (Object obj : buttonList) {
            if (!(obj instanceof CustomGuiButton)) {
                continue;
            }
            CustomGuiButton button = (CustomGuiButton) obj;
            if (button.visible) {
                span = Math.max(span, SaicoGuiTheme.slotEnd(button.introSlot()));
            }
        }
        if (cpsSlider != null && cpsSlider.visible) {
            span = Math.max(span, SaicoGuiTheme.slotEnd(cpsSlider.introSlot()));
        }
        SaicoGuiTheme.beginOutro(span);
    }

    private float mainPageCloseSpan() {
        float span = 420f;
        CustomGuiButton[] buttons = {
                closeButton, muteButton, statusButton, keybindsToggle,
                bindGuiKeyButton, bindToggleKeyButton, bindModeKeyButton,
                modeButton, actionBarButton
        };
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i] != null) {
                span = Math.max(span, SaicoGuiTheme.slotEnd(buttons[i].introSlot()));
            }
        }
        if (cpsSlider != null) {
            span = Math.max(span, SaicoGuiTheme.slotEnd(cpsSlider.introSlot()));
        }
        return span;
    }

    private int[] visibleSlots() {
        int count = 0;
        for (Object obj : buttonList) {
            if (obj instanceof CustomGuiButton && ((CustomGuiButton) obj).visible) {
                count++;
            }
        }
        int[] slots = new int[count];
        int cursor = 0;
        for (Object obj : buttonList) {
            if (obj instanceof CustomGuiButton && ((CustomGuiButton) obj).visible) {
                slots[cursor++] = ((CustomGuiButton) obj).introSlot();
            }
        }
        for (int i = 1; i < slots.length; i++) {
            int slot = slots[i];
            int j = i;
            while (j > 0 && slots[j - 1] > slot) {
                slots[j] = slots[j - 1];
                j--;
            }
            slots[j] = slot;
        }
        return slots;
    }

    private int labelStride() {
        return SaicoFont.height() + 8;
    }

    private int mainPanelHeight() {
        float shown = keybindSpace();
        int openOffset = labelStride() + (ROW_H + ROW_GAP) * 2 + ROW_H + 16;
        int closedOffset = 18;
        int cpsLabelOffset = closedOffset + Math.round((openOffset - closedOffset) * shown);
        int tail = labelStride() + 14 + 16 + ROW_H + ROW_GAP + ROW_H + 16;
        return headerH + cpsLabelOffset + tail;
    }

    private void stepKeybindFold(float dt) {
        boolean rowsLeaving = !keybindsOpen && rowPhase > 0.02f;
        if (!rowsLeaving) {
            if (!keybindsOpen && fold < 0.42f) {
                fold = 0.42f;
            }
            float rate = 3.6f;
            float span = 1f - (float) Math.exp(-rate);
            float speed = keybindsOpen
                    ? rate * (fold + (1f / span - 1f))
                    : rate * (1f / span - fold);
            if (keybindsOpen) {
                fold = Math.max(0f, fold - speed * dt);
            } else {
                fold = Math.min(1f, fold + speed * dt);
            }
        }
        float rowSpeed = 1.7f;
        if (keybindsOpen) {
            if (fold <= 0.42f) {
                rowPhase = Math.min(1f, rowPhase + rowSpeed * dt);
            }
        } else {
            rowPhase = Math.max(0f, rowPhase - rowSpeed * dt);
        }
    }

    private float keybindSpace() {
        if (fold <= 0.42f) {
            return 1f;
        }
        float u = (fold - 0.42f) / 0.58f;
        u = u * u * (3f - 2f * u);
        return 1f - u;
    }

    private float keybindRowReveal(int row) {
        float start = row * 0.16f;
        float u = (rowPhase - start) / 0.62f;
        if (u <= 0f) {
            return 0f;
        }
        if (u >= 1f) {
            return 1f;
        }
        return u * u * (3f - 2f * u);
    }

    private int soundPanelHeight() {
        int stride = ROW_H + ROW_GAP;
        int chrome = labelStride() + ROW_H + 8 + labelStride() + 8 + ROW_H + 16;
        soundWindow = 3;
        return headerH + chrome + soundWindow * stride - ROW_GAP;
    }

    private void layoutMain() {
        float shown = keybindSpace();
        int openOffset = labelStride() + (ROW_H + ROW_GAP) * 2 + ROW_H + 16;
        int closedOffset = 18;
        int cpsLabelOffset = closedOffset + Math.round((openOffset - closedOffset) * shown);

        int contentX = panelX + PAD;
        int contentW = PANEL_W - PAD * 2;
        closeButton.xPosition = panelX + PANEL_W - PAD - closeButton.width;
        closeButton.yPosition = panelY + TOP;
        int fontH = SaicoFont.height();
        int stack = fontH + 4 + muteButton.height;
        int stackY = panelY + TOP + Math.max(0, (logoDrawH - stack) / 2);
        titleX = contentX + logoDrawW + 10;
        titleY = stackY;
        muteButton.xPosition = titleX;
        muteButton.yPosition = stackY + fontH + 4;
        statusButton.xPosition = muteButton.xPosition + muteButton.width + 6;
        statusButton.yPosition = muteButton.yPosition + (muteButton.height - statusButton.height) / 2;

        int y0 = panelY + headerH;
        keybindLabelX = contentX;
        keybindLabelY = y0;
        keybindsToggle.xPosition = contentX + SaicoFont.width("Keybinds") + 8;
        keybindsToggle.yPosition = y0 + Math.round(SaicoFont.inkCenter("Keybinds") - keybindsToggle.height * 0.5f);

        int rowY = y0 + labelStride();
        placeRow(bindGuiKeyButton, contentX, rowY, contentW, keybindRowReveal(0) > 0.02f);
        placeRow(bindToggleKeyButton, contentX, rowY + ROW_H + ROW_GAP, contentW, keybindRowReveal(1) > 0.02f);
        placeRow(bindModeKeyButton, contentX, rowY + (ROW_H + ROW_GAP) * 2, contentW, keybindRowReveal(2) > 0.02f);

        cpsSlider.xPosition = contentX;
        cpsSlider.yPosition = y0 + cpsLabelOffset + labelStride();
        cpsSlider.width = contentW;
        cpsSlider.visible = true;

        int modeY = cpsSlider.yPosition + 14 + 16;
        placeRow(modeButton, contentX, modeY, contentW, true);
        placeRow(actionBarButton, contentX, modeY + ROW_H + ROW_GAP, contentW, true);
        if (backButton != null) {
            backButton.visible = false;
        }
        statusButton.visible = true;
        muteButton.visible = true;
        keybindsToggle.visible = true;
    }

    private void layoutSound() {
        int stride = ROW_H + ROW_GAP;
        int rows = 3;
        soundWindow = rows;

        closeButton.xPosition = panelX + PANEL_W - PAD - closeButton.width;
        closeButton.yPosition = panelY + TOP;
        backButton.visible = true;
        backButton.xPosition = closeButton.xPosition - 6 - backButton.width;
        backButton.yPosition = closeButton.yPosition;

        int contentX = panelX + PAD;
        int contentW = PANEL_W - PAD * 2;
        int y = panelY + headerH;
        soundLabelX = contentX;
        soundLabelY = y;
        placeRow(soundMuteButton, contentX, y + labelStride(), contentW, true);

        y = soundMuteButton.yPosition + ROW_H + 8;
        clickLabelX = contentX;
        clickLabelY = y;
        listX = contentX;
        listY = y + labelStride();
        listW = contentW - 10;
        listH = rows * stride - ROW_GAP;
        for (int i = 0; i < soundRows.length; i++) {
            if (i < rows) {
                placeRow(soundRows[i], listX, listY + i * stride, listW, true);
            } else {
                soundRows[i].visible = false;
            }
        }

        int addW = customAddButton.width;
        int customY = listY + listH + 8;
        customAddButton.visible = true;
        customAddButton.xPosition = contentX + contentW - addW;
        customAddButton.yPosition = customY;
        placeRow(customButton, contentX, customY, contentW - addW - 6, true);
        refreshSoundRows();
    }

    private boolean soundContentLive() {
        return soundText() > 0.02f;
    }

    private void applyPageFade() {
        float main = mainText();
        float sound = soundText();
        fade(statusButton, main);
        fade(muteButton, main);
        fade(keybindsToggle, main);
        fade(bindGuiKeyButton, main * keybindRowReveal(0));
        fade(bindToggleKeyButton, main * keybindRowReveal(1));
        fade(bindModeKeyButton, main * keybindRowReveal(2));
        fade(modeButton, main);
        fade(actionBarButton, main);
        if (cpsSlider != null) {
            cpsSlider.setReveal(main);
            if (main < 0.02f) {
                cpsSlider.visible = false;
            }
        }
        fade(backButton, sound);
        fade(soundMuteButton, sound);
        fade(customButton, sound);
        fade(customAddButton, sound);
        if (soundRows != null) {
            for (CustomGuiButton row : soundRows) {
                fade(row, sound);
            }
        }
        if (soundDeleteButtons != null) {
            for (CustomGuiButton row : soundDeleteButtons) {
                fade(row, sound);
            }
        }
        if (closeButton != null) {
            closeButton.setReveal(1f);
        }
    }

    private void fade(CustomGuiButton button, float reveal) {
        if (button == null) {
            return;
        }
        button.setReveal(reveal);
        if (reveal < 0.02f) {
            button.visible = false;
        }
    }

    private void openSoundPage() {
        if (waitingForBind != null) {
            endKeyCapture();
            mod.setKeybindsSuspended(false);
        }
        page = Page.SOUND;
        layout();
        pinSoundOrder();
        soundScroll = 0;
        soundScrollVisual = 0f;
        refreshSoundRows();
    }

    private void pinSoundOrder() {
        int count = SaicoSounds.count();
        int selected = SaicoSounds.selectedIndex();
        soundOrder = new int[count];
        int cursor = 0;
        if (selected >= 0 && selected < count) {
            soundOrder[cursor++] = selected;
        }
        for (int i = 0; i < count; i++) {
            if (i != selected) {
                soundOrder[cursor++] = i;
            }
        }
    }

    private int libraryIndexAt(int displayIndex) {
        if (soundOrder == null || displayIndex < 0 || displayIndex >= soundOrder.length) {
            return -1;
        }
        return soundOrder[displayIndex];
    }

    private void rememberAddedSound(int libraryIndex) {
        if (soundOrder == null) {
            pinSoundOrder();
            return;
        }
        int[] next = new int[soundOrder.length + 1];
        System.arraycopy(soundOrder, 0, next, 0, soundOrder.length);
        next[soundOrder.length] = libraryIndex;
        soundOrder = next;
    }

    private void rememberRemovedSound(int libraryIndex) {
        if (soundOrder == null || soundOrder.length == 0) {
            return;
        }
        int[] next = new int[soundOrder.length - 1];
        int cursor = 0;
        for (int value : soundOrder) {
            if (value == libraryIndex) {
                continue;
            }
            if (value > libraryIndex) {
                value--;
            }
            if (cursor < next.length) {
                next[cursor++] = value;
            }
        }
        soundOrder = next;
    }

    private int displayedIndex(int slot) {
        if (slot < 0) {
            return -1;
        }
        return (int) Math.floor(soundScrollVisual) + slot;
    }

    private int soundScrollLimit() {
        return Math.max(0, SaicoSounds.count() - soundWindow);
    }

    private void refreshSoundRows() {
        if (soundRows == null) {
            return;
        }
        mod.uiSoundsMuted = SaicoSounds.isMuted();
        CustomGuiButton.setUiMuted(mod.uiSoundsMuted);
        if (muteButton != null) {
            muteButton.setSoundOff(mod.uiSoundsMuted);
        }
        if (soundMuteButton != null) {
            boolean silenced = SaicoSounds.isMuted();
            soundMuteButton.displayString = "Mute";
            soundMuteButton.setValueText(silenced ? "On" : "Off");
            soundMuteButton.setValueColor(silenced ? SaicoGuiTheme.RED : SaicoGuiTheme.GREEN);
        }
        int count = SaicoSounds.count();
        int stride = ROW_H + ROW_GAP;
        int base = (int) Math.floor(soundScrollVisual);
        float frac = soundScrollVisual - base;
        CustomGuiButton.setListClipRect(listX, listY, listW, listH);
        for (int i = 0; i < soundRows.length; i++) {
            int index = base + i;
            CustomGuiButton row = soundRows[i];
            CustomGuiButton delete = soundDeleteButtons == null ? null : soundDeleteButtons[i];
            int rowY = Math.round(listY + (i - frac) * stride);
            boolean inList = rowY + ROW_H > listY && rowY < listY + listH;
            if (!soundContentLive() || index < 0 || index >= count || !inList) {
                row.visible = false;
                row.setListClip(false);
                if (delete != null) {
                    delete.visible = false;
                    delete.setListClip(false);
                }
                continue;
            }
            int library = libraryIndexAt(index);
            if (library < 0) {
                row.visible = false;
                row.setListClip(false);
                if (delete != null) {
                    delete.visible = false;
                    delete.setListClip(false);
                }
                continue;
            }
            boolean custom = SaicoSounds.isCustomId(SaicoSounds.idAt(library));
            int rowW = listW;
            if (custom && delete != null) {
                rowW = listW - delete.width - 6;
            }
            placeRow(row, listX, rowY, rowW, true);
            row.setListClip(true);
            row.setPlainText(SaicoSounds.labelAt(library));
            row.setSelected(library == SaicoSounds.selectedIndex());
            if (delete != null) {
                if (custom) {
                    placeRow(delete, listX + listW - delete.width, row.yPosition, delete.width, true);
                    delete.setListClip(true);
                } else {
                    delete.visible = false;
                    delete.setListClip(false);
                }
            }
        }
        if (customButton != null) {
            String name = SaicoSounds.customName();
            boolean empty = name.length() == 0;
            if (name.length() > 22) {
                name = name.substring(0, 20) + "...";
            }
            int addW = customAddButton == null ? 0 : customAddButton.width;
            int gap = 6;
            int customY = listY + listH + 8;
            int contentRight = listX + listW + 10;
            if (customAddButton != null) {
                customAddButton.visible = soundContentLive();
                customAddButton.xPosition = contentRight - addW;
                customAddButton.yPosition = customY;
            }
            placeRow(customButton, listX, customY, contentRight - listX - addW - gap, soundContentLive());
            customButton.displayString = "Custom Sound";
            customButton.setValueText(empty ? "None" : name);
            customButton.setValueColor(empty ? SaicoGuiTheme.TEXT_DIM : SaicoGuiTheme.TEXT);
            customButton.setSelected(SaicoSounds.isCustomSelected());
        }
        if (customAddButton != null) {
            customAddButton.displayString = SaicoSounds.hasCustom() ? "Change" : "Add";
        }
    }

    private void drawSoundScrollbar() {
        if (soundScrollLimit() <= 0 || listH <= 0) {
            return;
        }
        int trackX = listX + listW + 4;
        int max = soundScrollLimit();
        int thumbH = Math.max(14, listH * soundWindow / SaicoSounds.count());
        int thumbY = listY + Math.round((listH - thumbH) * soundScrollVisual / Math.max(1, max));
        float text = SaicoGuiTheme.contentFade() * soundText();
        int track = SaicoGuiTheme.scaleAlpha(SaicoGuiTheme.LINE, text);
        int thumb = SaicoGuiTheme.scaleAlpha(SaicoGuiTheme.PURPLE, text);
        drawRect(trackX, listY, trackX + 2, listY + listH, track);
        drawRect(trackX, thumbY, trackX + 2, thumbY + thumbH, thumb);
    }

    private boolean hitScrollbar(int mouseX, int mouseY) {
        if (page != Page.SOUND || soundScrollLimit() <= 0) {
            return false;
        }
        int trackX = listX + listW + 4;
        return mouseX >= trackX - 4 && mouseX <= trackX + 8 && mouseY >= listY && mouseY < listY + listH;
    }

    private void scrollToMouse(int mouseY) {
        int max = soundScrollLimit();
        float along = (mouseY - listY) / (float) Math.max(1, listH);
        soundScroll = Math.max(0, Math.min(max, Math.round(along * max)));
        soundScrollVisual = soundScroll;
        refreshSoundRows();
    }

    private boolean isSoundDelete(GuiButton button) {
        if (soundDeleteButtons == null) {
            return false;
        }
        for (CustomGuiButton delete : soundDeleteButtons) {
            if (delete == button) {
                return true;
            }
        }
        return false;
    }

    private int soundDeleteIndex(GuiButton button) {
        for (int i = 0; i < soundDeleteButtons.length; i++) {
            if (soundDeleteButtons[i] == button) {
                return i;
            }
        }
        return -1;
    }

    private boolean isSoundRow(GuiButton button) {
        if (soundRows == null) {
            return false;
        }
        for (CustomGuiButton row : soundRows) {
            if (row == button) {
                return true;
            }
        }
        return false;
    }

    private int soundRowIndex(GuiButton button) {
        for (int i = 0; i < soundRows.length; i++) {
            if (soundRows[i] == button) {
                return i;
            }
        }
        return -1;
    }

    private void pickCustomSound() {
        if (pickingSound) {
            return;
        }
        pickingSound = true;
        final Minecraft client = mc;
        new Thread(new Runnable() {
            @Override
            public void run() {
                final File[] chosen = new File[1];
                try {
                    EventQueue.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            chosen[0] = SaicoSounds.chooseFile();
                        }
                    });
                } catch (Throwable ignored) {
                    chosen[0] = null;
                }
                final File file = chosen[0];
                client.addScheduledTask(new Runnable() {
                    @Override
                    public void run() {
                        pickingSound = false;
                        if (file == null) {
                            return;
                        }
                        int index = SaicoSounds.importCustom(file);
                        if (index < 0) {
                            return;
                        }
                        rememberAddedSound(index);
                        refreshSoundRows();
                        SaicoSounds.preview(client.getSoundHandler());
                    }
                });
            }
        }, "Saico-Sound-Picker").start();
    }

    private static void placeRow(CustomGuiButton button, int x, int y, int w, boolean visible) {
        button.xPosition = x;
        button.yPosition = y;
        button.width = w;
        button.visible = visible;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        CustomGuiButton.beginFrame();
        if (waitingForBind == null) {
            mod.setKeybindsSuspended(false);
        }
        float dt = CustomGuiButton.frameDeltaSeconds();
        if (SaicoGuiTheme.outroDone()) {
            mc.displayGuiScreen(null);
            return;
        }
        stepKeybindFold(dt);
        if (scrollDrag) {
            soundScrollVisual = soundScroll;
        } else {
            soundScrollVisual = SaicoGuiTheme.approach(soundScrollVisual, soundScroll, 16f, CustomGuiButton.frameDeltaSeconds());
        }
        pageBlend = SaicoGuiTheme.approach(pageBlend, page == Page.SOUND ? 1f : 0f, 3.2f, CustomGuiButton.frameDeltaSeconds());
        layout();
        float fade = SaicoGuiTheme.introFade();
        SaicoGuiTheme.contentAlpha = fade;

        drawRect(0, 0, width, height, SaicoGuiTheme.scaleAlpha(SaicoGuiTheme.BACKDROP, fade));
        GlStateManager.pushMatrix();
        GlStateManager.translate(0f, (1f - fade) * 8f, 0f);
        SaicoGuiTheme.fillRound(panelX, panelY, PANEL_W, panelH, SaicoGuiTheme.PANEL, 12);
        SaicoGuiTheme.strokeRound(panelX, panelY, PANEL_W, panelH, SaicoGuiTheme.FRAME, 12);
        SaicoGuiTheme.chaseBorder(panelX, panelY, PANEL_W, panelH, 12);

        drawLogo();
        drawChosenClick();
        if (cpsSlider != null) {
            cpsSlider.animate(mod.currentMode == AutoClickerMod.Mode.RIGHT);
        }
        drawSectionLabels();
        refreshSwitches();

        updateKeybindConflictStyles();
        SaicoGuiTheme.contentAlpha = 1f;
        super.drawScreen(mouseX, mouseY, partialTicks);
        GlStateManager.popMatrix();
    }

    private void drawLogo() {
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        int x = logoX();
        int y = panelY + TOP;
        float fade = SaicoGuiTheme.contentFade();
        long now = System.currentTimeMillis();
        float[] motion = logoSweep(now);
        float along = motion[0];
        float sweep = motion[1];
        float pulse = motion[2];
        float settle = (0.94f + 0.06f * fade) * (1f + pulse * 0.028f);

        float eyeGlow = (0.12f + pulse * 0.38f) * fade;
        SaicoGuiTheme.hoverSpot(x + logoDrawW * 0.3f, y + logoDrawH * 0.48f, logoDrawH * 0.55f, eyeGlow);

        GlStateManager.color(1f, 1f, 1f, fade);
        mc.getTextureManager().bindTexture(LOGO);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + logoDrawW / 2f, y + logoDrawH / 2f, 0f);
        GlStateManager.scale(settle * logoDrawW / (float) LOGO_TEX_W, settle * logoDrawH / (float) LOGO_TEX_H, 1f);
        GlStateManager.translate(-LOGO_TEX_W / 2f, -LOGO_TEX_H / 2f, 0f);
        drawModalRectWithCustomSizedTexture(0, 0, 0, 0, LOGO_TEX_W, LOGO_TEX_H, LOGO_TEX_W, LOGO_TEX_H);
        GlStateManager.popMatrix();

        drawLogoSheen(x, y, fade, settle, along, sweep);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private float[] logoSweep(long now) {
        int travelMs = 1200;
        long cycle = now % (travelMs + 550);
        if (cycle >= travelMs) {
            return new float[] { 1f, 0f, 0f };
        }
        float u = cycle / (float) travelMs;
        float along = u * u * (3f - 2f * u);
        float edge = 0.18f;
        float sweep = 1f;
        if (u < edge) {
            sweep = u / edge;
        } else if (u > 1f - edge) {
            sweep = (1f - u) / edge;
        }
        sweep = sweep * sweep * (3f - 2f * sweep);
        float bell = (float) Math.sin(u * Math.PI);
        float pulse = bell * bell;
        return new float[] { along, sweep, pulse };
    }

    private void drawLogoSheen(int x, int y, float fade, float settle, float along, float sweep) {
        sweep *= fade;
        if (sweep < 0.02f || fade < 0.2f) {
            return;
        }

        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
        mc.getTextureManager().bindTexture(LOGO);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + logoDrawW / 2f, y + logoDrawH / 2f, 0f);
        GlStateManager.scale(settle * logoDrawW / (float) LOGO_TEX_W, settle * logoDrawH / (float) LOGO_TEX_H, 1f);
        GlStateManager.translate(-LOGO_TEX_W / 2f, -LOGO_TEX_H / 2f, 0f);

        int strips = 56;
        for (int i = 0; i < strips; i++) {
            float mid = (i + 0.5f) / strips;
            float dist = Math.abs(mid - along);
            float hot = Math.max(0f, 1f - dist / 0.13f);
            hot = hot * hot * sweep;
            if (hot < 0.03f) {
                continue;
            }
            int texX = LOGO_TEX_W * i / strips;
            int texW = LOGO_TEX_W * (i + 1) / strips - texX;
            if (texW <= 0) {
                continue;
            }
            GlStateManager.color(0.97f, 0.93f, 1f, hot);
            drawModalRectWithCustomSizedTexture(texX, 0, texX, 0, texW, LOGO_TEX_H, LOGO_TEX_W, LOGO_TEX_H);
        }

        GlStateManager.popMatrix();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private int logoX() {
        int left = panelX + PAD;
        int centered = panelX + (PANEL_W - logoDrawW) / 2;
        int occupied = closeButton.width + 6 + backButton.width;
        int limit = panelX + PANEL_W - PAD - occupied - 10 - logoDrawW;
        if (limit < left) {
            limit = left;
        }
        if (centered > limit) {
            centered = limit;
        }
        return Math.round(left + (centered - left) * pageResize());
    }

    private void drawChosenClick() {
        if (mainText() < 0.02f || muteButton == null || !muteButton.visible) {
            return;
        }
        int right = closeButton.xPosition - 8;
        String name = fitText(SaicoSounds.selectedLabel(), right - titleX);
        if (name.length() == 0) {
            return;
        }
        drawWithButton(name, titleX, titleY, SaicoGuiTheme.TEXT, muteButton.introSlot(), mainText());
    }

    private String fitText(String text, int maxWidth) {
        if (text == null || maxWidth <= 0) {
            return "";
        }
        if (SaicoFont.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisWidth = SaicoFont.width(ellipsis);
        if (maxWidth <= ellipsisWidth) {
            return "";
        }
        int end = text.length();
        while (end > 0 && SaicoFont.width(text.substring(0, end)) + ellipsisWidth > maxWidth) {
            end--;
        }
        return end == 0 ? "" : text.substring(0, end) + ellipsis;
    }

    private void drawSectionLabels() {
        float main = mainText();
        float sound = soundText();
        if (sound > 0.02f && soundMuteButton != null) {
            drawWithButton("Sound", soundLabelX, soundLabelY, SaicoGuiTheme.TEXT_DIM, soundMuteButton.introSlot(), sound);
            int clickSlot = soundRows != null && soundRows.length > 0 ? soundRows[0].introSlot() : soundMuteButton.introSlot();
            drawWithButton("Click Sound", clickLabelX, clickLabelY, SaicoGuiTheme.TEXT_DIM, clickSlot, sound);
            drawSoundScrollbar();
        }
        if (main > 0.02f && keybindsToggle != null) {
            drawWithButton("Keybinds", keybindLabelX, keybindLabelY, SaicoGuiTheme.TEXT_DIM, keybindsToggle.introSlot(), main);
        }
        if (main > 0.02f && cpsSlider != null) {
            int cpsColor = SaicoGuiTheme.lerpColor(SaicoGuiTheme.TEXT_DIM, 0xFF5A5A5A, cpsSlider.dimAmount());
            drawWithButton("Click Rate (CPS)", cpsSlider.xPosition, cpsSlider.yPosition - labelStride(), cpsColor, cpsSlider.introSlot(), main);
        }
    }

    private void drawWithButton(String text, int x, int y, int color, int slot, float page) {
        float intro = SaicoGuiTheme.appearFor(slot);
        float appear = page * intro;
        if (text == null || text.length() == 0 || appear < 0.02f) {
            return;
        }
        int drawY = y + Math.round((1f - intro) * 6f);
        CustomGuiButton.drawAlphaText(fontRendererObj, text, x, drawY, SaicoGuiTheme.scaleAlpha(color, appear));
    }

    private String modeLabel() {
        return mod.currentMode == AutoClickerMod.Mode.LEFT ? "Left" : "Right";
    }

    private String actionBarLabel() {
        return mod.showActionBar ? "On" : "Off";
    }

    private void refreshSwitches() {
        if (modeButton != null) {
            modeButton.displayString = "Click Mode";
            modeButton.setValueText(modeLabel());
        }
        if (actionBarButton != null) {
            boolean on = mod.showActionBar;
            actionBarButton.displayString = "Action Bar";
            actionBarButton.setValueText(actionBarLabel());
            actionBarButton.setValueColor(on ? SaicoGuiTheme.GREEN : SaicoGuiTheme.RED);
        }
        if (statusButton != null) {
            boolean active = mod.isIntendedEnabled();
            statusButton.displayString = active ? "Active" : "Inactive";
            int state = active ? SaicoGuiTheme.GREEN : SaicoGuiTheme.RED;
            statusButton.setTextColor(state);
            statusButton.setDotColor(state);
        }
        if (bindGuiKeyButton != null) {
            bindGuiKeyButton.setListening(waitingForBind == KeybindTarget.GUI);
        }
        if (bindToggleKeyButton != null) {
            bindToggleKeyButton.setListening(waitingForBind == KeybindTarget.TOGGLE);
        }
        if (bindModeKeyButton != null) {
            bindModeKeyButton.setListening(waitingForBind == KeybindTarget.MODE);
        }
        if (keybindsToggle != null) {
            keybindsToggle.displayString = keybindsOpen ? "Hide" : "Show";
        }
        if (muteButton != null) {
            muteButton.setSoundOff(mod.uiSoundsMuted);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == closeButton) {
            requestClose();
        } else if (button == muteButton) {
            pendingPage = Page.SOUND;
        } else if (button == backButton) {
            pendingPage = Page.MAIN;
        } else if (button == soundMuteButton) {
            SaicoSounds.setMuted(!SaicoSounds.isMuted());
            mod.uiSoundsMuted = SaicoSounds.isMuted();
            refreshSwitches();
            refreshSoundRows();
            if (!SaicoSounds.isMuted()) {
                SaicoSounds.playUi(mc.getSoundHandler());
            }
        } else if (button == customAddButton) {
            pickCustomSound();
        } else if (isSoundDelete(button)) {
            int library = libraryIndexAt(displayedIndex(soundDeleteIndex(button)));
            if (library >= 0 && SaicoSounds.deleteCustom(SaicoSounds.idAt(library))) {
                rememberRemovedSound(library);
                if (soundScroll > soundScrollLimit()) {
                    soundScroll = soundScrollLimit();
                }
                refreshSoundRows();
            }
        } else if (button == customButton) {
            if (SaicoSounds.hasCustom()) {
                SaicoSounds.selectCustom();
                customButton.pulse();
                refreshSoundRows();
                SaicoSounds.preview(mc.getSoundHandler());
            } else {
                pickCustomSound();
            }
        } else if (isSoundRow(button)) {
            int slot = soundRowIndex(button);
            int library = libraryIndexAt(displayedIndex(slot));
            if (library >= 0) {
                SaicoSounds.selectLibrary(library);
                if (slot >= 0 && slot < soundRows.length) {
                    soundRows[slot].pulse();
                }
                refreshSoundRows();
                SaicoSounds.preview(mc.getSoundHandler());
            }
        } else if (button == statusButton) {
            mod.toggleIntended();
            refreshSwitches();
        } else if (button == modeButton) {
            mod.toggleClickMode();
            refreshSwitches();
        } else if (button == actionBarButton) {
            mod.setShowActionBar(!mod.showActionBar);
            if (!mod.showActionBar) {
                mod.clearActionBar();
            }
            refreshSwitches();
        } else if (button == bindToggleKeyButton) {
            beginKeyCapture(KeybindTarget.TOGGLE);
        } else if (button == bindGuiKeyButton) {
            beginKeyCapture(KeybindTarget.GUI);
        } else if (button == bindModeKeyButton) {
            beginKeyCapture(KeybindTarget.MODE);
        } else if (button == keybindsToggle) {
            if (waitingForBind != null) {
                endKeyCapture();
                mod.setKeybindsSuspended(false);
            }
            keybindsOpen = !keybindsOpen;
            mod.setKeybindsHidden(!keybindsOpen);
            refreshSwitches();
        }
    }

    private void beginKeyCapture(KeybindTarget target) {
        waitingForBind = target;
        mod.setKeybindsSuspended(true);
        CustomGuiButton row = target == KeybindTarget.TOGGLE ? bindToggleKeyButton
                : target == KeybindTarget.GUI ? bindGuiKeyButton : bindModeKeyButton;
        if (row != null) {
            row.setValueText("Press a key");
            row.setListening(true);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (closing) {
            return;
        }
        if (waitingForBind == null && keyCode == Keyboard.KEY_ESCAPE && page != Page.SOUND) {
            requestClose();
            return;
        }
        if (page == Page.SOUND && waitingForBind == null && keyCode == Keyboard.KEY_ESCAPE) {
            page = Page.MAIN;
            layout();
            refreshSwitches();
            return;
        }
        if (waitingForBind != null) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                if (waitingForBind == KeybindTarget.TOGGLE && mod.toggleKey != null) {
                    mod.toggleKey.setKeyCode(0);
                } else if (waitingForBind == KeybindTarget.GUI && mod.guiKey != null) {
                    mod.guiKey.setKeyCode(0);
                } else if (waitingForBind == KeybindTarget.MODE && mod.modeKey != null) {
                    mod.modeKey.setKeyCode(0);
                }
                saveKeybinds();
                endKeyCapture();
                return;
            }
            finishKeybindWithKeyCode(keyCode);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void finishKeybindWithKeyCode(int keyCode) {
        if (waitingForBind == KeybindTarget.TOGGLE && mod.toggleKey != null) {
            mod.toggleKey.setKeyCode(keyCode);
        } else if (waitingForBind == KeybindTarget.GUI && mod.guiKey != null) {
            mod.guiKey.setKeyCode(keyCode);
        } else if (waitingForBind == KeybindTarget.MODE && mod.modeKey != null) {
            mod.modeKey.setKeyCode(keyCode);
        }
        saveKeybinds();
        endKeyCapture();
    }

    private void endKeyCapture() {
        waitingForBind = null;
        refreshKeybindLabels();
    }

    private void saveKeybinds() {
        net.minecraft.client.settings.KeyBinding.resetKeyBindingArrayAndHash();
        if (mc != null && mc.gameSettings != null) {
            mc.gameSettings.saveOptions();
        }
    }

    @Override
    public void onGuiClosed() {
        waitingForBind = null;
        mod.setKeybindsSuspended(false);
        super.onGuiClosed();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (closing) {
            return;
        }
        if (waitingForBind != null) {
            finishKeybindWithKeyCode(-100 + mouseButton);
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (pendingPage != null) {
            Page next = pendingPage;
            pendingPage = null;
            if (next == Page.SOUND) {
                openSoundPage();
            } else {
                page = Page.MAIN;
                layout();
                refreshSwitches();
            }
        }
        if (cpsSlider != null && cpsSlider.dragging) {
            mod.setLeftClickCPS(cpsSlider.getValue());
        }
        if (page == Page.SOUND && hitScrollbar(mouseX, mouseY)) {
            scrollDrag = true;
            scrollToMouse(mouseY);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (cpsSlider != null) {
            cpsSlider.mouseReleased(mouseX, mouseY);
        }
        scrollDrag = false;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (cpsSlider != null && cpsSlider.dragging && !cpsSlider.isDimmed()) {
            cpsSlider.updateSlider(mouseX);
            mod.setLeftClickCPS(cpsSlider.getValue());
        }
        if (scrollDrag) {
            scrollToMouse(mouseY);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (page != Page.SOUND) {
            return;
        }
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) {
            return;
        }
        int max = soundScrollLimit();
        soundScroll = wheel > 0 ? Math.max(0, soundScroll - 1) : Math.min(max, soundScroll + 1);
        refreshSoundRows();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void refreshKeybindLabels() {
        if (bindToggleKeyButton != null) {
            bindToggleKeyButton.setValueText(keyName(mod.toggleKey));
        }
        if (bindGuiKeyButton != null) {
            bindGuiKeyButton.setValueText(keyName(mod.guiKey));
        }
        if (bindModeKeyButton != null) {
            bindModeKeyButton.setValueText(keyName(mod.modeKey));
        }
        updateKeybindConflictStyles();
    }

    private static String keyName(net.minecraft.client.settings.KeyBinding binding) {
        if (binding == null) return "None";
        int code = binding.getKeyCode();
        if (code == 0) return "None";
        if (code < 0) {
            int btn = code + 100;
            if (btn == 0) return "LMB";
            if (btn == 1) return "RMB";
            if (btn == 2) return "MMB";
            return "Mouse " + (btn + 1);
        }
        if (code > 255) return "Key " + code;
        try {
            String name = Keyboard.getKeyName(code);
            return name == null ? ("Key " + code) : name;
        } catch (ArrayIndexOutOfBoundsException e) {
            return "Key " + code;
        }
    }

    private void updateKeybindConflictStyles() {
        if (bindToggleKeyButton != null) {
            bindToggleKeyButton.setConflict(hasKeybindConflict(mod.toggleKey));
        }
        if (bindGuiKeyButton != null) {
            bindGuiKeyButton.setConflict(hasKeybindConflict(mod.guiKey));
        }
        if (bindModeKeyButton != null) {
            bindModeKeyButton.setConflict(hasKeybindConflict(mod.modeKey));
        }
    }

    private boolean hasKeybindConflict(net.minecraft.client.settings.KeyBinding binding) {
        if (binding == null || mc == null || mc.gameSettings == null) return false;
        int code = binding.getKeyCode();
        if (code == 0) return false;

        int matches = 0;
        net.minecraft.client.settings.KeyBinding[] all = mc.gameSettings.keyBindings;
        if (all == null) return false;
        for (net.minecraft.client.settings.KeyBinding kb : all) {
            if (kb != null && kb.getKeyCode() == code) {
                matches++;
                if (matches > 1) return true;
            }
        }
        return false;
    }

    private static class CPSSlider extends GuiButton {
        private final int min;
        private final int max;
        private final int introSlot;
        private int value;
        private float shown;
        private float target;
        private boolean dragging;
        private boolean dimmed;
        private float hoverAnim;
        private float dimAnim;
        private float reveal = 1f;
        private int shownNumber = -1;
        private int previousNumber = -1;
        private float numberMix = 1f;

        public CPSSlider(int id, int x, int y, int width, int height, int min, int max, int startValue) {
            super(id, x, y, width, height, "");
            this.min = min;
            this.max = max;
            this.value = startValue;
            this.target = percentFor(startValue);
            this.shown = this.target;
            this.introSlot = SaicoGuiTheme.claimSlot();
        }

        int introSlot() {
            return introSlot;
        }

        public int getValue() {
            return value;
        }

        public boolean isDimmed() {
            return dimmed;
        }

        public void animate(boolean inactive) {
            if (inactive && dragging) {
                target = percentFor(value);
                dragging = false;
            }
            dimmed = inactive;
            dimAnim = SaicoGuiTheme.approach(dimAnim, inactive ? 1f : 0f, 10f, CustomGuiButton.frameDeltaSeconds());
        }

        public float dimAmount() {
            return dimAnim;
        }

        public void updateSlider(int mouseX) {
            int trackW = width - 28;
            float percent = (float) (mouseX - this.xPosition) / trackW;
            target = Math.max(0f, Math.min(1f, percent));
            value = min + Math.round(target * (max - min));
        }

        @Override
        public void playPressSound(net.minecraft.client.audio.SoundHandler soundHandler) {
            CustomGuiButton.playUiClick(soundHandler);
        }

        public void setReveal(float reveal) {
            this.reveal = reveal;
        }

        @Override
        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
            if (reveal < 0.9f || dimmed) {
                return false;
            }
            if (super.mousePressed(mc, mouseX, mouseY)) {
                dragging = true;
                updateSlider(mouseX);
                return true;
            }
            return false;
        }

        public void mouseReleased(int mouseX, int mouseY) {
            if (dragging) {
                target = percentFor(value);
            }
            dragging = false;
        }

        private float percentFor(int cps) {
            if (max == min) {
                return 0f;
            }
            return (cps - min) / (float) (max - min);
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!visible || reveal < 0.01f) {
                this.hovered = false;
                return;
            }

            this.hovered = reveal > 0.9f && mouseX >= xPosition && mouseY >= yPosition
                    && mouseX < xPosition + width && mouseY < yPosition + height;
            float dt = CustomGuiButton.frameDeltaSeconds();
            hoverAnim = SaicoGuiTheme.approach(hoverAnim, (hovered || dragging) ? 1f : 0f, 16f, dt);
            float follow = dragging ? 34f : 12f;
            shown = SaicoGuiTheme.approach(shown, target, follow, dt);

            if (shownNumber != value) {
                previousNumber = shownNumber;
                shownNumber = value;
                numberMix = previousNumber < 0 ? 1f : 0f;
            }
            numberMix = SaicoGuiTheme.approach(numberMix, 1f, 14f, dt);

            float intro = SaicoGuiTheme.appearFor(introSlot);
            float appear = intro * reveal;
            float strength = 1f - dimAnim * 0.62f;
            GlStateManager.pushMatrix();
            GlStateManager.translate(0f, (1f - intro) * 6f, 0f);
            float saved = SaicoGuiTheme.contentAlpha;
            SaicoGuiTheme.contentAlpha = appear * strength;

            int trackW = width - 26;
            int trackH = 5;
            int trackY = yPosition + (height - trackH) / 2;
            SaicoGuiTheme.fillRound(xPosition, trackY, trackW, trackH, 0xFF1C1C22, trackH / 2);

            int fillW = Math.round(shown * trackW);
            if (fillW > 1) {
                int fill = SaicoGuiTheme.lerpColor(SaicoGuiTheme.FRAME, SaicoGuiTheme.HOVER, hoverAnim * 0.35f);
                SaicoGuiTheme.fillRound(xPosition, trackY, Math.max(fillW, trackH), trackH, fill, trackH / 2);
            }

            int handleW = 6;
            int handleH = 13;
            int handleX = xPosition + Math.round(shown * trackW) - handleW / 2;
            int handleY = yPosition + (height - handleH) / 2;
            if (hoverAnim > 0.04f) {
                int glowColor = SaicoGuiTheme.scaleAlpha(0x66B14DFF, hoverAnim);
                SaicoGuiTheme.fillRound(handleX - 3, handleY - 2, handleW + 6, handleH + 4, glowColor, (handleW + 6) / 2);
            }
            SaicoGuiTheme.fillRound(handleX, handleY, handleW, handleH, 0xFFF4F4F4, handleW / 2);

            SaicoGuiTheme.contentAlpha = saved;
            int labelColor = SaicoGuiTheme.scaleAlpha(
                    SaicoGuiTheme.lerpColor(SaicoGuiTheme.TEXT, SaicoGuiTheme.TEXT_DIM, dimAnim), appear * strength);
            int numberRight = xPosition + width - 4;
            if (previousNumber >= 0 && numberMix < 0.98f) {
                drawCpsNumber(mc, previousNumber, numberRight, yPosition, height, labelColor, 1f - numberMix);
            }
            if (numberMix >= 0.06f) {
                drawCpsNumber(mc, shownNumber, numberRight, yPosition, height, labelColor, numberMix);
            }
            GlStateManager.popMatrix();
        }

        private void drawCpsNumber(Minecraft mc, int number, int right, int y, int height, int color, float mix) {
            if (mix < 0.06f || number < 0) {
                return;
            }
            String label = String.valueOf(number);
            int labelW = SaicoFont.width(label);
            CustomGuiButton.drawAlphaText(mc.fontRendererObj, label, right - labelW, y + (height - SaicoFont.height()) / 2, SaicoGuiTheme.scaleAlpha(color, mix));
        }
    }
}
