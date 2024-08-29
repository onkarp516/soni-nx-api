package com.truethic.soninx.SoniNxAPI.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.truethic.soninx.SoniNxAPI.filter.CustomAuthenticationProvider;
import com.truethic.soninx.SoniNxAPI.model.Employee;
import com.truethic.soninx.SoniNxAPI.repository.EmployeeRepository;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import com.truethic.soninx.SoniNxAPI.service.EmployeeService;
import com.truethic.soninx.SoniNxAPI.util.JwtTokenUtil;
import com.truethic.soninx.SoniNxAPI.util.Utility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.*;

@RestController
public class EmployeeController {
    private final String SECRET_KEY = "m!j^d8#0en6j&rye8$$s%v)3f%i#ngm2e!%x1=s*h1ds&2ulqe&0ls";
    @Autowired
    EmployeeService employeeService;
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    CustomAuthenticationProvider customAuthenticationProvider;
    @Autowired
    JwtTokenUtil jwtTokenUtil;
    @Autowired
    private Utility utility;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    @PostMapping(path = "/createEmployee")
    public ResponseEntity<?> createEmployee(MultipartHttpServletRequest request) {
        return ResponseEntity.ok(employeeService.createEmployee(request));
    }

    @PostMapping(path = "/DTEmployee")
    public Object DTEmployee(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return employeeService.DTEmployee(request,httpServletRequest);
    }

    @PostMapping(path = "/findEmployee")
    public Object findEmployee(@RequestBody Map<String, String> request) {
        return employeeService.findEmployee(request);
    }

    @PostMapping(path = "/updateEmployee")
    public Object updateEmployee(MultipartHttpServletRequest request) {
        return employeeService.updateEmployee(request);
    }

    @PostMapping(path = "/changeEmployeeStatus")
    public Object changeEmployeeStatus(@RequestBody Map<String, String> requestParam, HttpServletRequest request) {
        return employeeService.changeEmployeeStatus(requestParam, request);
    }

    @PostMapping(path = "/addEmployeeDeviceId")
    public Object addEmployeeDeviceId(@RequestBody Map<String, String> requestParam) {
        return employeeService.addEmployeeDeviceId(requestParam);
    }

