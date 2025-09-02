package net.flamgop.gpu.debug;

import net.flamgop.gpu.state.Capability;
import net.flamgop.gpu.state.StateManager;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL43C.*;
import static org.lwjgl.opengl.GL43C.GL_DEBUG_SEVERITY_HIGH;
import static org.lwjgl.opengl.GL43C.GL_DEBUG_SEVERITY_LOW;
import static org.lwjgl.opengl.GL43C.GL_DEBUG_SEVERITY_MEDIUM;
import static org.lwjgl.opengl.GL43C.GL_DEBUG_SEVERITY_NOTIFICATION;
import static org.lwjgl.opengl.GL43C.GL_DEBUG_SOURCE_APPLICATION;
import static org.lwjgl.opengl.GL43C.GL_DEBUG_SOURCE_OTHER;
import static org.lwjgl.opengl.GL43C.GL_DEBUG_SOURCE_THIRD_PARTY;
import static org.lwjgl.opengl.GL43C.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR;
import static org.lwjgl.opengl.GL43C.GL_DEBUG_TYPE_ERROR;
import static org.lwjgl.opengl.GL43C.GL_DEBUG_TYPE_MARKER;
import static org.lwjgl.opengl.GL43C.GL_DEBUG_TYPE_OTHER;
import static org.lwjgl.opengl.GL43C.GL_DEBUG_TYPE_PERFORMANCE;
import static org.lwjgl.opengl.GL43C.GL_DEBUG_TYPE_POP_GROUP;
import static org.lwjgl.opengl.GL43C.GL_DEBUG_TYPE_PORTABILITY;
import static org.lwjgl.opengl.GL43C.GL_DEBUG_TYPE_PUSH_GROUP;
import static org.lwjgl.opengl.GL43C.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR;

public class DebugLogging {
    private static String getSourceString(int source) {
        return switch (source) {
            case GL_DEBUG_SOURCE_API -> "API";
            case GL_DEBUG_SOURCE_WINDOW_SYSTEM -> "Window System";
            case GL_DEBUG_SOURCE_SHADER_COMPILER -> "Shader Compiler";
            case GL_DEBUG_SOURCE_THIRD_PARTY -> "Third Party";
            case GL_DEBUG_SOURCE_APPLICATION -> "Application";
            case GL_DEBUG_SOURCE_OTHER -> "Other";
            default -> "Unknown";
        };
    }

    private static String getTypeString(int type) {
        return switch (type) {
            case GL_DEBUG_TYPE_ERROR -> "Error";
            case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR -> "Deprecated Behavior";
            case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR -> "Undefined Behavior";
            case GL_DEBUG_TYPE_PORTABILITY -> "Portability";
            case GL_DEBUG_TYPE_PERFORMANCE -> "Performance";
            case GL_DEBUG_TYPE_MARKER -> "Marker";
            case GL_DEBUG_TYPE_PUSH_GROUP -> "Push Group";
            case GL_DEBUG_TYPE_POP_GROUP -> "Pop Group";
            case GL_DEBUG_TYPE_OTHER -> "Other";
            default -> "Unknown";
        };
    }

    private static String getSeverityString(int severity) {
        return switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH -> "High";
            case GL_DEBUG_SEVERITY_MEDIUM -> "Medium";
            case GL_DEBUG_SEVERITY_LOW -> "Low";
            case GL_DEBUG_SEVERITY_NOTIFICATION -> "Notification";
            default -> "Unknown";
        };
    }

    public static void enable() {
        StateManager.enable(Capability.DEBUG_OUTPUT);
        StateManager.enable(Capability.DEBUG_OUTPUT_SYNCHRONOUS);
        glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DONT_CARE, (IntBuffer) null, true);
        glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DEBUG_SEVERITY_NOTIFICATION, (IntBuffer) null, false);
        glDebugMessageCallback((source, type, id, severity, _, message, _) -> {
            System.out.println("OpenGL Debug Message:");
            System.out.println("  Source  : " + getSourceString(source));
            System.out.println("  Type    : " + getTypeString(type));
            System.out.println("  ID      : " + id);
            System.out.println("  Severity: " + getSeverityString(severity));
            System.out.println("  Message : " + MemoryUtil.memUTF8(message));
            System.out.println();
        }, 0L);
    }
}
