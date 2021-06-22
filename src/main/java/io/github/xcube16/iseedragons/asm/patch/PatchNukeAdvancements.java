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
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Optional;

@Patcher(name = "Remove all achievements", config = "NukeAchievements", defaultEnable = false)
public class PatchNukeAdvancements {

    @Patch(target = "net.minecraft.advancements.AdvancementManager")
    public static PatchResult nukeAdvancementManager(ISeeDragonsTransformer tweaker, ClassNode node) {
        // find breakBlock() method
        Optional<MethodNode> reloadAdvMethod = node.methods.stream()
                .filter(method -> method.name.equals("func_192779_a")) // reload()
                .findFirst();
        if (!reloadAdvMethod.isPresent()) {
            // scrap: ISD.logger.warn("Failed to find func_192777_a() (loadBuiltInAdvancements) method");
            ISD.logger.warn("Failed to find func_192779_a() (reload) method");
            return PatchResult.NO_MUTATION;
        }

        // nuke everything for the lulz
        reloadAdvMethod.get().instructions.clear();
        reloadAdvMethod.get().localVariables.clear();
        reloadAdvMethod.get().tryCatchBlocks.clear();
        // return;
        reloadAdvMethod.get().instructions.add(new InsnNode(Opcodes.RETURN));

        return PatchResult.NO_FLAGS;
    }
}
