package hudson.plugins.collabnet.actionhub;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkflowParameter {
    String description;
    boolean required;
    String type;

    @JsonProperty("default")
    String default_val;

    @JsonProperty("enum")
    String[] enum_val;

    public WorkflowParameter(String description, String type, String default_val, String[] enum_val) {
        this.description = description;
        this.required = false; //satisfies the workflow contract. jenkins has no concept of a required parameter. 
        this.type = type;
        this.default_val = default_val;
        this.enum_val = enum_val.clone();
    }

    
    // writing custom getter and setter for the enum_val variable
    // because we need specific behavior that is not provided by Lombok
    public String[] getEnum_val() {
        return enum_val.clone();
    }

    public void setEnum_val (String[] enum_val) {
        this.enum_val = enum_val.clone();
    }
    
}
