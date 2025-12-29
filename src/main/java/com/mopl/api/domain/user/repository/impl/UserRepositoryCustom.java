package com.mopl.api.domain.user.repository.impl;

import com.mopl.api.domain.user.entity.User;
import java.util.List;

public interface UserRepositoryCustom {

    List<User> tempUserPage(String var);
}