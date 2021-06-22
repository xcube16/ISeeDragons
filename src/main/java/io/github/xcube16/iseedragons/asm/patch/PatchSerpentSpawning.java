package io.github.xcube16.iseedragons.asm.patch;

import io.github.xcube16.iseedragons.ISD;
import io.github.xcube16.iseedragons.asm.ISeeDragonsTransformer;
import io.github.xcube16.iseedragons.asm.Patch;
import io.github.xcube16.iseedragons.asm.PatchResult;
import io.github.xcube16.iseedragons.asm.Patcher;
import io.github.xcube16.iseedragons.asm.helper.PatchHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodNode;

@Patcher(name = "Sea Serpent Spawn Fix", config = "FixSeaSerpentSpawn")
public class PatchSerpentSpawning {

    @Patch(target = "com.github.alexthe666.iceandfire.entity.EntitySeaSerpent", desc = "Fix an off-by-one bug that prevents the last sea serpent type from spawning")
    public static PatchResult fixSerpentOnWorldSpawn(ISeeDragonsTransformer tweaker, ClassNode node) throws NoSuchMethodException {
        // EntityPlayer#updateRidden()
        MethodNode updateRidden = PatchHelper.findMethod(node, "onWorldSpawn");

        // find that bipush 6 instruction!
        AbstractInsnNode bipushInsn = updateRidden.instructions.getFirst();
        while (bipushInsn != null &&
                (bipushInsn.getOpcode() != Opcodes.BIPUSH ||
                        ((IntInsnNode) bipushInsn).operand != 6)) {
            bipushInsn = bipushInsn.getNext();
        }
        if (bipushInsn == null) {
            ISD.logger.error("Failed to find second ifeq instruction in EntityPlayer#updateRidden");
            return PatchResult.NO_MUTATION;
        }

        // make that 6 a 7 ;)
        ((IntInsnNode) bipushInsn).operand = 7;
        return PatchResult.NO_FLAGS;
    }
}
