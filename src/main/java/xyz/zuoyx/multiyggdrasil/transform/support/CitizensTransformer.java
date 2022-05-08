/*
 * Copyright (C) 2021  Haowei Wen <yushijinhun@gmail.com> and contributors
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

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.F_SAME;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.RETURN;

import java.util.Optional;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import xyz.zuoyx.multiyggdrasil.transform.TransformContext;
import xyz.zuoyx.multiyggdrasil.transform.TransformUnit;

/**
 * Support for Citizens2
 *
 * In <https://github.com/CitizensDev/Citizens2/commit/28b0c4fdc3b343d4dc14f2a45cff37c0b75ced1d>,
 * the profile-url that Citizens use became configurable. This class is used to make Citizens ignore
 * the config property and use MultiYggdrasil's url.
 */
public class CitizensTransformer implements TransformUnit {

	@Override
	public Optional<ClassVisitor> transform(ClassLoader classLoader, String className, ClassVisitor writer, TransformContext ctx) {
		if ("net.citizensnpcs.Settings$Setting".equals(className)) {
			return Optional.of(new ClassVisitor(ASM9, writer) {
				@Override
				public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
					if (("loadFromKey".equals(name) || "setAtKey".equals(name))
							&& "(Lnet/citizensnpcs/api/util/DataKey;)V".equals(descriptor)) {
						return new MethodVisitor(ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
							@Override
							public void visitCode() {
								super.visitCode();
								super.visitLdcInsn("general.authlib.profile-url");
								super.visitVarInsn(ALOAD, 0);
								super.visitFieldInsn(GETFIELD, "net/citizensnpcs/Settings$Setting", "path", "Ljava/lang/String;");
								super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
								Label lbl = new Label();
								super.visitJumpInsn(IFEQ, lbl);
								super.visitInsn(RETURN);
								super.visitLabel(lbl);
								super.visitFrame(F_SAME, 0, null, 0, null);
								ctx.markModified();
							}
						};
					}
					return super.visitMethod(access, name, descriptor, signature, exceptions);
				}
			});
		}
		return Optional.empty();
	}

	@Override
	public String toString() {
		return "Citizens2 Support";
	}
}
