/*
 * Copyright (C) 2023  Ethan Zuo <yuxuan.zuo@outlook.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package xyz.zuoyx.multiyggdrasil.transform.support;

import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.ALOAD;

import java.util.Optional;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import xyz.zuoyx.multiyggdrasil.transform.TransformContext;
import xyz.zuoyx.multiyggdrasil.transform.TransformUnit;

/**
 * By default, Minecraft uses the username sent by the client to create a game profile.
 * This transformer will make Minecraft use the username from the hasJoined response to create the game profile.
 */
public class HasJoinedServerTransformer implements TransformUnit {

	@Override
	public Optional<ClassVisitor> transform(ClassLoader classLoader, String className, ClassVisitor writer, TransformContext context) {
		if ("com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService".equals(className)) {
			return Optional.of(new ClassVisitor(ASM9, writer) {
				@Override
				public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
					if ("hasJoinedServer".equals(name) &&
							"(Ljava/lang/String;Ljava/lang/String;Ljava/net/InetAddress;)Lcom/mojang/authlib/yggdrasil/ProfileResult;".equals(descriptor)) {
						return new MethodVisitor(ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {

							// States:
							// 0 - initial state
							// 1 - invokevirtual com/mojang/authlib/yggdrasil/response/HasJoinedMinecraftServerResponse.id:()Ljava/util/UUID;
							// 2 - aload_1
							int state = 0;

							@Override
							public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
								if (state == 0 &&
										opcode == INVOKEVIRTUAL &&
										"com/mojang/authlib/yggdrasil/response/HasJoinedMinecraftServerResponse".equals(owner) &&
										"id".equals(name) &&
										"()Ljava/util/UUID;".equals(descriptor)) {
									state++;
								}
								super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
							}

							@Override
							public void visitVarInsn(int opcode, int var) {
								if (state == 1 && opcode == ALOAD && var == 1) {
									state++;
									context.markModified();
									super.visitVarInsn(opcode, 6);
									super.visitMethodInsn(INVOKEVIRTUAL,
											"com/mojang/authlib/yggdrasil/response/HasJoinedMinecraftServerResponse",
											"name",
											"()Ljava/lang/String;",
											false);
								} else {
									super.visitVarInsn(opcode, var);
								}
							}
						};
					} else {
						return super.visitMethod(access, name, descriptor, signature, exceptions);
					}
				}
			});
		} else {
			return Optional.empty();
		}
	}

	@Override
	public String toString() {
		return "Has Joined Server Transformer";
	}
}
