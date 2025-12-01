package com.life.autoclicker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.init.Items;
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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.client.FMLClientHandler;

@Mod(modid = "saicoautoclicker", name = "SaiCo AutoClicker", version = "1.0")
public class AutoClickerMod {

    public KeyBinding toggleKey;
    public KeyBinding guiKey;

    public enum Mode { LEFT, RIGHT }
    public Mode currentMode = Mode.LEFT;

    public boolean clicking = false;
    public int leftClickCPS = 10;
    private final int rightClickCPS = 50;
    private long lastClick = 0;

    private boolean wasClickingBeforeGui = false;
    private boolean lastClickingState = false;

    private long lastServerActionBarTime = 0;
    private String lastServerActionBar = "";
    private static final long SERVER_ACTIONBAR_COOLDOWN = 1500;

    public boolean showActionBar = true;
    
    @SideOnly(Side.CLIENT)
    public boolean isIntendedEnabled() {
        return clicking || wasClickingBeforeGui;
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        toggleKey = new KeyBinding("Toggle Auto-Clicker", Keyboard.KEY_R, "SaiCo Auto-Clicker");
        guiKey = new KeyBinding("Open Auto-Clicker GUI", Keyboard.KEY_RSHIFT, "SaiCo Auto-Clicker");
        ClientRegistry.registerKeyBinding(toggleKey);
        ClientRegistry.registerKeyBinding(guiKey);

        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = FMLClientHandler.instance().getClient();
        if (toggleKey.isPressed()) toggleAutoClicker();
        if (guiKey.isPressed()) mc.displayGuiScreen(new AutoClickerGui(this));
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = FMLClientHandler.instance().getClient();
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
            ItemStack held = mc.thePlayer.getHeldItem();
            
            ItemStack itemInUse = mc.thePlayer.getItemInUse();
            boolean isUsingBow = itemInUse != null && itemInUse.getItem() == Items.bow && mc.thePlayer.getItemInUseDuration() > 0;
            boolean isEating = itemInUse != null && itemInUse.getItem() != null && 
                              itemInUse.getItem().getItemUseAction(itemInUse) == net.minecraft.item.EnumAction.EAT && 
                              mc.thePlayer.getItemInUseDuration() > 0;
            
            if (isUsingBow || isEating) {
                if (showActionBar) updateActionBar();
                return;
            }

            if (currentMode == Mode.LEFT) {
                if (Mouse.isButtonDown(0) && now - lastClick >= 1000 / Math.max(1, leftClickCPS)) {
                    lastClick = now;
                    mc.thePlayer.swingItem();

                    if (mc.objectMouseOver != null && mc.objectMouseOver.entityHit != null) {
                        mc.playerController.attackEntity(mc.thePlayer, mc.objectMouseOver.entityHit);
                    }
                }
            }

            if (currentMode == Mode.RIGHT && Mouse.isButtonDown(1)) {
                if (held != null &&
                    (held.getItem() == Items.paper || held.getItem() == Items.book) &&
                    now - lastClick >= 1000 / rightClickCPS) {
                    lastClick = now;
                    mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, held);
                }
            }

            if (showActionBar) updateActionBar();
        } else {
            if (lastClickingState) clearActionBar();
        }

        lastClickingState = clicking;
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {
        if (event.type == 2) {
            lastServerActionBar = event.message == null ? "" : event.message.getUnformattedText();
            lastServerActionBarTime = System.currentTimeMillis();
        }
    }

    @SideOnly(Side.CLIENT)
    public void updateActionBar() {
        Minecraft mc = FMLClientHandler.instance().getClient();
        if (mc.thePlayer == null || !showActionBar) return;

        long now = System.currentTimeMillis();
        if (now - lastServerActionBarTime < SERVER_ACTIONBAR_COOLDOWN) return;

        String msg;
        if (currentMode == Mode.RIGHT) {
            msg = EnumChatFormatting.WHITE + "" + EnumChatFormatting.BOLD + "Auto" +
                  EnumChatFormatting.BLUE + "" + EnumChatFormatting.BOLD + "Clicker " +
                  EnumChatFormatting.DARK_GRAY + "| " +
                  EnumChatFormatting.WHITE + "" + EnumChatFormatting.BOLD + "Mode: " +
                  EnumChatFormatting.LIGHT_PURPLE + "Right-Click";
        } else {
            msg = EnumChatFormatting.WHITE + "" + EnumChatFormatting.BOLD + "Auto" +
                    EnumChatFormatting.BLUE + "" + EnumChatFormatting.BOLD + "Clicker " +
                    EnumChatFormatting.DARK_GRAY + "| " +
                    EnumChatFormatting.WHITE + "" + EnumChatFormatting.BOLD + "Mode: " +
                    EnumChatFormatting.LIGHT_PURPLE + "Left-Click " +
                    EnumChatFormatting.DARK_GRAY + "| " +
                    EnumChatFormatting.WHITE + "" + EnumChatFormatting.BOLD + "CPS: " +
                    EnumChatFormatting.LIGHT_PURPLE + leftClickCPS;
        }

        mc.ingameGUI.setRecordPlaying(msg, true);
    }

    @SideOnly(Side.CLIENT)
    public void clearActionBar() {
        FMLClientHandler.instance().getClient().ingameGUI.setRecordPlaying("", true);
    }

    @SideOnly(Side.CLIENT)
    public void toggleAutoClicker() {
        clicking = !clicking;
        Minecraft mc = FMLClientHandler.instance().getClient();

        if (mc.thePlayer != null) {
            String status = clicking ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled";
            mc.thePlayer.addChatMessage(new ChatComponentText(
                    EnumChatFormatting.GREEN + "" + EnumChatFormatting.BOLD + "SaiCo" +
                            EnumChatFormatting.LIGHT_PURPLE + "" + EnumChatFormatting.BOLD + "PvP " +
                            EnumChatFormatting.WHITE + "" + EnumChatFormatting.BOLD + "Auto" +
                            EnumChatFormatting.BLUE + "" + EnumChatFormatting.BOLD + "Clicker " +
                            EnumChatFormatting.WHITE + "" + EnumChatFormatting.BOLD + "- " +
                            status
            ));
        }

        if (clicking && showActionBar) updateActionBar();
        else clearActionBar();
    }
}