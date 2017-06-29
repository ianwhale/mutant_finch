package esi.finch.probs;

public class InsertionSort {
	
	/**
	 * Insertion sort with only one mistake...
	 * 
	 * @param list 
	 * @return sorted list 
	 */
	public int[] sort(int[] list) {
        int size = list.length;
        
        for (int i = 1; i < size; i++) {
        	int value = list[i];
        	int j = i; // Mistake line, should be i - 1
        	
        	while(j >= 0 && list[j] > value) {
        		list[j + 1] = list[j];
        		j = j - 1; // Line containing solution
        	}
        }
        
        return list;
	}
}
