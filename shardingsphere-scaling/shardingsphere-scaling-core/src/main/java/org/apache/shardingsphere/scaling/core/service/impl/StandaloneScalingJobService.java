/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.scaling.core.service.impl;

import org.apache.shardingsphere.scaling.core.config.JobConfiguration;
import org.apache.shardingsphere.scaling.core.exception.ScalingJobNotFoundException;
import org.apache.shardingsphere.scaling.core.job.JobContext;
import org.apache.shardingsphere.scaling.core.job.JobProgress;
import org.apache.shardingsphere.scaling.core.job.preparer.ScalingJobPreparer;
import org.apache.shardingsphere.scaling.core.job.task.inventory.InventoryTaskGroupProgress;
import org.apache.shardingsphere.scaling.core.job.task.inventory.InventoryTaskProgress;
import org.apache.shardingsphere.scaling.core.schedule.JobStatus;
import org.apache.shardingsphere.scaling.core.schedule.ScalingTaskScheduler;
import org.apache.shardingsphere.scaling.core.service.AbstractScalingJobService;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Standalone scaling job service.
 */
public final class StandaloneScalingJobService extends AbstractScalingJobService {
    
    private final Map<Long, JobContext> jobContextMap = new ConcurrentHashMap<>();
    
    private final Map<Long, ScalingTaskScheduler> scalingTaskSchedulerMap = new ConcurrentHashMap<>();
    
    private final ScalingJobPreparer scalingJobPreparer = new ScalingJobPreparer();
    
    @Override
    public List<JobContext> listJobs() {
        return new LinkedList<>(jobContextMap.values());
    }
    
    @Override
    public Optional<JobContext> start(final JobConfiguration jobConfig) {
        JobContext jobContext = new JobContext(jobConfig);
        if (jobContext.getTaskConfigs().isEmpty()) {
            return Optional.empty();
        }
        jobContextMap.put(jobContext.getJobId(), jobContext);
        scalingJobPreparer.prepare(jobContext);
        if (!JobStatus.PREPARING_FAILURE.name().equals(jobContext.getStatus())) {
            ScalingTaskScheduler scalingTaskScheduler = new ScalingTaskScheduler(jobContext);
            scalingTaskScheduler.start();
            scalingTaskSchedulerMap.put(jobContext.getJobId(), scalingTaskScheduler);
        }
        return Optional.of(jobContext);
    }
    
    @Override
    public void stop(final long jobId) {
        JobContext jobContext = getJob(jobId);
        scalingTaskSchedulerMap.get(jobId).stop();
        jobContext.setStatus(JobStatus.STOPPED.name());
    }
    
    @Override
    public JobContext getJob(final long jobId) {
        if (!jobContextMap.containsKey(jobId)) {
            throw new ScalingJobNotFoundException(String.format("Can't find scaling job id %s", jobId));
        }
        return jobContextMap.get(jobId);
    }
    
    @Override
    public JobProgress getProgress(final long jobId) {
        JobProgress result = new JobProgress(jobId, getJob(jobId).getStatus());
        if (scalingTaskSchedulerMap.containsKey(jobId)) {
            result.getInventoryTaskProgress().add(getInventoryTaskProgress(jobId));
            result.getIncrementalTaskProgress().addAll(scalingTaskSchedulerMap.get(jobId).getIncrementalTaskProgress());
        }
        return result;
    }
    
    private InventoryTaskGroupProgress getInventoryTaskProgress(final long jobId) {
        Collection<InventoryTaskProgress> inventoryTaskProgress = scalingTaskSchedulerMap.get(jobId).getInventoryTaskProgress();
        return new InventoryTaskGroupProgress("", inventoryTaskProgress.size(), (int) inventoryTaskProgress.stream().filter(InventoryTaskProgress::isFinished).count());
    }
    
    @Override
    public void remove(final long jobId) {
        stop(jobId);
        jobContextMap.remove(jobId);
        scalingTaskSchedulerMap.remove(jobId);
    }
}
