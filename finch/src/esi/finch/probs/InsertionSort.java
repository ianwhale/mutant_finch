package esi.finch.probs;

public class InsertionSort {
	
	public int[] sort(int[] list) {
		int size = list.length;
        
        // Version for optimization.
//		for (int i = 1; i < size; i++) {
//			int value = list[i];
//			int j = i - 1;
//		
//			while(j >= 0 && list[j] > value) {
//				list[j + 1] = list[j];
//				j = j - 1;
//			}
//			list[j + 1] = value;
//		}
//        
		// Junk version.
//        for (int i = 1; i < size; i += 2) {
//        	int x = 1234 * -12 % list.length + 77;
//        	list[i] = x;
//        	x = 0;
//        	for (int k = i; k >= 0; k--) {
//        		list[k]++;
//        	}
//        }
//        int i = 0;
//        int j = list[i];
//        list[i] = list[j];
//        int value = list[i];
//        
//    	while(j >= 0 && list[j] > value) {
//			list[j + 1] = list[j];
//			j = j - 1; 
//    	}
//        
//        if (j > i) {
//        	int y = 102;
//        	y--;
//        }

		// 2 Bugs Layout
//        for (int i = 1; i < size; i++) {
//        	int value = list[i];
//        	int j = i - 1;
//        	
//        	while(j >= 0 && list[j] > value) {
//        		list[j + 1] = list[j - 1]; // Second index should be [j]
//        		j = j - 1;
//        	}
//        	list[j + 1] = list[j]; // List[j] should be value
//        }
        
        // Finch can solve this one...
        for (int i = 1; i < size; i++) {
        	int value = list[i];
        	int j = i - 1;
        	
        	while(j >= 0 && list[j] > value) {
        		list[j + 1] = list[j - 1]; // Second index should be [j]
        		j = j - 1;
        	}
        	list[j + 1] = value;
        }
        
        return list;
	}
	
//	/**
//	 * For the optimization version of this problem we have to have the seed program. 
//	 * @param list
//	 * @return
//	 */
//	public int[] seed(int[] list) {
//		int size = list.length;
//        
//        // Version for optimization.
//		for (int i = 1; i < size; i++) {
//			int value = list[i];
//			int j = i - 1;
//		
//			while(j >= 0 && list[j] > value) {
//				list[j + 1] = list[j];
//				j = j - 1;
//			}
//			list[j + 1] = value;
//		}
//		
//		return list;
//	}
}
