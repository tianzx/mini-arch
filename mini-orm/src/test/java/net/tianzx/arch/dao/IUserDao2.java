package net.tianzx.arch.dao;


import net.tianzx.arch.vo.User;

import java.util.List;

public interface IUserDao2 {
    User queryUserInfoById(Long userId);
    List<User> queryUserList(User user);
}