    @RequestMapping(value = "/mLogin", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthenticateToken(@RequestBody Map<String, String> request, HttpServletRequest req) throws Exception {
        ResponseMessage responseMessage = new ResponseMessage();
        String username = request.get("username");
        String password = request.get("password");
        try {
            Employee userDetails = employeeRepository.getByMobileNumberAndTextPassword(username, password);
            if(userDetails != null){
                if (userDetails.getStatus()) {
                    Object jwtToken = jwtTokenUtil.generateTokenForMobile(req, userDetails.getMobileNumber().toString());
                    responseMessage.setMessage("Login success");
                    responseMessage.setResponse(jwtToken);
                    responseMessage.setResponseStatus(HttpStatus.OK.value());
                } else {
                    responseMessage.setMessage("Unauthorized access to system, please contact to admin");
                    responseMessage.setResponseStatus(UNAUTHORIZED.value());
                }
            } else {
                responseMessage.setMessage("Incorrect username or password");
                responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            }
        } catch (Exception e1) {
            System.out.println(e1.getMessage());
            System.out.println("login fail");
            responseMessage.setMessage("User not found");
            responseMessage.setResponseStatus(HttpStatus.NOT_FOUND.value());
            return ResponseEntity.ok(responseMessage);
        }
        return ResponseEntity.ok(responseMessage);
    }

    @GetMapping("/getTokenDuration")
    public void getTokenDuration(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String accessAuthHeader = request.getHeader("RefreshAuth");
        if (accessAuthHeader != null && accessAuthHeader.startsWith("Bearer ")) {
            try {
                String access_token = accessAuthHeader.substring("Bearer ".length());

                System.out.println("access_token " + access_token);

                Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY.getBytes());
                JWTVerifier verifier = JWT.require(algorithm).build();
                DecodedJWT decodedJWT = verifier.verify(access_token);
                String username = decodedJWT.getSubject();
                System.out.println("username " + username);
                Date tokenExpiryDate = decodedJWT.getExpiresAt();
                LocalDateTime tokExpdt = convertToLocalDateTimeViaMilisecond(tokenExpiryDate);
                LocalDateTime currentDate = LocalDateTime.now();

                long minutes = currentDate.until(tokExpdt, ChronoUnit.MINUTES);
                Map<String, String> result = new HashMap<>();
                result.put("tokenDuration", String.valueOf(minutes));
                result.put("responseStatus", String.valueOf(OK.value()));
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(response.getOutputStream(), result);
            } catch (Exception exception) {
                response.setHeader("error", exception.getMessage());
                response.setStatus(UNAUTHORIZED.value());
                //response.sendError(FORBIDDEN.value());
                Map<String, String> error = new HashMap<>();
                error.put("responseStatus", String.valueOf(UNAUTHORIZED.value()));
                error.put("error_message", exception.getMessage());
                error.put("message", "session destroyed plz login");
                response.setContentType(MimeTypeUtils.APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(response.getOutputStream(), error);
            }
        } else {
            throw new RuntimeException("Refresh token is missing");
        }
    }

    public LocalDateTime convertToLocalDateTimeViaMilisecond(Date dateToConvert) {
        return Instant.ofEpochMilli(dateToConvert.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    @GetMapping("/AppToken/refresh")
    public void refreshAppToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String authorizationHeader = request.getHeader(AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            try {
                String refresh_token = authorizationHeader.substring("Bearer ".length());
                Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY.getBytes());
                JWTVerifier verifier = JWT.require(algorithm).build();
                DecodedJWT decodedJWT = verifier.verify(refresh_token);
                String username = decodedJWT.getSubject();
                Employee employee = (Employee) employeeService.findEmp(Long.valueOf(username));

                String access_token = JWT.create().withSubject(employee.getMobileNumber().toString())
                        .withExpiresAt(new Date(System.currentTimeMillis() + 7 * 60 * 1000))
                        .withIssuer(request.getRequestURL().toString())
                        .withClaim("employeeName", utility.getEmployeeName(employee))
                        .withClaim("username", employee.getMobileNumber().toString())
                        .withClaim("userId", employee.getId())
                        .withClaim("isSuperAdmin", false).sign(algorithm);

                Map<String, String> tokens = new HashMap<>();
                tokens.put("access_token", access_token);
                tokens.put("refresh_token", refresh_token);
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

    @PostMapping("/mobile/sendOtp")
    public Object sendOtp(@RequestBody Map<String, String> request) {
        return employeeService.sendOtp(request);
    }

    @PostMapping("/mobile/verifyOtp")
    public Object verifyOtp(@RequestBody Map<String, String> request) {
        return employeeService.verifyOtp(request);
    }

    @PostMapping("/mobile/forgetPassword")
    public Object forgetPassword(@RequestBody Map<String, String> request, HttpServletRequest httpServletRequest) {
        return employeeService.forgetPassword(request, httpServletRequest);
    }

    @PostMapping("/mobile/checkMobileNoExists")
    public Object checkMobileNumberExists(@RequestBody Map<String, String> request) {
        return employeeService.checkMobileNumberExists(request);
    }

    @PostMapping("/fileUpload")
    public Object fileUpload(MultipartHttpServletRequest request) {
        return employeeService.fileUpload(request);
    }

    @GetMapping(path = "/listOfEmployee")
    public Object listOfEmployee(HttpServletRequest httpServletRequest) {
        JsonObject res = employeeService.listOfEmployee(httpServletRequest);
        return res.toString();
    }

    @PostMapping(path = "/mobile/getEmployeeList")
    public Object getEmployeeList(@RequestBody Map<String, String> jsonRequest, HttpServletRequest request) {
        return employeeService.getEmployeeList(jsonRequest, request).toString();
    }


    @PostMapping(path = "/orderByEmployee")
    public Object orderByEmployee(HttpServletRequest request) {
        JsonObject res = employeeService.orderByEmployee(request);
        return res.toString();
    }
    @GetMapping(path = "/mobile/employeeList")
    public Object employeeList() {
        JsonObject res = employeeService.employeeList();
        return res.toString();
    }
}
