package mapper.utils;

import mapper.exceptions.ExportMapperException;

public class TypeConverter {
    public Object convert(String value, Class<?> clazz) {
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
            throw new ExportMapperException("Incorrect type of primitive field");
        }
    }

    public boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return (clazz.isPrimitive() && clazz != void.class) ||
                clazz == Double.class || clazz == Float.class || clazz == Long.class ||
                clazz == Integer.class || clazz == Short.class || clazz == Character.class ||
                clazz == Byte.class || clazz == Boolean.class || clazz == String.class;
    }
}
