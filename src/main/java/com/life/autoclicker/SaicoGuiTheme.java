package com.life.autoclicker;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;

@SideOnly(Side.CLIENT)
final class SaicoGuiTheme {

    static final int BACKDROP = 0x33000000;

    static final int PANEL = 0xF20E0E12;
    static final int BUTTON = 0x66000000;
    static final int BUTTON_HOVER = 0x880E0E0E;
    static final int BUTTON_DOWN = 0x55000000;

    static final int LINE = 0x66FFFFFF;
    static final int OUTLINE = 0xFF8A45C8;
    static final int FRAME = 0xFFB14DFF;
    static final int HOVER = 0xFFE0B0FF;
    static final int GLASS_SHEEN = 0x28FFFFFF;
    static final int TEXT = 0xFFF4F4F4;
    static final int TEXT_MUTED = 0xFFB0B0B0;
    static final int TEXT_DIM = 0xFFA0A0A8;

    static final int PURPLE = 0xFFB44DFF;
    static final int PURPLE_SOFT = 0xFF7A3BB8;
    static final int GREEN = 0xFF3DDC6A;
    static final int RED = 0xFFFF5C6A;

    private static final int MASK = 128;
    private static final int RADIUS = 48;
    private static final int STROKE = 4;
    private static DynamicTexture roundMask;
    private static DynamicTexture strokeMask;
    private static long introStart;
    private static int nextSlot;
    private static boolean outro;
    private static long outroStart;
    private static final float CLOSE_SPEED = 1.8f;
    private static final float SHELL_MS = 100f;
    private static float contentSpan = 420f;
    private static float outroSpan = 580f;
    private static float outroScale = 1f;
    private static int[] outroIndex;
    static float contentAlpha = 1f;

    private SaicoGuiTheme() {
    }

    static void fillRound(int x, int y, int w, int h, int color) {
        fillRound(x, y, w, h, color, 10);
    }

    static void fillRound(int x, int y, int w, int h, int color, int radius) {
        if (w < 2 || h < 2) {
            fillOpaque(x, y, x + w, y + h, color);
            return;
        }
        ensureMasks();
        drawMask(roundMask, x, y, w, h, radius, color);
    }

    static void strokeRound(int x, int y, int w, int h, int color, int radius) {
        if (w < 2 || h < 2) {
            return;
        }
        ensureMasks();
        drawMask(strokeMask, x, y, w, h, radius, color);
    }

