package xyz.xiao6.myboard.core.androidbridge

/**
 * 权限网关。
 * 集中管理权限请求，UI 只表达「请求能力」。
 * 阶段 01 只定义接口，阶段 07 实现真实逻辑。
 */
interface PermissionGateway {
    fun isGranted(permission: String): Boolean
    fun requestMicrophone(callback: (granted: Boolean) -> Unit)
    fun requestFileImport(callback: (granted: Boolean) -> Unit)
}