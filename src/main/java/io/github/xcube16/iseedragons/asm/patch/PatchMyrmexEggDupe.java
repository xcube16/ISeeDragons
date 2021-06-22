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
package io.github.xcube16.iseedragons.asm.patch;

import io.github.xcube16.iseedragons.asm.ISeeDragonsTransformer;
import io.github.xcube16.iseedragons.asm.Patch;
import io.github.xcube16.iseedragons.asm.PatchResult;
import io.github.xcube16.iseedragons.asm.Patcher;
import io.github.xcube16.iseedragons.asm.helper.PatchHelper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

@Patcher(name = "Ice and Fire Myrmex egg dupe fix", config = "FixMyrmexDupeBug")
public class PatchMyrmexEggDupe {

    @Patch(target = "com.github.alexthe666.iceandfire.entity.EntityMyrmexEgg", desc = "adds a check to if the entity is already dead")
    public static PatchResult fixMyrmexEggDupe(ISeeDragonsTransformer tweaker, ClassNode node) throws NoSuchMethodException {
        MethodNode attackEntityFrom = PatchHelper.findMethod(node, "func_70097_a");

        InsnList deadCheck = new InsnList();
        LabelNode continueLabel = new LabelNode();
        // ... this.
        deadCheck.add(new VarInsnNode(Opcodes.ALOAD, 0));
        // ... ...  isDead
        deadCheck.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/entity/Entity", "field_70128_L", "Z"));
        // if ( ...       ) {
        deadCheck.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel)); // if isDead is false, skip the code in the block

        // return false;
        deadCheck.add(new InsnNode(Opcodes.ICONST_0));
        deadCheck.add(new InsnNode(Opcodes.IRETURN));
        // }
        deadCheck.add(new FrameNode(Opcodes.F_SAME, 3, new Object[]{"com/github/alexthe666/entity/EntityMyrmexEgg", "net/minecraft/util/DamageSource", Opcodes.FLOAT}, 0, new Object[]{}));
        deadCheck.add(continueLabel);

        // put the check at the start of the method
        attackEntityFrom.instructions.insert(deadCheck);

        return PatchResult.NO_FLAGS;
    }
}
