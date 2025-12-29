package scheduling;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class TiredThread extends Thread implements Comparable<TiredThread> {

    private static final Runnable POISON_PILL = () -> {}; // Special task to signal shutdown

    private final int id; // Worker index assigned by the executor
    private final double fatigueFactor; // Multiplier for fatigue calculation

    private final AtomicBoolean alive = new AtomicBoolean(true); // Indicates if the worker should keep running

    // Single-slot handoff queue; executor will put tasks here
    private final BlockingQueue<Runnable> handoff = new ArrayBlockingQueue<>(1);

    private final AtomicBoolean busy = new AtomicBoolean(false); // Indicates if the worker is currently executing a task

    private final AtomicLong timeUsed = new AtomicLong(0); // Total time spent executing tasks
    private final AtomicLong timeIdle = new AtomicLong(0); // Total time spent idle
    private final AtomicLong idleStartTime = new AtomicLong(0); // Timestamp when the worker became idle

    public TiredThread(int id, double fatigueFactor) {
        this.id = id;
        this.fatigueFactor = fatigueFactor;
        this.idleStartTime.set(System.nanoTime());
        setName(String.format("FF=%.2f", fatigueFactor));
    }

    public int getWorkerId() {
        return id;
    }

    public double getFatigue() {
        return fatigueFactor * timeUsed.get();
    }

    public boolean isBusy() {
        return busy.get();
    }

    public long getTimeUsed() {
        return timeUsed.get();
    }

    public long getTimeIdle() {
        return timeIdle.get();
    }

    /**
     * Assign a task to this worker.
     * This method is non-blocking: if the worker is not ready to accept a task,
     * it throws IllegalStateException.
     */

    // נותן משימה לעובד
    // אם העובד לא מוכן לקבל משימה, זורק חריגה
    public void newTask(Runnable task) {
       if (!handoff.offer(task)) {
            throw new IllegalStateException("Worker " + id + " is not ready to accept a new task.");
        }
    }

    /**
     * Request this worker to stop after finishing current task.
     * Inserts a poison pill so the worker wakes up and exits.
     */


    // אם העובד עסוק הוא יסיים את המשימה הנוכחית ואז יפסיק
    // עובד לפי אם התור מלא
    public void shutdown() {
       try {
            handoff.put(POISON_PILL);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
        }
    }

    @Override
    public void run() {
       while (alive.get()) {
            try {
                // 1.ממתין למשימה חדשה, מתייבש ... 
                Runnable task = handoff.take();

                long now = System.nanoTime();
                long idleDuration = now - idleStartTime.get();
                timeIdle.addAndGet(idleDuration);

                // 2. בודק אם המשימה היא פקודת עצירה
                if (task == POISON_PILL) {
                    alive.set(false);
                    break;
                }

                // 3. מבצע את המשימה
                busy.set(true);
                long startWork = System.nanoTime();
                try {
                    task.run();
                } catch (RuntimeException e) {
                    //  מונע קריסת העובד במקרה של חריגה במשימה
                    System.err.println("Task in Worker " + id + " failed: " + e.getMessage());
                } finally {
                    long endWork = System.nanoTime();
                    long workDuration = endWork - startWork;
                    
                    // 4. מעדכן מדדים
                    timeUsed.addAndGet(workDuration);
                    busy.set(false);
                    
                    // מאפס את טיימר ההמתנה להמתנה הבאה
                    idleStartTime.set(System.nanoTime());
                }

            } catch (InterruptedException e) {
                // אם העובד מופרע בזמן ההמתנה, מתייחסים לכך כאות לעצירה
                alive.set(false);
                break;
            }
        }
    }

    @Override
    public int compareTo(TiredThread o) {
        // בוחן עייפות בין שני עובדים
        // עובד עם עייפות נמוכה יותר יקבל עדיפות גבוהה יותר
        return Double.compare(this.getFatigue(), o.getFatigue());
    }
}