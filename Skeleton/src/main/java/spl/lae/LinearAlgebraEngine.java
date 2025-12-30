package spl.lae;

import parser.*;
import memory.*;
import scheduling.*;

import java.util.List;
import java.util.ArrayList;

public class LinearAlgebraEngine {

    private SharedMatrix leftMatrix = new SharedMatrix();
    private SharedMatrix rightMatrix = new SharedMatrix();
    private TiredExecutor executor;

    public LinearAlgebraEngine(int numThreads) {
        // יוצר את המבצע עם מספר העובדים הנתון
        this.executor = new TiredExecutor(numThreads);
    }

    public ComputationNode run(ComputationNode computationRoot) {
        // 1. בודק אם השורש הוא כבר מטריצה (עלה)
        // אם כן, אין מה לחשב... א נחזיר את השורש כפי שהוא
        if (computationRoot.getNodeType() == ComputationNodeType.MATRIX) {
            return computationRoot;
        }

        // 2. לופ עד שכל העץ ״נפתר״
        while (true) {
            // (בעצם צומת שילדיו הם מטריצות) נמצא את הצומת הבאה הניתנת לפתרון
            // (צומת זה pandas ועלים זה מטריצות)
            ComputationNode node = computationRoot.findResolvable();

            // אם לא נמצא צומת כזה, יש לבדוק אם השורש נפתר
            if (node == null) {
                // לבדוק אם השורש נפתר בזמן הזה
                if (computationRoot.getNodeType() == ComputationNodeType.MATRIX) {
                    return computationRoot;
                }
                throw new IllegalStateException("Computation tree is stuck; no resolvable nodes found.");
            }

            // 3. מצוין ניתן לפתור את הצומת
            // נטען את המטריצות הדרושות ונחשב את התוצאה
            loadAndCompute(node);

            // 4. לבדוק אם הצומת שהפתרנו הוא השורש
            if (node == computationRoot) {
                return node;
            }
        }
    }

    public void loadAndCompute(ComputationNode node) {
        // מכין את המטריצות ל m1 וm2 כדי לבצע את החישוב

        // מקבל את הילדים וסוג הצומת
        // בעצם הטייפ זה הפעולה המתמטית והילדים הם האופרנדים
        List<ComputationNode> children = node.getChildren();
        ComputationNodeType type = node.getNodeType();

        // 1. טוען את המטריצות לאזור זכרון משותף לעובדים, ההייפ
        // נבדוק אם זה משימה במקום כמו טרנפוז או שלילי, כאן לא צריך עוד מטריצה...
        if (type == ComputationNodeType.NEGATE || type == ComputationNodeType.TRANSPOSE) {
            // נבדוק אם צריך לטעון מטריצה אחת בלבד
            if (children.size() != 1) {
                throw new IllegalArgumentException("Unary operation requires exactly one operand.");
            }
            // המטריצה היחידה נטענת ב Row-Major
            leftMatrix.loadRowMajor(children.get(0).getMatrix());
        } 

        // עכשיו נבדוק אם זה פעולה בינארית
        else if (type == ComputationNodeType.ADD || type == ComputationNodeType.MULTIPLY) {
            // חייב שנתי ילדים לפעולה בינארית...
            if (children.size() != 2) {
                throw new IllegalArgumentException("Binary operation requires exactly two operands.");
            }
            
            if (type == ComputationNodeType.ADD) {
                // נטען את שתי המטריצות ב Row-Major לשם חיבור שורה שורה
                leftMatrix.loadRowMajor(children.get(0).getMatrix());
                rightMatrix.loadRowMajor(children.get(1).getMatrix());
            } 
            else { 
                // למכפלה נטען את השמאלית ב Row-Major ואת הימנית ב Column-Major
                leftMatrix.loadRowMajor(children.get(0).getMatrix());
                rightMatrix.loadColumnMajor(children.get(1).getMatrix());
            }
        }

        // 2. עכשיו ניצור את המשימות המתאימות לפי סוג הצומת
        // כל משימה תתבצע על ידי העובדים ב TiredExecutor
        List<Runnable> tasks = null;
        switch (type) {
            case ADD:
                tasks = createAddTasks();
                break;
            case MULTIPLY:
                tasks = createMultiplyTasks();
                break;
            case NEGATE:
                tasks = createNegateTasks();
                break;
            case TRANSPOSE:
                tasks = createTransposeTasks();
                break;
            default:
                throw new IllegalStateException("Unknown operation type: " + type);
        }

        // 3. הגשת כל המשימות למבצע
        // ככה שהעובדים יבצעו את החישוב ואנחנו נחכה שיסיימו
        executor.submitAll(tasks);

        // 4. משוך את התוצאה מ m1 וחזור לצומת
        // (התוצאה תמיד מאוחסנת m1 לאחר החישוב)
        // מחזירים את המטריצה לפורמט דו מימדי רגיל
        double[][] result = leftMatrix.readRowMajor();
        node.resolve(result);
    }

