package com.life.autoclicker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
final class SaicoFont {

    private static final float GUI_SIZE = 9f;
    private static final int PAD = 2;

    private static Font source;
    private static Font activeFont;
    private static FontRenderContext frc;
    private static BufferedImage atlasImage;
    private static DynamicTexture atlas;
    private static int atlasW;
    private static int atlasH;
    private static int scale = -1;
    private static int penX;
    private static int penY;
    private static int rowH;
    private static float ascent;
    private static int lineHeight = 12;
    private static boolean failed;
    private static final Map<Integer, Glyph> glyphs = new HashMap<Integer, Glyph>();

    private SaicoFont() {
    }

    static int height() {
        ensure();
        return lineHeight;
    }

    static float inkCenter(String text) {
        ensure();
        if (failed || activeFont == null || frc == null || text == null || text.length() == 0 || scale < 1) {
            return height() * 0.5f;
        }
        Rectangle2D visual = activeFont.createGlyphVector(frc, text).getVisualBounds();
        float top = ascent + (float) visual.getY() / scale;
        float bottom = ascent + (float) Math.min(0.0, visual.getMaxY()) / scale;
        return (top + bottom) * 0.5f;
    }

    static int width(String text) {
        return Math.round(widthF(text));
    }

    static float widthF(String text) {
        if (text == null || text.length() == 0) {
            return 0f;
        }
        ensure();
        if (failed) {
            return text.length() * 6f;
        }
        float width = 0f;
        for (int i = 0; i < text.length(); i++) {
            width += glyph(text.charAt(i)).advance;
        }
        return width;
    }

    static void draw(String text, int x, int y, int color) {
        draw(text, (float) x, (float) y, color);
    }

    static void draw(String text, float x, float y, int color) {
        if (text == null || text.length() == 0) {
            return;
        }
        int alpha = (color >>> 24) & 255;
        if (alpha < 8) {
            return;
        }
        ensure();
        if (failed || atlas == null) {
            return;
        }

        float red = ((color >> 16) & 255) / 255f;
        float green = ((color >> 8) & 255) / 255f;
        float blue = (color & 255) / 255f;
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.01f);
        GlStateManager.color(red, green, blue, alpha / 255f);
        GlStateManager.bindTexture(atlas.getGlTextureId());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        Glyph[] drawn = new Glyph[text.length()];
        for (int i = 0; i < text.length(); i++) {
            drawn[i] = glyph(text.charAt(i));
        }

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();
        boolean started = false;
        float pen = x;
        for (int i = 0; i < text.length(); i++) {
            Glyph glyph = drawn[i];
            if (glyph.quadW > 0.2f && glyph.quadH > 0.2f) {
                if (!started) {
                    buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
                    started = true;
                }
                float gx = pen + glyph.offsetX;
                float gy = y + glyph.offsetY;
                buffer.pos(gx, gy + glyph.quadH, 0).tex(glyph.u0, glyph.v1).endVertex();
                buffer.pos(gx + glyph.quadW, gy + glyph.quadH, 0).tex(glyph.u1, glyph.v1).endVertex();
                buffer.pos(gx + glyph.quadW, gy, 0).tex(glyph.u1, glyph.v0).endVertex();
                buffer.pos(gx, gy, 0).tex(glyph.u0, glyph.v0).endVertex();
            }
            pen += glyph.advance;
        }
        if (started) {
            tessellator.draw();
        }
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static void ensure() {
        if (failed) {
            return;
        }
        int factor = 1;
        try {
            factor = new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
        } catch (Throwable ignored) {
            factor = 1;
        }
        if (factor < 1) {
            factor = 1;
        }
        if (atlas != null && factor == scale) {
            return;
        }
        try {
            rebuild(factor);
        } catch (Throwable ignored) {
            failed = true;
            lineHeight = 9;
        }
    }

