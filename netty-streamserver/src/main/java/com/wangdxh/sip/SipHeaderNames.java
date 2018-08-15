/*
 * Copyright 2014 The Netty Project
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

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.util.AsciiString;

/**
 * Standard RTSP header names.
 * <p>
 * These are all defined as lowercase to support HTTP/2 requirements while also not
 * violating RTSP/1.x requirements.  New header names should always be lowercase.
 */
public final class SipHeaderNames {

    public static final AsciiString FROM = HttpHeaderNames.FROM;
    public static final AsciiString TO = AsciiString.cached("to");
    public static final AsciiString CALL_ID = AsciiString.cached("call-id");
    public static final AsciiString MAX_FORWORDS = AsciiString.cached("max-forwords");
    public static final AsciiString AUTHORIZATION = HttpHeaderNames.AUTHORIZATION;
    public static final AsciiString CONTENT_LENGTH = HttpHeaderNames.CONTENT_LENGTH;
    public static final AsciiString CONTENT_TYPE = HttpHeaderNames.CONTENT_TYPE;
    public static final AsciiString CSEQ = AsciiString.cached("cseq");
    public static final AsciiString USER_AGENT = HttpHeaderNames.USER_AGENT;
    public static final AsciiString VIA = HttpHeaderNames.VIA;
    public static final AsciiString WWW_AUTHENTICATE = HttpHeaderNames.WWW_AUTHENTICATE;

    private SipHeaderNames() { }
}
