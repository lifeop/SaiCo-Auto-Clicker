package com.life.autoclicker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class CustomGuiButton extends GuiButton {
    private int normalColor = 0xFF1A2332;
    private int hoverColor = 0xFF2C3E50;
    private int selectedColor = 0xFF2C5F8F;
    private boolean selected = false;

    public CustomGuiButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
    }

    public void setColors(int normal, int hover, int selected) {
        this.normalColor = normal;
        this.hoverColor = hover;
        this.selectedColor = selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) {
            return;
        }

        this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition && 
                      mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

        int color;
        if (selected) {
            color = selectedColor;
        } else if (this.hovered) {
            color = hoverColor;
        } else {
            color = normalColor;
        }

        drawRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + this.height, color);

        int borderColor = 0xFF2C5F8F;
        drawRect(this.xPosition, this.yPosition, this.xPosition + this.width, this.yPosition + 1, borderColor);
        drawRect(this.xPosition, this.yPosition + this.height - 1, this.xPosition + this.width, this.yPosition + this.height, borderColor);
        drawRect(this.xPosition, this.yPosition, this.xPosition + 1, this.yPosition + this.height, borderColor);
        drawRect(this.xPosition + this.width - 1, this.yPosition, this.xPosition + this.width, this.yPosition + this.height, borderColor);

        int textColor = this.enabled ? 0xFFFFFF : 0xA0A0A0;
        this.drawCenteredString(mc.fontRendererObj, this.displayString, this.xPosition + this.width / 2, 
                               this.yPosition + (this.height - 8) / 2, textColor);
    }
}

