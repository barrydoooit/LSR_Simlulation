package sample;

import Algorithm.Router;
import Algorithm.Utils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Controller {
    private Router router;
    private String srcNodeId;
    File selectedFile;
    public Controller(){
    }
    public void initialize(){
        System.setOut(new PrintStream(new Console(this.textLog)));
    }
    @FXML
    private void btnAddNodeClicked(){//
        String addInput = textAddNode.getText().trim();
        try {
            textAddNode.clear();
            String nodeId = addInput.split(":", 2)[0];
            if(router.LSPs.containsKey(nodeId)){
                Utils.log("Error", "Node "+nodeId+" already exists in the node list! Try remove it first");
                return;
            }
            Map<String, Integer> temp = new HashMap<>();
            String[] distInfoBuffer;
            for (String distInfo : addInput.split(":", 2)[1].trim().split(" ")) {
                distInfoBuffer = distInfo.split(":");
                temp.put(distInfoBuffer[0], Integer.parseInt(distInfoBuffer[1]));
            }
            for (String key : router.LSPs.keySet()) {
                if (temp.get(key) != null) {
                    router.LSPs.get(key).put(nodeId, temp.get(key));
                }
            }
            router.LSPs.put(nodeId, temp);
            temp.forEach((K,V)->{
                if(!router.LSPs.containsKey(K)){
                    router.LSPs.put(K, new HashMap<>());
                    router.LSPs.get(K).put(nodeId, V);
                }
            });
            try {
                router.initializeRoutingTable();
            }catch(RuntimeException e){
                Utils.log("Error", e.getMessage());
            }
            BufferedWriter out = new BufferedWriter(new FileWriter("routes.lsa"));
            out.write(router.encodePackets());
            out.close();
            Utils.log("Controller", "Node "+addInput+" added into packet list");
            this.textTopo.setText(router.encodePackets());
        }catch(RuntimeException e){
            Utils.log("Error", "Cannot update the link state packets!");
        }
        catch (IOException e) {
            Utils.log("Error","Failed to write to designated file");
        }

    }
    @FXML
    private void btnRemoveNodeClicked(){
        String node = textRemoveNode.getText().trim();
        if(node.equals("")){
            return;
        }
        if(srcNodeId!=null && node.equals(srcNodeId)){
            Utils.log("Warning", "Please unset the source node before removing it!");
            return;
        }
        textRemoveNode.clear();
        try {
            router.LSPs.remove(node);
            for (String key : router.LSPs.keySet()) {
                if (router.LSPs.get(key).get(node) != null) {
                    router.LSPs.get(key).remove(node);
                }
                if(router.LSPs.get(key).size()==0){
                    router.LSPs.remove(key);
                }
            }
        }catch(Exception e){
            Utils.log("Error", e.getMessage());
            return;
        }
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("routes.lsa"));
            out.write(router.encodePackets());
            out.close();
            Utils.log("Controller", "Remove Node: "+node);
        } catch (IOException e) {
            Utils.log("Error","File writing failed");
        }
        router = new Router(selectedFile,srcNodeId);
        this.textSrcNode.setText(srcNodeId);
        this.textSrcNode.setEditable(false);
        this.btnSrcNode.setText("Reset");
        this.textTopo.setText(router.encodePackets());
    }
    @FXML
    private void btnBreakLinkClicked(){
        String node1 = textBreakLinkA.getText().trim();
        String node2 = textBreakLinkB.getText().trim();
        if(node1.equals("")||node2.equals("")){
            return;
        }
        textBreakLinkA.clear();
        textBreakLinkB.clear();
        try {
            router.LSPs.get(node1).remove(node2);
            if (router.LSPs.get(node1).size() == 0) {
                router.LSPs.remove(node1);
            }
            router.LSPs.get(node2).remove(node1);
            if (router.LSPs.get(node2).size() == 0) {
                router.LSPs.remove(node2);
            }
        }catch(Exception e){
            Utils.log("Error", "node not found");
            return;
        }
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("routes.lsa"));
            out.write(router.encodePackets());
            out.close();
            Utils.log("Controller", "Remove Link: "+node1+" to "+node2);
        } catch (IOException e) {
            Utils.log("Error","File writing fail!");
        }
        router = new Router(selectedFile,srcNodeId);
        this.textSrcNode.setText(srcNodeId);
        this.textSrcNode.setEditable(false);
        this.btnSrcNode.setText("Reset");
        this.textTopo.setText(router.encodePackets());
    }
    @FXML
    private void btnSrcNodeClicked(){
        if(btnSrcNode.getText().equals("Apply")){
            if(this.router!=null) {
                if (!(this.srcNodeId=textSrcNode.getText()).equals("")) {
                    try {
                        this.router.setNodeId(this.srcNodeId);
                        this.textSrcNode.setEditable(false);
                        this.btnSrcNode.setText("Reset");
                        Utils.log("Controller", "Source Node set to ID: " + this.srcNodeId);
                    }catch(RuntimeException e) {
                        Utils.log("Error", e.getMessage());
                        this.textSrcNode.setText("");
                    }
                }
            }else{
                Utils.log("Error", "Link State packets might have not been loaded!");
            }
        }else{
            this.textSrcNode.setEditable(true);
            this.btnSrcNode.setText("Apply");
            this.srcNodeId = null;
            this.textSrcNode.clear();
            Utils.log("Controller", "Source Node reset.");
            this.router.setNodeId(null);
        }
    }
    @FXML
    private void btnTestingClicked() {
        resetAll();
        selectedFile = new File("routes.lsa");
        try (BufferedReader reader = new BufferedReader(new FileReader(selectedFile))) {
            String packet;
            this.textSrcNode.setText("x");
            this.textSrcNode.setEditable(false);
            this.btnSrcNode.setText("Reset");
            this.srcNodeId="x";
            try {
                router = new Router(selectedFile, "x");
            }catch(Exception e){
                Utils.log("Error", e.getMessage());
                resetAll();
                return;
            }
            while ((packet = reader.readLine()) != null) {
                textTopo.appendText(packet.trim()+"\n");
            }
        }catch(IOException io){
            Utils.log("Error", "Cannot load testing file routes.lsa");
        }
    }
    @FXML
    private void btnLoadFileClicked(){
        resetAll();
        FileChooser fileChooser = new FileChooser();
        selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile!=null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(selectedFile))) {
                String packet;
                while ((packet = reader.readLine()) != null) {
                    textTopo.appendText(packet.trim()+"\n");
                }
                router =  new Router(selectedFile, null);
            }catch(IOException io){
                Utils.log("Error", "Cannot load selected file!");
            }catch(RuntimeException e){
                Utils.log("Error", e.getMessage());
            }
        }
    }
    @FXML
    private void btnTopoChangeClicked(){
        String packets = textTopo.getText();
        router =  new Router(packets, null);
        if(!router.LSPs.containsKey(srcNodeId)){
            this.srcNodeId = null;
            this.textSrcNode.setEditable(true);
            this.btnSrcNode.setText("Apply");
            this.textSrcNode.clear();
        }else{
            router.setNodeId(srcNodeId);
        }
    }
    @FXML
    private void btnSingleStepClicked(){
        if(srcNodeId!=null) {
            router.buildRoutingTableStep();
        }else{
            Utils.log("Error", "Source Node Id Remains Unset!");
        }
    }
    @FXML
    private void btnComputeAllClicked(){
        if(srcNodeId!=null) {
            router.buildRoutingTable();
        }else{
            Utils.log("Error", "Source Node Id Remains Unset!");
        }
    }
    private void resetAll(){
        this.router = null;
        this.srcNodeId = null;
        this.textSrcNode.setEditable(true);
        this.btnSrcNode.setText("Apply");
        this.textSrcNode.clear();
        this.textTopo.clear();
    }

    @FXML
    private TextField textAddNode;
    @FXML
    private TextField textRemoveNode;
    @FXML
    private TextField textBreakLinkA;
    @FXML
    private TextField textBreakLinkB;
    @FXML
    private TextField textSrcNode;
    @FXML
    private TextArea textTopo;
    @FXML
    private TextArea textLog;
    @FXML
    private Button  btnAddNode;
    @FXML
    private Button btnRemoveNode;
    @FXML
    private Button btnBreakLink;
    @FXML
    private Button btnSrcNode;
    @FXML
    private Button btnLoadFile;
    @FXML
    private Button btnTopoChange;
    @FXML
    private Button btnSingleStep;
    @FXML
    private Button btnComputeAll;
    @FXML
    private Parent root;

    private static class Console extends OutputStream {
        final private TextArea console;
        Console(TextArea console) {
            this.console=console;
        }
        private void appendText(String value){
            Platform.runLater(() -> console.appendText(value));
        }
        @Override
        public void write(int b) throws IOException {
            appendText(String.valueOf((char)b));
        }

    }
}