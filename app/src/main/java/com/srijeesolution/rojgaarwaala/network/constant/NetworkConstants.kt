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
    }
}