package io.github.xcube16.iseedragons.asm.helper;

import io.github.xcube16.iseedragons.ISD;
import io.github.xcube16.iseedragons.asm.PatchResult;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import javax.annotation.Nullable;
import java.util.Optional;

public class PatchHelper {

    public static PatchResult muteMethod(MethodNode method) {
        boolean flag = false;
        AbstractInsnNode ip = method.instructions.getFirst();
        while (ip != null) {
            if (ip.getOpcode() == Opcodes.INVOKEINTERFACE) {
                MethodInsnNode call = (MethodInsnNode) ip;
                if (call.owner.equals("org/apache/logging/log4j/Logger") &&
                        call.name.equals("error") &&
                        call.desc.equals("(Ljava/lang/String;Ljava/lang/Throwable;)V")) {
                    method.instructions.insert(call, new InsnNode(Opcodes.POP2));
                    method.instructions.insert(call, new InsnNode(Opcodes.POP));
                    ip = call.getPrevious();
                    method.instructions.remove(call);
                    flag = true;
                }
            }

            ip = ip.getNext();
        }
        return flag ? PatchResult.NO_FLAGS : PatchResult.NO_MUTATION;
    }

    public static MethodNode findMethod(ClassNode node, String name) throws NoSuchMethodException {
        return findMethod(node, name, null);
    }

    public static MethodNode findMethod(ClassNode node, String name, @Nullable String desc) throws NoSuchMethodException {
        Optional<MethodNode> method = node.methods.stream()
                .filter(m -> m.name.equals(name))
                .filter(m -> desc == null || m.desc.equals(desc))
                .findFirst();
        if (!method.isPresent()) {
            ISD.logger.warn("Failed to find loadCustomAdvancements() method");
            throw new NoSuchMethodException(node.name + " " + name + " " + desc);
        }
        return method.get();
    }
}
