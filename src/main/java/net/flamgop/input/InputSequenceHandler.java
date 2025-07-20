package net.flamgop.input;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class InputSequenceHandler {
    private final InputNode root = new InputNode();
    private InputNode current = root;
    private KeyCombination lastCombo = null;

    private final List<KeyCombination> debugCurrentSequenceView = new ArrayList<>();

    public void registerSequence(List<KeyCombination> sequence, Runnable action) {
        InputNode node = root;
        for (KeyCombination key : sequence) {
            node = node.next.computeIfAbsent(key, k -> new InputNode());
        }
        node.action = action;
    }

    public void handleKey(int key, int mods, int action) {
        KeyCombination combo = new KeyCombination(key, mods);

        if (isModifierKey(key)) return;
        if (combo.equals(lastCombo) && action == GLFW.GLFW_REPEAT) return;

        if (action == GLFW.GLFW_RELEASE) {
            reset();
            return;
        }

        debugCurrentSequenceView.add(combo);
        lastCombo = combo;

        InputNode next = current.next.get(combo);
        if (next != null) {
            current = next;

            if (current.action != null) {
                current.action.run();
                reset();
            }
        }
    }

    private boolean isModifierKey(int key) {
        return key == GLFW.GLFW_KEY_LEFT_SHIFT || key == GLFW.GLFW_KEY_RIGHT_SHIFT ||
                key == GLFW.GLFW_KEY_LEFT_CONTROL || key == GLFW.GLFW_KEY_RIGHT_CONTROL ||
                key == GLFW.GLFW_KEY_LEFT_ALT || key == GLFW.GLFW_KEY_RIGHT_ALT ||
                key == GLFW.GLFW_KEY_LEFT_SUPER || key == GLFW.GLFW_KEY_RIGHT_SUPER ||
                key == GLFW.GLFW_KEY_CAPS_LOCK || key == GLFW.GLFW_KEY_NUM_LOCK;
    }

    private void reset() {
        current = root;
        debugCurrentSequenceView.clear();
        lastCombo = null;
    }

    public String getDebugSequence() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < debugCurrentSequenceView.size(); i++) {
            KeyCombination kc = debugCurrentSequenceView.get(i);
            sb.append(formatKeyCombo(kc));
            if (i < debugCurrentSequenceView.size() - 1) {
                sb.append(" -> ");
            }
        }
        return sb.toString();
    }

    private String formatKeyCombo(KeyCombination kc) {
        List<String> parts = new ArrayList<>();

        if ((kc.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) parts.add("Ctrl");
        if ((kc.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0) parts.add("Shift");
        if ((kc.modifiers() & GLFW.GLFW_MOD_ALT) != 0) parts.add("Alt");
        if ((kc.modifiers() & GLFW.GLFW_MOD_SUPER) != 0) parts.add("Super");

        // Append key name
        String name = GLFW.glfwGetKeyName(kc.keycode(), 0);
        if (name != null) {
            parts.add(name);
        }

        return String.join("+", parts);
    }
}
