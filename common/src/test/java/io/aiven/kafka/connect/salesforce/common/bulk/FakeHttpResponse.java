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
package io.aiven.kafka.connect.salesforce.common.bulk;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import javax.net.ssl.SSLSession;

public class FakeHttpResponse implements HttpResponse<Object> {
  int statusCode;

  public FakeHttpResponse(int statusCode) {
    this.statusCode = statusCode;
  }

  @Override
  public int statusCode() {
    return statusCode;
  }

  @Override
  public HttpRequest request() {
    throw new UnsupportedOperationException("'request' not implemented");
  }

  @Override
  public Optional<HttpResponse<Object>> previousResponse() {
    throw new UnsupportedOperationException("'previous result' not implemented");
  }

  @Override
  public HttpHeaders headers() {
    throw new UnsupportedOperationException("'headers' not implemented");
  }

  @Override
  public String body() {
    return "";
  }

  @Override
  public Optional<SSLSession> sslSession() {
    return Optional.empty();
  }

  @Override
  public URI uri() {
    throw new UnsupportedOperationException("'uri' not implemented");
  }

  @Override
  public HttpClient.Version version() {
    throw new UnsupportedOperationException("'version' not implemented");
  }
}
