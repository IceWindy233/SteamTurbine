package cn.icewindy.steamturbine;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

/**
 * 蒸汽涡轮 Mod 配置管理。
 * 所有可调参数集中在此类中，通过 Forge Configuration API 读写。
 */
public class ModConfig {

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

    /** 转子效率配置（铁/钢/钛） */
    public static float[] rotorEfficiency = new float[] { 0.80f, 0.90f, 1.00f };

    /** 转子最优流量配置（铁/钢/钛） */
    public static int[] rotorOptimalFlow = new int[] { 400, 800, 1600 };

    /** 转子溢流等级配置（铁/钢/钛） */
    public static int[] rotorOverflow = new int[] { 1, 2, 3 };

    /** 转子耐久配置（铁/钢/钛） */
    public static int[] rotorDurability = new int[] { 4000, 8000, 16000 };

    private static Configuration config;

    public static void init(File configFile) {
        config = new Configuration(configFile);
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

        loadRotorConfig();

        if (config.hasChanged()) {
            config.save();
        }
    }

    private static void loadRotorConfig() {
        final String cat = "rotor";

        rotorEfficiency[0] = config.getFloat("ironEfficiency", cat, 0.80f, 0.05f, 5.0f, "Iron rotor efficiency");
        rotorEfficiency[1] = config.getFloat("steelEfficiency", cat, 0.90f, 0.05f, 5.0f, "Steel rotor efficiency");
        rotorEfficiency[2] = config
            .getFloat("titaniumEfficiency", cat, 1.00f, 0.05f, 5.0f, "Titanium rotor efficiency");

        rotorOptimalFlow[0] = config.getInt("ironOptimalFlow", cat, 400, 1, 200000, "Iron rotor optimal flow (L/t)");
        rotorOptimalFlow[1] = config.getInt("steelOptimalFlow", cat, 800, 1, 200000, "Steel rotor optimal flow (L/t)");
        rotorOptimalFlow[2] = config
            .getInt("titaniumOptimalFlow", cat, 1600, 1, 200000, "Titanium rotor optimal flow (L/t)");

        rotorOverflow[0] = config.getInt("ironOverflow", cat, 1, 1, 10, "Iron rotor overflow level");
        rotorOverflow[1] = config.getInt("steelOverflow", cat, 2, 1, 10, "Steel rotor overflow level");
        rotorOverflow[2] = config.getInt("titaniumOverflow", cat, 3, 1, 10, "Titanium rotor overflow level");

        rotorDurability[0] = config.getInt("ironDurability", cat, 4000, 1, Integer.MAX_VALUE, "Iron rotor durability");
        rotorDurability[1] = config
            .getInt("steelDurability", cat, 8000, 1, Integer.MAX_VALUE, "Steel rotor durability");
        rotorDurability[2] = config
            .getInt("titaniumDurability", cat, 16000, 1, Integer.MAX_VALUE, "Titanium rotor durability");
    }

    public static float getRotorEfficiency(int meta) {
        int idx = clampRotorIndex(meta);
        return rotorEfficiency[idx];
    }

    public static int getRotorOptimalFlow(int meta) {
        int idx = clampRotorIndex(meta);
        return rotorOptimalFlow[idx];
    }

    public static int getRotorOverflow(int meta) {
        int idx = clampRotorIndex(meta);
        return rotorOverflow[idx];
    }

    public static int getRotorDurability(int meta) {
        int idx = clampRotorIndex(meta);
        return rotorDurability[idx];
    }

    private static int clampRotorIndex(int meta) {
        if (meta < 0 || meta >= rotorEfficiency.length) {
            return 0;
        }
        return meta;
    }
}
