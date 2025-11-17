package com.life.autoclicker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import java.io.*;

@Mod(modid = "saicoautoclicker", name = "SaiCo AutoClicker", version = "1.0")
public class AutoClickerMod {
    private Minecraft mc;
    public KeyBinding toggleKey;
    public KeyBinding guiKey;
    public boolean clicking = false;
    public int cps = 10;
    private long lastClick = 0;
    private boolean wasClickingBeforeGui = false;
    private boolean lastClickingState = false;
    private long lastServerActionBarTime = 0;
    private String lastServerActionBar = "";
    private static final long SERVER_ACTIONBAR_COOLDOWN = 1500;
    public boolean showActionBar = true;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        mc = Minecraft.getMinecraft();

        toggleKey = new KeyBinding("Toggle AutoClicker", Keyboard.KEY_R, "SaiCo Auto-Clicker");
        guiKey = new KeyBinding("Open AutoClicker GUI", Keyboard.KEY_RSHIFT, "SaiCo Auto-Clicker");
        ClientRegistry.registerKeyBinding(toggleKey);
        ClientRegistry.registerKeyBinding(guiKey);

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (toggleKey.isPressed()) toggleAutoClicker();
        if (guiKey.isPressed()) mc.displayGuiScreen(new AutoClickerGui(this));
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        boolean guiOpen = mc.currentScreen != null;

        if (guiOpen) {
            if (clicking && !wasClickingBeforeGui) {
                wasClickingBeforeGui = true;
                clicking = false;
                clearActionBar();
            }
            return;
        }

        if (wasClickingBeforeGui) {
            clicking = true;
            wasClickingBeforeGui = false;
            updateActionBar();
        }

        if (clicking) {
            long now = System.currentTimeMillis();
            if (Mouse.isButtonDown(0) && now - lastClick >= 1000 / Math.max(1, cps)) {
                lastClick = now;

                mc.thePlayer.swingItem();

                if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit != null) {
                    mc.playerController.attackEntity(mc.thePlayer, mc.objectMouseOver.entityHit);
                }
            }

            if (showActionBar) {
                updateActionBar();
            }
        } else {
            if (lastClickingState) clearActionBar();
        }

        lastClickingState = clicking;
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (event.type == 2) {
            lastServerActionBar = event.message == null ? "" : event.message.getUnformattedText();
            lastServerActionBarTime = System.currentTimeMillis();
        }
    }

    public void updateActionBar() {
        if (mc.thePlayer == null) return;
        if (!showActionBar) return;

        long now = System.currentTimeMillis();

        if (now - lastServerActionBarTime < SERVER_ACTIONBAR_COOLDOWN) return;

        String ourMsg = 
                EnumChatFormatting.WHITE + "" + EnumChatFormatting.BOLD + "Auto" +
                EnumChatFormatting.BLUE + "" + EnumChatFormatting.BOLD + "Clicker " +
                EnumChatFormatting.GREEN + "Enabled " +
                EnumChatFormatting.GRAY + "| " +
                EnumChatFormatting.WHITE + "" + EnumChatFormatting.BOLD + "CPS: " +
                EnumChatFormatting.LIGHT_PURPLE + "" + EnumChatFormatting.BOLD + cps;

        mc.ingameGUI.setRecordPlaying(ourMsg, true);
    }

    public void clearActionBar() {
        mc.ingameGUI.setRecordPlaying("", true);
    }

    public void toggleAutoClicker() {
        clicking = !clicking;

        if (mc.thePlayer != null) {
            if (clicking) {
                mc.thePlayer.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.GREEN + "" + EnumChatFormatting.BOLD + "SaiCo" +
                                EnumChatFormatting.LIGHT_PURPLE + "" + EnumChatFormatting.BOLD + "PvP " +
                                EnumChatFormatting.WHITE + "" + EnumChatFormatting.BOLD + "Auto" +
                                EnumChatFormatting.BLUE + "" + EnumChatFormatting.BOLD + "Clicker " +
                                EnumChatFormatting.GREEN + "Enabled"
                ));
            } else {
                mc.thePlayer.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.GREEN + "" + EnumChatFormatting.BOLD + "SaiCo" +
                                EnumChatFormatting.LIGHT_PURPLE + "" + EnumChatFormatting.BOLD + "PvP " +
                                EnumChatFormatting.WHITE + "" + EnumChatFormatting.BOLD + "Auto" +
                                EnumChatFormatting.BLUE + "" + EnumChatFormatting.BOLD + "Clicker " +
                                EnumChatFormatting.RED + "Disabled"
                ));
            }
        }

        if (clicking && showActionBar) {
            updateActionBar();
        } else {
            clearActionBar();
        }
    }
}