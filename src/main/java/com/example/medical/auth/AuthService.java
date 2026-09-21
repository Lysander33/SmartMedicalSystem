package com.example.medical.auth;

import com.example.medical.entity.Doctor;
import com.example.medical.entity.Patient;
import com.example.medical.entity.SysUser;
import com.example.medical.mapper.DoctorMapper;
import com.example.medical.mapper.PatientMapper;
import com.example.medical.mapper.SysUserMapper;
import com.example.medical.util.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一登录逻辑：/api/auth/login 与患者注册共用。
 * 账号密码统一校验 sys_user，再按 role 取对应业务档案返回给前端。
 */
@Slf4j
@Service
public class AuthService {

    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private PatientMapper patientMapper;
    @Autowired
    private DoctorMapper doctorMapper;
    @Autowired
    private TokenStore tokenStore;

    /**
     * 登录：校验账号密码 + 身份类型，签发令牌
     *
     * @param role     前端页签选择的身份类型，必须与账号本身的角色一致
     * @param username 登录账号（患者=手机号，医生=工号，药房/管理员=员工账号）
     */
    public Map<String, Object> login(String role, String username, String password) {
        if (!Role.valid(role)) {
            throw new BizException(400, "未知的身份类型：" + role);
        }
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            throw new BizException(404, Role.PATIENT.equals(role) ? "账号不存在，请先注册" : Role.label(role) + "账号不存在");
        }
        //身份校验：拿医生工号在患者页签登录会被拒绝，并提示切换页签
        if (!user.getRole().equals(role)) {
            throw new BizException(400, "该账号是【" + Role.label(user.getRole()) + "】身份，请切换到对应页签登录");
        }
        if (user.getStatus() == 0) {
            throw new BizException(403, "账号已被停用，请联系医院");
        }
        if (!user.getPassword().equals(password)) {
            throw new BizException(401, "密码错误");
        }

        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getRole(),
                user.getRefId(), user.getName());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", tokenStore.issue(loginUser));
        data.put("role", user.getRole());
        data.put("roleLabel", Role.label(user.getRole()));
        data.put("home", Role.home(user.getRole()));
        data.put("profile", loadProfile(user));
        log.info("登录成功, role={}, account={}", role, username);
        return data;
    }

    /**
     * 患者注册：同时写 patient 档案 + sys_user 账号，两张表必须同事务
     */
    @Transactional
    public Patient registerPatient(Patient patient) {
        if (sysUserMapper.selectByUsername(patient.getPhone()) != null) {
            throw new BizException(400, "该手机号已注册，请直接登录");
        }
        patientMapper.insert(patient);
        createAccount(Role.PATIENT, patient.getId(), patient.getPhone(), patient.getPassword(), patient.getName());
        log.info("患者注册成功, phone={}, patientId={}", patient.getPhone(), patient.getId());
        return patient;
    }

    /**
     * 新建登录账号（管理员新增医生时也走这里，保证"有档案必有账号"）
     */
    public void createAccount(String role, Integer refId, String username, String password, String name) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        user.setRefId(refId);
        user.setName(name == null ? "" : name);
        user.setStatus(1);
        sysUserMapper.insert(user);
    }

    /** 同步业务档案上的停用/恢复到登录账号（登录的唯一开关在 sys_user.status） */
    public void syncAccountStatus(String role, Integer refId, Integer status) {
        sysUserMapper.updateStatus(role, refId, status);
    }

    /** 同步业务档案上的改名到登录账号 */
    public void syncAccountName(String role, Integer refId, String name) {
        sysUserMapper.updateName(role, refId, name);
    }

    /**
     * 按角色取业务档案：医生/患者取业务表（账号与科室信息一并查出），药房/管理员没有业务表
     */
    private Object loadProfile(SysUser user) {
        switch (user.getRole()) {
            case Role.DOCTOR: {
                Doctor doctor = doctorMapper.selectById(user.getRefId());
                if (doctor == null) {
                    throw new BizException(500, "医生档案缺失，请联系管理员");
                }
                doctor.setPassword(null);
                return doctor;
            }
            case Role.PATIENT: {
                Patient patient = patientMapper.selectById(user.getRefId());
                if (patient == null) {
                    throw new BizException(500, "患者档案缺失，请联系管理员");
                }
                return patient;
            }
            default: {
                Map<String, Object> staff = new LinkedHashMap<>();
                staff.put("id", user.getId());
                staff.put("account", user.getUsername());
                staff.put("name", user.getName());
                staff.put("role", user.getRole());
                return staff;
            }
        }
    }
}
