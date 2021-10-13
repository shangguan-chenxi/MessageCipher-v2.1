package chenxi.shangguan.messagecipher

import android.app.Activity
import android.app.Application

class ExitApplication private constructor() : Application() {
    private val list: MutableList<Activity> = ArrayList()
    fun addActivity(activity: Activity) {
        list.add(activity)
    }

    fun exit() {
        for (activity in list) {
            activity.finish()
        }
        System.exit(0)
    }

    companion object {
        var instance: ExitApplication? = null
            get() {
                if (field == null) {
                    field = ExitApplication()
                }
                return field
            }
            private set
    }
}