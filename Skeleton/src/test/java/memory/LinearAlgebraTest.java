package spl.lae;

import memory.SharedMatrix;
import memory.SharedVector;
import memory.VectorOrientation;

// להבנתי זה הframeework הסטנדטי לבדיקות יוניט 
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LinearAlgebraTest {

    // נבדוק את הפעולות הבסיסיות על SharedVector וSharedMatrix רגיל... בלי מקבליות עדיין

    @Test
    public void testMatrixAddition() {
        // [1, 2] + [3, 4] = [4, 6]
        double[] rowA = {1.0, 2.0};
        double[] rowB = {3.0, 4.0};

        SharedVector vA = new SharedVector(rowA, VectorOrientation.ROW_MAJOR);
        SharedVector vB = new SharedVector(rowB, VectorOrientation.ROW_MAJOR);

        // נעשה חיבור (vA = vA + vB)
        vA.add(vB);

        assertEquals(4.0, vA.get(0), 0.001); // 1 + 3 = 4
        assertEquals(6.0, vA.get(1), 0.001); // 2 + 4 = 6
    }

    @Test
    public void testTransposeLogic() {
        // [1, 2, 3]
        double[] data = {1.0, 2.0, 3.0};
        SharedVector v = new SharedVector(data, VectorOrientation.ROW_MAJOR);

        // נעשה טרנספוזיציה... בדיקה לוגית בלבד
        v.transpose();

        // הכיוון אמור להשתנות לCOLUMN_MAJOR
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
        
        // שוב נעשה טרנספוזיציה
        v.transpose();
        
        // ועכשיו הכיוון אמור לחזור להיות ROW_MAJOR
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
    }

    @Test
    public void testNegation() {
        // [1, -2]
        double[] data = {1.0, -2.0};
        SharedVector v = new SharedVector(data, VectorOrientation.ROW_MAJOR);

        // פעולת השלילה
        v.negate();

        // נבדוק שהתוצאה היא [-1, 2]
        // כו*** של העברית הזא ת... אבל הבנתם
        assertEquals(-1.0, v.get(0), 0.001);
        assertEquals(2.0, v.get(1), 0.001);
    }

    // אנחנו מהסתמכים מלא על הshared matrix והshared vector... אבל בואו נבדוק טעינה שלהם

    @Test
    public void testSharedMatrixLoading() {
        double[][] raw = {
            {1.0, 2.0},
            {3.0, 4.0}
        };
        SharedMatrix m = new SharedMatrix();
        
        // נבדוק טעינת Row Major
        m.loadRowMajor(raw);
        assertEquals(2, m.length()); // אמור להיות 2 שורות
        assertEquals(VectorOrientation.ROW_MAJOR, m.getOrientation());
        assertEquals(1.0, m.get(0).get(0)); // שורה ראשונה, עמודה ראשונה

        // נבדוק טעינת Column Major
        m.loadColumnMajor(raw);
        assertEquals(2, m.length()); // 2 עמודות
        assertEquals(VectorOrientation.COLUMN_MAJOR, m.getOrientation());
        assertEquals(1.0, m.get(0).get(0));  // שורה ראשונה, עמודה ראשונה
        assertEquals(3.0, m.get(0).get(1));
    }

    // נבדוק ניהול שגיאות בסיסי

    @Test
    public void testDotProductDimensionMismatch() {
        // ננסה לבצע dot product של וקטורים באורך שונה
        double[] d1 = {1.0, 2.0};
        double[] d2 = {1.0, 2.0, 3.0};
        
        SharedVector v1 = new SharedVector(d1, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(d2, VectorOrientation.COLUMN_MAJOR);

        // אמור לזרוק IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            v1.dot(v2);
        }, "Should throw exception for different lengths");
    }
}