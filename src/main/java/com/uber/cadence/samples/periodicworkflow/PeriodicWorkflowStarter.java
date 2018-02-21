/*
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package com.uber.cadence.samples.periodicworkflow;

import com.uber.cadence.WorkflowExecution;
import com.uber.cadence.WorkflowIdReusePolicy;
import com.uber.cadence.WorkflowService;
import com.uber.cadence.client.WorkflowClient;
import com.uber.cadence.client.WorkflowExecutionAlreadyStartedException;
import com.uber.cadence.client.WorkflowOptions;
import com.uber.cadence.samples.common.ConfigHelper;

public class PeriodicWorkflowStarter {

    private static WorkflowService.Iface swfService;

    private static String domain;

    public static void main(String[] args) throws Exception {

        // Load configuration
        ConfigHelper configHelper = ConfigHelper.createConfig();

        // Create the client for Simple Workflow Service
        swfService = configHelper.createWorkflowClient();
        domain = configHelper.getDomain();

        WorkflowClient client = WorkflowClient.newInstance(swfService, domain);


        // Execute activity every two 10 seconds, wait for it to complete before starting the new one, 
        // create new run every 30 seconds and stop the workflow after two minutes.
        // Obviously these periods are so low to make example run fast enough to not be boring.
        // In production case there is no need to create new runs so frequently.
        PeriodicWorkflowOptions options = new PeriodicWorkflowOptions();
        options.setExecutionPeriodSeconds(10);
        options.setContinueAsNewAfterSeconds(30);
        options.setCompleteAfterSeconds(120);
        options.setWaitForActivityCompletion(true);

        String activityType = "PeriodicWorkflowActivities::doSomeWork";
        Object[] parameters = new Object[]{"parameter1"};

        String workflowId = "Periodic";
        try {
            WorkflowOptions so = new WorkflowOptions.Builder()
                    .setWorkflowId(workflowId)
                    .setTaskList(PeriodicWorkflowWorker.TASK_LIST)
                    .setExecutionStartToCloseTimeoutSeconds(300)
                    .setTaskStartToCloseTimeoutSeconds(3)
                    .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.AllowDuplicate)
                    .build();
            // Passing instance id to ensure that only one periodic workflow can be active at a time.
            // Use different id for each schedule.
            PeriodicWorkflow workflow = client.newWorkflowStub(PeriodicWorkflow.class, so);
            WorkflowExecution workflowExecution = WorkflowClient.asyncStart(workflow::startPeriodicWorkflow,
                    activityType, parameters, options);

            System.out.println("Started periodic workflow with workflowId=\"" + workflowExecution.getWorkflowId()
                    + "\" and runId=\"" + workflowExecution.getRunId() + "\"");
        } catch (WorkflowExecutionAlreadyStartedException e) {
            // It is expected to get this exception if start is called before workflow run is completed.
            System.out.println("Periodic workflow with workflowId=\"" + workflowId
                    + " is already running");
        }
        System.exit(0);
    }
}