package com.dev.sbbooknetwork.setting;


import com.dev.sbbooknetwork.user.User;
import com.dev.sbbooknetwork.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SettingServiceImpl implements SettingService{

    private final UserService userService;
    @Override
    public List<User> getUsersByRole(String roleName) {

        return userService.getUsersByRole(roleName);
    }

}
