package net.flamgop.gpu.texture;

import static org.lwjgl.opengl.GL46.*;

public enum TextureFormat {
    RED(GL_RED, false, false),
    RG(GL_RG, false, false),
    RGB(GL_RGB, false, false),
    RGBA(GL_RGBA, false, false),

    R8(GL_R8, true, false),
    R8_SNORM(GL_R8_SNORM, true, false),
    R16(GL_R16, true, false),
    R16_SNORM(GL_R16_SNORM, true, false),
    RG8(GL_RG8, true, false),
    RG8_SNORM(GL_RG8_SNORM, true, false),
    RG16(GL_RG16, true, false),
    RG16_SNORM(GL_RG16_SNORM, true, false),
    R3_G3_B2(GL_R3_G3_B2, true, false),
    RGB4(GL_RGB4, true, false),
    RGB5(GL_RGB5, true, false),
    RGB8(GL_RGB8, true, false),
    RGB8_SNORM(GL_RGB8_SNORM, true, false),
    RGB10(GL_RGB10, true, false),
    RGB12(GL_RGB12, true, false),
    RGB16_SNORM(GL_RGB16_SNORM, true, false),
    RGBA2(GL_RGBA2, true, false),
    RGBA4(GL_RGBA4, true, false),
    RGB5_A1(GL_RGB5_A1, true, false),
    RGBA8(GL_RGBA8, true, false),
    RGBA8_SNORM(GL_RGBA8_SNORM, true, false),
    RGB10_A2(GL_RGB10_A2, true, false),
    RGB10_A2UI(GL_RGB10_A2UI, true, false),
    RGBA12(GL_RGBA12, true, false),
    RGBA16(GL_RGBA16, true, false),
    SRGB8(GL_SRGB8, true, false),
    SRGB8_ALPHA8(GL_SRGB8_ALPHA8, true, false),
    R16F(GL_R16F, true, false),
    RG16F(GL_RG16F, true, false),
    RGB16F(GL_RGB16F, true, false),
    RGBA16F(GL_RGBA16F, true, false),
    R32F(GL_R32F, true, false),
    RG32F(GL_RG32F, true, false),
    RGB32F(GL_RGB32F, true, false),
    RGBA32F(GL_RGBA32F, true, false),
    R11F_G11F_B10F(GL_R11F_G11F_B10F, true, false),
    RGB9_E5(GL_RGB9_E5, true, false),
    R8I(GL_R8I, true, false),
    R8UI(GL_R8UI, true, false),
    R16I(GL_R16I, true, false),
    R16UI(GL_R16UI, true, false),
    R32I(GL_R32I, true, false),
    R32UI(GL_R32UI, true, false),
    RG8I(GL_RG8I, true, false),
    RG8UI(GL_RG8UI, true, false),
    RG16I(GL_RG16I, true, false),
    RG16UI(GL_RG16UI, true, false),
    RG32I(GL_RG32I, true, false),
    RG32UI(GL_RG32UI, true, false),
    RGB8I(GL_RGB8I, true, false),
    RGB8UI(GL_RGB8UI, true, false),
    RGB16I(GL_RGB16I, true, false),
    RGB16UI(GL_RGB16UI, true, false),
    RGB32I(GL_RGB32I, true, false),
    RGB32UI(GL_RGB32UI, true, false),
    RGBA8I(GL_RGBA8I, true, false),
    RGBA8UI(GL_RGBA8UI, true, false),
    RGBA16I(GL_RGBA16I, true, false),
    RGBA16UI(GL_RGBA16UI, true, false),
    RGBA32I(GL_RGBA32I, true, false),
    RGBA32UI(GL_RGBA32UI, true, false),

    COMPRESSED_RED(GL_COMPRESSED_RED, false, true),
    COMPRESSED_RG(GL_COMPRESSED_RG, false, true),
    COMPRESSED_RGB(GL_COMPRESSED_RGB, false, true),
    COMPRESSED_RGBA(GL_COMPRESSED_RGBA, false, true),
    COMPRESSED_SRGB(GL_COMPRESSED_SRGB, false, true),
    COMPRESSED_SRGB_ALPHA(GL_COMPRESSED_SRGB_ALPHA, false, true),
    COMPRESSED_RED_RGTC1(GL_COMPRESSED_RED_RGTC1, false, true),
    COMPRESSED_SIGNED_RED_RGTC1(GL_COMPRESSED_SIGNED_RED_RGTC1, false, true),
    COMPRESSED_RG_RGTC2(GL_COMPRESSED_RG_RGTC2, false, true),
    COMPRESSED_SIGNED_RG_RGTC2(GL_COMPRESSED_SIGNED_RG_RGTC2, false, true),
    COMPRESSED_RGBA_BPTC_UNORM(GL_COMPRESSED_RGBA_BPTC_UNORM, false, true),
    COMPRESSED_SRGB_ALPHA_BPTC_UNORM(GL_COMPRESSED_SRGB_ALPHA_BPTC_UNORM, false, true),
    COMPRESSED_RGB_BPTC_SIGNED_FLOAT(GL_COMPRESSED_RGB_BPTC_SIGNED_FLOAT, false, true),
    COMPRESSED_RGB_BPTC_UNSIGNED_FLOAT(GL_COMPRESSED_RGB_BPTC_UNSIGNED_FLOAT, false, true),

    DEPTH_COMPONENT32F(GL_DEPTH_COMPONENT32F, false, false, true),
    DEPTH_COMPONENT24(GL_DEPTH_COMPONENT24, false, false, true),
    DEPTH_COMPONENT16(GL_DEPTH_COMPONENT16, false, false, true),
    DEPTH32F_STENCIL8(GL_DEPTH32F_STENCIL8, false, false, true),
    DEPTH24_STENCIL8(GL_DEPTH24_STENCIL8, false, false, true),
    STENCIL_INDEX8(GL_STENCIL_INDEX8, false, false, true),

    ;
    final int glQualifier;
    final boolean sized;
    final boolean compressed;
    final boolean fancy;
    TextureFormat(int glQualifier, boolean sized, boolean compressed) {
        this(glQualifier, sized, compressed, false);
    }
    TextureFormat(int glQualifier, boolean sized, boolean compressed, boolean fancy) {
        this.glQualifier = glQualifier;
        this.sized = sized;
        this.compressed = compressed;
        this.fancy = fancy;
    }
}
