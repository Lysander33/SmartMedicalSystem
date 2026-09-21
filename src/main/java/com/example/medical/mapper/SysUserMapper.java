package com.example.medical.mapper;

import com.example.medical.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 操作统一登录账号表的接口
 */
@Mapper
public interface SysUserMapper {

    SysUser selectByUsername(String username);

    SysUser selectByRoleAndRefId(@Param("role") String role, @Param("refId") Integer refId);

    int insert(SysUser user);

    int updatePassword(@Param("role") String role, @Param("refId") Integer refId,
                       @Param("password") String password);

    int updateStatus(@Param("role") String role, @Param("refId") Integer refId,
                     @Param("status") Integer status);

    int updateName(@Param("role") String role, @Param("refId") Integer refId,
                   @Param("name") String name);
}
