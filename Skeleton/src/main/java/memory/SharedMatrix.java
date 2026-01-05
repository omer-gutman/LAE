package memory;

public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    public SharedMatrix() {
        this.vectors = new SharedVector[0];
    }

    public SharedMatrix(double[][] matrix) {
        loadRowMajor(matrix);
    }

    public void loadRowMajor(double[][] matrix) {
        if(matrix.length == 0) {
            this.vectors = new SharedVector[0];
            return;
        }
        this.vectors = new SharedVector[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            this.vectors[i] = new SharedVector(matrix[i], VectorOrientation.ROW_MAJOR);
        }
    }

    public void loadColumnMajor(double[][] matrix) {
        if(matrix.length == 0) {
            this.vectors = new SharedVector[0];
            return;
        }
        this.vectors = new SharedVector[matrix[0].length];
        for(int i = 0; i < matrix[0].length; i++) {
            double[] col = new double[matrix.length];
            for(int j = 0; j < matrix.length; j++) {
                col[j] = matrix[j][i];
            }
            this.vectors[i] = new SharedVector(col, VectorOrientation.COLUMN_MAJOR);
        }
    }

    public double[][] readRowMajor() {
        if(this.vectors.length == 0) {
            return new double[0][0];
        }
        if (this.vectors[0].getOrientation() == VectorOrientation.ROW_MAJOR) {    
            double[][] result = new double[this.vectors.length][this.vectors[0].length()];
            for (int i = 0; i < this.vectors.length; i++) {
                for (int j = 0; j < this.vectors[0].length(); j++) {
                    result[i][j] = this.vectors[i].get(j);
                }
            }
            return result;
        }
        else {
            double[][] result = new double[this.vectors[0].length()][this.vectors.length];
            for (int i = 0; i < this.vectors[0].length(); i++) {
                for (int j = 0; j < this.vectors.length; j++) {
                    result[i][j] = this.vectors[j].get(i);
                }  
            }
            return result;
        } 
    }

    public SharedVector get(int index) {
        return this.vectors[index];
    }

    public int length() {
        return this.vectors.length;
    }

    public VectorOrientation getOrientation() {
        return this.vectors[0].getOrientation();
    }

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        for(SharedVector vec : vecs) {
            vec.readLock();
        }
    }

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        for(SharedVector vec : vecs) {
            vec.readUnlock();
        }
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        for(SharedVector vec : vecs) {
            vec.writeLock();
        }
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        for(SharedVector vec : vecs) {
            vec.writeUnlock();
        }
    }
}
