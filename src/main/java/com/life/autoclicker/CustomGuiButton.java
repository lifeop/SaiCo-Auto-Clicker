package com.life.autoclicker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class CustomGuiButton extends GuiButton {

    private static float frameDelta = 1f / 60f;
    private static long lastFrameNanos;

    private int accentColor = SaicoGuiTheme.HOVER;
    private Integer textColorOverride;
    private boolean selected = false;
    private boolean conflict = false;
    private static boolean uiMuted;

    private boolean closeIcon;
    private boolean backIcon;
    private boolean muteIcon;
    private boolean soundOff;
    private boolean leftAlign;
    private boolean silentPress;
    private float reveal = 1f;
    private boolean clipList;
    private String fromLabel = "";
    private String fromValue = "";
    private String trackedLabel = "";
    private String trackedValue = "";
    private float labelMix = 1f;
    private float valueMix = 1f;
    private float selectPulse;
    private static int clipX;
    private static int clipY;
    private static int clipRight;
    private static int clipBottom;
    private boolean listening;
    private boolean split;
    private String valueText = "";
    private Integer valueColorOverride;
    private Integer dotColor;
    private Integer fromTextColor;
    private Integer fromValueColor;
    private Integer fromDotColor;

    private static final int TEXT_PAD = 14;
    private static final int TEXT_PAD_LEFT = 8;
    private int cornerRadius = 6;
    private float textScale = 1f;

    private float hoverAnim;
    private float selectAnim;
    private float pressAnim;
    private boolean pressed;
    private final int introSlot;

    public CustomGuiButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
        this.introSlot = SaicoGuiTheme.claimSlot();
    }

    int introSlot() {
        return introSlot;
    }

    public static void beginFrame() {
        long now = System.nanoTime();
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now;
        }
        float dt = (now - lastFrameNanos) / 1_000_000_000f;
        lastFrameNanos = now;
        frameDelta = Math.max(0.001f, Math.min(0.05f, dt));
    }

    static float frameDeltaSeconds() {
        return frameDelta;
    }

    public void setCornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
    }

    public void setTextScale(float textScale) {
        this.textScale = textScale;
    }

    public void setValueText(String valueText) {
        this.valueText = valueText == null ? "" : valueText;
    }

    public void setAccentColor(int accentColor) {
        this.accentColor = accentColor;
    }

    public void setTextColor(int color) {
        if (textColorOverride == null || textColorOverride.intValue() != color) {
            fromTextColor = textColorOverride;
            textColorOverride = color;
        }
    }

    public void setValueColor(int color) {
        if (valueColorOverride == null || valueColorOverride.intValue() != color) {
            fromValueColor = valueColorOverride;
            valueColorOverride = color;
        }
    }

    public void setDotColor(int color) {
        if (dotColor == null || dotColor.intValue() != color) {
            fromDotColor = dotColor;
            dotColor = color;
        }
    }

    public void setListening(boolean listening) {
        this.listening = listening;
    }

    public void setSplit(boolean split) {
        this.split = split;
    }

    public boolean hitValueText(int mouseX) {
        if (!split || valueText == null || valueText.length() == 0) {
            return false;
        }
        return mouseX >= this.xPosition + this.width - valueZone();
    }

    private int valueZone() {
        int valueW = SaicoFont.width(valueText);
        return Math.max(64, valueW + 20);
    }

    public void setCloseIcon(boolean closeIcon) {
        this.closeIcon = closeIcon;
        if (closeIcon) {
            this.displayString = "";
        }
    }

    public void setMuteIcon(boolean muteIcon) {
        this.muteIcon = muteIcon;
        if (muteIcon) {
            this.displayString = "";
        }
    }

    public void setBackIcon(boolean backIcon) {
        this.backIcon = backIcon;
        if (backIcon) {
            this.displayString = "";
        }
    }

    public void setLeftAlign(boolean leftAlign) {
        this.leftAlign = leftAlign;
    }

    public void setSilentPress(boolean silentPress) {
        this.silentPress = silentPress;
    }

    public void setReveal(float reveal) {
        this.reveal = reveal;
    }

    public void setListClip(boolean clipList) {
        this.clipList = clipList;
    }

    public static void setListClipRect(int x, int y, int w, int h) {
        clipX = x;
        clipY = y;
        clipRight = x + w;
        clipBottom = y + h;
    }

    public void setPlainText(String text) {
        this.displayString = text == null ? "" : text;
        this.trackedLabel = this.displayString;
        this.fromLabel = "";
        this.labelMix = 1f;
    }

    public void pulse() {
        this.selectPulse = 1f;
    }

    public void setSoundOff(boolean soundOff) {
        this.soundOff = soundOff;
    }

    public static void setUiMuted(boolean muted) {
        uiMuted = muted;
    }

    public void setConflict(boolean conflict) {
        this.conflict = conflict;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    @Override
    public void playPressSound(SoundHandler soundHandler) {
        if (!silentPress) {
            playUiClick(soundHandler);
        }
    }

    public static void playUiClick(SoundHandler soundHandler) {
        SaicoSounds.playUi(soundHandler);
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        if (reveal < 0.9f || clippedOut(mouseX, mouseY)) {
            return false;
        }
        if (super.mousePressed(mc, mouseX, mouseY)) {
            pressed = true;
            pressAnim = 1f;
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        pressed = false;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) {
            return;
        }

        float intro = SaicoGuiTheme.appearFor(introSlot);
        float appear = intro * reveal;
        if (appear < 0.02f) {
            this.hovered = false;
            return;
        }

        this.hovered = reveal > 0.9f && mouseX >= this.xPosition && mouseY >= this.yPosition
                && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

        hoverAnim = SaicoGuiTheme.approach(hoverAnim, hovered ? 1f : 0f, 9f, frameDelta);
        selectAnim = SaicoGuiTheme.approach(selectAnim, selected ? 1f : 0f, 12f, frameDelta);
        selectPulse = SaicoGuiTheme.approach(selectPulse, 0f, 7f, frameDelta);
        pressAnim = SaicoGuiTheme.approach(pressAnim, pressed ? 1f : 0f, 18f, frameDelta);
        trackCrossfade();

        int x = xPosition;
        int y = yPosition;
        int w = width;
        int h = height;
        float rise = (1f - intro) * 6f;
        float dip = pressAnim * 1.15f;

        GlStateManager.pushMatrix();
        clipBegin(mc);
        try {
        GlStateManager.translate(0f, rise + dip, 0f);
        float saved = SaicoGuiTheme.contentAlpha;
        SaicoGuiTheme.contentAlpha = appear;
        int edge = SaicoGuiTheme.lerpColor(SaicoGuiTheme.OUTLINE, accentColor, Math.max(hoverAnim, selectAnim));
        if (selectPulse > 0.02f) {
            edge = SaicoGuiTheme.lerpColor(edge, SaicoGuiTheme.TEXT, selectPulse * 0.85f);
        }
        if (pressAnim > 0.02f) {
            edge = SaicoGuiTheme.lerpColor(edge, SaicoGuiTheme.TEXT, pressAnim * 0.28f);
        }
        if (listening) {
            float wave = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() * 0.007));
            edge = SaicoGuiTheme.lerpColor(SaicoGuiTheme.OUTLINE, accentColor, 0.2f + 0.8f * wave);
        } else if (conflict) {
            edge = SaicoGuiTheme.RED;
        }
        int radius = corner();
        if (selectAnim > 0.04f) {
            int fill = SaicoGuiTheme.scaleAlpha(0xFF7436C2, selectAnim);
            SaicoGuiTheme.fillRound(x, y, w, h, fill, radius);
        }
        SaicoGuiTheme.strokeRound(x, y, w, h, edge, radius);
        if (split && valueText.length() > 0) {
            int splitX = x + w - valueZone();
            drawRect(splitX, y, splitX + 1, y + h, SaicoGuiTheme.scaleAlpha(edge, appear));
        }

        int textY = y + (h - textLine()) / 2;
        boolean wave = !closeIcon && !listening && !conflict && hoverAnim > 0.02f;
        int waveCount = valueText.length() == 0 ? displayString.length() : displayString.length() + valueText.length();
        float[] motion = wave ? waveMotion(waveCount) : new float[] { 0f, 0f };
        float head = motion[0];
        float presence = motion[1] * hoverAnim;
        if (wave && presence > 0.03f && waveCount > 0) {
            float spot = wavePixel(x, w, head);
            SaicoGuiTheme.hoverSpot(spot, y + h * 0.5f, Math.max(5f, h * 0.4f), presence);
        }
        SaicoGuiTheme.contentAlpha = saved;

        int idleText = conflict ? SaicoGuiTheme.RED : (textColorOverride != null ? textColorOverride : SaicoGuiTheme.TEXT);
        int hotText = textColorOverride != null ? textColorOverride : (conflict ? SaicoGuiTheme.RED : SaicoGuiTheme.TEXT);
        int textColor = this.enabled ? SaicoGuiTheme.lerpColor(idleText, hotText, hoverAnim) : SaicoGuiTheme.TEXT_DIM;
        textColor = SaicoGuiTheme.scaleAlpha(textColor, appear);
        if (closeIcon) {
            drawCloseMark(x + w / 2, y + h / 2, SaicoGuiTheme.scaleAlpha(SaicoGuiTheme.TEXT, appear));
            GlStateManager.popMatrix();
            return;
        }
        if (backIcon) {
            drawBackMark(x + w / 2, y + h / 2, SaicoGuiTheme.scaleAlpha(SaicoGuiTheme.TEXT, appear));
            GlStateManager.popMatrix();
            return;
        }
        if (muteIcon) {
            drawMuteIcon(x + w / 2f, y + h / 2f, SaicoGuiTheme.scaleAlpha(SaicoGuiTheme.TEXT, appear));
            GlStateManager.popMatrix();
            return;
        }
        if (valueText.length() == 0) {
            float textW = textSpan(displayString);
            int dot = dotColor != null ? 4 : 0;
            int gap = dotColor != null ? 4 : 0;
            float start = leftAlign ? x + TEXT_PAD_LEFT : x + (w - textW - dot - gap) / 2f;
            if (fromLabel.length() > 0 && labelMix < 0.98f) {
                int fading = fromTextColor != null ? SaicoGuiTheme.scaleAlpha(fromTextColor, appear) : textColor;
                float oldW = textSpan(fromLabel);
                drawFadedString(fromLabel, start + (textW - oldW) / 2f, textY, fading, 1f - labelMix);
            }
            if (labelMix >= 0.06f) {
                drawLitString(displayString, start, textY, SaicoGuiTheme.scaleAlpha(textColor, labelMix), 0, head, presence * labelMix);
            }
            if (dotColor != null) {
                int dotPaint = fromDotColor != null && labelMix < 0.98f
                        ? SaicoGuiTheme.lerpColor(fromDotColor, dotColor, labelMix)
                        : dotColor;
                int dotX = Math.round(start + textW + gap);
                int dotY = textY + Math.max(0, (textLine() - dot) / 2);
                SaicoGuiTheme.fillRound(dotX, dotY, dot, dot, SaicoGuiTheme.scaleAlpha(dotPaint, appear), dot / 2);
            }
        } else {
            int valueColor = SaicoGuiTheme.TEXT;
            if (listening) {
                valueColor = SaicoGuiTheme.HOVER;
            } else if (conflict) {
                valueColor = SaicoGuiTheme.RED;
            } else if (valueColorOverride != null) {
                valueColor = valueColorOverride;
            }
            valueColor = SaicoGuiTheme.scaleAlpha(valueColor, appear);
            if (fromLabel.length() > 0 && labelMix < 0.98f) {
                drawFadedString(fromLabel, x + TEXT_PAD_LEFT, textY, textColor, 1f - labelMix);
            }
            if (labelMix >= 0.06f) {
                drawLitString(displayString, x + TEXT_PAD_LEFT, textY, SaicoGuiTheme.scaleAlpha(textColor, labelMix), 0, head, presence * labelMix);
            }
            if (fromValue.length() > 0 && valueMix < 0.98f) {
                float oldW = textSpan(fromValue);
                int fading = fromValueColor != null ? SaicoGuiTheme.scaleAlpha(fromValueColor, appear) : valueColor;
                drawFadedString(fromValue, x + w - TEXT_PAD - oldW, textY, fading, 1f - valueMix);
            }
            if (valueMix >= 0.06f) {
                float valueW = textSpan(valueText);
                drawLitString(valueText, x + w - TEXT_PAD - valueW, textY, SaicoGuiTheme.scaleAlpha(valueColor, valueMix), displayString.length(), head, presence * valueMix);
            }
        }
        GlStateManager.popMatrix();
        } finally {
            clipEnd();
        }
    }

    private void trackCrossfade() {
        String label = displayString == null ? "" : displayString;
        if (!label.equals(trackedLabel)) {
            fromLabel = trackedLabel;
            trackedLabel = label;
            labelMix = fromLabel.length() == 0 ? 1f : 0f;
        }
        labelMix = SaicoGuiTheme.approach(labelMix, 1f, 10f, frameDelta);
        String value = valueText == null ? "" : valueText;
        if (!value.equals(trackedValue)) {
            fromValue = trackedValue;
            trackedValue = value;
            valueMix = fromValue.length() == 0 ? 1f : 0f;
        }
        valueMix = SaicoGuiTheme.approach(valueMix, 1f, 10f, frameDelta);
    }

    private void drawFadedString(String text, float x, int y, int color, float mix) {
        if (text == null || text.length() == 0 || mix < 0.04f) {
            return;
        }
        drawText(text, x, y, SaicoGuiTheme.scaleAlpha(color, mix));
    }

    private void drawLitString(String text, float x, int y, int color, int indexOffset, float head, float presence) {
        if (text == null || text.length() == 0) {
            return;
        }
        if (presence < 0.04f || listening || conflict) {
            drawText(text, x, y, color);
            return;
        }
        float cursor = x;
        for (int i = 0; i < text.length(); i++) {
            String glyph = text.substring(i, i + 1);
            float width = textSpan(glyph);
            float dist = (indexOffset + i) - head;
            float hot = (float) Math.exp(-dist * dist * 0.72) * presence;
            int waved = SaicoGuiTheme.lerpColor(color, SaicoGuiTheme.HOVER, Math.min(1f, hot));
            drawText(glyph, cursor, y, (color & 0xFF000000) | (waved & 0x00FFFFFF));
            cursor += width;
        }
    }

    private float textSpan(String text) {
        return SaicoFont.widthF(text) * textScale;
    }

    private int textLine() {
        return Math.max(1, Math.round(SaicoFont.height() * textScale));
    }

    private void drawText(String text, float x, float y, int color) {
        if (text == null || text.length() == 0) {
            return;
        }
        if (textScale == 1f) {
            SaicoFont.draw(text, x, y, color);
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0f);
        GlStateManager.scale(textScale, textScale, 1f);
        SaicoFont.draw(text, 0f, 0f, color);
        GlStateManager.popMatrix();
    }

    static void drawAlphaText(net.minecraft.client.gui.FontRenderer font, String text, int x, int y, int color) {
        SaicoFont.draw(text, x, y, color);
    }

    private int corner() {
        if (cornerRadius < 0) {
            return Math.max(1, height / 2);
        }
        return Math.min(cornerRadius, Math.max(1, height / 2));
    }

    private boolean clippedOut(int mouseX, int mouseY) {
        return clipList && (mouseX < clipX || mouseX >= clipRight || mouseY < clipY || mouseY >= clipBottom);
    }

    private void clipBegin(Minecraft mc) {
        if (!clipList) {
            return;
        }
        ScaledResolution scaled = new ScaledResolution(mc);
        int factor = scaled.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        int x = clipX * factor;
        int y = mc.displayHeight - clipBottom * factor;
        int w = Math.max(0, (clipRight - clipX) * factor);
        int h = Math.max(0, (clipBottom - clipY) * factor);
        GL11.glScissor(x, y, w, h);
    }

    private void clipEnd() {
        if (clipList) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    private float[] waveMotion(int count) {
        if (count <= 0) {
            return new float[] { 0f, 0f };
        }
        int travelMs = Math.max(420, count * 64);
        int duration = travelMs + 240;
        long now = System.currentTimeMillis() % duration;
        if (now >= travelMs) {
            return new float[] { count + 2f, 0f };
        }
        float u = now / (float) travelMs;
        float eased = u * u * (3f - 2f * u);
        float head = -1.1f + eased * (count + 2.2f);
        float edge = 0.14f;
        float presence = 1f;
        if (u < edge) {
            presence = u / edge;
        } else if (u > 1f - edge) {
            presence = (1f - u) / edge;
        }
        presence = presence * presence * (3f - 2f * presence);
        return new float[] { head, presence };
    }

    private float wavePixel(int x, int w, float head) {
        if (valueText.length() == 0) {
            float textW = textSpan(displayString);
            int dot = dotColor != null ? 4 : 0;
            int gap = dotColor != null ? 4 : 0;
            float start = leftAlign ? x + TEXT_PAD_LEFT : x + (w - textW - dot - gap) / 2f;
            return glyphCenter(displayString, start, head);
        }
        float leftX = x + TEXT_PAD_LEFT;
        float valueW = textSpan(valueText);
        float rightX = x + w - TEXT_PAD - valueW;
        int leftCount = displayString.length();
        if (head <= leftCount - 0.35f) {
            return glyphCenter(displayString, leftX, head);
        }
        if (head >= leftCount + 0.35f) {
            return glyphCenter(valueText, rightX, head - leftCount);
        }
        float u = (head - (leftCount - 0.35f)) / 0.7f;
        u = u * u * (3f - 2f * u);
        float from = glyphCenter(displayString, leftX, leftCount - 0.2f);
        float to = glyphCenter(valueText, rightX, 0f);
        return from + (to - from) * u;
    }

    private float glyphCenter(String text, float x, float charIndex) {
        if (text == null || text.length() == 0) {
            return x;
        }
        float index = Math.max(0f, Math.min(text.length() - 0.01f, charIndex));
        int whole = (int) index;
        float frac = index - whole;
        float cursor = x;
        for (int i = 0; i < whole && i < text.length(); i++) {
            cursor += textSpan(text.substring(i, i + 1));
        }
        float glyphW = textSpan(text.substring(whole, whole + 1));
        return cursor + glyphW * (0.15f + 0.7f * frac);
    }

    private void drawMuteIcon(float cx, float cy, int color) {
        float shift = soundOff ? 1.3f : 0f;
        float bodyLeft = cx - 4.15f + shift;
        float bodyRight = cx - 2.15f + shift;
        float bodyTop = cy - 1.7f;
        float bodyBottom = cy + 1.7f;
        float mouthX = cx + 1.15f + shift;
        float mouthTop = cy - 3.6f;
        float mouthBottom = cy + 3.6f;
        fillQuad(bodyLeft, bodyBottom, bodyRight, bodyBottom, bodyRight, bodyTop, bodyLeft, bodyTop, color);
        fillQuad(bodyRight, bodyBottom, mouthX, mouthBottom, mouthX, mouthTop, bodyRight, bodyTop, color);
        if (soundOff) {
            int slash = (color & 0xFF000000) | (SaicoGuiTheme.RED & 0x00FFFFFF);
            drawStroke(bodyLeft - 0.15f, mouthTop + 0.4f, mouthX + 0.55f, mouthBottom - 0.4f, 1.05f, slash);
            return;
        }
        drawArc(mouthX - 1.14f, cy, 2.55f, 0.9f, -42f, 42f, color);
        drawArc(mouthX - 1.7f, cy, 4.7f, 0.9f, -40f, 40f, color);
    }

    private void fillQuad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, int color) {
        int alpha = (color >>> 24) & 255;
        if (alpha < 8) {
            return;
        }
        int red = (color >> 16) & 255;
        int green = (color >> 8) & 255;
        int blue = color & 255;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x1, y1, 0).color(red, green, blue, alpha).endVertex();
        buffer.pos(x2, y2, 0).color(red, green, blue, alpha).endVertex();
        buffer.pos(x3, y3, 0).color(red, green, blue, alpha).endVertex();
        buffer.pos(x4, y4, 0).color(red, green, blue, alpha).endVertex();
        tessellator.draw();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void drawStroke(float x1, float y1, float x2, float y2, float thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.hypot(dx, dy);
        if (len < 0.001f) {
            return;
        }
        float nx = -dy / len * thickness * 0.5f;
        float ny = dx / len * thickness * 0.5f;
        fillQuad(x1 + nx, y1 + ny, x2 + nx, y2 + ny, x2 - nx, y2 - ny, x1 - nx, y1 - ny, color);
    }

    private void drawArc(float cx, float cy, float radius, float thickness, float startDeg, float endDeg, int color) {
        int alpha = (color >>> 24) & 255;
        if (alpha < 8) {
            return;
        }
        int red = (color >> 16) & 255;
        int green = (color >> 8) & 255;
        int blue = color & 255;
        int steps = 8;
        float half = thickness * 0.5f;
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < steps; i++) {
            float t0 = (float) Math.toRadians(startDeg + (endDeg - startDeg) * i / steps);
            float t1 = (float) Math.toRadians(startDeg + (endDeg - startDeg) * (i + 1) / steps);
            float c0 = (float) Math.cos(t0);
            float s0 = (float) Math.sin(t0);
            float c1 = (float) Math.cos(t1);
            float s1 = (float) Math.sin(t1);
            float inner = radius - half;
            float outer = radius + half;
            buffer.pos(cx + c0 * inner, cy + s0 * inner, 0).color(red, green, blue, alpha).endVertex();
            buffer.pos(cx + c1 * inner, cy + s1 * inner, 0).color(red, green, blue, alpha).endVertex();
            buffer.pos(cx + c1 * outer, cy + s1 * outer, 0).color(red, green, blue, alpha).endVertex();
            buffer.pos(cx + c0 * outer, cy + s0 * outer, 0).color(red, green, blue, alpha).endVertex();
        }
        tessellator.draw();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private void drawBackMark(int cx, int cy, int color) {
        float tipX = cx - 2.15f;
        float tailX = cx + 2.05f;
        float wing = 2.7f;
        drawStroke(tailX, cy - wing, tipX, cy, 1.05f, color);
        drawStroke(tipX, cy, tailX, cy + wing, 1.05f, color);
    }

    private void drawCloseMark(int cx, int cy, int color) {
        float arm = 3.05f;
        drawStroke(cx - arm, cy - arm, cx + arm, cy + arm, 1.05f, color);
        drawStroke(cx - arm, cy + arm, cx + arm, cy - arm, 1.05f, color);
    }
}
