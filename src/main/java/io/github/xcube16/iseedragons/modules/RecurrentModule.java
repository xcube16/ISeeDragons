package io.github.xcube16.iseedragons.modules;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.github.xcube16.iseedragons.StaticConfig;
import ivorius.reccomplex.events.StructureGenerationEventLite;
import ivorius.reccomplex.world.gen.feature.WorldStructureGenerationData;
import ivorius.reccomplex.world.gen.feature.WorldStructureGenerationData.Entry;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

@ModEventModule(name="RecurrentModule", dependencies="reccomplex")
public class RecurrentModule 
{
	private long nanoSum = 0L;
	
	private int sensitivityMax = 50;
	private Map<Integer,Integer> dimensionSensitivity;
	
	private Field field_entryMap;
	
	@SuppressWarnings("deprecation")
	public RecurrentModule()
	{
		MinecraftForge.EVENT_BUS.register(this);
		dimensionSensitivity = new HashMap<Integer,Integer>();
		
		//TODO Replacement for ReflectionHelper? It's really good, though
		//Failure will throw UnableToAccessFieldException
		field_entryMap = ReflectionHelper.findField(WorldStructureGenerationData.class, "entryMap", null);
	}
	
	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onWorldLoad(WorldEvent.Load event)
	{
		World world = event.getWorld();
		if(world.isRemote) return;
		
		if(!StaticConfig.cleanupRecurrentComplex) return;
		
		long nanotime = System.nanoTime();
		
		//Wipe all recurrent data
		wipeRecurrentData(world);
		
		//Setup dimensionSensitivity for the world. This is important to do to avoid cross-save issues
		int dimension = world.provider.getDimension();
		dimensionSensitivity.put(dimension, sensitivityMax);
		
		nanoSum+=(System.nanoTime()-nanotime);
	}
	
	@SubscribeEvent(priority = EventPriority.LOW)
	public void onStructureGenerationLitePost(StructureGenerationEventLite.Post event)
	{
		World world = event.getWorld();
		if(world.isRemote) return;
		
		if(!StaticConfig.cleanupRecurrentComplex) return;
		
		long nanotime = System.nanoTime();

		//The key should have been made in WorldEvent.Load, but if it didn't somehow, this will do it.
		int dimension = world.provider.getDimension();
		if(!dimensionSensitivity.containsKey(dimension))
			dimensionSensitivity.put(dimension, sensitivityMax);
		
		//Get current sensitivity for dimension
		int sensitivity = dimensionSensitivity.get(dimension);
		
		//Decrement sensitivity and check if action should be skipped
		sensitivity-=1;
		if(sensitivity>0)
		{
			//Skip action
			dimensionSensitivity.put(dimension, sensitivity);
			nanoSum+=(System.nanoTime()-nanotime);
			return;
		}
		
		//Action is not skipped, reset dimension sensitivity
		dimensionSensitivity.put(dimension, sensitivityMax);
		
		//Clean all structures
		
		int amt = -1;
		
		try
		{
			amt = cleanAllStructures(world);
		}
		catch(IllegalArgumentException e)
		{
			throw new RuntimeException("RecurrentCleanup passed the wrong object type to entryMap");
		}
		catch(IllegalAccessException e)
		{
			throw new RuntimeException("RecurrentCleanup lost access to entryMap");
		}
		
		nanoSum+=(System.nanoTime()-nanotime);
	}
	
	private int cleanAllStructures(World world) throws IllegalArgumentException, IllegalAccessException
	{
		//Get all entry UUIDs
		WorldStructureGenerationData data = WorldStructureGenerationData.get(world);
		
		final Map<UUID, Entry> entryMap = (Map<UUID, Entry>)field_entryMap.get(data);
		
		final Set<UUID> entrySet = entryMap.keySet();
		
		//Avoid concurrency issues by cloning the set
		final Set<UUID> entrySetClone = new HashSet<UUID>();
		entrySetClone.addAll(entrySet);
		
		for(UUID uuid : entrySetClone)
		{
			data.removeEntry(uuid);
		}
		
		return entrySetClone.size();
	}
	
	private void wipeRecurrentData(World world)
	{
		WorldStructureGenerationData data = WorldStructureGenerationData.get(world);
		NBTTagCompound compound = data.serializeNBT();
		
		compound.removeTag("entries");
		compound.removeTag("checkedChunks");
		compound.removeTag("checkedChunksFinal");
		
		data.readFromNBT(compound);
		world.getPerWorldStorage().setData(data.mapName, data);
	}
	

	
	public long getProcessingTime()
	{
		//Logged processing time for debugging purposes
		return nanoSum;
	}
}
