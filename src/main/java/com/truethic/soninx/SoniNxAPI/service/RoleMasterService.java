package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.truethic.soninx.SoniNxAPI.repository.CompanyRepository;
import com.truethic.soninx.SoniNxAPI.repository.RoleMasterRepository;
import com.truethic.soninx.SoniNxAPI.repository.UserRepository;
import com.truethic.soninx.SoniNxAPI.repository.access_permission_repository.RoleAccessPermissionsRepository;
import com.truethic.soninx.SoniNxAPI.repository.access_permission_repository.SystemAccessPermissionsRepository;
import com.truethic.soninx.SoniNxAPI.repository.access_permission_repository.SystemActionMappingRepository;
import com.truethic.soninx.SoniNxAPI.repository.access_permission_repository.SystemMasterModuleRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.common.CommonAccessPermissions;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.dto.RoleDTDTO;
import com.truethic.soninx.SoniNxAPI.model.Company;
import com.truethic.soninx.SoniNxAPI.model.RoleMaster;
import com.truethic.soninx.SoniNxAPI.model.Users;
import com.truethic.soninx.SoniNxAPI.model.access_permissions.RoleAccessPermissions;
import com.truethic.soninx.SoniNxAPI.model.access_permissions.SystemAccessPermissions;
import com.truethic.soninx.SoniNxAPI.model.access_permissions.SystemActionMapping;
import com.truethic.soninx.SoniNxAPI.model.access_permissions.SystemMasterModules;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class RoleMasterService {
    @Autowired
    private JwtTokenUtil jwtRequestFilter;
    @Autowired
    private RoleMasterRepository roleMasterRepository;
    @Autowired
    private RoleAccessPermissionsRepository roleAccessPermissionsRepository;
    @Autowired
    private SystemActionMappingRepository systemActionMappingRepository;
    @Autowired
    private SystemAccessPermissionsRepository systemAccessPermissionsRepository;
    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private CommonAccessPermissions accessPermissions;
    @Autowired
    private SystemMasterModuleRepository systemMasterModulesRepository;
    @PersistenceContext
    private EntityManager entityManager;
    private static final Logger roleLogger = LogManager.getLogger(RoleMasterService.class);
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private UserRepository userRepository;

    public JsonObject addRole(HttpServletRequest request) {
        Map<String, String[]> paramMap = request.getParameterMap();
        //  ResponseMessage responseObject = new ResponseMessage();
        JsonObject responseObject = new JsonObject();
        RoleMaster roleMaster = new RoleMaster();
        Users user = null;
        try {
            RoleMaster roleMasterTest = roleMasterRepository.findByRoleNameAndStatus(request.getParameter("roleName"),true);
            if(roleMasterTest != null){
                roleLogger.error("Role with this name already exists");
                System.out.println("Role with this name already exists");
                responseObject.addProperty("responseStatus", HttpStatus.CONFLICT.value());
                responseObject.addProperty("message", "Role already exists");
                return responseObject;
            }

            roleMaster.setRoleName(request.getParameter("roleName"));
            roleMaster.setStatus(true);
            if (request.getHeader("Authorization") != null) {
                user = jwtRequestFilter.getUserDataFromToken(
                        request.getHeader("Authorization").substring(7));
                roleMaster.setCreatedBy(user.getId());
                roleMaster.setInstitute(user.getInstitute());
            }
            if (paramMap.containsKey("companyId")) {
                Company company = companyRepository.findByIdAndStatus(Long.parseLong(request.getParameter("companyId")), true);
                roleMaster.setCompany(company);
            }
            RoleMaster newRole = roleMasterRepository.save(roleMaster);
            try {
                /* Create Permissions */
                String jsonStr = request.getParameter("roles_permissions");
                if (jsonStr != null) {
                    JsonArray userPermissions = new JsonParser().parse(jsonStr).getAsJsonArray();
                    for (int i = 0; i < userPermissions.size(); i++) {
                        JsonObject mObject = userPermissions.get(i).getAsJsonObject();
                        RoleAccessPermissions mPermissions = new RoleAccessPermissions();
                        mPermissions.setRoleMaster(newRole);
                        SystemActionMapping mappings = systemActionMappingRepository.findByIdAndStatus(mObject.get("mapping_id").getAsLong(),
                                true);
                        mPermissions.setSystemActionMapping(mappings);
                        mPermissions.setStatus(true);
                        mPermissions.setCreatedBy(user.getId());
                        JsonArray mActionsArray = mObject.get("actions").getAsJsonArray();
                        String actionsId = "";
                        for (int j = 0; j < mActionsArray.size(); j++) {
                            actionsId = actionsId + mActionsArray.get(j).getAsString();
                            if (j < mActionsArray.size() - 1) {
                                actionsId = actionsId + ",";
                            }
                        }
                        mPermissions.setUserActionsId(actionsId);
                        roleAccessPermissionsRepository.save(mPermissions);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                roleLogger.error("Exception in Role Master: " + e.getMessage());
                System.out.println(e.getMessage());
            }
            responseObject.addProperty("message", "Role master created successfully");
            responseObject.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (DataIntegrityViolationException e1) {
            e1.printStackTrace();
            roleLogger.error("Exception in addUser: " + e1.getMessage());
            System.out.println("DataIntegrityViolationException " + e1.getMessage());
            responseObject.addProperty("responseStatus", HttpStatus.CONFLICT.value());
            responseObject.addProperty("message", "Usercode already used");
            return responseObject;
        } catch (Exception e) {
            e.printStackTrace();
            roleLogger.error("Exception in addUser: " + e.getMessage());
            responseObject.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseObject.addProperty("message", "Internal Server Error");
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }
        return responseObject;
    }

    public JsonObject getRolesById(String id) {
        RoleMaster role = roleMasterRepository.findByIdAndStatus(Long.parseLong(id), true);
        JsonObject result = new JsonObject();
        result.addProperty("message", "success");
        result.addProperty("responseStatus", HttpStatus.OK.value());
        JsonArray role_permission = new JsonArray();
        if (role != null) {
            /***** get Role Permissions from access_permissions_tbl ****/
            List<RoleAccessPermissions> accessPermissions = new ArrayList<>();
            accessPermissions = roleAccessPermissionsRepository.findByRoleMasterIdAndStatus(role.getId(), true);

            for (RoleAccessPermissions mPermissions : accessPermissions) {
                JsonObject masterObject = new JsonObject();
                JsonObject mObject = new JsonObject();

                SystemMasterModules parentModule = systemMasterModulesRepository.findByIdAndStatus(
                        mPermissions.getSystemActionMapping().getSystemMasterModules().getParentModuleId(), true);
                if (parentModule != null) {
                    masterObject.addProperty("id", parentModule.getId());
                    masterObject.addProperty("name", parentModule.getName());
                } else {
                    masterObject.addProperty("id", mPermissions.getSystemActionMapping().getSystemMasterModules().getId());
                    masterObject.addProperty("name", mPermissions.getSystemActionMapping().getSystemMasterModules().getName());
                }
                mObject.addProperty("id", mPermissions.getSystemActionMapping().getId());
                mObject.addProperty("name", mPermissions.getSystemActionMapping().getName());
                JsonArray actions = new JsonArray();
                String actionsId = mPermissions.getUserActionsId();
                String[] actionsList = actionsId.split(",");
                Arrays.sort(actionsList);
                for (String actionId : actionsList) {
                    actions.add(actionId);
                }
                mObject.add("actions", actions);
                masterObject.add("level", mObject);
                role_permission.add(masterObject);
            }
            result.add("level", role_permission);
            result.addProperty("roleName", role.getRoleName());
        } else {
            result.addProperty("message", "error");
            result.addProperty("responseStatus", HttpStatus.FORBIDDEN.value());
        }
        return result;
    }

    private RoleMaster getChilds(Long parentId) {
        RoleMaster modules = roleMasterRepository.findByIdAndStatus(parentId, true);
        if (modules.getId() == null) {
            return modules;
        } else {
            //   moduleSets.add(modules.getId());
            return getChilds(modules.getId());
        }
    }

    public JsonObject getRoleByIdForEdit(HttpServletRequest request) {
        Long id = Long.valueOf(request.getParameter("role_id"));
        RoleMaster roleMaster = roleMasterRepository.findByIdAndStatus(id, true);
        JsonObject response = new JsonObject();
        JsonObject result = new JsonObject();
        JsonArray user_permission = new JsonArray();
        if (roleMaster != null) {
            response.addProperty("id", roleMaster.getId());
            response.addProperty("roleId", roleMaster.getId());
            response.addProperty("roleName", roleMaster.getRoleName());


            /***** get User Permissions from access_permissions_tbl ****/
            List<RoleAccessPermissions> accessPermissions = new ArrayList<>();

            accessPermissions = roleAccessPermissionsRepository.findByRoleMasterIdAndStatus(roleMaster.getId(), true);
            for (RoleAccessPermissions mPermissions : accessPermissions) {
                JsonObject mObject = new JsonObject();
                mObject.addProperty("mapping_id", mPermissions.getSystemActionMapping().getId());
                JsonArray actions = new JsonArray();
                String actionsId = mPermissions.getUserActionsId();
                String[] actionsList = actionsId.split(",");
                Arrays.sort(actionsList);
                for (String actionId : actionsList) {
                    actions.add(actionId);
                }
                mObject.add("actions", actions);
                user_permission.add(mObject);
            }
            response.add("permissions", user_permission);
            result.addProperty("message", "success");
            result.addProperty("responseStatus", HttpStatus.OK.value());

            result.add("responseObject", response);
        } else {
            result.addProperty("message", "error");
            result.addProperty("responseStatus", HttpStatus.FORBIDDEN.value());
        }
        return result;
    }

    public JsonObject getAllRoles(HttpServletRequest request) {
        Users users = jwtRequestFilter.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        JsonArray result = new JsonArray();
        JsonObject res = new JsonObject();
        List<RoleMaster> list = new ArrayList<>();
        list = roleMasterRepository.findByInstituteIdAndStatus(users.getInstitute().getId(), true);
        if (list.size() > 0) {
            for (RoleMaster role : list) {
                JsonObject response = new JsonObject();
                response.addProperty("id", role.getId());
                response.addProperty("name", role.getRoleName());
                response.addProperty("created_at", role.getCreatedAt().toString());
                result.add(response);
            }
            res.addProperty("message", "success");
            res.addProperty("responseStatus", HttpStatus.OK.value());
            res.add("responseObject", result);
        } else {
            res.addProperty("message", "empty list");
            res.addProperty("responseStatus", HttpStatus.OK.value());
            res.add("responseObject", result);
        }
        return res;
    }

    public Object updateRole(HttpServletRequest request) {
        Map<String, String[]> paramMap = request.getParameterMap();
        ResponseMessage responseObject = new ResponseMessage();
        Users user = new Users();
        RoleMaster roleMaster = roleMasterRepository.findByIdAndStatus(Long.parseLong(request.getParameter("role_id")),
                true);
        List<SystemAccessPermissions> systemAccessPermissions = systemAccessPermissionsRepository.findByUserRoleIdAndStatus(Long.parseLong(request.getParameter("role_id")), true);
        if (systemAccessPermissions != null && systemAccessPermissions.size() > 0) {
            responseObject.setResponseStatus(HttpStatus.FORBIDDEN.value());
            responseObject.setMessage("Role is assigned to someone, you cannot update.");
        } else {
            if (roleMaster != null) {
                if (paramMap.containsKey("roleName")) {
                    roleMaster.setRoleName(request.getParameter("roleName"));
                }
                if (request.getHeader("Authorization") != null) {
                    user = jwtRequestFilter.getUserDataFromToken(
                            request.getHeader("Authorization").substring(7));
                    roleMaster.setCreatedBy(user.getId());
                    roleMaster.setInstitute(user.getInstitute());
                }
                /* Update Permissions */
                String jsonStr = request.getParameter("role_permissions");
                JsonArray userPermissions = new JsonParser().parse(jsonStr).getAsJsonArray();
                for (int i = 0; i < userPermissions.size(); i++) {
                    JsonObject mObject = userPermissions.get(i).getAsJsonObject();
                    SystemActionMapping mappings = systemActionMappingRepository.findByIdAndStatus(mObject.get("mapping_id").getAsLong(),
                            true);
                    RoleAccessPermissions mPermissions = roleAccessPermissionsRepository.findByRoleMasterIdAndStatusAndSystemActionMappingId(
                            roleMaster.getId(), true, mappings.getId());
                    if (mPermissions != null) {
                        JsonArray mActionsArray = mObject.get("actions").getAsJsonArray();
                        String actionsId = "";
                        for (int j = 0; j < mActionsArray.size(); j++) {
                            actionsId = actionsId + mActionsArray.get(j).getAsString();
                            if (j < mActionsArray.size() - 1) {
                                actionsId = actionsId + ",";
                            }
                        }
                        mPermissions.setUserActionsId(actionsId);
                    } else {
                        mPermissions = new RoleAccessPermissions();
                        JsonArray mActionsArray = mObject.get("actions").getAsJsonArray();
                        String actionsId = "";
                        for (int j = 0; j < mActionsArray.size(); j++) {
                            actionsId = actionsId + mActionsArray.get(j).getAsString();
                            if (j < mActionsArray.size() - 1) {
                                actionsId = actionsId + ",";
                            }
                        }
                        mPermissions.setUserActionsId(actionsId);
                    }
                    mPermissions.setRoleMaster(roleMaster);
                    mPermissions.setSystemActionMapping(mappings);
                    mPermissions.setStatus(true);
                    mPermissions.setCreatedBy(user.getId());

                    roleAccessPermissionsRepository.save(mPermissions);
                }
                String del_user_perm = request.getParameter("del_role_permissions");
                JsonArray deleteUserPermission = new JsonParser().parse(del_user_perm).getAsJsonArray();
                for (int j = 0; j < deleteUserPermission.size(); j++) {
                    Long moduleId = deleteUserPermission.get(j).getAsLong();
                    //  SystemActionMapping delMapping = mappingRepository.findByIdAndStatus(moduleId, true);
                    RoleAccessPermissions delPermissions = roleAccessPermissionsRepository.findByRoleMasterIdAndStatusAndSystemActionMappingId(
                            roleMaster.getId(), true, moduleId);
                    delPermissions.setStatus(false);
                    try {
                        roleAccessPermissionsRepository.save(delPermissions);
                    } catch (Exception e) {
                    }

                }
                roleMasterRepository.save(roleMaster);
                responseObject.setMessage("User Role updated sucessfully");
                responseObject.setResponseStatus(HttpStatus.OK.value());
            } else {
                responseObject.setResponseStatus(HttpStatus.FORBIDDEN.value());
                responseObject.setMessage("Not found");
            }
        }
        return responseObject;
    }

    public JsonObject getRolePermissions(HttpServletRequest request) {
        /* getting Role Permissions */
        JsonObject finalResult = new JsonObject();
        JsonArray rolePermissions = new JsonArray();
        JsonArray permissions = new JsonArray();
        JsonArray masterModules = new JsonArray();
        System.out.println(request.getParameter("role_id"));
        Long roleId = Long.parseLong(request.getParameter("role_id"));
        List<RoleAccessPermissions> list = roleAccessPermissionsRepository.findByRoleMasterIdAndStatus(roleId, true);
        /*
         * Print elements using the forEach
         */
        for (RoleAccessPermissions mapping : list) {
            JsonObject mObject = new JsonObject();
            mObject.addProperty("id", mapping.getId());
            mObject.addProperty("role_name", mapping.getRoleMaster().getRoleName());

            mObject.addProperty("action_mapping_id", mapping.getSystemActionMapping().getId());
            mObject.addProperty("action_mapping_name", mapping.getSystemActionMapping().getSystemMasterModules().getName());
            mObject.addProperty("action_mapping_slug", mapping.getSystemActionMapping().getSystemMasterModules().getSlug());
            String[] actions = mapping.getUserActionsId().split(",");
            permissions = accessPermissions.getActions(actions);
            masterModules = accessPermissions.getParentMasters(mapping.getSystemActionMapping().getSystemMasterModules().getParentModuleId());
            mObject.add("actions", permissions);
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", mapping.getSystemActionMapping().getSystemMasterModules().getId());
            jsonObject.addProperty("name", mapping.getSystemActionMapping().getSystemMasterModules().getName());
            jsonObject.addProperty("slug", mapping.getSystemActionMapping().getSystemMasterModules().getSlug());
            masterModules.add(jsonObject);
            mObject.add("parent_modules", masterModules);
            rolePermissions.add(mObject);
        }
        finalResult.add("RoleActions", rolePermissions);
        return finalResult;
    }

    public Object DTRole(Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        String status = request.get("status");
        Users user = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<RoleMaster> roleList = new ArrayList<>();
        List<RoleDTDTO> roleDTDTOList = new ArrayList<>();
        try {
            String query = "SELECT * FROM `role_master_tbl` WHERE role_master_tbl.status = "+status+" AND institute_id="+user.getInstitute().getId();

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND (role_name LIKE '%" + searchText + "%')";
            }

            String jsonToStr = request.get("sort");
            JsonObject jsonObject = new Gson().fromJson(jsonToStr, JsonObject.class);
            if (!jsonObject.get("colId").toString().equalsIgnoreCase("null") &&
                    jsonObject.get("colId").toString() != null) {
                System.out.println(" ORDER BY " + jsonObject.get("colId").toString());
                String sortBy = jsonObject.get("colId").toString();
                query = query + " ORDER BY " + sortBy;
                if (jsonObject.get("isAsc").getAsBoolean()) {
                    query = query + " ASC";
                } else {
                    query = query + " DESC";
                }
            } else {
                query = query + " ORDER BY id DESC";
            }
            String query1 = query;
            Integer endLimit = to - from;
            query = query + " LIMIT " + from + ", " + endLimit;
            System.out.println("query " + query);

            Query q = entityManager.createNativeQuery(query, RoleMaster.class);
            Query q1 = entityManager.createNativeQuery(query1, RoleMaster.class);

            roleList = q.getResultList();
            System.out.println("Limit total rows " + roleList.size());

            for (RoleMaster roleMaster : roleList) {
                roleDTDTOList.add(convertToDTO(roleMaster));
            }

            List<RoleMaster> roleArrayList = new ArrayList<>();
            roleArrayList = q1.getResultList();
            System.out.println("total rows " + roleArrayList.size());

            genericDTData.setRows(roleDTDTOList);
            genericDTData.setTotalRows(roleArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(roleDTDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    private RoleDTDTO convertToDTO(RoleMaster roleMaster) {
        RoleDTDTO roleDTDTO = new RoleDTDTO();
        roleDTDTO.setId(roleMaster.getId());
        roleDTDTO.setRoleName(roleMaster.getRoleName());
        roleDTDTO.setCreatedAt(String.valueOf(roleMaster.getCreatedAt()));
        roleDTDTO.setUpdatedAt(String.valueOf(roleMaster.getUpdatedAt()));
        roleDTDTO.setCreatedBy(roleMaster.getCreatedBy());
        roleDTDTO.setUpdatedBy(roleMaster.getUpdatedBy());
        return roleDTDTO;
    }

    public JsonObject removerRole(HttpServletRequest request) {
        Users users = jwtRequestFilter.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        JsonObject jsonObject = new JsonObject();

        RoleMaster roleMaster = roleMasterRepository.findByIdAndStatus(Long.parseLong(request.getParameter("role_id")),
                true);
        List<SystemAccessPermissions> systemAccessPermissions = systemAccessPermissionsRepository.findByUserRoleIdAndStatus(Long.parseLong(request.getParameter("role_id")), true);
        if (systemAccessPermissions != null && systemAccessPermissions.size() > 0) {
            jsonObject.addProperty("message", "Role is assigned to someone, you cannot delete.");
            jsonObject.addProperty("responseStatus", HttpStatus.FORBIDDEN.value());
        } else {
            try {
                roleMaster.setStatus(false);
                roleMaster.setUpdatedBy(users.getId());
                roleMaster.setInstitute(users.getInstitute());
                roleMaster.setUpdatedAt(LocalDateTime.now());
                roleMasterRepository.save(roleMaster);
                jsonObject.addProperty("message", "Role Deleted Successfully");
                jsonObject.addProperty("responseStatus", HttpStatus.OK.value());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Exception:" + e.getMessage());
                e.getMessage();
                e.printStackTrace();
            }
        }
        return jsonObject;
    }
}
