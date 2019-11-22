package io.github.xcube16.iseedragons;

import java.lang.reflect.InvocationTargetException;
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
	private Object handler;
	private Method onPlayerJump;
	private Method onPlayerHurt;
	private Method onBlockBreak;
	
	private Method getThirstData;
	
	private Class thirstHandlerClass;
	private Method addExhaustion;
	
	public FakeThirstStatHandler(Object handler) throws Exception
	{
		this.handler = handler;
		onPlayerJump = handler.getClass().getMethod("onPlayerJump", LivingJumpEvent.class);
		onPlayerHurt = handler.getClass().getMethod("onPlayerHurt", LivingHurtEvent.class);
		onBlockBreak = handler.getClass().getMethod("onBlockBreak", BlockEvent.BreakEvent.class);
		
		getThirstData = Class.forName("toughasnails.api.thirst.ThirstHelper").getMethod("getThirstData", EntityPlayer.class);
		
		thirstHandlerClass = Class.forName("toughasnails.thirst.ThirstHandler");
		addExhaustion = thirstHandlerClass.getMethod("addExhaustion", float.class);
	}
	
	@SubscribeEvent
	public void onPlayerJump(LivingJumpEvent event)
	{
		try 
		{
			onPlayerJump.invoke(handler, event);
		} 
		catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) 
		{
			failMethod(e);
		}
	}
	
	@SubscribeEvent
	public void onPlayerHurt(LivingHurtEvent event)
	{
		try 
		{
			onPlayerHurt.invoke(handler, event);
		} 
		catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) 
		{
			failMethod(e);
		}
	}
	
	@SubscribeEvent
	public void onBlockBreak(BlockEvent.BreakEvent event)
	{
		try 
		{
			onBlockBreak.invoke(handler, event);
		} 
		catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) 
		{
			failMethod(e);
		}
	}
	
	@SubscribeEvent
	public void onAttackEntity(AttackEntityEvent event)
	{
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
		catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) 
		{
			failMethod(e);
		}
		
	}
	
	private void failMethod(Exception e)
	{
		//Reflection failure, print out the stack trace and cause runtime exception
		throw new RuntimeException("FakeThirstStatHandler failed reflection", e);
	}
}
