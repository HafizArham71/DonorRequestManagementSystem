package com.example.donorrequestmanagementsystem.engine.datastructure;

import com.example.donorrequestmanagementsystem.engine.model.EmergencyRequest;

import java.util.Arrays;
import java.util.NoSuchElementException;

public class EmergencyRequestHeap {
    private EmergencyRequest[] heap;
    private int size;
    private int capacity;

    public EmergencyRequestHeap(int initialCapacity) {
        if (initialCapacity <= 0) throw new IllegalArgumentException("Capacity must be greater than zero.");
        this.capacity = initialCapacity;
        this.heap = new EmergencyRequest[capacity];
        this.size = 0;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Inserts a new request into the heap and bubbles it up to maintain heap invariants.
     */
    public void insert(EmergencyRequest request) {
        if (request == null) throw new IllegalArgumentException("Cannot insert a null request.");

        ensureCapacity();
        heap[size] = request;
        size++;
        bubbleUp(size - 1);
    }

    /**
     * Removes and returns the highest priority emergency request from the root of the heap.
     */
    public EmergencyRequest poll() {
        if (isEmpty()) throw new NoSuchElementException("The request heap is completely empty.");

        EmergencyRequest highestPriority = heap[0];
        heap[0] = heap[size - 1]; // Move the last element to the root
        heap[size - 1] = null;    // Avoid memory leak
        size--;

        if (size > 0) {
            bubbleDown(0);
        }

        return highestPriority;
    }

    /**
     * Inspects the root element without removing it.
     */
    public EmergencyRequest peek() {
        if (isEmpty()) throw new NoSuchElementException("The request heap is completely empty.");
        return heap[0];
    }

    // --- Core Heap Algorithmic Math ---

    private void bubbleUp(int index) {
        while (index > 0) {
            int parentIndex = (index - 1) / 2;
            if (compareRequests(heap[index], heap[parentIndex]) < 0) {
                swap(index, parentIndex);
                index = parentIndex;
            } else {
                break;
            }
        }
    }

    private void bubbleDown(int index) {
        while (index < size / 2) { // While there is at least one left child
            int leftChild = 2 * index + 1;
            int rightChild = 2 * index + 2;
            int smallestChild = leftChild;

            if (rightChild < size && compareRequests(heap[rightChild], heap[leftChild]) < 0) {
                smallestChild = rightChild;
            }

            if (compareRequests(heap[smallestChild], heap[index]) < 0) {
                swap(index, smallestChild);
                index = smallestChild;
            } else {
                break;
            }
        }
    }

    /**
     * Comparative Logic for Min-Heap Ordering.
     * Returns a negative value if r1 has HIGHER priority than r2 (should sit higher up in the heap).
     */
    private int compareRequests(EmergencyRequest r1, EmergencyRequest r2) {
        // Step 1: Compare weight values of Urgency levels (CRITICAL = 3, LOW = 1)
        // Since we want higher weights at the top of our min-heap, we invert the comparison logic
        int urgencyCompare = Integer.compare(r2.getUrgency().getWeight(), r1.getUrgency().getWeight());
        if (urgencyCompare != 0) {
            return urgencyCompare;
        }
        // Step 2: Tie-breaker using early registration timestamps
        return Long.compare(r1.getTimestamp(), r2.getTimestamp());
    }

    private void swap(int i, int j) {
        EmergencyRequest temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
    }

    private void ensureCapacity() {
        if (size == capacity) {
            capacity *= 2;
            heap = Arrays.copyOf(heap, capacity);
        }
    }
}