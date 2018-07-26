package chordDHT;

import java.math.BigInteger;
import java.security.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class CentralizedCoordinator {

	private static final int MAX_VALUE_SHA1 = 160;

	static int nbit = -1;
	static int nodes = -1;

	static ArrayList<NodeDescriptor> tbl = new ArrayList<NodeDescriptor>();

	/**
	 * This function converts a byte array to a long value via bitwise operations.
	 * 
	 * @param b
	 *            the byte array in input
	 */
	public static long bytesToLong(byte[] b) {
		long result = 0;
		if (b.length < Byte.SIZE)
			for (int i = 0; i < b.length; i++) {
				result <<= 8;
				result |= (b[i] & 0xFF);
			}
		else
			for (int i = 0; i < 8; i++) {
				result <<= 8;
				result |= (b[i] & 0xFF);
			}
		return result;
	}

	/**
	 * This function generates the SHA-1 of a random long, it truncates the SHA-1
	 * string at the last number of bits given in input by nbit, if this value is
	 * less than the number of bits returned by the SHA-1 algorithm and it returns
	 * the hexadecimal string corresponding to the byte array possibly truncated.
	 * 
	 * @param m
	 *            Number of bits taken in input from command line
	 * @throws NoSuchAlgorithmException
	 * 
	 */
	private static Long generateNode(int m) throws NoSuchAlgorithmException {
		Long id = new Random().nextLong();
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(id.toString().getBytes());
		byte[] output = md.digest();
		byte[] result = null;
		if (m < MAX_VALUE_SHA1)
			result = new byte[m];
		else
			result = new byte[MAX_VALUE_SHA1];

		/* Truncating the byte array output. */
		Integer j = m - 1;
		for (Integer i = output.length - 1; i >= output.length - m; i--) {
			result[j] = output[i];
			j--;
		}
		long l = bytesToLong(result);
		return (l < 0) ? (-1L * l) : l;
	}

	/**
	 * This function returns the random generated key modulo 2^nbit
	 */
	public static Long randomKey() throws NoSuchAlgorithmException {
		Long key = (generateNode(nbit)) % ((long) Math.pow(2, nbit));
		return key;
	}

	/**
	 * This function fills the fingers of all the nodes in the ring. It is
	 * implemented by calculating for each value of the interval [0, nbit] the
	 * corresponding value that assume the id of the finger node using its
	 * definition and then by identifying the node of the ring with that value.
	 * 
	 * @param tbl
	 *            ArrayList of nodes inserted into the ring
	 * @param nbit
	 *            Number of bits taken from command line
	 * 
	 */
	private static void fillFingerTable(ArrayList<NodeDescriptor> tbl, int nbit) {
		/* calculating the domain of the possible identifiers. */
		Long big = (long) Math.pow(2, nbit);
		/* for each element of the FingerTable */
		for (int j = 0; j < tbl.size(); j++) {
			NodeDescriptor nd = tbl.get(j);
			/* calculating the fingers */
			int i = 1;
			while (i <= nbit) {
				/* finger[i] = findSuccessor((node_i + 2^(i-1)) % big) */
				Long b = ((long) Math.pow(2, i - 1) + nd.nodeID) % big;
				int k = 0;
				while (tbl.get(k).nodeID.compareTo(b) < 0) {
					k++;
					if (k == tbl.size()) {
						b = 0L;
						k = 0;
					}
				}
				NodeDescriptor fingerIDnode = tbl.get(k);
				if (fingerIDnode.nodeID != tbl.get(j).nodeID) {
					if (!nd.fingerTable.contains(fingerIDnode))
						nd.fingerTable.add(fingerIDnode);
					else {
						if (!nd.load.containsKey(fingerIDnode.nodeID))
							nd.load.put(fingerIDnode.nodeID, 1);
						else {
							int newValue = nd.load.get(fingerIDnode.nodeID);
							newValue += 1;
							nd.load.replace(fingerIDnode.nodeID, newValue);

						}
					}
				}
				i++;
			}
		}
	}

	/**
	 * This function prints to the standard output the content of all the
	 * FingerTables of the peers.
	 */
	private static void printFingerTable() {
		for (int i = 0; i < CentralizedCoordinator.tbl.size(); i++) {
			if (CentralizedCoordinator.tbl.get(i).fingerTable.size() > 0) {
				System.out.print("The node " + CentralizedCoordinator.tbl.get(i).nodeID + " has fingers: ");
				for (int j = 0; j < CentralizedCoordinator.tbl.get(i).fingerTable.size() - 1; j++)
					System.out.print(CentralizedCoordinator.tbl.get(i).fingerTable.get(j).nodeID + ", ");
				System.out.println(CentralizedCoordinator.tbl.get(i).fingerTable
						.get(CentralizedCoordinator.tbl.get(i).fingerTable.size() - 1).nodeID);
			} else
				System.out.println("The node " + CentralizedCoordinator.tbl.get(i).nodeID + " has no fingers. [ERROR]");
		}
	}

	/**
	 * This function inserts the nodes that will belong to the chord ring by
	 * assigning for each node the value returning from the
	 * {@link #generateNode(int nbit)} function.
	 * 
	 * @param nbit
	 *            Number of bits taken from command line
	 * @param nodes
	 *            Number of nodes that participates to the chord ring simulation
	 * @param tbl
	 *            ArrayList of nodes inserted into the ring
	 * 
	 * 
	 */
	private static void insertNodes(int nbit, int nodes, ArrayList<NodeDescriptor> tbl) {
		int i = 0;
		while (i < nodes) {
			NodeDescriptor nd = new NodeDescriptor();
			try {
				nd.nodeID = generateNode(nbit);
				nd.nodeID = nd.nodeID % (long) Math.pow(2, nbit);
				if (nd.nodeID < 0)
					nd.nodeID = -1L * nd.nodeID;
				boolean found = false;
				for (NodeDescriptor t : tbl)
					if (t.nodeID.compareTo(nd.nodeID) == 0)
						found = true;
				if (!found) {
					tbl.add(nd);
					i++;
				}
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				System.err.println("No able to add the node " + i + " in the list of peers.");
				return;
			}
		}
	}

	/**
	 * This function updates the successor field of each NodeDescriptor with the
	 * successive element in the ArrayList.
	 */
	private static void updatingSuccessor() {
		for (int i = 0; i < tbl.size() - 1; i++)
			tbl.get(i).successor = tbl.get(i + 1);
		tbl.get(tbl.size() - 1).successor = tbl.get(0);
	}

	/**
	 * This is the function for initializing the data structure regarding the
	 * participants of the chord ring and the finger table of each of them. It
	 * inserts the number of nodes specified by the second argument, defines the
	 * successor of each node, it fills the finger table of each node and finally
	 * starts the routing simulation process.
	 * 
	 * @param args[0]
	 *            Number of bits used in the simulation for the identifiers;
	 * @param args[1]
	 *            Number of nodes that participates to the simulation.
	 */
	public static boolean centralizedCoordinatorInit(String[] args) {
		if (args.length < 2) {
			System.err.println("Usage: ./routingProcess noBit noNodes\n\r\t"
					+ "Where noBit is the number of bit used for the identifiers and "
					+ "noNodes is the number of participants to the chord routing simulation.");
			return false;
		}
		try {
			nbit = Integer.parseInt(args[0]); // max number of identifiers: 2^nbit
			nodes = Integer.parseInt(args[1]);

			if ((nbit <= 0) || (nodes <= 0)) {
				System.err.println(
						"Error: insert positive values for the nodes parameter and at least 8 bits for the identifiers.");
				return false;
			}

			insertNodes(nbit, nodes, tbl);
			Collections.sort(tbl, new chordDHT.NodeDescriptor());
			updatingSuccessor();
			fillFingerTable(tbl, nbit);
			printFingerTable();
			System.out.println("Modulus: " + (new BigInteger("2")).pow(nbit).toString(16));
			System.out.print("Successors: ");
			for (int i = 0; i < nodes; i++)
				System.out.print(tbl.get(i).nodeID.toString() + " -> " + tbl.get(i).successor.nodeID.toString() + " ");
			System.out.println();
		} catch (NumberFormatException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
