package com.dev.sbbooknetwork.setting;

import com.dev.sbbooknetwork.user.User;

import java.util.List;

public interface SettingService {

    List<User> getUsersByRole(String role);


}
