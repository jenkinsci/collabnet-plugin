/*
 * Copyright 2013 CollabNet, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hudson.plugins.collabnet.orchestrate;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.Project;
import hudson.model.listeners.ItemListener;
import hudson.util.DescribableList;

import java.util.logging.Logger;

/**
 * Listener class for clearing the source association key of new jobs
 * when needed.
 */
@Extension
public class JobCreationListener extends ItemListener {

    private Logger log = Logger.getLogger("JobCreationListener");

    /**
     * Receives notification that an item has been created and
     * clears the Source Association Key from the Orchestrate
     * BuildNotifier of the target
     * @param newItem the new job
     */
    @Override
     public void onCreated(Item newItem) {
        // ignore everything except projects
        if (!(newItem instanceof Project)) return;

        DescribableList publishersList = ((Project) newItem).getPublishersList();
        BuildNotifier notifier = (BuildNotifier) publishersList.get(BuildNotifier.class);
        if (notifier != null) notifier.setSourceKey(null);
    }
}