    public List<Runnable> createAddTasks() {
        // 1. נוודא ש המטריצות מתאימות לחיבור (באותו גודל)
        if (leftMatrix.length() != rightMatrix.length() || 
            (leftMatrix.length() > 0 && leftMatrix.get(0).length() != rightMatrix.get(0).length())) {
            //נו ברור...
            throw new IllegalArgumentException("Illegal operation: dimensions mismatch");
        }

        List<Runnable> tasks = new ArrayList<>();

        // 2. משימה לכל שורה במטריצה
        for (int i = 0; i < leftMatrix.length(); i++) {
            final int rowIdx = i;
            
            // נגיד את המשימה שמוסיפה שורה שורה
            tasks.add(() -> {
                // לקבל את הכתובת של השורות המתאימות
                SharedVector v1 = leftMatrix.get(rowIdx);  //היעד (m1)
                SharedVector v2 = rightMatrix.get(rowIdx); // המקור (m2)

                // 3. אסטרטגיית נעילה למניעת דדלוקים
                // בגלל שאנחנו כותבים ל v1 וקוראים מ v2
                // ננעל את v1 לכתיבה קודם ואז את v2 לקריאה
                v1.writeLock();
                v2.readLock();
                
                try {
                    // 4.סוף סוף מבצעים את החיבור
                    // מוסיפים את v2 ל v1
                    v1.add(v2);
                } finally {
                    // 5. נשחרר את הנעילות בסדר ההפוך
                    v2.readUnlock();
                    v1.writeUnlock();
                }
            });
        }
        
        return tasks;
    }

    public List<Runnable> createMultiplyTasks() {
        // 1. נוודא שהמטריצות מתאימות למכפלה (עמודות של שמאלית = שורות של ימנית)
        int vecLen1 = leftMatrix.length() > 0 ? leftMatrix.get(0).length() : 0;
        int vecLen2 = rightMatrix.length() > 0 ? rightMatrix.get(0).length() : 0;

        if (vecLen1 != vecLen2) {
            // נו ברור...
             throw new IllegalArgumentException("Illegal operation: dimensions mismatch");
        }

        List<Runnable> tasks = new ArrayList<>();

        // 2. משימה לכל שורה במטריצה השמאלית
        for (int i = 0; i < leftMatrix.length(); i++) {
            final int rowIdx = i;
            
            tasks.add(() -> {
                SharedVector v1 = leftMatrix.get(rowIdx); // השורה שאנחנו מעדכנים (מכפלה שורה-עמודה)
                
                // 3. אסטרטגיית נעילה
                // צריכים לנעול את v1 לכתיבה
                v1.writeLock();
                
                // ואז את כל העמודות של המטריצה הימנית לקריאה
                for (int k = 0; k < rightMatrix.length(); k++) {
                    rightMatrix.get(k).readLock();
                }
                
                try {
                    // 4. המכפלה עצמה...
                    // v1 = v1 * m2
                    // שימוש במתודה המיוחדת למכפלה
                    v1.vecMatMul(rightMatrix); 
                } finally {
                    // 5. נשחרר את הנעילות בסדר ההפוך
                    for (int k = 0; k < rightMatrix.length(); k++) {
                        rightMatrix.get(k).readUnlock();
                    }
                    // ואז את v1
                    v1.writeUnlock();
                }
            });
        }
        return tasks;
    }

    public List<Runnable> createNegateTasks() {
        List<Runnable> tasks = new ArrayList<>();
        
        // 1. משימה לכל שורה במטריצה
        for (int i = 0; i < leftMatrix.length(); i++) {
            final int rowIdx = i;
            
            tasks.add(() -> {
                // נקבל את הווקטור המתאים
                SharedVector v = leftMatrix.get(rowIdx);

                //2. אסטרטגיית נעילה פשוטה
                // רק ננעל לכתיבה כי אנחנו משנים את הווקטור
                v.writeLock();
                try {
                    // 3. מבצעים את השלילה
                    v.negate();
                } finally {
                    //5. נשחרר את הנעילה
                    v.writeUnlock();
                }
            });
        }
        return tasks;
    }

    public List<Runnable> createTransposeTasks() {
        List<Runnable> tasks = new ArrayList<>();
        
        // 1. משימה לכל שורה במטריצה
        for (int i = 0; i < leftMatrix.length(); i++) {
            final int rowIdx = i;
            
            tasks.add(() -> {
                SharedVector v = leftMatrix.get(rowIdx);
                
                // 2. אסטרטגיית נעילה
                // רק ננעל לכתיבה כי אנחנו משנים את הווקטור
                v.writeLock();
                try {
                    // 3. מבצעים את הטרנספוזיציה
                    v.transpose();
                } finally {
                    // 4. נשחרר את הנעילה   
                    v.writeUnlock();
                }
            });
        }
        return tasks;
    }

   public String getWorkerReport() {
        // יוצר דוח על כל העובדים
        return executor.getWorkerReport();
    }
}