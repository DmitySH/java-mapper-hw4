package mapper.utils;

import mapper.exceptions.ExportMapperException;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public class TypeConverter {
    public Object convertToPrimitiveOrWrapper(String value, Class<?> clazz) {
        if (byte.class.equals(clazz) || Byte.class.equals(clazz)) {
            return Byte.valueOf(value);
        } else if (short.class.equals(clazz) || Short.class.equals(clazz)) {
            return Short.valueOf(value);
        } else if (int.class.equals(clazz) || Integer.class.equals(clazz)) {
            return Integer.valueOf(value);
        } else if (long.class.equals(clazz) || Long.class.equals(clazz)) {
            return Long.valueOf(value);
        } else if (float.class.equals(clazz) || Float.class.equals(clazz)) {
            return Float.valueOf(value);
        } else if (double.class.equals(clazz) || Double.class.equals(clazz)) {
            return Double.valueOf(value);
        } else if (boolean.class.equals(clazz) || Boolean.class.equals(clazz)) {
            return Boolean.valueOf(value);
        } else if (char.class.equals(clazz) || Character.class.equals(clazz)) {
            return value.charAt(0);
        } else if (String.class.equals(clazz)) {
            return value;
        } else {
            throw new IllegalArgumentException("Incorrect type of primitive field");
        }
    }




    public boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return (clazz.isPrimitive() && clazz != void.class) ||
                clazz == Double.class || clazz == Float.class || clazz == Long.class ||
                clazz == Integer.class || clazz == Short.class || clazz == Character.class ||
                clazz == Byte.class || clazz == Boolean.class || clazz == String.class;
    }

    public boolean isListOrSet(Class<?> clazz) {
        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> i : interfaces) {
            if (i == Set.class || i == List.class) {
                return true;
            }
        }

        return clazz == Set.class || clazz == List.class;
    }

    public boolean isDateTime(Class<?> clazz) {
        return clazz == LocalDate.class || clazz == LocalTime.class ||
                clazz == LocalDateTime.class;
    }
}
