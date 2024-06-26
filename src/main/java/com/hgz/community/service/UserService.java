package com.hgz.community.service;

import com.hgz.community.dao.UserMapper;
import com.hgz.community.entity.LoginTicket;
import com.hgz.community.entity.User;
import com.hgz.community.util.CommunityConstant;
import com.hgz.community.util.CommunityUtil;
import com.hgz.community.util.MailClient;
import com.hgz.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private RedisTemplate redisTemplate;

    public User findUserById(int id) {
        User user = getCache(id);
        if (user == null) {
            user = initCache(id);
        }
        return user;
    }

    public User findUserByName(String username) {
        return userMapper.selectByName(username);
    }

    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>();

        if (user == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空!");
            return map;
        }

        User u = userMapper.selectByName(user.getUsername());
        if (u != null) {
            map.put("usernameMsg", "该账号已存在!");
            return map;
        }

        u = userMapper.selectByEmail(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "该邮箱已被注册!");
            return map;
        }

        // 注册用户
        user.setSalt(CommunityUtil.generationUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generationUUID());
        user.setHeaderUrl("https://hgzcommunity.oss-cn-beijing.aliyuncs.com/unknown.jpg");
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        // 发送激活账号邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        String url = domain + contextPath + "/activation/" + user.getId() + '/' + user.getActivationCode();
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "激活账号", content);

        return map;
    }

    public int activation(int userId, String code) {
        // 注册后用户数据未存入redis
        // 而且激活要修改数据，会删除redis中的用户数据，没必要从redis中查找
        User user = userMapper.selectById(userId);
        if (user.getStatus() == 1) {
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {
            userMapper.updateStatus(userId, 1);
            return ACTIVATION_SUCCESS;
        } else {
            return ACTIVATION_FAILURE;
        }
    }

    public Map<String, Object> login(String username, String password, int expiredSeconds) {
        Map<String, Object> map = new HashMap<>();

        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空！");
            return map;
        }

        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }

        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "该账号不存在！");
            return map;
        }

        if (user.getStatus() == 0) {
            map.put("usernameMsg", "该账号未激活！");
            return map;
        }

        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码不正确！");
            return map;
        }

        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generationUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000L));

        // redis存储 loginTicket json
        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey, loginTicket);

        map.put("ticket", loginTicket.getTicket());
        return map;
    }

    public void logout(String ticket) {
        if (StringUtils.isBlank(ticket)) {
            throw new IllegalArgumentException("ticket不能为空");
        }
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        if (loginTicket == null) {
            throw new IllegalStateException("找不到对应的ticket或ticket已过期");
        }
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey, loginTicket);
    }

    public LoginTicket findLoginTicket(String ticket) {
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
    }

    public int updateHeader(int userId, String headerUrl) {
        int rows = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return rows;
    }

    public Map<String, Object> updatePassword(User user, String oldPassword, String newPassword) {
        Map<String, Object> result = new HashMap<>();

        String oldPassword_md5 = CommunityUtil.md5(oldPassword + user.getSalt());
        if (!Objects.equals(oldPassword_md5, user.getPassword())) {
            result.put("oldPasswordMsg", "旧密码不正确！");
            return result;
        }

        String newPassword_md5 = CommunityUtil.md5(newPassword + user.getSalt());
        userMapper.updatePassword(user.getId(), newPassword_md5);
        clearCache(user.getId());
        return result;
    }

    public void forgetPassword(User user, String newPassword) {
        String newPassword_md5 = CommunityUtil.md5(newPassword + user.getSalt());
        userMapper.updatePassword(user.getId(), newPassword_md5);
    }

    // 1.优先查询缓存cache
    private User getCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }

    // 2.cache未命中时，初始化缓存
    private User initCache(int userId) {
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }

    // 3.用户数据更改时，清除缓存
    private void clearCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }

    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        User user = findUserById(userId);

        if (user == null) {
            throw new RuntimeException("用户不存在！");
        }

        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()) {
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });

        return list;
    }

    public User getUserByEmail(String email) {
        return userMapper.selectByEmail(email);
    }

    public void sendForgetEmail(String email, String code) {

        // 发送激活账号邮件
        Context context = new Context();
        context.setVariable("email", email);
        context.setVariable("code", code);
        String content = templateEngine.process("/mail/forget", context);
        mailClient.sendMail(email, "忘记账号密码", content);

    }
}
