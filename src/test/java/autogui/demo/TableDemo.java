package autogui.demo;

import autogui.GuiIncluded;
import autogui.swing.AutoGuiShell;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@GuiIncluded
public class TableDemo {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new TableDemo());
    }

    @GuiIncluded public TableA tableA = new TableA();
    @GuiIncluded public TableB tableB = new TableB();
    @GuiIncluded public TableC tableC = new TableC();
    @GuiIncluded public TableD tableD = new TableD();
    @GuiIncluded public TableE tableE = new TableE();
    @GuiIncluded public TableF tableF = new TableF();
    @GuiIncluded public TableG tableG = new TableG();

    ///////////////////////////

    @GuiIncluded(description = "List<V>")
    public static class TableA {
        @GuiIncluded public List<Float> listA = Arrays.asList(10f, 20f, 30f);

        @GuiIncluded public void actionA1(List<Float> l, String name) {
            System.err.println("actionA1: " + l + " : " + name);
        }

        @GuiIncluded public void actionA2(List<Integer> l, String name) {
            System.err.println("actionA2: " + l + " : " + name);
        }

        @GuiIncluded public void actionA3(List<int[]> l, String name) {
            System.err.println("actionA3: " + l.stream().map(Arrays::toString).collect(Collectors.toList()) + " : " + name);
        }
    }

    ///////////////////////////

    @GuiIncluded(description = "List<E>, E(V,V)")
    public static class TableB {
        @GuiIncluded public List<ElemB> listB = Arrays.asList(
                new ElemB(10f), new ElemB(20f), new ElemB(30f));

        @GuiIncluded public void actionB1(List<ElemB> l, String name) {
            System.err.println("actionB1: " + l + " : " + name);
        }

        @GuiIncluded public void actionB2(List<Integer> l, String name) {
            System.err.println("actionB2: " + l + " : " + name);
        }

        @GuiIncluded public void actionB3(List<int[]> l, String name) {
            System.err.println("actionB3: " + l.stream().map(Arrays::toString).collect(Collectors.toList()) + " : " + name);
        }
    }

    @GuiIncluded
    public static class ElemB {
        @GuiIncluded public float value;
        @GuiIncluded public String str;

        public ElemB(float value) {
            this.value = value;
            str = "(" + value + ")";
        }

        @GuiIncluded public void actionElemB1() {
            System.err.println(this + ".action1");
        }

        @Override
        public String toString() {
            return "B(" + value + "," + str + ")";
        }
    }

    ///////////////////////////

    @GuiIncluded(description = "List<List<V>>")
    public static class TableC {
        @GuiIncluded public List<List<Float>> listC = Arrays.asList(
                Arrays.asList(10f, 11f, 12f),
                Arrays.asList(20f, 21f, 22f, 23f),
                Arrays.asList(30f, 31f, 32f, 23f, 24f)
        );

        @GuiIncluded public void actionC1(List<List<Float>> l, String name) {
            System.err.println("actionC1: " + l + " : " + name);
        }

        @GuiIncluded public void actionC2(List<Integer> l, String name) {
            System.err.println("actionC2: " + l + " : " + name);
        }

        @GuiIncluded public void actionC3(List<int[]> l, String name) {
            System.err.println("actionC3: " + l.stream().map(Arrays::toString).collect(Collectors.toList()) + " : " + name);
        }
    }


    ///////////////////////////

    @GuiIncluded(description = "List<List<E>>, E(V)")
    public static class TableD {
        @GuiIncluded public List<List<ElemB>> listD = Arrays.asList(
                Arrays.asList(new ElemB(10f), new ElemB(11f), new ElemB(12f), new ElemB(13f), new ElemB(14f)),
                Arrays.asList(new ElemB(20f), new ElemB(21f), new ElemB(22f), new ElemB(23f)),
                Arrays.asList(new ElemB(30f), new ElemB(31f), new ElemB(32f))
        );

        @GuiIncluded public void actionD1(List<List<ElemB>> l, String name) {
            System.err.println("actionD1: " + l + " : " + name);
        }

        @GuiIncluded public void actionD2(List<Integer> l, String name) {
            System.err.println("actionD2: " + l + " : " + name);
        }

        @GuiIncluded public void actionD3(List<int[]> l, String name) {
            System.err.println("actionD3: " + l.stream().map(Arrays::toString).collect(Collectors.toList()) + " : " + name);
        }
    }


    ///////////////////////////

    @GuiIncluded(description = "List<E>, E(V,List<V>)")
    public static class TableE {
        @GuiIncluded public List<ElemE> listE = Arrays.asList(
                new ElemE(10f, 11f, 12f, 13f, 14f),
                new ElemE(20f, 21f, 22f, 23f),
                new ElemE(30f, 31f, 32f)
        );

        @GuiIncluded public void actionE1(List<ElemE> l, String name) {
            System.err.println("actionE1: " + l + " : " + name);
        }

        @GuiIncluded public void actionE2(List<Integer> l, String name) {
            System.err.println("actionE2: " + l + " : " + name);
        }

        @GuiIncluded public void actionE3(List<int[]> l, String name) {
            System.err.println("actionE3: " + l.stream().map(Arrays::toString).collect(Collectors.toList()) + " : " + name);
        }
    }

    @GuiIncluded
    public static class ElemE {
        @GuiIncluded(index = 1) public float value;
        @GuiIncluded(index = 2) public List<Float> floatList;

        public ElemE(float value, Float... floatList) {
            this.value = value;
            this.floatList = Arrays.asList(floatList);
        }

        @Override
        public String toString() {
            return "E(" + value + "," + floatList + ")";
        }

        @GuiIncluded public void actionElemE1(List<Float> l, String name) {
            System.err.println(this + ".action1: " + l + " : " + name);
        }

        @GuiIncluded public void actionElemE2(List<Integer> l, String name) {
            System.err.println(this + ".action2: " + l + " : " + name);
        }

        @GuiIncluded public void actionElemE3(List<int[]> l, String name) {
            System.err.println(this + ".action3: " + l.stream().map(Arrays::toString).collect(Collectors.toList()) + " : " + name);
        }


        @GuiIncluded public void actionElemE4() {
            System.err.println(this + ".action4");
        }
    }

    ///////////////////////////

    @GuiIncluded(description = "List<E>, E(V,F) F(List<V>)")
    public static class TableF {
        @GuiIncluded public List<ElemF> listF = Arrays.asList(
                new ElemF(10f, 11f, 12f, 13f, 14f),
                new ElemF(20f, 21f, 22f, 23f),
                new ElemF(30f, 31f, 32f)
        );

        @GuiIncluded public void actionF1(List<ElemF> l, String name) {
            System.err.println("actionF1: " + l + " : " + name);
        }

        @GuiIncluded public void actionF2(List<Integer> l, String name) {
            System.err.println("actionF2: " + l + " : " + name);
        }

        @GuiIncluded public void actionF3(List<int[]> l, String name) {
            System.err.println("actionF3: " + l.stream().map(Arrays::toString).collect(Collectors.toList()) + " : " + name);
        }
    }

    @GuiIncluded
    public static class ElemF {
        @GuiIncluded(index = 1) public float fValue;
        @GuiIncluded(index = 2) public ElemE e;

        public ElemF(float fValue, Float... floatList) {
            this.fValue = fValue;
            List<Float> fs = Arrays.asList(floatList);
            e = new ElemE(fs.get(0),
                    fs.subList(1, fs.size()).toArray(new Float[fs.size() - 1]));
        }


        @GuiIncluded public void actionElemF1() {
            System.err.println(this + ".action1");
        }

        @GuiIncluded public void actionElemF2(List<Integer> l, String name) {
            System.err.println(this + ".action2: " + l + " : " + name);
        }

        @GuiIncluded public void actionElemF3(List<int[]> l, String name) {
            System.err.println(this + ".action3: " + l.stream().map(Arrays::toString).collect(Collectors.toList()) + " : " + name);
        }

        @Override
        public String toString() {
            return "F(" + fValue + "," + e + ")";
        }
    }

    ///////////////////////////

    @GuiIncluded(description = "List<E>, E(V,E(V))")
    public static class TableG {
        @GuiIncluded public List<ElemG> listG = Arrays.asList(
                new ElemG(10f,11f), new ElemG(20f,21f), new ElemG(30f,31f));

        @GuiIncluded public void actionG1(List<ElemG> l, String name) {
            System.err.println("actionG1: " + l + " : " + name);
        }

        @GuiIncluded public void actionG2(List<Integer> l, String name) {
            System.err.println("actionG2: " + l + " : " + name);
        }

        @GuiIncluded public void actionG3(List<int[]> l, String name) {
            System.err.println("actionG3: " + l.stream().map(Arrays::toString).collect(Collectors.toList()) + " : " + name);
        }
    }

    @GuiIncluded
    public static class ElemG {
        @GuiIncluded(index = 1) public float gValue;
        @GuiIncluded(index = 2) public ElemB b;

        public ElemG(float gValue, float b) {
            this.gValue = gValue;
            this.b = new ElemB(b);
        }

        @GuiIncluded public void actionElemG1() {
            System.err.println(this + ".action1");
        }

        @Override
        public String toString() {
            return "G(" + gValue + "," + b + ")";
        }
    }
}
