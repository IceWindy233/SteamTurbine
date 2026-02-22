package cn.icewindy.steamturbine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.common.config.Configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

/**
 * Steam Turbine Mod Configuration Management.
 */
public class ModConfig {

    public static final String CONFIG_FOLDER_NAME = "steamturbine";
    private static final String CATEGORY_LHE = "largeHeatExchanger";

    /** Maximum output voltage (EU/t), default 512 (HV) */
    public static int maxOutputVoltage = 512;

    /** Amount of normal steam (mB) consumed per EU produced, default 2 (1L steam = 0.5 EU) */
    public static int steamPerEU = 2;

    /** Amount of superheated steam (mB) consumed per EU produced, default 1 (1L = 1 EU) */
    public static int superheatedSteamPerEU = 1;

    /** Internal steam buffer tank capacity (mB), default 64000 */
    public static int tankCapacity = 64000;

    /** Multiblock structure check interval (ticks), default 40 */
    public static int checkInterval = 40;

    /** Whether to explode when rotor breaks */
    public static boolean rotorBreakExplosion = true;

    /** Large Heat Exchanger base superheated threshold (mB/t) */
    public static int lheBaseThresholdPerTick = 800;

    /** Large Heat Exchanger base steam multiplier, final output approx consume * multiplier * 2 */
    public static float lheBaseSteamMultiplier = 20.0f;

    /** Large Heat Exchanger dry-heat explosion timer (ticks) */
    public static int lheDryHeatMaxTicks = 2000;

    /** Whether Large Heat Exchanger can output superheated steam */
    public static boolean lheEnableSuperheatedOutput = false;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();

    private static Configuration config;
    private static File modConfigDir;
    private static File rotorConfigFile;
    private static File globalConfigDir;

    private static final List<RotorDefinition> rotorDefinitions = new ArrayList<RotorDefinition>();

    public static class RotorDefinition {

        @SerializedName("_Comment")
        public String comment;

        @SerializedName("ItemName")
        public String itemName;

        @SerializedName("Overflow")
        public float overflow;

        @SerializedName("OptimalFlow")
        public int optimalFlow;

        @SerializedName("Efficiency")
        public float efficiency;

        @SerializedName("Durability")
        public int durability;

        @SerializedName("Icon")
        public String icon;

        @SerializedName("BladeIcon")
        public String bladeIcon;

        @SerializedName("InfiniteDurability")
        public boolean infiniteDurability;

        @SerializedName("Blade")
        public boolean hasBlade;

        public RotorDefinition() {}

        public RotorDefinition(String comment, String itemName, float overflow, int optimalFlow, float efficiency,
            int durability, String icon, String bladeIcon, boolean infiniteDurability, boolean hasBlade) {
            this.comment = comment;
            this.itemName = itemName;
            this.overflow = overflow;
            this.optimalFlow = optimalFlow;
            this.efficiency = efficiency;
            this.durability = durability;
            this.icon = icon;
            this.bladeIcon = bladeIcon;
            this.infiniteDurability = infiniteDurability;
            this.hasBlade = hasBlade;
        }
    }

    private static class RotorConfigData {

        @SerializedName("_Help1")
        public String help1 = "This is the turbine rotor configuration file. You can dynamically add rotors and blades here.";

        @SerializedName("_Help2")
        public String help2 = "ItemName: Material name used for OreDictionary lookup (e.g. Iron -> ingotIron) and tinting.";

        @SerializedName("_Help3")
        public String help3 = "OptimalFlow: Ideal steam flow (mB/t); Efficiency: Energy conversion rate (0.01 to 1.0).";

        @SerializedName("_Help4")
        public String help4 = "Overflow: Max multiplier to handle excess steam; Durability: Total rotor life.";

        @SerializedName("_Help5")
        public String help5 = "InfiniteDurability: Toggle for unbreakable rotors; Blade: Toggle for registering blade items.";

        @SerializedName("_Help6")
        public String help6 = "Icon/BladeIcon: Custom .png filenames; set to null for auto-tinting based on material.";

        @SerializedName("_Help7")
        public String help7 = "All changes take effect after game restart or configuration reload.";

        @SerializedName("Rotors")
        public List<RotorDefinition> rotors = new ArrayList<RotorDefinition>();
    }

