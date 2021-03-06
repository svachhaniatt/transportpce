/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.gnpy;

import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.opendaylight.transportpce.pce.utils.JsonUtil;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.GnpyApi;
import org.opendaylight.yang.gen.v1.gnpy.path.rev200202.service.PathRequest;
import org.opendaylight.yangtools.yang.common.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/gnpy/api/v1.0/files")
public class GnpyStub {
    private static final Logger LOG = LoggerFactory.getLogger(GnpyStub.class);

    @HEAD
    public Response testConnection() {
        return Response.ok().build();
    }

    @POST
    @Produces({ "application/json" })
    @Consumes({ "application/json" })
    public Response computePath(String body) {
        LOG.info("Received path request {}", body);
        // as type-element and explicit route usage are not in the same namespace than
        // gnpy-api,
        // we add to add prefix if they are not set (request generated by
        // GnpyUtilitiesImpl does not
        // contain prefix)
        if (body != null && !body.contains("gnpy-network-topology")) {
            body = body.replaceAll("\"type\":\\s*\"", "\"type\":\"gnpy-network-topology:")
                    .replaceAll("\"length_units\":\\s*\"", "\"length_units\":\"gnpy-network-topology:");
        }
        if (body != null && !body.contains("gnpy-path-computation-simplified")) {
            body = body.replaceAll("\"explicit-route-usage\":\\s*\"",
                    "\"explicit-route-usage\":\"gnpy-path-computation-simplified:");
        }
        GnpyApi request = (GnpyApi) JsonUtil.getInstance().getDataObjectFromJson(new JsonReader(new StringReader(body)),
                QName.create("gnpy:gnpy-api", "2019-01-03", "gnpy-api"));
        URI location = URI.create("http://127.0.0.1:8008/gnpy/api/v1.0/files");
        // TODO: return different response based on body data
        try {
            String response = null;
            List<PathRequest> pathRequest = request.getServiceFile().getPathRequest();
            // this condition is totally arbitrary and could be modified
            if (!pathRequest.isEmpty() && "127.0.0.31".contentEquals(pathRequest.get(0).getSource().stringValue())) {
                response = Files
                        .readString(Paths.get("src", "test", "resources", "gnpy", "gnpy_result_with_path.json"));
            } else {
                response = Files.readString(Paths.get("src", "test", "resources", "gnpy", "gnpy_result_no_path.json"));
            }

            return Response.created(location).entity(response).build();
        } catch (IOException e) {
            return Response.serverError().build();
        }
    }

}
