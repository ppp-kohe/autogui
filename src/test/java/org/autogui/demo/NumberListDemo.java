package org.autogui.demo;

import org.autogui.GuiIncluded;
import org.autogui.GuiListSelectionUpdater;
import org.autogui.swing.AutoGuiShell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@GuiIncluded
public class NumberListDemo {
    public static void main(String[] args) {
        AutoGuiShell.get().showWindow(new NumberListDemo());
    }

    @GuiIncluded(index = 1)
    public Integers integers = new Integers();

    @GuiIncluded(index = 2)
    public Floats floats = new Floats();

    @GuiIncluded(index = 3)
    public Matrix matrix = new Matrix();

    @GuiIncluded(index = 4)
    public MatrixPrimitive matrixPrimitive = new MatrixPrimitive();

    @GuiIncluded
    public class Integers {
        @GuiIncluded(index = 1)
        public int start = 0;
        @GuiIncluded(index = 2)
        public int stride = 1;
        @GuiIncluded(index = 3)
        public int size = 10;


        @GuiIncluded
        public List<Integer> integers = new ArrayList<>();


        @GuiIncluded
        public void add() {
            for (int i = 0 ; i < size; ++i) {
                integers.add(start);
                start += stride;
            }
            integers = new ArrayList<>(integers);
        }

        @GuiIncluded
        public void show(List<Integer> selectedIndexes) {
            StringBuilder buf = new StringBuilder();
            for (int i = 0, l = selectedIndexes.size(); i < l; ++i) {
                int idx = selectedIndexes.get(i);
                int n = integers.get(idx);
                buf.append(" [").append(idx).append("]: ").append(n);
                if (i > 0 && i % 10 == 0) {
                    System.out.println(buf.toString());
                    buf = new StringBuilder();
                }
            }
            if (buf.length() > 0) {
                System.out.println(buf);
            }
        }

        @GuiIncluded
        public void delete(List<Integer> selectedIndexes) {
            integers = IntStream.range(0, integers.size())
                    .filter(i -> !selectedIndexes.contains(i))
                    .mapToObj(integers::get)
                    .collect(Collectors.toList());
        }

        @GuiListSelectionUpdater(index = true)
        @GuiIncluded
        public List<Integer> next(List<Integer> selectedIndexes) {
            System.err.println("next : " + selectedIndexes);
            return selectedIndexes.stream()
                    .map(i -> i + 1 >= integers.size() ? 0 : i + 1)
                    .collect(Collectors.toList());
        }
    }

    @GuiIncluded
    public class Floats {
        @GuiIncluded(index = 1)
        public double start = 0;
        @GuiIncluded(index = 2)
        public double stride = 1;
        @GuiIncluded(index = 3)
        public double size = 10;

        @GuiIncluded
        public List<Float> floats = new ArrayList<>();

        @GuiIncluded
        public List<Double> doubles = new ArrayList<>();


        @GuiIncluded
        public void addFloats() {
            for (int i = 0 ; i < size; ++i) {
                floats.add((float) start);
                start += stride;
            }
            floats = new ArrayList<>(floats);
        }

        @GuiIncluded
        public void addDoubles() {
            for (int i = 0 ; i < size; ++i) {
                doubles.add(start);
                start += stride;
            }
            doubles = new ArrayList<>(doubles);
        }

        @GuiIncluded
        public void show(List<Integer> selectedIndexes, String propName) {
            List<?> data;
            if (propName.equals("floats")) {
                data = floats;
            } else {
                data = doubles;
            }
            System.out.println("[" + propName + "]:");
            StringBuilder buf = new StringBuilder();
            for (int i = 0, l = selectedIndexes.size(); i < l; ++i) {
                int idx = selectedIndexes.get(i);
                Object n = data.get(idx);
                buf.append(" [").append(idx).append("]: ").append(n);
                if (i > 0 && i % 10 == 0) {
                    System.out.println(buf.toString());
                    buf = new StringBuilder();
                }
            }
            if (buf.length() > 0) {
                System.out.println(buf);
            }
        }

        @GuiIncluded
        public void delete(List<Integer> selectedIndexes, String propName) {
            List<?> data;
            if (propName.equals("floats")) {
                floats = IntStream.range(0, floats.size())
                        .filter(i -> !selectedIndexes.contains(i))
                        .mapToObj(floats::get)
                        .collect(Collectors.toList());
            } else {
                doubles = IntStream.range(0, doubles.size())
                        .filter(i -> !selectedIndexes.contains(i))
                        .mapToObj(doubles::get)
                        .collect(Collectors.toList());
            }
        }

        @GuiListSelectionUpdater(index = true)
        @GuiIncluded
        public List<Integer> next(List<Integer> selectedIndexes, String propName) {
            System.err.println("next : " + selectedIndexes + " " + propName);
            return selectedIndexes.stream()
                    .map(i -> i + 1 >= (propName.equals("floats") ? floats : doubles).size() ? 0 : i + 1)
                    .collect(Collectors.toList());
        }
    }

    @GuiIncluded
    public static class Matrix {
        @GuiIncluded(index = 1)
        public double start = 0;
        @GuiIncluded(index = 2)
        public double stride = 1;
        @GuiIncluded(index = 3)
        public int width = 10;
        @GuiIncluded(index = 4)
        public int height = 10;

        @GuiIncluded
        public List<List<Float>> matrix = new ArrayList<>();

        @GuiIncluded
        public void add() {
            for (int i = 0; i < height; ++i) {
                List<Float> row = new ArrayList<>();
                for (int j = 0; j < width; ++j) {
                    row.add((float) start);
                    start += stride;
                }
                matrix.add(row);
            }
            matrix = new ArrayList<>(matrix);
        }

        @GuiIncluded
        public void show(List<int[]> indexes) {
            System.err.println("---------------- "+ indexes.size());
            for (int[] idx : indexes) {
                System.err.print(Arrays.toString(idx) + " : ");
                if (idx.length >= 2) {
                    try {
                        System.err.println(matrix.get(idx[0]).get(idx[1]));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    System.err.println();
                }
            }
        }
    }

    @GuiIncluded
    public static class MatrixPrimitive {
        @GuiIncluded(index = 1)
        public double start = 0;
        @GuiIncluded(index = 2)
        public double stride = 1;
        @GuiIncluded(index = 3)
        public int width = 10;
        @GuiIncluded(index = 4)
        public int height = 10;

        @GuiIncluded
        public float[][] matrix = {};

        @GuiIncluded
        public void set() {
            matrix = new float[height][width];
            for (int i = 0; i < height; ++i) {
                float[] row = matrix[i];
                for (int j = 0; j < width; ++j) {
                    row[j] = ((float) start);
                    start += stride;
                }
            }
        }

        @GuiIncluded
        public void show(List<int[]> indexes) {
            System.err.println("---------------- "+ indexes.size());
            for (int[] idx : indexes) {
                System.err.print(Arrays.toString(idx) + " : ");
                if (idx.length > 1) {
                    try {
                        System.err.println(matrix[idx[0]][idx[1]]);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {
                    System.err.println();
                }
            }
        }
    }
}
