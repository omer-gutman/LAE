package memory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SharedMemoryTest {

    @Test
    public void testVectorMatrixMultiplication() {
        // 1. Setup Vector: [1, 2]
        double[] vData = {1.0, 2.0};
        SharedVector v = new SharedVector(vData, VectorOrientation.ROW_MAJOR);

        // 2. Setup Matrix: 
        // [1, 2]
        // [3, 4]
        // In Column Major: Col 0 is [1, 3], Col 1 is [2, 4]
        double[][] matData = {
            {1.0, 2.0},
            {3.0, 4.0}
        };
        SharedMatrix m = new SharedMatrix();
        m.loadColumnMajor(matData);

        // 3. Perform Multiplication: [1, 2] * [[1, 2], [3, 4]]
        // Calculation logic:
        // Result[0] = dot([1,2], Col0[1,3]) = 1*1 + 2*3 = 7
        // Result[1] = dot([1,2], Col1[2,4]) = 1*2 + 2*4 = 10
        v.vecMatMul(m);

        // 4. Verify
        assertEquals(7.0, v.get(0), 0.001, "First element calculation wrong");
        assertEquals(10.0, v.get(1), 0.001, "Second element calculation wrong");
        System.out.println("Math Test Passed!");
    }
}