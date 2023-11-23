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

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.PUTFIELD;

import java.util.Optional;

import xyz.zuoyx.multiyggdrasil.transform.TransformContext;
import xyz.zuoyx.multiyggdrasil.transform.TransformUnit;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.RecordComponentVisitor;

public class HasJoinedServerResponseTransformer implements TransformUnit {

    private boolean isNameFieldPresent;
    private boolean isNameMethodPresent;

    @Override
    public Optional<ClassVisitor> transform(ClassLoader classLoader, String className, ClassVisitor writer, TransformContext context) {
        if ("com.mojang.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse".equals(className)) {
            return Optional.of(new ClassVisitor(ASM9, writer) {
                @Override
                public RecordComponentVisitor visitRecordComponent(String name, String descriptor, String signature) {
                    if ("properties".equals(name)) {
                        context.markModified();
                        RecordComponentVisitor rcv = super.visitRecordComponent("name", "Ljava/lang/String;", null);
                        if (rcv != null) {
                            rcv.visitAnnotation("Ljavax/annotation/Nullable;", true);
                            rcv.visitEnd();
                        }
                    }
                    return super.visitRecordComponent(name, descriptor, signature);
                }

                @Override
                public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                    if ("name".equals(name)) {
                        isNameFieldPresent = true;
                    }
                    return super.visitField(access, name, descriptor, signature, value);
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    if ("name".equals(name)) {
                        isNameMethodPresent = true;
                    }

                    if (access == ACC_PUBLIC &&
                            "<init>".equals(name) &&
                            "(Ljava/util/UUID;Lcom/mojang/authlib/properties/PropertyMap;Ljava/util/Set;)V".equals(descriptor) &&
                            "(Ljava/util/UUID;Lcom/mojang/authlib/properties/PropertyMap;Ljava/util/Set<Lcom/mojang/authlib/yggdrasil/response/ProfileAction;>;)V".equals(signature)) {
                        return new MethodVisitor(ASM9, super.visitMethod(access, name,
                                "(Ljava/util/UUID;Ljava/lang/String;Lcom/mojang/authlib/properties/PropertyMap;Ljava/util/Set;)V",
                                "(Ljava/util/UUID;Ljava/lang/String;Lcom/mojang/authlib/properties/PropertyMap;Ljava/util/Set<Lcom/mojang/authlib/yggdrasil/response/ProfileAction;>;)V", exceptions)) {

                            // 0 - initial state
                            //     aload_0
                            // 1 - aload_1
                            // 2 - putfield id:Ljava/util/UUID;
                            //     aload_0
                            // 3 - aload_2
                            //     putfield properties:Lcom/mojang/authlib/properties/PropertyMap;
                            //     aload_0
                            // 4 - aload_3
                            //     putfield profileActions:Ljava/util/Set;
                            int state = 0;

                            @Override
                            public void visitVarInsn(int opcode, int varIndex) {
                                if (state == 0 && opcode == ALOAD && varIndex == 1) {
                                    state++;
                                    super.visitVarInsn(opcode, varIndex);
                                } else if (state == 2 && opcode == ALOAD && varIndex == 2) {
                                    state++;
                                    context.markModified();
                                    super.visitVarInsn(opcode, 3);
                                } else if (state == 3 && opcode == ALOAD && varIndex == 3) {
                                    state++;
                                    context.markModified();
                                    super.visitVarInsn(opcode, 4);
                                } else {
                                    super.visitVarInsn(opcode, varIndex);
                                }
                            }

                            @Override
                            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                                if (state == 1 && opcode == PUTFIELD && "id".equals(name) && "Ljava/util/UUID;".equals(descriptor)) {
                                    state++;
                                    context.markModified();
                                    super.visitFieldInsn(opcode, owner, name, descriptor);
                                    super.visitVarInsn(ALOAD, 0);
                                    super.visitVarInsn(ALOAD, 2);
                                    super.visitFieldInsn(opcode, owner, "name", "Ljava/lang/String;");
                                } else {
                                    super.visitFieldInsn(opcode, owner, name, descriptor);
                                }
                            }
                        };
                    }

                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }

                @Override
                public void visitEnd() {
                    if (!isNameFieldPresent) {
                        context.markModified();
                        FieldVisitor fv = super.visitField(ACC_PRIVATE, "name", "Ljava/lang/String;", null, null);
                        if (fv != null) {
                            AnnotationVisitor av = fv.visitAnnotation("Lcom/google/gson/annotations/SerializedName;", true);
                            if (av != null) {
                                av.visit("value", "name");
                                av.visitEnd();
                            }
                            fv.visitAnnotation("Ljavax/annotation/Nullable;", true);
                            fv.visitEnd();
                        }
                    }

                    if (!isNameMethodPresent) {
                        context.markModified();
                        MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "name", "()Ljava/lang/String;", null, null);
                        if (mv != null) {
                            AnnotationVisitor av = mv.visitAnnotation("Lcom/google/gson/annotations/SerializedName;", true);
                            if (av != null) {
                                av.visit("value", "name");
                                av.visitEnd();
                            }
                            mv.visitAnnotation("Ljavax/annotation/Nullable;", true);
                            mv.visitCode();
                            mv.visitVarInsn(ALOAD, 0);
                            mv.visitFieldInsn(GETFIELD, "com/mojang/authlib/yggdrasil/response/HasJoinedMinecraftServerResponse", "name", "Ljava/lang/String;");
                            mv.visitInsn(ARETURN);
                            mv.visitMaxs(1, 1);
                            mv.visitEnd();
                        }
                    }

                    super.visitEnd();
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
