package mapper.serializers;

import mapper.annotations.Exported;
import mapper.exceptions.ExportMapperException;
import mapper.interfaces.Mapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SerializerTest {
    private Mapper serializer;

    @BeforeEach
    void init() {
        serializer = new Serializer();
    }

    @Test
    void readFromString() {
    }

    @Test
    void read() {
    }

    @Test
    void testRead() {
    }

    @Test
    void writeToString() {
        assertThrows(ExportMapperException.class,
                () -> serializer.writeToString(null));
        assertThrows(ExportMapperException.class,
                () -> serializer.writeToString(new NotExportedClass()));
        assertThrows(ExportMapperException.class,
                () -> serializer.writeToString(new NoEmptyCtorClass(5)));
        assertThrows(ExportMapperException.class,
                () -> serializer.writeToString(new SubClass()));
        assertDoesNotThrow(() -> serializer.writeToString(new GoodClass()));


    }

    @Test
    void write() {
    }

    @Test
    void testWrite() {
    }
}


class NotExportedClass {
}

@Exported
class NoEmptyCtorClass {
    public NoEmptyCtorClass(int x) {
    }
}

class Base {

}

@Exported
class SubClass extends Base {
    public SubClass() {
    }
}

@Exported
class GoodClass {
    public GoodClass() {
    }
}