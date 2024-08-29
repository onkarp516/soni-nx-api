package com.truethic.soninx.SoniNxAPI.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.model.Users;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.service.UserService;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
public class UserController {
    private final String SECRET_KEY = "m!j^d8#0en6j&rye8$$s%v)3f%i#ngm2e!%x1=s*h1ds&2ulqe&0ls";
    @Autowired
    UserService userService;
    @Autowired
    JwtTokenUtil jwtUtil;

    Logger userLogger = LoggerFactory.getLogger(UserController.class);

    @PostMapping(path = "/register_superAdmin")
    public ResponseEntity<?> createSuperAdmin(HttpServletRequest request) {
        return ResponseEntity.ok(userService.createSuperAdmin(request));
    }

    @PostMapping(path = "/register_user")
    public ResponseEntity<?> createUser(HttpServletRequest request) {
        return ResponseEntity.ok(userService.addUser(request));
    }

    @PostMapping(path = "/add_bo_user_with_roles")
    public Object addBoUserWithRoles(HttpServletRequest request) {
        JsonObject response = userService.addBoUserWithRoles(request);
        return response.toString();
    }

    @GetMapping(path = "/get_all_users")
    public Object getAllUsers(HttpServletRequest request) {
        JsonObject res = userService.getAllUsers(request);
        return res.toString();
    }

    /*** get access permissions of User *****/
    @PostMapping(path = "/get_user_permissions")
    public Object getUserPermissions(HttpServletRequest request) {
        JsonObject jsonObject = userService.getUserPermissions(request);
        return jsonObject.toString();
    }

    @PostMapping(path = "/DTUser")
    public Object DTUser(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return userService.DTUser(request, httpServletRequest);
    }

    @PostMapping(path = "/get_user_by_id")
    public Object getUsersById(HttpServletRequest requestParam) {
        JsonObject response = userService.getUsersById(requestParam.getParameter("id"));
        return response.toString();
    }

    /**** update Users ****/
    @PostMapping(path = "/updateUser")
    public ResponseEntity<?> updateUser(HttpServletRequest request) {
        return ResponseEntity.ok(userService.updateUser(request));
    }

    @PostMapping(path="/remove_user")
    public Object removeRole(HttpServletRequest request)
    {
        JsonObject result=userService.removeUser(request);
        return result.toString();
    }

    @PostMapping(path="/activate_deactivate_employee")
    public Object activateDeactivateEmployee(HttpServletRequest request)
    {
        JsonObject result=userService.activateDeactivateEmployee(request);
        return result.toString();
    }

    @PostMapping(path = "/test")
    public String greeting(@RequestBody Map<String, String> jsonRequest) {
        userLogger.info(" >>>>>>>>>>>>>>>> Request {}" + jsonRequest.toString());
        try {
            if (jsonRequest.get("name").equalsIgnoreCase("test")) {
//                throw new RuntimeException("Opps exceptoin raised");
                userLogger.error("*************** exception  custom exception *******************");
            }
        } catch (Exception e) {
            userLogger.error("exception " + e.getMessage());
        }
        String response = "Hi " + jsonRequest.get("name") + " welcome to Java";
//        log.info("Response {}", response);
        return response;
    }

    @RequestMapping(value = "/authenticate", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthenticateToken(@RequestBody Map<String, String> request,
                                                     HttpServletRequest req) throws Exception {
        ResponseMessage responseMessage = new ResponseMessage();
        String username = request.get("username");
        String password = request.get("password");

        try {
            Users userDetails = userService.findUserByUsername(username, password);
            System.out.println("userDetails : "+userDetails);
            if (userDetails != null) {
                Object jwtToken = jwtUtil.generateToken(req, userDetails.getUsername());

                responseMessage.setMessage("Login success");
                responseMessage.setResponse(jwtToken);

                responseMessage.setResponseStatus(HttpStatus.OK.value());
                System.out.println("login success");
                return ResponseEntity.ok(responseMessage);
            } else {
                System.out.println("login fail");
                responseMessage.setMessage("Incorrect username or password");
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
                return ResponseEntity.ok(responseMessage);
            }
        } catch (Exception e1) {
            System.out.println(e1.getMessage());
            System.out.println("login fail");
            responseMessage.setMessage("User not found");
            responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            return ResponseEntity.ok(responseMessage);
        }
    }


    @PostMapping("/change-password")
    public Object changePassword(@RequestBody Map<String, String> request, HttpServletRequest req) {
        return userService.changePassword(request, req);
    }

    @GetMapping("/token/refresh")
    public void refreshToken(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String authorizationHeader = request.getHeader(AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            try {
                String refresh_token = authorizationHeader.substring("Bearer ".length());
                System.out.println("refresh_token " + refresh_token);
                Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY.getBytes());
                JWTVerifier verifier = JWT.require(algorithm).build();
                DecodedJWT decodedJWT = verifier.verify(refresh_token);
                String username = decodedJWT.getSubject();
                Users user = (Users) userService.findUser(username);
                String access_token = JWT.create()
                        .withSubject(user.getUsername())
                        .withExpiresAt(new Date(System.currentTimeMillis() + 60 * 60 * 1000))
                        .withIssuer(request.getRequestURL().toString())
                        .withClaim("username", user.getUsername())
                        .withClaim("status", "OK")
                        .withClaim("userId", user.getId())
                        .withClaim("isSuperAdmin", user.getIsSuperadmin())
                        .sign(algorithm);

                String new_refresh_token = JWT.create()
                        .withSubject(user.getUsername())
                        .withExpiresAt(new Date(System.currentTimeMillis() + 65 * 60 * 1000))
                        .withIssuer(request.getRequestURI())
                        .sign(algorithm);

                Map<String, String> tokens = new HashMap<>();
                tokens.put("access_token", access_token);
                tokens.put("refresh_token", new_refresh_token);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(response.getOutputStream(), tokens);
            } catch (Exception exception) {
                response.setHeader("error", exception.getMessage());
                response.setStatus(FORBIDDEN.value());
                //response.sendError(FORBIDDEN.value());
                Map<String, String> error = new HashMap<>();
                error.put("error_message", exception.getMessage());
                error.put("message", "session destroyed plz login");
                response.setContentType(MimeTypeUtils.APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(response.getOutputStream(), error);
            }
        } else {
            throw new RuntimeException("Refresh token is missing");
        }
    }

    @GetMapping(path = "/getVersionCode")
    public Object getVersionCode() {
        return userService.getVersionCode().toString();
    }

    /*****for Sadmin Login, sdamin can only view cadmins ****/
    @GetMapping(path = "/get_company_admins")
    public Object getCompanyAdmins(HttpServletRequest httpServletRequest) {
        JsonObject res = userService.getCompanyAdmins(httpServletRequest);
        return res.toString();
    }
}


