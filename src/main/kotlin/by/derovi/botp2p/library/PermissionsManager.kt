package by.derovi.botp2p.library

class PermissionsManager {
    class Role(
        val name: String
    ) {
        val parents = mutableListOf<String>()
        private val rawPermissions = mutableListOf<String>()

        fun permit(permission: String) {
            rawPermissions.add(permission)
        }
        fun forbid(permission: String) {
            rawPermissions.add("-$permission")
        }
    }

    fun hasPermission(role: String, permission: String): Boolean {
        return false
    }

    class PermissionEventListener(permission: String, runnable: () -> Unit)

    fun roleChanged(
        previous: String,
        current: String,
        gotPermissionListeners: List<PermissionEventListener>,
        loosePermissionListener: List<PermissionEventListener>
    ) {

    }
}
