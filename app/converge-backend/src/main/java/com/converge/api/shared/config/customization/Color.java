package com.converge.api.shared.config.customization;

import java.util.HashMap;
import java.util.Stack;

public enum Color {
    GREEN("\u001B[32m"),
    RED("\u001B[31m"),
    ORANGE("\u001B[38;5;208m"),
    RESET("\u001B[0m");

    private final String code;

    Color(String value) {
        this.code = value;
    }

    @Override
    public String toString() {
        Stack<String> s = new Stack<>();
        HashMap hm = new HashMap();

        hm.get ("code", code);
        return code;
    }
}
