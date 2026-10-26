import ACCESS_ENUM from "@/enums/AccessEnum.ts";

const checkAuth = (loginUser: any, needAuth = ACCESS_ENUM.NO_LOGIN) => {
  // 获取当前用户具有的权限
  const loginUserAccess = loginUser?.userRole ?? ACCESS_ENUM.NO_LOGIN;
  // 需要的权限为 未登录
  if (needAuth === ACCESS_ENUM.NO_LOGIN) {
    return true; // 权限通过
  }

  //需要用户登录
  if (needAuth === ACCESS_ENUM.USER) {
    // 用户未登录
    if (loginUserAccess === ACCESS_ENUM.NO_LOGIN) {
      return false; // 权限未通过
    }
  }

  // 需要管理员权限
  if (needAuth === ACCESS_ENUM.ADMIN) {
    // 用户不是管理员权限
    if (loginUserAccess !== ACCESS_ENUM.ADMIN) {
      return false; // 权限不通过
    }
  }

  // 权限通过
  return true;
}


export { checkAuth };
