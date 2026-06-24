package xyz.xiao6.myboard.androidbridge

/**
 * 权限网关真实实现。
 * 阶段 07 简化版：通过 lambda 注入权限查询。
 */
class PermissionGatewayImpl(
    private val permissionChecker: (String) -> Boolean = { false },
    private val microphoneRequester: ((Boolean) -> Unit) -> Unit = { callback -> callback(false) },
    private val fileImportRequester: ((Boolean) -> Unit) -> Unit = { callback -> callback(false) }
) : PermissionGateway {
    
    override fun isGranted(permission: String): Boolean {
        return permissionChecker(permission)
    }
    
    override fun requestMicrophone(callback: (granted: Boolean) -> Unit) {
        microphoneRequester(callback)
    }
    
    override fun requestFileImport(callback: (granted: Boolean) -> Unit) {
        fileImportRequester(callback)
    }
}
