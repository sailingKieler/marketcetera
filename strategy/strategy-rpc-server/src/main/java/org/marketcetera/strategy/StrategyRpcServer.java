//
// this file is automatically generated
//
package org.marketcetera.strategy;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.marketcetera.core.Preserve;
import org.marketcetera.strategy.StrategyRpc.FileUploadResponse;
import org.marketcetera.util.log.SLF4JLoggerProxy;

import com.google.protobuf.ByteString;

import io.grpc.stub.StreamObserver;

/* $License$ */

/**
 * Provides an RPC Server for StrategyRpc services.
 *
 * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
 * @version $Id$
 * @since $Release$
 */
@Preserve
public class StrategyRpcServer<SessionClazz>
        extends org.marketcetera.rpc.server.AbstractRpcService<SessionClazz,org.marketcetera.strategy.StrategyRpcServiceGrpc.StrategyRpcServiceImplBase>
{
    /* (non-Javadoc)
    * @see org.marketcetera.rpc.server.AbstractRpcService#getServiceDescription()
    */
    @Override
    protected String getServiceDescription()
    {
        return description;
    }
    /* (non-Javadoc)
    * @see org.marketcetera.rpc.server.AbstractRpcService#getService()
    */
    @Override
    protected org.marketcetera.strategy.StrategyRpcServiceGrpc.StrategyRpcServiceImplBase getService()
    {
        return service;
    }
    /**
     * Validate and start the object.
     */
    @javax.annotation.PostConstruct
    public void start()
            throws Exception
    {
        service = new Service();
        super.start();
    }
    private OutputStream getFilePath(StrategyRpc.FileUploadRequest request)
            throws IOException
    {
        String fileName = request.getMetadata().getName() + ".jar";
        return Files.newOutputStream(SERVER_BASE_PATH.resolve(fileName),
                                     StandardOpenOption.CREATE,
                                     StandardOpenOption.APPEND);
    }
    private static final Path SERVER_BASE_PATH = Paths.get("/tmp/");
    private void writeFile(OutputStream writer,
                           ByteString content)
            throws IOException
    {
        writer.write(content.toByteArray());
        writer.flush();
    }

    private void closeFile(OutputStream writer){
        try {
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * StrategyRpc Service implementation.
     *
     * @author <a href="mailto:colin@marketcetera.com">Colin DuPlantis</a>
     * @version $Id$
     * @since $Release$
     */
    private class Service
            extends org.marketcetera.strategy.StrategyRpcServiceGrpc.StrategyRpcServiceImplBase
    {
        /* (non-Javadoc)
         * @see org.marketcetera.strategy.StrategyRpcServiceGrpc.StrategyRpcServiceImplBase#login(org.marketcetera.rpc.base.BaseRpc.LoginRequest, io.grpc.stub.StreamObserver)
         */
        @Override
        public void login(org.marketcetera.rpc.base.BaseRpc.LoginRequest inRequest,io.grpc.stub.StreamObserver<org.marketcetera.rpc.base.BaseRpc.LoginResponse> inResponseObserver)
        {
            StrategyRpcServer.this.doLogin(inRequest,inResponseObserver);
        }
        /* (non-Javadoc)
         * @see org.marketcetera.strategy.StrategyRpcServiceGrpc.StrategyRpcServiceImplBase#logout(org.marketcetera.rpc.base.BaseRpc.LogoutRequest, io.grpc.stub.StreamObserver)
         */
        @Override
        public void logout(org.marketcetera.rpc.base.BaseRpc.LogoutRequest inRequest,io.grpc.stub.StreamObserver<org.marketcetera.rpc.base.BaseRpc.LogoutResponse> inResponseObserver)
        {
            StrategyRpcServer.this.doLogout(inRequest,inResponseObserver);
        }
        /* (non-Javadoc)
         * @see org.marketcetera.strategy.StrategyRpcServiceGrpc.StrategyRpcServiceImplBase#heartbeat(org.marketcetera.rpc.base.BaseRpc.HeartbeatRequest, io.grpc.stub.StreamObserver)
         */
        @Override
        public void heartbeat(org.marketcetera.rpc.base.BaseRpc.HeartbeatRequest inRequest,io.grpc.stub.StreamObserver<org.marketcetera.rpc.base.BaseRpc.HeartbeatResponse> inResponseObserver)
        {
            StrategyRpcServer.this.doHeartbeat(inRequest,inResponseObserver);
        }
        /* (non-Javadoc)
         * @see org.marketcetera.strategy.StrategyRpcServiceGrpc.StrategyRpcServiceImplBase#getStrategyInstances(org.marketcetera.strategy.StrategyRpc.ReadStrategyInstancesRequest ,io.grpc.stub.StreamObserver)
         */
        @Override
        public void getStrategyInstances(org.marketcetera.strategy.StrategyRpc.ReadStrategyInstancesRequest inReadStrategyInstancesRequest,io.grpc.stub.StreamObserver<org.marketcetera.strategy.StrategyRpc.ReadStrategyInstancesResponse> inResponseObserver)
        {
            try {
                org.marketcetera.util.log.SLF4JLoggerProxy.trace(StrategyRpcServer.this,"Received {}",inReadStrategyInstancesRequest);
                org.marketcetera.util.ws.stateful.SessionHolder<SessionClazz> sessionHolder = validateAndReturnSession(inReadStrategyInstancesRequest.getSessionId());
                authzService.authorize(sessionHolder.getUser(),StrategyPermissions.ReadStrategyAction.name());
                authzService.authorize(sessionHolder.getUser(),StrategyPermissions.LoadStrategyAction.name());
                authzService.authorize(sessionHolder.getUser(),StrategyPermissions.UnloadStrategyAction.name());
                authzService.authorize(sessionHolder.getUser(),StrategyPermissions.StartStrategyAction.name());
                authzService.authorize(sessionHolder.getUser(),StrategyPermissions.StopStrategyAction.name());
                org.marketcetera.strategy.StrategyRpc.ReadStrategyInstancesResponse.Builder responseBuilder = org.marketcetera.strategy.StrategyRpc.ReadStrategyInstancesResponse.newBuilder();
                java.util.Collection<? extends org.marketcetera.strategy.StrategyInstance> serviceData = strategyService.getStrategyInstances(sessionHolder.getUser());
                serviceData.forEach(strategyInstance -> StrategyRpcUtil.getRpcStrategyInstance(strategyInstance).ifPresent(rpcStrategyInstance -> responseBuilder.addStrategyInstances(rpcStrategyInstance)));
                org.marketcetera.strategy.StrategyRpc.ReadStrategyInstancesResponse response = responseBuilder.build();
                org.marketcetera.util.log.SLF4JLoggerProxy.trace(StrategyRpcServer.this,"Responding {}",response);
                inResponseObserver.onNext(response);
                inResponseObserver.onCompleted();
            } catch (Exception e) {
                handleError(e,inResponseObserver);
            }
        }
        /* (non-Javadoc)
         * @see org.marketcetera.strategy.StrategyRpcServiceGrpc.StrategyRpcServiceImplBase#loadStrategyInstance(org.marketcetera.strategy.StrategyRpc.LoadStrategyInstanceRequest ,io.grpc.stub.StreamObserver)
         */
        @Override
        public void loadStrategyInstance(org.marketcetera.strategy.StrategyRpc.LoadStrategyInstanceRequest inLoadStrategyInstanceRequest,io.grpc.stub.StreamObserver<org.marketcetera.strategy.StrategyRpc.LoadStrategyInstanceResponse> inResponseObserver)
        {
            try {
                org.marketcetera.util.log.SLF4JLoggerProxy.trace(StrategyRpcServer.this,"Received {}",inLoadStrategyInstanceRequest);
                org.marketcetera.util.ws.stateful.SessionHolder<SessionClazz> sessionHolder = validateAndReturnSession(inLoadStrategyInstanceRequest.getSessionId());
                authzService.authorize(sessionHolder.getUser(),StrategyPermissions.LoadStrategyAction.name());
                org.marketcetera.strategy.StrategyRpc.LoadStrategyInstanceResponse.Builder responseBuilder = org.marketcetera.strategy.StrategyRpc.LoadStrategyInstanceResponse.newBuilder();
                org.marketcetera.strategy.StrategyInstance strategyInstance = StrategyRpcUtil.getStrategyInstance(inLoadStrategyInstanceRequest.getStrategyInstance(),strategyInstanceFactory,userFactory).orElse(null);
                org.marketcetera.strategy.StrategyStatus serviceData = strategyService.loadStrategyInstance(strategyInstance);
                StrategyRpcUtil.getRpcStrategyStatus(serviceData).ifPresent(rpcStatus -> responseBuilder.setStatus(rpcStatus));
                org.marketcetera.strategy.StrategyRpc.LoadStrategyInstanceResponse response = responseBuilder.build();
                org.marketcetera.util.log.SLF4JLoggerProxy.trace(StrategyRpcServer.this,"Responding {}",response);
                inResponseObserver.onNext(response);
                inResponseObserver.onCompleted();
            } catch (Exception e) {
                handleError(e,inResponseObserver);
            }
        }
        /* (non-Javadoc)
         * @see org.marketcetera.strategy.StrategyRpcServiceGrpc.StrategyRpcServiceImplBase#uploadFile(io.grpc.stub.StreamObserver)
         */
        @Override
        public StreamObserver<StrategyRpc.FileUploadRequest> uploadFile(StreamObserver<StrategyRpc.FileUploadResponse> inResponseObserver)
        {
            return new StreamObserver<StrategyRpc.FileUploadRequest>() {
                // upload context variables
                OutputStream writer;
                StrategyTypesRpc.FileUploadStatus status = StrategyTypesRpc.FileUploadStatus.IN_PROGRESS;

                @Override
                public void onNext(StrategyRpc.FileUploadRequest fileUploadRequest)
                {
                    try {
                        switch(fileUploadRequest.getRequestCase()) {
                            case FILE:
                                writeFile(writer,
                                          fileUploadRequest.getFile().getContent());
                                break;
                            case METADATA:
                                writer = getFilePath(fileUploadRequest);
                                break;
                            case REQUEST_NOT_SET:
                            default:
                                throw new UnsupportedOperationException("Request case not set in file upload request");
                        }
                    } catch (IOException e) {
                        SLF4JLoggerProxy.error(StrategyRpcServer.this,
                                               e);
                        this.onError(e);
                    }
                }
                @Override
                public void onError(Throwable throwable)
                {
                    status = StrategyTypesRpc.FileUploadStatus.FAILED;
                    this.onCompleted();
                }
                @Override
                public void onCompleted()
                {
                    closeFile(writer);
                    status = StrategyTypesRpc.FileUploadStatus.IN_PROGRESS.equals(status) ? StrategyTypesRpc.FileUploadStatus.SUCCESS : status;
                    FileUploadResponse response = FileUploadResponse.newBuilder().setStatus(status).build();
                    inResponseObserver.onNext(response);
                    inResponseObserver.onCompleted();
                }
            };
        }
    }
    /**
     * Creates new StrategyInstance objects
     */
    @org.springframework.beans.factory.annotation.Autowired
    private org.marketcetera.strategy.StrategyInstanceFactory strategyInstanceFactory;
    /**
     * Creates new User objects
     */
    @org.springframework.beans.factory.annotation.Autowired
    private org.marketcetera.admin.UserFactory userFactory;
    /**
     * provides services for Strategy
     */
    @org.springframework.beans.factory.annotation.Autowired
    private org.marketcetera.strategy.StrategyService strategyService;
    /**
     * provides access to authorization services
     */
    @org.springframework.beans.factory.annotation.Autowired
    private org.marketcetera.admin.service.AuthorizationService authzService;
    /**
     * provides the RPC service
     */
    private Service service;
    /**
     * description of this service
     */
    private final static String description = "Strategy RPC Service";
}
