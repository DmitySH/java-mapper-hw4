package mapper.serializers;

import mapper.annotations.DateFormat;
import mapper.annotations.Exported;
import mapper.annotations.Ignored;
import mapper.annotations.PropertyName;
import mapper.enums.NullHandling;
import mapper.exceptions.ExportMapperException;
import mapper.interfaces.Cleaner;
import mapper.interfaces.Mapper;
import mapper.utils.StringCleaner;
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
    private final Cleaner stringCleaner;

    private List<Object> colors;

    public Serializer() {
        converter = new TypeConverter();
        writer = new JsonWriter();
        reader = new JsonReader();
        stringCleaner = new StringCleaner();
    }

    @Override
    public <T> T readFromString(Class<T> clazz, String input) {
        try {
            return clazz.cast(reader.parseObject(clazz, input));
        } catch (Exception e) {
            throw new ExportMapperException(e.getMessage());
        }
    }

    @Override
    public <T> T read(Class<T> clazz, InputStream inputStream) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(inputStream);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        for (int result = bis.read(); result != -1; result = bis.read()) {
            buf.write((byte) result);
        }

        return clazz.cast(reader.parseObject(clazz, buf.toString(StandardCharsets.UTF_8)));
    }

    @Override
    public <T> T read(Class<T> clazz, File file) throws IOException {
        String strObject;
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            strObject = reader.readLine();
        }

        return clazz.cast(reader.parseObject(clazz, strObject));
    }

    @Override
    public String writeToString(Object object) {
        try {
            colors = new ArrayList<>();
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

    class JsonReader {
        private Object parseObject(Class<?> clazz, String str) {
            checkClassExportation(clazz);
            StringBuilder sb = new StringBuilder(str);
            if (isNotSerializableType(clazz)) {
                throw new ExportMapperException("Type " + clazz.getSimpleName() + " is not exportable");
            }

            Object obj = createObject(clazz);

            Map<String, Field> fields = getFieldMap(clazz);

            try {
                int i = 1;
                int prev;
                if (sb.length() == 2) {
                    return obj;
                }

                while (i < sb.length()) {
                    prev = i;
                    int keyStart = str.indexOf("\"", i) + 1;
                    int keyEnd = str.indexOf("#", keyStart);
                    String key = str.substring(keyStart, keyEnd);

                    Field field = fields.get(key);

                    if (field.trySetAccessible()) {
                        Class<?> fieldType = field.getType();

                        if (converter.isPrimitiveOrWrapper(fieldType) || fieldType.isEnum()) {
                            int typeEnd = str.indexOf("\"", keyEnd);

                            i = parsePrimitiveField(str, obj, field, fieldType, typeEnd);
                        } else if (converter.isDateTime(fieldType)) {
                            i = parseDateTimeField(str, obj, keyEnd, field, fieldType);
                        } else if (converter.isListOrSet(fieldType)) {
                            int typeEnd = str.indexOf("\"", keyEnd);
                            String[] typeStr = str.substring(keyEnd + 1, typeEnd).split("#");

                            if (typeStr[0].equals("null")) {
                                field.set(obj, null);
                                i = str.indexOf('\"', typeEnd + 3) + 1;
                                continue;
                            }

                            int opened = 1;
                            int pos = typeEnd + 3;
                            pos = getPos(sb, opened, pos, '[', ']');
                            String value = str.substring(typeEnd + 2, pos);
                            Class<?> type = Class.forName(typeStr[0]);
                            field.set(obj, parseCollection(type,
                                    value));
                            i = pos + 1;
                        } else {
                            int typeEnd = str.indexOf("\"", keyEnd);
                            String typeStr = str.substring(keyEnd + 1, typeEnd);

                            if (str.charAt(typeEnd + 2) == '\"') {
                                field.set(obj, null);
                                i = str.indexOf('\"', typeEnd + 3) + 1;
                                continue;
                            }

                            int opened = 1;
                            int pos = typeEnd + 3;
                            pos = getPos(sb, opened, pos, '{', '}');
                            String value = str.substring(typeEnd + 2, pos);

                            Class<?> type = Class.forName(typeStr);

                            field.set(obj, parseObject(type,
                                    value));
                            i = pos + 1;
                        }
                    }
                    if (prev >= i) {
                        throw new ExportMapperException("Incorrect string format");
                    }
                }
            } catch (ClassNotFoundException | IllegalAccessException e) {
                throw new ExportMapperException(e.getMessage());
            }

            return obj;
        }

        private int getPos(StringBuilder sb, int opened, int pos, char c, char c2) {
            while (opened != 0) {
                if (sb.charAt(pos) == c) {
                    ++opened;
                } else if (sb.charAt(pos) == c2) {
                    --opened;
                }
                ++pos;
            }
            return pos;
        }

        private int parseDateTimeField(String str, Object obj, int keyEnd, Field field, Class<?> fieldType) throws IllegalAccessException {
            int i;
            int valueEnd;
            int typeEnd = str.indexOf("\"", keyEnd);

            valueEnd = str.indexOf("\"", typeEnd + 3);
            String value = str.substring(typeEnd + 3, valueEnd);

            if (!value.equals("null")) {
                Object dt;
                dt = parseDateTime(field, fieldType, value);
                field.set(obj, dt);
            } else {
                field.set(obj, null);
            }

            i = valueEnd + 2;
            return i;
        }

        private int parsePrimitiveField(String str, Object obj, Field field, Class<?> fieldType, int typeEnd) throws IllegalAccessException {
            int i;
            int valueEnd;
            valueEnd = str.indexOf("\"", typeEnd + 3);
            String value = str.substring(typeEnd + 3, valueEnd);
            if (value.equals("null")) {
                field.set(obj, null);
            } else {
                if (fieldType.isEnum()) {
                    try {
                        Method valueOfMethod = fieldType.getDeclaredMethod("valueOf", String.class);
                        field.set(obj, valueOfMethod.invoke(null, value));
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        throw new ExportMapperException("Can't parse enum" + fieldType);
                    }
                } else {
                    if (field.getType().equals(String.class)) {
                        value = stringCleaner.recoverString(value);
                    }
                    field.set(obj, converter.convertToPrimitiveOrWrapper(value, fieldType));
                }
            }
            i = valueEnd + 2;
            return i;
        }

        private Map<String, Field> getFieldMap(Class<?> clazz) {
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
            return fields;
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
            int prev;

            if (sb.length() == 2) {
                return collection;
            }
            while (i < sb.length()) {
                prev = i;
                int innerTypeBegin = sb.indexOf("\"", i) + 1;
                int innerTypeEnd = sb.indexOf("\"", innerTypeBegin);

                String innerType = sb.substring(innerTypeBegin, innerTypeEnd);
//                System.out.println(innerType);
                if (innerType.equals("null")) {
                    collection.add(null);
                    i = sb.indexOf("\"", innerTypeEnd + 3) + 2;
                    continue;
                }

                Class<?> innerClass = Class.forName(innerType);


                if (converter.isPrimitiveOrWrapper(innerClass) || innerClass.isEnum()) {
                    i = parsePrimitiveElement(collection, sb, innerTypeEnd, innerType, innerClass);
                } else if (converter.isDateTime(innerClass)) {
                    int valueEnd = sb.indexOf("\"", innerTypeEnd + 3);
                    String value = sb.substring(innerTypeEnd + 3, valueEnd);

                    Object dt = parseNotFormattedDateTime(innerClass, value);
                    collection.add(dt);

                    i = valueEnd + 2;
                } else if (converter.isListOrSet(innerClass)) {
                    i = parseCollectionAsElement(collection, sb, innerTypeEnd, innerClass);
                } else {
                    i = parseObjectElement(collection, sb, innerTypeEnd, innerClass);
                }
                if (prev >= i) {
                    throw new ExportMapperException("Incorrect string format");
                }
            }

            return collection;
        }

        private int parseObjectElement(Collection<Object> collection, StringBuilder sb, int innerTypeEnd, Class<?> innerClass) {
            int i;
            int opened = 1;
            int pos = innerTypeEnd + 3;

            pos = getPos(sb, opened, pos, '{', '}');

            String obj = sb.substring(innerTypeEnd + 2, pos);
            Object value = parseObject(innerClass, obj);
            collection.add(value);
            i = pos + 1;
            return i;
        }

        private int parseCollectionAsElement(Collection<Object> collection, StringBuilder sb, int innerTypeEnd, Class<?> innerClass) throws ClassNotFoundException {
            int i;
            int opened = 1;
            int pos = innerTypeEnd + 3;
            pos = getPos(sb, opened, pos, '[', ']');

            String col = sb.substring(innerTypeEnd + 2, pos);
            Object value = parseCollection(innerClass, col);
            collection.add(value);
            i = pos + 1;
            return i;
        }

        private int parsePrimitiveElement(Collection<Object> collection, StringBuilder sb, int innerTypeEnd, String innerType, Class<?> innerClass) {
            int i;
            int valueEnd = sb.indexOf("\"", innerTypeEnd + 3);
            String value = sb.substring(innerTypeEnd + 3, valueEnd);

            if (innerClass.isEnum()) {
                try {
                    Method valueOfMethod = innerClass.getDeclaredMethod("valueOf", String.class);
                    collection.add(valueOfMethod.invoke(null, value));
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    throw new ExportMapperException("Can't parse enum" + innerType);
                }
            } else {
                if (innerClass.equals(String.class)) {
                    value = stringCleaner.recoverString(value);
                }
                collection.add(converter.convertToPrimitiveOrWrapper(value, innerClass));
            }

            i = valueEnd + 2;
            return i;
        }
    }

    class JsonWriter {
        private String serializeObject(Object obj) {
            checkObjectExportation(obj);
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
                        if (converter.isPrimitiveOrWrapper(field.getType()) || field.getType().isEnum()) {
                            serializePrimitiveField(obj, sb, fieldNames, field);
                        } else if (converter.isDateTime(field.getType())) {
                            serializeDateTimeField(obj, sb, fieldNames, field);
                        } else if (converter.isListOrSet(field.getType())) {
                            serializeCollectionField(obj, sb, fieldNames, field);
                        } else {
                            serializeObjectField(obj, sb, fieldNames, field);
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

        private void serializeObjectField(Object obj, StringBuilder sb, Set<String> fieldNames, Field field) throws IllegalAccessException {
            String type = field.getType().getName();

            // Key + type.
            sb.append('\"');
            sb.append(getPropertyName(field, fieldNames))
                    .append('#').append(type);
            sb.append("\":");

            // Value.
            Object objValue = field.get(obj);
            if (objValue == null) {
                sb.append("\"null\"");
            } else {
                sb.append(serializeObject(objValue));
            }
        }

        private void serializeCollectionField(Object obj, StringBuilder sb, Set<String> fieldNames, Field field) throws IllegalAccessException {
            Object objValue = field.get(obj);
            String realType;
            String abstractType;
            if (objValue != null) {
                realType = objValue.getClass().getName();
                abstractType = field.getGenericType().getTypeName();
            } else {
                realType = "null";
                abstractType = "null";
            }


            // Key + type.
            sb.append('\"');
            sb.append(getPropertyName(field, fieldNames))
                    .append('#').append(realType)
                    .append('#').append(abstractType);
            sb.append("\":");

            // Value.
            if (objValue == null) {
                sb.append("\"null\"");
            } else {
                sb.append(serializeArray((Collection<?>) objValue));
            }
        }

        private void serializeDateTimeField(Object obj, StringBuilder sb, Set<String> fieldNames, Field field) throws IllegalAccessException {
            String type = field.getType().getName();
            String dtFormat = field.isAnnotationPresent(DateFormat.class) ?
                    field.getAnnotation(DateFormat.class).value() : null;

            String res;
            Object objValue = field.get(obj);
            if (objValue == null) {
                res = "null";
            } else {
                if (dtFormat != null) {
                    DateTimeFormatter timeColonFormatter = DateTimeFormatter.ofPattern(dtFormat);
                    res = timeColonFormatter.format((TemporalAccessor) objValue);
                } else {
                    res = objValue.toString();
                }
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
        }

        private void serializePrimitiveField(Object obj, StringBuilder sb, Set<String> fieldNames, Field field) throws IllegalAccessException {
            String type = field.getType().getName();

            // Key + type.
            sb.append('\"');
            sb.append(getPropertyName(field, fieldNames))
                    .append('#').append(type);
            sb.append('\"');

            // Value.
            sb.append(":\"");
            String value = String.valueOf(field.get(obj));
            if (field.getType().equals(String.class)) {
                value = stringCleaner.cleanString(value);
            }
            sb.append(value);
            sb.append("\"");
        }


        private String serializeArray(Collection<?> array) {
            checkForCycles(array);
            StringBuilder sb = new StringBuilder();
            sb.append('[');

            if (array.isEmpty()) {
                sb.append(']');
                return sb.toString();
            }

            for (Object obj : array) {
                if (obj == null) {
                    sb.append('\"');
                    sb.append("null");
                    sb.append('\"');
                    sb.append(":\"");
                    sb.append("null");
                    sb.append("\"");
                    sb.append(',');
                    continue;
                }

                Class<?> clazz = obj.getClass();
                if (isNotSerializableType(clazz)) {
                    throw new ExportMapperException("Type " + clazz.getSimpleName() + " is not exportable");
                }

                if (converter.isPrimitiveOrWrapper(clazz) || converter.isDateTime(clazz) || clazz.isEnum()) {
                    serializePrimitiveElement(sb, obj, clazz);
                } else if (converter.isListOrSet(clazz)) {
                    String type = clazz.getName();

                    // Key + type.
                    sb.append('\"');
                    sb.append(type);
                    sb.append("\":");

                    // Value.
                    sb.append(serializeArray((Collection<?>) obj));
                } else {
                    serializeObjectElement(sb, obj, clazz);
                }
                sb.append(',');
            }

            int index;
            if ((index = sb.lastIndexOf(",")) != -1) {
                sb.deleteCharAt(index);
            }
            sb.append(']');
            colors.remove(array);

            return sb.toString();
        }

        private void serializeObjectElement(StringBuilder sb, Object obj, Class<?> clazz) {
            String type = clazz.getName();

            // Key + type.
            sb.append('\"');
            sb.append(type);
            sb.append("\":");

            // Value.
            sb.append(serializeObject(obj));
        }

        private void serializePrimitiveElement(StringBuilder sb, Object obj, Class<?> clazz) {
            String type = clazz.getName();

            // Key + type.
            sb.append('\"');
            sb.append(type);
            sb.append('\"');

            // Value.
            sb.append(":\"");

            String value = String.valueOf(obj);
            if (clazz.equals(String.class)) {
                value = stringCleaner.cleanString(value);
            }
            sb.append(value);
            sb.append("\"");
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
                    throw new ExportMapperException("Can't get access to field " +
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
