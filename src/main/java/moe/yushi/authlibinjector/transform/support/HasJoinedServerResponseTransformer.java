/*
 * Copyright (C) 2022  Ethan Zuo <yuxuan.zuo@outlook.com>
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
package moe.yushi.authlibinjector.transform.support;

import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.ARETURN;

import java.util.Optional;

import moe.yushi.authlibinjector.transform.TransformContext;
import moe.yushi.authlibinjector.transform.TransformUnit;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

public class HasJoinedServerResponseTransformer implements TransformUnit {

    private boolean isFieldPresent;
    private boolean isMethodPresent;

    @Override
    public Optional<ClassVisitor> transform(ClassLoader classLoader, String className, ClassVisitor writer, TransformContext context) {
        if ("com.mojang.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse".equals(className)) {
            return Optional.of(new ClassVisitor(ASM9, writer) {
                @Override
                public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                    if ("name".equals(name)) {
                        isFieldPresent = true;
                    }
                    return super.visitField(access, name, descriptor, signature, value);
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    if ("getName".equals(name)) {
                        isMethodPresent = true;
                    }
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }

                @Override
                public void visitEnd() {
                    if (!isFieldPresent & !isMethodPresent) {
                        context.markModified();

                        FieldVisitor fv = cv.visitField(ACC_PRIVATE, "name", "Ljava/lang/String;", null, null);
                        if (fv != null) {
                            fv.visitEnd();
                        }

                        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "getName", "()Ljava/lang/String;", null, null);
                        if (mv != null) {
                            mv.visitCode();
                            mv.visitVarInsn(ALOAD, 0);
                            mv.visitFieldInsn(GETFIELD, "com/mojang/authlib/yggdrasil/response/HasJoinedMinecraftServerResponse", "name", "Ljava/lang/String;");
                            mv.visitInsn(ARETURN);
                            mv.visitMaxs(1, 1);
                            mv.visitEnd();
                        }
                    }
                    cv.visitEnd();
                }
            });
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return "Has Joined Server Response Transformer";
    }
}
