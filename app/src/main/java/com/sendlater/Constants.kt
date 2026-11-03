package com.sendlater

object Constants {
    const val EXTRA_ID = "message_id"            // alarm -> receiver / wake activity
    const val EXTRA_SEND_ID = "send_message_id"  // notification tap -> MainActivity
    const val EXTRA_SKIP_INTRO = "skip_intro"    // don't replay the opening when we return after sending
}