    public static void init(File configFile) {
        File effectiveConfigFile = resolveMainConfigFile(configFile);
        config = new Configuration(effectiveConfigFile);
        config.load();

        maxOutputVoltage = config.getInt(
            "maxOutputVoltage",
            Configuration.CATEGORY_GENERAL,
            512,
            32,
            8192,
            "Maximum EU/t output voltage. 32=LV, 128=MV, 512=HV, 2048=EV");

        steamPerEU = config.getInt(
            "steamPerEU",
            Configuration.CATEGORY_GENERAL,
            2,
            1,
            10,
            "Amount of normal steam (mB) consumed per EU produced");

        superheatedSteamPerEU = config.getInt(
            "superheatedSteamPerEU",
            Configuration.CATEGORY_GENERAL,
            1,
            1,
            10,
            "Amount of superheated steam (mB) consumed per EU produced");

        tankCapacity = config.getInt(
            "tankCapacity",
            Configuration.CATEGORY_GENERAL,
            64000,
            1000,
            256000,
            "Internal steam buffer tank capacity in mB");

        checkInterval = config.getInt(
            "checkInterval",
            Configuration.CATEGORY_GENERAL,
            40,
            1,
            200,
            "Multiblock structure check interval in ticks");

        rotorBreakExplosion = config.getBoolean(
            "rotorBreakExplosion",
            Configuration.CATEGORY_GENERAL,
            true,
            "Whether turbine explodes when rotor breaks");

        lheBaseThresholdPerTick = config.getInt(
            "baseThresholdPerTick",
            CATEGORY_LHE,
            800,
            1,
            200000,
            "Large Heat Exchanger base superheated threshold (mB/t) before coolant multiplier");

        String multiplierRaw = config.getString(
            "baseSteamMultiplier",
            CATEGORY_LHE,
            "20.0",
            "Large Heat Exchanger base steam multiplier (final output is consume * multiplier * 2)");
        try {
            lheBaseSteamMultiplier = Float.parseFloat(multiplierRaw);
        } catch (Exception ignored) {
            lheBaseSteamMultiplier = 20.0f;
        }
        if (lheBaseSteamMultiplier <= 0.0f) {
            lheBaseSteamMultiplier = 20.0f;
        }

        lheDryHeatMaxTicks = config.getInt(
            "dryHeatMaxTicks",
            CATEGORY_LHE,
            2000,
            20,
            200000,
            "Large Heat Exchanger dry-heat explosion timer in ticks");

        lheEnableSuperheatedOutput = config.getBoolean(
            "enableSuperheatedOutput",
            CATEGORY_LHE,
            false,
            "Whether Large Heat Exchanger can output superheated steam");

        initRotorConfigPath(effectiveConfigFile);
        initResourcePackTemplate();
        loadRotorConfig();

        if (config.hasChanged()) {
            config.save();
        }
    }

    private static File resolveMainConfigFile(File suggestedConfigFile) {
        File configRoot = suggestedConfigFile.getParentFile();
        globalConfigDir = configRoot;
        modConfigDir = new File(configRoot, CONFIG_FOLDER_NAME);
        if (!modConfigDir.exists()) {
            modConfigDir.mkdirs();
        }

        return new File(modConfigDir, "steamturbine.cfg");
    }

    private static void initRotorConfigPath(File configFile) {
        File configRoot = configFile.getParentFile();
        rotorConfigFile = new File(configRoot, "rotors.json");
    }

    private static void loadRotorConfig() {
        if (!rotorConfigFile.exists()) {
            saveRotorConfig(rotorConfigFile, defaultRotors());
        }

        RotorConfigData data = null;
        try (Reader reader = new FileReader(rotorConfigFile)) {
            data = GSON.fromJson(reader, RotorConfigData.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        rotorDefinitions.clear();
        if (data != null && data.rotors != null) {
            for (RotorDefinition def : data.rotors) {
                RotorDefinition normalized = normalize(def);
                if (normalized != null) {
                    rotorDefinitions.add(normalized);
                }
            }
        }

        if (rotorDefinitions.isEmpty()) {
            rotorDefinitions.addAll(defaultRotors());
            // Only save if it's empty/missing to provide a valid template
            saveRotorConfig(rotorConfigFile, rotorDefinitions);
        }
    }

    private static void initResourcePackTemplate() {
        if (globalConfigDir == null) {
            return;
        }
        File runRoot = globalConfigDir.getParentFile();
        if (runRoot == null) {
            return;
        }

        File templateRoot = new File(runRoot, "resourcepacks/SteamTurbine-RotorIcons-Template");
        File itemsDir = new File(templateRoot, "assets/steamturbine/textures/items");
        if (!itemsDir.exists()) {
            itemsDir.mkdirs();
        }

        writePackMetaIfMissing(new File(templateRoot, "pack.mcmeta"));
        writeReadmeIfMissing(new File(templateRoot, "README.txt"));
        copyBundledIconIfMissing("rotor_iron.png", new File(itemsDir, "rotor_iron.png"));
        copyBundledIconIfMissing("rotor_steel.png", new File(itemsDir, "rotor_steel.png"));
        copyBundledIconIfMissing("rotor_titanium.png", new File(itemsDir, "rotor_titanium.png"));
    }

    private static void writePackMetaIfMissing(File packMeta) {
        if (packMeta.exists()) {
            return;
        }
        String json = "{\n" + "  \"pack\": {\n"
            + "    \"pack_format\": 1,\n"
            + "    \"description\": \"SteamTurbine rotor icon override template\"\n"
            + "  }\n"
            + "}\n";
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(packMeta), StandardCharsets.UTF_8)) {
            writer.write(json);
        } catch (Exception ignored) {}
    }

