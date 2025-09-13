package net.flamgop.gpu.vertex;

import net.flamgop.gpu.state.StateManager;
import org.lwjgl.opengl.GL46;

import java.util.*;

public class VertexFormat {
    // goal:
    // make this code get wrapped into an awesome nice little format.
    // buffer.attribute(0, 3, GL_FLOAT, false, 0); // position
    // buffer.attribute(1, 2, GL_FLOAT, false, 3 * Float.BYTES); // uv
    // buffer.attribute(2, 4, GL_INT_2_10_10_10_REV, true, 5 * Float.BYTES); // normal
    // buffer.attribute(3, 4, GL_INT_2_10_10_10_REV, true, 5 * Float.BYTES + Integer.BYTES); // tangent

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Attribute[] bindings;

        private Builder() {
            this.bindings = new Attribute[StateManager.getStateInteger(GL46.GL_MAX_VERTEX_ATTRIB_BINDINGS)];
        }

        public Builder clear() {
            Arrays.fill(bindings, null);
            return this;
        }

        public Builder attribute(int index, Attribute attribute) {
            bindings[index] = attribute;
            return this;
        }

        public Builder attributes(int index, Attribute... attributes) {
            System.arraycopy(attributes, 0, bindings, index, attributes.length);
            return this;
        }

        public VertexFormat build() {
            int size = bindings.length;
            while (size > 0 && bindings[size - 1] == null) size--;
            return new VertexFormat(Arrays.copyOf(bindings, size));
        }
    }

    private final Attribute[] bindings;

    // sparse array blegh
    public VertexFormat(Attribute... bindings) {
        int maxAttribs = StateManager.getStateInteger(GL46.GL_MAX_VERTEX_ATTRIB_BINDINGS);
        if (bindings.length > maxAttribs)
            throw new IllegalArgumentException("Number of bindings in a vertex format must not exceed " + maxAttribs + "! (" + bindings.length + " > " + maxAttribs + ")");
        this.bindings = bindings;
    }

    public int stride(int bindingIndex) {
        int vertexSize = 0;
        for (Attribute attribute : bindings) {
            if (attribute == null) continue;
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
        for (int index = 0; index < bindings.length; index++) {
            Attribute attribute = bindings[index];
            if (attribute == null) continue;
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