package memory;

public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    public SharedMatrix() {
        // TODO: initialize empty matrix
        this.vectors = new SharedVector[0];
    }

    public SharedMatrix(double[][] matrix) {
        // TODO: construct matrix as row-major SharedVectors
        loadRowMajor(matrix);
    }

    public void loadRowMajor(double[][] matrix) {
        // TODO: replace internal data with new row-major matrix
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
        // TODO: replace internal data with new column-major matrix
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
        // TODO: return matrix contents as a row-major double[][]
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
        // TODO: return vector at index
        return this.vectors[index];
    }

    public int length() {
        // TODO: return number of stored vectors
        return this.vectors.length;
    }

    public VectorOrientation getOrientation() {
        // TODO: return orientation
        return this.vectors[0].getOrientation();
    }

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: acquire read lock for each vector
        for(SharedVector vec : vecs) {
            vec.readLock();
        }
    }

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: release read locks
        for(SharedVector vec : vecs) {
            vec.readUnlock();
        }
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: acquire write lock for each vector
        for(SharedVector vec : vecs) {
            vec.writeLock();
        }
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: release write locks
        for(SharedVector vec : vecs) {
            vec.writeUnlock();
        }
    }
}
