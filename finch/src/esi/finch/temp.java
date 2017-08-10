package esi.finch;

import java.util.List;
import java.util.SortedSet;
import java.util.logging.Logger;

import de.uka.ipd.sdq.ByCounter.execution.BytecodeCounter;
import de.uka.ipd.sdq.ByCounter.execution.CountingResultBase;
import de.uka.ipd.sdq.ByCounter.execution.CountingResultCollector;
import de.uka.ipd.sdq.ByCounter.results.CountingResult;
import de.uka.ipd.sdq.ByCounter.utils.InvocationResultData;
import de.uka.ipd.sdq.ByCounter.utils.MethodDescriptor;

public class temp {
	public static void main(String[] args) {
		int[] A = {-36, -31, 35, 14, -63, 26, 80, 92};
		
		Logger.getLogger(BytecodeCounter.class.getCanonicalName()).setLevel(java.util.logging.Level.OFF);
		
		BytecodeCounter counter = new BytecodeCounter();
		
		String className = "esi.finch.temp";
		String methodString = "public static int[] sort(int[] list)";
		MethodDescriptor method = new MethodDescriptor(className, methodString);
		
		counter.addEntityToInstrument(method);
		// counter.getInstrumentationParams().setInstrumentRecursively(true);
		counter.instrument();
		
		InvocationResultData exeResult = counter.execute(method, new Object[] {A});
		
		SortedSet<CountingResult> countResults = CountingResultCollector.getInstance().retrieveAllCountingResults().getCountingResults();
		
		
		
		System.out.println("Result: "); arrPrint((int[])exeResult.returnValue);
		System.out.println("This took: " + exeResult.duration + " nanoseconds.");
		System.out.println("Instructions executed: ");
		for (CountingResultBase result : countResults) {
			System.out.println(result.getTotalCount(true));
		}	
	}
	
	public static void arrPrint(int[] A) {
		for (int a : A) {
			System.out.print(a + " ");
		}
	}
	
	public static int[] sort(int[] list) {
		int i = 1;
		while (i > 0) { i++; }
//		int size = list.length;
//        for (int i = 1; i < size; i++) {
//        	int value = list[i];
//        	int j = i - 1;
//        	
//        	while(j >= 0 && list[j] > value) {
//        		list[j + 1] = list[j]; 
//        		j = j - 1;
//        	}
//        	list[j + 1] = value;
//      
        return list;
	}
	
	public static int fact(int a) {
		if (a <= 0) { return 1; }
		else { return a * fact(a); }
	}
}
