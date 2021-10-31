package Algorithm;

import com.sun.deploy.util.StringUtils;

import java.util.*;

public class RoutingTable {

    //Confirmed routing table entries
    private final HashMap<String, RoutingTableEntry> routingTableConfirmed; //<destinationId, entry>
    //Candidate elements in the middle step of Dijkstra algorithm
    private final TreeSet<RoutingTableEntry> routingTableTentative;
    //The node ID of current router, hence source node
    private final String nodeId;

    public RoutingTable(Map<String, Map<String, Integer>> LSPs, String nodeId) {
        this.routingTableConfirmed = new HashMap<>();
        this.routingTableTentative = new TreeSet<>();
        this.nodeId = nodeId;
        initialize(LSPs, nodeId);
    }

    /**
     * Initialize the routing table
     * @param LSPs the adjacency list used to generate routing table
     * @param nodeId the node ID to be assigned to the router of current routing table
     * @throws RuntimeException when no node ID was assigned to the router of current routing table
     */
    public void initialize(Map<String, Map<String, Integer>> LSPs, String nodeId) throws RuntimeException{
        this.routingTableConfirmed.clear();
        this.routingTableTentative.clear();
        Utils.log("Algorithm", "Routing table has been cleared");
        if(LSPs.get(nodeId)==null){
            Utils.log("Notice", "Please set source node ID");
            return;
        }else {
            try {
                LSPs.get(nodeId).forEach((K, V) -> {
                    routingTableTentative.add(new RoutingTableEntry(K, V, K, new ArrayList<>()));
                });
                Utils.log("Algorithm", "Routing Table initialized at node ID=" + this.nodeId + "!");
            } catch (NullPointerException np) {
                Utils.log("Error", "Initializing routing table with null parameters!");
            }
        }
    }

    public int buildRoutingTable(Map<String, Map<String, Integer>> LSPs){
        return buildRoutingTable(LSPs, true);
    }
    public int buildRoutingTable(Map<String, Map<String, Integer>> LSPs, boolean log) {
        int result=0;
        while(true){
            if((result=buildRoutingTableStep(LSPs, log))==0)
                break;
        }
        return result;
    }
    public int buildRoutingTableStep(Map<String, Map<String, Integer>> LSPs){
        return buildRoutingTableStep( LSPs, true);
    }

