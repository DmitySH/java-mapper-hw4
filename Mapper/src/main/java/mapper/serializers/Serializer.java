package mapper.serializers;

import jdk.jshell.spi.ExecutionControl;
import mapper.interfaces.Mapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(Object object, OutputStream outputStream) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(Object object, File file) throws IOException {
        throw new UnsupportedOperationException();
    }
}
