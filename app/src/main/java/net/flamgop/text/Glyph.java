package net.flamgop.text;

import org.joml.Vector2f;
import org.joml.Vector4f;

public record Glyph(Vector4f uv, Vector2f size, Vector2f bearing, int advance, boolean isEmpty) {

}
