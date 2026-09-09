package nl.fred.lostandfound.web.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.fred.lostandfound.domain.service.AuthService;
import nl.fred.lostandfound.web.dto.LoginRequest;
import nl.fred.lostandfound.web.dto.LoginResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  @ResponseStatus(HttpStatus.OK)
  @PostMapping(value = "/login", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  public LoginResponse login(@Valid @RequestBody final LoginRequest request) {
    log.info("Login request for username '{}'.", request.username());
    return new LoginResponse(authService.login(request.username(), request.password()));
  }

}
