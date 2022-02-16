package mapper.serializers;

import mapper.annotations.Exported;
import mapper.exceptions.ExportMapperException;
import mapper.interfaces.Mapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class Serializer implements Mapper {
    @Override
    public <T> T readFromString(Class<T> clazz, String input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T read(Class<T> clazz, InputStream inputStream) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T read(Class<T> clazz, File file) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String writeToString(Object object) {
        checkExportation(object);

        Class<?> objectClass = object.getClass();


        return null;
    }

    @Override
    public void write(Object object, OutputStream outputStream) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(Object object, File file) throws IOException {
        throw new UnsupportedOperationException();
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
}
