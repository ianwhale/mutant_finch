package esi.finch.probs;

public class InsertionSort {
	
	public int[] sort(int[] list) {
        int size = list.length;
        
        for (int i = 1; i < size; i += 2) {
        	int x = 1234 * -12 % list.length + 77;
        	list[i] = x;
        	x = 0;
        }
        int i = 0;
        int j = list[i];
        list[i] = list[j];
        int value = list[i];
        
    	while(j >= 0 && list[j] > value) {
			list[j + 1] = list[j];
			j = j - 1; 
    	}
        
        if (j > i) {
        	int y = 102;
        	y--;
        }
        
        // Original with 1 bug.
//        for (int i = 1; i < size; i++) {
//        	int value = list[i];
//        	int j = i; // Mistake line, should be i - 1
//        	
//        	while(j >= 0 && list[j] > value) {
//        		list[j + 1] = list[j];
//        		j = j - 1; // Line containing solution
//        	}
//        }
        
        return list;
	}
}
