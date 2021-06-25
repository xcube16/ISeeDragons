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
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

@Patcher(name = "Patch MmmMmm dummy", config = "PatchMmmMmm")
public class PatchMmmMmm {

    @Patch(target = "boni.dummy.client.RenderFloatingNumber")
    public static PatchResult damageNotHearts(ISeeDragonsTransformer tweaker, ClassNode node) throws NoSuchMethodException {
        return patchFCONST2(PatchHelper.findMethod(node, "doRender"));
    }

    @Patch(target = "boni.dummy.network.DamageMessage$MessageHandlerClient$1")
    public static PatchResult damageNotHeartsNetwork(ISeeDragonsTransformer tweaker, ClassNode node) throws NoSuchMethodException {
        return patchFCONST2(PatchHelper.findMethod(node, "run"));
    }

    private static PatchResult patchFCONST2(MethodNode node) {
        // find that fconst_2 instruction!
        AbstractInsnNode fconst = node.instructions.getFirst();
        while (fconst != null &&
                (fconst.getOpcode() != Opcodes.FCONST_2)) {
            fconst = fconst.getNext();
        }
        if (fconst == null) {
            ISD.logger.error("Failed to find FCONST_2 instruction");
            return PatchResult.NO_MUTATION;
        }

        // swap it out
        node.instructions.insert(fconst, new InsnNode(Opcodes.FCONST_1));
        node.instructions.remove(fconst);

        return PatchResult.NO_FLAGS;
    }
}