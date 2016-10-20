package hudson.plugins.collabnet.actionhub;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nshah on 11/8/2016.
 */
public class Util {

    public static boolean respondsTo(String radioButtonState,
                                     boolean manualMessageCheckBox,
                                     boolean buildMessageCheckBox,
                                     boolean commitMessageCheckBox,
                                     boolean workitemMessageCheckBox,
                                     boolean reviewMessageCheckBox,
                                     boolean customMessageCheckBox,
                                     String customMessageString,
                                     String requestMessage) {

        if (radioButtonState.equals(Constants.RadioButtonState.ALL)) {
            return true;
        } else if (radioButtonState.equals(Constants.RadioButtonState.NONE)) {
            return false;
        } else if (radioButtonState.equals(Constants.RadioButtonState.CUSTOM)) {

            List<String> messages = new ArrayList<String>();

            if (manualMessageCheckBox) {
                messages.add(Constants.ActionMessageType.MANUAL);
            }

            if (buildMessageCheckBox) {
                messages.add(Constants.ActionMessageType.BUILD);
            }

            if (reviewMessageCheckBox) {
                messages.add(Constants.ActionMessageType.REVIEW);
            }

            if (commitMessageCheckBox) {
                messages.add(Constants.ActionMessageType.COMMIT);
            }

            if (workitemMessageCheckBox) {
                messages.add(Constants.ActionMessageType.WORKITEM);
            }

            if (customMessageCheckBox) {

                customMessageString = customMessageString.trim();
                String[] customMessageTypes = new String[0];
                if (customMessageString != null && customMessageString.length()>0) {
                    customMessageTypes = customMessageString.split(",");
                }

                if (customMessageTypes.length > 0) {
                    for (int i =0; i < customMessageTypes.length; i++) {
                        messages.add(Constants.ActionMessageType.CUSTOM + "-" + customMessageTypes[i].trim().toUpperCase());
                    }
                } else {
                    messages.add(Constants.ActionMessageType.CUSTOM);
                }
            }

            if (messages.contains(requestMessage.toUpperCase())) {
                return true;
            }
        }

        return false;





    }


}
