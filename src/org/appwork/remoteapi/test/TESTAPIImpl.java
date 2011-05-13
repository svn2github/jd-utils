/**
 * Copyright (c) 2009 - 2011 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.remoteapi.test
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.remoteapi.test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.RemoteAPIRequest;
import org.appwork.remoteapi.RemoteAPIResponse;
import org.appwork.utils.net.HTTPHeader;

/**
 * @author daniel
 * 
 */
public class TESTAPIImpl implements TESTAPI, TestApiInterface {

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.remoteapi.test.TestApiInterface#iAmGod(int,
     * org.appwork.remoteapi.RemoteAPIRequest, int,
     * org.appwork.remoteapi.RemoteAPIResponse, int)
     */
    @Override
    public void iAmGod(final int b, final RemoteAPIRequest request, final int a, final RemoteAPIResponse response, final int c) throws UnsupportedEncodingException, IOException {
        response.setResponseCode(ResponseCode.SUCCESS_OK);
        final String text = "You called god?" + b + "-" + a + "-" + c;

        final int length = text.getBytes().length;
        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_LENGTH, length + ""));
        response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_REQUEST_CONTENT_TYPE, "text"));
        response.getOutputStream().write(text.getBytes("UTF-8"));

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.remoteapi.test.TestApiInterface#merge(java.lang.String,
     * java.lang.String, int, boolean)
     */
    @Override
    public String merge(final String a, final String b, final int a2, final boolean b2) {
        // TODO Auto-generated method stub
        return a + b + a2 + b2;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.remoteapi.test.TestApiInterface#sum(int, int)
     */
    @Override
    public int sum(final long a, final Byte b) {
        // TODO Auto-generated method stub
        return (int) (a + b);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.remoteapi.test.TESTAPI#test()
     */
    @Override
    public String test() {
        return "TestSucessfull";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.appwork.remoteapi.test.TestApiInterface#toggle(boolean)
     */
    @Override
    public boolean toggle(final boolean b) {
        // TODO Auto-generated method stub
        return !b;
    }

}
