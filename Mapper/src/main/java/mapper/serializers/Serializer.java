package mapper.serializers;

import mapper.annotations.DateFormat;
import mapper.annotations.Exported;
import mapper.annotations.Ignored;
import mapper.annotations.PropertyName;
import mapper.enums.NullHandling;
import mapper.exceptions.ExportMapperException;
import mapper.interfaces.Mapper;
import mapper.utils.TypeConverter;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;

public class Serializer implements Mapper {
    private final TypeConverter converter;
    private final JsonWriter writer;
    private final JsonReader reader;
    private Set<Object> colors;

    public Serializer() {
        converter = new TypeConverter();
        writer = new JsonWriter();
        reader = new JsonReader();
    }

    class JsonReader {
        private Object parseObject(Class<?> clazz, String str) {
            StringBuilder sb = new StringBuilder(str);
            if (isNotSerializableType(clazz)) {
                throw new ExportMapperException("Type " + clazz.getSimpleName() + " is not exportable");
            }

            Object obj = createObject(clazz);

            Map<String, Field> fields = new HashMap<>();
            for (Field field : clazz.getDeclaredFields()) {
                if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers())
                        && !field.isAnnotationPresent(Ignored.class)) {
                    if (field.isAnnotationPresent(PropertyName.class)) {
                        fields.put(field.getAnnotation(PropertyName.class).value(), field);
                    } else {
                        fields.put(field.getName(), field);
                    }
                }
            }

            try {
                int i = 1;
                if (sb.length() == 2) {
                    return obj;
                }

                while (i < sb.length()) {
                    int keyStart = str.indexOf("\"", i) + 1;
                    int keyEnd = str.indexOf("#", keyStart);
                    String key = str.substring(keyStart, keyEnd);

                    int valueEnd;
                    Field field = fields.get(key);

                    if (field.trySetAccessible()) {
                        Class<?> fieldType = field.getType();

                        if (converter.isPrimitiveOrWrapper(fieldType)) {
                            int typeEnd = str.indexOf("\"", keyEnd);
//                            String typeStr = str.substring(keyEnd + 1, typeEnd);
//                            System.out.println(typeStr);

                            valueEnd = str.indexOf("\"", typeEnd + 3);
                            String value = str.substring(typeEnd + 3, valueEnd);
//                            System.out.println(value);

                            field.set(obj, converter.convertToPrimitiveOrWrapper(value, fieldType));
                            i = valueEnd + 2;
                        } else if (converter.isDateTime(fieldType)) {
                            int typeEnd = str.indexOf("\"", keyEnd);
//                            String typeStr = str.substring(keyEnd + 1, typeEnd);

                            valueEnd = str.indexOf("\"", typeEnd + 3);
                            String value = str.substring(typeEnd + 3, valueEnd);

                            Object dt;
                            dt = parseDateTime(field, fieldType, value);
                            field.set(obj, dt);
                            i = valueEnd + 2;
                        } else if (converter.isListOrSet(fieldType)) {
                            int typeEnd = str.indexOf("\"", keyEnd);
                            String[] typeStr = str.substring(keyEnd + 1, typeEnd).split("#");

                            int opened = 1;
                            int pos = typeEnd + 3;
                            while (opened != 0) {
                                if (sb.charAt(pos) == '[') {
                                    ++opened;
                                } else if (sb.charAt(pos) == ']') {
                                    --opened;
                                }
                                ++pos;
                            }

                            String value = str.substring(typeEnd + 2, pos);
//                            System.out.println(value);

                            Class<?> type = Class.forName(typeStr[0]);

                            field.set(obj, parseCollection(type,
                                    value));
                            i = pos + 1;
                        } else {
                            int typeEnd = str.indexOf("\"", keyEnd);
                            String typeStr = str.substring(keyEnd + 1, typeEnd);
//                            System.out.println(typeStr);

                            int opened = 1;
                            int pos = typeEnd + 3;
                            while (opened != 0) {
                                if (sb.charAt(pos) == '{') {
                                    ++opened;
                                } else if (sb.charAt(pos) == '}') {
                                    --opened;
                                }
                                ++pos;
                            }
                            String value = str.substring(typeEnd + 2, pos);

//                            System.out.println(value);

                            Class<?> type = Class.forName(typeStr);

                            field.set(obj, parseObject(type,
                                    value));
                            i = pos + 1;
                        }
                    }
                }
            } catch (ClassNotFoundException | IllegalAccessException e) {
                throw new ExportMapperException(e.getMessage());
            }

