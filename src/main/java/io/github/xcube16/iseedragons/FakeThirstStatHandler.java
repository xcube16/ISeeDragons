package io.github.xcube16.iseedragons;

import java.lang.reflect.Method;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class FakeThirstStatHandler
{
	Object handler;
	boolean initialized = false;
	Method onPlayerJump;
	Method onPlayerHurt;
	Method onBlockBreak;
	
	Method getThirstData;
	
	Class thirstHandlerClass;
	Method addExhaustion;
	
	public FakeThirstStatHandler(Object handler)
	{
		this.handler = handler;
		try
		{
			onPlayerJump = handler.getClass().getMethod("onPlayerJump", LivingJumpEvent.class);
			onPlayerHurt = handler.getClass().getMethod("onPlayerHurt", LivingHurtEvent.class);
			onBlockBreak = handler.getClass().getMethod("onBlockBreak", BlockEvent.BreakEvent.class);
			
			getThirstData = Class.forName("toughasnails.api.thirst.ThirstHelper").getMethod("getThirstData", EntityPlayer.class);
			
			thirstHandlerClass = Class.forName("toughasnails.thirst.ThirstHandler");
			addExhaustion = thirstHandlerClass.getMethod("addExhaustion", float.class);
			
			//No exceptions, report as successfully initialized
			initialized = true;
		}
		catch(Exception e)
		{
			ISeeDragons.logger.error("Failed to initialize FakeThirstStatHandler");
		}
	}
	
	@SubscribeEvent
	public void onPlayerJump(LivingJumpEvent event)
	{
		if(!initialized)
			return;
		try 
		{
			onPlayerJump.invoke(handler, event);
		} 
		catch (Exception e) 
		{
			failMethod(e);
		}
	}
	
	@SubscribeEvent
	public void onPlayerHurt(LivingHurtEvent event)
	{
		if(!initialized)
			return;
		try 
		{
			onPlayerHurt.invoke(handler, event);
		} 
		catch (Exception e) 
		{
			failMethod(e);
		}
	}
	
	@SubscribeEvent
	public void onBlockBreak(BlockEvent.BreakEvent event)
	{
		if(!initialized)
			return;
		try 
		{
			onBlockBreak.invoke(handler, event);
		} 
		catch (Exception e) 
		{
			failMethod(e);
		}
	}
	
	@SubscribeEvent
	public void onAttackEntity(AttackEntityEvent event)
	{
		if(!initialized)
			return;
		try 
		{
			World world = event.getEntity().world;
			if(world.isRemote)
				return;
			EntityPlayer player = event.getEntityPlayer();
			Entity monster = event.getTarget();
			if(monster.canBeAttackedWithItem() && !player.isCreative() && !monster.hitByEntity(player))
			{
				Object thirstHandler = thirstHandlerClass.cast(getThirstData.invoke(null,player));
				addExhaustion.invoke(thirstHandler, 0.3F);
			}
		}
		catch (Exception e)
		{
			failMethod(e);
		}
		
	}
	
	private void failMethod(Exception e)
	{
		initialized = false;
		ISeeDragons.logger.error("FakeThirstStatHandler failed reflection");
		e.printStackTrace();
	}
}
