package net.flamgop.input;

public record KeyCombination(int keycode, int modifiers) {

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof KeyCombination(int keycode1, int modifiers1))) return false;
        return keycode == keycode1 && modifiers == modifiers1;
    }

    @Override
    public int hashCode() {
        return keycode ^ (modifiers << 8);
    }
}
