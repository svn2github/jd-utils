/**
 * Copyright (c) 2009 - 2011 AppWork UG(haftungsbeschränkt) <e-mail@appwork.org>
 * 
 * This file is part of org.appwork.utils.net.httpserver.requests
 * 
 * This software is licensed under the Artistic License 2.0,
 * see the LICENSE file or http://www.opensource.org/licenses/artistic-license-2.0.php
 * for details
 */
package org.appwork.utils.net.httpserver.requests;

import java.io.IOException;
import java.util.LinkedList;

import org.appwork.utils.net.HeaderCollection;

/**
 * @author daniel
 * 
 */
public interface HttpRequestInterface {
    public LinkedList<String[]> getPostParameter() throws IOException;

    public String getRequestedPath();

    public String getRequestedURL();

    /**
     * @return the requestedURLParameters
     */
    public LinkedList<String[]> getRequestedURLParameters();

    public HeaderCollection getRequestHeaders();

}
