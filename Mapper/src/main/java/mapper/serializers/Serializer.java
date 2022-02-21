package mapper.serializers;

import mapper.annotations.Exported;
import mapper.annotations.Ignored;
import mapper.annotations.PropertyName;
import mapper.enums.NullHandling;
import mapper.exceptions.ExportMapperException;
import mapper.interfaces.Mapper;
import mapper.utils.TypeConverter;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class Serializer implements Mapper {
    private static final TypeConverter converter = new TypeConverter();

    @Override
    public <T> T readFromString(Class<T> clazz, String input) {

        checkClassExportation(clazz);
        Map<String, Field> fields = new HashMap<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers())
                    && !field.isAnnotationPresent(Ignored.class)) {
                fields.put(field.getName(), field);
            }
        }


        Object obj = createObject(clazz);


        Map<String, String> elements = parseObject(input);
        try {
            for (String elementName :
                    elements.keySet()) {
                Field field = fields.get(elementName);

                if (field.trySetAccessible()) {
                    Class<?> fieldType = field.getType();
                    if (converter.isPrimitiveOrWrapper(fieldType)) {
                        field.set(obj, converter.convert(elements.get(elementName), fieldType));
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new ExportMapperException("Can't get access to field\n\r" + e.getMessage());
        }

        return clazz.cast(obj);
    }

    @Override
    public <T> T read(Class<T> clazz, InputStream inputStream) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        for (int result = bis.read(); result != -1; result = bis.read()) {
            buf.write((byte) result);
        }

        System.out.println(buf.toString(StandardCharsets.UTF_8));
        return null;
    }

    @Override
    public <T> T read(Class<T> clazz, File file) throws IOException {
        String strObject;
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            strObject = reader.readLine();
        }

        System.out.println(strObject);
        return null;
    }

    @Override
    public String writeToString(Object object) {
        try {
            checkObjectExportation(object);
            return serializeToJson(object);
        } catch (Exception e) {
            throw new ExportMapperException(e.getMessage());
        }
    }

    @Override
    public void write(Object object, OutputStream outputStream) throws IOException {
        try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            writer.write(writeToString(object));
        }
    }

    @Override
    public void write(Object object, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            writer.write(writeToString(object));
        }
    }

    private Object createObject(Class<?> clazz) {
        try {
            Class<?> emptyClass = Class.forName(clazz.getName());
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | InvocationTargetException |
                InstantiationException | IllegalAccessException |
                NoSuchMethodException e) {
            throw new ExportMapperException(e.getMessage());
        }
    }

    private void checkObjectExportation(Object object) {
        if (Objects.isNull(object)) {
            throw new ExportMapperException("Can't serialize a null object");
        }

        checkClassExportation(object.getClass());
    }

    private void checkClassExportation(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(Exported.class)) {
            throw new ExportMapperException("The class " + clazz.getSimpleName() +
                    " is not annotated with Exported");
        }

        if (clazz.getSuperclass() != Object.class) {
            throw new ExportMapperException("The class " + clazz.getSimpleName() +
                    " has not only Object superclass");
        }

        try {
            clazz.getConstructor();
        } catch (NoSuchMethodException ex) {
            throw new ExportMapperException("The class " + clazz.getSimpleName() +
                    " does not have parameterless public constructor");
        }
    }

    private String serializeToJson(Object object) throws IllegalAccessException {
        Class<?> clazz = object.getClass();
        Map<String, String> elements = new HashMap<>();
        Set<String> fieldNames = new HashSet<>();

        boolean excludeNulls =
                clazz.getAnnotation(Exported.class).nullHandling().equals(NullHandling.EXCLUDE);

        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers())) {
                fieldNames.add(field.getName());
            }
        }

        for (Field field : clazz.getDeclaredFields()) {
            if (checkFieldIsSerializable(field, excludeNulls, object)) {
                elements.put(getPropertyName(field, fieldNames), String.valueOf(field.get(object)));
            }
        }

        String jsonString = elements.entrySet()
                .stream()
                .map(entry -> "\"" + entry.getKey() + "\":\"" + entry.getValue() + "\"")
                .collect(Collectors.joining(","));

        return "{" + jsonString + "}";
    }

    private boolean checkFieldIsSerializable(Field field, boolean excludeNulls,
                                             Object object) throws IllegalAccessException {

        if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers())) {
            if (field.trySetAccessible() && !field.isAnnotationPresent(Ignored.class)) {
                return !excludeNulls || field.get(object) != null;
            }
        }

        return false;
    }

    private String getPropertyName(Field field, Set<String> fieldNames) {
        String value = "";
        if (field.isAnnotationPresent(PropertyName.class)) {
            value = field.getAnnotation(PropertyName.class).value();
        }

        if (fieldNames.contains(value)) {
            throw new ExportMapperException("The class has property name same as " +
                    "field's name: " + value);
        }

        return value.isEmpty() ? field.getName() : value;
    }

    private Map<String, String> parseObject(String str) {
        Map<String, String> elements = new HashMap<>();
        StringBuilder sb = new StringBuilder(str);

        int i = 0;
        while (i < sb.length()) {
            if (sb.charAt(i) == '\"') {
                int keyEnd = sb.indexOf("\"", i + 1);
                String key = sb.substring(i + 1, keyEnd);

                int valueEnd = sb.indexOf("\"", keyEnd + 3);
                String value = sb.substring(keyEnd + 3, valueEnd);
                elements.put(key, value);
                i = valueEnd + 1;
            } else {
                i++;
            }
        }

        return elements;
    }
}