    private static void drawMask(DynamicTexture texture, int x, int y, int w, int h, int radius, int color) {
        int drawR = Math.max(1, Math.min(radius, Math.min(w, h) / 2));
        drawR = Math.min(drawR, RADIUS);
        float a = ((color >> 24) & 255) / 255f * contentAlpha;
        float r = ((color >> 16) & 255) / 255f;
        float g = ((color >> 8) & 255) / 255f;
        float b = (color & 255) / 255f;

        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(r, g, b, a);
        GlStateManager.bindTexture(texture.getGlTextureId());

        float u1 = RADIUS / (float) MASK;
        float u2 = (MASK - RADIUS) / (float) MASK;
        int midW = w - drawR * 2;
        int midH = h - drawR * 2;

        blit(x, y, drawR, drawR, 0f, 0f, u1, u1);
        blit(x + drawR, y, midW, drawR, u1, 0f, u2, u1);
        blit(x + w - drawR, y, drawR, drawR, u2, 0f, 1f, u1);

        blit(x, y + drawR, drawR, midH, 0f, u1, u1, u2);
        blit(x + drawR, y + drawR, midW, midH, u1, u1, u2, u2);
        blit(x + w - drawR, y + drawR, drawR, midH, u2, u1, 1f, u2);

        blit(x, y + h - drawR, drawR, drawR, 0f, u2, u1, 1f);
        blit(x + drawR, y + h - drawR, midW, drawR, u1, u2, u2, 1f);
        blit(x + w - drawR, y + h - drawR, drawR, drawR, u2, u2, 1f, 1f);

        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static void ensureMasks() {
        if (roundMask != null) {
            return;
        }
        BufferedImage filled = new BufferedImage(MASK, MASK, BufferedImage.TYPE_INT_ARGB);
        BufferedImage stroke = new BufferedImage(MASK, MASK, BufferedImage.TYPE_INT_ARGB);
        float corner = RADIUS;
        for (int py = 0; py < MASK; py++) {
            for (int px = 0; px < MASK; px++) {
                float coverage = sampleCoverage(px, py, corner, true);
                float hole = sampleCoverage(px, py, corner, false);
                int fillAlpha = Math.round(coverage * 255f);
                int strokeAlpha = Math.round(clamp(coverage - hole, 0f, 1f) * 255f);
                filled.setRGB(px, py, (fillAlpha << 24) | 0x00FFFFFF);
                stroke.setRGB(px, py, (strokeAlpha << 24) | 0x00FFFFFF);
            }
        }
        roundMask = new DynamicTexture(filled);
        strokeMask = new DynamicTexture(stroke);
        smoothTexture(roundMask.getGlTextureId());
        smoothTexture(strokeMask.getGlTextureId());
    }

    private static void smoothTexture(int textureId) {
        GlStateManager.bindTexture(textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }

    private static float sampleCoverage(int px, int py, float corner, boolean outer) {
        float total = 0f;
        int samples = 4;
        for (int sy = 0; sy < samples; sy++) {
            for (int sx = 0; sx < samples; sx++) {
                float x = px + (sx + 0.5f) / samples;
                float y = py + (sy + 0.5f) / samples;
                total += outer ? roundCoverage(x, y, corner) : innerCoverage(x, y, corner, STROKE);
            }
        }
        return total / (samples * samples);
    }

    private static float innerCoverage(float px, float py, float radius, float stroke) {
        float left = stroke;
        float top = stroke;
        float right = MASK - stroke;
        float bottom = MASK - stroke;
        if (px < left || py < top || px > right || py > bottom) {
            return 0f;
        }
        float innerR = Math.max(0f, radius - stroke);
        boolean cornerX = px < left + innerR || px > right - innerR;
        boolean cornerY = py < top + innerR || py > bottom - innerR;
        if (!cornerX || !cornerY) {
            return 1f;
        }
        float cx = px < left + innerR ? left + innerR : right - innerR;
        float cy = py < top + innerR ? top + innerR : bottom - innerR;
        float dist = (float) Math.hypot(px - cx, py - cy);
        return clamp(innerR - dist + 0.5f, 0f, 1f);
    }

    private static float roundCoverage(float px, float py, float radius) {
        boolean cornerX = px < radius || px > MASK - radius;
        boolean cornerY = py < radius || py > MASK - radius;
        if (!cornerX || !cornerY) {
            return 1f;
        }
        float cx = px < radius ? radius : MASK - radius;
        float cy = py < radius ? radius : MASK - radius;
        float dist = (float) Math.hypot(px - cx, py - cy);
        return clamp(radius - dist + 0.5f, 0f, 1f);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void blit(int x, int y, int w, int h, float u1, float v1, float u2, float v2) {
        if (w <= 0 || h <= 0) {
            return;
        }
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(x, y + h, 0).tex(u1, v2).endVertex();
        buffer.pos(x + w, y + h, 0).tex(u2, v2).endVertex();
        buffer.pos(x + w, y, 0).tex(u2, v1).endVertex();
        buffer.pos(x, y, 0).tex(u1, v1).endVertex();
        tessellator.draw();
    }

    static void beginIntro() {
        introStart = System.currentTimeMillis();
        nextSlot = 0;
        contentAlpha = 0f;
        outro = false;
        outroScale = 1f;
        outroIndex = null;
    }

    static void beginOutro(float spanMs) {
        beginOutro(spanMs, null);
    }

    static void beginOutro(float spanMs, int[] slots) {
        outro = true;
        outroStart = System.currentTimeMillis();
        contentSpan = Math.max(160f, spanMs / CLOSE_SPEED);
        outroSpan = contentSpan + SHELL_MS;
        outroScale = 1f;
        outroIndex = null;
        if (slots == null || slots.length == 0) {
            return;
        }
        int cap = nextSlot;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] >= cap) {
                cap = slots[i] + 1;
            }
        }
        outroIndex = new int[cap];
        for (int i = 0; i < cap; i++) {
            outroIndex[i] = -1;
        }
        int mapped = 0;
        for (int i = 0; i < slots.length; i++) {
            int slot = slots[i];
            if (slot >= 0 && slot < cap && outroIndex[slot] < 0) {
                outroIndex[slot] = mapped++;
            }
        }
        if (mapped > 0) {
            outroScale = spanMs / slotEnd(mapped - 1);
        }
    }

    static boolean outroDone() {
        return outro && System.currentTimeMillis() - outroStart >= outroSpan;
    }

    static float slotEnd(int slot) {
        return 40f + slot * 42f + 240f;
    }

    static int claimSlot() {
        return nextSlot++;
    }

