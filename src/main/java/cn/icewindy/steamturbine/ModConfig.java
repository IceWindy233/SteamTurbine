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
 * 蒸汽涡轮 Mod 配置管理。
 */
public class ModConfig {

    public static final String CONFIG_FOLDER_NAME = "steamturbine";

    /** 最大输出电压 (EU/t)，默认 512 (HV) */
    public static int maxOutputVoltage = 512;

    /** 每 EU 消耗的普通蒸汽量 (mB)，默认 2（即 1L 蒸汽 = 0.5 EU） */
    public static int steamPerEU = 2;

    /** 每 EU 消耗的过热蒸汽量 (mB)，默认 1（即 1L = 1 EU） */
    public static int superheatedSteamPerEU = 1;

    /** 内部蒸汽缓冲罐容量 (mB)，默认 64000 */
    public static int tankCapacity = 64000;

    /** 结构检测间隔 (tick)，默认 40 */
    public static int checkInterval = 40;

    /** 转子损坏时是否爆炸 */
    public static boolean rotorBreakExplosion = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
        .create();

    private static Configuration config;
    private static File modConfigDir;
    private static File rotorConfigFile;
    private static File globalConfigDir;

    private static final List<RotorDefinition> rotorDefinitions = new ArrayList<RotorDefinition>();

    public static class RotorDefinition {

        @SerializedName("ItemName")
        public String itemName;

        @SerializedName("Overflow")
        public int overflow;

        @SerializedName("OptimalFlow")
        public int optimalFlow;

        @SerializedName("Efficiency")
        public float efficiency;

        @SerializedName("Durability")
        public int durability;

        @SerializedName("Icon")
        public String icon;

        @SerializedName("InfiniteDurability")
        public boolean infiniteDurability;

        public RotorDefinition() {}

        public RotorDefinition(String itemName, int overflow, int optimalFlow, float efficiency, int durability,
            String icon, boolean infiniteDurability) {
            this.itemName = itemName;
            this.overflow = overflow;
            this.optimalFlow = optimalFlow;
            this.efficiency = efficiency;
            this.durability = durability;
            this.icon = icon;
            this.infiniteDurability = infiniteDurability;
        }
    }

    private static class RotorConfigData {

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
            writeDefaultRotorConfig(rotorConfigFile);
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
            writeDefaultRotorConfig(rotorConfigFile);
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
        int overflow = Math.max(1, def.overflow);
        int optimalFlow = Math.max(1, def.optimalFlow);
        float efficiency = def.efficiency <= 0 ? 0.01f : def.efficiency;
        int durability = Math.max(1, def.durability);
        String icon = def.icon == null ? "rotor_iron.png" : def.icon.trim();
        if (icon.isEmpty()) {
            icon = "rotor_iron.png";
        }
        return new RotorDefinition(name, overflow, optimalFlow, efficiency, durability, icon, def.infiniteDurability);
    }

    private static List<RotorDefinition> defaultRotors() {
        List<RotorDefinition> defaults = new ArrayList<RotorDefinition>();
        defaults.add(new RotorDefinition("Iron", 1, 400, 0.80f, 4000, "rotor_iron.png", false));
        defaults.add(new RotorDefinition("Steel", 2, 800, 0.90f, 8000, "rotor_steel.png", false));
        defaults.add(new RotorDefinition("Titanium", 3, 1600, 1.00f, 16000, "rotor_titanium.png", false));
        return defaults;
    }

    private static void writeDefaultRotorConfig(File rotorConfigFile) {
        RotorConfigData data = new RotorConfigData();
        data.rotors = defaultRotors();
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

    public static int getRotorOverflow(int meta) {
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

    private static int clampRotorIndex(int meta) {
        if (meta < 0 || meta >= rotorDefinitions.size()) {
            return 0;
        }
        return meta;
    }
}
