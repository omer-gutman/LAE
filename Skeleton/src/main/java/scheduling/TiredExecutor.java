package scheduling;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public TiredExecutor(int numThreads) {
        this.workers = new TiredThread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            // לתת פקטור עייפות אקראי בין 0.5 ל-1.5
            double fatigueFactor = 0.5 + ThreadLocalRandom.current().nextDouble();
            workers[i] = new TiredThread(i, fatigueFactor);
            
            // התחל את העובד והוסף אותו לתור המינימום
            workers[i].start();
            idleMinHeap.add(workers[i]);
        }
    }

    public void submit(Runnable task) {
        try {
            // 1. משוך את העובד הכי פחות עייף מהתור 
            // אם אין עובדים זמינים, המתן
            TiredThread worker = idleMinHeap.take();
            
            inFlight.incrementAndGet();

            // 2. עוטף את המשימה כך שכשיסיים, יחזיר את עצמו לתור
            // העייפות של העובד מתעדכן בסוף המשימה כדי שיכנס לתור עם העייפות הנכונה
            Runnable wrapper = () -> {
                try {
                    task.run();
                } finally {
                    inFlight.decrementAndGet();
                    idleMinHeap.add(worker);
                }
            };

            // 3. מעביר את המשימה לעובד
            worker.newTask(wrapper);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void submitAll(Iterable<Runnable> tasks) {
        // מנע חסימה על ידי שימוש באובייקט נעילה ומונה אטומי
        Object lock = new Object();
        AtomicInteger remaining = new AtomicInteger(0);
        List<Runnable> batch = new ArrayList<>();

        // 1. עוטף כל משימה כך שכשיסיים, יעדכן את המונה ויעיר את המתנה אם זו הייתה האחרונה
        for (Runnable task : tasks) {
            remaining.incrementAndGet();
            batch.add(() -> {
                try {
                    task.run();
                } finally {
                    // להחדיש את המונה וליידע אם זו הייתה האחרונה
                    if (remaining.decrementAndGet() == 0) {
                        synchronized (lock) {
                            lock.notifyAll();
                        }
                    }
                }
            });
        }

        // 2. להגיש את כל המשימות
        for (Runnable t : batch) {
            submit(t);
        }

        // 3. המתן שכל המשימות יסתיימו
        synchronized (lock) {
            while (remaining.get() > 0) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public void shutdown() throws InterruptedException {
        // שולח פקודת עצירה לכל העובדים
        for (TiredThread worker : workers) {
            worker.shutdown();
        }
        // מחכה שכל העובדים יסיימו
        for (TiredThread worker : workers) {
            worker.join();
        }
    }

    public synchronized String getWorkerReport() {
        // יוצר דוח על כל העובדים
        StringBuilder sb = new StringBuilder();
        sb.append("Worker Report:\n");
        for (TiredThread worker : workers) {
            sb.append(String.format("Worker %d: Fatigue=%.2f, Idle=%d ms, Worked=%d ms\n",
                    worker.getWorkerId(),
                    worker.getFatigue(),
                    worker.getTimeIdle() / 1_000_000, // Convert nano to milli
                    worker.getTimeUsed() / 1_000_000));
        }
        return sb.toString();
    }
}
