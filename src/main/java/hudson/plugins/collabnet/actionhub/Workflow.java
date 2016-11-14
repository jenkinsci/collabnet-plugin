package hudson.plugins.collabnet.actionhub;

import java.util.Map;

public class Workflow {
    String name;
    String id;
    String description;
    Map<String, WorkflowParameter> parameters;
    
    public Workflow(String name, String id, String description, Map<String, WorkflowParameter> parameters) {
        this.name = name;
        this.id = id;
        this.description = description;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, WorkflowParameter> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, WorkflowParameter> parameters) {
        this.parameters = parameters;
    }
}
