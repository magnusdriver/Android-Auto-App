package helpers;


public class SharedNotificationHelper {
    // This class allows to use a NotificationHelpers instance from the main thread in a different thread of kotlin coroutine.
    private NotificationHelper helper;
    private final Object lock = new Object();
    public static SharedNotificationHelper sInstance = null;

    public SharedNotificationHelper(NotificationHelper newHelper) {
        synchronized (lock) {
            this.helper = newHelper;
            sInstance = this;
            lock.notifyAll();
        }
    }


    public NotificationHelper getHelper() {
        synchronized (lock) {
            return helper;
        }
    }
}
