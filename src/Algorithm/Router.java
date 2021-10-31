package Algorithm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Router {
    //Link state packets received, stored in adjacency list
    public Map<String, Map<String, Integer>> LSPs;
    //The ID of current source node
    private String nodeId;
    //The routing table of this router
    RoutingTable routingTable;
    //Whether there's packets for calculation
    private boolean LSPSettled = false;
    //When router is initialized without explicit ID, set this to be the ID temporarily
    public static final String UNSET_ID = "$UNSET";

    /**
     * Create a router with Link State packets in a single string, and, the node ID of this router
     * @param LSPs Link State packets in a single string
     * @param nodeId the node ID of this router
     */
    public Router(String LSPs, String nodeId) {
       initializeRouter(nodeId);
       decodePackets(LSPs);
       this.LSPSettled = true;
       initializeRoutingTable();
    }

    /**
     * Create a router with Link State packets in a file, and, the node ID of this router
     * @param LSPs File stores the Link State packets
     * @param nodeId the node ID of this router
     */
    public Router(File LSPs, String nodeId){
        initializeRouter(nodeId);
        try (BufferedReader reader = new BufferedReader(new FileReader(LSPs))) {
            String packet;
            while ((packet = reader.readLine()) != null)
                addPacket(packet.trim());
            Utils.log("Algorithm", "Link State packets settled!");
            this.LSPSettled = true;
        } catch (IOException e) {
            Utils.log("Error", "Cannot read from file: "+LSPs.getAbsolutePath());
            return;
        }
       initializeRoutingTable();
    }

    /**
     * Override hashCode function
     * @return the hashcode of the router ID
     */
    public int hashCode(){
        return nodeId.hashCode();
    }

    /**
     * Initialize the router with resetting everything and a new node ID
     * @param nodeId the node ID assigned to this router
     */
    private void initializeRouter(String nodeId){
        if(this.LSPs==null) {
            this.LSPs = new HashMap<>();
        }else{
            this.LSPs.clear();
        }
        this.LSPSettled=false;
        this.nodeId = nodeId;
    }

    /**
     * Initialize or reset the routing table
     */
    public void initializeRoutingTable(){
        if(!this.LSPSettled) {
            Utils.log("Error", "Initializing routing table with empty Link State packets!");
        }else {
            this.routingTable = new RoutingTable(this.LSPs, this.nodeId);
        }
    }

    /**
     * Decode the Link State packets into an adjacency list
     * @param packets the Link State packets received
     */
    public void decodePackets(String packets) {
        this.LSPs.clear();
        String[] packetList = packets.split("\n");
        String nodeId = "";
        for (String packet : packetList) {
            addPacket(packet.trim());
        }
        this.LSPSettled = true;
        Utils.log("Algorithm", "Link State packets settled in router!!");
    }

    /**
     * Single step of the above decoder
     * @param packet A single packet from anode
     * @param storage the adjacency list to store
     * @return the host that sent the decoded packet
     */
    static private String addPacket(String packet, Map<String, Map<String, Integer>> storage){
        String nodeId = packet.split(":", 2)[0].trim();
        storage.put(nodeId, new HashMap<String, Integer>());
        String[] distInfoBuffer;
        for (String distInfo : packet.split(":", 2)[1].trim().split(" ")) {
            distInfoBuffer = distInfo.split(":");
            storage.get(nodeId).put(distInfoBuffer[0], Integer.parseInt(distInfoBuffer[1]));
        }
        return nodeId;
    }
    private String addPacket(String packet){
        return addPacket(packet, this.LSPs);
    }

    /**
     * Encode the adjacency list to string format for reading
     * @return Adjacency list in string format
     */
    public String encodePackets() {
        StringBuilder stringBuilder = new StringBuilder();
        LSPs.forEach((K,V)->{
            stringBuilder.append(K).append(": ");
            V.forEach((k,v)->{
                stringBuilder.append(k).append(":").append(v).append(" ");
            });
            stringBuilder.replace(stringBuilder.length()-1, stringBuilder.length(),"\n");
        });
        return stringBuilder.toString();
    }

    public Router setNodeId(String nodeId){
        this.nodeId = nodeId;
        initializeRoutingTable();
        return this;
    }
    public void deleteNodeId(){
        this.nodeId = null;

    }
    /**
     * Build the routing table with current adjacency list
     */
    public void buildRoutingTable(){
        this.routingTable.buildRoutingTable(this.LSPs);
    }
    /**
     * Find the next node in Dijkstra Algorithm with the routing table with current adjacency list
     */
    public void buildRoutingTableStep(){ this.routingTable.buildRoutingTableStep(this.LSPs);}
}
