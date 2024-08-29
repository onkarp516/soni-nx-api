package com.truethic.soninx.SoniNxAPI.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.truethic.soninx.SoniNxAPI.response.ResponseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

@Slf4j
public class CustomAuthorizationFilter extends OncePerRequestFilter {
    private final String SECRET_KEY = "m!j^d8#0en6j&rye8$$s%v)3f%i#ngm2e!%x1=s*h1ds&2ulqe&0ls";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (request.getServletPath().equals("/api/login") || request.getServletPath().equals("/api/token/refresh")
                || request.getServletPath().equals("/user-save") || request.getServletPath().equals("/uploads/**")) {
            try {
                System.out.println("request " + request);
                filterChain.doFilter(request, response);
            } catch (UsernameNotFoundException e) {
                System.out.println("User not found exception occurred .....");
            } catch (Exception e) {
                System.out.println("User not found exception occurred ....." + e.getMessage());
            }
        } else {
            ResponseMessage responseMessage = new ResponseMessage();
            String authorizationHeader = request.getHeader(AUTHORIZATION);
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                try {
                    String token = authorizationHeader.substring("Bearer ".length());
                    Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY.getBytes());
                    JWTVerifier verifier = JWT.require(algorithm).build();
                    DecodedJWT decodedJWT = verifier.verify(token);
                    String username = decodedJWT.getSubject();
                    System.out.println("username " + username);

                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    System.out.println("authenticationToken " + authenticationToken);
                    filterChain.doFilter(request, response);
                } catch (TokenExpiredException exception) {
                    log.error("Error logging in: {}", exception.getMessage());
                    response.setHeader("error", exception.getMessage());
                    response.setStatus(FORBIDDEN.value());
                    //response.sendError(FORBIDDEN.value());
                    Map<String, String> error = new HashMap<>();
                    error.put("message", "Token expired, please re-login");
                    error.put("error_message", exception.getMessage());
                    error.put("responseStatus", String.valueOf(FORBIDDEN.value()));
                    responseMessage.setMessage("Token Expiry...");
                    responseMessage.setResponseStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(APPLICATION_JSON_VALUE);
                    new ObjectMapper().writeValue(response.getOutputStream(), responseMessage);
                } catch (Exception exception) {
                    log.error("Error logging in: {}", exception.getMessage());
                    response.setHeader("error", exception.getMessage());
                    response.setStatus(UNAUTHORIZED.value());
                    //response.sendError(FORBIDDEN.value());
                    Map<String, String> error = new HashMap<>();
                    error.put("message", "exception occurred");
                    error.put("error_message", exception.getMessage());
                    error.put("status", String.valueOf(UNAUTHORIZED.value()));
                    responseMessage.setMessage("Internal Server Error...");
                    responseMessage.setResponseStatus(HttpStatus.UNAUTHORIZED.value());

                    response.setContentType(APPLICATION_JSON_VALUE);
                    new ObjectMapper().writeValue(response.getOutputStream(), responseMessage);
                }
            } else {
                filterChain.doFilter(request, response);
            }
        }
    }
}
