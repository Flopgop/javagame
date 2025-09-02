package net.flamgop.gpu.vertex;

import java.util.*;

public class VertexFormat {
    // goal:
    // make this code get wrapped into an awesome nice little format.
    // buffer.attribute(0, 3, GL_FLOAT, false, 0); // position
    // buffer.attribute(1, 2, GL_FLOAT, false, 3 * Float.BYTES); // uv
    // buffer.attribute(2, 4, GL_INT_2_10_10_10_REV, true, 5 * Float.BYTES); // normal
    // buffer.attribute(3, 4, GL_INT_2_10_10_10_REV, true, 5 * Float.BYTES + Integer.BYTES); // tangent

    private final List<Attribute> attributes = new ArrayList<>();

    public VertexFormat() {}

    public VertexFormat attribute(Attribute attribute) {
        this.attributes.add(attribute);
        return this;
    }

    public VertexFormat attributes(Attribute... attributes) {
        this.attributes.addAll(Arrays.asList(attributes));
        return this;
    }

    public VertexFormat clear() {
        this.attributes.clear();
        return this;
    }

    public VertexFormat attribute(int index, Attribute attribute) {
        this.attributes.add(index, attribute);
        return this;
    }

    public VertexFormat attributes(int index, Attribute... attributes) {
        for (int i = 0; i < attributes.length; i++) {
            this.attributes.add(index + i, attributes[i]);
        }
        return this;
    }

    public int stride(int bindingIndex) {
        int vertexSize = 0;
        for (Attribute attribute : attributes) {
            if (attribute.bindingIndex() == bindingIndex) {
                if (attribute.type().packed())
                    vertexSize += attribute.type().byteCount();
                else
                    vertexSize += attribute.size() * attribute.type().byteCount();
            }
        }
        return vertexSize;
    }

    public void setup(VertexArray buffer) {
        Map<Integer, Integer> bindingOffsets = new HashMap<>();
        for (int index = 0; index < attributes.size(); index++) {
            Attribute attribute = attributes.get(index);
            int bindingIndex = attribute.bindingIndex();

            int offset = bindingOffsets.getOrDefault(bindingIndex, 0);

            buffer.attribute(index, attribute.size(), attribute.type().glQualifier(), attribute.normalized(), offset, bindingIndex, attribute.bindingDivisor());

            if (attribute.type().packed()) // packed means the format is all in one value sizeof byteCount(),
                bindingOffsets.put(bindingIndex, offset + attribute.type().byteCount());
            else
                bindingOffsets.put(bindingIndex, offset + attribute.size() * attribute.type().byteCount());
        }
    }
}