    /**
     * A single step of building the routing table
     * @param LSPs the adjacency list used to generate routing table
     * @param log whether to print log
     * @return 0 if routing table has already been constructed; 1 if current step is successful; -1 if the routing table length is incorrectly larger than adjacency list length
     */
    public int buildRoutingTableStep(Map<String, Map<String, Integer>> LSPs, boolean log){
        if(this.routingTableConfirmed.size()<LSPs.size()){
            RoutingTableEntry entry;//tentative里全是到本node的距离，找最小的输出
            do{
                if ((entry = routingTableTentative.pollFirst()) == null) {//清空了tentative，build就结束了
                    if(log) {
                        Utils.log("Algorithm", "Routing Table Constructed!");
                    }
                    return 0;
                }
            }while((routingTableConfirmed.get(entry.getDestId())!=null) && (routingTableConfirmed.get(entry.getDestId()).getDist()<=entry.getDist()));//若选出来的没有已confirm的好，就继续选大一点的
            if(log) {
                Utils.log("Algorithm", String.format("Found %s: Path %s  Cost: %d", entry.getDestId(), entry.getMidwaysAsString(), entry.getDist()));
            }

            routingTableConfirmed.put(entry.getDestId(), entry);
            RoutingTableEntry entryFinal = entry;
            try {
                LSPs.get(entryFinal.getDestId()).forEach((K, V) -> {//把新confirm的节点的邻居加到tentative里，dist直接计算为到本node的距离
                    int dist = V + routingTableConfirmed.get(entryFinal.getDestId()).getDist();
                    routingTableTentative.add(new RoutingTableEntry(entryFinal.getDestId(), dist, K, entryFinal.getMidways()));
                });
            }catch(NullPointerException e){
                Utils.log("Error", "Please write the packets in bi-direction format!");
                return 0;
            }
            integrateDistance();
            return 1;
        }else{
            return -1;
        }
    }
    /**
     * In a middle step of Dijkstra Algorithm, drop unreasonable candidate from tentative set
     */
    private void integrateDistance(){
        HashMap<String, RoutingTableEntry> bestRoutes = new HashMap<>();
        routingTableTentative.forEach(entry->{
            RoutingTableEntry confirmedPredecessor =  routingTableConfirmed.get(entry.byId);
            if (confirmedPredecessor!=null) {//confirm过这个entry的前继节点
                {
                    if (!entry.getDestId().equals(this.nodeId)) {//自己到自己的entry没用，跳过
                        if (bestRoutes.get(entry.getDestId()) != null) {//如果这次的tentative里已经出现过这个dest
                            if (entry.getDist() < bestRoutes.get(entry.getDestId()).getDist()) {//如果比tentative里已经出现过的的路线短就换掉
                                bestRoutes.remove(entry.getDestId());
                                bestRoutes.put(entry.getDestId(), entry.setById(confirmedPredecessor.getById()));
                            }
                        } else {//这次的tentative里这个destination就这一个， 不动他
                            bestRoutes.put(entry.getDestId(), entry.setById(confirmedPredecessor.getById()));
                        }
                    }
                }
            }else{//confirm的列表里没有路线能到这个entry的起点，所以把这个entry原样放进tentative里面，因为这算是个起始情况（这个entry一定是source到他的某个邻居），没法用泛用的confirmedPredecessor.getById()来算byId
                if (bestRoutes.get(entry.getDestId())==null || bestRoutes.get(entry.getDestId()).getDist()>entry.getDist()){
                    bestRoutes.put(entry.getDestId(), entry);
                }
            }
        });
        routingTableTentative.clear();
        bestRoutes.forEach((destID, entry)->{
            routingTableTentative.add(entry);
        });
    }

    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        this.routingTableConfirmed.forEach((destId, entry)->{
            stringBuilder.append("(").append(destId).append(",").append(entry.getDist()).append(",").append(entry.getById()).append(")");
        });
        stringBuilder.append("------");
        this.routingTableTentative.forEach(entry->{
            stringBuilder.append("(").append(entry.destId).append(",").append(entry.getDist()).append(",").append(entry.getById()).append(")");
        });
        return stringBuilder.toString();
    }

    private class RoutingTableEntry implements Comparable<RoutingTableEntry>{
        //To reach the destination, which router should the source node choose at the first hop
        private String byId;
        //Distance from source to destination
        private int dist;
        //Destination Node ID
        private String destId;
        //Path from source to destination
        private List<String> midways;
        RoutingTableEntry(String byId, int dist, String destId, List<String> midways){
            this.byId = byId;
            this.dist = dist;
            this.destId = destId;
            this.midways=new ArrayList<>();
            this.midways.addAll(midways);
            this.midways.add(destId);
        }

        public RoutingTableEntry setById(String byId) {
            this.byId = byId;
            return this;
        }
        public RoutingTableEntry setDestId(String destId) {
            this.destId = destId;
            return this;
        }
        public RoutingTableEntry setDist(int dist) {
            this.dist = dist;
            return this;
        }
        public String getById(){
            return byId;
        }
        public int getDist() {
            return dist;
        }
        public String getDestId() {
            return destId;
        }

        public List<String> getMidways(){
            return this.midways;
        }
        public String toString(){
            return "(" + destId + "," + dist + byId + ")";
        }

        public String getMidwaysAsString(){
            return StringUtils.join(this.midways, ">");
        }
        @Override
        public int compareTo(RoutingTableEntry o) {
            int result = Integer.compare(this.dist, ((RoutingTableEntry) o).getDist());
            return result==0?1:result;
        }
    }
}
