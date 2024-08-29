package com.truethic.soninx.SoniNxAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.truethic.soninx.SoniNxAPI.repository.AppVersionRepository;
import com.truethic.soninx.SoniNxAPI.repository.RoleMasterRepository;
import com.truethic.soninx.SoniNxAPI.repository.UserRepository;
import com.truethic.soninx.SoniNxAPI.repository.access_permission_repository.SystemAccessPermissionsRepository;
import com.truethic.soninx.SoniNxAPI.repository.access_permission_repository.SystemActionMappingRepository;
import com.truethic.soninx.SoniNxAPI.repository.access_permission_repository.SystemMasterModuleRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.common.CommonAccessPermissions;
import com.truethic.soninx.SoniNxAPI.common.PasswordEncoders;
import com.truethic.soninx.SoniNxAPI.dto.GenericDTData;
import com.truethic.soninx.SoniNxAPI.dto.UserDTDTO;
import com.truethic.soninx.SoniNxAPI.model.AppVersion;
import com.truethic.soninx.SoniNxAPI.model.Employee;
import com.truethic.soninx.SoniNxAPI.model.RoleMaster;
import com.truethic.soninx.SoniNxAPI.model.Users;
import com.truethic.soninx.SoniNxAPI.model.access_permissions.SystemAccessPermissions;
import com.truethic.soninx.SoniNxAPI.model.access_permissions.SystemActionMapping;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
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
public class UserService implements UserDetailsService {
    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtTokenUtil jwtTokenUtil;
    @Autowired
    private AppVersionRepository appVersionRepository;
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private SystemMasterModuleRepository systemMasterModulesRepository;
    private static final Logger UserLogger = LogManager.getLogger(UserService.class);

//    @Autowired
//    private OutletRepository outletRepository;
    @Autowired
    private PasswordEncoders bcryptEncoder;
    @Autowired
    private SystemActionMappingRepository mappingRepository;
    @Autowired
    private SystemAccessPermissionsRepository accessPermissionsRepository;
    @Autowired
    private CommonAccessPermissions accessPermissions;
    @Autowired
    private SystemAccessPermissionsRepository systemAccessPermissionsRepository;
    @Autowired
    private RoleMasterRepository roleMasterRepository;

    public JsonObject addBoUserWithRoles(HttpServletRequest request) {
        Map<String, String[]> paramMap = request.getParameterMap();
        //  ResponseMessage responseObject = new ResponseMessage();
        JsonObject responseObject = new JsonObject();
        Users users = new Users();
        Users user = null;
        try {
            Users userTest = userRepository.findByUsernameAndStatus(request.getParameter("userName"),true);
            if(userTest != null){
                UserLogger.error("User with this name already exists");
                System.out.println("User with this name already exists");
                responseObject.addProperty("responseStatus", HttpStatus.CONFLICT.value());
                responseObject.addProperty("message", "User already exists");
                return responseObject;
            }
            users.setUsername(request.getParameter("userName"));
            users.setUserRole(request.getParameter("userRole"));
            RoleMaster roleMaster = roleMasterRepository.findRoleMasterById(Long.parseLong(request.getParameter("userRole")));
            users.setRoleMaster(roleMaster);
            users.setStatus(true);
            users.setIsSuperadmin(false);
            users.setIsAdmin(false);
            //  users.setPermissions(request.getParameter("permissions"));
            if (request.getHeader("Authorization") != null) {
                user = jwtTokenUtil.getUserDataFromToken(
                        request.getHeader("Authorization").substring(7));
                users.setCreatedBy(user.getId());
                users.setInstitute(user.getInstitute());
            }
            users.setPassword(bcryptEncoder.passwordEncoderNew().encode(
                    request.getParameter("password")));
            users.setPlain_password(request.getParameter("password"));
//            if (paramMap.containsKey("companyId")) {
//                Outlet mOutlet = outletRepository.findByIdAndStatus(Long.parseLong(request.getParameter("companyId")),
//                        true);
//                users.setOutlet(mOutlet);
//            }
            if (paramMap.containsKey("permissions"))
                users.setPermissions(request.getParameter("permissions"));
            Users newUser = userRepository.save(users);
            try {
                /* Create Permissions */
                String jsonStr = request.getParameter("user_permissions");
                if (jsonStr != null) {
                    JsonArray userPermissions = new JsonParser().parse(jsonStr).getAsJsonArray();
                    for (int i = 0; i < userPermissions.size(); i++) {
                        JsonObject mObject = userPermissions.get(i).getAsJsonObject();
                        SystemAccessPermissions mPermissions = new SystemAccessPermissions();
                        mPermissions.setUsers(newUser);
                        SystemActionMapping mappings = mappingRepository.findByIdAndStatus(mObject.get("mapping_id").getAsLong(),
                                true);
                        mPermissions.setUserRole(roleMaster);
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
                        accessPermissionsRepository.save(mPermissions);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                UserLogger.error("Exception in addBoUserWithRoles: " + e.getMessage());
                System.out.println(e.getMessage());
            }
            responseObject.addProperty("message", "User added succussfully");
            responseObject.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (DataIntegrityViolationException e1) {
            e1.printStackTrace();
            UserLogger.error("Exception in addBoUserWithRoles: " + e1.getMessage());
            System.out.println("DataIntegrityViolationException " + e1.getMessage());
            responseObject.addProperty("responseStatus", HttpStatus.CONFLICT.value());
            responseObject.addProperty("message", "Usercode already used");
            return responseObject;
        } catch (Exception e) {
            e.printStackTrace();
            UserLogger.error("Exception in addBoUserWithRoles: " + e.getMessage());
            responseObject.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseObject.addProperty("message", "Internal Server Error");
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }
        return responseObject;
    }

    public Object addUser(HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        Users users = new Users();

        if (request.getParameter("empployeeId") != null) {
            Employee employee = new Employee();
            employee.setId(Long.valueOf(request.getParameter("empployeeId")));
            users.setEmployee(employee);
        }
        users.setPermissions(request.getParameter("permissions"));
        users.setStatus(true);
        if (request.getHeader("Authorization") != null) {
            Users user = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
            users.setCreatedBy(user.getId());
            users.setUpdatedBy(user.getId());
            users.setInstitute(user.getInstitute());
        }
        users.setUsername(request.getParameter("username"));
        users.setPassword(passwordEncoder.encode(request.getParameter("password")));
        users.setPlain_password(request.getParameter("password"));
        try {
            userRepository.save(users);
            responseObject.setMessage("User added successfully");
            responseObject.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {

            responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseObject.setMessage("Internal Server Error");
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }
        return responseObject;
    }

    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        Users user = userRepository.findByUsername(userName);
        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + userName);
        }
        return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(),
                new ArrayList<>());
    }

