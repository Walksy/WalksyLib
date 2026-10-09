package main.walksy.lib.core.render.gpu;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class VulkanShaderSource {
    private static final Pattern VERSION = Pattern.compile("^#version[^\\n]*\\n", Pattern.MULTILINE);
    private static final Pattern IN_OUT = Pattern.compile("^(in|out)\\s+(\\w+)\\s+(\\w+)\\s*;", Pattern.MULTILINE);
    private static final Pattern SAMPLER = Pattern.compile("^uniform\\s+sampler2D\\s+(\\w+)\\s*;", Pattern.MULTILINE);
    private static final Pattern BLOCK = Pattern.compile("layout\\(std140\\)\\s+uniform\\s+(\\w+)\\s*\\{([^}]*)\\}");
    private static final Pattern FIELD = Pattern.compile("(\\w+)\\s+(\\w+)\\s*;");
    private final String vertex;
    private final String fragment;
    private final Map<String, Block> blocks;
    private final Map<String, Integer> samplers;

    private VulkanShaderSource(final String vertex, final String fragment, final Map<String, Block> blocks, final Map<String, Integer> samplers) {
        this.vertex = vertex;
        this.fragment = fragment;
        this.blocks = blocks;
        this.samplers = samplers;
    }

    static VulkanShaderSource convert(final String vertex, final String fragment, final List<String> attributes, final String define) {
        final Map<String, Block> blocks = new LinkedHashMap<>();
        for (final String source : List.of(vertex, fragment)) {
            final Matcher block = BLOCK.matcher(source);
            while (block.find()) {
                if (blocks.containsKey(block.group(1))) {
                    continue;
                }
                final List<String[]> fields = new ArrayList<>();
                final Matcher field = FIELD.matcher(block.group(2));
                while (field.find()) {
                    fields.add(new String[] { field.group(1), field.group(2) });
                }
                blocks.put(block.group(1), new Block(blocks.size(), fields));
            }
        }
        final Map<String, Integer> samplers = new LinkedHashMap<>();
        for (final String source : List.of(vertex, fragment)) {
            final Matcher sampler = SAMPLER.matcher(source);
            while (sampler.find()) {
                samplers.putIfAbsent(sampler.group(1), blocks.size() + samplers.size());
            }
        }
        final List<String> varyings = new ArrayList<>();
        final Matcher out = IN_OUT.matcher(vertex);
        while (out.find()) {
            if (out.group(1).equals("out")) {
                varyings.add(out.group(3));
            }
        }
        final String vsh = rewriteInOut(common(vertex, blocks, samplers, define), true, attributes, varyings);
        final String fsh = rewriteInOut(common(fragment, blocks, samplers, define), false, attributes, varyings);
        return new VulkanShaderSource(vsh, fsh, blocks, samplers);
    }

    String vertex() {
        return this.vertex;
    }

    String fragment() {
        return this.fragment;
    }

    Map<String, Block> blocks() {
        return this.blocks;
    }

    Map<String, Integer> samplers() {
        return this.samplers;
    }

    private static String common(final String source, final Map<String, Block> blocks, final Map<String, Integer> samplers, final String define) {
        String result = VERSION.matcher(source).replaceFirst(Matcher.quoteReplacement("#version 450\n" + define + "\n"));
        final Matcher block = BLOCK.matcher(result);
        final StringBuilder blockOut = new StringBuilder();
        while (block.find()) {
            final String header = "layout(std140, binding = " + blocks.get(block.group(1)).binding() + ") uniform " + block.group(1) + " {" + block.group(2) + "}";
            block.appendReplacement(blockOut, Matcher.quoteReplacement(header));
        }
        block.appendTail(blockOut);
        result = blockOut.toString();
        final Matcher sampler = SAMPLER.matcher(result);
        final StringBuilder samplerOut = new StringBuilder();
        while (sampler.find()) {
            sampler.appendReplacement(samplerOut, Matcher.quoteReplacement("layout(binding = " + samplers.get(sampler.group(1)) + ") uniform sampler2D " + sampler.group(1) + ";"));
        }
        sampler.appendTail(samplerOut);
        return samplerOut.toString();
    }

    private static String rewriteInOut(final String source, final boolean vertexStage, final List<String> attributes, final List<String> varyings) {
        final Matcher matcher = IN_OUT.matcher(source);
        final StringBuilder out = new StringBuilder();
        int fragmentOutputs = 0;
        while (matcher.find()) {
            final boolean input = matcher.group(1).equals("in");
            final String name = matcher.group(3);
            final int location;
            if (vertexStage) {
                location = input ? attributes.indexOf(name) : varyings.indexOf(name);
            } else {
                location = input ? varyings.indexOf(name) : fragmentOutputs++;
            }
            if (location < 0) {
                throw new IllegalStateException("walksylib shader " + (input ? "input " : "output ") + name + " has no Vulkan location");
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement("layout(location = " + location + ") " + matcher.group(1) + " " + matcher.group(2) + " " + name + ";"));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    record Block(int binding, List<String[]> fields) {
    }
}