    private static void writeReadmeIfMissing(File readme) {
        if (readme.exists()) {
            return;
        }
        String text = "SteamTurbine rotor icon override template\n" + "\n"
            + "1) Put your custom icons into assets/steamturbine/textures/items/\n"
            + "2) Keep file names referenced by config/"
            + CONFIG_FOLDER_NAME
            + "/rotors.json field \"Icon\"\n"
            + "3) Enable this resource pack in Minecraft resource packs screen\n";
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(readme), StandardCharsets.UTF_8)) {
            writer.write(text);
        } catch (Exception ignored) {}
    }

    private static void copyBundledIconIfMissing(String fileName, File target) {
        if (target.exists()) {
            return;
        }
        String path = "/assets/steamturbine/textures/items/" + fileName;
        try (InputStream in = ModConfig.class.getResourceAsStream(path)) {
            if (in == null) {
                return;
            }
            try (FileOutputStream out = new FileOutputStream(target)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
            }
        } catch (Exception ignored) {}
    }

    private static RotorDefinition normalize(RotorDefinition def) {
        if (def == null) {
            return null;
        }
        String name = def.itemName == null ? "Rotor" : def.itemName.trim();
        if (name.isEmpty()) {
            name = "Rotor";
        }
        float overflow = Math.max(1.0f, def.overflow);
        int optimalFlow = Math.max(1, def.optimalFlow);
        float efficiency = def.efficiency <= 0 ? 0.01f : def.efficiency;
        int durability = Math.max(1, def.durability);
        String icon = def.icon == null ? null : def.icon.trim();
        if (icon != null && icon.isEmpty()) {
            icon = null;
        }
        return new RotorDefinition(
            def.comment,
            name,
            overflow,
            optimalFlow,
            efficiency,
            durability,
            icon,
            def.bladeIcon,
            def.infiniteDurability,
            def.hasBlade);
    }

    private static List<RotorDefinition> defaultRotors() {
        List<RotorDefinition> defaults = new ArrayList<RotorDefinition>();
        // If no icon is provided in the config, dynamic tinting will be triggered
        defaults.add(new RotorDefinition("Basic Iron Rotor", "Iron", 1.0f, 400, 0.80f, 4000, null, null, false, true));
        defaults.add(
            new RotorDefinition("Reinforced Steel Rotor", "Steel", 2.0f, 800, 0.90f, 8000, null, null, false, true));
        defaults.add(
            new RotorDefinition(
                "Advanced Titanium Rotor",
                "Titanium",
                3.0f,
                1600,
                1.00f,
                16000,
                null,
                null,
                false,
                true));
        return defaults;
    }

    private static void saveRotorConfig(File rotorConfigFile, List<RotorDefinition> definitions) {
        RotorConfigData data = new RotorConfigData();
        data.rotors = new ArrayList<RotorDefinition>(definitions);
        try (Writer writer = new FileWriter(rotorConfigFile)) {
            GSON.toJson(data, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getRotorCount() {
        return rotorDefinitions.size();
    }

    public static RotorDefinition getRotor(int meta) {
        if (rotorDefinitions.isEmpty()) {
            rotorDefinitions.addAll(defaultRotors());
        }
        int idx = clampRotorIndex(meta);
        return rotorDefinitions.get(idx);
    }

    public static String getRotorItemName(int meta) {
        return getRotor(meta).itemName;
    }

    public static float getRotorEfficiency(int meta) {
        return getRotor(meta).efficiency;
    }

    public static int getRotorOptimalFlow(int meta) {
        return getRotor(meta).optimalFlow;
    }

    public static float getRotorOverflow(int meta) {
        return getRotor(meta).overflow;
    }

    public static int getRotorDurability(int meta) {
        return getRotor(meta).durability;
    }

    public static String getRotorIconFileName(int meta) {
        return getRotor(meta).icon;
    }

    public static boolean isRotorInfiniteDurability(int meta) {
        return getRotor(meta).infiniteDurability;
    }

    public static boolean hasRotorBlade(int meta) {
        return getRotor(meta).hasBlade;
    }

    public static String getBladeIconFileName(int meta) {
        return getRotor(meta).bladeIcon;
    }

    private static final java.util.Map<Integer, Integer> colorCache = new java.util.HashMap<Integer, Integer>();

    public static void setCachedColor(int meta, int color) {
        colorCache.put(meta, color);
    }

    public static int getCachedColor(int meta) {
        Integer c = colorCache.get(meta);
        return c != null ? c : 0xFFFFFF;
    }

    private static int clampRotorIndex(int meta) {
        if (meta < 0 || meta >= rotorDefinitions.size()) {
            return 0;
        }
        return meta;
    }
}
