package com.dyes.backend.domain.user.service;

import com.dyes.backend.domain.admin.entity.Admin;
import com.dyes.backend.domain.admin.entity.RoleType;
import com.dyes.backend.domain.admin.repository.AdminRepository;
import com.dyes.backend.domain.user.controller.form.UserProfileModifyRequestForm;
import com.dyes.backend.domain.user.entity.Active;
import com.dyes.backend.domain.user.entity.Address;
import com.dyes.backend.domain.user.entity.User;
import com.dyes.backend.domain.user.entity.UserProfile;
import com.dyes.backend.domain.user.repository.UserProfileRepository;
import com.dyes.backend.domain.user.repository.UserRepository;
import com.dyes.backend.domain.user.service.response.*;
import com.dyes.backend.utility.provider.GoogleOauthSecretsProvider;
import com.dyes.backend.utility.provider.KakaoOauthSecretsProvider;
import com.dyes.backend.utility.provider.NaverOauthSecretsProvider;
import com.dyes.backend.utility.redis.RedisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisException;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@ToString
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    final private GoogleOauthSecretsProvider googleOauthSecretsProvider;
    final private NaverOauthSecretsProvider naverOauthSecretsProvider;
    final private KakaoOauthSecretsProvider kakaoOauthSecretsProvider;
    final private UserRepository userRepository;
    final private UserProfileRepository userProfileRepository;
    final private AdminRepository adminRepository;
    final private RedisService redisService;
    final private RestTemplate restTemplate;
    final private ObjectMapper objectMapper;

    // 구글 로그인
    @Override
    public String googleUserLogin(String code) {
        log.info("googleUserLogin start");

        final GoogleOauthAccessTokenResponse accessTokenResponse = googleRequestAccessTokenWithAuthorizationCode(code);
        ResponseEntity<GoogleOauthUserInfoResponse> userInfoResponse =
                googleRequestUserInfoWithAccessToken(accessTokenResponse.getAccessToken());

        log.info("userInfoResponse: " + userInfoResponse);
        User user = googleUserSave(accessTokenResponse, userInfoResponse.getBody());
        log.info("user" + user);

        String userToken = "google" + UUID.randomUUID();

        Optional<Admin> maybeAdmin = adminRepository.findByUser(user);
        if(maybeAdmin.isPresent()) {
            Admin admin = maybeAdmin.get();

            if(admin.getRoleType().equals(RoleType.MAIN_ADMIN)) {
                userToken = "mainadmin" + userToken;
            } else if (admin.getRoleType().equals(RoleType.NORMAL_ADMIN)) {
                userToken = "normaladmin" + userToken;
            }
        }

        redisService.setUserTokenAndUser(userToken, user.getAccessToken());

        final String redirectUrl = googleOauthSecretsProvider.getGOOGLE_REDIRECT_VIEW_URL();
        log.info("googleUserLogin end");

        return redirectUrl + userToken;
    }
    
    // 구글에서 인가 코드를 받으면 엑세스 코드 요청
    public GoogleOauthAccessTokenResponse googleRequestAccessTokenWithAuthorizationCode(String code) {

        log.info("requestAccessToken start");

        final String googleClientId = googleOauthSecretsProvider.getGOOGLE_AUTH_CLIENT_ID();
        final String googleRedirectUrl = googleOauthSecretsProvider.getGOOGLE_AUTH_REDIRECT_URL();
        final String googleClientSecret = googleOauthSecretsProvider.getGOOGLE_AUTH_SECRETS();
        final String googleTokenRequestUrl = googleOauthSecretsProvider.getGOOGLE_TOKEN_REQUEST_URL();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        body.add("code", code);
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        body.add("redirect_uri", googleRedirectUrl);
        body.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<GoogleOauthAccessTokenResponse> accessTokenResponse = restTemplate.postForEntity(googleTokenRequestUrl, requestEntity, GoogleOauthAccessTokenResponse.class);
        log.info("accessTokenRequest: " + accessTokenResponse);

        if(accessTokenResponse.getStatusCode() == HttpStatus.OK){
            log.info("requestAccessToken end");
            return accessTokenResponse.getBody();
        }
        log.info("requestAccessToken end");
        return null;
    }

    // 구글 엑세스 토큰으로 유저 정보 요청
    public ResponseEntity<GoogleOauthUserInfoResponse> googleRequestUserInfoWithAccessToken(String AccessToken) {
        log.info("requestUserInfoWithAccessTokenForSignIn start");

        HttpHeaders headers = new HttpHeaders();


        try {
            headers.add("Authorization","Bearer "+ AccessToken);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity(headers);
            log.info("request: " + request);

            ResponseEntity<GoogleOauthUserInfoResponse> response = restTemplate.exchange(
                    googleOauthSecretsProvider.getGOOGLE_USERINFO_REQUEST_URL(), HttpMethod.GET, request, GoogleOauthUserInfoResponse.class);
            log.info("response: " + response);

            log.info("requestUserInfoWithAccessTokenForSignIn end");

            return response;
        } catch (RestClientException e) {
            log.error("Error during requestUserInfoWithAccessTokenForSignIn: " + e.getMessage());

            String responseAccessToken = expiredGoogleAccessTokenRequester(AccessToken);

            headers.add("Authorization","Bearer "+ responseAccessToken);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity(headers);
            log.info("request: " + request);

            ResponseEntity<GoogleOauthUserInfoResponse> response = restTemplate.exchange(
                    googleOauthSecretsProvider.getGOOGLE_USERINFO_REQUEST_URL(), HttpMethod.GET, request, GoogleOauthUserInfoResponse.class);
            log.info("response: " + response);

            log.info("requestUserInfoWithAccessTokenForSignIn end");

            return response;
        }
    }

    // 구글 리프래쉬 토큰으로 엑세스 토큰 재발급 받은 후 유저 정보 요청
    public String expiredGoogleAccessTokenRequester (String accessToken) {
        log.info("expiredGoogleAccessTokenRequester start");

        User user = findUserByAccessTokenInDatabase(accessToken);

        String refreshToken = user.getRefreshToken();

        final String googleClientId = googleOauthSecretsProvider.getGOOGLE_AUTH_CLIENT_ID();
        final String googleClientSecret = googleOauthSecretsProvider.getGOOGLE_AUTH_SECRETS();
        final String googleRefreshTokenRequestUrl = googleOauthSecretsProvider.getGOOGLE_REFRESH_TOKEN_REQUEST_URL();
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        body.add("refresh_token", refreshToken);
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        body.add("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<GoogleOauthAccessTokenResponse> accessTokenResponse = restTemplate.postForEntity(googleRefreshTokenRequestUrl, requestEntity, GoogleOauthAccessTokenResponse.class);
        if(accessTokenResponse.getStatusCode() == HttpStatus.OK){
            user.setAccessToken(accessTokenResponse.getBody().getAccessToken());
            userRepository.save(user);
            log.info("expiredGoogleAccessTokenRequester end");
            return user.getAccessToken();
        }
        log.info("expiredGoogleAccessTokenRequester end");
        return null;
    }

    // 구글 유저 찾기 후 없으면 저장
    public User googleUserSave (GoogleOauthAccessTokenResponse accessTokenResponse, GoogleOauthUserInfoResponse userInfoResponse) {
        log.info("userCheckIsOurUser start");
        Optional<User> maybeUser = userRepository.findByStringId(userInfoResponse.getId());
        if (maybeUser.isEmpty()) {
            User user = User.builder()
                    .id(userInfoResponse.getId())
                    .active(Active.YES)
                    .accessToken(accessTokenResponse.getAccessToken())
                    .refreshToken(accessTokenResponse.getRefreshToken())
                    .build();
            userRepository.save(user);

            UserProfile userProfile = UserProfile.builder()
                    .user(user)
                    .id(userInfoResponse.getId())
                    .email(userInfoResponse.getEmail())
                    .profileImg(userInfoResponse.getPicture())
                    .build();
            userProfileRepository.save(userProfile);
            log.info("userCheckIsOurUser Not Our User");
            log.info("userCheckIsOurUser end");
            return user;
        } else if (maybeUser.get().getActive() == Active.NO) {
            User user = maybeUser.get();
            user.setActive(Active.YES);
            user.setAccessToken(accessTokenResponse.getAccessToken());
            user.setRefreshToken(accessTokenResponse.getRefreshToken());
            userRepository.save(user);

            UserProfile userProfile = UserProfile.builder()
                    .user(user)
                    .id(userInfoResponse.getId())
                    .email(userInfoResponse.getEmail())
                    .profileImg(userInfoResponse.getPicture())
                    .build();
            userProfileRepository.save(userProfile);
            log.info("userCheckIsOurUser rejoin user");
            log.info("userCheckIsOurUser end");
            return user;
        } else {
            log.info("userCheckIsOurUser OurUser");
            User user = maybeUser.get();
            user.setAccessToken(accessTokenResponse.getAccessToken());
            userRepository.save(user);
            log.info("userCheckIsOurUser end");
            return user;
        }
    }

    // 구글 회원 탈퇴
    public Boolean googleUserDelete (String userToken) throws NullPointerException{
        User user = findUserByAccessTokenInDatabase(redisService.getAccessToken(userToken));
        log.info("user.getAccessToken(): " + user.getAccessToken());
        String responseAccessToken = expiredGoogleAccessTokenRequester(user.getAccessToken());
        log.info("responseAccessToken: " + responseAccessToken);


        final String googleRevokeUrl = googleOauthSecretsProvider.getGOOGLE_REVOKE_URL();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("token", responseAccessToken);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<JsonNode> jsonNodeResponseEntity = restTemplate.postForEntity(googleRevokeUrl, requestEntity, JsonNode.class);
            JsonNode responseBody = jsonNodeResponseEntity.getBody();
            log.info("jsonNodeResponseEntity: " + responseBody);
            if (responseBody.has("error")) {
                String error = responseBody.get("error").asText();
                String errorDescription = responseBody.get("error_description").asText();

                log.error("Error: " + error + ", Error Description: " + errorDescription);
                return false;
            } else {
                user.setActive(Active.NO);
                userRepository.save(user);

                UserProfile userProfile = userProfileRepository.findByUser(user).get();
                userProfileRepository.delete(userProfile);

                redisService.deleteKeyAndValueWithUserToken(userToken);
                return true;
            }
        }catch (Exception e){
            log.error("Can't Delete User", e);
            return false;
        }
    }

    /*
    <------------------------------------------------------------------------------------------------------------------>
     */

    // 네이버 로그인
    @Override
    public String naverUserLogin(String code) {
        log.info("naverUserLogin start");

        final NaverOauthAccessTokenResponse accessTokenResponse = naverRequestAccessTokenWithAuthorizationCode(code);

        NaverOauthUserInfoResponse userInfoResponse =
                naverRequestUserInfoWithAccessToken(accessTokenResponse.getAccessToken());

        log.info("userInfoResponse: " + userInfoResponse);
        User user = naverUserSave(accessTokenResponse, userInfoResponse);
        log.info("user: " + user);

        String userToken = "naver" + UUID.randomUUID();

        Optional<Admin> maybeAdmin = adminRepository.findByUser(user);
        if(maybeAdmin.isPresent()) {
            Admin admin = maybeAdmin.get();

            if(admin.getRoleType().equals(RoleType.MAIN_ADMIN)) {
                userToken = "mainadmin" + userToken;
            } else if (admin.getRoleType().equals(RoleType.NORMAL_ADMIN)) {
                userToken = "normaladmin" + userToken;
            }
        }

        redisService.setUserTokenAndUser(userToken, user.getAccessToken());

        final String redirectUrl = naverOauthSecretsProvider.getNAVER_REDIRECT_VIEW_URL();
        log.info("naverUserLogin end");

        return redirectUrl + userToken;
    }

    // 네이버에서 인가 코드를 받으면 엑세스 코드 요청
    public NaverOauthAccessTokenResponse naverRequestAccessTokenWithAuthorizationCode(String code) {
        log.info("requestAccessToken start");

        final String naverClientId = naverOauthSecretsProvider.getNAVER_AUTH_CLIENT_ID();
        final String naverRedirectUrl = naverOauthSecretsProvider.getNAVER_AUTH_REDIRECT_URL();
        final String naverClientSecret = naverOauthSecretsProvider.getNAVER_AUTH_SECRETS();
        final String naverTokenRequestUrl = naverOauthSecretsProvider.getNAVER_TOKEN_REQUEST_URL();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        body.add("code", code);
        body.add("client_id", naverClientId);
        body.add("client_secret", naverClientSecret);
        body.add("redirect_uri", naverRedirectUrl);
        body.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<NaverOauthAccessTokenResponse> accessTokenResponse = restTemplate.postForEntity(naverTokenRequestUrl, requestEntity, NaverOauthAccessTokenResponse.class);
        log.info("accessTokenRequest: " + accessTokenResponse);

        if(accessTokenResponse.getStatusCode() == HttpStatus.OK){
            log.info("requestAccessToken end");
            return accessTokenResponse.getBody();
        }
        log.info("requestAccessToken end");
        return null;
    }

    // 네이버 엑세스 토큰으로 유저 정보 요청
    public NaverOauthUserInfoResponse naverRequestUserInfoWithAccessToken(String AccessToken) {
        log.info("requestUserInfoWithAccessTokenForSignIn start");

        HttpHeaders headers = new HttpHeaders();


        try {
            headers.add("Authorization","Bearer "+ AccessToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity(headers);
            log.info("request: " + request);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    naverOauthSecretsProvider.getNAVER_USERINFO_REQUEST_URL(), HttpMethod.GET, request, JsonNode.class);
            log.info("response: " + response);

            JsonNode responseNode = response.getBody().get("response");

            NaverOauthUserInfoResponse userInfoResponse =
                    objectMapper.convertValue(responseNode, NaverOauthUserInfoResponse.class);

            log.info("requestUserInfoWithAccessTokenForSignIn end");

            return userInfoResponse;
        } catch (RestClientException e) {
            log.error("Error during requestUserInfoWithAccessTokenForSignIn: " + e.getMessage());

            String responseAccessToken = expiredNaverAccessTokenRequester(AccessToken);
            headers.add("Authorization","Bearer "+ responseAccessToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity(headers);
            log.info("request: " + request);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    naverOauthSecretsProvider.getNAVER_USERINFO_REQUEST_URL(), HttpMethod.GET, request, JsonNode.class);
            log.info("response: " + response);

            JsonNode responseNode = response.getBody().get("response");

            NaverOauthUserInfoResponse userInfoResponse =
                    objectMapper.convertValue(responseNode, NaverOauthUserInfoResponse.class);

            log.info("requestUserInfoWithAccessTokenForSignIn end");

            return userInfoResponse;
        }

    }

    // 네이버 리프래쉬 토큰으로 엑세스 토큰 재발급 받은 후 유저 정보 요청
    public String expiredNaverAccessTokenRequester (String accessToken) {
        log.info("expiredNaverAccessTokenRequester start");

        User user = findUserByAccessTokenInDatabase(accessToken);

        String refreshToken = user.getRefreshToken();

        final String naverClientId = naverOauthSecretsProvider.getNAVER_AUTH_CLIENT_ID();
        final String naverClientSecret = naverOauthSecretsProvider.getNAVER_AUTH_SECRETS();
        final String naverRefreshTokenRequestUrl = naverOauthSecretsProvider.getNAVER_REFRESH_TOKEN_REQUEST_URL();
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        body.add("refresh_token", refreshToken);
        body.add("client_id", naverClientId);
        body.add("client_secret", naverClientSecret);
        body.add("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<NaverOauthAccessTokenResponse> accessTokenResponse = restTemplate.postForEntity(naverRefreshTokenRequestUrl, requestEntity, NaverOauthAccessTokenResponse.class);
        if(accessTokenResponse.getStatusCode() == HttpStatus.OK){
            user.setAccessToken(accessTokenResponse.getBody().getAccessToken());
            userRepository.save(user);
            log.info("expiredNaverAccessTokenRequester end");

            return user.getAccessToken();
        }
        log.info("expiredNaverAccessTokenRequester end");
        return null;
    }

    // 네이버 유저 찾기 후 없으면 저장
    public User naverUserSave (NaverOauthAccessTokenResponse accessTokenResponse, NaverOauthUserInfoResponse userInfoResponse) {
        log.info("userCheckIsOurUser start");
        Optional<User> maybeUser = userRepository.findByStringId(userInfoResponse.getId());
        if (maybeUser.isEmpty()) {
            User user = User.builder()
                    .id(userInfoResponse.getId())
                    .active(Active.YES)
                    .accessToken(accessTokenResponse.getAccessToken())
                    .refreshToken(accessTokenResponse.getRefreshToken())
                    .build();
            userRepository.save(user);

            UserProfile userProfile = UserProfile.builder()
                    .user(user)
                    .id(userInfoResponse.getId())
                    .contactNumber(userInfoResponse.getMobile_e164())
                    .email(userInfoResponse.getEmail())
                    .profileImg(userInfoResponse.getProfile_image())
                    .build();
            userProfileRepository.save(userProfile);
            log.info("userCheckIsOurUser NotOurUser");
            log.info("userCheckIsOurUser end");
            return user;
        } else if (maybeUser.get().getActive() == Active.NO) {
            User user = maybeUser.get();
            user.setActive(Active.YES);
            user.setAccessToken(accessTokenResponse.getAccessToken());
            user.setRefreshToken(accessTokenResponse.getRefreshToken());
            userRepository.save(user);

            UserProfile userProfile = UserProfile.builder()
                    .user(user)
                    .id(userInfoResponse.getId())
                    .contactNumber(userInfoResponse.getMobile_e164())
                    .email(userInfoResponse.getEmail())
                    .profileImg(userInfoResponse.getProfile_image())
                    .build();
            userProfileRepository.save(userProfile);
            log.info("userCheckIsOurUser rejoin user");
            log.info("userCheckIsOurUser end");
            return user;
        } else {
            log.info("userCheckIsOurUser OurUser");
            User user = maybeUser.get();
            user.setAccessToken(accessTokenResponse.getAccessToken());
            userRepository.save(user);
            log.info("userCheckIsOurUser end");
            return user;
        }
    }

    // 네이버 유저 탈퇴
    public Boolean naverUserDelete (String userToken) throws NullPointerException{
        User user = findUserByAccessTokenInDatabase(redisService.getAccessToken(userToken));
        log.info("user.getAccessToken(): " + user.getAccessToken());
        String responseAccessToken = expiredNaverAccessTokenRequester(user.getAccessToken());
        log.info("responseAccessToken: " + responseAccessToken);

        final String naverRevokeUrl = naverOauthSecretsProvider.getNAVER_REVOKE_URL();
        final String naverClientId = naverOauthSecretsProvider.getNAVER_AUTH_CLIENT_ID();
        final String naverSecrets = naverOauthSecretsProvider.getNAVER_AUTH_SECRETS();

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("access_token", responseAccessToken);
        body.add("client_id", naverClientId);
        body.add("client_secret", naverSecrets);
        body.add("grant_type", "delete");
        body.add("service_provider", "NAVER");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<JsonNode> jsonNodeResponseEntity = restTemplate.postForEntity(naverRevokeUrl, requestEntity, JsonNode.class);
            JsonNode responseBody = jsonNodeResponseEntity.getBody();
            log.info("jsonNodeResponseEntity: " + responseBody);
            if (responseBody.has("error")) {
                String error = responseBody.get("error").asText();
                String errorDescription = responseBody.get("error_description").asText();

                log.error("Error: " + error + ", Error Description: " + errorDescription);
                return false;
            } else {
                user.setActive(Active.NO);
                userRepository.save(user);

                UserProfile userProfile = userProfileRepository.findByUser(user).get();
                userProfileRepository.delete(userProfile);

                redisService.deleteKeyAndValueWithUserToken(userToken);
                return true;
            }
        }catch (Exception e){
            log.error("Can't Delete User", e);
            return false;
        }
    }
  
    /*
    <------------------------------------------------------------------------------------------------------------------>
     */

    // 카카오 로그인
    @Override
    public String kakaoUserLogin(String code) {
        // 카카오 서버에서 accessToken 받아오기
        KakaoAccessTokenResponseForm kakaoAccessTokenResponseForm = getAccessTokenFromKakao(code);
        final String accessToken = kakaoAccessTokenResponseForm.getAccess_token();
        final String refreshToken = kakaoAccessTokenResponseForm.getRefresh_token();
        String userToken = "kakao" + UUID.randomUUID();

        log.info("kakao accessToken: " + accessToken);
        log.info("kakao refreshToken: " + refreshToken);

        // 카카오 서버에서 받아온 accessToken으로 사용자 정보 받아오기
        KakaoUserInfoResponseForm kakaoUserInfoResponseForm = getUserInfoFromKakao(accessToken);

        // 받아온 사용자 id로 우리 DB에서 조회하기
        Optional<User> maybeUser = userRepository.findByStringId(kakaoUserInfoResponseForm.getId());

        // 없다면 회원가입(사용자, 사용자 프로필 생성)
        if(maybeUser.isEmpty()) {
            User user = new User(
                    kakaoUserInfoResponseForm.getId(),
                    kakaoAccessTokenResponseForm.getAccess_token(),
                    kakaoAccessTokenResponseForm.getRefresh_token(),
                    Active.YES);

            userRepository.save(user);

            UserProfile userProfile = UserProfile.builder()
                    .user(user)
                    .id(kakaoUserInfoResponseForm.getId())
                    .nickName(kakaoUserInfoResponseForm.getProperties().getNickname())
                    .profileImg(kakaoUserInfoResponseForm.getProperties().getProfile_image())
                    .build();

            userProfileRepository.save(userProfile);

            redisService.setUserTokenAndUser(userToken, accessToken);

            final String redirectUrl = kakaoOauthSecretsProvider.getKAKAO_REDIRECT_VIEW_URL();
            return redirectUrl + userToken;

        } else if(maybeUser.isPresent() && maybeUser.get().getActive().equals(Active.YES)) {

            // 활동하고 있는 회원이면 accessToken, refreshToken 갱신 후 로그인
            final User user = maybeUser.get();
            user.setAccessToken(accessToken);
            user.setRefreshToken(refreshToken);
            userRepository.save(user);

            Optional<Admin> maybeAdmin = adminRepository.findByUser(user);
            if(maybeAdmin.isPresent()) {
                Admin admin = maybeAdmin.get();

                if(admin.getRoleType().equals(RoleType.MAIN_ADMIN)) {
                    userToken = "mainadmin" + userToken;
                } else if (admin.getRoleType().equals(RoleType.NORMAL_ADMIN)) {
                    userToken = "normaladmin" + userToken;
                }
            }

            redisService.setUserTokenAndUser(userToken, accessToken);

            final String redirectUrl = kakaoOauthSecretsProvider.getKAKAO_REDIRECT_VIEW_URL();
            return redirectUrl + userToken;

        } else if(maybeUser.isPresent() && maybeUser.get().getActive().equals(Active.NO)) {

            // 탈퇴한 회원이면 Active YES로 변경 후 프로필 재생성
            final User user = maybeUser.get();
            user.setActive(Active.YES);
            user.setAccessToken(accessToken);
            user.setRefreshToken(refreshToken);
            userRepository.save(user);

            Optional<Admin> maybeAdmin = adminRepository.findByUser(user);
            if(maybeAdmin.isPresent()) {
                Admin admin = maybeAdmin.get();

                if(admin.getRoleType().equals(RoleType.MAIN_ADMIN)) {
                    userToken = "mainadmin" + userToken;
                } else if (admin.getRoleType().equals(RoleType.NORMAL_ADMIN)) {
                    userToken = "normaladmin" + userToken;
                }
            }

            UserProfile userProfile = UserProfile.builder()
                    .user(user)
                    .id(kakaoUserInfoResponseForm.getId())
                    .nickName(kakaoUserInfoResponseForm.getProperties().getNickname())
                    .profileImg(kakaoUserInfoResponseForm.getProperties().getProfile_image())
                    .build();

            userProfileRepository.save(userProfile);
            redisService.setUserTokenAndUser(userToken, accessToken);

            final String redirectUrl = kakaoOauthSecretsProvider.getKAKAO_REDIRECT_VIEW_URL();
            return redirectUrl + userToken;
        }

        return null;
    }

    // 카카오에서 인가 코드를 받으면 엑세스 토큰 요청
    public KakaoAccessTokenResponseForm getAccessTokenFromKakao(String code) {
        // 헤더 설정
        HttpHeaders httpHeaders = setHeaders();

        // 바디 설정
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("grant_type", "authorization_code");
        parameters.add("client_id", kakaoOauthSecretsProvider.getKAKAO_AUTH_RESTAPI_KEY());
        parameters.add("redirect_uri", kakaoOauthSecretsProvider.getKAKAO_AUTH_REDIRECT_URL());
        parameters.add("code", code);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(parameters, httpHeaders);

        ResponseEntity<KakaoAccessTokenResponseForm> kakaoAccessTokenResponseForm = restTemplate.postForEntity(
                kakaoOauthSecretsProvider.getKAKAO_TOKEN_REQUEST_URL(),
                requestEntity,
                KakaoAccessTokenResponseForm.class);

        return kakaoAccessTokenResponseForm.getBody();
    }

    // 카카오 엑세스 토큰으로 유저 정보 요청
    public KakaoUserInfoResponseForm getUserInfoFromKakao(String accessToken) {

        try {
            // 헤더 설정
            HttpHeaders httpHeaders = setHeaders();
            httpHeaders.add("Authorization", "Bearer " + accessToken);

            HttpEntity<String> requestEntity = new HttpEntity<>(httpHeaders);

            ResponseEntity<KakaoUserInfoResponseForm> kakaoUserInfoResponseForm = restTemplate.postForEntity(
                    kakaoOauthSecretsProvider.getKAKAO_USERINFO_REQUEST_URL(),
                    requestEntity,
                    KakaoUserInfoResponseForm.class);

            return kakaoUserInfoResponseForm.getBody();

        } catch (RestClientException e) {
            log.error("Error during requestUserInfoWithAccessTokenForSignIn: " + e.getMessage());
            KakaoAccessTokenResponseForm kakaoAccessTokenResponseForm = expiredKakaoAccessTokenRequester(accessToken);

            // 헤더 설정
            HttpHeaders httpHeaders = setHeaders();
            httpHeaders.add("Authorization", "Bearer " + kakaoAccessTokenResponseForm.getAccess_token());

            HttpEntity<String> requestEntity = new HttpEntity<>(httpHeaders);

            ResponseEntity<KakaoUserInfoResponseForm> kakaoUserInfoResponseForm = restTemplate.postForEntity(
                    kakaoOauthSecretsProvider.getKAKAO_USERINFO_REQUEST_URL(),
                    requestEntity,
                    KakaoUserInfoResponseForm.class);

            return kakaoUserInfoResponseForm.getBody();
        }
    }

    // 카카오 리프래쉬 토큰으로 엑세스 토큰 재발급
    public KakaoAccessTokenResponseForm expiredKakaoAccessTokenRequester (String accessToken) {

        final User user = findUserByAccessTokenInDatabase(accessToken);
        final String refreshToken = user.getRefreshToken();

        // 헤더 설정
        HttpHeaders httpHeaders = setHeaders();

        // 바디 설정
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("grant_type", "refresh_token");
        parameters.add("client_id", kakaoOauthSecretsProvider.getKAKAO_AUTH_RESTAPI_KEY());
        parameters.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(parameters, httpHeaders);

        ResponseEntity<KakaoAccessTokenResponseForm> kakaoAccessTokenResponseForm = restTemplate.postForEntity(
                kakaoOauthSecretsProvider.getKAKAO_REFRESH_TOKEN_REQUEST_URL(),
                requestEntity,
                KakaoAccessTokenResponseForm.class);

        final String renewAccessToken = kakaoAccessTokenResponseForm.getBody().getAccess_token();
        final String renewRefreshToken = kakaoAccessTokenResponseForm.getBody().getRefresh_token();

        user.setAccessToken(renewAccessToken);

        // refreshToken의 유효 기간이 1개월 미만인 경우 새로운 refreshToken을 받아오므로 새롭게 저장
        if(!renewAccessToken.equals(user.getRefreshToken())) {
            user.setRefreshToken(renewRefreshToken);
        }
        userRepository.save(user);

        return kakaoAccessTokenResponseForm.getBody();
    }

    // 카카오 유저 탈퇴
    public Boolean kakaoUserDelete (String userToken) throws NullPointerException{

        final String accessToken = redisService.getAccessToken(userToken);
        if(accessToken == null) {
            return false;
        }

        final User user = findUserByAccessTokenInDatabase(accessToken);
        if(user == null) {
            return false;
        }

        // 헤더 설정
        HttpHeaders httpHeaders = setHeaders();
        httpHeaders.add("Authorization", "Bearer " + accessToken);

        // 바디 설정
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("target_id_type", "user_id");
        parameters.add("target_id", user.getId());

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(parameters, httpHeaders);

        ResponseEntity<KakaoDisconnectUserIdResponseForm> kakaoDisconnectUserResponse = restTemplate.postForEntity(
                kakaoOauthSecretsProvider.getKAKAO_DISCONNECT_REQUEST_URL(),
                requestEntity,
                KakaoDisconnectUserIdResponseForm.class);

        try {
            String receivedUserId = kakaoDisconnectUserResponse.getBody().getId().toString();
            Optional<User> foundUser = userRepository.findByStringId(receivedUserId);
            Optional<UserProfile> foundUserProfile = userProfileRepository.findByUser(foundUser.get());
            if(foundUser.isEmpty() || foundUserProfile.isEmpty()) {
                log.info("Cannot find User");
                return false;
            }

            UserProfile withdrawalUserProfile = foundUserProfile.get();
            userProfileRepository.delete(withdrawalUserProfile);

            User withdrawalUser = foundUser.get();
            withdrawalUser.setActive(Active.NO);
            userRepository.save(withdrawalUser);

            UserLogOut(userToken);

            return true;
        } catch (RestClientException e) {
            log.error("Error during kakaoUserWithdrawal: " + e.getMessage());

            return false;
        }
    }
    
    /*
    <------------------------------------------------------------------------------------------------------------------>
     */

    public HttpHeaders setHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        httpHeaders.add("Accept", "application/json");

        return httpHeaders;
    }

    // 닉네임 중복 확인
    @Override
    public Boolean checkNickNameDuplicate(String nickName) {
        Optional<UserProfile> maybeUserProfile = userProfileRepository.findByNickName(nickName);

        if(maybeUserProfile.isPresent()) {
            log.info("nickname already exists");
            return false;
        }

        return true;
    }

    // 이메일 중복 확인
    @Override
    public Boolean checkEmailDuplicate(String email) {
        Optional<UserProfile> maybeUserProfile = userProfileRepository.findByEmail(email);

        if(maybeUserProfile.isPresent()) {
            log.info("email already exists");
            return false;
        }

        return true;
    }

    // 유저 프로필 가져오기
    @Override
    public UserProfileResponseForm getUserProfile(String userToken) {
        final User user = findUserByUserToken(userToken);
        if(user == null) {
            return null;
        }

        Optional<UserProfile> maybeUserProfile = userProfileRepository.findByUser(user);

        if(maybeUserProfile.isEmpty()) {
            UserProfileResponseForm userProfileResponseForm = new UserProfileResponseForm(user.getId());
            return userProfileResponseForm;
        }

        UserProfile userProfile = maybeUserProfile.get();
        UserProfileResponseForm userProfileResponseForm
                = new UserProfileResponseForm(
                    user.getId(),
                    userProfile.getNickName(),
                    userProfile.getEmail(),
                    userProfile.getProfileImg(),
                    userProfile.getContactNumber(),
                    userProfile.getAddress());

        return userProfileResponseForm;
    }

    // 유저 프로필 수정하기
    @Override
    public UserProfileResponseForm modifyUserProfile(UserProfileModifyRequestForm requestForm) {
        final User user = findUserByUserToken(requestForm.getUserToken());
        if(user == null) {
            return null;
        }

        Optional<UserProfile> maybeUserProfile = userProfileRepository.findByUser(user);
        if(maybeUserProfile.isEmpty()) {

            Address address = new Address(requestForm.getAddress(), requestForm.getZipCode(), requestForm.getAddressDetail());

            UserProfile userProfile = UserProfile.builder()
                    .id(user.getId())
                    .nickName(requestForm.getNickName())
                    .email(requestForm.getEmail())
                    .profileImg(requestForm.getProfileImg())
                    .contactNumber(requestForm.getContactNumber())
                    .address(address)
                    .user(user)
                    .build();

            userProfileRepository.save(userProfile);

            UserProfileResponseForm userProfileResponseForm
                    = new UserProfileResponseForm(
                        user.getId(),
                        userProfile.getNickName(),
                        userProfile.getEmail(),
                        userProfile.getProfileImg(),
                        userProfile.getContactNumber(),
                        userProfile.getAddress());

            return userProfileResponseForm;
        }

        Address address = new Address(requestForm.getAddress(), requestForm.getZipCode(), requestForm.getAddressDetail());
        UserProfile userProfile = maybeUserProfile.get();

        userProfile.setNickName(requestForm.getNickName());
        userProfile.setEmail(requestForm.getEmail());
        userProfile.setProfileImg(requestForm.getProfileImg());
        userProfile.setContactNumber(requestForm.getContactNumber());
        userProfile.setAddress(address);

        userProfileRepository.save(userProfile);

        UserProfileResponseForm userProfileResponseForm
                = new UserProfileResponseForm(
                    user.getId(),
                    userProfile.getNickName(),
                    userProfile.getEmail(),
                    userProfile.getProfileImg(),
                    userProfile.getContactNumber(),
                    userProfile.getAddress());

        return userProfileResponseForm;
    }

    // 유저 탈퇴하기
    @Override
    public boolean userWithdraw(String userToken) {
        String platform = divideUserByPlatform(userToken);
        if (platform.contains("google")) {
            log.info("divideUserByPlatform end");
            return googleUserDelete(userToken);
          
        } else if (platform.contains("naver")) {
            log.info("divideUserByPlatform end");
            return naverUserDelete(userToken);
          
        } else {
            log.info("divideUserByPlatform end");
            return kakaoUserDelete(userToken);
        }
    }

    /*
    <------------------------------------------------------------------------------------------------------------------>
     */

    // userToken으로 Redis에서 accessToken 조회 후 Oauth 서버로 사용자 정보 요청
    @Override
    public User findUserByUserToken (String userToken) {
        final String accessToken = redisService.getAccessToken(userToken);
        if(accessToken == null){
            log.info("accessToken is empty");
            return null;
        }

        String userId = "";
        if(userToken.contains("google")) {
            ResponseEntity<GoogleOauthUserInfoResponse> googleOauthUserInfoResponse
                    = googleRequestUserInfoWithAccessToken(accessToken);
            userId = googleOauthUserInfoResponse.getBody().getId();
        }

        if(userToken.contains("naver")) {
            NaverOauthUserInfoResponse naverOauthUserInfoResponse
                    = naverRequestUserInfoWithAccessToken(accessToken);
            userId = naverOauthUserInfoResponse.getId();
        }

        if(userToken.contains("kakao")) {
            KakaoUserInfoResponseForm kakaoUserInfoResponseForm
                    = getUserInfoFromKakao(accessToken);
            userId = kakaoUserInfoResponseForm.getId();
        }

        Optional<User> maybeUser = userRepository.findByStringId(userId);

        if(maybeUser.isEmpty()) {
            log.info("user is empty");
            return null;
        }

        User user = maybeUser.get();
        return user;
    }

    // accessToken으로 DB에서 사용자 조회
    public User findUserByAccessTokenInDatabase (String accessToken) {
        log.info("findUserByAccessTokenInDatabase Start");

        Optional<User> maybeUser = userRepository.findByAccessToken(accessToken);
        if (maybeUser.isEmpty()) {
            log.warn("사용자를 찾지 못함: access token - {}", accessToken);
            log.info("findUserByAccessTokenInDatabase end");
            return null;
        }
        User user = maybeUser.get();
        log.info("findUserByAccessTokenInDatabase end");
        return user;
    }

    // userToken으로 사용자 로그아웃
    public boolean UserLogOut (String userToken) {
        log.info("UserLogOut start");
            log.info("UserLogOut end");
            try {
                logOutWithDeleteKeyAndValueInRedis(userToken);
                return true;
            } catch (Exception e) {
                log.error("Can't logOut {}", e.getMessage(), e);
                return false;
            }
    }

    // 로그아웃 요청한 사용자의 플랫폼 판별
    public String divideUserByPlatform(String userToken) {
        log.info("divideUserByPlatform start");
        String platform;
        if (userToken.contains("google")){
            platform = "google";
            log.info("divideUserByPlatform end");
            return platform;
        } else if (userToken.contains("naver")) {
            platform = "naver";
            log.info("divideUserByPlatform end");
            return platform;
        } else {
            platform = "kakao";
            log.info("divideUserByPlatform end");
            return platform;
        }
    }

    // 로그아웃 요청한 사용자의 userToken
    public Boolean logOutWithDeleteKeyAndValueInRedis (String userToken) {
        log.info("logOutWithDeleteKeyAndValueInRedis start");
        try {
            redisService.deleteKeyAndValueWithUserToken(userToken);
            log.info("logOutWithDeleteKeyAndValueInRedis end");
            return true;
        } catch (RedisException e) {
            log.error("Can't not logout with this userToken: {}", userToken, e);
            log.info("logOutWithDeleteKeyAndValueInRedis end");
            return false;
        }
    }

}