package de.glmtk.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

public class ReflectionUtils {
    public static <T> T newInstance(Class<T> clazz,
                                    Object... params) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class<?>[] paramTypes = new Class<?>[params.length];
        for (int i = 0; i != params.length; ++i)
            paramTypes[i] = params[i].getClass();
        Constructor<T> cons = clazz.getDeclaredConstructor(paramTypes);
        cons.setAccessible(true);
        T obj = cons.newInstance(params);
        return obj;
    }

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
