/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package org.entando.entando.aps.system.services.oauth2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import org.entando.entando.aps.system.services.oauth2.model.OAuth2AccessTokenImpl;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.common.DefaultOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

/**
 * @author E.Santoboni
 */
@ExtendWith(MockitoExtension.class)
public class ApiOAuth2TokenManagerTest {

    @Mock
    private OAuth2TokenDAO tokenDAO;

    @InjectMocks
    private ApiOAuth2TokenManager tokenManager;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void findTokensByUserName() {
        Mockito.lenient().when(tokenDAO.findTokensByClientIdAndUserName(Mockito.anyString(), Mockito.anyString())).thenReturn(new ArrayList<>());
        Collection<OAuth2AccessToken> tokens = tokenManager.findTokensByUserName("username");
        Assertions.assertNotNull(tokens);
    }

    @Test
    public void findTokensByClientIdAndUserName() {
        when(tokenDAO.findTokensByClientIdAndUserName(Mockito.anyString(), Mockito.anyString())).thenReturn(new ArrayList<>());
        Collection<OAuth2AccessToken> tokens = tokenManager.findTokensByClientIdAndUserName("clientId", "username");
        Assertions.assertNotNull(tokens);
    }

    @Test
    public void findTokensByClientId() {
        Mockito.lenient().when(tokenDAO.findTokensByClientIdAndUserName(Mockito.anyString(), Mockito.anyString())).thenReturn(new ArrayList<>());
        Collection<OAuth2AccessToken> tokens = tokenManager.findTokensByClientId("clientId");
        Assertions.assertNotNull(tokens);
    }

    @Test
    public void createAccessTokenForLocalUser() {
        OAuth2AccessToken token = this.tokenManager.createAccessTokenForLocalUser("username");
        Assertions.assertNotNull(token);
        Mockito.verify(tokenDAO, Mockito.times(1)).storeAccessToken(Mockito.any(OAuth2AccessToken.class), Mockito.eq(null));
        Assertions.assertTrue(token instanceof OAuth2AccessTokenImpl);
        Assertions.assertEquals("LOCAL_USER", ((OAuth2AccessTokenImpl) token).getClientId());
    }

    @Test
    public void readUnsupportedAuthenticationByAccessTokenObject() throws Exception {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            this.tokenManager.readAuthentication(this.createMockAccessToken());
        });
    }

    @Test
    public void readUnsupportedAuthenticationByAccessToken() throws Exception {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            this.tokenManager.readAuthentication("token");
        });
    }

    @Test
    public void storeAccessToken() {
        this.tokenManager.storeAccessToken(this.createMockAccessToken(), this.createMockAuthentication());
        Mockito.verify(tokenDAO, Mockito.times(1)).storeAccessToken(Mockito.any(OAuth2AccessToken.class), Mockito.any(OAuth2Authentication.class));
    }

    @Test
    public void readAccessToken() throws Exception {
        when(tokenDAO.readAccessToken(Mockito.anyString())).thenReturn(new OAuth2AccessTokenImpl("token"));
        OAuth2AccessToken token = tokenManager.readAccessToken("token");
        Assertions.assertNotNull(token);
        Assertions.assertTrue(token instanceof OAuth2AccessTokenImpl);
        Assertions.assertEquals("token", token.getValue());
    }

    @Test
    public void removeAccessToken() throws Exception {
        this.tokenManager.removeAccessToken(this.createMockAccessToken());
        Mockito.verify(tokenDAO, Mockito.times(1)).removeAccessToken(Mockito.anyString());
    }

    @Test
    public void storeRefreshToken() throws Exception {
        this.tokenManager.storeRefreshToken(new DefaultOAuth2RefreshToken("value"), this.createMockAuthentication());
        Mockito.verifyZeroInteractions(tokenDAO);
    }

    @Test
    public void readRefreshToken() throws Exception {
        when(tokenDAO.readRefreshToken(Mockito.anyString())).thenReturn(Mockito.any(OAuth2RefreshToken.class));
        OAuth2RefreshToken refreshToken = this.tokenManager.readRefreshToken("refresh_token");
        Assertions.assertNull(refreshToken);
        Mockito.verify(tokenDAO, Mockito.times(1)).readRefreshToken("refresh_token");
    }

    @Test
    public void readAuthenticationForRefreshToken() throws Exception {
        when(tokenDAO.readAuthenticationForRefreshToken(Mockito.any(OAuth2RefreshToken.class))).thenReturn(Mockito.any(OAuth2Authentication.class));
        OAuth2RefreshToken refreshToken = new DefaultOAuth2RefreshToken("value");
        OAuth2Authentication auth = this.tokenManager.readAuthenticationForRefreshToken(refreshToken);
        Assertions.assertNull(auth);
        Mockito.verify(tokenDAO, Mockito.times(1)).readAuthenticationForRefreshToken(refreshToken);
    }

    @Test
    public void removeRefreshToken() throws Exception {
        OAuth2RefreshToken refreshToken = new DefaultOAuth2RefreshToken("value_1");
        this.tokenManager.removeRefreshToken(refreshToken);
        Mockito.verify(tokenDAO, Mockito.times(1)).removeAccessTokenUsingRefreshToken("value_1");
    }

    @Test
    public void removeAccessTokenUsingRefreshToken() throws Exception {
        OAuth2RefreshToken refreshToken = new DefaultOAuth2RefreshToken("value_2");
        this.tokenManager.removeAccessTokenUsingRefreshToken(refreshToken);
        Mockito.verify(tokenDAO, Mockito.times(1)).removeAccessTokenUsingRefreshToken("value_2");
    }

    @Test
    public void getAccessToken() throws Exception {
        OAuth2AccessToken token = tokenManager.getAccessToken(this.createMockAuthentication());
        Assertions.assertNotNull(token);
        Assertions.assertTrue(token instanceof OAuth2AccessTokenImpl);
        Assertions.assertEquals("clientId", ((OAuth2AccessTokenImpl) token).getClientId());
        Assertions.assertEquals("username", ((OAuth2AccessTokenImpl) token).getLocalUser());
    }

    private OAuth2AccessToken createMockAccessToken() {
        OAuth2AccessTokenImpl token = new OAuth2AccessTokenImpl("token");
        token.setValue("token");
        token.setClientId("client_id");
        token.setExpiration(new Date());
        token.setGrantType("password");
        token.setLocalUser("username");
        token.setRefreshToken(new DefaultOAuth2RefreshToken("refresh"));
        token.setTokenType("bearer");
        return token;
    }

    private OAuth2Authentication createMockAuthentication() {
        TestingAuthenticationToken mock = new TestingAuthenticationToken("username", "password");
        OAuth2Request oAuth2Request = new OAuth2Request(null, "clientId", null, true, null, null, null, null, null);
        OAuth2Authentication authentication = new OAuth2Authentication(oAuth2Request, mock);
        return authentication;
    }

}