            return obj;
        }

        private Object parseDateTime(Field field, Class<?> fieldType, String value) {
            Object dt;
            if (field.isAnnotationPresent(DateFormat.class)) {
                DateTimeFormatter timeFormatter =
                        DateTimeFormatter.ofPattern(field.getAnnotation(DateFormat.class).value());

                if (LocalDate.class.equals(fieldType)) {
                    dt = LocalDate.parse(value, timeFormatter);
                } else if (LocalTime.class.equals(fieldType)) {
                    dt = LocalTime.parse(value, timeFormatter);
                } else {
                    dt = LocalDateTime.parse(value, timeFormatter);
                }
            } else {
                dt = parseNotFormattedDateTime(fieldType, value);
            }

            return dt;
        }

        private Object parseNotFormattedDateTime(Class<?> type, String value) {
            Object dt;
            if (LocalDate.class.equals(type)) {
                dt = LocalDate.parse(value);
            } else if (LocalTime.class.equals(type)) {
                dt = LocalTime.parse(value);
            } else {
                dt = LocalDateTime.parse(value);
            }

            return dt;
        }

        private Collection<Object> parseCollection(Class<?> collectionType, String str) throws ClassNotFoundException {

            if (isNotSerializableType(collectionType)) {
                throw new ExportMapperException("Type " + collectionType.getSimpleName() + " is not exportable");
            }

            // Here we can use only createObject with parameter == type of collection to be created.
            // It is always collection, and we can put there any Object.
            @SuppressWarnings("unchecked")
            Collection<Object> collection = (Collection<Object>) createObject(collectionType);
            StringBuilder sb = new StringBuilder(str);

            int i = 1;
            if (sb.length() == 2) {
                return collection;
            }

            while (i < sb.length()) {
                int innerTypeBegin = sb.indexOf("\"", i) + 1;
                int innerTypeEnd = sb.indexOf("\"", innerTypeBegin);

                String innerType = sb.substring(innerTypeBegin, innerTypeEnd);

                Class<?> innerClass = Class.forName(innerType);


                if (converter.isPrimitiveOrWrapper(innerClass)) {
                    int valueEnd = sb.indexOf("\"", innerTypeEnd + 3);
                    String value = sb.substring(innerTypeEnd + 3, valueEnd);
                    collection.add(converter.convertToPrimitiveOrWrapper(value, innerClass));

                    i = valueEnd + 2;
                } else if (converter.isDateTime(innerClass)) {
                    int valueEnd = sb.indexOf("\"", innerTypeEnd + 3);
                    String value = sb.substring(innerTypeEnd + 3, valueEnd);

                    Object dt = parseNotFormattedDateTime(innerClass, value);
                    collection.add(dt);

                    i = valueEnd + 2;
                } else if (converter.isListOrSet(innerClass)) {
                    int opened = 1;
                    int pos = innerTypeEnd + 3;
                    while (opened != 0) {
                        if (sb.charAt(pos) == '[') {
                            ++opened;
                        } else if (sb.charAt(pos) == ']') {
                            --opened;
                        }
                        ++pos;
                    }

                    String col = sb.substring(innerTypeEnd + 2, pos);
                    Object value = parseCollection(innerClass, col);
//                    System.out.println(value);
                    collection.add(value);
                    i = pos + 1;
                } else {
                    int opened = 1;
                    int pos = innerTypeEnd + 3;

                    while (opened != 0) {
                        if (sb.charAt(pos) == '{') {
                            ++opened;
                        } else if (sb.charAt(pos) == '}') {
                            --opened;
                        }
                        ++pos;
                    }

                    String obj = sb.substring(innerTypeEnd + 2, pos);
                    Object value = parseObject(innerClass, obj);
                    collection.add(value);
                    i = pos + 1;
                }
            }


            return collection;
        }
    }

    @Override
    public <T> T readFromString(Class<T> clazz, String input) {
        checkClassExportation(clazz);

        return clazz.cast(reader.parseObject(clazz, input));
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

    class JsonWriter {
        private String serializeObject(Object obj) {
            checkForCycles(obj);
            Class<?> clazz = obj.getClass();
            StringBuilder sb = new StringBuilder();
            Set<String> fieldNames = getFieldNames(clazz);

            sb.append('{');

            boolean excludeNulls =
                    clazz.getAnnotation(Exported.class).nullHandling().equals(NullHandling.EXCLUDE);

            for (Field field : clazz.getDeclaredFields()) {
                if (checkFieldIsSerializable(field, excludeNulls, obj)) {
                    try {
                        if (converter.isPrimitiveOrWrapper(field.getType())) {
                            String type = field.getType().getName();

                            // Key + type.
                            sb.append('\"');
                            sb.append(getPropertyName(field, fieldNames))
                                    .append('#').append(type);
                            sb.append('\"');

                            // Value.
                            sb.append(":\"");
                            sb.append(field.get(obj));
                            sb.append("\"");
                        } else if (converter.isDateTime(field.getType())) {
                            String type = field.getType().getName();
                            String dtFormat = field.isAnnotationPresent(DateFormat.class) ?
                                    field.getAnnotation(DateFormat.class).value() : null;

                            String res;
                            if (dtFormat != null) {
                                DateTimeFormatter timeColonFormatter = DateTimeFormatter.ofPattern(dtFormat);
                                res = timeColonFormatter.format((TemporalAccessor) field.get(obj));
                            } else {
                                res = field.get(obj).toString();
                            }

                            // Key + type.
                            sb.append('\"');
                            sb.append(getPropertyName(field, fieldNames))
                                    .append('#').append(type);
                            sb.append('\"');

                            // Value.
                            sb.append(":\"");
                            sb.append(res);
                            sb.append("\"");

                        } else if (converter.isListOrSet(field.getType())) {
                            String realType = field.get(obj).getClass().getName();
                            String abstractType = field.getGenericType().getTypeName();

                            // Key + type.
                            sb.append('\"');
                            sb.append(getPropertyName(field, fieldNames))
                                    .append('#').append(realType)
                                    .append('#').append(abstractType);
                            sb.append("\":");

                            // Value.
                            sb.append(serializeArray((Collection<?>) field.get(obj)));
                        } else {
                            String type = field.getType().getName();

                            // Key + type.
                            sb.append('\"');
                            sb.append(getPropertyName(field, fieldNames))
                                    .append('#').append(type);
                            sb.append("\":");

                            // Value.
                            sb.append(serializeObject(field.get(obj)));
                        }
                        sb.append(',');
                    } catch (IllegalAccessException e) {
                        throw new ExportMapperException("Can't get access to field | " + e.getMessage());
                    }
                }
            }

            int index;
            if ((index = sb.lastIndexOf(",")) != -1) {
                sb.deleteCharAt(index);
            }
            sb.append('}');

            colors.remove(obj);

            return sb.toString();
        }


        private String serializeArray(Collection<?> array) {
            StringBuilder sb = new StringBuilder();

            sb.append('[');

            if (array.isEmpty()) {
                sb.append(']');
                return sb.toString();
            }

            for (Object obj : array) {
                Class<?> clazz = obj.getClass();
                if (isNotSerializableType(clazz)) {
                    throw new ExportMapperException("Type " + clazz.getSimpleName() + " is not exportable");
                }

                if (converter.isPrimitiveOrWrapper(clazz) || converter.isDateTime(clazz)) {
                    String type = clazz.getName();

                    // Key + type.
                    sb.append('\"');
                    sb.append(type);
                    sb.append('\"');

                    // Value.
                    sb.append(":\"");
                    sb.append(obj);
                    sb.append("\"");
                } else if (converter.isListOrSet(clazz)) {
                    String type = clazz.getName();

                    // Key + type.
                    sb.append('\"');
                    sb.append(type);
                    sb.append("\":");

                    // Value.
                    sb.append(serializeArray((Collection<?>) obj));
                } else {
                    String type = clazz.getName();

                    // Key + type.
                    sb.append('\"');
                    sb.append(type);
                    sb.append("\":");

                    // Value.
                    sb.append(serializeObject(obj));
                }
                sb.append(',');
            }

            int index;
            if ((index = sb.lastIndexOf(",")) != -1) {
                sb.deleteCharAt(index);
            }
            sb.append(']');

            return sb.toString();
        }
    }

    @Override
    public String writeToString(Object object) {
        try {
            checkObjectExportation(object);
            colors = new HashSet<>();
            return writer.serializeObject(object);
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

    private boolean checkFieldIsSerializable(Field field, boolean excludeNulls,
                                             Object object) {

        if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers())) {
            if (field.trySetAccessible() && !field.isAnnotationPresent(Ignored.class)) {
                try {
                    return !excludeNulls || field.get(object) != null;
                } catch (IllegalAccessException e) {
                    throw new ExportMapperException("Can't get acess to field " +
                            field.getName() + " of " + object.getClass());
                }
            }
        }

        return false;
    }

    private Set<String> getFieldNames(Class<?> clazz) {
        Set<String> fieldNames = new HashSet<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isSynthetic() && !Modifier.isStatic(field.getModifiers())) {
                fieldNames.add(field.getName());
            }
        }

        return fieldNames;
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

    public boolean isNotSerializableType(Class<?> clazz) {
        return !converter.isPrimitiveOrWrapper(clazz) && !converter.isListOrSet(clazz) &&
                !converter.isDateTime(clazz) && !clazz.isEnum() &&
                !clazz.isAnnotationPresent(Exported.class);
    }

    private Object createObject(Class<?> clazz) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (InvocationTargetException |
                InstantiationException | IllegalAccessException |
                NoSuchMethodException e) {
            throw new ExportMapperException(e.getMessage());
        }
    }

    private void checkForCycles(Object obj) {
        if (colors.contains(obj)) {
            throw new ExportMapperException("There is cycle for object of " + obj.getClass());
        }

        colors.add(obj);
    }
}
