package io.github.xcube16.iseedragons;

import com.google.common.collect.BiMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Mod(modid= ISeeDragons.MODID, version = ISeeDragons.VERSION, acceptableRemoteVersions = "*", name = ISeeDragons.NAME)
public class ISeeDragons {
    public static final String MODID = "iseedragons";
    public static final String NAME = "ISeeDragons";
    public static final String VERSION = "0.6.1";
    public static final Logger logger = LogManager.getLogger(NAME);

    @Nullable // lazy init
    private Method dragonSetSleeping;
    @Nullable // lazy init
    private Field dragonCurrentAnimation;
    @Nullable
    private Field dragonAnimationTick;
    @Nullable // lazy init
    private Field dragon_ANIMATION_SHAKEPREY;

    // static is kind of ugly, but so are ASM hooks and hacks :P
    private static Map<Block, Integer> dropChances;
    private static Map<Block, Integer> effectChances;

    @Mod.EventHandler
    public void preinit(FMLPreInitializationEvent event)
    {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {

        this.loadConfig();

        logger.info("Fixing sea serpent armor repair...");
        try {
            Class enumSeaSerpent = Class.forName("com.github.alexthe666.iceandfire.enums.EnumSeaSerpent");
            Object[] values = (Object[]) enumSeaSerpent.getDeclaredMethod("values").invoke(null);
            Field armorMaterialField = enumSeaSerpent.getDeclaredField("armorMaterial");
            Field scaleField = enumSeaSerpent.getDeclaredField("scale");
            for (Object serpent : values) {
                ItemArmor.ArmorMaterial material = ((ItemArmor.ArmorMaterial) armorMaterialField.get(serpent));
                material.setRepairItem(new ItemStack((Item) scaleField.get(serpent)));
                logger.info("Fixed " + material);
            }
        } catch (ClassNotFoundException e) {
            logger.warn("Could not find " + e.getMessage() + " while trying to fix sea serpent armor");
        } catch (Exception e) {
            logger.error("Failed to fix sea serpent armor", e);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW) // run after Ice and Fire
    public void registerItems(RegistryEvent.Register<Item> event) {
        logger.info("Fixing Ice and Fire axes");
        OreDictionary.registerOre("toolAxe", Item.getByNameOrId("iceandfire:dragonbone_axe"));
        OreDictionary.registerOre("toolAxe", Item.getByNameOrId("iceandfire:myrmex_desert_axe"));
        OreDictionary.registerOre("toolAxe", Item.getByNameOrId("iceandfire:myrmex_jungle_axe"));
        OreDictionary.registerOre("toolAxe", Item.getByNameOrId("iceandfire:silver_axe"));
        //OreDictionary.registerOre("toolAxe", Item.getByNameOrId("iceandfire:dragonsteel_fire_axe"));
        //OreDictionary.registerOre("toolAxe", Item.getByNameOrId("iceandfire:dragonsteel_ice_axe"));

        logger.info("Fixing broken Ice and Fire OreDictionary entries. (all that ore dictionary spam that you can now ignore!)");
        OreDictionary.registerOre("ingotSilver",   Item.getByNameOrId("iceandfire:silver_ingot"));
        OreDictionary.registerOre("nuggetSilver",  Item.getByNameOrId("iceandfire:silver_nugget"));
        OreDictionary.registerOre("oreSilver",     Item.getByNameOrId("iceandfire:silver_ore"));
        OreDictionary.registerOre("blockSilver",   Item.getByNameOrId("iceandfire:silver_block"));
        OreDictionary.registerOre("gemSapphire",   Item.getByNameOrId("iceandfire:sapphire_gem"));
        OreDictionary.registerOre("oreSapphire",   Item.getByNameOrId("iceandfire:sapphire_ore"));
        OreDictionary.registerOre("blockSapphire", Item.getByNameOrId("iceandfire:sapphire_block"));
        OreDictionary.registerOre("boneWither",    Item.getByNameOrId("iceandfire:witherbone"));
        /*OreDictionary.registerOre("woolBlock", new ItemStack(Blocks.field_150325_L, 1, 32767));
        OreDictionary.registerOre("foodMeat", Items.field_151076_bf);
        OreDictionary.registerOre("foodMeat", Items.field_151077_bg);
        OreDictionary.registerOre("foodMeat", Items.field_151082_bd);
        OreDictionary.registerOre("foodMeat", Items.field_151083_be);
        OreDictionary.registerOre("foodMeat", Items.field_151147_al);
        OreDictionary.registerOre("foodMeat", Items.field_151157_am);
        OreDictionary.registerOre("foodMeat", Items.field_179561_bm);
        OreDictionary.registerOre("foodMeat", Items.field_179557_bn);
        OreDictionary.registerOre("foodMeat", Items.field_179558_bo);
        OreDictionary.registerOre("foodMeat", Items.field_179559_bp);*/
        OreDictionary.registerOre("boneWithered",  Item.getByNameOrId("iceandfire:witherbone"));
        OreDictionary.registerOre("boneDragon",    Item.getByNameOrId("iceandfire:dragonbone"));
        try {
            Class enumSeaSerpent = Class.forName("com.github.alexthe666.iceandfire.enums.EnumSeaSerpent");
            Object[] values = (Object[]) enumSeaSerpent.getDeclaredMethod("values").invoke(null);
            Field scaleField = enumSeaSerpent.getDeclaredField("scale");
            for (Object serpent : values) {
                OreDictionary.registerOre("seaSerpentScales", (Item) scaleField.get(serpent));
            }
        } catch (ClassNotFoundException e) {
            logger.warn("Could not find " + e.getMessage() + " while trying to fix sea serpent scales ore dictionary");
        } catch (Exception e) {
            logger.error("Failed register sea serpent scales in ore dictionary", e);
        }

        this.registerEgg(Item.getByNameOrId("iceandfire:hippogryph_egg"));
        //this.registerEgg(Item.getByNameOrId("iceandfire:deathworm_egg"));
        this.registerEgg(Item.getByNameOrId("iceandfire:myrmex_jungle_egg"));
        this.registerEgg(Item.getByNameOrId("iceandfire:myrmex_desert_egg"));
        logger.info("Done fixing Ice and Fire ore dictionary");
    }

    @SubscribeEvent
    public void onConfigChange(ConfigChangedEvent.OnConfigChangedEvent e) {
        if (e.getModID().equals(ISeeDragons.MODID)) {
            ConfigManager.sync(ISeeDragons.MODID, Config.Type.INSTANCE);

            loadConfig();
        }
    }

    private void loadConfig() {
        dropChances = loadBlockChanceMapping(StaticConfig.dropChances);
        effectChances = loadBlockChanceMapping(StaticConfig.effectChances);

        logger.info("Scanning for Ice and Fire dragons...");
        boolean foundOne = false;
        try {
            Field regField = EntityRegistry.instance().getClass().getDeclaredField("entityClassRegistrations");
            regField.setAccessible(true);
            BiMap<Class<? extends Entity>, EntityRegistry.EntityRegistration> reg =
                    (BiMap<Class<? extends Entity>, EntityRegistry.EntityRegistration>)regField.get(EntityRegistry.instance());
            for (EntityRegistry.EntityRegistration entity : reg.values()) {
                //logger.info(entity.getRegistryName().toString());
                Optional boost = this.getRenderBoost(entity.getRegistryName());
                if (boost.isPresent()) {
                    foundOne = true;
                    logger.info("Fixed " + entity.getRegistryName() + " tracking distance");
                    Field rangeField = entity.getClass().getDeclaredField("trackingRange");
                    rangeField.setAccessible(true);
                    rangeField.set(entity, boost.get());
                }
            }
        } catch (Exception e) {
            logger.error("Failed to fix Ice and Fire entity tracking distance", e);
        }
        if (!foundOne) {
            logger.warn("No Ice and Fire dragons found! Is it installed?");
        }
    }

    private Map<Block, Integer> loadBlockChanceMapping(Map<String, Integer> input) {
        Map<Block, Integer> map = new HashMap<>();
        for (Map.Entry<String, Integer> entry : input.entrySet()) {
            @Nullable
            Block block = Block.REGISTRY.getObject(new ResourceLocation(entry.getKey()));
            if (block == Blocks.AIR) { // FIXME: air is the default form Block.REGISTRY, but what if someone adds air to the list
                logger.warn("Could not find block \"" + entry.getKey() + "\", ignoring!");
            } else {
                // clamp to range 0%-100%
                map.put(block, Math.min(Math.max(entry.getValue(), 0), 100));
            }
        }
        return map;
    }

    /**
     * This method is called by EntityDragonBase from ASMed code! Don't change its signature!
     *
     * @param pos
     * @param world
     * @param state
     * @return dummy value
     */
    public static boolean dragonBreakBlockHook(World world, BlockPos pos, IBlockState state) {
        Block block = state.getBlock();
        int chance = Optional.ofNullable(dropChances.get(block)).orElse(StaticConfig.defaultDropChance);

        boolean shouldDrop = true;
        if (chance == 0) {
            shouldDrop = false;
        } else if (chance != 100) {
            shouldDrop = world.rand.nextInt(100) < chance;
        }

        if (!block.isAir(state, world, pos)) {

            chance = Optional.ofNullable(effectChances.get(block)).orElse(StaticConfig.defaultEffectChance);
            if (chance == 100 || (chance != 0 && world.rand.nextInt(100) < chance)) {
                world.playEvent(2001, pos, Block.getStateId(state));
            }

            if (shouldDrop)
            {
                block.dropBlockAsItem(world, pos, state, 0);
            }
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
        }

        return true; // DUMMY RETURN VALUE
    }

    private void registerEgg(Item egg) {
        OreDictionary.registerOre("listAllEgg",    new ItemStack(egg, 1, 32767));
        OreDictionary.registerOre("objectEgg",     new ItemStack(egg, 1, 32767));
        OreDictionary.registerOre("bakingEgg",     new ItemStack(egg, 1, 32767));
        OreDictionary.registerOre("egg",           new ItemStack(egg, 1, 32767));
        OreDictionary.registerOre("ingredientEgg", new ItemStack(egg, 1, 32767));
        OreDictionary.registerOre("foodSimpleEgg", new ItemStack(egg, 1, 32767));
    }

    @SubscribeEvent
    public void livingEntityAttacked(LivingAttackEvent e) {
        //EntityRegistry.getEntry(e.getEntity().getClass()).getRegistryName();

        //note: EntityList.getKey(e.getEntity()) is null when a/the player is hit
        if (this.isDragon(EntityList.getKey(e.getEntity())) && e.getAmount() > 0.0F) {
            try {
                if (this.dragonSetSleeping == null) {
                    this.dragonSetSleeping = this.getMethodInHierarchy(e.getEntity().getClass(), "setSleeping", boolean.class);
                    this.dragonSetSleeping.setAccessible(true); // it was public last I checked, but lets just do this anyway
                }
                this.dragonSetSleeping.invoke(e.getEntity(), false);
            } catch (Exception ex) {
                logger.error("Failed to wake up Ice and Fire dragon", ex);
            }
        }
    }

    @SubscribeEvent()
    public void onDismount(EntityMountEvent e) {
        if (e.getEntityMounting() instanceof EntityPlayer) {
            if (e.isDismounting()) {
                if (!this.canDismount(e.getEntityBeingMounted())) {
                    e.setCanceled(true);
                }
            } else {
                @Nullable
                Entity previousMount = e.getEntityMounting().getRidingEntity();
                if (previousMount != null && !this.canDismount(previousMount)) {
                    e.setCanceled(true);
                }
            }
        }
    }

    private boolean canDismount(Entity entity) {
        if (!entity.isDead && this.isDragon(EntityList.getKey(entity.getClass()))) {
            try {
                if (this.dragonCurrentAnimation == null) {
                    this.dragonCurrentAnimation = this.getFieldInHierarchy(entity.getClass(), "currentAnimation");
                    this.dragonCurrentAnimation.setAccessible(true);
                }
                if (this.dragonAnimationTick == null) {
                    this.dragonAnimationTick = this.getFieldInHierarchy(entity.getClass(), "animationTick");
                    this.dragonAnimationTick.setAccessible(true);
                }
                if (this.dragon_ANIMATION_SHAKEPREY == null) {
                    this.dragon_ANIMATION_SHAKEPREY = this.getFieldInHierarchy(entity.getClass(), "ANIMATION_SHAKEPREY");
                }

                Object animation = this.dragonCurrentAnimation.get(entity);
                Object aniShakePray = this.dragon_ANIMATION_SHAKEPREY.get(entity);
                int aniTick = (Integer) this.dragonAnimationTick.get(entity);

                if (animation == aniShakePray && aniTick <= 55) {
                    return false;
                }

            } catch (Exception ex) {
                logger.error("Failed to check dragon state", ex);
            }
        }
        return true;
    }

    private Optional<Integer> getRenderBoost(@Nullable ResourceLocation id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(StaticConfig.distanceOverrides.get(id.toString()));
        /*if (id == null) {
            return Optional.empty();
        } else if (this.isDragon(id) ||
                id.getResourcePath().equals("seaserpent") ||
                id.getResourcePath().equals("golem") && id.getResourceDomain().equals("battletowers")) {
            return Optional.of(256);
        } else if (id.getResourcePath().equals("cyclops")) {
            return Optional.of(160);
        }
        return Optional.empty();*/
    }

    private boolean isDragon(@Nullable ResourceLocation id) {
        if (id == null) {
            return false;
        }

        return id.getResourceDomain().equals("iceandfire") && (
                id.getResourcePath().equals("icedragon") ||
                id.getResourcePath().equals("firedragon"));
    }

    private Method getMethodInHierarchy(Class<?> clazz, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        try {
            return clazz.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            if (clazz.getSuperclass() != null) {
                return this.getMethodInHierarchy(clazz.getSuperclass(), name, parameterTypes);
            }
            throw e;
        }
    }

    private Field getFieldInHierarchy(Class<?> clazz, String name) throws NoSuchFieldException {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                return this.getFieldInHierarchy(clazz.getSuperclass(), name);
            }
            throw e;
        }
    }
}
