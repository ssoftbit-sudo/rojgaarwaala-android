package com.srijeesolution.rojgaarwaala.network.constant

import retrofit2.http.DELETE

class NetworkConstants {
    companion object {
        const val ON_LOGIN = "login"
        const val ON_REGISTER = "register"
        const val ON_LOGOUT = "logout"
        const val HOMEPAGE_DATA = "home"
        const val GET_PROFILE = "user"
        const val UPDATE_PROFILE = "profile/update"
        const val JOB_SUBMIT = "job/add"
        const val SEND_OTP = "send-otp"
        const val VERIFY_OTP = "verify-otp"
        const val CATEGORIES = "categories"
        const val CATEGORIES_LIST = "category-list"
        const val VIDEO_DETAILS = "video/{id}"
        const val JOB_LIST = "job/list"
        const val CATEGORY_VIDEOS = "category/{id}/videos"
        const val VIDEO_LIKE = "video/like"
        const val VIDEO_UNLIKE = "video/unlike"
        const val VIDEO_INCREMENT_VIEW = "video/increment-view"
        const val JOB_DELETE = "job/delete/{id}"
        const val JOB_UPDATE = "job/update/{id}"
        const val SCHEDULED_IMAGES_GROUPED = "scheduled-images/grouped"
    const val SCHEDULED_IMAGES = "scheduled-images"
    }
}