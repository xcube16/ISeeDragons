package io.github.xcube16.iseedragons;

import net.minecraftforge.common.config.Config;

import java.util.LinkedHashMap;
import java.util.Map;

// ugly global state...  :(
@Config(modid = ISeeDragons.MODID)
public class StaticConfig {

    @Config.Comment("A list of entities and view distences ")
    @Config.Name("EntityDistanceOverrides")
    public static Map<String, Integer> distanceOverrides;

    @Config.Comment("The default % chance for a block to drop items when smashed by a dragon")
    public static Integer defaultDropChance = 100;

    @Config.Comment("The default % chance for a block to play sound and particle effects when smashed by a dragon")
    public static Integer defaultEffectChance = 100;

    @Config.Comment("Disables advancements (can be used to stop log spam when recipes are tweaked)")
    public static boolean disableAdvancements = false;

    @Config.Comment("Mutes errors caused by broken advancements (can be used to stop log spam when recipes are tweaked)")
    public static boolean muteErroringAdvancements = false;

    @Config.Comment("Prevents lightning strikes from destroying items")
    public static boolean disableLightningItemDamage = true;

    @Config.Comment("A list of block drop % chances")
    @Config.Name("DropChances")
    public static Map<String, Integer> dropChances;

    @Config.Comment("A list of block effect % chances")
    @Config.Name("EffectChances")
    public static Map<String, Integer> effectChances;

    @Config.Comment("A list of tools/armor and there new repair item (note: only list one tool of a given 'ToolMaterial')")
    @Config.Name("RepairFixes")
    public static Map<String, String> repairFixes;

    @Config.Comment("A list of items that need to do extra damage to undead enemies")
    @Config.Name("ExtraUndeadDamage")
    public static Map<String, Float> extraUndeadDamage;

    static {
        distanceOverrides = new LinkedHashMap<>();
        distanceOverrides.put("iceandfire:firedragon", 256);
        distanceOverrides.put("iceandfire:icedragon", 256);
        distanceOverrides.put("iceandfire:seaserpent", 256);
        distanceOverrides.put("iceandfire:cyclops", 256);
        distanceOverrides.put("battletower:golem", 256);

        dropChances = new LinkedHashMap<>();
        dropChances.put("iceandfire:ash", 2);
        dropChances.put("iceandfire:chared_cobblestone", 2);
        dropChances.put("iceandfire:chared_stone", 2);
        dropChances.put("iceandfire:chared_grass", 2);
        dropChances.put("iceandfire:chared_dirt", 2);
        dropChances.put("iceandfire:chared_gravel", 2);
        dropChances.put("iceandfire:chared_grass_path", 2);
        dropChances.put("minecraft:cobblestone", 3);
        dropChances.put("minecraft:dirt", 3);
        dropChances.put("minecraft:grass", 4);
        dropChances.put("minecraft:sand", 3);
        dropChances.put("minecraft:stone", 2);
        dropChances.put("iceandfire:frozen_cobblestone", 2);
        dropChances.put("iceandfire:frozen_stone", 2);
        dropChances.put("iceandfire:frozen_grass", 2);
        dropChances.put("iceandfire:frozen_dirt", 2);
        dropChances.put("iceandfire:frozen_gravel", 2);
        dropChances.put("iceandfire:frozen_grass_path", 2);
        dropChances.put("iceandfire:frozen_splinters", 2);

        effectChances = new LinkedHashMap<>();
        effectChances.put("iceandfire:ash", 5);
        effectChances.put("iceandfire:chared_cobblestone", 5);
        effectChances.put("iceandfire:chared_stone", 5);
        effectChances.put("iceandfire:chared_dirt", 5);
        effectChances.put("iceandfire:chared_gravel", 5);
        effectChances.put("minecraft:dirt", 5);
        effectChances.put("minecraft:stone", 5);
        effectChances.put("iceandfire:frozen_cobblestone", 5);
        effectChances.put("iceandfire:frozen_stone", 5);
        effectChances.put("iceandfire:frozen_dirt", 5);
        effectChances.put("iceandfire:frozen_gravel", 5);
        effectChances.put("iceandfire:frozen_splinters", 5);

        repairFixes = new LinkedHashMap<>();
        repairFixes.put("aquaculture:neptunium_pickaxe", "aquaculture:loot,1");
        repairFixes.put("aquaculture:neptunium_chestplate", "aquaculture:loot,1");

        extraUndeadDamage = new LinkedHashMap<>();
        extraUndeadDamage.put("minecraft:bedrock", 100.0f);
    }
}
