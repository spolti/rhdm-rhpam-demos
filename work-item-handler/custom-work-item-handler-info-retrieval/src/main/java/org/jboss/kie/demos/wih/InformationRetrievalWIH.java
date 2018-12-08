package org.jboss.kie.demos.wih;

import org.jboss.kie.demos.functions.FunctionsHelper;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class InformationRetrievalWIH implements WorkItemHandler {

    private final Logger log = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());
    private FunctionsHelper fh = new FunctionsHelper();

    public void abortWorkItem(WorkItem workItem, WorkItemManager workItemManager) {
        log.warning("WIH " + workItem.getName() + " aborted.");
        workItemManager.abortWorkItem(workItem.getId());
    }

    public void executeWorkItem(WorkItem workItem, WorkItemManager workItemManager) {

        Map<String, Object> result = new HashMap<>();

        Object option = workItem.getParameter("Option");
        log.info("Option selected: " + option);

        switch (option.toString()) {
            case "user.name":
                result.put("Result", fh.systemUsername());
                break;

            case "user.home":
                result.put("Result", fh.systemUserHome());
                break;

            case "user.lang":
                result.put("Result", fh.systemUserCountryLang());
                break;

            case "java.version":
                result.put("Result", fh.javaVersion());
                break;

            default:
                log.warning("Option " + option + " not found. The valid options are [user.name, user.home, user.lang, java.version]");
                result.put("Result", "Option not found. The valid options are [user.name, user.home, user.lang, java.version]");
                break;
        }
        workItemManager.completeWorkItem(workItem.getId(), result);
    }

}