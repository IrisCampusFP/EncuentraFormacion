package com.irisperez.tfg.encuentraformacion.util;

import java.text.Normalizer;

public final class H2Functions {

    private H2Functions() {}

    public static String unaccent(String input) {
        if (input == null) return null;
        return Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
