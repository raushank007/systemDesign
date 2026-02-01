/*
	Sliding window Rate Limiter 
	
	-> every instant calculate the esitimated count = currentRequest + (PreviousRequest*weight)
	Imagine our limit is 100 requests/minute
	.Previous Minute (10:00 - 10:01): 50 requests occurred
	.Current Minute (10:01 - 10:02): 10 requests have occurred so far
	.Current Time: 10:01:15 (We are 15 seconds into the current minute).
	Step 1: 
	Calculate the Weight
	Since 15 seconds have passed in the current minute, 
	the "sliding window" (the last 60 seconds) must include the last 45 seconds of the previous minute.
	$$Weight = \frac{60 - 15}{60} = \frac{45}{60} = 0.75$$
	Step 2:
	 Calculate the Total Count
	 We take all 10 requests from the current minute and add 75% of the requests from the previous minute.
	 $$Count = 10 + (50 \times 0.75) = 10 + 37.5 = 47.5$$
	 In code, we usually floor this to 47. Since 47 < 100, the request is allowed! ✅
	
*/
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

class SlidingWindowRateLimiterImpl {
    private final int limit;
    private final ConcurrentHashMap<String, UserStats> repo = new ConcurrentHashMap<>();

    public SlidingWindowRateLimiterImpl(int limit) {
        this.limit = limit;
    }

    static class UserStats {
        AtomicLong preCount = new AtomicLong(0);
        AtomicLong currCount = new AtomicLong(0);
        volatile long lastWindowSyncTime = System.currentTimeMillis() / 1000 / 60; // Minute-based
    }

    public boolean isAllowed(String userId) {
        long nowInMinutes = System.currentTimeMillis() / 1000 / 60;
        UserStats stats = repo.computeIfAbsent(userId, k -> new UserStats());

        synchronized (stats) { // Ensure window sliding is atomic per user
            long diff = nowInMinutes - stats.lastWindowSyncTime;

            if (diff > 0) {
                // If more than 1 minute passed, the old 'current' becomes 'previous'
                // If more than 2 minutes passed, both are effectively 0
                stats.preCount.set(diff == 1 ? stats.currCount.get() : 0);
                stats.currCount.set(0);
                stats.lastWindowSyncTime = nowInMinutes;
            }

            // Calculate weighted sum
            double weight = 1.0 - ((System.currentTimeMillis() % 60000) / 60000.0);
            double estimatedCount = stats.currCount.get() + (stats.preCount.get() * weight);

            if (estimatedCount < limit) {
                stats.currCount.incrementAndGet();
                return true;
            }
            return false;
        }
    }
}
public class SlidingWindowRateLimiter {
    public static void main(String[] args) throws InterruptedException {
        // Limit of 2 requests per minute
        SlidingWindowRateLimiterImpl limiter = new SlidingWindowRateLimiterImpl(2);
        String user = "User_A";

        System.out.println("--- Phase 1: Rapid Fire ---");
        System.out.println("Req 1: " + limiter.isAllowed(user)); // Expected: true
        System.out.println("Req 2: " + limiter.isAllowed(user)); // Expected: true
        System.out.println("Req 3: " + limiter.isAllowed(user)); // Expected: false (Limit hit!)

        System.out.println("\n--- Phase 2: Wait for Window to Slide ---");
        System.out.println("Waiting 30 seconds...");
        Thread.sleep(30000); 

        // After 30s, the weight of the previous minute is 0.5.
        // Even if we are in the same minute, the "estimatedCount" should 
        // reflect the passage of time.
        System.out.println("Req 4 (after 30s): " + limiter.isAllowed(user));
    }
}
