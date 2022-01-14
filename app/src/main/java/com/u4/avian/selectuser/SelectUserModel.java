package com.u4.avian.selectuser;

public class SelectUserModel {
    private String userId;
    private String userName;
    private String photoName;

    public SelectUserModel(String userId, String userName, String photoName) {
        this.userId = userId;
        this.userName = userName;
        this.photoName = photoName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPhotoName() {
        return photoName;
    }

    public void setPhotoName(String photoName) {
        this.photoName = photoName;
    }
}
