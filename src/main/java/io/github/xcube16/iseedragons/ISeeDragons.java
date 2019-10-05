package io.github.xcube16.iseedragons;

import com.google.common.collect.BiMap;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

@Mod(modid= ISeeDragons.MODID, version = ISeeDragons.VERSION, acceptableRemoteVersions = "*", name = ISeeDragons.NAME)
public class ISeeDragons {
    public static final String MODID = "iseedragons";
    public static final String NAME = "ISeeDragons";
    public static final String VERSION = "0.2";
    public static final Logger logger = LogManager.getLogger(NAME);

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        System.out.println(MODID + " dummy mod loaded");
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        try {
            Field regField = EntityRegistry.instance().getClass().getDeclaredField("entityClassRegistrations");
            regField.setAccessible(true);
            BiMap<Class<? extends Entity>, EntityRegistry.EntityRegistration> reg =
                    (BiMap<Class<? extends Entity>, EntityRegistry.EntityRegistration>)regField.get(EntityRegistry.instance());
            for (EntityRegistry.EntityRegistration entity : reg.values()) {
                logger.info(entity.getRegistryName().toString());
                if (entity.getRegistryName().toString().equals("iceandfire:icedragon") || entity.getRegistryName().toString().equals("iceandfire:firedragon")) {
                    logger.info("Fixed " + entity.getRegistryName() + " tracking distance");
                    Field rangeField = entity.getClass().getDeclaredField("trackingRange");
                    rangeField.setAccessible(true);
                    rangeField.set(entity, 256);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to fix Ice and Fire entity tracking distance");
            e.printStackTrace();
        }
    }
}
