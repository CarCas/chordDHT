package chordDHT;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class NodeDescriptor implements Comparator<NodeDescriptor> {
	Long nodeID;
	NodeDescriptor successor;
	ArrayList<NodeDescriptor> fingerTable = new ArrayList<NodeDescriptor>();
	HashMap<Long, Integer> load = new HashMap<Long, Integer>();
	int appearance = 0;
	int appInCurrentRoute = 0;
	
	/**
	 * This function search for the node whose ID most immediately precedes key.
	 * 
	 * @param key
	 *            is the key to be searched
	 * @return the identifier of the node to which the key is addressed
	 */
	private NodeDescriptor closestPrecedingNode(Long key, ArrayList<Long> route) {
		appearance += 1;
		for (int i = fingerTable.size() - 1; i >= 0; i--) {
			Long fingerID = fingerTable.get(i).nodeID;
			if (fingerID != key) {
				if (nodeID < key) {
					if (nodeID < fingerID && fingerID < key) {
						route.add(fingerID);
						return fingerTable.get(i);
					}
				} else if (nodeID >= key)
					if (nodeID < fingerID || fingerID <= key) {
						route.add(fingerID);
						return fingerTable.get(i);
					}
			}
		}
		return this;
	}

	/**
	 * This function returns the ID of the successor node chosen by the chord
	 * algorithm. It calls the {@link #closestPrecedingNode(Long key)} if: - the
	 * searched key is not in the range (nodeID, successor] when the nodeID <
	 * successor; - the searched key is not in the range (nodeID, successor] when
	 * the nodeID > successor; - the searched key is equal to the nodeID.
	 * 
	 * @param key
	 *            to be searched
	 * @return the identifier of the node to which the key is addressed
	 */
	public Long findSuccessor(Long key, ArrayList<Long> route) {
		if (key != nodeID) {
			if (nodeID < successor.nodeID) {
				if (nodeID < key && key <= successor.nodeID) {
					route.add(successor.nodeID);
					appearance += 1;
					appInCurrentRoute = 0;
					successor.appearance += 1;
					return successor.nodeID;
				} else {
					//System.out.println("Key: " + key + " - nodeID: " + nodeID);
					if (appInCurrentRoute < 2) {
						appInCurrentRoute += 1;
						appearance += 1;
						return closestPrecedingNode(key, route).findSuccessor(key, route);
					} else {
						appInCurrentRoute = 0;
						appearance += 1;
						return this.nodeID;
					}
				}
			} else if (nodeID > successor.nodeID) {
				if (nodeID < key || key <= successor.nodeID) {
					route.add(successor.nodeID);
					appearance += 1;
					appInCurrentRoute = 0;
					successor.appearance += 1;
					return successor.nodeID;
				} else {
					appInCurrentRoute += 1;
					appearance += 1;
					return closestPrecedingNode(key, route).findSuccessor(key, route);
				}
			} else {
				appInCurrentRoute += 1;
				appearance += 1;
				return closestPrecedingNode(key, route).findSuccessor(key, route);
			}
		} else {
			if (appInCurrentRoute < 2) {
				appInCurrentRoute += 1;
				appearance += 1;
				return closestPrecedingNode(key, route).findSuccessor(key, route);
			} else {
				appInCurrentRoute = 0;
				appearance += 1;
				return this.nodeID;
			}
		}
		// return this.nodeID;

	}

	public int equals(NodeDescriptor nd) {
		return this.nodeID.compareTo(nd.nodeID);
	}

	@Override
	public int compare(NodeDescriptor o1, NodeDescriptor o2) {
		return o1.nodeID.compareTo(o2.nodeID);
	}
}