    private static void rebuild(int factor) throws Exception {
        Font font = load().deriveFont(GUI_SIZE * factor);
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D probeGraphics = probe.createGraphics();
        probeGraphics.setFont(font);
        FontMetrics metrics = probeGraphics.getFontMetrics();
        int fontAscent = metrics.getAscent();
        int fontDescent = metrics.getDescent();
        probeGraphics.dispose();

        int estimate = (int) Math.ceil(GUI_SIZE * factor * 1.5f) + PAD * 2 + 2;
        int cols = 16;
        int rows = ((95 + cols - 1) / cols) + 6;
        int nextW = pow2(Math.max(256, cols * estimate));
        int nextH = pow2(Math.max(128, rows * estimate));
        BufferedImage image = new BufferedImage(nextW, nextH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.setFont(font);
        graphics.setColor(Color.WHITE);

        activeFont = font;
        frc = graphics.getFontRenderContext();
        atlasImage = image;
        atlasW = nextW;
        atlasH = nextH;
        penX = 0;
        penY = 0;
        rowH = 0;
        ascent = fontAscent / (float) factor;
        lineHeight = Math.max(8, Math.round((fontAscent + fontDescent) / (float) factor));
        glyphs.clear();
        scale = factor;

        for (int code = 32; code < 127; code++) {
            place(graphics, (char) code, factor);
        }
        graphics.dispose();
        whiten(image, 0, 0, nextW, nextH);
        upload();
    }

    private static Glyph glyph(char c) {
        Glyph found = glyphs.get(Integer.valueOf(c));
        if (found != null || failed || activeFont == null) {
            if (found != null) {
                return found;
            }
            return glyphs.containsKey(Integer.valueOf(' ')) ? glyphs.get(Integer.valueOf(' ')) : Glyph.EMPTY;
        }
        Graphics2D graphics = atlasImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        graphics.setFont(activeFont);
        graphics.setColor(Color.WHITE);
        frc = graphics.getFontRenderContext();
        Glyph created = place(graphics, c, scale);
        graphics.dispose();
        if (created != null && created.quadW > 0f) {
            whiten(atlasImage, 0, 0, atlasW, atlasH);
            upload();
        }
        return created == null ? Glyph.EMPTY : created;
    }

    private static Glyph place(Graphics2D graphics, char c, int factor) {
        GlyphVector vector = activeFont.createGlyphVector(frc, new char[] { c });
        GlyphMetrics metrics = vector.getGlyphMetrics(0);
        Rectangle2D visual = vector.getVisualBounds();
        float advance = metrics.getAdvance() / factor;
        boolean ink = visual.getWidth() >= 0.5 && visual.getHeight() >= 0.5;
        int gw = ink ? (int) Math.ceil(visual.getWidth()) + PAD * 2 + 1 : 0;
        int gh = ink ? (int) Math.ceil(visual.getHeight()) + PAD * 2 + 1 : 0;
        Glyph glyph = new Glyph();
        glyph.advance = advance;
        if (ink && gw > 0 && gh > 0 && claim(gw, gh)) {
            int dx = penX - gw;
            int dy = penY;
            float ox = PAD - (float) visual.getX();
            float oy = PAD - (float) visual.getY();
            graphics.drawString(String.valueOf(c), dx + ox, dy + oy);
            glyph.offsetX = (float) visual.getX() / factor - PAD / (float) factor;
            glyph.offsetY = ascent + (float) visual.getY() / factor - PAD / (float) factor;
            glyph.quadW = gw / (float) factor;
            glyph.quadH = gh / (float) factor;
            glyph.u0 = dx / (float) atlasW;
            glyph.v0 = dy / (float) atlasH;
            glyph.u1 = (dx + gw) / (float) atlasW;
            glyph.v1 = (dy + gh) / (float) atlasH;
        }
        glyphs.put(Integer.valueOf(c), glyph);
        return glyph;
    }

    private static boolean claim(int gw, int gh) {
        if (gw > atlasW || gh > atlasH) {
            return false;
        }
        if (penX + gw > atlasW) {
            penX = 0;
            penY += rowH;
            rowH = 0;
        }
        if (penY + gh > atlasH) {
            return false;
        }
        rowH = Math.max(rowH, gh);
        penX += gw;
        return true;
    }

    private static void whiten(BufferedImage image, int x, int y, int w, int h) {
        int x1 = Math.min(image.getWidth(), x + w);
        int y1 = Math.min(image.getHeight(), y + h);
        for (int py = y; py < y1; py++) {
            for (int px = x; px < x1; px++) {
                int pixel = image.getRGB(px, py);
                int alpha = (pixel >>> 24) & 255;
                image.setRGB(px, py, (alpha << 24) | 0x00FFFFFF);
            }
        }
    }

    private static void upload() {
        if (atlas != null) {
            atlas.deleteGlTexture();
            atlas = null;
        }
        atlas = new DynamicTexture(atlasImage);
        GlStateManager.bindTexture(atlas.getGlTextureId());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
    }

    private static Font load() throws Exception {
        if (source != null) {
            return source;
        }
        InputStream in = SaicoFont.class.getResourceAsStream("/assets/saicoautoclicker/font/Inter-Medium.ttf");
        if (in == null) {
            source = new Font("SansSerif", Font.PLAIN, 12);
            return source;
        }
        try {
            source = Font.createFont(Font.TRUETYPE_FONT, in);
        } finally {
            in.close();
        }
        return source;
    }

    private static int pow2(int value) {
        int power = 1;
        while (power < value) {
            power <<= 1;
        }
        return power;
    }

    private static final class Glyph {
        private static final Glyph EMPTY = new Glyph();
        private float advance;
        private float offsetX;
        private float offsetY;
        private float quadW;
        private float quadH;
        private float u0;
        private float v0;
        private float u1;
        private float v1;
    }
}
