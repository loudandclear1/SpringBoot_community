package com.hgz.community;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class QuartzTests {

    @Autowired
    private Scheduler scheduler ;

    @Test
    public void testDeleteJob() {
        boolean result = false;
        try {
            result = scheduler.deleteJob(new JobKey("testJob", "testJobGroup"));
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
        System.out.println(result);
    }

}
