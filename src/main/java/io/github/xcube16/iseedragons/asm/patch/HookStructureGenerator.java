package io.github.xcube16.iseedragons.asm.patch;

import io.github.xcube16.iseedragons.asm.ISeeDragonsTransformer;
import io.github.xcube16.iseedragons.asm.Patch;
import io.github.xcube16.iseedragons.asm.PatchResult;
import io.github.xcube16.iseedragons.asm.Patcher;
import io.github.xcube16.iseedragons.asm.helper.ASMHelper;
import io.github.xcube16.iseedragons.asm.helper.PatchHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

@Patcher(name = "Hook Structure Generator", config = "HookStructureGenerator")
public class HookStructureGenerator {

    @Patch(target = "com.github.alexthe666.iceandfire.event.StructureGenerator")
    public static PatchResult hookGenerate(ISeeDragonsTransformer tweaker, ClassNode node) throws NoSuchMethodException {
        MethodNode gen = PatchHelper.findMethod(node, "generate");
        AbstractInsnNode first = ASMHelper.findFirstInstruction(gen);

        InsnList hook = new InsnList();
        LabelNode continueLabel = new LabelNode();

        hook.add(new VarInsnNode(Opcodes.ALOAD, 4));
        // stack: world
        // ... ISeeDragons.dragonBreakBlockHook( dump stack: <world> ) ...
        hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "io/github/xcube16/iseedragons/ISeeDragons",
                "iceAndFireGenerateHook",
                "(Lnet/minecraft/world/World;)Z",
                false));
        // stack: boolean
        // if ( ... ) {
        hook.add(new JumpInsnNode(Opcodes.IFGT, continueLabel)); // if iceAndFireGenerateHook() returned true, skip the code in the block
        // return;
        hook.add(new InsnNode(Opcodes.RETURN));
        // }
        //hook.add(new FrameNode(Opcodes.F_SAME, 3, new Object[]{"com/github/alexthe666/entity/EntityMyrmexEgg", "net/minecraft/util/DamageSource", Opcodes.FLOAT}, 0, new Object[]{}));
        hook.add(continueLabel);

        gen.instructions.insertBefore(first, hook);

        return PatchResult.FRAMES;
    }
}
