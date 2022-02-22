package mapper.serializers;

import mapper.annotations.DateFormat;
import mapper.annotations.Exported;
import mapper.annotations.Ignored;
import mapper.annotations.PropertyName;
import mapper.enums.NullHandling;
import mapper.exceptions.ExportMapperException;
import mapper.interfaces.Mapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    void readWriteOutputStream() {
        GoodClass obj = new GoodClass();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertDoesNotThrow(() -> serializer.write(obj, out));

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        assertDoesNotThrow(() -> serializer.read(GoodClass.class, in));

    }

    @Test
    void readWriteFile() {
        GoodClass obj = new GoodClass();
        assertDoesNotThrow(() -> serializer.write(obj, new File("src/test/files/test1")));
        assertDoesNotThrow(() -> serializer.read(GoodClass.class, new File("src/test/files/test1")));
    }

    @Test
    void readString() {
        ReviewComment comment = new ReviewComment();
        comment.setComment("KlakClik");
        String str = assertDoesNotThrow(() -> serializer.writeToString(comment));
        System.out.println(str);
//        GoodClass obj = new GoodClass();
//        str = assertDoesNotThrow(()->serializer.writeToString(obj));

        ReviewComment com = serializer.readFromString(ReviewComment.class, str);
        System.out.println(serializer.writeToString(com));
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

        System.out.println(assertDoesNotThrow(() ->
                serializer.writeToString(new GoodClass())));

        System.out.println(assertDoesNotThrow(() ->
                serializer.writeToString(new EmptyClass())));

        assertThrows(ExportMapperException.class,
                () -> serializer.writeToString(new SameNameClass()));

        ReviewComment reviewComment = new ReviewComment();
        reviewComment.setComment("Одни static'и в работе");
        reviewComment.setResolved(false);
        reviewComment.setAuthor("Проверяющий #1");

        assertEquals(assertDoesNotThrow(() ->
                        serializer.writeToString(reviewComment)),
                "{\"comment\":\"Одни static'и в работе\",\"resolved\":\"false\"}");

        assertEquals(assertDoesNotThrow(() ->
                serializer.writeToString(new ExcludeNullClass())), "{}");
        assertEquals(assertDoesNotThrow(() ->
                serializer.writeToString(new IncludeNullClass())), "{\"integer\":\"null\"}");


    }

    @Test
    void write() {
    }

    @Test
    void testWrite() {
    }
}


class NotExportedClass {
    public NotExportedClass() {
    }
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
class EmptyClass {
    public EmptyClass() {
    }
}

@Exported
class GoodClass {
    static int statica = 5;

    public GoodClass() {
    }


    String str = "stroka";
    int intField = 5;
    Integer k = 123;

    Set<Integer> ints = new HashSet<>(List.of(1, 3, 2));

    @PropertyName("List")
    List<List<Integer>> lllist = new ArrayList<>(
            List.of(new ArrayList<>(List.of(1, 2, 3)),
                    new ArrayList<>(List.of(1, 2, 3))));
    List<Integer> ls = new ArrayList<>(List.of(1, 2, 3));
}

@Exported
class SmallGoodClass {

    public SmallGoodClass() {
    }


    String str = "st";
    int intField = 123;
}

@Exported
class SameNameClass {
    public SameNameClass() {
    }

    String str = "stroka";
    @PropertyName("str")
    int intField = 5;
}

@Exported
class ReviewComment {
    public ReviewComment() {
    }

    private final NullHandling enu = NullHandling.EXCLUDE;

    private String comment;
    @Ignored
    private String author;
    private boolean resolved;
    Boolean annBul = true;

    LocalDate lDate = LocalDate.now();
    @DateFormat("hh:mm:ss a")
    LocalTime lTime = LocalTime.now();
    LocalDateTime ldT = LocalDateTime.now();

    @PropertyName("List")
    List<List<Integer>> lllist = new ArrayList<>(
            List.of(new ArrayList<>(List.of(1, 2, 3)),
                    new ArrayList<>(List.of(5, 2, 2))));

    List<List<SmallGoodClass>> coms = new ArrayList<>(
            List.of(
                    new ArrayList<>(List.of(
                            new SmallGoodClass(), new SmallGoodClass())),
            new ArrayList<>(List.of(new SmallGoodClass()))
            ));
    @PropertyName("MegByte")
    Byte b = 43;

    char ch = 'q';
    Character wCharr = 'W';

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }
}

@Exported(nullHandling = NullHandling.EXCLUDE)
class ExcludeNullClass {
    public ExcludeNullClass() {
    }

    private final Integer integer = null;
}

@Exported(nullHandling = NullHandling.INCLUDE)
class IncludeNullClass {
    public IncludeNullClass() {
    }

    private final Integer integer = null;
}