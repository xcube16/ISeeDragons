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

import io.github.xcube16.iseedragons.ISD;
import io.github.xcube16.iseedragons.asm.ISeeDragonsTransformer;
import io.github.xcube16.iseedragons.asm.Patch;
import io.github.xcube16.iseedragons.asm.PatchResult;
import io.github.xcube16.iseedragons.asm.Patcher;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Optional;

@Patcher(name = "Fix Dragon Item Lag", config = "DragonLag")
public class PatchDragonItemLag {

    @Patch(target = "com.github.alexthe666.iceandfire.entity.EntityDragonBase", desc = "hooks into EntityDragonBase#breakBlock to spawn less items")
    public static PatchResult fixEntityDragonBase(ISeeDragonsTransformer tweaker, ClassNode node) {
        // find breakBlock() method
        Optional<MethodNode> breakBlockMethod = node.methods.stream()
                .filter(method -> method.name.equals("breakBlock"))
                .findFirst();
        if (!breakBlockMethod.isPresent()) {
            ISD.logger.warn("Failed to find breakBlock() method");
            return PatchResult.NO_MUTATION;
        }

        // find World#destroyBlock() call
        AbstractInsnNode breakBlockCall = breakBlockMethod.get().instructions.getFirst();
        while (breakBlockCall != null &&
                (breakBlockCall.getOpcode() != Opcodes.INVOKEVIRTUAL ||
                        !((MethodInsnNode) breakBlockCall).name.equals("func_175655_b"))) {
            breakBlockCall = breakBlockCall.getNext();
        }
        if (breakBlockCall == null) {
            ISD.logger.error("Failed to find func_175655_b (World#destroyBlock()) call");
            return PatchResult.NO_MUTATION;
        }

        breakBlockMethod.get().instructions.remove(breakBlockCall.getPrevious()); // delete ICONST_1 (true)

        // call hook method in ISeeDragons to see if the block should drop
        InsnList callHook = new InsnList();

        // ...                                  this.world
        // *world already on stack*
        // ...                                              state
        callHook.add(new VarInsnNode(Opcodes.ALOAD, 5));
        // ... ISeeDragons.dragonBreakBlockHook(   ...    , ...  )
        callHook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "io/github/xcube16/iseedragons/ISeeDragons",
                "dragonBreakBlockHook",
                "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)Z"));
        breakBlockMethod.get().instructions.insertBefore(breakBlockCall, callHook);

        breakBlockMethod.get().instructions.remove(breakBlockCall);

        return PatchResult.NO_FLAGS;
    }
}
