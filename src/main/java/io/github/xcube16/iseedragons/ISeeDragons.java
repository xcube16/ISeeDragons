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

import com.google.common.collect.BiMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptionSlider;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.*;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.IEventListener;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod(modid = ISeeDragons.MODID, version = ISeeDragons.VERSION, acceptableRemoteVersions = "*", name = ISeeDragons.NAME)
public class ISeeDragons {
    public static final String MODID = ISD.MODID;
    public static final String NAME = ISD.NAME;
    public static final String VERSION = ISD.VERSION;
    public static final Logger logger = ISD.logger;

    @Mod.Instance(ISeeDragons.MODID)
    private static ISeeDragons instance;

    @Nullable // lazy init
    private Method dragonSetSleeping;
    @Nullable // lazy init
    private Field dragonCurrentAnimation;
    @Nullable
    private Field dragonAnimationTick;
    @Nullable // lazy init
    private Field dragon_ANIMATION_SHAKEPREY;

    @Nullable // lazy init
    private Field cyclopsCurrentAnimation;
    @Nullable
    private Field cyclopsAnimationTick;
    @Nullable // lazy init
    private Field cyclops_ANIMATION_EATPLAYER;

    // static is kind of ugly, but so are ASM hooks and hacks :P
    private static Map<Block, Integer> dropChances;
    private static Map<Block, Integer> effectChances;

    private Map<Item, Float> extraUndeadDamage;

    @Mod.EventHandler
    public void preinit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SuppressWarnings("deprecation")
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

        logger.info("Fixing tools/armor repair listed in config...");
        StaticConfig.repairFixes.forEach((toolId, repairItem) -> {
            String[] split = repairItem.split(",");
            int meta = 0;
            if (split.length == 1) {
                this.fixToolRepair(toolId, split[0], 0);
            } else if (split.length == 2) {
                this.fixToolRepair(toolId, split[0], Integer.parseInt(split[1]));
            } else {
                logger.error("Bad item string " + repairItem);
            }
        });

