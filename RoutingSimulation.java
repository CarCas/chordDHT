package chordDHT;

import chordDHT.NodeDescriptor;
import chordDHT.CentralizedCoordinator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class RoutingSimulation {

	private static NodeDescriptor routingProcedure(NodeDescriptor startingNode, ArrayList<Long> route)
			throws NoSuchAlgorithmException {
		Long key = chordDHT.CentralizedCoordinator.randomKey();

		Long succID = startingNode.findSuccessor(key, route);
		int found = -1;
		for (int i = 0; i < CentralizedCoordinator.tbl.size(); i++)
			if (CentralizedCoordinator.tbl.get(i).nodeID.compareTo(succID) == 0)
				found = i;
		if (found != -1) {
			if(startingNode.nodeID < 10)
				System.out.println("Node 00" + startingNode.nodeID.toString() + " searches key " + key
						+ ";	query sent to " + CentralizedCoordinator.tbl.get(found).nodeID);
			else if(startingNode.nodeID < 100)
				System.out.println("Node 0" + startingNode.nodeID.toString() + " searches key " + key
						+ ";	query sent to " + CentralizedCoordinator.tbl.get(found).nodeID);
			else
				System.out.println("Node " + startingNode.nodeID.toString() + " searches key " + key
						+ ";	query sent to " + CentralizedCoordinator.tbl.get(found).nodeID);
			return CentralizedCoordinator.tbl.get(found);
		}
		return null;
	}

	private static void fillFileFingers() throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File("./fingers.sif")));
		for (int i = 0; i < CentralizedCoordinator.tbl.size(); i++) {
			NodeDescriptor finger = CentralizedCoordinator.tbl.get(i);
			bw.write(finger.nodeID.toString() + " link ");
			for (int j = 0; j < finger.fingerTable.size() - 1; j++)
				bw.write(finger.fingerTable.get(j).nodeID.toString() + " ");
			bw.write(finger.fingerTable.get(finger.fingerTable.size() - 1).nodeID.toString() + "\n");
		}
		bw.close();
		return;
	}

	private static void fillFileAverages(HashMap<Long, Integer> map, double length) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File("./averages.txt")));
		for(Entry<Long, Integer> l : map.entrySet()) {
			Double d = (double) ((double) l.getValue() / length);
			bw.write(l.getKey().toString() + " " + d.toString());
			bw.newLine();
		}
		bw.close();
		return;		
	}
	
	private static void fillFileLoadBalancing(float sumAppearances) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File("./loadBalancing.txt")));
		bw.write("Keys");
		bw.newLine();
		for (int i = 0; i < CentralizedCoordinator.tbl.size(); i++) {
			Float f = CentralizedCoordinator.tbl.get(i).appearance / (float) sumAppearances;
			bw.write(f.toString());
			bw.newLine();
		}
		bw.close();
		return;		
	}
	
	public static void main(String[] args) {
		boolean r = chordDHT.CentralizedCoordinator.centralizedCoordinatorInit(args);
		ArrayList<ArrayList<Long>> jumps = new ArrayList<ArrayList<Long>>();
		if (!r)
			return;
		for (int i = 0; i < CentralizedCoordinator.tbl.size(); i++) {
			ArrayList<Long> jumpingRoute = new ArrayList<Long>();
			NodeDescriptor startingNode = CentralizedCoordinator.tbl.get(i);
			try {
				NodeDescriptor res = routingProcedure(startingNode, jumpingRoute);
				if (res == null)
					System.out.println("No result has been obtained.");
				else
					jumps.add(jumpingRoute);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			}
		}
		try {
			fillFileFingers();
		} catch (IOException e) {
			e.printStackTrace();
		}
		

		/*
		 * Calculating the ratio no.Appearances of a node / no.Appearances of the
		 * totality of the nodes for facing up to the load balancing issue.
		 */
		long sumAppearances = 0L;
		for (NodeDescriptor a : CentralizedCoordinator.tbl)
			sumAppearances += a.appearance;
		for (int i = 0; i < CentralizedCoordinator.tbl.size(); i++)
			System.out.println((float) ((float) CentralizedCoordinator.tbl.get(i).appearance / (float) sumAppearances)
					+ "	= load balancing for node " + CentralizedCoordinator.tbl.get(i).nodeID + ":	it compares "
					+ CentralizedCoordinator.tbl.get(i).appearance + "	times over " + sumAppearances
					+ "	total appearances");
		
		try {
			fillFileLoadBalancing(sumAppearances);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		/*
		 * Calculating the number of times a peer is the destination of a query. The
		 * calculation has been approximated by counting the number of times the same
		 * long identifier has been found at the end of a routing path and by dividing
		 * this number by the number of routes performed.
		 */
		long[] lastRoutedElement = new long[jumps.size()];
//		int i = 0;
//		while(i < jumps.size()) {
//			
//		}
		for (int i = 0; i < jumps.size(); i++) {
			ArrayList<Long> currEl = jumps.get(i);
			if (currEl.size() > 0)
				lastRoutedElement[i] = currEl.get(currEl.size() - 1);
		}

		HashMap<Long, Integer> compareInDestination = new HashMap<Long, Integer>();
		for (int l = 0; l < lastRoutedElement.length; l++) {
			if (compareInDestination.containsKey(lastRoutedElement[l])) {
				int newValue = compareInDestination.get(lastRoutedElement[l]);
				newValue += 1;
				compareInDestination.replace(lastRoutedElement[l], newValue);
			} else
				compareInDestination.put(lastRoutedElement[l], 1);
		}
		
		try {
			fillFileAverages(compareInDestination, (double)lastRoutedElement.length);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		for (Entry<Long, Integer> l : compareInDestination.entrySet()) {
			if (l.getKey() < 10) 
				System.out.println("Node: 000" + l.getKey().toString() + "	compares " + l.getValue()
						+ "	as destination over " + lastRoutedElement.length + "	routes. Average: "
						+ (double) ((double) l.getValue() / (double) lastRoutedElement.length));
			else if (l.getKey() < 100) 
				System.out.println("Node: 00" + l.getKey().toString() + "	compares " + l.getValue()
						+ "	as destination over " + lastRoutedElement.length + "	routes. Average: "
						+ (double) ((double) l.getValue() / (double) lastRoutedElement.length));
			else if (l.getKey() < 1000)
				System.out.println("Node: 0" + l.getKey().toString() + "	compares " + l.getValue()
						+ "	as destination over " + lastRoutedElement.length + "	routes. Average: "
						+ (double) ((double) l.getValue() / (double) lastRoutedElement.length));
			else
				System.out.println("Node: " + l.getKey().toString() + "	compares " + l.getValue()
				+ "	as destination over " + lastRoutedElement.length + "	routes. Average: "
				+ (double) ((double) l.getValue() / (double) lastRoutedElement.length));
		}

		/*
		 * Calculating the average path length. Before, the calculation needs the
		 * summation of all the path lengths, then to perform the calculation of the
		 * average path length it's enough to divide the sum of paths with the number of
		 * paths.
		 */
		int[] pathsLength = new int[jumps.size()];
		Integer pathsSum = 0;
		for (int i = 0; i < jumps.size(); i++) {
			pathsLength[i] = jumps.get(i).size();
			pathsSum += jumps.get(i).size();
		}

		System.out.println("The average path length is composed of " + (float) ((float) pathsSum / (float) jumps.size())
				+ " peers.");

		return;
	}

}
