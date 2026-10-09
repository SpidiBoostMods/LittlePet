package com.spidiboost.build;

import com.google.gson.Gson;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import java.nio.file.*;
import java.util.*;

public final class RemapPetProbes {
    public static void main(String[] args) throws Exception {
        Map<?, ?> data = new Gson().fromJson(Files.readString(Path.of(args[0])), Map.class);
        Map<?, ?> classes = (Map<?, ?>) data.get("classes");
        Map<?, ?> methods = (Map<?, ?>) data.get("methods");
        Map<?, ?> fields = (Map<?, ?>) data.get("fields");
        Remapper remapper = new Remapper() {
            private String mapped(Map<?, ?> map, String key, String fallback) {
                Object result = map.get(key);
                return result == null ? fallback : result.toString();
            }
            @Override public String map(String type) { return mapped(classes, type, type); }
            @Override public String mapMethodName(String owner, String name, String descriptor) {
                return mapped(methods, owner + "." + name + descriptor, name);
            }
            @Override public String mapFieldName(String owner, String name, String descriptor) {
                return mapped(fields, owner + "." + name + ":" + descriptor, name);
            }
        };
        Path input = Path.of(args[1]), output = Path.of(args[2]);
        try (var paths = Files.walk(input)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".class")).toList()) {
                if (path.getFileName().toString().equals("ProbeNames.class")) {
                    Files.createDirectories(output);
                    Files.copy(path, output.resolve("ProbeNames.class"), StandardCopyOption.REPLACE_EXISTING);
                    continue;
                }
                ClassWriter writer = new ClassWriter(0);
                ClassVisitor visitor = new ClassRemapper(writer, remapper) {
                    @Override public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                        MethodVisitor next = super.visitMethod(access, name, desc, signature, exceptions);
                        return new MethodVisitor(Opcodes.ASM9, next) {
                            @Override public void visitMethodInsn(int opcode, String owner, String method, String descriptor, boolean iface) {
                                if (owner.equals("java/lang/Class") && method.equals("getDeclaredField")
                                        && descriptor.equals("(Ljava/lang/String;)Ljava/lang/reflect/Field;")) {
                                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "ProbeNames", "field",
                                            "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/reflect/Field;", false);
                                } else super.visitMethodInsn(opcode, owner, method, descriptor, iface);
                            }
                        };
                    }
                };
                new ClassReader(Files.readAllBytes(path)).accept(visitor, 0);
                Path target = output.resolve(input.relativize(path));
                Files.createDirectories(target.getParent());
                Files.write(target, writer.toByteArray());
            }
        }
    }
}
