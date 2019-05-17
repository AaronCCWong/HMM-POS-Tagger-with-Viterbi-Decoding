package viterbi;

public class Matrix<T> {

    private T[][] matrix;

    public Matrix(Integer rows, Integer cols) {
        matrix = (T[][])new Object[rows][cols];
        for (int i = 0; i < rows; i++) {
            matrix[i] = (T[])new Object[cols];
        }
    }

    public T get(Integer row, Integer col) {
        return matrix[row][col];
    }

    public void set(Integer row, Integer col, T val) {
        matrix[row][col] = val;
    }

}
