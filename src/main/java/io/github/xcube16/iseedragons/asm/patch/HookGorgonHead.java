package io.github.xcube16.iseedragons.asm.patch;

import io.github.xcube16.iseedragons.asm.ISeeDragonsTransformer;
import io.github.xcube16.iseedragons.asm.Patch;
import io.github.xcube16.iseedragons.asm.PatchResult;
import io.github.xcube16.iseedragons.asm.Patcher;
import io.github.xcube16.iseedragons.asm.helper.ASMHelper;
import io.github.xcube16.iseedragons.asm.helper.PatchHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

@Patcher(config = "HookGorgonHead")
public class HookGorgonHead {

    @Patch(target = "com.github.alexthe666.iceandfire.item.ItemGorgonHead$1",
            desc = "Hook ItemGorganHead$1 to stop some entities from getting stoned")
    public static PatchResult hookItemGorgon(ISeeDragonsTransformer tweaker, ClassNode node) throws NoSuchMethodException {
        MethodNode apply = PatchHelper.findMethod(node, "apply", "(Lnet/minecraft/entity/Entity;)Z");

        AbstractInsnNode first = ASMHelper.findFirstInstruction(apply);

        InsnList hook = new InsnList();
        LabelNode continueLabel = new LabelNode();

        hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
        // stack: entity
        // ... ISeeDragons.gorgonHeadHook( dump stack: <entity> ) ...
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "io/github/xcube16/iseedragons/ISeeDragons",
                "canStoneHook",
                "(Lnet/minecraft/entity/Entity;)Z",
                false));
        // stack: boolean
        // if ( ... ) {
        hook.add(new JumpInsnNode(Opcodes.IFGT, continueLabel)); // if iceAndFireGenerateHook() returned true, skip the code in the block
        // return false;
        hook.add(new InsnNode(Opcodes.ICONST_0));
        hook.add(new InsnNode(Opcodes.IRETURN));
        // }
        // frame node
        hook.add(continueLabel);

        apply.instructions.insertBefore(first, hook);

        return PatchResult.FRAMES;
    }

    @Patch(target = "com.github.alexthe666.iceandfire.entity.EntityGorgon$4",
            desc = "Hook EntityGorgon$4 to stop some entities from getting stoned")
    public static PatchResult hookEntityGorgon(ISeeDragonsTransformer tweaker, ClassNode node) throws NoSuchMethodException {
        // Its just another Predicate that we can deal with the exact same way
        return hookItemGorgon(tweaker, node);
    }
}
