package io.github.xcube16.iseedragons.asm.patch;

import io.github.xcube16.iseedragons.ISD;
import io.github.xcube16.iseedragons.asm.ISeeDragonsTransformer;
import io.github.xcube16.iseedragons.asm.Patch;
import io.github.xcube16.iseedragons.asm.PatchResult;
import io.github.xcube16.iseedragons.asm.Patcher;
import io.github.xcube16.iseedragons.asm.helper.PatchHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

@Patcher(name = "Fix Dragon Escape", config = "DragonDismountFix")
public class PatchDragonDismount {

    /**
     * Adds a hook in EntityPlayer#updateRidden() to act as a cancelable pre-dismount event.
     */
    @Patch(target = "net.minecraft.entity.player.EntityPlayer", desc = "add hook to EntityPlayer#updateRidden()")
    public static PatchResult hookPlayerUpdateRidden(ISeeDragonsTransformer tweaker, ClassNode node) throws NoSuchMethodException {
        // EntityPlayer#updateRidden()
        MethodNode updateRidden = PatchHelper.findMethod(node, "func_70098_U");

        // find the first if block
        boolean foundFirst = false;
        AbstractInsnNode secondIfeqInsn = updateRidden.instructions.getFirst();
        while (secondIfeqInsn != null &&
                (secondIfeqInsn.getOpcode() != Opcodes.IFEQ ||
                        !foundFirst)) {
            if (secondIfeqInsn.getOpcode() == Opcodes.IFEQ) {
                foundFirst = true;
            }
            secondIfeqInsn = secondIfeqInsn.getNext();
        }
        if (secondIfeqInsn == null) {
            ISD.logger.error("Failed to find second ifeq instruction in EntityPlayer#updateRidden");
            return PatchResult.NO_MUTATION;
        }

        LabelNode elseBlock = ((JumpInsnNode) secondIfeqInsn).label;

        // call hook method in ISeeDragons to see if the player can dismount
        InsnList callHook = new InsnList();

        // Get the stack (the hook's arguments) ready
        callHook.add(new VarInsnNode(Opcodes.ALOAD, 0)); // stack: <this>
        // ISeeDragons.dragonBreakBlockHook( dump stack: <this> ) stack after call: <boolean>
        callHook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "io/github/xcube16/iseedragons/ISeeDragons",
                "canPlayerDismountHook",
                "(Lnet/minecraft/entity/player/EntityPlayer;)Z",
                false));
        // jump to else block if false
        callHook.add(new JumpInsnNode(Opcodes.IFEQ, elseBlock));

        // add the new instructions after the second ifeq
        updateRidden.instructions.insert(secondIfeqInsn, callHook);
        return PatchResult.NO_FLAGS;
    }


    /**
     * Adds a hook to EntityDragonBase#updatePreyInMouth() so we can do some
     * custom logic as well as fix players using dismount to escape.
     */
    @Patch(target = "com.github.alexthe666.iceandfire.entity.EntityDragonBase", desc = "add hook to EntityDragonBase#updatePreyInMouth()")
    public static PatchResult hookDragonShakePray(ISeeDragonsTransformer tweaker, ClassNode node) throws NoSuchMethodException {
        MethodNode updatePreyInMouth = PatchHelper.findMethod(node, "updatePreyInMouth");

        // find the first if block
        AbstractInsnNode ifNullInsn = updatePreyInMouth.instructions.getFirst();
        while (ifNullInsn != null &&
                ifNullInsn.getOpcode() != Opcodes.IFNULL) {
            ifNullInsn = ifNullInsn.getNext();
        }
        if (ifNullInsn == null) {
            ISD.logger.error("Failed to find ifnull instruction in EntityDragonBase#updatePreyInMouth()");
            return PatchResult.NO_MUTATION;
        }

        // remove all instructions inside the if statement (just past the label on the next instruction)
        //                         \/ ifnull  \/ label2  \/ JUNK!
        AbstractInsnNode toRemove = ifNullInsn.getNext().getNext();
        // loop until we hit the label referenced by the ifnull instruction or we hit the end of the method
        while (toRemove != null &&
                (toRemove.getType() != AbstractInsnNode.LABEL ||
                        toRemove != ((JumpInsnNode) ifNullInsn).label)) {
            AbstractInsnNode next = toRemove.getNext();
            updatePreyInMouth.instructions.remove(toRemove); // bye bye!
            toRemove = next;
        }
        if (toRemove == null) {
            ISD.logger.error("Failed to find end of if statement in EntityDragonBase#updatePreyInMouth()");
            return PatchResult.NO_MUTATION;
        }

        // call hook method in ISeeDragons to do the custom logic
        InsnList callHook = new InsnList();

        // Get the stack (the hook's arguments) ready
        callHook.add(new VarInsnNode(Opcodes.ALOAD, 0)); // stack: <this>
        callHook.add(new VarInsnNode(Opcodes.ALOAD, 1)); // stack: <this, pray>
        // this. ...
        callHook.add(new VarInsnNode(Opcodes.ALOAD, 0));
        // ... .getAnimationTick()
        callHook.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "com/github/alexthe666/iceandfire/entity/EntityDragonBase",
                "getAnimationTick",
                "()I", // stack: <this, pray, int>
                false));

        // ISeeDragons.dragonBreakBlockHook( dump stack: <this, pray, int> )
        callHook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "io/github/xcube16/iseedragons/ISeeDragons",
                "dragonShakePrayHook",
                "(Lnet/minecraft/entity/passive/EntityTameable;Lnet/minecraft/entity/Entity;I)V",
                false));

        // add the new instructions after label2
        updatePreyInMouth.instructions.insert(ifNullInsn.getNext(), callHook);
        return PatchResult.NO_FLAGS;
    }
}