    private static float outroTime() {
        return System.currentTimeMillis() - outroStart;
    }

    static float introFade() {
        if (!outro) {
            return easeOut((System.currentTimeMillis() - introStart) / 420f);
        }
        float t = outroTime();
        if (t <= contentSpan) {
            return 1f;
        }
        return 1f - easeOut((t - contentSpan) / SHELL_MS);
    }

    static float contentFade() {
        if (!outro) {
            return introFade();
        }
        float t = outroTime();
        if (t >= contentSpan) {
            return 0f;
        }
        return easeOut(1f - t / contentSpan);
    }

    static float appearFor(int slot) {
        if (!outro) {
            float start = 40f + slot * 42f;
            return easeOut((System.currentTimeMillis() - introStart - start) / 240f);
        }
        int index = slot;
        if (outroIndex != null) {
            index = slot >= 0 && slot < outroIndex.length ? outroIndex[slot] : -1;
            if (index < 0) {
                return 0f;
            }
        }
        float t = outroTime();
        if (t >= contentSpan) {
            return 0f;
        }
        float startScaled = (40f + index * 42f) * outroScale / CLOSE_SPEED;
        float window = (240f / CLOSE_SPEED) * outroScale;
        float elapsed = contentSpan - t;
        return easeOut((elapsed - startScaled) / Math.max(1f, window));
    }

