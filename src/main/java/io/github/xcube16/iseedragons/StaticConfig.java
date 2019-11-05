package io.github.xcube16.iseedragons;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Config;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// ugly global state...  :(
@Config(modid = ISeeDragons.MODID)
public class StaticConfig {

    @Config.Comment("A list of entities and view distences ")
    @Config.Name("EntityDistanceOverrides")
    public static Map<String, Integer> distanceOverrides = new LinkedHashMap<>();

    @Config.Comment("The default % chance for a block to drop items when smashed by a dragon")
    public static Integer defaultDropChance = 100;

    @Config.Comment("The default % chance for a block to play sound and particle effects when smashed by a dragon")
    public static Integer defaultEffectChance = 100;

    @Config.Comment("A list of block drop % chances")
    @Config.Name("DropChances")
    public static Map<String, Integer> dropChances = new LinkedHashMap<>();

    @Config.Comment("A list of block effect % chances")
    @Config.Name("EffectChances")
    public static Map<String, Integer> effectChances = new LinkedHashMap<>();
}
