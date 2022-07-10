package io.github.xcube16.iseedragons.asm.patch;

import io.github.xcube16.iseedragons.asm.ISeeDragonsTransformer;
import io.github.xcube16.iseedragons.asm.Patch;
import io.github.xcube16.iseedragons.asm.PatchResult;
import io.github.xcube16.iseedragons.asm.Patcher;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import static io.github.xcube16.iseedragons.asm.helper.PatchHelper.findMethod;

/**
 * Inserts a check if the entity is alive to fix the event handler firing for dead entities.
 */
@Patcher(name = "Patch Statue Dupe", config = "PatchStatueDupe")
public class PatchStatueDupe {

    @Patch(target = "com.github.alexthe666.iceandfire.event.EventLiving")
    public static PatchResult patchStatueDupe(ISeeDragonsTransformer tweaker, ClassNode node) throws NoSuchMethodException {
        MethodNode methodNode = findMethod(node, "onPlayerAttack");

        LabelNode label = new LabelNode();
        InsnList insnList = new InsnList();
        insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
        insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraftforge/event/entity/player/AttackEntityEvent", "getTarget", "()Lnet/minecraft/entity/Entity;", false));
        insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/entity/Entity", "func_70089_S", "()Z", false));
        insnList.add(new JumpInsnNode(Opcodes.IFNE, label));
        insnList.add(new InsnNode(Opcodes.RETURN));
        insnList.add(label);
        methodNode.instructions.insert(insnList);

        return PatchResult.FRAMES;
    }
}
