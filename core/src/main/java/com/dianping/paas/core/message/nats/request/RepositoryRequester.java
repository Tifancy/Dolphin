package com.dianping.paas.core.message.nats.request;

import com.dianping.paas.core.dto.request.AllocateWebPackageRequest;
import com.dianping.paas.core.dto.request.DockerfileRequest;
import com.dianping.paas.core.dto.request.InstanceStartRequest;
import com.dianping.paas.core.dto.response.AllocateWebPackageResponse;
import com.dianping.paas.core.dto.response.DockerfileResponse;
import com.dianping.paas.core.message.nats.MessageCallBack;
import com.dianping.paas.core.message.nats.subscribe.Subject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * Created by yuchao on 12/4/15.
 */
@Component
public class RepositoryRequester extends Requester {
    private static final Logger logger = LogManager.getLogger(RepositoryRequester.class);

    @Resource
    private AgentRequester agentRequester;

    /**
     * 分配上传地址
     *
     * @param request
     */
    public AllocateWebPackageResponse allocateWebPackage(final AllocateWebPackageRequest request) {
        logger.info(String.format("begin allocateWebPackage request: %s", request));

        AllocateWebPackageResponse allocateWebPackageResponse =
                requestSync(Subject.Repository.ALLOCATE_WEB_PACKAGE_REQUEST, request,
                        new MessageCallBack<AllocateWebPackageResponse>(AllocateWebPackageResponse.class) {
                            @Override
                            public void error(Throwable throwable) {
                                logger.error(String.format("error when allocateWebPackage: %s", request));
                            }

                            @Override
                            public void timeout() {
                                logger.error(String.format("timeout when allocateWebPackage: %s", request));
                            }
                        });

        logger.info(String.format("end allocateWebPackage response: %s", allocateWebPackageResponse));

        return allocateWebPackageResponse;
    }

    public void newAndDeploy(final DockerfileRequest dockerfileRequest) {
        logger.info(String.format("begin newAndDeploy request: %s", dockerfileRequest));

        requestAsync(Subject.Instance.NEW_AND_DEPLOY, dockerfileRequest, new MessageCallBack<DockerfileResponse>(DockerfileResponse.class) {
            @Override
            public void success(DockerfileResponse dockerfileResponse) {
                if (dockerfileResponse.isSuccess()) {
                    logger.info("init app success, request: " + dockerfileRequest + ", response: " + dockerfileResponse);
                    agentRequester.pullImageAndRun(buildStartInstanceRequest(dockerfileRequest, dockerfileResponse));
                } else {
                    logger.error("init app error, request: " + dockerfileRequest + ", response: " + dockerfileResponse);
                }
            }

            @Override
            public void error(Throwable throwable) {
                logger.error(String.format("init app error: %s", dockerfileRequest), throwable);
            }

            @Override
            public void timeout() {
                logger.error(String.format("init app timeout, request: %s", dockerfileRequest));
            }
        });
    }

    private InstanceStartRequest buildStartInstanceRequest(DockerfileRequest dockerfileRequest, DockerfileResponse dockerfileResponse) {
        InstanceStartRequest instanceStartRequest = new InstanceStartRequest();

        instanceStartRequest.setRepository(dockerfileResponse.getRepository());
        instanceStartRequest.setAppId(dockerfileRequest.getAppName());
        instanceStartRequest.setImageId(dockerfileResponse.getImageId());
        // TODO 计算当前实例为第几个实例
        instanceStartRequest.setInstanceIndex(1);

        return instanceStartRequest;
    }
}
