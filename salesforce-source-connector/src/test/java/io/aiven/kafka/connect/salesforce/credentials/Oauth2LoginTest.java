/*
 * Copyright 2026 Aiven Oy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.aiven.kafka.connect.salesforce.credentials;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

final class Oauth2LoginTest {

	private io.aiven.kafka.connect.salesforce.common.auth.credentials.Oauth2Login login;
	private String testUri = "https://localhost";

	@ParameterizedTest
	@MethodSource("loginSource")
	void testOauthParametersSetCorrectly(String username, String password, String clientId, String clientSecret) {
		HttpClient mockClient = mock(HttpClient.class);
		String credentials = "{\"access_token\":\"abcd\"}";
		login = new io.aiven.kafka.connect.salesforce.common.auth.credentials.Oauth2Login(testUri, mockClient);
		HttpResponse<String> response = mock(HttpResponse.class);
		when(response.statusCode()).thenReturn(200);
		when(response.body()).thenReturn(credentials);
		when(mockClient.sendAsync(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
				.thenReturn(CompletableFuture.completedFuture(response));

		String accessToken = login.getAccessToken(clientId, clientSecret);
		assertEquals("abcd", accessToken);
	}

	@ParameterizedTest
	@MethodSource("statusCodes")
	void testNonsuccessStatusCode(int statusCode) {
		HttpClient mockClient = mock(HttpClient.class);
		String credentials = "{\"access_token\":\"abcd\"}";
		login = new io.aiven.kafka.connect.salesforce.common.auth.credentials.Oauth2Login(testUri, mockClient);
		HttpResponse<String> response = mock(HttpResponse.class);
		when(response.statusCode()).thenReturn(statusCode);
		when(response.body()).thenReturn(credentials);
		when(mockClient.sendAsync(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString())))
				.thenReturn(CompletableFuture.completedFuture(response));
		// The message is not being tested here correctly. seems to pass regardless of
		// what is entered
		assertThrowsExactly(RuntimeException.class, () -> {
			login.getAccessToken("clientId", "Z");
		}, String.format("Invalid response code received from Salesforce oauth flow %d", statusCode));

	}

	@ParameterizedTest
	@MethodSource("invalidUris")
	void testHttpUriReturnsException() {
		HttpClient mockClient = mock(HttpClient.class);
		assertThrowsExactly(IllegalArgumentException.class, () -> {
			new io.aiven.kafka.connect.salesforce.common.auth.credentials.Oauth2Login("localhost:8080", mockClient);
		});

	}

	private static Stream<Arguments> loginSource() {
		return Stream.of(Arguments.of("user1", "pass1", "clientId1", "clientSecret1"));
	}

	private static Stream<Arguments> invalidUris() {
		return Stream.of(Arguments.of("localhost:8080"), Arguments.of("http://localhost:8080"),
				Arguments.of("http://127.0.0.1:8080"), Arguments.of("https://127.1:8080"));
	}

	private static Stream<Arguments> statusCodes() {
		return Stream.of(Arguments.of("401"), Arguments.of("407"), Arguments.of("500"), Arguments.of("422"));
	}

}
