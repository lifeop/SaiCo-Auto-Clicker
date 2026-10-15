package com.life.autoclicker;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.util.ResourceLocation;
import paulscode.sound.SoundSystem;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

final class SaicoSounds {

    private static final String[][] BUILTIN = {
            {"air_sweep", "Air Sweep"},
            {"baby_click", "Baby Click"},
            {"big_toggle", "Big Toggle"},
            {"bleepy_toggle", "Bleepy Toggle"},
            {"button_solid", "Button Solid"},
            {"buzz_snap", "Buzz Snap"},
            {"chunk_key", "Chunk Key"},
            {"digi_chunk_3", "Digital Chunk"},
            {"digi_select", "Digital Select"},
            {"glass_beads", "Glass Beads"},
            {"glitch_flam", "Glitch Flam"},
            {"hightech", "High Tech"},
            {"key_chonk", "Key Chonk"},
            {"keyboard_snap", "Keyboard Snap"},
            {"keycap_roll", "Keycap Roll"},
            {"keypad_click", "Keypad Click"},
            {"lazer_click", "Lazer Click"},
            {"lazer_click_2", "Lazer Click 2"},
            {"low_profi_2", "Low Profile"},
            {"mechanism", "Mechanism"},
            {"old_keycap", "Old Keycap"},
            {"old_tech_click", "Old Tech Click"},
            {"percussive_2", "Percussive"},
            {"radio_button", "Radio Button"},
            {"rubber_duck_clicks_5", "Rubber Duck Click"},
            {"rubber_duck_clicks_6", "Rubber Duck Click 2"},
            {"super_switch", "Super Switch"},
            {"swish_2", "Swish"},
            {"switch_throw", "Switch Throw"},
            {"synth_sweep", "Synth Sweep"},
            {"typewriter_double", "Typewriter Double"},
            {"well_oiled", "Well Oiled"}
    };

    private static final String DEFAULT_ID = "baby_click";

    private static final List<String[]> library = new ArrayList<String[]>();
    private static final Map<String, File> diskFiles = new LinkedHashMap<String, File>();

    private static boolean muted;
    private static String selected = DEFAULT_ID;

    static {
        for (String[] entry : BUILTIN) {
            library.add(new String[] { entry[0], entry[1] });
        }
    }

    private SaicoSounds() {
    }

    static void load(AutoClickerMod mod) {
        discoverAddedSounds();
        File file = configFile();
        if (file.isFile()) {
            Properties properties = new Properties();
            try (FileInputStream input = new FileInputStream(file)) {
                properties.load(input);
            } catch (IOException ignored) {
                properties.clear();
            }
            muted = Boolean.parseBoolean(properties.getProperty("muted", "false"));
            selected = properties.getProperty("sound", DEFAULT_ID);
            applyGameplay(mod, properties);
        }
        pruneMissingFiles();
        if (indexOf(selected) < 0 || (!isBuiltin(selected) && !hasDisk(selected))) {
            selected = DEFAULT_ID;
            save();
        }
        mod.uiSoundsMuted = muted;
        CustomGuiButton.setUiMuted(muted);
    }

    static boolean isMuted() {
        return muted;
    }

    static void setMuted(boolean muted) {
        SaicoSounds.muted = muted;
        CustomGuiButton.setUiMuted(muted);
        save();
    }

    static int count() {
        return library.size();
    }

    static String idAt(int index) {
        return library.get(index)[0];
    }

    static String labelAt(int index) {
        return library.get(index)[1];
    }

    static String selectedId() {
        return selected;
    }

    static String selectedLabel() {
        int index = indexOf(selected);
        if (index < 0) {
            return "Baby Click";
        }
        if (diskFiles.containsKey(selected)) {
            String name = customName();
            return name.length() == 0 ? "Custom Sound" : name;
        }
        return library.get(index)[1];
    }

    static int selectedIndex() {
        return indexOf(selected);
    }

    static boolean isCustomId(String id) {
        return id != null && diskFiles.containsKey(id);
    }

    static boolean deleteCustom(String id) {
        if (!isCustomId(id) || isBuiltin(id)) {
            return false;
        }
        File file = diskFiles.remove(id);
        if (file != null && file.isFile()) {
            file.delete();
        }
        File configCopy = new File(customDir(), id + ".ogg");
        if (configCopy.isFile()) {
            configCopy.delete();
        }
        removeFromLibrary(id);
        if (id.equals(selected)) {
            selected = DEFAULT_ID;
        }
        save();
        return true;
    }

    static boolean isCustomSelected() {
        return diskFiles.containsKey(selected);
    }

    static boolean hasCustom() {
        return !diskFiles.isEmpty();
    }

    static String customName() {
        int index = indexOf(selected);
        if (index < 0 || !diskFiles.containsKey(selected)) {
            return "";
        }
        String label = library.get(index)[1];
        if (label.equalsIgnoreCase("custom") || label.equalsIgnoreCase("custom sound")) {
            return "Loaded";
        }
        return label;
    }

