package spl.lae;

import java.io.IOException;
import parser.InputParser;
import parser.OutputWriter;
import parser.ComputationNode;

public class Main {
    public static void main(String[] args) {
        // 1. נודא שהארגומנטים נכונים... שבאמת קיבלנו 3
        if (args.length != 3) {
            System.err.println("Usage: java -jar lga-1.0.jar <numThreads> <inputPath> <outputPath>");
            return;
        }

        // קריאת ארגומנטים
        int numThreads = Integer.parseInt(args[0]);
        String inputPath = args[1];
        String outputPath = args[2];

        // יצירת מנוע האלגברה הליניארית עם מספר העובדים הנתון
        LinearAlgebraEngine engine = new LinearAlgebraEngine(numThreads);
        InputParser parser = new InputParser();

        try {
            // 2. נתרגם את הקלט לעץ חישובי
            ComputationNode root = parser.parse(inputPath);

            // 3. אעאאעאעא נריץ את החישוב
            // בעצם כל העבודה המקבלית נעשית כאן... מחזיר את השורש הפותר
            ComputationNode resultNode = engine.run(root);

            // 4. כתיבת התוצאה הסופית לקובץ הפלט
            // נתרגם את המטריצה לתוצאה בפורמט JSON ונכתוב לקובץ הפלט
            OutputWriter.write(resultNode.getMatrix(), outputPath);

        } catch (Exception e) {
            // 5. ניול שגיאות
            // כפי שבוקש...
            try {
                OutputWriter.write(e.getMessage(), outputPath);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } finally {
            // 6. זהו סיימנו, נסגור את המנוע ונוציא דוח על העובדים
            try {
                // הpoison pill שלא ישארו עובדים רצים ברקע
                engine.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // דוח על פעילות העובדים
            System.out.println(engine.getWorkerReport());
        }
    }
}