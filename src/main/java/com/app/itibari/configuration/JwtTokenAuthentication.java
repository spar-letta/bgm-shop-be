package com.app.itibari.configuration;

import com.app.itibari.dataTypes.AgentType;
import com.app.itibari.dataTypes.CustomerStatus;
import com.app.itibari.dataTypes.MobileUserType;
import com.app.itibari.models.entities.Customers;
import com.app.itibari.models.entities.DeliveryAgents;
import com.app.itibari.models.entities.Person;
import com.app.itibari.models.entities.SalesAgents;
import com.app.itibari.models.request.*;
import com.app.itibari.models.response.LoginResponse;
import com.app.itibari.models.response.MobileResponse;
import com.app.itibari.repository.CustomersRepository;
import com.app.itibari.repository.DeliveryAgentsRepository;
import com.app.itibari.repository.PersonRepository;
import com.app.itibari.repository.SalesAgentsRepository;
import com.app.itibari.services.ItibariSmsService;
import com.app.itibari.services.DeliveryAgentLoginSessionService;
import com.app.itibari.utils.exceptions.rest.BadRequestRestApiException;
import com.app.itibari.utils.helpers.AgentActiveChecker;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class JwtTokenAuthentication {
    private final PersonRepository personRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtToken jwtToken;
    private final ItibariSmsService itibariSmsService;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final CustomersRepository customersRepository;
    private final DeliveryAgentsRepository deliveryAgentsRepository;
    private final SalesAgentsRepository salesAgentsRepository;
    private final AgentActiveChecker agentActiveChecker;
    private final DeliveryAgentLoginSessionService deliveryAgentLoginSessionService;

    public JwtTokenAuthentication(PersonRepository personRepository, AuthenticationManager authenticationManager, JwtToken jwtToken, ItibariSmsService itibariSmsService, PasswordEncoder passwordEncoder, UserService userService, CustomersRepository customersRepository, DeliveryAgentsRepository deliveryAgentsRepository, SalesAgentsRepository salesAgentsRepository, AgentActiveChecker agentActiveChecker, DeliveryAgentLoginSessionService deliveryAgentLoginSessionService) {
        this.personRepository = personRepository;
        this.authenticationManager = authenticationManager;
        this.jwtToken = jwtToken;
        this.itibariSmsService = itibariSmsService;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
        this.customersRepository = customersRepository;
        this.deliveryAgentsRepository = deliveryAgentsRepository;
        this.salesAgentsRepository = salesAgentsRepository;
        this.agentActiveChecker = agentActiveChecker;
        this.deliveryAgentLoginSessionService = deliveryAgentLoginSessionService;
    }


    public LoginResponse generateJwtToken(LoginRequest loginRequest) {
        log.info("LoginRequest: {}", loginRequest);

        try {
            UserDetails userDetails = userService.loadUserByUsername(loginRequest.getUsername());

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();


            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = jwtToken.generateJwtToken(authentication, roles);

            Person person = personRepository.findByEmailAndDeletedIsFalse(loginRequest.getUsername())
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("User not found")
                            .userMessage("User not found"));

            return new LoginResponse(token, person.getFullName());
        } catch (UsernameNotFoundException e) {
            throw new BadRequestRestApiException()
                    .developerMessage("User not found")
                    .userMessage("User not found");
        } catch (AuthenticationException e) {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid username or password")
                    .userMessage("Invalid username or password");
        }
    }


    public void logout(String email) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getName().equals(email)) {
            SecurityContextHolder.clearContext();
        }
    }

    public void resetPassword(ResetPasswordRequest resetPasswordRequest) {
        Person person = personRepository.findByEmailAndDeletedIsFalse(resetPasswordRequest.getEmail())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("User not found")
                        .userMessage("User not found"));

        String pin = generatePin();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(5);

        person.setResetPin(pin);
        person.setExpiryDate(expiryTime);


        itibariSmsService.send(person.getPhoneNumber(), String.format("Your reset pin is %s and expires at %s", pin, expiryTime));

        personRepository.save(person);
    }

    private String generatePin() {
        return RandomStringUtils.randomNumeric(6);
    }

    public void confirmPassword(ConfirmPasswordRequest confirmPasswordRequest) {

        Person person = personRepository.findByEmailAndDeletedIsFalse(confirmPasswordRequest.getEmail())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("User not found")
                        .userMessage("User not found"));

        if (!person.getResetPin().equalsIgnoreCase(confirmPasswordRequest.getPin()))
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid pin")
                    .userMessage("Invalid pin");

        if (LocalDateTime.now().isAfter(person.getExpiryDate()))
            throw new BadRequestRestApiException()
                    .developerMessage("Pin expired")
                    .userMessage("Pin expired");

        if (!confirmPasswordRequest.getPassword().equalsIgnoreCase(confirmPasswordRequest.getConfirmPassword()))
            throw new BadRequestRestApiException()
                    .developerMessage("Passwords do not match")
                    .userMessage("Passwords do not match");

        person.setPassword(passwordEncoder.encode(confirmPasswordRequest.getPassword()));
        person.setResetPin(null);
        person.setExpiryDate(null);

        itibariSmsService.send(person.getPhoneNumber(), "Password reset successful");

        personRepository.save(person);

    }

    public MobileResponse mobileResponse(MobileLoginRequest mobileLoginRequest, String appVersion) {

        String pin = generatePin();

        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(5);

        if (mobileLoginRequest.getMobileUserType() == MobileUserType.Customer) {
            Optional<Customers> optionalCustomer = customersRepository.findByPhoneNumberAndDeletedIsFalse(mobileLoginRequest.getPhoneNumber());

            if (optionalCustomer.isPresent()) {
                Customers customer = optionalCustomer.get();

                if (customer.getStatus() == CustomerStatus.De_Activated || !customer.isActive()) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Customer is De_Activated")
                            .userMessage("Sorry!. Customer is De_Activated");
                }

                if (customer.getParentCustomer() != null && !customer.isCanChildCustomerLogin()) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Child customer cannot login")
                            .userMessage("Sorry!. Child Shop cannot login");
                }

                customer.setOtpCode(pin);
                customer.setExpiresAt(expiryTime);
                itibariSmsService.send(customer.getPhoneNumber(), String.format("Your OTP pin is %s and expires at %s", pin, expiryTime));
                customersRepository.save(customer);
                return new MobileResponse("Enter otp", mobileLoginRequest.getPhoneNumber(), pin);
            } else {

                itibariSmsService.send(mobileLoginRequest.getPhoneNumber(), String.format("Your OTP pin is %s", pin));
                return new MobileResponse("Complete Registration", mobileLoginRequest.getPhoneNumber(), pin);
            }
        } else if (mobileLoginRequest.getMobileUserType() == MobileUserType.DeliveryAgent) {


            Optional<DeliveryAgents> optionalDeliveryAgent = deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse(mobileLoginRequest.getPhoneNumber());

            if (optionalDeliveryAgent.isPresent()) {
                DeliveryAgents deliveryAgent = optionalDeliveryAgent.get();

                if (StringUtils.isNotBlank(deliveryAgent.getPassword())) {
                    if (StringUtils.isBlank(mobileLoginRequest.getPassword())) {
                        throw new BadRequestRestApiException()
                                .developerMessage("Password is required")
                                .userMessage("Password is required");
                    }

                    if (!passwordEncoder.matches(mobileLoginRequest.getPassword(), deliveryAgent.getPassword())) {
                        throw new BadRequestRestApiException()
                                .developerMessage("Invalid password")
                                .userMessage("Invalid password");
                    }

                    var session = deliveryAgentLoginSessionService.openSession(deliveryAgent, mobileLoginRequest.getDeviceId(),
                            mobileLoginRequest.getDeviceName(), mobileLoginRequest.getDeviceModel(), mobileLoginRequest.getPlatform(), appVersion);
                    String token = jwtToken.generateMobilePlatformJwtToken(MobileUserType.DeliveryAgent,
                            deliveryAgent.getPhoneNumber(), session.getPublicId());
                    return new MobileResponse("Logged in", deliveryAgent.getPhoneNumber(), token, deliveryAgent.getFullName(),
                            Boolean.TRUE.equals(deliveryAgent.getForcePasswordReset()));
                }

                deliveryAgent.setOtpCode(pin);
                deliveryAgent.setExpiresAt(expiryTime);
                deliveryAgent.setForcePasswordReset(true);
                itibariSmsService.send(deliveryAgent.getPhoneNumber(), String.format("Your OTP pin is %s and expires at %s", pin, expiryTime));
                deliveryAgentsRepository.save(deliveryAgent);
                return new MobileResponse("Reset Password", mobileLoginRequest.getPhoneNumber(), pin, true);
            } else {
                throw new BadRequestRestApiException()
                        .developerMessage("Invalid mobile user type")
                        .userMessage("Invalid mobile user type");
            }
        } else if (mobileLoginRequest.getMobileUserType() == MobileUserType.SalesAgent) {

            Optional<SalesAgents> optionalSalesAgent = salesAgentsRepository.findSalesAgentsByPhoneNumberAndDeletedIsFalse(mobileLoginRequest.getPhoneNumber());

            if (optionalSalesAgent.isPresent()) {
                SalesAgents salesAgent = optionalSalesAgent.get();

                agentActiveChecker.checkIfAgentIsActive(salesAgent.getPublicId(), AgentType.SALES_AGENT);

                if (StringUtils.isNotBlank(salesAgent.getPassword())) {
                    if (StringUtils.isBlank(mobileLoginRequest.getPassword())) {
                        throw new BadRequestRestApiException()
                                .developerMessage("Password is required")
                                .userMessage("Password is required");
                    }

                    if (!passwordEncoder.matches(mobileLoginRequest.getPassword(), salesAgent.getPassword())) {
                        throw new BadRequestRestApiException()
                                .developerMessage("Invalid password")
                                .userMessage("Invalid password");
                    }

                    String token = jwtToken.generateMobilePlatformJwtToken(MobileUserType.SalesAgent, salesAgent.getPhoneNumber());
                    return new MobileResponse("Logged in", salesAgent.getPhoneNumber(), token, salesAgent.getEmail(),
                            Boolean.TRUE.equals(salesAgent.getForcePasswordReset()));
                }

                salesAgent.setOtpCode(pin);
                salesAgent.setExpiresAt(expiryTime);
                salesAgent.setForcePasswordReset(true);
                itibariSmsService.send(salesAgent.getPhoneNumber(), String.format("Your OTP pin is %s and expires at %s", pin, expiryTime));
                salesAgentsRepository.save(salesAgent);
                return new MobileResponse("Reset Password", mobileLoginRequest.getPhoneNumber(), pin, true);
            } else {
                throw new BadRequestRestApiException()
                        .developerMessage("Invalid mobile user type")
                        .userMessage("Invalid mobile user type");
            }

        } else
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid user type")
                    .userMessage("Invalid user type");

    }

    public LoginResponse mobileOTPConfirm(ConfirmRequest confirmRequest, String appVersion) {


        if (confirmRequest.getMobileUserType() == MobileUserType.Customer) {
            Optional<Customers> optionalCustomer = customersRepository.findByPhoneNumberAndDeletedIsFalse(confirmRequest.getPhoneNumber());

            if (optionalCustomer.isPresent()) {
                Customers customer = optionalCustomer.get();

                if (customer.getStatus() == CustomerStatus.De_Activated) {
                    throw new BadRequestRestApiException()
                            .developerMessage("Customer is De_Activated")
                            .userMessage("Sorry!. Customer is De_Activated");
                }

                if (customer.getAttemptCount() != null) {
                    if (customer.getAttemptCount() >= 3) {
                        throw new BadRequestRestApiException()
                                .developerMessage("Too many attempts")
                                .userMessage("Too many attempts");
                    }
                    customer.setAttemptCount(customer.getAttemptCount() + 1);
                }


                if (!customer.getOtpCode().equalsIgnoreCase(confirmRequest.getOtp()))
                    throw new BadRequestRestApiException()
                            .developerMessage("Invalid otp")
                            .userMessage("Invalid otp");

                if (LocalDateTime.now().isAfter(customer.getExpiresAt()))
                    throw new BadRequestRestApiException()
                            .developerMessage("Otp expired")
                            .userMessage("Otp expired");


                customer.setOtpCode(null);
                customer.setExpiresAt(null);
                customer.setIsVerified(true);
                customer.setAttemptCount(0);

                customersRepository.save(customer);

                return new LoginResponse(jwtToken.generateMobilePlatformJwtToken(MobileUserType.Customer, customer.getPhoneNumber()), customer.getEmail());

            } else {
                throw new BadRequestRestApiException()
                        .developerMessage("Customer not found")
                        .userMessage("Customer not found");
            }
        } else if (confirmRequest.getMobileUserType() == MobileUserType.DeliveryAgent) {
            Optional<DeliveryAgents> optionalDeliveryAgent = deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse(confirmRequest.getPhoneNumber());

            if (optionalDeliveryAgent.isPresent()) {
                DeliveryAgents deliveryAgent = optionalDeliveryAgent.get();
                if (deliveryAgent.getAttemptCount() != null) {
                    if (deliveryAgent.getAttemptCount() >= 3) {
                        throw new BadRequestRestApiException()
                                .developerMessage("Too many attempts")
                                .userMessage("Too many attempts");
                    }
                    deliveryAgent.setAttemptCount(deliveryAgent.getAttemptCount() + 1);
                }

                if (!deliveryAgent.getOtpCode().equalsIgnoreCase(confirmRequest.getOtp()))
                    throw new BadRequestRestApiException()
                            .developerMessage("Invalid otp")
                            .userMessage("Invalid otp");

                if (LocalDateTime.now().isAfter(deliveryAgent.getExpiresAt()))
                    throw new BadRequestRestApiException()
                            .developerMessage("Otp expired")
                            .userMessage("Otp expired");

                deliveryAgent.setOtpCode(null);
                deliveryAgent.setExpiresAt(null);
                deliveryAgent.setIsVerified(true);
                deliveryAgent.setAttemptCount(0);
                deliveryAgentsRepository.save(deliveryAgent);

                var session = deliveryAgentLoginSessionService.openSession(deliveryAgent, confirmRequest.getDeviceId(),
                        confirmRequest.getDeviceName(), confirmRequest.getDeviceModel(), confirmRequest.getPlatform(), appVersion);
                return new LoginResponse(jwtToken.generateMobilePlatformJwtToken(MobileUserType.DeliveryAgent,
                                deliveryAgent.getPhoneNumber(), session.getPublicId()),
                        deliveryAgent.getFullName(), Boolean.TRUE.equals(deliveryAgent.getForcePasswordReset()));
            } else {
                throw new BadRequestRestApiException()
                        .developerMessage("Delivery agent not found")
                        .userMessage("Delivery agent not found");
            }
        } else if (confirmRequest.getMobileUserType() == MobileUserType.SalesAgent) {

            Optional<SalesAgents> optionalSalesAgent = salesAgentsRepository.findSalesAgentsByPhoneNumberAndDeletedIsFalse(confirmRequest.getPhoneNumber());

            if (optionalSalesAgent.isPresent()) {
                SalesAgents salesAgent = optionalSalesAgent.get();
                if (salesAgent.getAttemptCount() != null) {
                    if (salesAgent.getAttemptCount() >= 3) {
                        throw new BadRequestRestApiException()
                                .developerMessage("Too many attempts")
                                .userMessage("Too many attempts");
                    }
                    salesAgent.setAttemptCount(salesAgent.getAttemptCount() + 1);
                }

                if (!salesAgent.getOtpCode().equalsIgnoreCase(confirmRequest.getOtp()))
                    throw new BadRequestRestApiException()
                            .developerMessage("Invalid otp")
                            .userMessage("Invalid otp");

                if (LocalDateTime.now().isAfter(salesAgent.getExpiresAt()))
                    throw new BadRequestRestApiException()
                            .developerMessage("Otp expired")
                            .userMessage("Otp expired");

                salesAgent.setOtpCode(null);
                salesAgent.setExpiresAt(null);
                salesAgent.setIsVerified(true);
                salesAgent.setAttemptCount(0);
                salesAgentsRepository.save(salesAgent);

                return new LoginResponse(jwtToken.generateMobilePlatformJwtToken(MobileUserType.SalesAgent, salesAgent.getPhoneNumber()),
                        salesAgent.getEmail(), Boolean.TRUE.equals(salesAgent.getForcePasswordReset()));
            } else {
                throw new BadRequestRestApiException()
                        .developerMessage("Sales agent not found")
                        .userMessage("Sales agent not found");
            }
        } else {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid user type")
                    .userMessage("Invalid user type");
        }

    }

    public MobileResponse mobileForgotPassword(MobileForgotPasswordRequest forgotPasswordRequest) {

        validateAgentPasswordRequest(forgotPasswordRequest.getPhoneNumber(), forgotPasswordRequest.getMobileUserType());

        String pin = generatePin();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(5);

        if (forgotPasswordRequest.getMobileUserType() == MobileUserType.DeliveryAgent) {
            DeliveryAgents deliveryAgent = deliveryAgentsRepository
                    .findDeliveryAgentsByPhoneNumberAndDeletedIsFalse(forgotPasswordRequest.getPhoneNumber())
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Delivery agent not found")
                            .userMessage("Delivery agent not found"));

            deliveryAgent.setOtpCode(pin);
            deliveryAgent.setExpiresAt(expiryTime);
            deliveryAgent.setAttemptCount(0);
            deliveryAgent.setForcePasswordReset(true);
            deliveryAgentsRepository.save(deliveryAgent);

            itibariSmsService.send(deliveryAgent.getPhoneNumber(), String.format("Your password reset OTP is %s and expires at %s", pin, expiryTime));
            return new MobileResponse("Reset", deliveryAgent.getPhoneNumber(), pin, true);
        }

        SalesAgents salesAgent = salesAgentsRepository
                .findSalesAgentsByPhoneNumberAndDeletedIsFalse(forgotPasswordRequest.getPhoneNumber())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sales agent not found")
                        .userMessage("Sales agent not found"));

        agentActiveChecker.checkIfAgentIsActive(salesAgent.getPublicId(), AgentType.SALES_AGENT);

        salesAgent.setOtpCode(pin);
        salesAgent.setExpiresAt(expiryTime);
        salesAgent.setAttemptCount(0);
        salesAgent.setForcePasswordReset(true);
        salesAgentsRepository.save(salesAgent);

        itibariSmsService.send(salesAgent.getPhoneNumber(), String.format("Your password reset OTP is %s and expires at %s", pin, expiryTime));
        return new MobileResponse("Enter otp", salesAgent.getPhoneNumber(), pin, true);
    }

    public LoginResponse mobileResetPassword(MobileResetPasswordRequest resetPasswordRequest, String appVersion) {

        validateAgentPasswordRequest(resetPasswordRequest.getPhoneNumber(), resetPasswordRequest.getMobileUserType());


        if (StringUtils.isBlank(resetPasswordRequest.getPassword())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Password is required")
                    .userMessage("Password is required");
        }

        if (resetPasswordRequest.getMobileUserType() == MobileUserType.DeliveryAgent) {
            DeliveryAgents deliveryAgent = deliveryAgentsRepository
                    .findDeliveryAgentsByPhoneNumberAndDeletedIsFalse(resetPasswordRequest.getPhoneNumber())
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Delivery agent not found")
                            .userMessage("Delivery agent not found"));

            if (deliveryAgent.getAttemptCount() != null && deliveryAgent.getAttemptCount() >= 3) {
                throw new BadRequestRestApiException()
                        .developerMessage("Too many attempts")
                        .userMessage("Too many attempts");
            }

            deliveryAgent.setAttemptCount(deliveryAgent.getAttemptCount() == null ? 1 : deliveryAgent.getAttemptCount() + 1);


            if (deliveryAgent.getExpiresAt() == null || LocalDateTime.now().isAfter(deliveryAgent.getExpiresAt())) {
                throw new BadRequestRestApiException()
                        .developerMessage("Otp expired")
                        .userMessage("Otp expired");
            }

            deliveryAgent.setPassword(passwordEncoder.encode(resetPasswordRequest.getPassword()));
            deliveryAgent.setOtpCode(null);
            deliveryAgent.setExpiresAt(null);
            deliveryAgent.setIsVerified(true);
            deliveryAgent.setAttemptCount(0);
            deliveryAgent.setForcePasswordReset(false);
            deliveryAgentsRepository.save(deliveryAgent);

            itibariSmsService.send(deliveryAgent.getPhoneNumber(), "Password reset successful");

            var session = deliveryAgentLoginSessionService.openSession(deliveryAgent, resetPasswordRequest.getDeviceId(),
                    resetPasswordRequest.getDeviceName(), resetPasswordRequest.getDeviceModel(), resetPasswordRequest.getPlatform(), appVersion);
            return new LoginResponse(jwtToken.generateMobilePlatformJwtToken(MobileUserType.DeliveryAgent,
                            deliveryAgent.getPhoneNumber(), session.getPublicId()),
                    deliveryAgent.getFullName(), false);
        }

        SalesAgents salesAgent = salesAgentsRepository
                .findSalesAgentsByPhoneNumberAndDeletedIsFalse(resetPasswordRequest.getPhoneNumber())
                .orElseThrow(() -> new BadRequestRestApiException()
                        .developerMessage("Sales agent not found")
                        .userMessage("Sales agent not found"));

        agentActiveChecker.checkIfAgentIsActive(salesAgent.getPublicId(), AgentType.SALES_AGENT);

        if (salesAgent.getAttemptCount() != null && salesAgent.getAttemptCount() >= 3) {
            throw new BadRequestRestApiException()
                    .developerMessage("Too many attempts")
                    .userMessage("Too many attempts");
        }

        salesAgent.setAttemptCount(salesAgent.getAttemptCount() == null ? 1 : salesAgent.getAttemptCount() + 1);

        if (salesAgent.getExpiresAt() == null || LocalDateTime.now().isAfter(salesAgent.getExpiresAt())) {
            throw new BadRequestRestApiException()
                    .developerMessage("Otp expired")
                    .userMessage("Otp expired");
        }

        salesAgent.setPassword(passwordEncoder.encode(resetPasswordRequest.getPassword()));
        salesAgent.setOtpCode(null);
        salesAgent.setExpiresAt(null);
        salesAgent.setIsVerified(true);
        salesAgent.setAttemptCount(0);
        salesAgent.setForcePasswordReset(false);
        salesAgentsRepository.save(salesAgent);

        itibariSmsService.send(salesAgent.getPhoneNumber(), "Password reset successful");

        return new LoginResponse(jwtToken.generateMobilePlatformJwtToken(MobileUserType.SalesAgent, salesAgent.getPhoneNumber()),
                salesAgent.getEmail(), false);
    }

    private void validateAgentPasswordRequest(String phoneNumber, MobileUserType mobileUserType) {

        if (mobileUserType == null || (mobileUserType != MobileUserType.DeliveryAgent && mobileUserType != MobileUserType.SalesAgent)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid user type")
                    .userMessage("Invalid user type");
        }

        if (StringUtils.isBlank(phoneNumber)) {
            throw new BadRequestRestApiException()
                    .developerMessage("Invalid phone number")
                    .userMessage("Sorry.Invalid phone number");
        }
    }

    public boolean validatePhoneNumber(String phoneNumber, MobileUserType mobileUserType) {
        validateAgentPasswordRequest(phoneNumber, mobileUserType);

        if (mobileUserType == MobileUserType.DeliveryAgent) {

            DeliveryAgents deliveryAgents = deliveryAgentsRepository.findDeliveryAgentsByPhoneNumberAndDeletedIsFalse(phoneNumber)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Delivery agent not found")
                            .userMessage("Delivery agent not found")
                    );
            return deliveryAgents.getPassword() != null && !deliveryAgents.getPassword().isBlank();
        }
        if (mobileUserType == MobileUserType.SalesAgent) {
            SalesAgents salesAgent = salesAgentsRepository
                    .findSalesAgentsByPhoneNumberAndDeletedIsFalse(phoneNumber)
                    .orElseThrow(() -> new BadRequestRestApiException()
                            .developerMessage("Sales agent not found")
                            .userMessage("Sales agent not found"));

            return salesAgent.getPassword() != null && !salesAgent.getPassword().isBlank();
        }
        return false;
    }

    public String getTestTokenForDA() {
        throw new UnsupportedOperationException("Test DA tokens require a persisted login session");
    }
}
