package mapper.serializers;

import mapper.annotations.Exported;
import mapper.annotations.Ignored;
import mapper.annotations.PropertyName;
import mapper.enums.NullHandling;
import mapper.exceptions.ExportMapperException;
import mapper.interfaces.Mapper;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class Serializer implements Mapper {
    @Override
    public <T> T readFromString(Class<T> clazz, String input) {
        throw new UnsupportedOperationException();
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
            checkExportation(object);
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

    private void checkExportation(Object object) {
        if (Objects.isNull(object)) {
            throw new ExportMapperException("Can't serialize a null object");
        }

        Class<?> objectClass = object.getClass();

        if (!objectClass.isAnnotationPresent(Exported.class)) {
            throw new ExportMapperException("The class " + objectClass.getSimpleName() +
                    " is not annotated with Exported");
        }

        try {
            objectClass.getConstructor();
        } catch (NoSuchMethodException ex) {
            throw new ExportMapperException("The class " + objectClass.getSimpleName() +
                    " does not have parameterless public constructor");
        }

        if (objectClass.getSuperclass() != Object.class) {
            throw new ExportMapperException("The class " + objectClass.getSimpleName() +
                    " has not only Object superclass");
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
            if (checkWriteField(field, excludeNulls, object)) {
                elements.put(getPropertyName(field, fieldNames), String.valueOf(field.get(object)));
            }
        }

        String jsonString = elements.entrySet()
                .stream()
                .map(entry -> "\"" + entry.getKey() + "\":\"" + entry.getValue() + "\"")
                .collect(Collectors.joining(","));

        return "{" + jsonString + "}";
    }

    private boolean checkWriteField(Field field, boolean excludeNulls,
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
}