    private static float easeOut(float t) {
        t = Math.max(0f, Math.min(1f, t));
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    static int scaleAlpha(int color, float factor) {
        int alpha = (int) (((color >> 24) & 255) * Math.max(0f, Math.min(1f, factor)));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
    static void chaseBorder(int x, int y, int w, int h, int radius) {
        int r = Math.max(1, Math.min(radius, Math.min(w, h) / 2));
        float top = w - 2f * r;
        float side = h - 2f * r;
        float arc = (float) (Math.PI * 0.5 * r);
        float total = top * 2f + side * 2f + arc * 4f;
        if (total < 8f) {
            return;
        }

        float phase = (System.currentTimeMillis() % 3400L) / 3400f;
        float head = phase * total;
        float strip = total * 0.30f;
        int steps = 64;
        float[] px = new float[steps];
        float[] py = new float[steps];
        float[] fade = new float[steps];
        for (int i = 0; i < steps; i++) {
            float t = i / (float) (steps - 1);
            float along = head - strip * t;
            along = along % total;
            if (along < 0f) {
                along += total;
            }
            float[] point = borderPoint(x, y, w, h, r, along, top, side, arc);
            px[i] = point[0];
            py[i] = point[1];
            float remain = 1f - t;
            fade[i] = remain * remain * (3f - 2f * remain);
        }

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);
        chaseRibbon(px, py, fade, 0.25f, 2.4f, 80, 0xB14DFF);
        chaseRibbon(px, py, fade, 0.2f, 1.05f, 220, 0xF4E8FF);
        GlStateManager.shadeModel(7424);
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static void chaseRibbon(float[] px, float[] py, float[] fade, float tailWidth, float headWidth, int alpha, int rgb) {
        int red = (rgb >> 16) & 255;
        int green = (rgb >> 8) & 255;
        int blue = rgb & 255;
        int count = px.length;
        float[] nx = new float[count];
        float[] ny = new float[count];
        for (int i = 0; i < count; i++) {
            int prev = Math.max(0, i - 1);
            int next = Math.min(count - 1, i + 1);
            float dx = px[next] - px[prev];
            float dy = py[next] - py[prev];
            float len = (float) Math.hypot(dx, dy);
            if (len < 0.001f) {
                nx[i] = 0f;
                ny[i] = 1f;
            } else {
                nx[i] = -dy / len;
                ny[i] = dx / len;
            }
        }
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < count - 1; i++) {
            float halfA = (tailWidth + (headWidth - tailWidth) * fade[i]) * 0.5f;
            float halfB = (tailWidth + (headWidth - tailWidth) * fade[i + 1]) * 0.5f;
            int alphaA = (int) (alpha * fade[i] * contentAlpha);
            int alphaB = (int) (alpha * fade[i + 1] * contentAlpha);
            buffer.pos(px[i] + nx[i] * halfA, py[i] + ny[i] * halfA, 0).color(red, green, blue, alphaA).endVertex();
            buffer.pos(px[i] - nx[i] * halfA, py[i] - ny[i] * halfA, 0).color(red, green, blue, alphaA).endVertex();
            buffer.pos(px[i + 1] - nx[i + 1] * halfB, py[i + 1] - ny[i + 1] * halfB, 0).color(red, green, blue, alphaB).endVertex();
            buffer.pos(px[i + 1] + nx[i + 1] * halfB, py[i + 1] + ny[i + 1] * halfB, 0).color(red, green, blue, alphaB).endVertex();
        }
        tessellator.draw();
    }

    static void hoverSpot(float cx, float cy, float radius, float strength) {
        if (strength <= 0.02f || radius < 2f) {
            return;
        }
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);
        GlStateManager.color(1f, 1f, 1f, 1f);
        spotDisc(cx, cy, radius * 1.05f, 46, 0xB14DFF, strength);
        spotDisc(cx, cy, radius * 0.55f, 78, 0xE7C6FF, strength);
        GlStateManager.shadeModel(7424);
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static void spotDisc(float cx, float cy, float radius, int alpha, int rgb, float strength) {
        int red = (rgb >> 16) & 255;
        int green = (rgb >> 8) & 255;
        int blue = rgb & 255;
        int centerAlpha = (int) (alpha * strength * contentAlpha);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();
        buffer.begin(6, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(cx, cy, 0).color(red, green, blue, centerAlpha).endVertex();
        int steps = 20;
        for (int i = 0; i <= steps; i++) {
            double angle = i / (double) steps * Math.PI * 2.0;
            buffer.pos(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius, 0).color(red, green, blue, 0).endVertex();
        }
        tessellator.draw();
    }

    private static float[] borderPoint(int x, int y, int w, int h, int r, float along, float top, float side, float arc) {
        float cursor = along;
        if (cursor <= top) {
            return new float[] { x + r + cursor, y };
        }
        cursor -= top;
        if (cursor <= arc) {
            return arcPoint(x + w - r, y + r, r, -Math.PI / 2, cursor / arc);
        }
        cursor -= arc;
        if (cursor <= side) {
            return new float[] { x + w, y + r + cursor };
        }
        cursor -= side;
        if (cursor <= arc) {
            return arcPoint(x + w - r, y + h - r, r, 0, cursor / arc);
        }
        cursor -= arc;
        if (cursor <= top) {
            return new float[] { x + w - r - cursor, y + h };
        }
        cursor -= top;
        if (cursor <= arc) {
            return arcPoint(x + r, y + h - r, r, Math.PI / 2, cursor / arc);
        }
        cursor -= arc;
        if (cursor <= side) {
            return new float[] { x, y + h - r - cursor };
        }
        cursor -= side;
        return arcPoint(x + r, y + r, r, Math.PI, Math.max(0f, Math.min(1f, cursor / arc)));
    }

    private static float[] arcPoint(float cx, float cy, float radius, double start, float t) {
        double angle = start + t * (Math.PI / 2);
        return new float[] {
                cx + (float) (Math.cos(angle) * radius),
                cy + (float) (Math.sin(angle) * radius)
        };
    }

    static void fillOpaque(int x1, int y1, int x2, int y2, int color) {
        if (x1 > x2) {
            int swap = x1;
            x1 = x2;
            x2 = swap;
        }
        if (y1 > y2) {
            int swap = y1;
            y1 = y2;
            y2 = swap;
        }
        float r = ((color >> 16) & 255) / 255f;
        float g = ((color >> 8) & 255) / 255f;
        float b = (color & 255) / 255f;

        GlStateManager.disableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.color(r, g, b, 1f);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();
        buffer.begin(7, DefaultVertexFormats.POSITION);
        buffer.pos(x1, y2, 0).endVertex();
        buffer.pos(x2, y2, 0).endVertex();
        buffer.pos(x2, y1, 0).endVertex();
        buffer.pos(x1, y1, 0).endVertex();
        tessellator.draw();

        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    static int lerpColor(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int fa = (from >> 24) & 0xFF;
        int fr = (from >> 16) & 0xFF;
        int fg = (from >> 8) & 0xFF;
        int fb = from & 0xFF;
        int ta = (to >> 24) & 0xFF;
        int tr = (to >> 16) & 0xFF;
        int tg = (to >> 8) & 0xFF;
        int tb = to & 0xFF;
        int a = (int) (fa + (ta - fa) * t);
        int r = (int) (fr + (tr - fr) * t);
        int g = (int) (fg + (tg - fg) * t);
        int b = (int) (fb + (tb - fb) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    static float approach(float current, float target, float speed, float deltaSeconds) {
        if (deltaSeconds <= 0f) {
            return current;
        }
        float blend = 1f - (float) Math.exp(-speed * deltaSeconds);
        return current + (target - current) * blend;
    }
}
