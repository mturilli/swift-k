//----------------------------------------------------------------------
//This code is developed as part of the Java CoG Kit project
//The terms of the license can be found at http://www.cogkit.org/license
//This message may not be removed or altered.
//----------------------------------------------------------------------

/*
 * Created on Jul 21, 2005
 */
package org.globus.cog.abstraction.coaster.service.local;

import org.globus.cog.abstraction.coaster.service.Registering;
import org.globus.cog.karajan.workflow.service.ProtocolException;
import org.globus.cog.karajan.workflow.service.handlers.RequestHandler;

public class RegistrationHandler extends RequestHandler {
    public static final String NAME = "REGISTER";

    public void requestComplete() throws ProtocolException {
        String id = this.getInDataAsString(0);
        String url = this.getInDataAsString(1);
        Registering ls = (Registering) getChannel().getChannelContext()
                .getService();
        try {
            ls.registrationReceived(id, url, getChannel());
            this.addOutData("OK");
            this.sendReply();
        }
        catch (Exception e) {
            throw new ProtocolException(e);
        }
    }
}
