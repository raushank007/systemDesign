/*
  given a stream of message requests and their timestamp as input implement a logger rate limiter system that decides whether the current message request is displayed
  The decision depends on whether the same message has already been displayed in the last s seconds 
  yes -> the decision is false
  no -> the decision is true
*/


/*
		given message -> with its timestamp , in s second if same message is there then false otherwise save 
		HashMap -> key will be message -> lastUpdated timestamp will be value 
		
		if lastUpdate - currentTime :-> is less than or equal to s seconds then return false 
		if lastUpdated - currentTime -> is greater than s seconds then return true
*/

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

class Cache {
    private static class Node {
        String key;
        LocalDateTime value;
        Node prev, next;
        Node(String key, LocalDateTime value) {
            this.key = key;
            this.value = value;
        }
    }

    private static class DoublyLinkedList {
        Node head, tail;
        DoublyLinkedList() {
            head = new Node(null, null);
            tail = new Node(null, null);
            head.next = tail;
            tail.prev = head;
        }

        void remove(Node node) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
        }

        void addFirst(Node node) {
            Node first = head.next;
            node.next = first;
            first.prev = node;
            node.prev = head;
            head.next = node;
        }
    }

    int capacity;
    int threshold; // Fixed spelling
    Map<String, Node> map;
    DoublyLinkedList dll;

    public Cache(int capacity, int threshold) {
        this.capacity = capacity;
        this.threshold = threshold;
        this.map = new HashMap<>();
        this.dll = new DoublyLinkedList();
    }

    public boolean put(String key, LocalDateTime value) {
        if (map.containsKey(key)) {
            Node node = map.get(key);
            // If the message is NOT expired, rate limit it (return false)
            if (!isExpired(node.value)) {
                return false; 
            }
            // If expired, refresh it
            dll.remove(node);
            dll.addFirst(node);
            node.value = value;
            return true;
        }

        if (map.size() >= capacity) {
            Node last = dll.tail.prev;
            // Strategy: Evict the oldest message to make room
            map.remove(last.key);
            dll.remove(last);
        }

        Node node = new Node(key, value);
        dll.addFirst(node);
        map.put(key, node);
        return true;
    }

    private boolean isExpired(LocalDateTime lastUpdated) {
        return Duration.between(lastUpdated, LocalDateTime.now()).getSeconds() >= threshold;
    }
}



public class LoggerRateLimiter{
    public static void main(String[] args) {
        // Capacity: 2 unique messages, Threshold: 2 seconds
        Cache logger = new Cache(2, 2);

        System.out.println("--- Test 1: Initial Logging ---");
        log(logger, "LoginSuccess"); // Expected: true
        log(logger, "LoginSuccess"); // Expected: false (Too fast!)
        
        System.out.println("\n--- Test 2: Capacity Eviction ---");
        log(logger, "DB_Error");     // Expected: true
        log(logger, "Disk_Full");    // Expected: true (Should evict LoginSuccess)
        
        System.out.println("\n--- Test 3: Waiting for Expiry ---");
        try {
            System.out.println("Waiting 3 seconds...");
            Thread.sleep(3000); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log(logger, "Disk_Full");    // Expected: true (Threshold passed)
        log(logger, "LoginSuccess"); // Expected: true (Was evicted, now new entry)
    }

    private static void log(Cache cache, String msg) {
        boolean result = cache.put(msg, LocalDateTime.now());
        System.out.println("Message: [" + msg + "] -> Logged: " + result);
    }
}

