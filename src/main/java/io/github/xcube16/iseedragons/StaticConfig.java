/*
MIT License

Copyright (c) 2021 xcube16

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package io.github.xcube16.iseedragons;

import net.minecraftforge.common.config.Config;

import java.util.LinkedHashMap;
import java.util.Map;

@Config(modid = ISeeDragons.MODID)
public class StaticConfig {

    @Config.Comment("A list of entities and view distances ")
    @Config.Name("EntityDistanceOverrides")
    public static Map<String, Integer> distanceOverrides;

    @Config.Comment("The default % chance for a block to drop items when smashed by a dragon (requires ASM.DragonLag)")
    public static Integer defaultDropChance = 100;

    @Config.Comment("The default % chance for a block to play sound and particle effects when smashed by a dragon (requires ASM.DragonLag)")
    public static Integer defaultEffectChance = 100;

    @Config.Comment("Prevents lightning strikes from destroying items")
    public static boolean disableLightningItemDamage = false;

    @Config.Comment("Prevents Tough As Nails from creating an extra attack entity event")
    @Config.RequiresMcRestart
    public static boolean preventTANAttackEntityEvent = true;

    @Config.Comment("A list of block drop % chances (requires ASM.DragonLag)")
    @Config.Name("DropChances")
    public static Map<String, Integer> dropChances;

    @Config.Comment("A list of block effect % chances (requires ASM.DragonLag)")
    @Config.Name("EffectChances")
    public static Map<String, Integer> effectChances;

    @Config.Comment("A list of tools/armor and there new repair item (note: only list one tool of a given 'ToolMaterial')")
    @Config.Name("RepairFixes")
    public static Map<String, String> repairFixes;

    @Config.Comment("A list of items that need to do extra damage to undead enemies")
    @Config.Name("ExtraUndeadDamage")
    public static Map<String, Float> extraUndeadDamage;

    @Config.Comment("A list of dimension IDs that Ice and Fire should NOT generate ANY structures in.")
    @Config.Name("iceandfire_structure_dim_blacklist")
    public static int[] generatorBlacklist;

    @Config.Comment("Minimum brightness override (can be negative)")
    public static float minBrightness = 0.0f;

    @Config.Comment("Maximum brightness override (can be negative)")
    public static float maxBrightness = 1.0f;

    @Config.Comment("Core modifications")
    @Config.Name("ASM")
    public static ASM asm = new ASM();

    public static final class ASM {

        @Config.Comment("Patches EntityDragonBase to help with lag")
        @Config.Name("DragonLag")
        public boolean dragonLag = true;

        @Config.Comment("Patches ItemModAxe to allow axes to work on modded wood blocks")
        @Config.Name("IceAndFireAxeFix")
        public boolean iceAndFireAxeFix = true;

        @Config.Comment("Mutes harmless noisy warnings/errors in the RLCraft modpack")
        @Config.Name("STFU")
        public boolean stfu = true;

        @Config.Comment("Prevents Myrmex eggs from being duped when broken with multiple damage events in one tick")
        @Config.Name("FixMyrmexDupeBug")
        public boolean fixMyrmexDupeBug = true;

        @Config.Comment("Removes everything from the vanilla achievements system! Can be used to stop log spam when recipes are tweaked.")
        @Config.Name("NukeAchievements")
        public boolean nukeAchievements = false;

        @Config.Comment("Fixes a bug and allows the last Ice and Fire Sea Serpent type to spawn")
        @Config.Name("FixSeaSerpentSpawn")
        public boolean fixSeaSerpentSpawn = true;

        @Config.Comment("Part of a complex fix to prevent players from using dismount (pressing 'shift') to escape a dragon's jaws")
        @Config.Name("DragonDismountFix")
        public boolean dragonDismountFix = true;

        @Config.Comment("Adds a hook to Ice and Fire's StructureGenerator so we can cancel generation in some worlds")
        @Config.Name("HookStructureGenerator")
        public boolean hookStructureGenerator = true;
    }

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

        generatorBlacklist = new int[]{111};
    }
}
