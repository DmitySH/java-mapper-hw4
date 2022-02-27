package mapper.serializers;

import mapper.annotations.DateFormat;
import mapper.annotations.Exported;
import mapper.annotations.Ignored;
import mapper.annotations.PropertyName;
import mapper.enums.NullHandling;
import mapper.exceptions.ExportMapperException;
import mapper.interfaces.Mapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

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
    }

    @Test
    void write() {
    }

    @Test
    void testWrite() {
    }

    @Test
    void testWritePrimitives() {
        OnlyPrimitives op = new OnlyPrimitives();

        op.setInteger(123);
        op.setNum(4543);
        op.setStr("fs");

        String str = serializer.writeToString(op);
        System.out.println(str);

        OnlyPrimitives des = serializer.readFromString(OnlyPrimitives.class, str);

        String str2 = serializer.writeToString(des);
        System.out.println(str2);

        assertEquals(str, str2);

    }

    @Test
    void testWriteArrays() {
        Arrays ar = new Arrays();

        ar.setList(new ArrayList<>(List.of(1, 2, 3)));
        ar.setSet(new TreeSet<>(List.of("ku", "ka", "re", "ku")));
        ar.setSet(new TreeSet<>(List.of("fsd", "f2222")));
        ar.setInnerList(
                new LinkedList<>(
                        List.of(
                                new HashSet<>(List.of(1, 2, 3)),
                                new TreeSet<>(List.of(5, 1))
                        )
                )
        );

        OnlyPrimitives op1 = new OnlyPrimitives();
        op1.setStr("st1");
        op1.setNum(123);
        OnlyPrimitives op2 = new OnlyPrimitives();


        ar.setOpList(new LinkedList<>(
                List.of(op2, op1)
        ));

        String str = serializer.writeToString(ar);
        System.out.println(str);

        String str2 = serializer.writeToString(serializer.readFromString(Arrays.class, str));
        System.out.println(str2);
        assertEquals(str, str2);
    }

    @Test
    void testWriteObjects() {
        OnlyPrimitives op = new OnlyPrimitives();

        op.setInteger(123);
        op.setNum(4543);
        op.setStr("fs");

        Arrays ar = new Arrays();


        Arrays innerAr = new Arrays();

        op.setArInAr(innerAr);

        ar.setList(new ArrayList<>(List.of(1, 2, 3)));
        ar.setSet(new TreeSet<>(List.of("ku", "ka", "re", "ku")));
        ar.setSet(new TreeSet<>(List.of("fsd", "f2222")));
        ar.setInnerList(
                new LinkedList<>(
                        List.of(
                                new HashSet<>(List.of(1, 2, 3)),
                                new TreeSet<>(List.of(5, 1))
                        )
                )
        );

        ar.setOp(op);



        ar.setTripleList(
                new LinkedList<>(
                        List.of(
                                new HashSet<>(List.of(
                                        new LinkedList<>(List.of(123, 321)),
                                        new ArrayList<>(List.of(3433, 0))
                                )),
                                new HashSet<>(List.of(
                                        new LinkedList<>(List.of(12333, 32331)),
                                        new ArrayList<>(List.of(-111))
                                ))
                        )
                )
        );

        OnlyPrimitives op1 = new OnlyPrimitives();
        op1.setStr("st1");
        op1.setNum(123);
        OnlyPrimitives op2 = new OnlyPrimitives();


        ar.setOpList(new LinkedList<>(
                List.of(op2, op1)
        ));

        ObjectsIn in = new ObjectsIn();
        in.setArra(ar);
        in.setOps(op);


        Arrays arToColOne = new Arrays();

        OnlyPrimitives innerOp = new OnlyPrimitives();
        innerOp.setNum(123);


        arToColOne.setOp(innerOp);

        TimeClass tc = new TimeClass();
        tc.setListDate(new ArrayList<>(List.of(LocalDate.now(), LocalDate.of(2002, 11, 23))));
        tc.setListTime(new LinkedList<>(List.of(LocalTime.now())));
        tc.setListDateTime(new ArrayList<>(List.of(LocalDateTime.now())));

        arToColOne.setTc(tc);

        in.setLotOfArr(
                new LinkedList<>(List.of(arToColOne, new Arrays()))
        );

        in.setEmpty(new HashSet<>());

        String str = serializer.writeToString(in);
        System.out.println(str);
        String str2 = serializer.writeToString(serializer.readFromString(ObjectsIn.class, str));
        System.out.println(str2);
        assertEquals(str, str2);
    }

    @Test
    void testDtFormat() {
        TimeClass tc = new TimeClass();

        tc.setListDate(new ArrayList<>(List.of(LocalDate.now(), LocalDate.of(2002, 11, 23))));
        tc.setListTime(new LinkedList<>(List.of(LocalTime.now())));
        tc.setListDateTime(new ArrayList<>(List.of(LocalDateTime.now())));
        String str = serializer.writeToString(tc);
        System.out.println(str);

        String str2 = serializer.writeToString(serializer.readFromString(TimeClass.class, str));
        System.out.println(str2);

        assertEquals(str, str2);


    }

    @Test
    void testCycle() {
        CycleClassHelper helper = new CycleClassHelper();
        CycleClass cycle = new CycleClass();

        helper.setCycle(cycle);
        cycle.setHelper(helper);

        assertThrows(ExportMapperException.class, ()->serializer.writeToString(cycle));

        PrimitiveCycle primitiveCycle = new PrimitiveCycle();
        primitiveCycle.setCycle(primitiveCycle);

        assertThrows(ExportMapperException.class, ()->serializer.writeToString(primitiveCycle));
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
class SameNameClass {
    public SameNameClass() {
    }

    String str = "stroka";
    @PropertyName("str")
    int intField = 5;
}


@Exported
class CycleClass{
    public CycleClass() {
    }

    private CycleClassHelper helper;

    public void setHelper(CycleClassHelper helper) {
        this.helper = helper;
    }
}

@Exported
class CycleClassHelper{
    public CycleClassHelper() {
    }

    private CycleClass cycle;

    public void setCycle(CycleClass cycle) {
        this.cycle = cycle;
    }
}

@Exported
class PrimitiveCycle{
    public PrimitiveCycle() {
    }

    private PrimitiveCycle cycle;

    public void setCycle(PrimitiveCycle cycle) {
        this.cycle = cycle;
    }
}

@Exported
class TimeClass {
    public TimeClass() {
    }

    LocalDate lDate = LocalDate.now();
    LocalTime lTime = LocalTime.now();
    LocalDateTime ldT = LocalDateTime.now();

    @DateFormat("dd/MM/yyyy")
    LocalDate lDateFormatted = LocalDate.now();
    @DateFormat("hh:mm:ss a")
    LocalTime lTimeFormatted = LocalTime.now();
    @DateFormat("yyyy/MM/dd HH-mm")
    LocalDateTime ldTFormatted = LocalDateTime.now();

    List<LocalDate> listDate;
    List<LocalTime> listTime;

    public void setListDate(List<LocalDate> listDate) {
        this.listDate = listDate;
    }

    public void setListTime(List<LocalTime> listTime) {
        this.listTime = listTime;
    }

    public void setListDateTime(List<LocalDateTime> listDateTime) {
        this.listDateTime = listDateTime;
    }

    List<LocalDateTime> listDateTime;
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

@Exported
class OnlyPrimitives {
    public OnlyPrimitives() {

    }
    Arrays arInAr;

    public void setArInAr(Arrays arInAr) {
        this.arInAr = arInAr;
    }

    @PropertyName("chislo")
    private Integer integer;
    public String str;

    private int num;

    public void setInteger(Integer integer) {
        this.integer = integer;
    }

    public void setStr(String str) {
        this.str = str;
    }

    public void setNum(int num) {
        this.num = num;
    }
}

@Exported
class Arrays {
    public Arrays() {

    }

    private List<Integer> list;
    private Set<String> set;

    OnlyPrimitives op;


    TimeClass tc;

    public void setTc(TimeClass tc) {
        this.tc = tc;
    }

    public void setOp(OnlyPrimitives op) {
        this.op = op;
    }



    public void setOpList(List<OnlyPrimitives> opList) {
        this.opList = opList;
    }

    private List<OnlyPrimitives> opList;

    public void setList(List<Integer> list) {
        this.list = list;
    }

    public void setSet(Set<String> set) {
        this.set = set;
    }

    private List<Set<Integer>> innerList;

    public void setTripleList(List<Set<List<Integer>>> tripleList) {
        this.tripleList = tripleList;
    }

    private List<Set<List<Integer>>> tripleList;


    public void setInnerList(List<Set<Integer>> innerList) {
        this.innerList = innerList;
    }
}

@Exported
class ObjectsIn {
    public ObjectsIn() {
    }

    Arrays arra;
    OnlyPrimitives ops;

    public void setArra(Arrays arra) {
        this.arra = arra;
    }

    public void setOps(OnlyPrimitives ops) {
        this.ops = ops;
    }

    public void setLotOfArr(List<Arrays> lotOfArr) {
        this.lotOfArr = lotOfArr;
    }

    public void setEmpty(HashSet<Integer> empty) {
        this.empty = empty;
    }

    HashSet<Integer> empty;

    List<Arrays> lotOfArr;
}
