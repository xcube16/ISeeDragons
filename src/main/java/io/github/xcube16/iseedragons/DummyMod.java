package io.github.xcube16.iseedragons;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid=DummyMod.MODID, version = DummyMod.VERSION, acceptableRemoteVersions = "*", name = DummyMod.NAME)
public class DummyMod {
    public static final String MODID = "iseedragons";
    public static final String NAME = "ISeeDragons";
    public static final String VERSION = "0.1";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        System.out.println(MODID + " dummy mod loaded");
    }
}