    static void selectLibrary(int index) {
        if (index < 0 || index >= library.size()) {
            return;
        }
        selected = library.get(index)[0];
        save();
    }

    static void selectCustom() {
        if (diskFiles.isEmpty()) {
            return;
        }
        String last = null;
        for (String id : diskFiles.keySet()) {
            last = id;
        }
        if (last != null) {
            selected = last;
            save();
        }
    }

    static void playUi(SoundHandler handler) {
        if (muted || handler == null) {
            return;
        }
        playCurrent(handler);
    }

    static void preview(SoundHandler handler) {
        if (handler == null) {
            return;
        }
        playCurrent(handler);
    }

    static File chooseFile() {
        FileDialog dialog = new FileDialog((Frame) null, "Add a click sound", FileDialog.LOAD);
        dialog.setFile("*.ogg");
        dialog.setVisible(true);
        String name = dialog.getFile();
        String directory = dialog.getDirectory();
        dialog.dispose();
        if (name == null || directory == null) {
            return null;
        }
        return new File(directory, name);
    }

    static int importCustom(File source) {
        if (source == null || !source.isFile()) {
            return -1;
        }
        String lower = source.getName().toLowerCase(Locale.ROOT);
        String ext = lower.endsWith(".ogg") ? "ogg" : "";
        if (ext.isEmpty() || !isVorbis(source)) {
            return -1;
        }
        String base = source.getName();
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        String id = uniqueId(slug(base));
        String label = labelFrom(base);
        if (label.equalsIgnoreCase("custom")) {
            label = "Custom Sound";
        }
        File stored = new File(customDir(), id + "." + ext);
        if (!copyTo(source, stored)) {
            return -1;
        }
        diskFiles.put(id, stored);
        int existing = indexOf(id);
        if (existing >= 0) {
            library.set(existing, new String[] { id, label });
        } else {
            library.add(new String[] { id, label });
            existing = library.size() - 1;
        }
        selected = id;
        save();
        return existing;
    }

    private static void playCurrent(SoundHandler handler) {
        pruneMissingFiles();
        File disk = diskFiles.get(selected);
        if (disk != null && disk.isFile() && playFile(disk)) {
            return;
        }
        if (!isBuiltin(selected)) {
            selected = DEFAULT_ID;
            save();
        }
        String id = isBuiltin(selected) ? selected : DEFAULT_ID;
        try {
            handler.playSound(PositionedSoundRecord.create(new ResourceLocation("saicoautoclicker", id), 1.0F));
        } catch (Throwable ignored) {
            if (!DEFAULT_ID.equals(id)) {
                handler.playSound(PositionedSoundRecord.create(new ResourceLocation("saicoautoclicker", DEFAULT_ID), 1.0F));
            }
        }
    }

