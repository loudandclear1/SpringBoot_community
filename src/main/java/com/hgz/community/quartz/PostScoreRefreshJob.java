package com.hgz.community.quartz;

import com.hgz.community.entity.DiscussPost;
import com.hgz.community.service.DiscussPostService;
import com.hgz.community.service.ElasticsearchService;
import com.hgz.community.service.LikeService;
import com.hgz.community.util.CommunityConstant;
import com.hgz.community.util.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class PostScoreRefreshJob implements Job, CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    private static final Date epoch;

    static {
        try {
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2024-04-05 00:00:00");
        } catch (ParseException e) {
            logger.error("初始化社区纪元失败！", e);
            throw new RuntimeException("初始化社区纪元失败！", e);
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String redisKey = RedisKeyUtil.getPostScoreKey();
        BoundSetOperations operations = redisTemplate.boundSetOps(redisKey);

        if (operations.size() == 0) {
            logger.info("[任务取消] 没有需要刷新的帖子！");
            return;
        }

        logger.info("[任务开始] 正在刷新帖子分数：" + operations.size());

        while (operations.size() > 0) {
            this.refresh((Integer) operations.pop());
        }

        logger.info("[任务结束] 帖子分数刷新完毕！");
    }

    private void refresh(int postId) {
        DiscussPost post = discussPostService.findDiscussPostById(postId);

        if (post == null) {
            logger.error("该帖子不存在： id = " + postId);
            return;
        }

        boolean wonderful = post.getStatus() == 1;
        int commentCount = post.getCommentCount();
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, postId);

        double w = (wonderful ? 100 : 0) + commentCount * 5L + likeCount * 2;
        double score = Math.log(Math.max(w, 1))
                + (double) (post.getCreateTime().getTime() - epoch.getTime()) / (1000 * 3600 * 24);

        discussPostService.updateScore(postId, score);
        post.setScore(score);
        elasticsearchService.saveDiscussPost(post);
    }
}
