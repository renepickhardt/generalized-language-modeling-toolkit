/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2014-2015 Lukas Schmelzeisen
 *
 * GLMTK is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * GLMTK is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GLMTK. If not, see <http://www.gnu.org/licenses/>.
 *
 * See the AUTHORS file for contributors.
 */

package de.glmtk.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;


public class ReflectionUtils {
    private ReflectionUtils() {}

    public static <T> T newInstance(Class<T> clazz,
                                    Object... params)
                                            throws NoSuchMethodException,
                                            SecurityException,
                                            InstantiationException,
                                            IllegalAccessException,
                                            IllegalArgumentException,
                                            InvocationTargetException {
        Class<?>[] paramTypes = new Class<?>[params.length];
        for (int i = 0; i != params.length; ++i) {
            paramTypes[i] = params[i].getClass();
        }
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
                                           Object newValue)
                                                   throws IllegalArgumentException,
                                                   IllegalAccessException,
                                                   NoSuchFieldException,
                                                   SecurityException {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, newValue);
    }
}
