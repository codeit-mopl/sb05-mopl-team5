package com.mopl.api.domain.user.repository.impl;

import com.mopl.api.domain.user.entity.User;
import java.util.List;

public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    @Override
    public List<User> tempUserPage(String var) {
        return List.of();
    }
}