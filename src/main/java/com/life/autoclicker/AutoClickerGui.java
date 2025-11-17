package com.life.autoclicker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AutoClickerGui extends GuiScreen {

    private final AutoClickerMod mod;
    private static final ResourceLocation LOGO = new ResourceLocation("autoclicker", "textures/gui/saico_logo.png");

    private final Random random = new Random();
    private final List<Particle> particles = new ArrayList<>();
    private final int PARTICLE_COUNT = 500;

    private int logoX, logoY, logoWidth = 128, logoHeight = 128;

    private CPSSlider cpsSlider;
    private GuiButton modeButton;
    private GuiButton closeButton;

    public AutoClickerGui(AutoClickerMod mod) {
        this.mod = mod;
    }

    @Override
    public void initGui() {
        logoX = (width - logoWidth) / 2;
        logoY = (height - logoHeight) / 2;

        buttonList.clear();

        cpsSlider = new CPSSlider(1, width / 2 - 100, logoY + logoHeight + 10, 200, 20,
                "CPS: ", 1, 10, mod.leftClickCPS);
        buttonList.add(cpsSlider);

        modeButton = new GuiButton(2, width / 2 - 100, logoY + logoHeight + 40, 200, 20,
                "Mode: " + (mod.currentMode == AutoClickerMod.Mode.LEFT ? "Left-Click" : "Right-Click"));
        buttonList.add(modeButton);

        closeButton = new GuiButton(3, width / 2 - 100, logoY + logoHeight + 70, 200, 20, "Close");
        buttonList.add(closeButton);

        particles.clear();
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particles.add(new Particle(random, logoX, logoY, logoWidth, logoHeight));
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        updateParticles();
        drawParticles();

        GL11.glColor4f(1f, 1f, 1f, 1f);
        mc.getTextureManager().bindTexture(LOGO);
        drawModalRectWithCustomSizedTexture(logoX, logoY, 0, 0, logoWidth, logoHeight, logoWidth, logoHeight);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void updateParticles() {
        for (Particle p : particles) {
            p.prevX = p.x;
            p.prevY = p.y;

            p.y -= p.speed;

            p.angle += p.angularSpeed;
            p.x = logoX + logoWidth / 2 + (float)Math.cos(p.angle) * p.radius;

            p.alpha -= 0.002f;

            if (p.y < 0 || p.alpha <= 0) {
                p.reset(random, logoX, logoY, logoWidth, logoHeight);
            }
        }
    }

    private void drawParticles() {
        for (Particle p : particles) {
            int alpha = (int)(p.alpha * 255);
            int color;
            if (p.isNetherParticle) {
                color = (alpha << 24) | (128 << 16) | (0 << 8) | 255;
            } else {
                color = (alpha << 24) | 0xFFFFFF;
            }

            drawRect((int)p.prevX, (int)p.prevY, (int)p.prevX + 2, (int)p.prevY + 2, color);
            drawRect((int)p.x, (int)p.y, (int)p.x + 2, (int)p.y + 2, color);
        }
    }    

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == closeButton) mc.displayGuiScreen(null);
        else if (button == modeButton) {
            mod.currentMode = (mod.currentMode == AutoClickerMod.Mode.LEFT ? AutoClickerMod.Mode.RIGHT : AutoClickerMod.Mode.LEFT);
            modeButton.displayString = "Mode: " + (mod.currentMode == AutoClickerMod.Mode.LEFT ? "Left-Click" : "Right-Click");
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        cpsSlider.mousePressed(mc, mouseX, mouseY);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        cpsSlider.mouseReleased(mouseX, mouseY);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (cpsSlider.dragging) {
            cpsSlider.updateSlider(mouseX);
            mod.leftClickCPS = cpsSlider.getValue();
        }
    }

    private static class CPSSlider extends GuiButton {
        private final int min, max;
        private int value;
        private boolean dragging = false;

        public CPSSlider(int id, int x, int y, int width, int height, String prefix, int min, int max, int startValue) {
            super(id, x, y, width, height, prefix + startValue);
            this.min = min;
            this.max = max;
            this.value = startValue;
        }

        public int getValue() { return value; }

        public void updateSlider(int mouseX) {
            float percent = (float)(mouseX - this.xPosition) / width;
            percent = Math.max(0f, Math.min(1f, percent));
            value = min + Math.round(percent * (max - min));
            displayString = "CPS: " + value;
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

        public void mouseReleased(int mouseX, int mouseY) { dragging = false; }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (visible) {
                int trackHeight = 4;
                int trackY = yPosition + height / 2 - trackHeight / 2;
                drawRect(xPosition, trackY, xPosition + width, trackY + trackHeight, 0xFF555555);

                float percent = (float)(value - min) / (max - min);
                int handleX = xPosition + (int)(percent * width) - 4;
                int handleY = yPosition + height / 2 - 6;
                drawRect(handleX, handleY, handleX + 8, handleY + 12, 0xFFFFFFFF);

                drawCenteredString(mc.fontRendererObj, displayString, xPosition + width / 2, yPosition - 10, 0xFFFFFF);
            }
        }
    }

    private static class Particle {
        float x, y;
        float prevX, prevY;
        float speed;
        float alpha;
        boolean isNetherParticle;

        float angle;
        float angularSpeed;
        float radius;

        Particle(Random random, int logoX, int logoY, int logoWidth, int logoHeight) {
            reset(random, logoX, logoY, logoWidth, logoHeight);
        }

        void reset(Random random, int logoX, int logoY, int logoWidth, int logoHeight) {
            radius = 20 + random.nextFloat() * 60;
            angle = random.nextFloat() * 2 * (float)Math.PI;
            angularSpeed = 0.01f + random.nextFloat() * 0.03f;
            speed = 0.3f + random.nextFloat() * 1.2f;
            alpha = 0.1f + random.nextFloat() * 0.3f;
            isNetherParticle = random.nextBoolean();
            y = logoY + logoHeight / 2 + random.nextFloat() * 50;
            x = logoX + logoWidth / 2 + (float)Math.cos(angle) * radius;
            prevX = x;
            prevY = y;
        }
    }
}