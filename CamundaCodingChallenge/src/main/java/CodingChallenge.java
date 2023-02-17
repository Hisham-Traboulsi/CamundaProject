import org.camunda.bpm.engine.impl.util.json.JSONException;
import org.camunda.bpm.engine.impl.util.json.JSONObject;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

public class CodingChallenge {

    private Stack<FlowNode> stack = new Stack<>();
    private ArrayList<FlowNode> visited = new ArrayList<>();

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Too run this program you need 2 String values: 1. Start Node ID and 2. End Node ID");
            System.exit(-1);
        }
        String startFlowID = args[0];
        String endFlowID = args[1];

        CodingChallenge codingChallenge = new CodingChallenge();

        BpmnModelInstance modelInstance = codingChallenge.getBPMNGraph();

        FlowNode startingNode = codingChallenge.getFlowNode(modelInstance, startFlowID);

        FlowNode endNode = codingChallenge.getFlowNode(modelInstance, endFlowID);

        codingChallenge.stack.push(startingNode);
        codingChallenge.findPath(startingNode, endNode, startingNode);

        codingChallenge.printPath(startFlowID, endFlowID);
    }

    public BpmnModelInstance getBPMNGraph() {
        JSONObject jsonObject;
        String bpmnGraph = "";
        try {
            String response = "";
            URL url = new URL("https://n35ro2ic4d.execute-api.eu-central-1.amazonaws.com/prod/engine-rest/process-definition/key/invoice/xml");
            Scanner sc = new Scanner(url.openStream());
            while (sc.hasNext()) {
                response += sc.nextLine();
            }
            sc.close();
            jsonObject = new JSONObject(response);
            bpmnGraph = jsonObject.getString("bpmn20Xml");
        } catch (IOException ex) {
            System.out.println(ex);
            System.exit(-1);
        } catch (JSONException ex) {
            System.out.println(ex);
            System.exit(-1);
        }

        InputStream stream = new ByteArrayInputStream(bpmnGraph.getBytes(Charset.forName("UTF-8")));
        return Bpmn.readModelFromStream(stream);
    }

    public FlowNode getFlowNode(BpmnModelInstance modelInstance, String nodeID) {
        FlowNode flowNode = modelInstance.getModelElementById(nodeID);
        if (flowNode == null) {
            System.out.println("Node with Node ID " + nodeID + " cannot be found in given model");
            System.exit(-1);
        }
        return flowNode;
    }

    public boolean findPath(FlowNode node, FlowNode endNode, FlowNode parentNode) {
        if (endNode.getId().equals(node.getId())) {
            return true;
        } else if (node.getOutgoing().isEmpty() && !endNode.getId().equals(node.getId())) {
            stack.pop();
            return false;
        } else {
            Collection<FlowNode> flowNodes = getFlowingFlowNodes(node);

            Iterator<FlowNode> iterator = flowNodes.iterator();
            FlowNode flowNode;
            while (iterator.hasNext()) {
                flowNode = iterator.next();
                if (visited.contains(flowNode)) {
                    return false;
                }

                stack.push(flowNode);
                visited.add(flowNode);

                if (findPath(flowNode, endNode, node)) {
                    return true;
                } else {
                    if (iterator.hasNext() && !stack.contains(flowNode)) {
                        stack.push(parentNode);
                    }
                    stack.pop();
                }
            }
        }
        return false;
    }

    public Collection<FlowNode> getFlowingFlowNodes(FlowNode node) {
        Collection<FlowNode> followingFlowNodes = new ArrayList<FlowNode>();
        for (SequenceFlow sequenceFlow : node.getOutgoing()) {
            followingFlowNodes.add(sequenceFlow.getTarget());
        }
        return followingFlowNodes;
    }

    public void printPath(String startFlowID, String endFlowID) {
        if (stack.isEmpty()) {
            System.out.println("No path found in given model between the Start Node ID: " + startFlowID + " and End Node ID: " + endFlowID);
            System.exit(-1);
        }
        System.out.println("The path from " + startFlowID + " to " + endFlowID + " is:");
        String path = "[";
        Iterator<FlowNode> iterator = stack.iterator();
        while (iterator.hasNext()) {
            path += iterator.next().getId();
            if (iterator.hasNext())
                path += ", ";
        }
        path += "]";
        System.out.println(path);
    }
}
