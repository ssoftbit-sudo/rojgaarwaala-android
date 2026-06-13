package com.srijeesolution.rojgaarwaala.utils.sp

object SharedPrefsConstant {
    const val USER_AUTH_TOKEN = "user_auth_token"
    const val USER_LOGGED_IN_STATUS = "user_logged_in_status"
    const val USER_SKIP_STATUS = "user_skip_status"
    const val USER_LOGIN_SKIP_STATUS = "user_log_in_skip_status"
    const val FCM_TOKEN = "fcm_token"
    /** Set when a push is shown; cleared when user taps the bell in MainActivity. */
    const val NOTIFICATION_BADGE_PENDING = "notification_badge_pending"
    /** Persisted district / location label for home filters. */
    const val HOME_SELECTED_LOCATION = "home_selected_location"
    /** Anonymous device id for story seen state. */
    const val DEVICE_KEY = "device_key"
}