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

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FakeThirstStatHandler {
    private final Object handler;
    private final Method onPlayerJump;
    private final Method onPlayerHurt;
    private final Method onBlockBreak;

    private final Method getThirstData;

    private final Class thirstHandlerClass;
    private final Method addExhaustion;

    public FakeThirstStatHandler(Object handler) throws Exception {
        this.handler = handler;
        onPlayerJump = handler.getClass().getMethod("onPlayerJump", LivingJumpEvent.class);
        onPlayerHurt = handler.getClass().getMethod("onPlayerHurt", LivingHurtEvent.class);
        onBlockBreak = handler.getClass().getMethod("onBlockBreak", BlockEvent.BreakEvent.class);

        getThirstData = Class.forName("toughasnails.api.thirst.ThirstHelper").getMethod("getThirstData", EntityPlayer.class);

        thirstHandlerClass = Class.forName("toughasnails.thirst.ThirstHandler");
        addExhaustion = thirstHandlerClass.getMethod("addExhaustion", float.class);
    }

    @SubscribeEvent
    public void onPlayerJump(LivingJumpEvent event) {
        try {
            onPlayerJump.invoke(handler, event);
        } catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
            failMethod(e);
        }
    }

    @SubscribeEvent
    public void onPlayerHurt(LivingHurtEvent event) {
        try {
            onPlayerHurt.invoke(handler, event);
        } catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
            failMethod(e);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        try {
            onBlockBreak.invoke(handler, event);
        } catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
            failMethod(e);
        }
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        try {
            World world = event.getEntity().world;
            if (world.isRemote)
                return;
            EntityPlayer player = event.getEntityPlayer();
            Entity monster = event.getTarget();
            if (monster.canBeAttackedWithItem() && !player.isCreative() && !monster.hitByEntity(player)) {
                Object thirstHandler = thirstHandlerClass.cast(getThirstData.invoke(null, player));
                addExhaustion.invoke(thirstHandler, 0.3F);
            }
        } catch (InvocationTargetException | IllegalAccessException | IllegalArgumentException e) {
            failMethod(e);
        }

    }

    private void failMethod(Exception e) {
        //Reflection failure, print out the stack trace and cause runtime exception
        throw new RuntimeException("FakeThirstStatHandler failed reflection", e);
    }
}
