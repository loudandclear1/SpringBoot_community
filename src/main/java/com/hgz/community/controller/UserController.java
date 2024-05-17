package com.hgz.community.controller;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.CannedAccessControlList;
import com.hgz.community.annotation.LoginRequired;
import com.hgz.community.dao.UserMapper;
import com.hgz.community.entity.User;
import com.hgz.community.service.FollowService;
import com.hgz.community.service.LikeService;
import com.hgz.community.service.UserService;
import com.hgz.community.util.CommunityConstant;
import com.hgz.community.util.CommunityUtil;
import com.hgz.community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Value("${aliyun.oss.file.keyId}")
    private String accessKey;

    @Value("${aliyun.oss.file.keySecret}")
    private String secretKey;

    @Value("${aliyun.oss.file.bucketName}")
    private String headerBucketName;

    @Value("${aliyun.oss.file.url}")
    private String headerBucketUrl;

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
        String userPassword = user.getPassword();
        String salt = user.getSalt();
        String oldPassword_Salt = oldPassword + salt;
        String oldPassword_md5 = CommunityUtil.md5(oldPassword_Salt);

        if (oldPassword_md5 != null && !oldPassword_md5.equals(userPassword)) {
            model.addAttribute("oldPasswordMsg", "原始密码不正确！");
            return "/site/setting";
        }

        userService.updatePassword(user.getId(), newPassword, salt);
        userService.logout(ticket);

        return "redirect:/login";
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
}
