package io.github.xcube16.iseedragons;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.FMLLog;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class AsmTransformer implements IClassTransformer {
	private static Logger logger = FMLLog.getLogger();

	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) {
		//km note: There will be a suffix that may vary ($3 as of this writing), so use startsWith
		if(transformedName.equals("net.minecraft.entity.Entity")) {
			ClassReader reader = new ClassReader(bytes);
			ClassNode node = new ClassNode();
			reader.accept(node, 0);
			if(analyzeClass(node)) {
				ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
				node.accept(writer);
				bytes = writer.toByteArray();
				logger.info("[" + DummyMod.NAME + "] Patched render distance!");
			} else {
                logger.error("[" + DummyMod.NAME + "] Failed to patch methods");
			}
		}

		return bytes;
	}

	private boolean analyzeClass(ClassNode node) {
		boolean success = false;

		for(MethodNode method : node.methods) {
			if(method.name.equals("isInRangeToRenderDist") || (method.name.equals("func_70112_a") && method.desc.equals("(D)Z"))) {
				//logger.info("mtqfix found method processPlayer");
				success = fixRenderDistance(method);
			}
		}

		return success;
	}


	private boolean fixRenderDistance(MethodNode node) {
		AbstractInsnNode insn = node.instructions.getFirst();
		while(insn != null) {
			if (insn.getOpcode() == Opcodes.LDC && ((LdcInsnNode) insn).cst.equals(64.0)) {
				//Found it!
				((LdcInsnNode) insn).cst = 128.0;
				return true;
			}
			insn = insn.getNext();
		}
		return false;
	}
}

