package com.hgz.community.controller;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.CannedAccessControlList;
import com.hgz.community.annotation.LoginRequired;
import com.hgz.community.entity.Comment;
import com.hgz.community.entity.DiscussPost;
import com.hgz.community.entity.Page;
import com.hgz.community.entity.User;
import com.hgz.community.service.*;
import com.hgz.community.util.CommunityConstant;
import com.hgz.community.util.CommunityUtil;
import com.hgz.community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private CommentService commentService;

    @Value("${aliyun.oss.file.keyId}")
    private String accessKey;

    @Value("${aliyun.oss.file.keySecret}")
    private String secretKey;

    @Value("${aliyun.oss.file.bucketName}")
    private String headerBucketName;

    @Value("${aliyun.oss.file.endPoint}")
    private String endPoint;

    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage() {
        return "/site/setting";
    }

    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model) {
        if (headerImage == null) {
            model.addAttribute("error", "您还没有选择图片！");
            return "/site/setting";
        }

        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "文件格式不正确！");
            return "/site/setting";
        }

        fileName = CommunityUtil.generationUUID() + suffix;
        OSS ossClient = new OSSClient(endPoint, accessKey, secretKey);
        try {
            InputStream inputStream = headerImage.getInputStream();
            ossClient.putObject(headerBucketName, fileName, inputStream);
            ossClient.setObjectAcl(headerBucketName, fileName, CannedAccessControlList.PublicRead);
        } catch (IOException e) {
            logger.error("上传文件失败：" + e.getMessage());
            throw new RuntimeException("上传文件失败，服务器发生异常！");
        }

        String headerUrl = "https://" + headerBucketName + "." + endPoint + "/" + fileName;
        userService.updateHeader(hostHolder.getUser().getId(), headerUrl);
        ossClient.shutdown();

        return "redirect:/index";
    }

    @LoginRequired
    @RequestMapping(path = "/changePassword", method = RequestMethod.POST)
    public String changePassword(String oldPassword, String newPassword, Model model, @CookieValue("ticket") String ticket) {
        if (StringUtils.isBlank(oldPassword)) {
            model.addAttribute("oldPasswordMsg", "原始密码不能为空！");
            return "/site/setting";
        }
        if (StringUtils.isBlank(newPassword)) {
            model.addAttribute("newPasswordMsg", "新密码不能为空！");
            return "/site/setting";
        }
        if (oldPassword.equals(newPassword)) {
            model.addAttribute("newPasswordMsg", "新密码不能和旧密码相同！");
            return "/site/setting";
        }

        User user = hostHolder.getUser();

        Map<String, Object> map = userService.updatePassword(user, oldPassword, newPassword);

        if (!map.isEmpty()) {
            for (Map.Entry<String, Object> m : map.entrySet()) {
                model.addAttribute(m.getKey(), m.getValue());
            }
            return "/site/setting";
        }

        model.addAttribute("msg", "您的账号密码已经修改成！");
        model.addAttribute("target", "/login");
        userService.logout(ticket);

        return "/site/operate-result";
    }

    // 个人主页 userId 不一定是自己的id，可能是看别人的个人主页
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在！");
        }

        // 用户
        model.addAttribute("user", user);
        // 点赞数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);

        // 关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);

        // 粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);

        // 是否已关注
        boolean hasFollowered = false;
        if (hostHolder.getUser() != null) {
            hasFollowered = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowered);

        return "/site/profile";
    }

    @RequestMapping(path = "/my-post/{userId}", method = RequestMethod.GET)
    public String getMyPostPage(@PathVariable("userId") int userId, Model model, Page page) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在！");
        }
        model.addAttribute("user", user);

        // 发帖数量统计
        int postCount = discussPostService.findDiscussPostRows(userId);
        model.addAttribute("postCount", postCount);

        page.setRows(postCount);
        page.setPath("/user/my-post/" + userId);
        page.setLimit(5);

        List<DiscussPost> postList = discussPostService.findDiscussPosts(userId, page.getOffset(), page.getLimit(), 0);
        List<Map<String, Object>> postVoList = new ArrayList<>();
        if (postList != null) {
            for (DiscussPost post : postList) {
                Map<String, Object> postVo = new HashMap<>();
                postVo.put("post", post);
                postVo.put("like", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));
                postVoList.add(postVo);
            }
        }

        model.addAttribute("postVoList", postVoList);

        return "/site/my-post";
    }

    @RequestMapping(path = "/my-reply/{userId}", method = RequestMethod.GET)
    public String getMyReplyPage(@PathVariable("userId") int userId, Model model, Page page) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在！");
        }
        model.addAttribute("user", user);

        // 查找用户的所有评论
        List<Comment> commentList = commentService.findCommentsByUserId(userId);
        // 返回给model的
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        // comment 中评论类型是帖子 post id集合
        Set<Integer> postIds = new HashSet<>();
        // 帖子id 和用户最新评论的映射关系
        Map<Integer, Comment> postLastComment = new HashMap<>();
        if (commentList != null) {
            for (Comment comment : commentList) {
                if (comment.getEntityType() == ENTITY_TYPE_POST) {
                    postIds.add(comment.getEntityId());
                    postLastComment.put(comment.getEntityId(), comment);
                }
            }
        }

        // 评论数量统计
        int commentCount = postIds.size();
        model.addAttribute("commentCount", commentCount);

        page.setRows(commentCount);
        page.setPath("/user/my-reply/" + userId);
        page.setLimit(5);

        if (postIds != null) {
            for (int id : postIds) {
                DiscussPost post = discussPostService.findDiscussPostById(id);
                if (post != null) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("post", post);
                    map.put("comment", postLastComment.get(id));
                    commentVoList.add(map);
                }
            }
        }
        model.addAttribute("commentVoList", commentVoList);

        return "/site/my-reply";
    }
}
