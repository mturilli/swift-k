//----------------------------------------------------------------------
//This code is developed as part of the Java CoG Kit project
//The terms of the license can be found at http://www.cogkit.org/license
//This message may not be removed or altered.
//----------------------------------------------------------------------

/*
 * Created on Mar 13, 2008
 */
package org.globus.cog.abstraction.coaster.service.job.manager;

import java.net.ServerSocket;

public class TCPTest {
    public static void main(String[] args) {
        try {
            final ServerSocket ss = new ServerSocket(40000);
            new Thread() {
                
            }.start();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
