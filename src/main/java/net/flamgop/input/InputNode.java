package net.flamgop.input;

import java.util.HashMap;
import java.util.Map;

public class InputNode {
    public Runnable action = null;
    public Map<KeyCombination, InputNode> next = new HashMap<>();
}