    public Users findUserByUsername(String username, String password) throws UsernameNotFoundException {
        Users users = userRepository.findByUsernameAndStatus(username, true);
        if (passwordEncoder.matches(password, users.getPassword())) {
            return users;
        }
        return null;
    }

    public Object findUser(String username) throws UsernameNotFoundException {
        Users users = userRepository.findByUsernameAndStatus(username, true);
        if (users == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        return users;
    }

    public Object createSuperAdmin(HttpServletRequest request) {
        ResponseMessage responseObject = new ResponseMessage();
        Users users = new Users();
        users.setUsername(request.getParameter("username"));
        users.setIsSuperadmin(true);
        users.setStatus(true);
        users.setPassword(passwordEncoder.encode(request.getParameter("password")));
        users.setPlain_password(request.getParameter("password"));
        try {
            userRepository.save(users);
            responseObject.setMessage("Super admin created sucessfully");
            responseObject.setResponseStatus(HttpStatus.OK.value());
        } catch (Exception e) {

            responseObject.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseObject.setMessage("Internal Server Error");
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
        }
        return responseObject;
    }

    public Object changePassword(Map<String, String> request, HttpServletRequest req) {
        ResponseMessage responseMessage = new ResponseMessage();
        Users users = jwtTokenUtil.getUserDataFromToken(req.getHeader("Authorization").substring(7));
        if (users != null) {
            users.setPlain_password(request.get("password"));
            String encPassword = passwordEncoder.encode(request.get("password"));
            users.setPassword(encPassword);
            try {
                userRepository.save(users);
                responseMessage.setMessage("Password changed successfully");
                responseMessage.setResponseStatus(HttpStatus.OK.value());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Exception " + e.getMessage());
                responseMessage.setMessage("Failed change password");
                responseMessage.setResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        } else {
            responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            responseMessage.setMessage("User not exist, Try again.");
        }
        return responseMessage;
    }

    public JsonObject getVersionCode() {
        JsonObject responseMessage = new JsonObject();
        try {
            AppVersion appVersion = appVersionRepository.findById(Long.valueOf("1")).get();
            if (appVersion != null) {
                JsonObject appObject = new JsonObject();
                appObject.addProperty("id", appVersion.getId());
                appObject.addProperty("versionCode", appVersion.getVersionCode());
                appObject.addProperty("versionName", appVersion.getVersionName());

                responseMessage.add("response", appObject);
                responseMessage.addProperty("responseStatus", HttpStatus.OK.value());
            } else {
                responseMessage.addProperty("message", "Version not found");
                responseMessage.addProperty("responseStatus", HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());
            responseMessage.addProperty("message", "Failed to load data");
            responseMessage.addProperty("responseStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return responseMessage;
    }

    public JsonObject getCompanyAdmins(HttpServletRequest httpServletRequest) {
//        Users users = jwtRequestFilter.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        JsonObject res = new JsonObject();
        JsonArray result = new JsonArray();
        List<Users> list = new ArrayList<>();
        // Long companyId = Long.parseLong(httpServletRequest.getParameter("companyId"));
        list = userRepository.findByUserRoleIgnoreCaseAndStatus("CADMIN", true);
        if (list.size() > 0) {
            res = getUserData(list);
        } else {
            res.addProperty("message", "empty list");
            res.addProperty("responseStatus", HttpStatus.OK.value());
            res.add("responseObject", result);
        }
        return res;
    }

    public JsonObject getUserData(List<Users> list) {
        JsonObject res = new JsonObject();
        JsonArray result = new JsonArray();
        for (Users mUser : list) {
            JsonObject response = new JsonObject();
            response.addProperty("id", mUser.getId());
            if (mUser.getEmployee().getCompany().getCompanyName() != null)
                response.addProperty("companyName", mUser.getEmployee().getCompany().getCompanyName());
            response.addProperty("username", mUser.getUsername());
            response.addProperty("fullName", mUser.getEmployee().getFullName());
            response.addProperty("mobileNumber", mUser.getEmployee().getMobileNumber() != 0 ? mUser.getEmployee().getMobileNumber().toString() : "NA");
            response.addProperty("address", mUser.getEmployee().getAddress());
            response.addProperty("gender", mUser.getEmployee().getGender());
            result.add(response);
        }
        res.addProperty("responseStatus", HttpStatus.OK.value());
        res.add("responseObject", result);
        /*if (list.size() > 0) {
        } else {
            res.addProperty("message", "empty list");
            res.addProperty("responseStatus", HttpStatus.OK.value());
            res.add("responseObject", result);
        }*/
        return res;
    }

    public JsonObject getAllUsers(HttpServletRequest request) {
//        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        JsonArray result = new JsonArray();
        JsonObject res = new JsonObject();
        List<Users> list = new ArrayList<>();
        list = userRepository.findByStatus(true);
        if (list.size() > 0) {
            for (Users user : list) {
                JsonObject response = new JsonObject();
                response.addProperty("id", user.getId());
                response.addProperty("name", user.getUsername());
                response.addProperty("created_at", user.getCreatedAt().toString());
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

    public Object DTUser(Map<String, String> request, HttpServletRequest httpServletRequest) {
        Integer from = Integer.parseInt(request.get("from"));
        Integer to = Integer.parseInt(request.get("to"));
        String searchText = request.get("searchText");
        String status = request.get("status");
        Users users = jwtTokenUtil.getUserDataFromToken(httpServletRequest.getHeader("Authorization").substring(7));
        GenericDTData genericDTData = new GenericDTData();
        List<Users> usersList = new ArrayList<>();
        List<UserDTDTO> userDTDTOList = new ArrayList<>();
        try {
            String query = "SELECT * FROM `users` WHERE users.status = "+status+" AND users.institute_id="+users.getInstitute().getId()+" AND !is_admin";

            if (!searchText.equalsIgnoreCase("")) {
                query = query + " AND (username LIKE '%" + searchText + "%')";
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

            Query q = entityManager.createNativeQuery(query, Users.class);
            Query q1 = entityManager.createNativeQuery(query1, Users.class);

            usersList = q.getResultList();
            System.out.println("Limit total rows " + usersList.size());

            for (Users user : usersList) {
                userDTDTOList.add(convertToDTO(user));
            }

            List<Users> userArrayList = new ArrayList<>();
            userArrayList = q1.getResultList();
            System.out.println("total rows " + userArrayList.size());

            genericDTData.setRows(userDTDTOList);
            genericDTData.setTotalRows(userArrayList.size());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception " + e.getMessage());

            genericDTData.setRows(userDTDTOList);
            genericDTData.setTotalRows(0);
        }
        return genericDTData;
    }

    private UserDTDTO convertToDTO(Users users) {
        UserDTDTO userDTDTO = new UserDTDTO();
        userDTDTO.setId(users.getId());
        userDTDTO.setUsername(users.getUsername());
        userDTDTO.setUserRole(users.getUserRole() != null ? users.getUserRole() : "");
        if(users.getUserRole() != null){
            RoleMaster roleMaster = roleMasterRepository.getById(users.getRoleMaster().getId());
            userDTDTO.setRoleName(roleMaster != null ? roleMaster.getRoleName() : "");
        }
        userDTDTO.setPermissions(users.getPermissions() != null ? users.getPermissions() : "");
        userDTDTO.setIsSuperadmin(users.getIsSuperadmin());
        userDTDTO.setPassword(users.getPassword());
        userDTDTO.setStatus(users.getStatus());
        userDTDTO.setCreatedAt(String.valueOf(users.getCreatedAt()));
        userDTDTO.setCreatedBy(users.getCreatedBy());
        return userDTDTO;
    }

    public JsonObject getUserPermissions(HttpServletRequest request) {
        /* getting User Permissions */
        JsonObject finalResult = new JsonObject();
        JsonArray userPermissions = new JsonArray();
        JsonArray permissions = new JsonArray();
        JsonArray masterModules = new JsonArray();
            Long userId = Long.parseLong(request.getParameter("user_id"));
        List<SystemAccessPermissions> list = systemAccessPermissionsRepository.findByUsersIdAndStatus(userId, true);
        /*
         * Print elements using the forEach
         */
        for (SystemAccessPermissions mapping : list) {
            JsonObject mObject = new JsonObject();
            mObject.addProperty("id", mapping.getId());
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
            userPermissions.add(mObject);
        }
        finalResult.add("userActions", userPermissions);
        return finalResult;
    }

    public Object updateUser(HttpServletRequest request) {
        Map<String, String[]> paramMap = request.getParameterMap();
        ResponseMessage responseObject = new ResponseMessage();
        Users users = userRepository.findByIdAndStatus(Long.parseLong(request.getParameter("user_id")),
                true);
        if (users != null) {
            users.setUsername(request.getParameter("userName"));
//            users.setPassword(request.getParameter("password"));
            users.setPassword(bcryptEncoder.passwordEncoderNew().encode(request.getParameter("password")));
            users.setPlain_password(request.getParameter("password"));

            RoleMaster userRole=roleMasterRepository.findByIdAndStatus(Long.parseLong(request.getParameter("role_id")),true);
            if(userRole!=null)
            {
                users.setRoleMaster(userRole);
                users.setUserRole(userRole.getRoleName());
            }
            //users.setUserRole(request.getParameter("userRole"));
//            users.setPermissions(request.getParameter("permissions"));
            if (request.getHeader("Authorization") != null) {
                Users user = jwtTokenUtil.getUserDataFromToken(
                        request.getHeader("Authorization").substring(7));
                users.setCreatedBy(user.getId());
                users.setInstitute(user.getInstitute());
            }
//            users.setPassword(bcryptEncoder.passwordEncoderNew().encode(request.getParameter("password")));
//            users.setPlain_password(request.getParameter("password"));

            /* Update Permissions */
            String jsonStr = request.getParameter("user_permissions");
            if (jsonStr != null) {
                JsonArray userPermissions = new JsonParser().parse(jsonStr).getAsJsonArray();
                for (int i = 0; i < userPermissions.size(); i++) {
                    JsonObject mObject = userPermissions.get(i).getAsJsonObject();
                    SystemActionMapping mappings = mappingRepository.findByIdAndStatus(mObject.get("mapping_id").getAsLong(),
                            true);
                    SystemAccessPermissions mPermissions = accessPermissionsRepository.findByUsersIdAndStatusAndSystemActionMappingId(
                            users.getId(), true, mappings.getId());
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
                        mPermissions = new SystemAccessPermissions();
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
                    mPermissions.setUsers(users);
                    mPermissions.setSystemActionMapping(mappings);
                    mPermissions.setStatus(true);
                    mPermissions.setCreatedBy(users.getId());
                    mPermissions.setUserRole(userRole);
                    try {
                        accessPermissionsRepository.save(mPermissions);
                    }catch (Exception exception){
                        System.out.println(exception);
                    }
                }
                String del_user_perm = request.getParameter("del_user_permissions");
                JsonArray deleteUserPermission = new JsonParser().parse(del_user_perm).getAsJsonArray();
                for (int j = 0; j < deleteUserPermission.size(); j++) {
                    Long moduleId = deleteUserPermission.get(j).getAsLong();
                    //  SystemActionMapping delMapping = mappingRepository.findByIdAndStatus(moduleId, true);
                    SystemAccessPermissions delPermissions = accessPermissionsRepository.findByUsersIdAndStatusAndSystemActionMappingId(
                            users.getId(), true, moduleId);
                    delPermissions.setStatus(false);
                    delPermissions.setCreatedBy(users.getId());
                    delPermissions.setUserRole(userRole);
                    try {
                        accessPermissionsRepository.save(delPermissions);
                    } catch (Exception e) {
                    }

                }
                userRepository.save(users);
                responseObject.setMessage("User updated sucessfully");
                responseObject.setResponseStatus(HttpStatus.OK.value());
            } else {
                responseObject.setResponseStatus(HttpStatus.FORBIDDEN.value());
                responseObject.setMessage("Not found");
            }
        }  else{
            responseObject.setResponseStatus(HttpStatus.FORBIDDEN.value());
            responseObject.setMessage("Not found");
        }
        return responseObject;
    }

    public JsonObject getUsersById(String id) {
        Users user = userRepository.findByIdAndStatus(Long.parseLong(id), true);
        JsonObject response = new JsonObject();
        JsonObject result = new JsonObject();
        JsonArray user_permission = new JsonArray();
        if (user != null) {
            response.addProperty("id", user.getId());
//            response.addProperty("roleId", user.getRoleMaster().getId());
            response.addProperty("userRole", user.getUserRole());
            response.addProperty("password", user.getPlain_password());
            response.addProperty("userName", user.getUsername());
            response.addProperty("role_id", user.getRoleMaster().getId());
            response.addProperty("password", user.getPlain_password());
            if(user.getUserRole() != null){
                RoleMaster roleMaster = roleMasterRepository.getById(user.getRoleMaster().getId());
                response.addProperty("roleName", roleMaster != null ? roleMaster.getRoleName() : "");
            }

            /***** get User Permissions from access_permissions_tbl ****/
            List<SystemAccessPermissions> accessPermissions = new ArrayList<>();
            accessPermissions = systemAccessPermissionsRepository.findByUsersIdAndStatus(user.getId(), true);
            for (SystemAccessPermissions mPermissions : accessPermissions) {
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

    public JsonObject removeUser(HttpServletRequest request) {
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("message", "User Not Found");
        jsonObject.addProperty("responseStatus", HttpStatus.FORBIDDEN.value());
        Users userToBeDeleted = userRepository.findByIdAndStatus(Long.parseLong(request.getParameter("user_id")),true);
        List<SystemAccessPermissions> systemAccessPermissions = systemAccessPermissionsRepository.findByUsersIdAndStatus(userToBeDeleted.getId(),true);
        try {
            for(SystemAccessPermissions mPermission: systemAccessPermissions){
                mPermission.setStatus(false);
            }
            userToBeDeleted.setStatus(false);
            userToBeDeleted.setUpdatedBy(users.getId());
            userToBeDeleted.setInstitute(users.getInstitute());
            userToBeDeleted.setUpdatedAt(LocalDateTime.now());
            userRepository.save(userToBeDeleted);
            jsonObject.addProperty("message", "User Deleted Successfully");
            jsonObject.addProperty("responseStatus", HttpStatus.OK.value());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception:" + e.getMessage());
            e.getMessage();
            e.printStackTrace();
        }
        return jsonObject;
    }

    public JsonObject activateDeactivateEmployee(HttpServletRequest request) {
        Users users = jwtTokenUtil.getUserDataFromToken(request.getHeader("Authorization").substring(7));
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("message", "User Not Found");
        jsonObject.addProperty("responseStatus", HttpStatus.FORBIDDEN.value());
        boolean status = Boolean.parseBoolean(request.getParameter("status"));
        Users user = userRepository.findByIdAndStatus(Long.parseLong(request.getParameter("user_id")),!status);
//        if(status)
//            user = userRepository.findByIdAndStatus(Long.parseLong(request.getParameter("user_id")),!status);
//        else
//            user = userRepository.findByIdAndStatus(Long.parseLong(request.getParameter("user_id")),!status);
        if(user != null){
            List<SystemAccessPermissions> systemAccessPermissions = systemAccessPermissionsRepository.findByUsersIdAndStatus(user.getId(),!status);
            try {
                for(SystemAccessPermissions mPermission: systemAccessPermissions){
                    mPermission.setStatus(status);
                }
                user.setStatus(status);
                user.setUpdatedBy(users.getId());
                user.setInstitute(users.getInstitute());
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
                if(status) {
                    jsonObject.addProperty("message", "User Activated Successfully");
                    jsonObject.addProperty("responseStatus", HttpStatus.OK.value());
                } else {
                    jsonObject.addProperty("message", "User De-Activated Successfully");
                    jsonObject.addProperty("responseStatus", HttpStatus.OK.value());
                }
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
