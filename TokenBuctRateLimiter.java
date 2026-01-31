/*
	Token bucket Rate Limiter
	-> uses : 1 . if a user is allowed to make 1 post per second, add 150 friend per day .... each endpoint required there own bucket for each user
		  2. if we need to throttle requests based on IP address, each IP address required a bucket
		  3. if the system allows a maximum of 10000 requests per second, it makes sense to have a global bucket shared by all requests.
		  
	token bucker is a container that has pre-defined capacity. Tokens are put in the bucket at present rates periodically.Once the bucket is full, no more token are added. Example : the token bucket capacity is 4. The refiller puts 2 tokens into the bucket every second. Once the bucket is full, exta token will overflow
	
	Each request consumes one token. When a request arrives, we check if there are enough token in the bucket 
		if there are enough token, we take one token out for each request, the request goes through
		if there are not enough token, the request is dropped
		
		
	The token bucker algo takes two parameters:
		1. Bucket size : the maximum number of tokens allowed in the bucket
		2. Refill rate : number of tokens put into the bucket every second
		
		
	Bucket size and  refill rate -> int
	store the request how much token use by user in hashMap
	
	flow -> request  ->
		if -> there is enough token -> decrease token -> request goes through 
		if -> there is not enough token -> request is dropped
	
	schdules run for every send for refill to fill the bucket
	
	it will be fast retrivel and store so use cache->hashmap			
*/

class TokenBucketRateLimiterDS{
	private final long maxBucketSize;
	private final long refillRatePerSecond;
	
	
	private double currentTokens;
	private long lastRefillTimeStamp;
	TokenBucketRateLimiterDS(long maxBucketSize, long refillRatePerSecond){
		this.maxBucketSize=maxBucketSize;
		this.refillRatePerSecond=refillRatePerSecond;
		this.currentTokens = maxBucketSize;
		this.lastRefillTimeStamp = System.currentTimeMillis();
	}
	
	public synchronized boolean isAllowed(){
		refill();
		
		if(currentTokens >= 1.0){
			currentTokens = currentTokens - 1.0;
			return true;
		}
		return false;		
	}
	
	private void refill(){
		long now = System.currentTimeMillis();
		double tokenToAdd = (now-lastRefillTimeStamp) *(refillRatePerSecond/1000.0);
		
		currentTokens = Math.min(maxBucketSize, tokenToAdd+currentTokens);
		
		lastRefillTimeStamp = now;
	}
	
}


public class TokenBuctRateLimiter{
	public static void main(String[] args){
		
		TokenBucketRateLimiterDS ds = new TokenBucketRateLimiterDS(5,2);
		
		System.out.println("----------phase 1 : Rapid fire ------------");
		for(int i=0;i<10;i++){
			boolean allowed = ds.isAllowed();
			System.out.println("Request :"+i+", allowed :"+ allowed);
		}
		
		System.out.println("\n--- Phase 2: Waiting for Refill (1.5 seconds) ---");
		// In 1.5 seconds, we expect ~3 tokens to refill (1.5s * 2 tokens/s)
			try {
		    System.out.println("Waiting 1.5 seconds...");
		    Thread.sleep(1500); 
		} catch (InterruptedException e) {
		    Thread.currentThread().interrupt();
		}

		System.out.println("--- Phase 3: Post-Refill Requests ---");
		for (int i = 1; i <= 4; i++) {
		    boolean allowed = ds.isAllowed();
		    System.out.println("Request " + i + ": " + (allowed ? " Allowed" : "Rate Limited"));
		}
		
	}
}
