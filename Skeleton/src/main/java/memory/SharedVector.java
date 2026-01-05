package memory;

import java.util.concurrent.locks.ReadWriteLock;

public class SharedVector {

    private double[] vector;
    private VectorOrientation orientation;
    private ReadWriteLock lock = new java.util.concurrent.locks.ReentrantReadWriteLock();

    public SharedVector(double[] vector, VectorOrientation orientation) {
        this.vector = vector;
        this.orientation = orientation;
    }

    public double get(int index) {
        return this.vector[index];
    }

    public int length() {
        return this.vector.length;
    }

    public VectorOrientation getOrientation() {
        return this.orientation;
    }

    public void writeLock() {
        lock.writeLock().lock();
    }

    public void writeUnlock() {
        lock.writeLock().unlock();
    }

    public void readLock() {
        lock.readLock().lock();
    }

    public void readUnlock() {
        lock.readLock().unlock();
    }

    public void transpose() {
        if (this.orientation == VectorOrientation.ROW_MAJOR) {
            this.orientation = VectorOrientation.COLUMN_MAJOR;
        } else {
            this.orientation = VectorOrientation.ROW_MAJOR;
        }
    }

    public void add(SharedVector other) {
        if (this.vector.length != other.length()) {
            throw new IllegalArgumentException("Vectors must be of the same length for addition.");
        }

        for (int i = 0; i < this.vector.length; i++) {
            this.vector[i] += other.get(i);
        }
    }

    public void negate() {
        for (int i = 0; i < this.vector.length; i++) {
            this.vector[i] = -this.vector[i];
        }
    }

    public double dot(SharedVector other) {
        // TODO: compute dot product (row · column)
        //יש, לא נעול עדיין
        if (this.vector.length != other.length()) {
            throw new IllegalArgumentException("Vectors must be of the same length for dot product.");
        }
        if (this.orientation != VectorOrientation.ROW_MAJOR || other.getOrientation() != VectorOrientation.COLUMN_MAJOR) {
            throw new IllegalArgumentException("First vector must be row-major and second vector must be column-major for dot product.");
        }
        double result = 0;
        for (int i = 0; i < this.vector.length; i++) {
            result += this.vector[i] * other.get(i);
        }
        return result;
    }

    public void vecMatMul(SharedMatrix matrix) {
        // TODO: compute row-vector × matrix
        //יש, לא נעול עדיין
        if (this.orientation != VectorOrientation.ROW_MAJOR) {
            throw new IllegalArgumentException("Vector must be row-major for vector-matrix multiplication.");
        }
        if (matrix.length() > 0 && this.length() != matrix.get(0).length()) {
            throw new IllegalArgumentException("Vector length must match number of rows in matrix for multiplication.");
        }
        if (matrix.getOrientation() != VectorOrientation.COLUMN_MAJOR) {
            throw new IllegalArgumentException("Matrix must be column-major for vector-matrix multiplication.");
        }
        double[] result = new double[matrix.length()];
        for(int i = 0; i < matrix.length(); i++) {
            result[i] = dot(matrix.get(i));
        }
        this.vector = result;
    }
}
