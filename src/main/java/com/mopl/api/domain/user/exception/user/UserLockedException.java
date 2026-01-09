package com.mopl.api.domain.user.exception.user;


public class UserLockedException extends UserException {

    public UserLockedException() {super(UserErrorCode.USER_LOCKED);}

    public static UserLockedException withUserEmail(String email) {
        UserLockedException exception = new UserLockedException();
        exception.addDetail("email", email);
        return exception;
    }
}
