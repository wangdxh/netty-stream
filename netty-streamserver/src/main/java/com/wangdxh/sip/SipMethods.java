/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.wangdxh.sip;

import io.netty.handler.codec.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;

/**
 * The request getMethod of RTSP.
 */
public final class SipMethods {

    /**
     * The OPTIONS getMethod represents a request for information about the communication options
     * available on the request/response chain identified by the Request-URI. This getMethod allows
     * the client to determine the options and/or requirements associated with a resource, or the
     * capabilities of a server, without implying a resource action or initiating a resource
     * retrieval.
     */
    public static final HttpMethod INVITE = new HttpMethod("INVITE");

    /**
     * The DESCRIBE getMethod retrieves the description of a presentation or
     * media object identified by the request URL from a server.
     */
    public static final HttpMethod ACK = new HttpMethod("ACK");

    /**
     * The ANNOUNCE posts the description of a presentation or media object
     * identified by the request URL to a server, or updates the client-side
     * session description in real-time.
     */
    public static final HttpMethod INFO = new HttpMethod("INFO");

    /**
     * The SETUP request for a URI specifies the transport mechanism to be
     * used for the streamed media.
     */
    public static final HttpMethod MESSAGE = new HttpMethod("MESSAGE");

    /**
     * The PLAY getMethod tells the server to start sending data via the
     * mechanism specified in SETUP.
     */
    public static final HttpMethod BYE = new HttpMethod("BYE");

    /**
     * The PAUSE request causes the stream delivery to be interrupted
     * (halted) temporarily.
     */
    public static final HttpMethod REGISTER = new HttpMethod("REGISTER");


    private static final Map<String, HttpMethod> methodMap = new HashMap<String, HttpMethod>();

    static {
        methodMap.put(INVITE.toString(), INVITE);
        methodMap.put(INFO.toString(), INFO);
        methodMap.put(ACK.toString(), ACK);
        methodMap.put(BYE.toString(), BYE);
        methodMap.put(REGISTER.toString(), REGISTER);
        methodMap.put(MESSAGE.toString(), MESSAGE);
    }

    /**
     * Returns the {@link HttpMethod} represented by the specified name.
     * If the specified name is a standard RTSP getMethod name, a cached instance
     * will be returned.  Otherwise, a new instance will be returned.
     */
    public static HttpMethod valueOf(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }

        name = name.trim().toUpperCase();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("empty name");
        }

        HttpMethod result = methodMap.get(name);
        if (result != null) {
            return result;
        } else {
            return new HttpMethod(name);
        }
    }

    private SipMethods() {
    }
}
