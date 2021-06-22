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

public class PatchScrapCode {


	/*
	"net.minecraft.advancements.AdvancementRewards$Deserializer"
	private boolean fixAdvancementRewards(ClassNode node) {
		// find deserialize() method
		Optional<MethodNode> deserializeMethod = node.methods.stream()
				.filter(method -> method.name.equals("deserialize"))
				.filter(method -> method.desc.equals("(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/advancements/AdvancementRewards;"))
				.findFirst();
		if (!deserializeMethod.isPresent()) {
			ISD.logger.warn("Failed to find deserialize() method");
			return false;
		}

		InsnList instructions = deserializeMethod.get().instructions;

		// find World#destroyBlock() call
		AbstractInsnNode newExcInsn = instructions.getFirst();
		while (newExcInsn != null &&
				(newExcInsn.getOpcode() != Opcodes.NEW ||
						!((TypeInsnNode) newExcInsn).desc.equals("com/google/gson/JsonSyntaxException"))) {
			newExcInsn = newExcInsn.getNext();
		}
		if (newExcInsn == null) {
			ISD.logger.error("Failed to find NEW JsonSyntaxException instruction");
			return false;
		}

		while (newExcInsn.getNext() != null && newExcInsn.getNext().getOpcode() != Opcodes.ATHROW) {
			instructions.remove(newExcInsn.getNext());
		}
		if (newExcInsn.getNext() == null) {
			ISD.logger.error("Found NEW JsonSyntaxException, but could not find ATHROW after it");
			return false;
		}
		instructions.remove(newExcInsn.getNext());

		// Find GOTO instruction at the end of the for loop
		AbstractInsnNode gotoInsn = newExcInsn;
		while(gotoInsn.getNext() != null && gotoInsn.getOpcode() != Opcodes.GOTO) {
			gotoInsn = gotoInsn.getNext();
		}
		if (gotoInsn.getNext() == null) {
			ISD.logger.error("Found NEW JsonSyntaxException, but could not find GOTO after it");
			return false;
		}

		// Remove NEW JsonSyntaxException
		instructions.remove(newExcInsn);

		InsnList hook = new InsnList();
		// ...                  ...                                      aresourcelocation1
		hook.add(new VarInsnNode(Opcodes.ALOAD, 9));
		//                      ISeeDragons.cleanAdvancementRequardsHook(        ...       )
		hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
				"io/github/xcube16/iseedragons/ISeeDragons",
				"cleanAdvancementRequardsHook",
				"([Lnet/minecraft/util/ResourceLocation;)[Lnet/minecraft/util/ResourceLocation;"));
		// aresourcelocation1 = ...                                              ...
		hook.add(new VarInsnNode(Opcodes.ASTORE, 9));

		// insert hook
		instructions.insert(gotoInsn.getNext().getNext().getNext(), hook);

		return true;
	}*/

    /*
     * WOW... I am so dumb! ... I was fixing code that never worked!
     */
	/*private boolean fixJawsEscape(ClassNode node) {
		// find onUpdate() method
		Optional<MethodNode> onUpdateMethod = node.methods.stream()
				.filter(method -> method.name.equals("func_70071_h_"))
				.findFirst();
		if (!onUpdateMethod.isPresent()) {
			ISD.logger.warn("Failed to find breakBlock() method");
			return false;
		}

		// find Entity#isRiding() call
		AbstractInsnNode isRidingCall = onUpdateMethod.get().instructions.getFirst();
		while (isRidingCall != null &&
				(isRidingCall.getOpcode() != Opcodes.INVOKEVIRTUAL ||
						!((MethodInsnNode) isRidingCall).name.equals("func_70093_af"))) {
			isRidingCall = isRidingCall.getNext();
		}
		if (isRidingCall == null) {
			ISD.logger.error("Failed to find func_70093_af (Entity#isRiding() call)");
			return false;
		}

		// call hook method in ISeeDragons to see if the block should drop
		InsnList aniCheck = new InsnList();

		// .. this............... .. ...................
		aniCheck.add(new VarInsnNode(Opcodes.ALOAD, 0));
		// .. .....getAnimation() .. ...................
		aniCheck.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
				"com/github/alexthe666/iceandfire/entity/EntityDragonBase",
				"getAnimation",
				"()Lnet/ilexiconn/llibrary/server/animation/Animation;"));
		// .. ................... .. ANIMATION_SHAKEPREY
		aniCheck.add(new FieldInsnNode(Opcodes.GETSTATIC,
				"com/github/alexthe666/iceandfire/entity/EntityDragonBase",
				"ANIMATION_SHAKEPREY",
				"Lnet/ilexiconn/llibrary/server/animation/Animation;"));
		// && ................... == ...................
		aniCheck.add(new JumpInsnNode(Opcodes.IFNE, ((JumpInsnNode) isRidingCall.getNext()).label));

		return true;
	}*/
}
