/*
 * Copyright 2018 original authors
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
package io.micronaut.http;

import io.micronaut.http.cookie.CookieFactory;
import io.micronaut.core.io.service.ServiceDefinition;
import io.micronaut.core.io.service.SoftServiceLoader;
import io.micronaut.http.cookie.CookieFactory;

import java.util.Optional;

/**
 * A factory interface for {@link MutableHttpRequest} objects
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public interface HttpRequestFactory {
    /**
     * The default {@link CookieFactory} instance
     */
    Optional<HttpRequestFactory> INSTANCE = SoftServiceLoader.load(HttpRequestFactory.class)
            .firstOr("io.micronaut.http.client.NettyClientHttpRequestFactory",
                    HttpRequestFactory.class.getClassLoader()
            )
            .map(ServiceDefinition::load);

    /**
     * Return a {@link MutableHttpRequest} that executes an {@link HttpMethod#GET} request for the given URI
     *
     * @param uri The URI
     * @return The {@link MutableHttpRequest} instance
     */
    <T> MutableHttpRequest<T> get(String uri);

    /**
     * Return a {@link MutableHttpRequest} that executes an {@link HttpMethod#POST} request for the given URI
     *
     * @param uri The URI
     * @return The {@link MutableHttpRequest} instance
     */
    <T> MutableHttpRequest<T> post(String uri, T body);

    /**
     * Return a {@link MutableHttpRequest} that executes an {@link HttpMethod#PUT} request for the given URI
     *
     * @param uri The URI
     * @return The {@link MutableHttpRequest} instance
     */
    <T> MutableHttpRequest<T> put(String uri, T body);

    /**
     * Return a {@link MutableHttpRequest} that executes an {@link HttpMethod#PATCH} request for the given URI
     *
     * @param uri The URI
     * @return The {@link MutableHttpRequest} instance
     */
    <T> MutableHttpRequest<T> patch(String uri, T body);

    /**
     * Return a {@link MutableHttpRequest} that executes an {@link HttpMethod#HEAD} request for the given URI
     *
     * @param uri The URI
     * @return The {@link MutableHttpRequest} instance
     */
    <T> MutableHttpRequest<T> head(String uri);

    /**
     * Return a {@link MutableHttpRequest} that executes an {@link HttpMethod#OPTIONS} request for the given URI
     *
     * @param uri The URI
     * @return The {@link MutableHttpRequest} instance
     */
    <T> MutableHttpRequest<T> options(String uri);

    /**
     * Return a {@link MutableHttpRequest} that executes an {@link HttpMethod#DELETE} request for the given URI
     *
     * @param uri The URI
     * @param body an optional body
     * @return The {@link MutableHttpRequest} instance
     */
    <T> MutableHttpRequest<T> delete(String uri, T body);
    /**
     * Create a new {@link MutableHttpRequest} for the given method and URI
     * @param httpMethod The method
     * @param uri The URI
     * @param <T>
     * @return The request
     */
    <T> MutableHttpRequest<T> create(HttpMethod httpMethod, String uri);
}