    private static boolean playFile(File file) {
        if (!isVorbis(file)) {
            return false;
        }
        try {
            SoundSystem system = soundSystem(Minecraft.getMinecraft().getSoundHandler());
            if (system == null) {
                return false;
            }
            String sourceName = "saico-" + slug(file.getName());
            system.removeSource(sourceName);
            system.newSource(false, sourceName, file.toURI().toURL(), file.getName(), false, 0F, 0F, 0F, 0, 0F);
            system.setVolume(sourceName, 1.0F);
            system.play(sourceName);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean hasDisk(String id) {
        return isVorbis(diskFiles.get(id));
    }

    private static boolean isVorbis(File file) {
        if (file == null || !file.isFile() || file.length() < 64L) {
            return false;
        }
        byte[] buffer = new byte[(int) Math.min(file.length(), 8192L)];
        try (FileInputStream input = new FileInputStream(file)) {
            int read = input.read(buffer);
            if (read < 64 || buffer[0] != 'O' || buffer[1] != 'g' || buffer[2] != 'g' || buffer[3] != 'S') {
                return false;
            }
            for (int i = 0; i < read - 6; i++) {
                if (buffer[i] == 1
                        && buffer[i + 1] == 'v'
                        && buffer[i + 2] == 'o'
                        && buffer[i + 3] == 'r'
                        && buffer[i + 4] == 'b'
                        && buffer[i + 5] == 'i'
                        && buffer[i + 6] == 's') {
                    return true;
                }
            }
            return false;
        } catch (IOException ex) {
            return false;
        }
    }

    private static void pruneMissingFiles() {
        List<String> missing = new ArrayList<String>();
        for (Map.Entry<String, File> entry : new ArrayList<Map.Entry<String, File>>(diskFiles.entrySet())) {
            File file = entry.getValue();
            if (!isVorbis(file)) {
                missing.add(entry.getKey());
            }
        }
        boolean changed = false;
        for (String id : missing) {
            if (isBuiltin(id)) {
                continue;
            }
            diskFiles.remove(id);
            deleteUnplayableCopy(id);
            if (removeFromLibrary(id)) {
                changed = true;
            }
            if (id.equals(selected)) {
                selected = DEFAULT_ID;
                changed = true;
            }
        }
        if (changed) {
            save();
        }
    }

    private static boolean removeFromLibrary(String id) {
        boolean removed = false;
        for (int i = library.size() - 1; i >= 0; i--) {
            if (id.equals(library.get(i)[0]) && !isBuiltin(id)) {
                library.remove(i);
                removed = true;
            }
        }
        return removed;
    }

    private static SoundSystem soundSystem(SoundHandler handler) throws Exception {
        SoundManager manager = (SoundManager) readField(handler, "sndManager", "field_147694_f");
        if (manager == null) {
            return null;
        }
        return (SoundSystem) readField(manager, "sndSystem", "field_148620_e");
    }

    private static Object readField(Object owner, String mcp, String srg) throws Exception {
        Field field = findField(owner.getClass(), mcp);
        if (field == null) {
            field = findField(owner.getClass(), srg);
        }
        if (field == null) {
            return null;
        }
        field.setAccessible(true);
        return field.get(owner);
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> cursor = type;
        while (cursor != null) {
            try {
                return cursor.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        return null;
    }

    private static void discoverAddedSounds() {
        File config = customDir();
        File[] files = config.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!isVorbis(file)) {
                continue;
            }
            String id = slug(file.getName().substring(0, file.getName().lastIndexOf('.')));
            if (id.isEmpty() || isBuiltin(id)) {
                continue;
            }
            diskFiles.put(id, file);
            if (indexOf(id) < 0) {
                String label = labelFrom(id.replace('_', ' '));
                if (label.equalsIgnoreCase("custom")) {
                    label = "Custom Sound";
                }
                library.add(new String[] { id, label });
            }
        }
    }

    private static void deleteUnplayableCopy(String id) {
        File configCopy = new File(customDir(), id + ".ogg");
        if (configCopy.isFile() && !isVorbis(configCopy)) {
            configCopy.delete();
        }
    }

    private static boolean isBuiltin(String id) {
        for (String[] entry : BUILTIN) {
            if (entry[0].equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static boolean copyTo(File source, File dest) {
        File parent = dest.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            return false;
        }
        try {
            Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return dest.isFile();
        } catch (IOException ex) {
            return false;
        }
    }

    private static String uniqueId(String base) {
        String id = base;
        int suffix = 2;
        while (isBuiltin(id) || new File(customDir(), id + ".ogg").isFile()) {
            id = base + "_" + suffix;
            suffix++;
        }
        return id;
    }

    private static String slug(String text) {
        String cleaned = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        cleaned = cleaned.replaceAll("^_+|_+$", "");
        return cleaned.isEmpty() ? "custom" : cleaned;
    }

    private static String labelFrom(String text) {
        String[] parts = text.trim().replace('_', ' ').split("\\s+");
        StringBuilder label = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (label.length() > 0) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                label.append(part.substring(1));
            }
        }
        return label.length() == 0 ? "Custom" : label.toString();
    }

    private static int indexOf(String id) {
        for (int i = 0; i < library.size(); i++) {
            if (library.get(i)[0].equals(id)) {
                return i;
            }
        }
        return -1;
    }

    static void saveGameplay(AutoClickerMod mod) {
        Properties properties = readConfig();
        properties.setProperty("cps", Integer.toString(Math.max(1, Math.min(10, mod.leftClickCPS))));
        properties.setProperty("mode", mod.currentMode == AutoClickerMod.Mode.RIGHT ? "RIGHT" : "LEFT");
        properties.setProperty("actionBar", Boolean.toString(mod.showActionBar));
        writeConfig(properties);
    }

    private static void applyGameplay(AutoClickerMod mod, Properties properties) {
        int cps = 10;
        try {
            cps = Integer.parseInt(properties.getProperty("cps", "10").trim());
        } catch (NumberFormatException ignored) {
        }
        mod.leftClickCPS = Math.max(1, Math.min(10, cps));
        mod.currentMode = "RIGHT".equalsIgnoreCase(properties.getProperty("mode", "LEFT"))
                ? AutoClickerMod.Mode.RIGHT
                : AutoClickerMod.Mode.LEFT;
        mod.showActionBar = Boolean.parseBoolean(properties.getProperty("actionBar", "true"));
    }

    private static void save() {
        Properties properties = readConfig();
        properties.setProperty("muted", Boolean.toString(muted));
        properties.setProperty("sound", selected == null ? DEFAULT_ID : selected);
        writeConfig(properties);
    }

    private static Properties readConfig() {
        Properties properties = new Properties();
        File file = configFile();
        if (!file.isFile()) {
            return properties;
        }
        try (FileInputStream input = new FileInputStream(file)) {
            properties.load(input);
        } catch (IOException ignored) {
            properties.clear();
        }
        return properties;
    }

    private static void writeConfig(Properties properties) {
        File file = configFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            parent.mkdirs();
        }
        try (FileOutputStream output = new FileOutputStream(file)) {
            properties.store(output, "SaiCo Auto-Clicker settings");
        } catch (IOException ignored) {
        }
    }

    private static File configFile() {
        return new File(Minecraft.getMinecraft().mcDataDir, "config/saicoautoclicker.cfg");
    }

    private static File customDir() {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "config/saicoautoclicker");
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        return dir;
    }
}
