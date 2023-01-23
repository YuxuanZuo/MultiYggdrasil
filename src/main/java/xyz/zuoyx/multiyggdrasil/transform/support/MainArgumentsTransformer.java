/*
 * Copyright (C) 2022  Haowei Wen <yushijinhun@gmail.com> and contributors
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

import static xyz.zuoyx.multiyggdrasil.util.Logging.log;
import static xyz.zuoyx.multiyggdrasil.util.Logging.Level.DEBUG;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.ASTORE;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import xyz.zuoyx.multiyggdrasil.transform.CallbackMethod;
import xyz.zuoyx.multiyggdrasil.transform.TransformContext;
import xyz.zuoyx.multiyggdrasil.transform.TransformUnit;

public class MainArgumentsTransformer implements TransformUnit {

	@Override
	public Optional<ClassVisitor> transform(ClassLoader classLoader, String className, ClassVisitor writer, TransformContext ctx) {
		if ("net.minecraft.client.main.Main".equals(className)) {
			return Optional.of(new ClassVisitor(ASM9, writer) {
				@Override
				public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
					if ("main".equals(name) && "([Ljava/lang/String;)V".equals(descriptor)) {
						return new MethodVisitor(ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
							@Override
							public void visitCode() {
								super.visitCode();
								ctx.markModified();

								super.visitVarInsn(ALOAD, 0);
								ctx.invokeCallback(mv, MainArgumentsTransformer.class, "processMainArguments");
								super.visitVarInsn(ASTORE, 0);
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
		return "Main Arguments Transformer";
	}

	// ==== Main arguments processing ====
	private static final List<Function<String[], String[]>> ARGUMENTS_LISTENERS = new CopyOnWriteArrayList<>();

	@CallbackMethod
	public static String[] processMainArguments(String[] args) {
		log(DEBUG, "Original main arguments: " + String.join(" ", args));

		String[] result = args;
		for (Function<String[], String[]> listener : ARGUMENTS_LISTENERS) {
			result = listener.apply(result);
		}
		log(DEBUG, "Transformed main arguments: " + String.join(" ", result));
		return result;
	}

	public static List<Function<String[], String[]>> getArgumentsListeners() {
		return ARGUMENTS_LISTENERS;
	}
	// ====

	// ==== Version series detection ====
	private static final List<Consumer<String>> VERSION_SERIES_LISTENERS = new CopyOnWriteArrayList<>();

	public static Optional<String> inferVersionSeries(String[] args) {
		boolean hit = false;
		for (String arg : args) {
			if (hit) {
				if (arg.startsWith("--")) {
					// arg doesn't seem to be a value
					// maybe the previous argument is a value, but we wrongly recognized it as an option
					hit = false;
				} else {
					return Optional.of(arg);
				}
			}

			if ("--assetIndex".equals(arg)) {
				hit = true;
			}
		}
		return Optional.empty();
	}

	static {
		getArgumentsListeners().add(args -> {
			inferVersionSeries(args).ifPresent(versionSeries -> {
				log(DEBUG, "Version series detected: " + versionSeries);
				VERSION_SERIES_LISTENERS.forEach(listener -> listener.accept(versionSeries));
			});
			return args;
		});
	}

	public static List<Consumer<String>> getVersionSeriesListeners() {
		return VERSION_SERIES_LISTENERS;
	}
	// ====
}
