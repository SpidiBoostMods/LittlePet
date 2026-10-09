package com.spidiboost.build;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.jar.*;

public final class PetObfuscator implements Opcodes {
    private static final String ROOT = System.getProperty("littlepet.root", "com/rwstafftool/patch/");
    private static final String VAULT = "com/spidiboost/protection/StringVault";
    private static final String BOOTSTRAP = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;JJ)Ljava/lang/invoke/CallSite;";
    private final Random random;
    private final Map<String, ClassNode> classes = new TreeMap<>();
    private final Map<String, String> classNames = new TreeMap<>();
    private final Map<String, String> methods = new TreeMap<>();
    private final Map<String, String> fields = new TreeMap<>();
    private final Set<String> names = new HashSet<>();
    private final Map<String, Integer> counts = new TreeMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, String> reverse = new HashMap<>();
    private final Remapper remapper = new Remapper() {
        @Override public String map(String type) { return classNames.getOrDefault(type, type); }
        @Override public String mapMethodName(String owner, String name, String descriptor) {
            return methods.getOrDefault(owner + "." + name + descriptor, name);
        }
        @Override public String mapFieldName(String owner, String name, String descriptor) {
            return fields.getOrDefault(owner + "." + name + ":" + descriptor, name);
        }
    };

    private PetObfuscator(long seed) { random = new Random(seed); }

    public static void main(String[] args) throws Exception {
        if (args.length != 6) throw new IllegalArgumentException("input output map runtime classpath seed");
        new PetObfuscator(Long.parseUnsignedLong(args[5], 16)).run(
                Path.of(args[0]), Path.of(args[1]), Path.of(args[2]), Path.of(args[3]), Path.of(args[4]));
    }

    private String name() {
        String result;
        do {
            StringBuilder builder = new StringBuilder("_");
            for (int i = 0; i < 20; i++) builder.append("IlO01".charAt(random.nextInt(5)));
            result = builder.toString();
        } while (!names.add(result));
        return result;
    }

    private void count(String key) { counts.merge(key, 1, Integer::sum); }

    private static boolean annotation(List<AnnotationNode> list, String type) {
        return list != null && list.stream().anyMatch(a -> a.desc.equals(type));
    }

    private static boolean shadow(FieldNode field) {
        return annotation(field.visibleAnnotations, "Lorg/spongepowered/asm/mixin/Shadow;")
                || annotation(field.invisibleAnnotations, "Lorg/spongepowered/asm/mixin/Shadow;");
    }

    private static boolean shadow(MethodNode method) {
        return annotation(method.visibleAnnotations, "Lorg/spongepowered/asm/mixin/Shadow;")
                || annotation(method.invisibleAnnotations, "Lorg/spongepowered/asm/mixin/Shadow;");
    }

    private void index() {
        for (ClassNode node : classes.values()) {
            String prefix = node.name.contains("/mixin/") ? "spidiboost/littlepet/m/" : "spidiboost/littlepet/p/";
            classNames.put(node.name, prefix + name());
            boolean record = (node.access & ACC_RECORD) != 0;
            Set<String> recordFields = new HashSet<>();
            if (node.recordComponents != null) node.recordComponents.forEach(r -> recordFields.add(r.name));
            boolean json = node.name.endsWith("DumplingFamily$Asset") || node.name.endsWith("DumplingFamily$Part");
            for (FieldNode field : node.fields) {
                if (shadow(field) || json || recordFields.contains(field.name) || annotation(field.visibleAnnotations,"Lcom/google/gson/annotations/SerializedName;")) continue;
                fields.put(node.name + "." + field.name + ":" + field.desc, name());
            }
            for (MethodNode method : node.methods) {
                if (method.name.startsWith("<") || shadow(method) || recordFields.contains(method.name) || (node.access & ACC_INTERFACE)!=0 || method.name.equals("onInitializeClient")) continue;
                if (Set.of("equals", "hashCode", "toString", "clone", "finalize").contains(method.name)) continue;
                boolean ownHierarchy = "java/lang/Object".equals(node.superName) || record || node.name.contains("/mixin/");
                if ((method.access & (ACC_PRIVATE | ACC_STATIC)) != 0) {
                    methods.put(node.name + "." + method.name + method.desc, name());
                }
            }
        }
        classNames.forEach((oldName, newName) -> reverse.put(newName, oldName));
    }

    private void run(Path input, Path output, Path mapping, Path runtime, Path classpath) throws Exception {
        Map<String, byte[]> entries = new TreeMap<>();
        Manifest manifest;
        try (JarFile jar = new JarFile(input.toFile())) {
            manifest = jar.getManifest();
            for (JarEntry entry : Collections.list(jar.entries())) {
                if (entry.isDirectory() || entry.getName().equals("META-INF/MANIFEST.MF")) continue;
                byte[] bytes = jar.getInputStream(entry).readAllBytes();
                if (entry.getName().endsWith(".class")) {
                    ClassNode node = new ClassNode(ASM9);
                    new ClassReader(bytes).accept(node, ClassReader.EXPAND_FRAMES);
                    if (node.name.startsWith("com/spidiboost/updates/")) { entries.put(entry.getName(), bytes); continue; }
                    if (!node.name.startsWith(ROOT)) throw new IllegalStateException("Unexpected class: " + node.name);
                    classes.put(node.name, node);
                } else if (ship(entry.getName())) entries.put(entry.getName(), bytes);
            }
        }
        ClassNode vault = new ClassNode(ASM9);
        new ClassReader(Files.readAllBytes(runtime)).accept(vault, ClassReader.EXPAND_FRAMES);
        classes.put(vault.name, vault);
        index();
        List<URL> urls = new ArrayList<>();
        urls.add(input.toUri().toURL());
        for (String entry : Files.readAllLines(classpath)) {
            if (!entry.isBlank()) urls.add(Path.of(entry).toUri().toURL());
        }
        try (URLClassLoader loader = new URLClassLoader(urls.toArray(URL[]::new), ClassLoader.getPlatformClassLoader())) {
            Hierarchy hierarchy = new Hierarchy(loader);
            for (ClassNode node : classes.values()) {
                strip(node);
                for (InnerClassNode inner : node.innerClasses) {
                    String mapped = classNames.get(inner.name);
                    if (mapped != null && inner.innerName != null) inner.innerName = mapped.substring(mapped.lastIndexOf('/') + 1);
                }
                if (!node.name.equals(VAULT)) protect(node);
                FieldNode watermark = new FieldNode(((node.access & ACC_INTERFACE)!=0?ACC_PUBLIC:ACC_PRIVATE) | ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC,
                        "$SpidiBoost", "Ljava/lang/String;", null, "SpidiBoost");
                if (node.name.contains("/mixin/")) {
                    watermark.visibleAnnotations = new ArrayList<>();
                    watermark.visibleAnnotations.add(new AnnotationNode("Lorg/spongepowered/asm/mixin/Unique;"));
                }
                node.fields.add(watermark);
                Collections.shuffle(node.fields, random);
                Collections.shuffle(node.methods, random);
                ClassNode renamed = new ClassNode(ASM9);
                node.accept(new ClassRemapper(renamed, remapper));
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
                    @Override protected String getCommonSuperClass(String first, String second) {
                        return hierarchy.common(first, second);
                    }
                };
                renamed.accept(writer);
                byte[] bytes = writer.toByteArray();
                ClassNode check = new ClassNode(ASM9);
                new ClassReader(bytes).accept(check, ClassReader.EXPAND_FRAMES);
                for (MethodNode method : check.methods) {
                    if ((method.access & (ACC_ABSTRACT | ACC_NATIVE)) == 0) {
                        new Analyzer<>(new BasicVerifier()).analyze(check.name, method);
                    }
                }
                entries.put(renamed.name + ".class", bytes);
            }
        }
        for(String resource:new ArrayList<>(entries.keySet())) {
            if(resource.endsWith(".mixins.json")) {
                JsonObject config=gson.fromJson(new String(entries.get(resource),StandardCharsets.UTF_8),JsonObject.class);
                String oldPackage=config.get("package").getAsString();
                config.addProperty("package","spidiboost.littlepet.m");
                for(String group:List.of("client","mixins","server"))if(config.has(group)) {
                    var renamedMixins=new com.google.gson.JsonArray();
                    for(JsonElement e:config.getAsJsonArray(group)) {
                        String next=classNames.get(oldPackage.replace('.','/')+"/"+e.getAsString());
                        if(next==null)throw new IllegalStateException("Missing mixin "+e);
                        renamedMixins.add(next.substring(next.lastIndexOf('/')+1));
                    }
                    config.add(group,renamedMixins);
                }
                entries.put(resource,gson.toJson(config).getBytes(StandardCharsets.UTF_8));
            } else if(resource.endsWith("refmap.json")) {
                String refmap=new String(entries.get(resource),StandardCharsets.UTF_8);
                for(var pair:classNames.entrySet())refmap=refmap.replace("\""+pair.getKey()+"\"","\""+pair.getValue()+"\"");
                entries.put(resource,refmap.getBytes(StandardCharsets.UTF_8));
            }
        }
        if(entries.containsKey("fabric.mod.json")) {
            String metadata=new String(entries.get("fabric.mod.json"),StandardCharsets.UTF_8);
            for(var pair:classNames.entrySet())metadata=metadata.replace("\""+pair.getKey().replace('/','.')+"\"","\""+pair.getValue().replace('/','.')+"\"");
            entries.put("fabric.mod.json",metadata.getBytes(StandardCharsets.UTF_8));
        }
        if (manifest == null) manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Implementation-Title", "LittlePet by SpidiBoost");
        manifest.getMainAttributes().putValue("Implementation-Vendor", "SpidiBoost");
        manifest.getMainAttributes().putValue("Built-By", "SpidiBoost");
        manifest.getMainAttributes().putValue("LittlePet-Author", "SpidiBoost");
        Files.createDirectories(output.toAbsolutePath().getParent());
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(output))) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            manifest.write(bytes);
            write(jar, "META-INF/MANIFEST.MF", bytes.toByteArray());
            List<String> order = new ArrayList<>(entries.keySet());
            Collections.shuffle(order, random);
            for (String key : order) write(jar, key, entries.get(key));
        }
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("author", "SpidiBoost");
        report.put("classes", classNames);
        report.put("methods", methods);
        report.put("fields", fields);
        report.put("transforms", counts);
        report.put("sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(output))));
        Files.createDirectories(mapping.toAbsolutePath().getParent());
        Files.writeString(mapping, gson.toJson(report));
        System.out.println("SpidiBoost protection: " + classNames.size() + " classes, " + methods.size()
                + " methods, " + fields.size() + " fields; " + counts);
        System.out.println("Created " + output.toAbsolutePath());
    }

    private static void write(JarOutputStream jar, String name, byte[] bytes) throws IOException {
        JarEntry entry = new JarEntry(name);
        entry.setTime(315532800000L);
        jar.putNextEntry(entry);
        jar.write(bytes);
        jar.closeEntry();
    }

    private static boolean ship(String name) {
        return !name.startsWith("sources/") && !name.startsWith("upstream/") && !name.startsWith("tools/")
                && !name.endsWith(".java") && !name.endsWith(".py") && !name.endsWith(".bbmodel")
                && !name.endsWith(".glb") && !name.endsWith("-readme.md") && !name.equals(".DS_Store");
    }

    private static void strip(ClassNode node) {
        node.sourceFile = null;
        node.sourceDebug = null;
        node.signature = null;
        for (FieldNode field : node.fields) field.signature = null;
        for (MethodNode method : node.methods) {
            method.signature = null;
            method.parameters = null;
            if (method.localVariables != null) method.localVariables.clear();
            method.visibleLocalVariableAnnotations = null;
            method.invisibleLocalVariableAnnotations = null;
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (insn instanceof LineNumberNode || insn instanceof FrameNode) method.instructions.remove(insn);
            }
        }
    }

    private void protect(ClassNode node) {
        MethodNode initializer = null;
        for (FieldNode field : node.fields) {
            if (field.value != null && (field.access & ACC_STATIC) != 0) {
                if (initializer == null) {
                    initializer = node.methods.stream().filter(m -> m.name.equals("<clinit>")).findFirst().orElse(null);
                    if (initializer == null) {
                        initializer = new MethodNode(ACC_STATIC, "<clinit>", "()V", null, null);
                        initializer.instructions.add(new InsnNode(RETURN));
                        node.methods.add(initializer);
                    }
                }
                InsnList assignment = new InsnList();
                assignment.add(new LdcInsnNode(field.value));
                assignment.add(new FieldInsnNode(PUTSTATIC, node.name, field.name, field.desc));
                initializer.instructions.insert(assignment);
                field.value = null;
            }
        }
        for (MethodNode method : node.methods) {
            if ((method.access & (ACC_ABSTRACT | ACC_NATIVE)) != 0) continue;
            AbstractInsnNode[] originals = method.instructions.toArray();
            for (AbstractInsnNode insn : originals) {
                if (insn instanceof LdcInsnNode ldc) {
                    if (ldc.cst instanceof String value) replace(method, insn, string(value));
                    else if (ldc.cst instanceof Integer value) replace(method, insn, integer(value));
                    else if (ldc.cst instanceof Float value) {
                        InsnList list = integer(Float.floatToRawIntBits(value));
                        list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Float", "intBitsToFloat", "(I)F", false));
                        replace(method, insn, list);
                    } else if (ldc.cst instanceof Long value) replace(method, insn, longNumber(value));
                    else if (ldc.cst instanceof Double value) {
                        InsnList list = longNumber(Double.doubleToRawLongBits(value));
                        list.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Double", "longBitsToDouble", "(J)D", false));
                        replace(method, insn, list);
                    }
                } else if (insn instanceof IntInsnNode push && (push.getOpcode() == BIPUSH || push.getOpcode() == SIPUSH)) {
                    replace(method, insn, integer(push.operand));
                } else if (insn instanceof InvokeDynamicInsnNode indy && indy.bsm.getOwner().equals("java/lang/invoke/StringConcatFactory")) {
                    protectConcat(node, method, indy);
                }
            }
            if (method.name.startsWith("<")) continue;
            int transformed = 0;
            for (AbstractInsnNode insn : originals) {
                if (!(insn instanceof JumpInsnNode branch) || transformed >= 6) continue;
                if (branch.getOpcode() == GOTO || branch.getOpcode() == JSR) continue;
                LabelNode taken = new LabelNode();
                LabelNode fall = new LabelNode();
                InsnList replacement = new InsnList();
                replacement.add(new JumpInsnNode(branch.getOpcode(), taken));
                replacement.add(dispatch(fall));
                replacement.add(taken);
                replacement.add(dispatch(branch.label));
                replacement.add(fall);
                replace(method, branch, replacement);
                transformed++;
                count("routedBranches");
            }
            LabelNode start = new LabelNode();
            InsnList gate = dispatch(start);
            gate.add(start);
            method.instructions.insert(gate);
            count("entryDispatchers");
        }
    }

    private void protectConcat(ClassNode node, MethodNode method, InvokeDynamicInsnNode indy) {
        if (!indy.bsm.getName().equals("makeConcatWithConstants") || indy.bsmArgs.length == 0
                || !(indy.bsmArgs[0] instanceof String recipe)) return;
        for (int i = 1; i < indy.bsmArgs.length; i++) if (!(indy.bsmArgs[i] instanceof String)) return;
        Type[] args = Type.getArgumentTypes(indy.desc);
        int local = method.maxLocals;
        int[] locals = new int[args.length];
        for (int i = 0; i < args.length; i++) { locals[i] = local; local += args[i].getSize(); }
        method.maxLocals = local;
        InsnList list = new InsnList();
        for (int i = args.length - 1; i >= 0; i--) list.add(new VarInsnNode(args[i].getOpcode(ISTORE), locals[i]));
        list.add(new TypeInsnNode(NEW, "java/lang/StringBuilder"));
        list.add(new InsnNode(DUP));
        list.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
        int argument = 0, constant = 1;
        StringBuilder literal = new StringBuilder();
        for (int i = 0; i <= recipe.length(); i++) {
            char ch = i == recipe.length() ? 0 : recipe.charAt(i);
            if (i == recipe.length() || ch == 1 || ch == 2) {
                if (!literal.isEmpty()) {
                    list.add(string(literal.toString()));
                    append(list, "Ljava/lang/String;");
                    literal.setLength(0);
                }
                if (ch == 1) {
                    Type type = args[argument];
                    list.add(new VarInsnNode(type.getOpcode(ILOAD), locals[argument++]));
                    String descriptor = switch (type.getSort()) {
                        case Type.BOOLEAN -> "Z";
                        case Type.CHAR -> "C";
                        case Type.BYTE, Type.SHORT, Type.INT -> "I";
                        case Type.FLOAT -> "F";
                        case Type.LONG -> "J";
                        case Type.DOUBLE -> "D";
                        default -> "Ljava/lang/Object;";
                    };
                    append(list, descriptor);
                } else if (ch == 2) {
                    list.add(string((String) indy.bsmArgs[constant++]));
                    append(list, "Ljava/lang/String;");
                }
            } else literal.append(ch);
        }
        if (argument != args.length) throw new IllegalStateException("Concat arity in " + node.name);
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false));
        replace(method, indy, list);
        count("encryptedConcatRecipes");
    }

    private static void append(InsnList list, String descriptor) {
        list.add(new MethodInsnNode(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
                "(" + descriptor + ")Ljava/lang/StringBuilder;", false));
    }

    private static void replace(MethodNode method, AbstractInsnNode old, InsnList replacement) {
        method.instructions.insertBefore(old, replacement);
        method.instructions.remove(old);
    }

    private InsnList string(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        long first = random.nextLong(), second = random.nextLong();
        long state = first ^ Long.rotateLeft(second, 23);
        for (int i = 0; i < bytes.length; i++) {
            state += 0x9E3779B97F4A7C15L;
            long mixed = state;
            mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
            mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
            bytes[i] ^= (byte) (mixed ^ (mixed >>> 31));
        }
        InsnList list = new InsnList();
        list.add(new InvokeDynamicInsnNode(name(), "()Ljava/lang/String;",
                new Handle(H_INVOKESTATIC, VAULT, "open", BOOTSTRAP, false),
                Base64.getEncoder().encodeToString(bytes), first, second));
        count("encryptedStrings");
        return list;
    }

    private InsnList integer(int value) {
        int mask = random.nextInt();
        InsnList list = new InsnList();
        list.add(new LdcInsnNode(value ^ mask));
        list.add(new LdcInsnNode(mask));
        list.add(new InsnNode(IXOR));
        count("maskedNumbers");
        return list;
    }

    private InsnList longNumber(long value) {
        long mask = random.nextLong();
        InsnList list = new InsnList();
        list.add(new LdcInsnNode(value ^ mask));
        list.add(new LdcInsnNode(mask));
        list.add(new InsnNode(LXOR));
        count("maskedNumbers");
        return list;
    }

    private InsnList dispatch(LabelNode destination) {
        LabelNode decoy = new LabelNode();
        int value = random.nextInt();
        InsnList list = integer(value);
        list.add(new LookupSwitchInsnNode(decoy, new int[]{value}, new LabelNode[]{destination}));
        list.add(decoy);
        list.add(new LdcInsnNode(random.nextInt()));
        list.add(new InsnNode(POP));
        list.add(new JumpInsnNode(GOTO, destination));
        return list;
    }

    private record Header(String parent, List<String> interfaces, boolean iface) { }

    private final class Hierarchy {
        private final Map<String, Header> cache = new HashMap<>();
        private final ClassLoader loader;
        private Hierarchy(ClassLoader loader) { this.loader = loader; }

        private Header header(String name) {
            return cache.computeIfAbsent(name, key -> {
                String original = reverse.getOrDefault(key, key);
                ClassNode node = classes.get(original);
                if (node != null) return new Header(remapper.mapType(node.superName),
                        node.interfaces.stream().map(remapper::mapType).toList(), (node.access & ACC_INTERFACE) != 0);
                try (InputStream input = loader.getResourceAsStream(original + ".class")) {
                    if (input == null) throw new IllegalStateException("Missing hierarchy class: " + original);
                    ClassReader reader = new ClassReader(input);
                    return new Header(reader.getSuperName(), Arrays.asList(reader.getInterfaces()), (reader.getAccess() & ACC_INTERFACE) != 0);
                } catch (IOException error) { throw new UncheckedIOException(error); }
            });
        }

        private boolean assignable(String target, String source) {
            if (target.equals(source) || target.equals("java/lang/Object")) return true;
            if (source.startsWith("[")) {
                if (target.equals("java/lang/Cloneable") || target.equals("java/io/Serializable")) return true;
                if (!target.startsWith("[")) return false;
                String left = element(target), right = element(source);
                return reference(left) && reference(right) && assignable(left, right);
            }
            if (target.startsWith("[")) return false;
            Header info = header(source);
            if (info.parent != null && assignable(target, info.parent)) return true;
            for (String iface : info.interfaces) if (assignable(target, iface)) return true;
            return false;
        }

        private String common(String first, String second) {
            if (assignable(first, second)) return first;
            if (assignable(second, first)) return second;
            if (first.startsWith("[") && second.startsWith("[")) {
                String a = element(first), b = element(second);
                if (reference(a) && reference(b)) {
                    String common = common(a, b);
                    return "[" + (common.startsWith("[") ? common : "L" + common + ";");
                }
            }
            if (first.startsWith("[") || second.startsWith("[") || header(first).iface || header(second).iface) return "java/lang/Object";
            String parent = header(first).parent;
            while (parent != null && !assignable(parent, second)) parent = header(parent).parent;
            return parent == null ? "java/lang/Object" : parent;
        }

        private static String element(String array) {
            String result = array.substring(1);
            return result.startsWith("L") ? result.substring(1, result.length() - 1) : result;
        }

        private static boolean reference(String name) { return name.length() > 1; }
    }
}
