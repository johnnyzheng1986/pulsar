/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.pulsar.broker.admin.impl;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang.StringUtils;
import org.apache.pulsar.broker.admin.AdminResource;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.common.io.ConnectorDefinition;
import org.apache.pulsar.functions.proto.Function.FunctionMetaData;
import org.apache.pulsar.functions.proto.InstanceCommunication.FunctionStatus;
import org.apache.pulsar.functions.worker.WorkerService;
import org.apache.pulsar.functions.worker.rest.api.FunctionsImpl;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SourceBase extends AdminResource implements Supplier<WorkerService> {

    private final FunctionsImpl functions;

    public SourceBase() {
        this.functions = new FunctionsImpl(this);
    }

    @Override
    public WorkerService get() {
        return pulsar().getWorkerService();
    }

    @POST
    @ApiOperation(value = "Creates a new Pulsar Source in cluster mode")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "The requester doesn't have admin permissions"),
            @ApiResponse(code = 400, message = "Invalid request (function already exists, etc.)"),
            @ApiResponse(code = 408, message = "Request timeout"),
            @ApiResponse(code = 200, message = "Pulsar Function successfully created")
    })
    @Path("/{tenant}/{namespace}/{sourceName}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response registerSource(final @PathParam("tenant") String tenant,
                                   final @PathParam("namespace") String namespace,
                                   final @PathParam("sourceName") String sourceName,
                                   final @FormDataParam("data") InputStream uploadedInputStream,
                                   final @FormDataParam("data") FormDataContentDisposition fileDetail,
                                   final @FormDataParam("url") String functionPkgUrl,
                                   final @FormDataParam("sourceConfig") String sourceConfigJson) {

        return functions.registerFunction(tenant, namespace, sourceName, uploadedInputStream, fileDetail,
                functionPkgUrl, null, sourceConfigJson, FunctionsImpl.SOURCE, clientAppId());
    }

    @PUT
    @ApiOperation(value = "Updates a Pulsar Source currently running in cluster mode")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "The requester doesn't have admin permissions"),
            @ApiResponse(code = 400, message = "Invalid request (function doesn't exist, etc.)"),
            @ApiResponse(code = 200, message = "Pulsar Function successfully updated")
    })
    @Path("/{tenant}/{namespace}/{sourceName}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response updateSource(final @PathParam("tenant") String tenant,
                                 final @PathParam("namespace") String namespace,
                                 final @PathParam("sourceName") String sourceName,
                                 final @FormDataParam("data") InputStream uploadedInputStream,
                                 final @FormDataParam("data") FormDataContentDisposition fileDetail,
                                 final @FormDataParam("url") String functionPkgUrl,
                                 final @FormDataParam("sourceConfig") String sourceConfigJson) {

        return functions.updateFunction(tenant, namespace, sourceName, uploadedInputStream, fileDetail,
                functionPkgUrl, null, sourceConfigJson, FunctionsImpl.SOURCE, clientAppId());

    }


    @DELETE
    @ApiOperation(value = "Deletes a Pulsar Source currently running in cluster mode")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "The requester doesn't have admin permissions"),
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 404, message = "The function doesn't exist"),
            @ApiResponse(code = 408, message = "Request timeout"),
            @ApiResponse(code = 200, message = "The function was successfully deleted")
    })
    @Path("/{tenant}/{namespace}/{sourceName}")
    public Response deregisterSource(final @PathParam("tenant") String tenant,
                                       final @PathParam("namespace") String namespace,
                                       final @PathParam("sourceName") String sourceName) {
        return functions.deregisterFunction(tenant, namespace, sourceName, FunctionsImpl.SOURCE, clientAppId());
    }

    @GET
    @ApiOperation(
            value = "Fetches information about a Pulsar Source currently running in cluster mode",
            response = FunctionMetaData.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "The requester doesn't have admin permissions"),
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 408, message = "Request timeout"),
            @ApiResponse(code = 404, message = "The function doesn't exist")
    })
    @Path("/{tenant}/{namespace}/{sourceName}")
    public Response getSourceInfo(final @PathParam("tenant") String tenant,
                                  final @PathParam("namespace") String namespace,
                                  final @PathParam("sourceName") String sourceName) throws IOException {
        return functions.getFunctionInfo(
            tenant, namespace, sourceName, FunctionsImpl.SOURCE);
    }

    @GET
    @ApiOperation(
            value = "Displays the status of a Pulsar Source instance",
            response = FunctionStatus.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 403, message = "The requester doesn't have admin permissions"),
            @ApiResponse(code = 404, message = "The function doesn't exist")
    })
    @Path("/{tenant}/{namespace}/{sourceName}/{instanceId}/status")
    public Response getSourceInstanceStatus(final @PathParam("tenant") String tenant,
                                            final @PathParam("namespace") String namespace,
                                            final @PathParam("sourceName") String sourceName,
                                            final @PathParam("instanceId") String instanceId) throws IOException {
        return functions.getFunctionInstanceStatus(
            tenant, namespace, sourceName, FunctionsImpl.SOURCE, instanceId, uri.getRequestUri());
    }

    @GET
    @ApiOperation(
            value = "Displays the status of a Pulsar Source running in cluster mode",
            response = FunctionStatus.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 403, message = "The requester doesn't have admin permissions")
    })
    @Path("/{tenant}/{namespace}/{sourceName}/status")
    public Response getSourceStatus(final @PathParam("tenant") String tenant,
                                    final @PathParam("namespace") String namespace,
                                    final @PathParam("sourceName") String sourceName) throws IOException {
        return functions.getFunctionStatus(tenant, namespace, sourceName, FunctionsImpl.SOURCE, uri.getRequestUri());
    }

    @GET
    @ApiOperation(
            value = "Lists all Pulsar Sources currently deployed in a given namespace",
            response = String.class,
            responseContainer = "Collection"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 403, message = "The requester doesn't have admin permissions")
    })
    @Path("/{tenant}/{namespace}")
    public Response listSources(final @PathParam("tenant") String tenant,
                                final @PathParam("namespace") String namespace) {
        return functions.listFunctions(
            tenant, namespace, FunctionsImpl.SOURCE);

    }

    @POST
    @ApiOperation(value = "Restart source instance", response = Void.class)
    @ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 404, message = "The function does not exist"),
            @ApiResponse(code = 500, message = "Internal server error") })
    @Path("/{tenant}/{namespace}/{sourceName}/{instanceId}/restart")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response restartSource(final @PathParam("tenant") String tenant,
            final @PathParam("namespace") String namespace, final @PathParam("sourceName") String sourceName,
            final @PathParam("instanceId") String instanceId) {
        return functions.restartFunctionInstance(tenant, namespace, sourceName, FunctionsImpl.SOURCE, instanceId, uri.getRequestUri());
    }

    @POST
    @ApiOperation(value = "Restart all source instances", response = Void.class)
    @ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 404, message = "The function does not exist"),
            @ApiResponse(code = 500, message = "Internal server error") })
    @Path("/{tenant}/{namespace}/{sourceName}/restart")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response restartSource(final @PathParam("tenant") String tenant,
            final @PathParam("namespace") String namespace, final @PathParam("sourceName") String sourceName) {
        return functions.restartFunctionInstances(tenant, namespace, sourceName, FunctionsImpl.SOURCE);
    }

    @POST
    @ApiOperation(value = "Stop source instance", response = Void.class)
    @ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 404, message = "The function does not exist"),
            @ApiResponse(code = 500, message = "Internal server error") })
    @Path("/{tenant}/{namespace}/{sourceName}/{instanceId}/stop")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response stopSource(final @PathParam("tenant") String tenant,
            final @PathParam("namespace") String namespace, final @PathParam("sourceName") String sourceName,
            final @PathParam("instanceId") String instanceId) {
        return functions.stopFunctionInstance(tenant, namespace, sourceName, FunctionsImpl.SOURCE, instanceId, uri.getRequestUri());
    }

    @POST
    @ApiOperation(value = "Stop all source instances", response = Void.class)
    @ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 404, message = "The function does not exist"),
            @ApiResponse(code = 500, message = "Internal server error") })
    @Path("/{tenant}/{namespace}/{sourceName}/stop")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response stopSource(final @PathParam("tenant") String tenant,
            final @PathParam("namespace") String namespace, final @PathParam("sourceName") String sourceName) {
        return functions.stopFunctionInstances(tenant, namespace, sourceName, FunctionsImpl.SOURCE);
    }

    @GET
    @ApiOperation(
            value = "Fetches a list of supported Pulsar IO source connectors currently running in cluster mode",
            response = List.class
    )
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "The requester doesn't have admin permissions"),
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 408, message = "Request timeout")
    })
    @Path("/builtinsources")
    public List<ConnectorDefinition> getSourceList() {
        List<ConnectorDefinition> connectorDefinitions = functions.getListOfConnectors();
        List<ConnectorDefinition> retval = new ArrayList<>();
        for (ConnectorDefinition connectorDefinition : connectorDefinitions) {
            if (!StringUtils.isEmpty(connectorDefinition.getSourceClass())) {
                retval.add(connectorDefinition);
            }
        }
        return retval;
    }
}
