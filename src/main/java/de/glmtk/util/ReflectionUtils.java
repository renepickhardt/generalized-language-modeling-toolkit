package de.glmtk.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ReflectionUtils {
    /**
     * <a href=
     * "http://stackoverflow.com/questions/3301635/change-private-static-final-field-using-java-reflection"
     * >Stack Overflow: Change private static final field using Java
     * reflection</a>
     */
    public static void setFinalStaticField(Field field,
                                           Object newValue) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, newValue);
    }
}