        if (StaticConfig.preventTANAttackEntityEvent && Loader.isModLoaded("toughasnails")) {
            logger.info("Fixing TAN's attack entity event damage problem...");
            boolean found_listener = false;
            //Find the ThirstStatHandler in the event bus and remove it
            ConcurrentHashMap<Object, ArrayList<IEventListener>> listeners = ReflectionHelper.getPrivateValue(EventBus.class, MinecraftForge.EVENT_BUS, "listeners");
            for (Map.Entry<Object, ArrayList<IEventListener>> listener_entry : listeners.entrySet()) {
                if (listener_entry.getKey().getClass().getName().equals("toughasnails.handler.thirst.ThirstStatHandler")) {
                    found_listener = true;
                    //Create the FakeThirstStatHandler and send it the real one to use
                    try {
                        MinecraftForge.EVENT_BUS.register(new FakeThirstStatHandler(listener_entry.getKey()));
                    } catch (Exception e) {
                        //If an exception is thrown, FakeThirstStatHandler never gets added to the event bus
                        logger.error("Failed to initialize FakeThirstStatHandler");
                        e.printStackTrace();
                        break;
                    }

                    //Unregister the real ThirstStatHandler
                    MinecraftForge.EVENT_BUS.unregister(listener_entry.getKey());
                    break;
                }
            }
            if (!found_listener) {
                logger.error("Could not find toughasnails ThirstStatHandler event listener");
                return;
            }
        }
    }

    private void fixToolRepair(String toolId, String repairItemId, int meta) {
        try {
            Item tool = Item.getByNameOrId(toolId);
            if (tool == null) {
                logger.info("Could not find " + toolId + ", ignoring");
                return;
            }
            if (tool instanceof ItemTool) {
                Field toolMaterialField = ItemTool.class.getDeclaredField("field_77862_b"); // toolMaterial
                toolMaterialField.setAccessible(true);
                Object toolMaterial = toolMaterialField.get(tool);

                if (toolMaterial instanceof Item.ToolMaterial) {
                    @Nullable
                    Item repairItem = Item.getByNameOrId(repairItemId);
                    if (repairItem != null) {
                        ((Item.ToolMaterial) toolMaterial).setRepairItem(new ItemStack(repairItem, 1, meta));
                        logger.info(toolId + " can now be repaired with " + repairItemId);
                    } else {
                        logger.error(repairItemId + " does not exist! Failed to fix " + toolId + " repair!");
                    }
                } else {
                    logger.error(toolId + " has a bad tool material of " + toolMaterial);
                }
            } else if (tool instanceof ItemArmor) {
                Field materialField = ItemArmor.class.getDeclaredField("field_77878_bZ"); // material
                materialField.setAccessible(true);
                Object toolMaterial = materialField.get(tool);

                if (toolMaterial instanceof ItemArmor.ArmorMaterial) {
                    @Nullable
                    Item repairItem = Item.getByNameOrId(repairItemId);
                    if (repairItem != null) {
                        ((ItemArmor.ArmorMaterial) toolMaterial).setRepairItem(new ItemStack(repairItem, 1, meta));
                        logger.info(toolId + " can now be repaired with " + repairItemId);
                    } else {
                        logger.error(repairItemId + " does not exist! Failed to fix " + toolId + " repair!");
                    }
                } else {
                    logger.error(toolId + " has a bad armor material of " + toolMaterial);
                }
            } else if (tool instanceof ItemSword) {
                Field materialField = ItemSword.class.getDeclaredField("field_150933_b"); // material
                materialField.setAccessible(true);
                Object toolMaterial = materialField.get(tool);

                if (toolMaterial instanceof Item.ToolMaterial) {
                    @Nullable
                    Item repairItem = Item.getByNameOrId(repairItemId);
                    if (repairItem != null) {
                        ((ItemArmor.ToolMaterial) toolMaterial).setRepairItem(new ItemStack(repairItem, 1, meta));
                        logger.info(toolId + " can now be repaired with " + repairItemId);
                    } else {
                        logger.error(repairItemId + " does not exist! Failed to fix " + toolId + " repair!");
                    }
                } else {
                    logger.error(toolId + " has a bad sword material of " + toolMaterial);
                }
            } else {
                logger.info(toolId + " is not a tool, armor, or sword");
            }
        } catch (Exception e) {
            logger.error("Critical error while fixing tool/armor repair for " + toolId, e);
        }
    }

    @SubscribeEvent
    public void lightningStruckEntity(EntityStruckByLightningEvent event) {
        //Prevent lightning from destroying items
        if (StaticConfig.disableLightningItemDamage && (event.getEntity() instanceof EntityItem))
            event.setCanceled(true);
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
    public void onLivingHurt(LivingHurtEvent e) {
        DamageSource source = e.getSource();
        if (e.getEntityLiving().getCreatureAttribute() != EnumCreatureAttribute.UNDEAD ||
                source.isExplosion() || source.isFireDamage() || source.isMagicDamage() || source.isProjectile() ||
                !(source.getDamageType().equals("player") || source.getDamageType().equals("mob"))) {
            return;
        }

        if (source.getTrueSource() != null && source.getTrueSource() instanceof EntityLivingBase) {
            ItemStack weapon = ((EntityLivingBase) source.getTrueSource()).getHeldItemMainhand();
            if (!weapon.isEmpty()) {
                @Nullable
                Float extra = this.extraUndeadDamage.get(weapon.getItem());
                if (extra != null) {
                    e.setAmount(e.getAmount() + extra);
                }
            }
        }
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

        Map<Item, Float> extraUndeadDamage = new HashMap<>();
        for (Map.Entry<String, Float> entry : StaticConfig.extraUndeadDamage.entrySet()) {
            Item weapon = Item.getByNameOrId(entry.getKey());
            if (weapon != Items.AIR) {
                extraUndeadDamage.put(weapon, entry.getValue());
            } else {
                logger.warn(entry.getKey() + " was not found!");
            }
        }
        this.extraUndeadDamage = extraUndeadDamage;

        GameSettings.Options.GAMMA.valueMin = StaticConfig.minBrightness;
        GameSettings.Options.GAMMA.valueMax = StaticConfig.maxBrightness;

        logger.info("Scanning for Ice and Fire dragons...");
        boolean foundOne = false;
        try {
            Field regField = EntityRegistry.instance().getClass().getDeclaredField("entityClassRegistrations");
            regField.setAccessible(true);
            BiMap<Class<? extends Entity>, EntityRegistry.EntityRegistration> reg =
                    (BiMap<Class<? extends Entity>, EntityRegistry.EntityRegistration>) regField.get(EntityRegistry.instance());
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
     * This method is called by EntityDragonBase from ASM'ed code! Don't change its signature!
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

            if (shouldDrop) {
                block.dropBlockAsItem(world, pos, state, 0);
            }
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
        }

        return true; // DUMMY RETURN VALUE
    }

    /**
     * This method is called by EntityDragonBase from ASM'ed code! Don't change its signature!
     *
     * @param dragon
     * @param prey
     * @param animTick
     */
    public static void dragonShakePrayHook(EntityTameable dragon, Entity prey, int animTick) {
        if (dragon.world.isRemote) return;
        if (animTick == 56) {
            prey.attackEntityFrom(
                    DamageSource.causeMobDamage(dragon),
                    prey instanceof EntityPlayer ? 17.0F :
                            (float) dragon.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue() * 4.0F);
        } else if (animTick > 60) {
            // animTick should always be >= 56, but lets leave a sanity check
            // in case someone messes with the ASM code
            prey.dismountRidingEntity();

            // NOTE: HANDLE POST-DISMOUNT EFFECTS IN THE EVENT HANDLER!
            // (that way dismounting with shift in the last tick still causes the effects)
        }
    }

    /**
     * Called from EntityPlayer when
     * (!player.world.isRemote && player.isSneaking() && player.isRiding())
     * to see if the player is allowed to dismount. Canceling Forge's event causes updateRidden() to
     * not be called when a player is holding shift.
     *
     * note: This does NOT get called when a player mounts another entity.
     * (ex: right-clicking a boat to escape a minecart)
     *
     * @param player The mounted player
     * @return True to allow dismount, false to block and do normal updateRidden()
     */
    public static boolean canPlayerDismountHook(EntityPlayer player) {
        Entity mount = player.getRidingEntity();
        if (mount != null) {
            return ISeeDragons.instance.canDismount(mount);
        }
        return true;
    }

    public static ResourceLocation[] cleanAdvancementRequardsHook(ResourceLocation[] craftable) {
        return Arrays.stream(craftable)
                .filter(item -> CraftingManager.getRecipe(item) != null)
                .toArray(ResourceLocation[]::new);
    }

    /**
     * Called by {@link com.github.alexthe666.iceandfire.event.StructureGenerator#generate(Random, int, int, World, IChunkGenerator, IChunkProvider)}
     * to see if Ice and Fire structures are allowed to generate in the given world/dimension.
     * 
     * @param world The world that the structure might be generated in
     * @return True if generation should be allowed to continue
     */
    public static boolean iceAndFireGenerateHook(World world) {
        return !Arrays.stream(StaticConfig.generatorBlacklist)
                .anyMatch(i -> i == world.provider.getDimension());
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
        if (this.isDragon(e.getEntity()) && e.getAmount() > 0.0F) {
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
        if (!e.isCanceled() && e.getEntityMounting() instanceof EntityPlayer) {
            if (e.isDismounting()) {
                if (!this.canDismount(e.getEntityBeingMounted())) {
                    e.setCanceled(true);
                } else {
                    // Fixes TAN picking up on infinite micro-jump thingy when a player dismounts anything while holding space
                    ((EntityPlayer) e.getEntityMounting()).setJumping(false);

                    this.handleDismount(e.getEntityBeingMounted(), e.getEntityMounting());
                }
            } else {
                @Nullable
                Entity previousMount = e.getEntityMounting().getRidingEntity();
                if (previousMount != null) {
                    if (!this.canDismount(previousMount)) {
                        e.setCanceled(true);
                    } else {
                        this.handleDismount(previousMount, e.getEntityMounting());
                    }
                }
            }
        }
    }

    private boolean canDismount(Entity dragon) {
        if (dragon.isDead || (dragon instanceof EntityLivingBase && ((EntityLivingBase) dragon).getHealth() <= 0.0f)) { // sanity check. Yes, players get stuck without this :P
            return true;
        }
        if (this.isDragon(dragon)) {
            try {
                if (this.dragonCurrentAnimation == null) {
                    this.dragonCurrentAnimation = this.getFieldInHierarchy(dragon.getClass(), "currentAnimation");
                    this.dragonCurrentAnimation.setAccessible(true);
                }
                if (this.dragonAnimationTick == null) {
                    this.dragonAnimationTick = this.getFieldInHierarchy(dragon.getClass(), "animationTick");
                    this.dragonAnimationTick.setAccessible(true);
                }
                if (this.dragon_ANIMATION_SHAKEPREY == null) {
                    this.dragon_ANIMATION_SHAKEPREY = this.getFieldInHierarchy(dragon.getClass(), "ANIMATION_SHAKEPREY");
                }

                Object animation = this.dragonCurrentAnimation.get(dragon);
                Object aniShakePray = this.dragon_ANIMATION_SHAKEPREY.get(dragon);
                int aniTick = (Integer) this.dragonAnimationTick.get(dragon);

                if (animation == aniShakePray && aniTick <= 60) {
                    return false;
                }

            } catch (Exception ex) {
                logger.error("Failed to check dragon state", ex);
            }
        } else if (isCyclops(dragon)) {
            try {
                if (this.cyclopsCurrentAnimation == null) {
                    this.cyclopsCurrentAnimation = this.getFieldInHierarchy(dragon.getClass(), "currentAnimation");
                    this.cyclopsCurrentAnimation.setAccessible(true);
                }
                if (this.cyclopsAnimationTick == null) {
                    this.cyclopsAnimationTick = this.getFieldInHierarchy(dragon.getClass(), "animationTick");
                    this.cyclopsAnimationTick.setAccessible(true);
                }
                if (this.cyclops_ANIMATION_EATPLAYER == null) {
                    this.cyclops_ANIMATION_EATPLAYER = this.getFieldInHierarchy(dragon.getClass(), "ANIMATION_EATPLAYER");
                }

                Object animation = this.cyclopsCurrentAnimation.get(dragon);
                Object aniShakePray = this.cyclops_ANIMATION_EATPLAYER.get(dragon);
                int aniTick = (Integer) this.cyclopsAnimationTick.get(dragon);

                if (animation == aniShakePray && aniTick < 32) {
                    return false;
                }

            } catch (Exception ex) {
                logger.error("Failed to check cyclops state", ex);
            }
        }
        return true;
    }

    private void handleDismount(Entity mount, Entity rider) {
        if (mount.isDead || (mount instanceof EntityLivingBase && ((EntityLivingBase) mount).getHealth() <= 0.0f)) { // sanity check. Yes, players get stuck without this :P
            // don't apply any effects when the mount is dead
            return;
        }
        if (this.isDragon(mount)) {
            try {
                if (this.dragonCurrentAnimation == null) {
                    this.dragonCurrentAnimation = this.getFieldInHierarchy(mount.getClass(), "currentAnimation");
                    this.dragonCurrentAnimation.setAccessible(true);
                }
                if (this.dragon_ANIMATION_SHAKEPREY == null) {
                    this.dragon_ANIMATION_SHAKEPREY = this.getFieldInHierarchy(mount.getClass(), "ANIMATION_SHAKEPREY");
                }

                Object animation = this.dragonCurrentAnimation.get(mount);
                Object aniShakePray = this.dragon_ANIMATION_SHAKEPREY.get(mount);

                if (animation == aniShakePray && !mount.world.isRemote) {
                    float yaw = mount.getRotationYawHead();
                    //rider.motionY = rider.motionY + 0.1;
                    //rider.motionX = rider.motionX - 0.1 * MathHelper.cos(yaw);
                    //rider.motionZ = rider.motionZ - 0.1 * MathHelper.sin(yaw);
                    //rider.velocityChanged = true;
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error("Failed to check dragon state", ex);
            }
        }
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

    private boolean isDragon(Entity entity) {
        @Nullable
        ResourceLocation id = EntityList.getKey(entity);
        if (id == null) {
            return false;
        }

        return id.getResourceDomain().equals("iceandfire") && (
                id.getResourcePath().equals("icedragon") ||
                id.getResourcePath().equals("firedragon"));
    }

    private boolean isCyclops(Entity entity) {
        @Nullable
        ResourceLocation id = EntityList.getKey(entity);
        if (id == null) {
            return false;
        }

        return id.getResourceDomain().equals("iceandfire") && (
                id.getResourcePath().equals("cyclops"));
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
