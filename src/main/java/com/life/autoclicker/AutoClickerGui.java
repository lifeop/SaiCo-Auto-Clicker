package com.life.autoclicker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.input.Keyboard;
import java.io.IOException;

@SideOnly(Side.CLIENT)
public class AutoClickerGui extends GuiScreen {

    private final AutoClickerMod mod;
    private static final ResourceLocation LOGO = new ResourceLocation("autoclicker", "textures/gui/saico_logo.png");

    private int panelWidth = 600;
    private int panelHeight = 350;
    private int panelX = 0;
    private int panelY = 0;

    private int sidebarWidth = 200;

    private CPSSlider cpsSlider;
    private CustomGuiButton modeButton;
    private CustomGuiButton actionBarToggleButton;
    private CustomGuiButton bindToggleKeyButton;
    private CustomGuiButton bindGuiKeyButton;
    private CustomGuiButton closeButton;
    
    private String statusText = "";
    private int statusColor = 0xFFAAAAAA;

    private enum KeybindTarget { TOGGLE, GUI }
    private KeybindTarget waitingForBind = null;

    public AutoClickerGui(AutoClickerMod mod) {
        this.mod = mod;
    }

    @Override
    public void initGui() {
        buttonList.clear();

        panelX = (width - panelWidth) / 2;
        int margin = 40;
        int screenAvailableHeight = height - (2 * margin);
        if (panelHeight > screenAvailableHeight) {
            panelHeight = screenAvailableHeight;
        }
        panelY = margin + (screenAvailableHeight - panelHeight) / 2;

        int sidebarButtonWidth = 170;
        int sidebarButtonHeight = 25;
        int sidebarX = panelX + 15;

        closeButton = new CustomGuiButton(102, sidebarX, panelY + panelHeight - 35, sidebarButtonWidth, sidebarButtonHeight, "Close");
        closeButton.setColors(0xFF8B2C3E, 0xFF9B3C4E, 0xFF8B2C3E);
        buttonList.add(closeButton);

        int contentStartX = panelX + sidebarWidth + 30;
        
        int titleHeight = 27;
        int cpsSliderHeight = 20;
        int buttonHeight = 25;
        int keybindGap = 10;
        int keybindSectionHeight = (buttonHeight * 2) + keybindGap;
        int keybindToSliderSpacing = 18;
        int spacing = 40;
        int totalContentHeight = titleHeight + keybindSectionHeight + keybindToSliderSpacing + cpsSliderHeight + spacing + buttonHeight;
        
        int availableHeight = panelHeight - 15;
        int contentStartY = panelY + 15 + titleHeight + (availableHeight - totalContentHeight) / 2;

        int keybindsY = contentStartY;
        bindGuiKeyButton = new CustomGuiButton(5, contentStartX, keybindsY, 300, buttonHeight,
                "Open Auto-Clicker GUI: " + keyName(mod.guiKey));
        bindGuiKeyButton.setColors(0xFF1A2332, 0xFF2C3E50, 0xFF2C5F8F);
        buttonList.add(bindGuiKeyButton);

        bindToggleKeyButton = new CustomGuiButton(4, contentStartX, keybindsY + buttonHeight + keybindGap, 300, buttonHeight,
                "Toggle Auto-Clicker: " + keyName(mod.toggleKey));
        bindToggleKeyButton.setColors(0xFF1A2332, 0xFF2C3E50, 0xFF2C5F8F);
        buttonList.add(bindToggleKeyButton);

        int sliderY = keybindsY + keybindSectionHeight + keybindToSliderSpacing;
        int currentCPS = Math.max(1, Math.min(mod.leftClickCPS, 10));
        mod.leftClickCPS = currentCPS;
        cpsSlider = new CPSSlider(1, contentStartX, sliderY, 300, 20,
                "CPS: ", 1, 10, currentCPS);
        buttonList.add(cpsSlider);

        int buttonWidth = 140;
        int buttonSpacing = 20;
        int buttonsY = sliderY + cpsSliderHeight + spacing;

        modeButton = new CustomGuiButton(2, contentStartX, buttonsY, buttonWidth, buttonHeight,
                "Mode: " + (mod.currentMode == AutoClickerMod.Mode.LEFT ? "Left-Click" : "Right-Click"));
        modeButton.setColors(0xFF1A2332, 0xFF2C3E50, 0xFF2C5F8F);
        buttonList.add(modeButton);

        String actionBarText = "Action Bar: " + (mod.showActionBar ? "Enabled" : "Disabled");
        actionBarToggleButton = new CustomGuiButton(3, contentStartX + buttonWidth + buttonSpacing, buttonsY, buttonWidth, buttonHeight, actionBarText);
        actionBarToggleButton.setColors(0xFF1A2332, 0xFF2C3E50, mod.showActionBar ? 0xFF2C5F8F : 0xFF1A2332);
        actionBarToggleButton.setSelected(mod.showActionBar);
        buttonList.add(actionBarToggleButton);
        
        updateStatusDisplay();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0x80000000);

        drawRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xD81A2332);

        drawRect(panelX, panelY, panelX + panelWidth, panelY + 1, 0xFF2C5F8F);
        drawRect(panelX, panelY + panelHeight - 1, panelX + panelWidth, panelY + panelHeight, 0xFF2C5F8F);
        drawRect(panelX, panelY, panelX + 1, panelY + panelHeight, 0xFF2C5F8F);
        drawRect(panelX + panelWidth - 1, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF2C5F8F);

        drawRect(panelX, panelY, panelX + sidebarWidth, panelY + panelHeight, 0xD81A2332);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(1f, 1f, 1f, 1f);
        mc.getTextureManager().bindTexture(LOGO);
        int logoSize = 100;
        int logoX = panelX + (sidebarWidth - logoSize) / 2;
        int logoY = panelY + 20;
        drawModalRectWithCustomSizedTexture(logoX, logoY, 0, 0, logoSize, logoSize, logoSize, logoSize);

        String title = EnumChatFormatting.BOLD + "AUTO CLICKER";
        int titleWidth = this.fontRendererObj.getStringWidth(title);
        int titleY = logoY + logoSize + 15;
        this.fontRendererObj.drawStringWithShadow(title, panelX + (sidebarWidth - titleWidth) / 2, titleY, 0xFFFFFFFF);

        int separatorY = titleY + 12;
        drawRect(panelX + 15, separatorY, panelX + sidebarWidth - 15, separatorY + 1, 0xFF2C5F8F);

        drawStatusIndicator(separatorY);

        drawRect(panelX + sidebarWidth, panelY, panelX + panelWidth, panelY + panelHeight, 0x800A0E1A);

        drawContentTitle();
        
        updateKeybindConflictStyles();
        drawTooltips(mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawContentTitle() {
        String title = EnumChatFormatting.WHITE + "" + EnumChatFormatting.BOLD + "Settings";
        String subtitle = EnumChatFormatting.GRAY + "Configure auto-clicker options";

        int titleX = panelX + sidebarWidth + 30;
        int titleY = panelY + 15;

        this.fontRendererObj.drawStringWithShadow(title, titleX, titleY, 0xFFFFFFFF);
        this.fontRendererObj.drawString(subtitle, titleX, titleY + 12, 0xFFAAAAAA);
    }
    
    private void updateStatusDisplay() {
        boolean isActive = mod.isIntendedEnabled();
        if (isActive) {
            statusText = EnumChatFormatting.GREEN + "" + EnumChatFormatting.BOLD + "ACTIVE";
            statusColor = 0xFF00FF00;
        } else {
            statusText = EnumChatFormatting.RED + "" + EnumChatFormatting.BOLD + "INACTIVE";
            statusColor = 0xFFFF0000;
        }
    }
    
    private void drawStatusIndicator(int separatorY) {
        updateStatusDisplay();
        
        int statusY = separatorY + 10;
        int statusWidth = sidebarWidth - 30;
        int statusHeight = 25;
        int statusX = panelX + (sidebarWidth - statusWidth) / 2;
        
        drawRect(statusX, statusY, statusX + statusWidth, statusY + statusHeight, 0x80000000);
        drawRect(statusX, statusY, statusX + statusWidth, statusY + 1, statusColor);
        drawRect(statusX, statusY + statusHeight - 1, statusX + statusWidth, statusY + statusHeight, statusColor);
        drawRect(statusX, statusY, statusX + 1, statusY + statusHeight, statusColor);
        drawRect(statusX + statusWidth - 1, statusY, statusX + statusWidth, statusY + statusHeight, statusColor);
        
        String statusLabel = EnumChatFormatting.WHITE + "Status: " + statusText;
        int textWidth = this.fontRendererObj.getStringWidth(statusLabel);
        this.fontRendererObj.drawStringWithShadow(statusLabel, statusX + (statusWidth - textWidth) / 2, 
                statusY + (statusHeight - 8) / 2, 0xFFFFFFFF);
    }
    
    private void drawTooltips(int mouseX, int mouseY) {
        int contentStartX = panelX + sidebarWidth + 30;
        
        if (modeButton != null && mouseX >= modeButton.xPosition && mouseX < modeButton.xPosition + modeButton.width &&
            mouseY >= modeButton.yPosition && mouseY < modeButton.yPosition + modeButton.height) {
            String tooltip = EnumChatFormatting.GRAY + "Switch between left and right click modes";
            this.fontRendererObj.drawString(tooltip, contentStartX, modeButton.yPosition + modeButton.height + 2, 0xFFAAAAAA);
        }
        
        if (actionBarToggleButton != null && mouseX >= actionBarToggleButton.xPosition && 
            mouseX < actionBarToggleButton.xPosition + actionBarToggleButton.width &&
            mouseY >= actionBarToggleButton.yPosition && mouseY < actionBarToggleButton.yPosition + actionBarToggleButton.height) {
            String tooltip = EnumChatFormatting.GRAY + "Show/hide status messages in action bar";
            this.fontRendererObj.drawString(tooltip, actionBarToggleButton.xPosition, actionBarToggleButton.yPosition + actionBarToggleButton.height + 2, 0xFFAAAAAA);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == closeButton) {
            mc.displayGuiScreen(null);
        } else if (button == modeButton) {
            mod.currentMode = (mod.currentMode == AutoClickerMod.Mode.LEFT ? AutoClickerMod.Mode.RIGHT : AutoClickerMod.Mode.LEFT);
            modeButton.displayString = "Mode: " + (mod.currentMode == AutoClickerMod.Mode.LEFT ? "Left-Click" : "Right-Click");
        } else if (button == actionBarToggleButton) {
            mod.showActionBar = !mod.showActionBar;
            String actionBarText = "Action Bar: " + (mod.showActionBar ? "Enabled" : "Disabled");
            actionBarToggleButton.displayString = actionBarText;
            actionBarToggleButton.setSelected(mod.showActionBar);
            actionBarToggleButton.setColors(0xFF1A2332, 0xFF2C3E50, mod.showActionBar ? 0xFF2C5F8F : 0xFF1A2332);
            if (!mod.showActionBar) {
                mod.clearActionBar();
            }
        } else if (button == bindToggleKeyButton) {
            waitingForBind = KeybindTarget.TOGGLE;
            bindToggleKeyButton.displayString = "Toggle Auto-Clicker: " + EnumChatFormatting.YELLOW + "Press a key...";
        } else if (button == bindGuiKeyButton) {
            waitingForBind = KeybindTarget.GUI;
            bindGuiKeyButton.displayString = "Open Auto-Clicker GUI: " + EnumChatFormatting.YELLOW + "Press a key...";
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (waitingForBind != null) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                if (waitingForBind == KeybindTarget.TOGGLE && mod.toggleKey != null) {
                    mod.toggleKey.setKeyCode(0);
                } else if (waitingForBind == KeybindTarget.GUI && mod.guiKey != null) {
                    mod.guiKey.setKeyCode(0);
                }

                net.minecraft.client.settings.KeyBinding.resetKeyBindingArrayAndHash();
                if (mc != null && mc.gameSettings != null) {
                    mc.gameSettings.saveOptions();
                }

                waitingForBind = null;
                refreshKeybindLabels();
                return;
            }

            if (waitingForBind == KeybindTarget.TOGGLE && mod.toggleKey != null) {
                mod.toggleKey.setKeyCode(keyCode);
            } else if (waitingForBind == KeybindTarget.GUI && mod.guiKey != null) {
                mod.guiKey.setKeyCode(keyCode);
            }

            net.minecraft.client.settings.KeyBinding.resetKeyBindingArrayAndHash();
            if (mc != null && mc.gameSettings != null) {
                mc.gameSettings.saveOptions();
            }

            waitingForBind = null;
            refreshKeybindLabels();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (cpsSlider != null) {
            cpsSlider.mousePressed(mc, mouseX, mouseY);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (cpsSlider != null) {
            cpsSlider.mouseReleased(mouseX, mouseY);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (cpsSlider != null && cpsSlider.dragging) {
            cpsSlider.updateSlider(mouseX);
            mod.leftClickCPS = cpsSlider.getValue();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void refreshKeybindLabels() {
        if (bindToggleKeyButton != null) {
            bindToggleKeyButton.displayString = "Toggle Auto-Clicker: " + keyName(mod.toggleKey);
        }
        if (bindGuiKeyButton != null) {
            bindGuiKeyButton.displayString = "Open Auto-Clicker GUI: " + keyName(mod.guiKey);
        }
        updateKeybindConflictStyles();
    }

    private static String keyName(net.minecraft.client.settings.KeyBinding binding) {
        if (binding == null) return "NONE";
        int code = binding.getKeyCode();
        if (code == 0) return "NONE";
        String name = Keyboard.getKeyName(code);
        return name == null ? ("Key " + code) : name;
    }

    private void updateKeybindConflictStyles() {
        if (bindToggleKeyButton != null) {
            updateKeybindButtonStyle(bindToggleKeyButton, mod.toggleKey);
        }
        if (bindGuiKeyButton != null) {
            updateKeybindButtonStyle(bindGuiKeyButton, mod.guiKey);
        }
    }

    private void updateKeybindButtonStyle(CustomGuiButton button, net.minecraft.client.settings.KeyBinding binding) {
        boolean conflict = hasKeybindConflict(binding);
        if (conflict) {
            button.setColors(0xFF8B2C3E, 0xFF9B3C4E, 0xFFB33A4F);
        } else {
            button.setColors(0xFF1A2332, 0xFF2C3E50, 0xFF2C5F8F);
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
        private final int min, max;
        private int value;
        private boolean dragging = false;
        private final String prefix;

        public CPSSlider(int id, int x, int y, int width, int height, String prefix, int min, int max, int startValue) {
            super(id, x, y, width, height, prefix + startValue);
            this.prefix = prefix;
            this.min = min;
            this.max = max;
            this.value = startValue;
        }

        public int getValue() {
            return value;
        }

        public void updateSlider(int mouseX) {
            float percent = (float) (mouseX - this.xPosition) / width;
            percent = Math.max(0f, Math.min(1f, percent));
            value = min + Math.round(percent * (max - min));
            displayString = prefix + value;
        }

        @Override
        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
            if (super.mousePressed(mc, mouseX, mouseY)) {
                dragging = true;
                updateSlider(mouseX);
                return true;
            }
            return false;
        }

        public void mouseReleased(int mouseX, int mouseY) {
            dragging = false;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!visible) {
                return;
            }

            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition &&
                    mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

            int trackHeight = 4;
            int trackY = yPosition + height / 2 - trackHeight / 2;
            drawRect(xPosition, trackY, xPosition + width, trackY + trackHeight, 0xFF1A2332);

            drawRect(xPosition, trackY, xPosition + width, trackY + 1, 0xFF2C5F8F);
            drawRect(xPosition, trackY + trackHeight - 1, xPosition + width, trackY + trackHeight, 0xFF2C5F8F);

            float percent = (float) (value - min) / (max - min);
            int handleX = xPosition + (int) (percent * width) - 6;
            int handleY = yPosition + height / 2 - 8;

            int handleColor = dragging || hovered ? 0xFF3C6F9F : 0xFF2C5F8F;
            drawRect(handleX, handleY, handleX + 12, handleY + 16, handleColor);
            drawRect(handleX, handleY, handleX + 12, handleY + 1, 0xFF2C5F8F);
            drawRect(handleX, handleY + 15, handleX + 12, handleY + 16, 0xFF2C5F8F);
            drawRect(handleX, handleY, handleX + 1, handleY + 16, 0xFF2C5F8F);
            drawRect(handleX + 11, handleY, handleX + 12, handleY + 16, 0xFF2C5F8F);

            drawRect(xPosition, trackY, xPosition + (int) (percent * width), trackY + trackHeight, 0xFF2C5F8F);

            int textColor = this.enabled ? 0xFFFFFF : 0xA0A0A0;
            this.drawCenteredString(mc.fontRendererObj, displayString, xPosition + width / 2, yPosition + height + 5, textColor);
        }
    }
}
